package com.fastasyncworldedit.core.history;

import com.fastasyncworldedit.core.internal.io.FaweOutputStream;
import com.sk89q.worldedit.world.World;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for the lazy stream getters in {@link MemoryOptimizedHistory}.
 *
 * <p>The bulk of this class covers a deterministic, single-threaded bug: every getter used to
 * assign its {@code *Stream} buffer field before constructing the wrapping compressed/NBT stream,
 * which can throw. A failure there left the buffer field non-null while the wrapper field
 * ({@code *StreamZip}) stayed null, and {@code flush()}/{@code close()} then dereferenced the
 * wrapper - so an {@link IOException} during history setup surfaced later as a
 * {@link NullPointerException} escaping {@code close()}.</p>
 *
 * <p>The single {@link RepeatedTest} below covers the double-checked-locking hardening. It is
 * deliberately the only one of its kind: all production callers currently reach these getters
 * through {@code AbstractChangeSet#processSet}, which is {@code synchronized} on the same
 * instance, so the interleaving it looks for is not reachable today and the test documents the
 * getters' standalone contract rather than guarding a live regression. Repeating it for all six
 * getters would cost ~2000 threads per CI run for no extra signal.</p>
 */
class MemoryOptimizedHistoryConcurrencyTest {

    private static final int THREADS = 16;
    private static final int ITERATIONS = 5;

    @FunctionalInterface
    private interface StreamGetter {

        Object get(MemoryOptimizedHistory history) throws IOException;

    }

    private static Stream<Arguments> allStreamGetters() {
        return Stream.of(
                arguments("getBlockOS", (StreamGetter) history -> history.getBlockOS(0, 0, 0)),
                arguments("getBiomeOS", (StreamGetter) MemoryOptimizedHistory::getBiomeOS),
                arguments("getEntityCreateOS", (StreamGetter) MemoryOptimizedHistory::getEntityCreateOS),
                arguments("getEntityRemoveOS", (StreamGetter) MemoryOptimizedHistory::getEntityRemoveOS),
                arguments("getTileCreateOS", (StreamGetter) MemoryOptimizedHistory::getTileCreateOS),
                arguments("getTileRemoveOS", (StreamGetter) MemoryOptimizedHistory::getTileRemoveOS)
        );
    }

    /**
     * Regression test for the partial-state bug, across all six getters. Forces the failure
     * deterministically with a spy that makes {@code getCompressedOS()} throw, rather than trying
     * to trigger a real compression-library failure.
     *
     * <p>Against the pre-fix code every parameter fails with
     * {@code Cannot invoke FaweOutputStream.flush() because *StreamZip is null}.</p>
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allStreamGetters")
    void getterFailureLeavesNoPartialStateForFlushOrClose(String name, StreamGetter getter) throws IOException {
        MemoryOptimizedHistory history = spy(new MemoryOptimizedHistory(mock(World.class)));
        doThrow(new IOException("forced failure for test")).when(history).getCompressedOS(any(OutputStream.class));

        assertThrows(IOException.class, () -> getter.get(history), name + "() should propagate the forced failure");

        // The real regression check: flush()/close() must not NPE on the partially-constructed
        // state. Both catch IOException internally and don't declare NPE, so pre-fix this surfaced
        // as an uncaught NullPointerException escaping flush()/close().
        assertDoesNotThrow(history::flush, "flush() must not NPE when " + name + "() failed before publishing");
        assertDoesNotThrow(history::close, "close() must not NPE when " + name + "() failed before publishing");
    }

    /**
     * {@code getBlockOS} is the only getter that does further work ({@code writeHeader}) after
     * successfully constructing the compressed stream. If that work throws, the stream is never
     * published, so nothing else can ever close it - it must be closed on the way out instead of
     * being leaked along with its buffer.
     */
    @Test
    void getBlockOSClosesCompressedStreamWhenHeaderWriteFails() throws IOException {
        MemoryOptimizedHistory history = spy(new MemoryOptimizedHistory(mock(World.class)));
        List<FaweOutputStream> constructed = new ArrayList<>();
        doAnswer(invocation -> {
            FaweOutputStream real = spy((FaweOutputStream) invocation.callRealMethod());
            constructed.add(real);
            return real;
        }).when(history).getCompressedOS(any(OutputStream.class));
        doThrow(new IOException("forced header failure"))
                .when(history).writeHeader(any(OutputStream.class), anyInt(), anyInt(), anyInt());

        assertThrows(IOException.class, () -> history.getBlockOS(0, 0, 0));

        assertEquals(1, constructed.size(), "expected exactly one compressed stream to be constructed");
        verify(constructed.get(0)).close();
        assertDoesNotThrow(history::flush, "flush() must not NPE after a failed header write");
        assertDoesNotThrow(history::close, "close() must not NPE after a failed header write");
    }

    /**
     * Documents the getters' standalone lazy-init contract: concurrent callers must all observe
     * the same instance. See the class javadoc for why only {@code getBlockOS} is covered.
     */
    @RepeatedTest(ITERATIONS)
    void getBlockOSReturnsSingleInstanceUnderConcurrency() throws InterruptedException {
        MemoryOptimizedHistory history = new MemoryOptimizedHistory(mock(World.class));
        assertSingleInstanceAcrossThreads(() -> history.getBlockOS(0, 0, 0), "FaweOutputStream");
    }

    /**
     * Invokes {@code getter} from {@link #THREADS} threads released simultaneously and asserts
     * that every thread observed the very same instance.
     */
    private static void assertSingleInstanceAcrossThreads(Callable<?> getter, String streamName)
            throws InterruptedException {
        Set<Object> distinctInstances = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        CyclicBarrier barrier = new CyclicBarrier(THREADS);
        CountDownLatch done = new CountDownLatch(THREADS);

        for (int i = 0; i < THREADS; i++) {
            Thread thread = new Thread(() -> {
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                    Object stream = getter.call();
                    assertNotNull(stream, "getter returned null");
                    synchronized (distinctInstances) {
                        distinctInstances.add(stream);
                    }
                } catch (Throwable t) {
                    // Recorded rather than rethrown: an exception on a worker thread would
                    // otherwise leave the set empty and masquerade as a race failure.
                    failures.add(t);
                } finally {
                    done.countDown();
                }
            });
            // Daemon: if a getter deadlocks (exactly the failure mode this test guards against),
            // the thread hangs past the done.await() timeout below rather than keeping the whole
            // build alive.
            thread.setDaemon(true);
            thread.start();
        }

        boolean completed = done.await(30, TimeUnit.SECONDS);
        if (!completed) {
            fail("Timed out waiting for racing threads to finish (possible barrier deadlock or hang - "
                    + (THREADS - done.getCount()) + "/" + THREADS + " threads finished)");
        }
        assertTrue(failures.isEmpty(), () -> "Threads failed unexpectedly: " + failures);
        assertEquals(
                1,
                distinctInstances.size(),
                "Expected exactly one distinct " + streamName + " instance across all threads"
        );
    }

}
