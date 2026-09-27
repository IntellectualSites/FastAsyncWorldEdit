package com.fastasyncworldedit.core.concurrency;

import com.fastasyncworldedit.core.util.task.KeyQueuedExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.List;
import java.util.Queue;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stress tests for {@link KeyQueuedExecutorService}, which backs FAWE's per-player edit queues. Its contract: tasks
 * sharing a key never run concurrently and run in submission order, tasks with distinct keys may run in parallel, and
 * no submitted task is ever dropped.
 */
@Tag("stress")
@Isolated
class KeyQueuedExecutorServiceStressTest {

    private ExecutorService parent;
    private KeyQueuedExecutorService<Integer> executor;

    @BeforeEach
    void setUp() {
        // Same kind of pool that AsyncNotifyKeyedQueue uses in production
        parent = new ForkJoinPool(Math.max(4, Runtime.getRuntime().availableProcessors()));
        executor = new KeyQueuedExecutorService<>(parent);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "executor did not terminate");
    }

    /**
     * Many submitters, many keys: per key, tasks must be mutually exclusive and each submitter's tasks must run in the
     * order they were submitted. Every task must run exactly once.
     */
    @Test
    void sameKeyTasksAreExclusiveOrderedAndNeverLost() throws Exception {
        final int submitters = StressHarness.THREADS;
        final int keys = 32;
        final int perSubmitter = StressHarness.scaled(20_000);

        final AtomicIntegerArray active = new AtomicIntegerArray(keys);
        // lastSeen[key * submitters + submitter] = last sequence number executed; only touched while "holding" the key
        final int[] lastSeen = new int[keys * submitters];
        java.util.Arrays.fill(lastSeen, -1);
        final LongAdder executed = new LongAdder();
        final Queue<Throwable> violations = new ConcurrentLinkedQueue<>();
        final Queue<Future<?>> futures = new ConcurrentLinkedQueue<>();

        StressHarness.run("keyed-submit", submitters, s -> {
            SplittableRandom random = new SplittableRandom(s);
            for (int seq = 0; seq < perSubmitter; seq++) {
                final int key = random.nextInt(keys);
                final int sequence = seq;
                futures.add(executor.submit(key, () -> {
                    if (active.incrementAndGet(key) != 1) {
                        violations.add(new AssertionError("two tasks for key " + key + " ran concurrently"));
                    }
                    int slot = key * submitters + s;
                    if (lastSeen[slot] >= sequence) {
                        violations.add(new AssertionError("key " + key + ", submitter " + s + ": task " + sequence
                                + " ran after task " + lastSeen[slot]));
                    }
                    lastSeen[slot] = sequence;
                    if ((sequence & 63) == 0) {
                        Thread.yield(); // widen the window for overlap
                    }
                    executed.increment();
                    active.decrementAndGet(key);
                }));
            }
        });

        for (Future<?> future : futures) {
            future.get(StressHarness.TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }
        StressHarness.rethrow("keyed-invariants", violations);
        assertEquals((long) submitters * perSubmitter, executed.sum(), "tasks were lost or run twice");
    }

    /**
     * Targets the hand-off race in the key runner: the worker finds the queue empty and is about to retire the key at
     * the same moment another thread submits for that key. The new task must still run.
     */
    @Test
    void submissionsRacingQueueDrainAreNeverStranded() throws Exception {
        final int threads = StressHarness.THREADS;
        final int rounds = StressHarness.scaled(5_000);
        final CyclicBarrier barrier = new CyclicBarrier(threads);
        final Queue<Future<?>> futures = new ConcurrentLinkedQueue<>();
        final LongAdder executed = new LongAdder();

        StressHarness.run("keyed-drain-race", threads, t -> {
            for (int round = 0; round < rounds; round++) {
                barrier.await(StressHarness.TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                // Everyone hits the same key, which is usually just about to drain
                futures.add(executor.submit(round & 3, executed::increment));
            }
        });

        for (Future<?> future : futures) {
            future.get(StressHarness.TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }
        assertEquals((long) threads * rounds, executed.sum());
    }

    /**
     * Distinct keys must not be serialized behind each other: N tasks on N keys that all wait on a common barrier can
     * only complete if they run concurrently.
     */
    @Test
    void distinctKeysRunInParallel() throws Exception {
        final int parties = 4;
        final int rounds = StressHarness.scaled(200);
        for (int round = 0; round < rounds; round++) {
            CyclicBarrier barrier = new CyclicBarrier(parties);
            List<Future<Integer>> results = new java.util.ArrayList<>();
            for (int key = 0; key < parties; key++) {
                final int k = key;
                results.add(executor.submit(key, () -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    return k;
                }));
            }
            for (int key = 0; key < parties; key++) {
                assertEquals(key, results.get(key).get(StressHarness.TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
            }
        }
    }

    /**
     * A task that throws must not wedge its key: later tasks for the same key must still run.
     */
    @Test
    void failingTasksDoNotWedgeTheirKey() throws Exception {
        final int iterations = StressHarness.scaled(10_000);
        final AtomicInteger ran = new AtomicInteger();
        final Queue<Future<?>> futures = new ConcurrentLinkedQueue<>();
        StressHarness.run("keyed-failures", StressHarness.THREADS, t -> {
            for (int i = 0; i < iterations; i++) {
                final int n = i;
                futures.add(executor.submit(i & 7, () -> {
                    ran.incrementAndGet();
                    if ((n & 1) == 0) {
                        throw new IllegalStateException("expected");
                    }
                }));
            }
        });
        for (Future<?> future : futures) {
            try {
                future.get(StressHarness.TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.ExecutionException expected) {
                // thrown by the even tasks
            }
        }
        assertEquals(StressHarness.THREADS * iterations, ran.get());
    }

}
