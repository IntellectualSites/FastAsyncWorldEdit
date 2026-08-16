package com.fastasyncworldedit.core.history;

import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.internal.io.FaweOutputStream;
import com.sk89q.worldedit.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.File;
import java.io.IOException;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Regression test for the double-checked locking bug in {@link DiskStorageHistory}'s lazy
 * stream getters: racing threads must always observe the same, single, lazily-constructed
 * stream instance.
 *
 * <p>Runs its test methods on the same thread: {@code @BeforeEach}/{@code @AfterEach} save and
 * restore the global {@code Settings.settings().HISTORY.COMPRESSION_LEVEL}, and this suite has
 * method-level parallelism enabled by default, so two methods of this class racing on that field
 * would corrupt each other's setting.</p>
 */
@Execution(ExecutionMode.SAME_THREAD)
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

        // MainUtil.getCompressedOS() returns a plain, uncompressed stream at COMPRESSION_LEVEL 0
        // and otherwise builds an LZ4/Zstd stack. This test only cares about the identity of the
        // lazily-constructed stream, not compression behavior, so force level 0 to keep it
        // independent of the compression backend selection logic.
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
            try {
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
            } finally {
                // Must run even on assertion failure, or the open stream(s) from the racing
                // getBlockOS() calls above leak - on Windows that can also make @TempDir cleanup
                // fail with a confusing secondary error that masks the real assertion failure.
                history.close();
            }
        }
    }

    @Test
    void getTileCreateOSReturnsSingleInstanceUnderConcurrentAccess() throws Exception {
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            File folder = new File(tempDir, "tile-" + iteration);
            DiskStorageHistory history = new DiskStorageHistory(folder, world, UUID.randomUUID(), iteration);

            Set<Object> instances = Collections.newSetFromMap(new IdentityHashMap<>());
            try {
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
            } finally {
                // See getBlockOSReturnsSingleInstanceUnderConcurrentAccess for why this must run
                // even on assertion failure.
                history.close();
            }
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
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failures.incrementAndGet();
                } catch (BrokenBarrierException | java.util.concurrent.TimeoutException e) {
                    failures.incrementAndGet();
                } catch (Throwable e) {
                    // Throwable, not just RuntimeException: an Error (e.g. an AssertionError from
                    // a failed assertion inside this worker) would otherwise complete this
                    // submitted task's Future exceptionally with nothing checking it, and the
                    // test could still pass since failures wouldn't be incremented.
                    failures.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        boolean completed;
        boolean terminated;
        try {
            completed = done.await(30, TimeUnit.SECONDS);
        } finally {
            // Must run even if done.await() itself is interrupted, not just on the timeout path
            // below - otherwise a stuck/interrupted wait leaks the pool's non-daemon threads.
            executor.shutdownNow();
            terminated = executor.awaitTermination(10, TimeUnit.SECONDS);
        }

        if (!completed) {
            fail("Timed out waiting for racing threads to finish (possible barrier deadlock or hang)");
        }
        if (!terminated) {
            fail("Executor did not terminate after shutdownNow(); worker threads may still be running");
        }
        assertEquals(0, failures.get(), "no thread should have failed while racing to acquire the lazy stream");
    }

    /**
     * Deterministic regression test for the cleanup-on-failure paths in {@code getBlockOS()}: if
     * {@code writeHeader()} fails, the not-yet-published stream must be closed - but if that
     * close attempt itself fails, the resulting exception must not replace (mask) the original
     * header-write failure; it must be attached via {@link Throwable#addSuppressed}.
     */
    @Test
    void getBlockOSCloseFailureIsSuppressedNotMasked() throws Exception {
        DiskStorageHistory history = spy(new DiskStorageHistory(tempDir, world, UUID.randomUUID(), 0));

        IOException closeFailure = new IOException("close failed");
        // Let getCompressedOS() run for real (so the actual FileOutputStream is genuinely wrapped
        // and can be genuinely closed, same as production), then spy just its close() so it
        // releases the real handle - keeping @TempDir cleanup happy - before throwing the
        // synthetic failure this test is targeting.
        doAnswer(invocation -> {
            FaweOutputStream real = (FaweOutputStream) invocation.callRealMethod();
            FaweOutputStream streamSpy = spy(real);
            doAnswer(closeInvocation -> {
                real.close();
                throw closeFailure;
            }).when(streamSpy).close();
            return streamSpy;
        }).when(history).getCompressedOS(any());

        IOException headerFailure = new IOException("header failed");
        doThrow(headerFailure).when(history).writeHeader(any(), anyInt(), anyInt(), anyInt());

        IOException thrown = assertThrows(IOException.class, () -> history.getBlockOS(0, 0, 0));

        assertSame(headerFailure, thrown, "the original header-write failure must propagate, not be masked by a "
                + "failure while closing the stream");
        assertEquals(1, thrown.getSuppressed().length, "the close failure must be attached as a suppressed exception");
        assertSame(closeFailure, thrown.getSuppressed()[0]);
    }

    /**
     * Same as {@link #getBlockOSCloseFailureIsSuppressedNotMasked()}, but the cleanup close()
     * itself fails with an {@link Error} rather than an {@code IOException}. {@code
     * closeQuietly()} must catch {@link Throwable}, not just {@link Exception} - {@link
     * RuntimeException} would not distinguish the two, since it is itself an {@code Exception} -
     * or an {@code Error} from close() would propagate in place of the original and mask it
     * exactly like the checked-exception case does.
     */
    @Test
    void getBlockOSErrorFromCloseIsSuppressedNotMasked() throws Exception {
        DiskStorageHistory history = spy(new DiskStorageHistory(tempDir, world, UUID.randomUUID(), 0));

        Error closeFailure = new AssertionError("close failed with an Error");
        doAnswer(invocation -> {
            FaweOutputStream real = (FaweOutputStream) invocation.callRealMethod();
            FaweOutputStream streamSpy = spy(real);
            doAnswer(closeInvocation -> {
                real.close();
                throw closeFailure;
            }).when(streamSpy).close();
            return streamSpy;
        }).when(history).getCompressedOS(any());

        IOException headerFailure = new IOException("header failed");
        doThrow(headerFailure).when(history).writeHeader(any(), anyInt(), anyInt(), anyInt());

        IOException thrown = assertThrows(IOException.class, () -> history.getBlockOS(0, 0, 0));

        assertSame(headerFailure, thrown, "the original header-write failure must propagate, not be masked by an "
                + "unchecked failure while closing the stream");
        assertEquals(1, thrown.getSuppressed().length, "the close failure must be attached as a suppressed exception");
        assertSame(closeFailure, thrown.getSuppressed()[0]);
    }

}
