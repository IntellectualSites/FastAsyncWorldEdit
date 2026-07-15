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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Regression tests for the write-worker queue lost-wakeup race and non-atomic {@code close()} in
 * {@link AbstractChangeSet}.
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

        // The core assertion: none of these futures should hang. On the unfixed code this
        // occasionally times out under enough contention because a task got stranded in the
        // queue with no worker left to drain it.
        for (Future<?> future : writeTaskFutures) {
            future.get(10, TimeUnit.SECONDS);
        }

        assertEquals(expected, executedCount.get());
    }

    /**
     * Concurrent close() calls must be safe: no exceptions, and the instance ends up closed.
     * Does not attempt to prove flush() only ran once (that's covered by close()'s CAS guard),
     * just that racing close() is safe and idempotent from the outside.
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
