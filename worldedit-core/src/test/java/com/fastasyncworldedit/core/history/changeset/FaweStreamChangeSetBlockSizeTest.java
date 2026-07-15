package com.fastasyncworldedit.core.history.changeset;

import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.history.MemoryOptimizedHistory;
import com.sk89q.worldedit.world.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression test for the {@code blockSize} counter on {@link FaweStreamChangeSet}. Historically
 * this was a plain, unsynchronized {@code long} incremented from multiple pipeline worker
 * threads via {@code blockSize++}, which is not atomic and can silently lose increments under
 * real contention. It is now a {@link java.util.concurrent.atomic.LongAdder}, which this test
 * verifies is accurate under many concurrent writers.
 *
 * <p>Mutates the process-wide {@link Settings} singleton (to avoid needing the {@code lz4-java}
 * compression codec, which is {@code compileOnly} and not present on the unit test runtime
 * classpath), so this class is marked {@link Isolated} to avoid interference with other tests
 * that may run concurrently in the same JVM.</p>
 */
@Isolated
class FaweStreamChangeSetBlockSizeTest {

    private static final int THREADS = 16;
    private static final int CALLS_PER_THREAD = 1000;

    @Test
    void addIsAccurateUnderConcurrentWriters() throws InterruptedException {
        int previousCompressionLevel = Settings.settings().HISTORY.COMPRESSION_LEVEL;
        // Compression level 0 skips the LZ4/Zstd codecs entirely (see MainUtil#getCompressedOS),
        // which keeps this test independent of the compileOnly lz4-java dependency.
        Settings.settings().HISTORY.COMPRESSION_LEVEL = 0;

        World world = mock(World.class);
        when(world.getMinY()).thenReturn(-64);
        when(world.getMaxY()).thenReturn(319);

        try {
            MemoryOptimizedHistory changeSet = new MemoryOptimizedHistory(world);

            ExecutorService executor = Executors.newFixedThreadPool(THREADS);
            CountDownLatch startLatch = new CountDownLatch(1);

            List<Future<?>> futures = new ArrayList<>(THREADS);
            for (int t = 0; t < THREADS; t++) {
                final int threadIndex = t;
                Callable<Void> task = () -> {
                    startLatch.await();
                    for (int i = 0; i < CALLS_PER_THREAD; i++) {
                        changeSet.add(threadIndex, 0, i, 0, 1);
                    }
                    return null;
                };
                futures.add(executor.submit(task));
            }

            startLatch.countDown();
            for (Future<?> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    throw new AssertionError("worker thread failed while calling add()", e.getCause());
                } catch (java.util.concurrent.TimeoutException e) {
                    throw new AssertionError("worker thread did not finish in time", e);
                }
            }
            executor.shutdown();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS), "executor did not terminate in time");

            assertEquals((long) THREADS * CALLS_PER_THREAD, changeSet.longSize());
        } finally {
            Settings.settings().HISTORY.COMPRESSION_LEVEL = previousCompressionLevel;
        }
    }

}
