package com.fastasyncworldedit.core.database;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the concurrency hardening of {@link DBHandler}.
 *
 * <p>Historically {@code DBHandler#dbHandler()} used an unsynchronized, non-volatile
 * check-then-act lazy singleton ({@code if (INSTANCE == null) INSTANCE = new DBHandler();}),
 * which allowed racing threads to observe distinct (or even partially constructed) instances.
 * {@code dbHandler()} now delegates to a nested initialization-on-demand {@code Holder} class
 * instead.</p>
 *
 * <p><b>Important caveat about {@link #dbHandlerReturnsSameInstanceUnderConcurrentAccess()}:</b>
 * {@code DBHandler} still exposes the deprecated {@code public static final DBHandler IMP =
 * dbHandler();} field, whose initializer runs during {@code DBHandler}'s static class
 * initialization (JLS 12.4.2). Class initialization is guaranteed by the JVM to run at most once,
 * on a single thread, with every other thread that references {@code DBHandler} blocking until it
 * completes. In practice this means the very first call to {@code DBHandler.dbHandler()} from any
 * thread already forces single-threaded initialization of {@code INSTANCE} (or {@code
 * Holder.INSTANCE}) before any other thread can observe the class at all — so a barrier-based
 * concurrency test driving {@code dbHandler()} calls cannot, by itself, distinguish the fixed
 * holder-pattern implementation from the original racy {@code if (INSTANCE == null) INSTANCE =
 * new DBHandler();} implementation: both pass this kind of test today because {@code IMP} happens
 * to mask the race. It is still kept below because it documents/enforces the intended external
 * contract (concurrent callers observe one instance) and would catch a regression that changes
 * {@code IMP} to no longer force eager initialization. {@link
 * #dbHandlerDoesNotUseMutableStaticInstanceField()} is the test that actually pins the fix itself,
 * by asserting the racy mutable {@code static DBHandler INSTANCE} field is gone and a holder class
 * is used instead.</p>
 */
class DBHandlerConcurrencyTest {

    private static final int THREADS = 16;

    @Test
    void dbHandlerReturnsSameInstanceUnderConcurrentAccess() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(THREADS);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            List<Callable<DBHandler>> tasks = IntStream.range(0, THREADS)
                    .<Callable<DBHandler>>mapToObj(i -> () -> {
                        barrier.await(10, TimeUnit.SECONDS);
                        return DBHandler.dbHandler();
                    })
                    .collect(Collectors.toList());

            List<Future<DBHandler>> futures = executor.invokeAll(tasks, 15, TimeUnit.SECONDS);

            Set<DBHandler> distinctInstances = futures.stream()
                    .map(DBHandlerConcurrencyTest::getUnchecked)
                    .collect(Collectors.toSet());

            assertEquals(1, distinctInstances.size(), "all threads must observe the same DBHandler instance");
            DBHandler theInstance = distinctInstances.iterator().next();
            assertSame(DBHandler.dbHandler(), theInstance);
            assertSame(DBHandler.IMP, theInstance, "deprecated IMP field must be reference-equal to dbHandler()");
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    /**
     * Structural regression guard for the actual fix: {@code DBHandler} must not declare a
     * mutable (non-final) {@code static DBHandler}-typed field — that shape is exactly what made
     * {@code dbHandler()}'s old {@code if (INSTANCE == null) INSTANCE = new DBHandler();} racy —
     * and must instead expose the lazy instance via a nested initialization-on-demand holder class
     * whose own instance field is {@code static final}.
     */
    @Test
    void dbHandlerDoesNotUseMutableStaticInstanceField() throws Exception {
        for (Field field : DBHandler.class.getDeclaredFields()) {
            if (field.getType() == DBHandler.class && Modifier.isStatic(field.getModifiers())) {
                assertTrue(
                        Modifier.isFinal(field.getModifiers()),
                        "static DBHandler-typed field '" + field.getName()
                                + "' must be final; a mutable static instance field reintroduces the "
                                + "unsynchronized check-then-act singleton race"
                );
            }
        }

        Class<?> holder = null;
        for (Class<?> nested : DBHandler.class.getDeclaredClasses()) {
            if (Modifier.isStatic(nested.getModifiers())) {
                for (Field field : nested.getDeclaredFields()) {
                    if (field.getType() == DBHandler.class) {
                        holder = nested;
                    }
                }
            }
        }
        assertNotNull(holder, "expected DBHandler to delegate lazy construction to a nested holder class");

        Field instanceField = null;
        for (Field field : holder.getDeclaredFields()) {
            if (field.getType() == DBHandler.class) {
                instanceField = field;
            }
        }
        assertNotNull(instanceField, "holder class must declare a DBHandler-typed instance field");
        assertTrue(Modifier.isStatic(instanceField.getModifiers()) && Modifier.isFinal(instanceField.getModifiers()),
                "holder's instance field must be static final so class-initialization semantics make construction "
                        + "thread-safe without explicit locking");
    }

    private static <T> T getUnchecked(Future<T> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
