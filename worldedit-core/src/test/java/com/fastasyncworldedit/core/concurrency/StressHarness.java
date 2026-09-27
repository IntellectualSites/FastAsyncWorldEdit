package com.fastasyncworldedit.core.concurrency;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Small harness for concurrency stress tests.
 * <p>
 * All workers are released at the same instant to maximise contention. Any throwable thrown by a worker fails the run,
 * and a run that does not finish within its timeout fails with a thread dump (and the deadlock cycle, if the JVM can
 * detect one) instead of hanging the build.
 * <p>
 * The intensity can be raised with the {@code fawe.stress.factor} system property (default 1), e.g.
 * {@code ./gradlew :worldedit-core:stressTest -Pfawe.stress.factor=20}.
 */
final class StressHarness {

    static final int FACTOR = Math.max(1, Integer.getInteger("fawe.stress.factor", 1));
    static final int THREADS = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
    static final Duration TIMEOUT = Duration.ofSeconds(60L * FACTOR);

    private StressHarness() {
    }

    /**
     * Scale a base iteration count by the configured stress factor.
     */
    static int scaled(int base) {
        return base * FACTOR;
    }

    /**
     * Run {@code worker} on {@code threads} threads released simultaneously and wait for all of them to finish.
     *
     * @throws AssertionError if any worker throws, or if the workers do not finish within {@link #TIMEOUT}
     */
    static void run(String name, int threads, Worker worker) {
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();
        List<Thread> workers = new ArrayList<>(threads);
        for (int i = 0; i < threads; i++) {
            final int index = i;
            Thread thread = new Thread(() -> {
                try {
                    ready.countDown();
                    go.await();
                    worker.run(index);
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    done.countDown();
                }
            }, "stress-" + name + "-" + i);
            thread.setDaemon(true);
            workers.add(thread);
            thread.start();
        }
        try {
            if (!ready.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new AssertionError("[" + name + "] workers failed to start");
            }
            go.countDown();
            if (!done.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                String dump = describeStuck(workers);
                workers.forEach(Thread::interrupt);
                throw new AssertionError("[" + name + "] workers did not finish within " + TIMEOUT + " (deadlock or "
                        + "livelock?)\n" + dump);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("[" + name + "] interrupted", e);
        }
        rethrow(name, failures);
    }

    /**
     * Throw an {@link AssertionError} carrying every collected failure, if there are any.
     */
    static void rethrow(String name, Queue<Throwable> failures) {
        if (failures.isEmpty()) {
            return;
        }
        Throwable first = failures.peek();
        AssertionError error = new AssertionError(
                "[" + name + "] " + failures.size() + " worker failure(s), first: " + first,
                first
        );
        failures.stream().skip(1).limit(10).forEach(error::addSuppressed);
        throw error;
    }

    private static String describeStuck(List<Thread> workers) {
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        StringBuilder sb = new StringBuilder();
        long[] deadlocked = mx.findDeadlockedThreads();
        if (deadlocked != null) {
            sb.append("Deadlock detected between threads:\n");
            for (ThreadInfo info : mx.getThreadInfo(deadlocked, true, true)) {
                sb.append(info);
            }
        }
        for (Thread thread : workers) {
            if (!thread.isAlive()) {
                continue;
            }
            sb.append('"').append(thread.getName()).append("\" ").append(thread.getState()).append('\n');
            for (StackTraceElement element : thread.getStackTrace()) {
                sb.append("\tat ").append(element).append('\n');
            }
        }
        return sb.toString();
    }

    @FunctionalInterface
    interface Worker {

        void run(int threadIndex) throws Throwable;

    }

}
