package com.fastasyncworldedit.core.history;

import com.sk89q.worldedit.world.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Regression test for broken double-checked locking in {@link MemoryOptimizedHistory}'s lazy
 * stream getters. Prior to the fix, concurrent callers of {@code getBlockOS} / {@code
 * getTileCreateOS} (and siblings) could race past the outer null-check and each construct their
 * own backing stream, silently discarding one caller's buffer.
 *
 * <p>This is a race detector rather than a deterministic reproduction: each test gates many
 * threads on a {@link CyclicBarrier} so they call the getter at roughly the same instant, and
 * is repeated to make the offending interleaving likely to surface.
 */
class MemoryOptimizedHistoryConcurrencyTest {

    private static final int THREADS = 16;
    private static final int ITERATIONS = 20;

    @RepeatedTest(ITERATIONS)
    void getBlockOSReturnsSingleInstanceUnderConcurrency() throws Exception {
        MemoryOptimizedHistory history = new MemoryOptimizedHistory(mock(World.class));
        assertSingleInstanceAcrossThreads(() -> history.getBlockOS(0, 0, 0), "FaweOutputStream");
    }

    @RepeatedTest(ITERATIONS)
    void getBiomeOSReturnsSingleInstanceUnderConcurrency() throws Exception {
        MemoryOptimizedHistory history = new MemoryOptimizedHistory(mock(World.class));
        assertSingleInstanceAcrossThreads(history::getBiomeOS, "FaweOutputStream");
    }

    @RepeatedTest(ITERATIONS)
    void getEntityCreateOSReturnsSingleInstanceUnderConcurrency() throws Exception {
        MemoryOptimizedHistory history = new MemoryOptimizedHistory(mock(World.class));
        assertSingleInstanceAcrossThreads(history::getEntityCreateOS, "NBTOutputStream");
    }

    @RepeatedTest(ITERATIONS)
    void getEntityRemoveOSReturnsSingleInstanceUnderConcurrency() throws Exception {
        MemoryOptimizedHistory history = new MemoryOptimizedHistory(mock(World.class));
        assertSingleInstanceAcrossThreads(history::getEntityRemoveOS, "NBTOutputStream");
    }

    @RepeatedTest(ITERATIONS)
    void getTileCreateOSReturnsSingleInstanceUnderConcurrency() throws Exception {
        MemoryOptimizedHistory history = new MemoryOptimizedHistory(mock(World.class));
        assertSingleInstanceAcrossThreads(history::getTileCreateOS, "NBTOutputStream");
    }

    @RepeatedTest(ITERATIONS)
    void getTileRemoveOSReturnsSingleInstanceUnderConcurrency() throws Exception {
        MemoryOptimizedHistory history = new MemoryOptimizedHistory(mock(World.class));
        assertSingleInstanceAcrossThreads(history::getTileRemoveOS, "NBTOutputStream");
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

    /**
     * Deterministic regression test for a partial-state bug distinct from the DCL race above:
     * every lazy getter used to assign its {@code *Stream} buffer field before constructing the
     * wrapping compressed/NBT stream, which can throw. A failure there left the buffer field
     * non-null while the wrapper field ({@code *StreamZip}) stayed null - and {@code flush()}/
     * {@code close()} gate on the buffer field being non-null before dereferencing the wrapper
     * field, so they would then throw {@link NullPointerException}. The getters now build both
     * into locals and publish them together, so a failed construction leaves neither field set.
     *
     * <p>Forces the failure deterministically with a spy that makes {@code getCompressedOS()}
     * throw on the first call, rather than trying to trigger a real compression-library
     * failure.</p>
     */
    @Test
    void getBlockOSFailureLeavesNoPartialStateForFlushOrClose() throws Exception {
        MemoryOptimizedHistory history = spy(new MemoryOptimizedHistory(mock(World.class)));
        doThrow(new IOException("forced failure for test")).when(history).getCompressedOS(any(OutputStream.class));

        assertThrows(IOException.class, () -> history.getBlockOS(0, 0, 0),
                "getBlockOS() should propagate the forced failure");

        // The real regression check: flush()/close() must not NPE on the partially-constructed
        // state. Both methods catch IOException internally and don't declare NPE, so pre-fix this
        // would surface as an uncaught NullPointerException escaping flush()/close().
        assertDoesNotThrow(history::flush,
                "flush() must not NPE when getBlockOS() failed before publishing its stream");
        assertDoesNotThrow(history::close,
                "close() must not NPE when getBlockOS() failed before publishing its stream");
    }

}
