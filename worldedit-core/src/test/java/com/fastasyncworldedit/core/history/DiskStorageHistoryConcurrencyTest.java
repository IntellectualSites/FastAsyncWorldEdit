package com.fastasyncworldedit.core.history;

import com.fastasyncworldedit.core.configuration.Settings;
import com.sk89q.worldedit.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression test for the double-checked locking bug in {@link DiskStorageHistory}'s lazy
 * stream getters: racing threads must always observe the same, single, lazily-constructed
 * stream instance.
 */
class DiskStorageHistoryConcurrencyTest {

    private static final int THREADS = 16;
    private static final int ITERATIONS = 20;

    @TempDir
    File tempDir;

    private World world;
    private int originalCompressionLevel;

    @BeforeEach
    void setUp() {
        world = mock(World.class);
        when(world.getMinY()).thenReturn(-64);
        when(world.getMaxY()).thenReturn(319);
        when(world.getName()).thenReturn("concurrency-test-world");

        // The LZ4/Zstd compression backends used at COMPRESSION_LEVEL > 0 are only
        // `compileOnly` dependencies of worldedit-core (they're expected to be shaded in at
        // runtime by the platform jars), so they aren't on the unit test classpath. Force
        // uncompressed streams so getCompressedOS() doesn't throw NoClassDefFoundError - this
        // test is only concerned with the identity of the lazily-constructed stream, not with
        // compression behavior.
        originalCompressionLevel = Settings.settings().HISTORY.COMPRESSION_LEVEL;
        Settings.settings().HISTORY.COMPRESSION_LEVEL = 0;
    }

    @AfterEach
    void tearDown() {
        Settings.settings().HISTORY.COMPRESSION_LEVEL = originalCompressionLevel;
    }

    @Test
    void getBlockOSReturnsSingleInstanceUnderConcurrentAccess() throws Exception {
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            File folder = new File(tempDir, "block-" + iteration);
            DiskStorageHistory history = new DiskStorageHistory(folder, world, UUID.randomUUID(), iteration);

            Set<Object> instances = Collections.newSetFromMap(new IdentityHashMap<>());
            runConcurrently(THREADS, () -> {
                try {
                    return history.getBlockOS(0, 0, 0);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, instances);

            assertEquals(
                    1,
                    instances.size(),
                    "getBlockOS() must return exactly one distinct stream instance across racing threads (iteration "
                            + iteration + ")"
            );
            history.close();
        }
    }

    @Test
    void getTileCreateOSReturnsSingleInstanceUnderConcurrentAccess() throws Exception {
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            File folder = new File(tempDir, "tile-" + iteration);
            DiskStorageHistory history = new DiskStorageHistory(folder, world, UUID.randomUUID(), iteration);

            Set<Object> instances = Collections.newSetFromMap(new IdentityHashMap<>());
            runConcurrently(THREADS, () -> {
                try {
                    return history.getTileCreateOS();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, instances);

            assertEquals(
                    1,
                    instances.size(),
                    "getTileCreateOS() must return exactly one distinct stream instance across racing threads (iteration "
                            + iteration + ")"
            );
            history.close();
        }
    }

    /**
     * Spins up {@code threadCount} threads, gates them behind a {@link CyclicBarrier} so they all
     * invoke {@code action} at roughly the same instant, and records the identity of every
     * returned object into {@code resultsOut}.
     */
    private void runConcurrently(int threadCount, java.util.function.Supplier<Object> action, Set<Object> resultsOut)
            throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                    Object result = action.get();
                    synchronized (resultsOut) {
                        resultsOut.add(result);
                    }
                } catch (InterruptedException | BrokenBarrierException | java.util.concurrent.TimeoutException e) {
                    failures.incrementAndGet();
                } catch (RuntimeException e) {
                    failures.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        done.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertEquals(0, failures.get(), "no thread should have failed while racing to acquire the lazy stream");
    }

}
