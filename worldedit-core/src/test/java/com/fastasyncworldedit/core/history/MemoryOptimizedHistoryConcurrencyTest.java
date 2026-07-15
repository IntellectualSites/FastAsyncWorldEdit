package com.fastasyncworldedit.core.history;

import com.sk89q.worldedit.world.World;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

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
    void getTileCreateOSReturnsSingleInstanceUnderConcurrency() throws Exception {
        MemoryOptimizedHistory history = new MemoryOptimizedHistory(mock(World.class));
        assertSingleInstanceAcrossThreads(history::getTileCreateOS, "NBTOutputStream");
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
