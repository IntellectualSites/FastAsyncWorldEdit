package com.fastasyncworldedit.core.history;

import com.fastasyncworldedit.core.history.changeset.FaweStreamChangeSet;
import com.sk89q.worldedit.util.io.file.SafeFiles;
import com.sk89q.worldedit.world.NullWorld;
import com.sk89q.worldedit.world.World;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Hand-rolled micro-benchmark for the {@code com.fastasyncworldedit.core.history} write hot path
 * ({@link DiskStorageHistory} and {@link MemoryOptimizedHistory}).
 *
 * <p>This is intentionally NOT a JMH benchmark. Wiring the {@code me.champeau.jmh} plugin into this
 * repository's Gradle 9 multi-module build (custom ANTLR source generation, annotation processors,
 * platform sub-modules) was judged likely to eat more time than the number itself was worth, so a
 * plain warmup + measured-loop benchmark using {@link System#nanoTime()} was used instead, per the
 * task's documented fallback. The goal is a repeatable, comparable number across a "before" and
 * "after" run of the Phase 1 concurrency fixes, not a polished benchmarking harness.</p>
 *
 * <p>Covers, per the write hot path that Phase 1 touches:</p>
 * <ul>
 *     <li>single-threaded {@code add(x, y, z, from, to)} throughput for both change set types</li>
 *     <li>a contended variant with multiple threads calling {@code add(...)} concurrently on the
 *     <em>same</em> instance, which exercises the lazy stream-init ({@code getBlockOS}) and the
 *     non-atomic {@code blockSize++} counter -- exactly the paths Phase 1's fixes touch</li>
 * </ul>
 *
 * <p>Run via the {@code historyBenchmark} Gradle task added in {@code worldedit-core/build.gradle.kts}:</p>
 * <pre>{@code ./gradlew :worldedit-core:historyBenchmark}</pre>
 */
public final class HistoryWriteBenchmark {

    private static final int WARMUP_OPS = 500_000;
    private static final int MEASURED_OPS = 5_000_000;
    private static final int TRIALS = 5;
    private static final int THREADS = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors()));

    private HistoryWriteBenchmark() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== FAWE history write-path baseline benchmark ===");
        System.out.printf(
                "warmupOps=%d measuredOps=%d trials=%d contendedThreads=%d%n%n",
                WARMUP_OPS, MEASURED_OPS, TRIALS, THREADS
        );

        runSingleThreaded("DiskStorageHistory (single-thread)", HistoryWriteBenchmark::newDiskStorageHistory);
        runSingleThreaded("MemoryOptimizedHistory (single-thread)", HistoryWriteBenchmark::newMemoryOptimizedHistory);
        runContended("DiskStorageHistory (contended, " + THREADS + " threads)", HistoryWriteBenchmark::newDiskStorageHistory);
        runContended(
                "MemoryOptimizedHistory (contended, " + THREADS + " threads)",
                HistoryWriteBenchmark::newMemoryOptimizedHistory
        );

        System.out.println("=== done ===");
    }

    private interface HistoryFactory {

        FaweStreamChangeSet create() throws IOException;

    }

    /**
     * {@link NullWorld} whose {@code getMinY()}/{@code getMaxY()} do not reach into
     * {@code WorldEdit.getInstance()} (which would require a fully bootstrapped platform). Every
     * other call used by the write hot path is already a no-op / dummy in {@link NullWorld}.
     */
    private static final class BenchWorld extends NullWorld {

        @Override
        public int getMinY() {
            return -64;
        }

        @Override
        public int getMaxY() {
            return 319;
        }

    }

    private static World world() {
        return new BenchWorld();
    }

    private static FaweStreamChangeSet newDiskStorageHistory() throws IOException {
        Path dir = Files.createTempDirectory("fawe-history-bench-");
        return new DiskStorageHistory(dir.toFile(), world(), UUID.randomUUID(), 0);
    }

    private static FaweStreamChangeSet newMemoryOptimizedHistory() {
        return new MemoryOptimizedHistory(world());
    }

    private static void doAdd(FaweStreamChangeSet changeSet, int i) {
        // Bitmasks rather than i % 256 / i % 384: 384 is not a power of two, so that modulo
        // compiles to an actual integer division, which would otherwise inflate the measured
        // per-op cost with work that has nothing to do with add() itself. y needs the 384-value
        // range of BenchWorld's height (-64..319), so fold the 9-bit 0..511 mask down to 0..383
        // with a single conditional subtract (still no division), then offset by minY.
        int x = i & 0xFF;
        int rawY = (i >>> 8) & 0x1FF;
        int y = (rawY >= 384 ? rawY - 384 : rawY) - 64;
        int z = (i >>> 17) & 0xFF;
        changeSet.add(x, y, z, 1, 2);
    }

    private static void closeAndCleanup(FaweStreamChangeSet changeSet) {
        try {
            changeSet.close();
        } catch (Exception e) {
            // The contended benchmark deliberately hammers the pre-fix DCL race in getBlockOS(),
            // which can leave the underlying (non-thread-safe) compression stream corrupted.
            // close()/flush() can then throw unchecked exceptions (e.g. AIOOBE from LZ4). That is
            // itself a data point (see the error count printed by report()), not a benchmark bug -
            // swallow it here so remaining trials still run. Only Exception, not Throwable: a
            // real Error (OutOfMemoryError, StackOverflowError) must still propagate and stop the
            // run rather than being treated as expected race noise.
            System.err.println("  (close() threw " + e + ")");
        }
        if (changeSet instanceof DiskStorageHistory dsh) {
            File dir = dsh.getBDFile().getParentFile();
            try {
                SafeFiles.tryHardToDeleteDir(dir.toPath());
            } catch (IOException ignored) {
            }
        }
    }

    private static void runSingleThreaded(String label, HistoryFactory factory) throws Exception {
        long[] elapsedNanos = new long[TRIALS];
        for (int trial = 0; trial < TRIALS; trial++) {
            FaweStreamChangeSet changeSet = factory.create();
            int i = 0;
            for (; i < WARMUP_OPS; i++) {
                doAdd(changeSet, i);
            }
            long start = System.nanoTime();
            for (int j = 0; j < MEASURED_OPS; j++, i++) {
                doAdd(changeSet, i);
            }
            elapsedNanos[trial] = System.nanoTime() - start;
            closeAndCleanup(changeSet);
        }
        report(label, elapsedNanos, MEASURED_OPS, 0);
    }

    private static void runContended(String label, HistoryFactory factory) throws Exception {
        int opsPerThread = MEASURED_OPS / THREADS;
        long[] elapsedNanos = new long[TRIALS];
        long totalErrors = 0;
        for (int trial = 0; trial < TRIALS; trial++) {
            // Warm up JIT/class-loading on a throwaway instance rather than the measured one: the
            // measured instance must still start with uninitialized lazy streams so this trial
            // keeps exercising the lazy stream-init race under contention, which is the entire
            // point of the contended variant.
            FaweStreamChangeSet warmupChangeSet = factory.create();
            for (int i = 0; i < WARMUP_OPS; i++) {
                doAdd(warmupChangeSet, i);
            }
            closeAndCleanup(warmupChangeSet);

            FaweStreamChangeSet changeSet = factory.create();
            ExecutorService pool = Executors.newFixedThreadPool(THREADS);
            CountDownLatch ready = new CountDownLatch(THREADS);
            CountDownLatch go = new CountDownLatch(1);
            AtomicLong errorCount = new AtomicLong();
            for (int t = 0; t < THREADS; t++) {
                final int base = t * opsPerThread;
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        go.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int j = 0; j < opsPerThread; j++) {
                        try {
                            doAdd(changeSet, base + j);
                        } catch (Exception t2) {
                            // Only Exception: a real Error must propagate rather than being
                            // counted as expected race noise (see closeAndCleanup above).
                            errorCount.incrementAndGet();
                        }
                    }
                });
            }
            ready.await();
            long start = System.nanoTime();
            go.countDown();
            pool.shutdown();
            if (!pool.awaitTermination(5, TimeUnit.MINUTES)) {
                pool.shutdownNow();
                throw new IllegalStateException("Benchmark threads did not finish in time");
            }
            elapsedNanos[trial] = System.nanoTime() - start;
            totalErrors += errorCount.get();
            closeAndCleanup(changeSet);
        }
        report(label, elapsedNanos, (long) opsPerThread * THREADS, totalErrors);
    }

    private static void report(String label, long[] elapsedNanos, long opsPerTrial, long totalErrors) {
        System.out.println(label + ":");
        long totalElapsedNanos = 0;
        for (int trial = 0; trial < elapsedNanos.length; trial++) {
            double seconds = elapsedNanos[trial] / 1_000_000_000.0;
            double opsPerSec = opsPerTrial / seconds;
            double nsPerOp = (double) elapsedNanos[trial] / opsPerTrial;
            totalElapsedNanos += elapsedNanos[trial];
            System.out.printf(
                    "  trial %d: %.2f ms, %,.0f ops/sec, %.1f ns/op%n",
                    trial + 1, elapsedNanos[trial] / 1_000_000.0, opsPerSec, nsPerOp
            );
        }
        // Aggregate throughput (total ops / total time) rather than the mean of the per-trial
        // rates, so a single slow/fast trial (GC pause, OS scheduling) is weighted by how long it
        // actually ran instead of counting equally with the others.
        long totalOps = opsPerTrial * elapsedNanos.length;
        double avgOpsPerSec = totalOps / (totalElapsedNanos / 1_000_000_000.0);
        System.out.printf("  average: %,.0f ops/sec (%.1f ns/op)", avgOpsPerSec, 1_000_000_000.0 / avgOpsPerSec);
        if (totalErrors > 0) {
            System.out.printf(" -- %d/%d ops threw (unsynchronized shared stream access)%n", totalErrors, totalOps);
        } else {
            System.out.println();
        }
        System.out.println();
    }

}
