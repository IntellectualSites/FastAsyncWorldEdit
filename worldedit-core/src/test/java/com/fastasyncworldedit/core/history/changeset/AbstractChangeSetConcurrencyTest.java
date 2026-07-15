package com.fastasyncworldedit.core.history.changeset;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.implementation.QueueHandler;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.world.biome.BiomeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Concurrency tests for the write-worker queue and {@code close()} in {@link AbstractChangeSet}.
 *
 * <p>Honesty note on what these tests prove: the lost-wakeup window in the pre-fix
 * {@code drainQueue()} is a narrow interleaving that this stress test does <em>not</em> reliably
 * hit — it passes against the unfixed code too. It is a contention smoke test guarding the
 * queue/close paths against gross regressions (hangs, dropped tasks, exceptions), not a
 * deterministic reproduction of the race. The lost-wakeup and close-atomicity fixes rest on the
 * reasoning documented in {@code AbstractChangeSet} itself. The interrupt test below <em>is</em>
 * deterministic for the behavior it checks.</p>
 */
class AbstractChangeSetConcurrencyTest {

    private final List<ExecutorService> poolsToShutdown = new ArrayList<>();

    @AfterEach
    void tearDown() {
        poolsToShutdown.forEach(ExecutorService::shutdownNow);
        poolsToShutdown.clear();
    }

    /**
     * {@link AbstractChangeSet#triggerWorker()} dispatches drain work via
     * {@code Fawe.instance().getQueueHandler().async(...)}. There is no live Fawe platform in unit
     * tests, so every thread that may reach that code path needs {@code Fawe.instance()} stubbed.
     * Mockito's static mocks are thread-confined (a {@link MockedStatic} registered on one thread
     * has no effect on calls made from another thread), so a single {@code mockStatic()} call in
     * the test thread is not enough here - producer threads and the emulated async-drain-worker
     * pool both call into {@code Fawe.instance()} directly. To cover every thread, this creates a
     * {@link ThreadFactory} whose threads open the {@link MockedStatic} scope for their entire
     * lifetime (wrapping the pool's internal work loop) before ever executing a submitted task.
     */
    private ExecutorService newFaweAwareFixedThreadPool(int threads, Fawe faweMock) {
        ThreadFactory factory = runnable -> new Thread(() -> {
            try (MockedStatic<Fawe> faweStatic = Mockito.mockStatic(Fawe.class)) {
                faweStatic.when(Fawe::instance).thenReturn(faweMock);
                runnable.run();
            }
        });
        ExecutorService pool = Executors.newFixedThreadPool(threads, factory);
        poolsToShutdown.add(pool);
        return pool;
    }

    /**
     * Creates the mocked {@code Fawe}/{@code QueueHandler} pair and the Fawe-aware pool that backs
     * {@code QueueHandler.async(...)}. The async-drain-worker pool must itself be Fawe-aware too:
     * {@code drainQueue}'s lost-wakeup re-check can call {@code triggerWorker()} - and therefore
     * {@code Fawe.instance()} - again from a drain-worker thread, not just from producer threads.
     */
    private Fawe newFaweMockWithAsyncPool(int asyncPoolThreads) {
        Fawe faweMock = Mockito.mock(Fawe.class);
        QueueHandler queueHandlerMock = Mockito.mock(QueueHandler.class);
        when(faweMock.getQueueHandler()).thenReturn(queueHandlerMock);
        ExecutorService asyncPool = newFaweAwareFixedThreadPool(asyncPoolThreads, faweMock);
        when(queueHandlerMock.async(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            return asyncPool.submit(task);
        });
        return faweMock;
    }

    /**
     * Stress the write-worker queue: many threads race to enqueue write tasks while workers drain
     * them. Prior to the lost-wakeup fix, a drainer could observe the queue as empty and release
     * its permit at the same moment a producer added a task and found the permit still "held",
     * skipping the scheduling of a new worker; the produced task's future would then never
     * complete. With the fix, every returned future must complete promptly.
     */
    @Test
    void addWriteTaskFuturesAllCompleteUnderContention() throws Exception {
        TestChangeSet changeSet = new TestChangeSet();

        int producerThreads = 16;
        int tasksPerThread = 200;
        AtomicInteger executedCount = new AtomicInteger();
        List<Future<?>> writeTaskFutures = Collections.synchronizedList(new ArrayList<>());

        // Async-drain-worker pool emulating FAWE's real QueueHandler executor (where
        // drainQueue(false) actually runs), plus the producer pool that calls addWriteTask(...).
        // Both need Fawe.instance() stubbed - see newFaweAwareFixedThreadPool's javadoc.
        Fawe faweMock = newFaweMockWithAsyncPool(8);
        ExecutorService producers = newFaweAwareFixedThreadPool(producerThreads, faweMock);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> producerHandles = new ArrayList<>();

        for (int t = 0; t < producerThreads; t++) {
            producerHandles.add(producers.submit(() -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < tasksPerThread; i++) {
                    // completeNow = false forces tasks through the async queue/drainQueue path
                    // that contains the fix, rather than running synchronously.
                    Future<?> future = changeSet.addWriteTask(executedCount::incrementAndGet, false);
                    writeTaskFutures.add(future);
                }
            }));
        }

        startLatch.countDown();
        for (Future<?> handle : producerHandles) {
            handle.get(15, TimeUnit.SECONDS);
        }

        int expected = producerThreads * tasksPerThread;
        assertEquals(expected, writeTaskFutures.size());

        // The core assertion: none of these futures should hang. On the unfixed code a task
        // could in principle be stranded in the queue with no worker left to drain it (the
        // lost-wakeup window), though in practice this test does not reliably hit that narrow
        // interleaving - see the class javadoc. A timeout here still catches gross regressions.
        for (Future<?> future : writeTaskFutures) {
            future.get(10, TimeUnit.SECONDS);
        }

        assertEquals(expected, executedCount.get());
    }

    /**
     * Concurrent close() calls must be safe: no exceptions, and the instance ends up closed.
     * Does not attempt to prove flush() only ran once (that's covered by close()'s
     * synchronized-on-closeLock guard), just that racing close() is safe and idempotent from
     * the outside.
     */
    @Test
    void concurrentCloseIsSafeAndIdempotent() throws Exception {
        TestChangeSet changeSet = new TestChangeSet();

        Fawe faweMock = newFaweMockWithAsyncPool(4);

        int threads = 8;
        ExecutorService pool = newFaweAwareFixedThreadPool(threads, faweMock);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                try {
                    startLatch.await();
                    changeSet.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        startLatch.countDown();
        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }

        assertTrue(changeSet.closed);
    }

    /**
     * Deterministic regression test for the interrupted-close hole: a thread that calls
     * {@code close()} while another worker holds the drain permit, and that is interrupted,
     * must still wait for the queue to be drained before {@code close()} returns — and must
     * observe its interrupt flag afterwards. Prior to the {@code acquireUninterruptibly} fix,
     * the interrupted closer returned from the semaphore wait early, {@code flush()} swallowed
     * the situation, and {@code close()} marked the changeset closed with tasks still queued.
     *
     * <p>Both outcomes are checked deterministically: pre-fix, {@code close()} returns almost
     * immediately (the interrupted acquire throws at once when the flag is already set), which
     * the short-timeout {@code get} below converts into a test failure; post-fix, {@code close()}
     * is still blocked at that point and completes only after the in-flight drainer is
     * released.</p>
     */
    @Test
    void interruptedCloseStillDrainsQueue() throws Exception {
        TestChangeSet changeSet = new TestChangeSet();
        Fawe faweMock = newFaweMockWithAsyncPool(2);

        CountDownLatch drainerStarted = new CountDownLatch(1);
        CountDownLatch releaseDrainer = new CountDownLatch(1);
        AtomicInteger executed = new AtomicInteger();

        ExecutorService submitter = newFaweAwareFixedThreadPool(1, faweMock);
        // First task occupies the single drain permit until released.
        submitter.submit(() -> changeSet.addWriteTask(() -> {
            drainerStarted.countDown();
            try {
                releaseDrainer.await(20, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executed.incrementAndGet();
        }, false)).get(10, TimeUnit.SECONDS);
        assertTrue(drainerStarted.await(10, TimeUnit.SECONDS), "drain worker never started");

        // Second task sits in the queue behind the blocked drainer; triggerWorker skips
        // scheduling because the permit is held.
        submitter.submit(() -> changeSet.addWriteTask(executed::incrementAndGet, false))
                .get(10, TimeUnit.SECONDS);

        AtomicBoolean interruptFlagPreserved = new AtomicBoolean();
        ExecutorService closer = newFaweAwareFixedThreadPool(1, faweMock);
        Future<?> closeResult = closer.submit(() -> {
            // Arrive at the semaphore wait with the interrupt flag already set: the pre-fix
            // interruptible acquire() throws immediately in that state.
            Thread.currentThread().interrupt();
            try {
                changeSet.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            interruptFlagPreserved.set(Thread.interrupted());
        });

        try {
            closeResult.get(2, TimeUnit.SECONDS);
            fail("close() returned before pending write tasks were drained (pre-fix behavior)");
        } catch (TimeoutException expectedWhileDrainerHoldsPermit) {
            // Correct: close() is waiting uninterruptibly for the in-flight drainer.
        }

        releaseDrainer.countDown();
        closeResult.get(15, TimeUnit.SECONDS);

        assertEquals(2, executed.get(), "close() must not return with queued tasks undrained");
        assertTrue(changeSet.closed);
        assertTrue(interruptFlagPreserved.get(), "the interrupt flag must survive close()");
    }

    /**
     * Minimal concrete {@link AbstractChangeSet} for exercising queue/close concurrency without
     * any platform/world dependencies. {@link NullChangeSet} is not usable here since it overrides
     * {@code close()} as a no-op, bypassing the exact logic under test.
     */
    private static class TestChangeSet extends AbstractChangeSet {

        TestChangeSet() {
            super(null);
        }

        @Override
        public void add(int x, int y, int z, int combinedFrom, int combinedTo) {
        }

        @Override
        public void addTileCreate(FaweCompoundTag tag) {
        }

        @Override
        public void addTileRemove(FaweCompoundTag tag) {
        }

        @Override
        public void addEntityRemove(FaweCompoundTag tag) {
        }

        @Override
        public void addEntityCreate(FaweCompoundTag tag) {
        }

        @Override
        public void addBiomeChange(int x, int y, int z, BiomeType from, BiomeType to) {
        }

        @Override
        public ChangeExchangeCoordinator getCoordinatedChanges(BlockBag blockBag, int mode, boolean dir) {
            return null;
        }

        @Override
        public Iterator<Change> getIterator(boolean redo) {
            return Collections.emptyIterator();
        }

        @Override
        public boolean isRecordingChanges() {
            return false;
        }

        @Override
        public void setRecordChanges(boolean recordChanges) {
        }

        @Override
        public int size() {
            return 0;
        }

    }

}
