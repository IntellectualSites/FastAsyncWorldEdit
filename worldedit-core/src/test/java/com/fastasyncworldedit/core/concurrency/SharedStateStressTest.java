package com.fastasyncworldedit.core.concurrency;

import com.fastasyncworldedit.core.limit.FaweLimit;
import com.fastasyncworldedit.core.queue.implementation.QueuePool;
import com.fastasyncworldedit.core.util.collection.CleanableThreadLocal;
import com.fastasyncworldedit.core.util.task.AsyncNotifyKeyedQueue;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Stress tests for state that FAWE shares between its parallel queue workers: edit limits, object pools, thread
 * locals and the per-player async queue.
 */
@Tag("stress")
@Isolated
class SharedStateStressTest {

    /**
     * A {@link FaweLimit} is shared by every worker of a parallel edit, so the budget must be exact: with a limit of
     * N, exactly N operations are allowed no matter how many threads consume it.
     */
    @Test
    void limitBudgetIsExactUnderContention() {
        final long budget = StressHarness.scaled(1_000_000);
        final FaweLimit limit = new FaweLimit();
        limit.MAX_CHANGES = new AtomicLong(budget);
        limit.MAX_CHECKS = new AtomicLong(budget);
        final LongAdder allowedChanges = new LongAdder();
        final LongAdder allowedChecks = new LongAdder();

        StressHarness.run("limit-budget", StressHarness.THREADS, t -> {
            boolean changes = true;
            boolean checks = true;
            while (changes || checks) {
                if (changes) {
                    if (limit.MAX_CHANGES()) {
                        changes = false;
                    } else {
                        allowedChanges.increment();
                    }
                }
                if (checks) {
                    try {
                        // Batched consumption, as done when a whole chunk is accounted for at once
                        limit.THROW_MAX_CHECKS(1);
                        allowedChecks.increment();
                    } catch (RuntimeException e) {
                        checks = false;
                    }
                }
            }
        });

        assertEquals(budget, allowedChanges.sum(), "MAX_CHANGES budget over/under-granted");
        assertEquals(budget, allowedChecks.sum(), "MAX_CHECKS budget over/under-granted");
    }

    /**
     * An object handed out by a {@link QueuePool} must be exclusively owned until it is returned.
     */
    @Test
    void poolNeverHandsOutAnObjectTwice() {
        final AtomicInteger created = new AtomicInteger();
        final QueuePool<Pooled> pool = new QueuePool<>(() -> {
            created.incrementAndGet();
            return new Pooled();
        });
        final int iterations = StressHarness.scaled(200_000);

        StressHarness.run("pool-ownership", StressHarness.THREADS, t -> {
            for (int i = 0; i < iterations; i++) {
                Pooled a = pool.poll();
                Pooled b = pool.poll();
                if (!a.inUse.compareAndSet(false, true) || !b.inUse.compareAndSet(false, true)) {
                    throw new AssertionError("pooled object handed out while still in use");
                }
                assertNotSame(a, b);
                a.inUse.set(false);
                pool.offer(a);
                b.inUse.set(false);
                pool.offer(b);
            }
        });
        // Every object ever created is back in the pool, exactly once
        assertEquals(created.get(), pool.size());
        assertEquals(created.get(), Set.copyOf(pool).size());
    }

    /**
     * {@link CleanableThreadLocal} is used for per-thread scratch buffers; a buffer must never be visible to two
     * threads, and {@link CleanableThreadLocal#clean()} must only affect the calling thread.
     */
    @Test
    void threadLocalValuesAreNeverShared() {
        final Set<Object> seen = ConcurrentHashMap.newKeySet();
        final CleanableThreadLocal<int[]> local = new CleanableThreadLocal<>(() -> new int[1]);
        final int iterations = StressHarness.scaled(100_000);

        StressHarness.run("thread-local", StressHarness.THREADS, t -> {
            int[] mine = local.get();
            if (!seen.add(mine)) {
                throw new AssertionError("thread-local buffer shared between threads");
            }
            for (int i = 0; i < iterations; i++) {
                int[] current = local.get();
                assertSame(mine, current, "thread-local changed without clean()");
                current[0]++;
                if (i % 10_000 == 9_999) {
                    local.clean();
                    mine = local.get();
                    if (!seen.add(mine)) {
                        throw new AssertionError("fresh thread-local buffer shared between threads");
                    }
                }
            }
        });
    }

    /**
     * {@link AsyncNotifyKeyedQueue} runs a player's edits one at a time, in order, on a shared pool. Several players
     * submitting at once must not see their own tasks overlap or reorder, and must not block each other.
     */
    @Test
    void keyedAsyncQueueSerializesPerPlayer() {
        final int players = StressHarness.THREADS;
        final int perPlayer = StressHarness.scaled(5_000);
        final Queue<Throwable> failures = new ConcurrentLinkedQueue<>();
        final LongAdder executed = new LongAdder();

        StressHarness.run("async-keyed-queue", players, p -> {
            UUID player = new UUID(0xFA3E, p);
            java.util.List<Future<Object>> futures = new java.util.ArrayList<>(perPlayer);
            AtomicBoolean running = new AtomicBoolean();
            int[] last = {-1};
            try (AsyncNotifyKeyedQueue queue = new AsyncNotifyKeyedQueue((thread, e) -> failures.add(e), () -> player)) {
                for (int i = 0; i < perPlayer; i++) {
                    final int seq = i;
                    futures.add(queue.run(() -> {
                        if (!running.compareAndSet(false, true)) {
                            throw new AssertionError("player " + player + ": tasks overlapped");
                        }
                        if (last[0] != seq - 1) {
                            throw new AssertionError("player " + player + ": task " + seq + " ran after " + last[0]);
                        }
                        last[0] = seq;
                        executed.increment();
                        running.set(false);
                    }));
                }
                // Wait for this player's tasks before closing, closing cancels pending work
                for (Future<Object> f : futures) {
                    f.get(StressHarness.TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                }
            }
        });

        StressHarness.rethrow("async-keyed-queue", failures);
        assertEquals((long) players * perPlayer, executed.sum(), "tasks lost");
    }

    /**
     * Once {@link AsyncNotifyKeyedQueue#close()} has returned, no task that has not yet started may run.
     */
    @Test
    void closedKeyedAsyncQueueRunsNothingAfterClose() throws Exception {
        final int rounds = StressHarness.scaled(2_000);
        final AtomicInteger ranAfterClose = new AtomicInteger();
        for (int round = 0; round < rounds; round++) {
            UUID player = new UUID(0xC105ED, round);
            AtomicBoolean closed = new AtomicBoolean();
            AsyncNotifyKeyedQueue queue = new AsyncNotifyKeyedQueue((thread, e) -> {
            }, () -> player);
            java.util.concurrent.CountDownLatch gate = new java.util.concurrent.CountDownLatch(1);
            // First task blocks the key so the following ones are definitely still queued at close()
            Future<Object> blocker = queue.run(() -> {
                try {
                    gate.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            Queue<Future<Object>> queued = new ConcurrentLinkedQueue<>();
            for (int i = 0; i < 4; i++) {
                queued.add(queue.run(() -> {
                    if (closed.get()) {
                        ranAfterClose.incrementAndGet();
                    }
                }));
            }
            queue.close();
            closed.set(true);
            gate.countDown();
            waitQuietly(blocker);
            queued.forEach(SharedStateStressTest::waitQuietly);
        }
        assertEquals(0, ranAfterClose.get(), "tasks ran after the queue was closed");
    }

    private static void waitQuietly(Future<?> future) {
        try {
            future.get(StressHarness.TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.CancellationException | java.util.concurrent.ExecutionException ignored) {
            // cancelled because the queue was closed
        } catch (InterruptedException | java.util.concurrent.TimeoutException e) {
            throw new AssertionError("task never completed", e);
        }
    }

    private static final class Pooled {

        final AtomicBoolean inUse = new AtomicBoolean();

    }

}
