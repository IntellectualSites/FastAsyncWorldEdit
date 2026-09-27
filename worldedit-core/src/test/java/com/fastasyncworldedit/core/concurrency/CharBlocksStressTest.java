package com.fastasyncworldedit.core.concurrency;

import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.implementation.blocks.CharBlocks;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Stress tests for the per-section locking in {@link CharBlocks}. Sections are lazily allocated under
 * {@code sectionLocks[layer]}, and chunk data is read and written from several threads at once (e.g. the main thread
 * and FAWE's parallel queue workers), so lazy allocation must never discard data written by another thread.
 */
@Tag("stress")
@Isolated
class CharBlocksStressTest {

    private static final int SECTION_SIZE = 4096;
    private static final char DEFAULT = 1;

    /**
     * Many threads write disjoint indices of the same, not-yet-allocated section. Every write must survive, regardless
     * of which thread ends up allocating the section.
     */
    @Test
    void concurrentWritesToUnallocatedSectionAreNotLost() {
        final int threads = StressHarness.THREADS;
        final int rounds = StressHarness.scaled(500);
        final TestBlocks[] holder = new TestBlocks[1];
        // Barrier action runs once per round, before any thread touches the new instance
        final CyclicBarrier barrier = new CyclicBarrier(threads, () -> holder[0] = new TestBlocks());
        final int[] lost = new int[rounds];

        StressHarness.run("section-alloc-writes", threads, t -> {
            for (int round = 0; round < rounds; round++) {
                barrier.await(StressHarness.TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                TestBlocks blocks = holder[0];
                char value = (char) (t + 2);
                for (int index = t; index < SECTION_SIZE; index += threads) {
                    blocks.set(0, index, value);
                }
                barrier.await(StressHarness.TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                if (t == 0) {
                    lost[round] = countMismatches(blocks, threads);
                }
            }
        });

        int totalLost = 0;
        int badRounds = 0;
        for (int l : lost) {
            totalLost += l;
            badRounds += l > 0 ? 1 : 0;
        }
        assertEquals(0, totalLost, "writes lost in " + badRounds + "/" + rounds + " rounds (lazy section allocation "
                + "overwrote data written by another thread)");
    }

    /**
     * A reader must never observe a block reverting to the default value once a writer has set it and published that
     * fact, even if the reader races the lazy allocation of the section.
     */
    @Test
    void readersNeverObserveRevertedWrites() {
        final int threads = StressHarness.THREADS;
        final int rounds = StressHarness.scaled(500);
        final TestBlocks[] holder = new TestBlocks[1];
        final CyclicBarrier barrier = new CyclicBarrier(threads, () -> holder[0] = new TestBlocks());

        StressHarness.run("section-alloc-read", threads, t -> {
            for (int round = 0; round < rounds; round++) {
                barrier.await(StressHarness.TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                TestBlocks blocks = holder[0];
                if (t == 0) {
                    // Writer: set every block, a single index at a time
                    for (int index = 0; index < SECTION_SIZE; index++) {
                        blocks.set(0, index, (char) 7);
                    }
                    blocks.written = true;
                } else {
                    // Readers: hammer the section (forcing lazy allocation if still null)
                    int index = t;
                    while (!blocks.written) {
                        blocks.get(0, index);
                        index = (index + 31) & (SECTION_SIZE - 1);
                    }
                }
                barrier.await(StressHarness.TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                for (int index = 0; index < SECTION_SIZE; index++) {
                    char actual = blocks.get(0, index);
                    if (actual != 7) {
                        throw new AssertionError("round " + round + ": index " + index + " reverted to " + (int) actual
                                + " after being written");
                    }
                }
            }
        });
    }

    /**
     * Mixed set/get/reset/trim traffic must not throw (e.g. NPE or AIOOBE from a section being nulled mid-access).
     */
    @Test
    void mixedTrafficDoesNotThrow() {
        final TestBlocks blocks = new TestBlocks();
        final int iterations = StressHarness.scaled(200_000);
        StressHarness.run("section-mixed", StressHarness.THREADS, t -> {
            SplittableRandom random = new SplittableRandom(t);
            for (int i = 0; i < iterations; i++) {
                int layer = random.nextInt(16);
                int x = random.nextInt(16);
                int y = (layer << 4) | random.nextInt(16);
                int z = random.nextInt(16);
                switch (random.nextInt(100)) {
                    case 0 -> blocks.reset(layer);
                    case 1 -> blocks.trim(false, layer);
                    case 2 -> blocks.trim(false);
                    case 3 -> blocks.load(layer);
                    default -> {
                        if (random.nextBoolean()) {
                            blocks.set(x, y, z, (char) (2 + random.nextInt(100)));
                        } else {
                            blocks.get(x, y, z);
                        }
                    }
                }
            }
        });
    }

    private static int countMismatches(TestBlocks blocks, int threads) {
        int mismatches = 0;
        for (int index = 0; index < SECTION_SIZE; index++) {
            if (blocks.get(0, index) != (char) ((index % threads) + 2)) {
                mismatches++;
            }
        }
        return mismatches;
    }

    private static final class TestBlocks extends CharBlocks {

        volatile boolean written;

        TestBlocks() {
            super(0, 15);
        }

        @Override
        protected char defaultOrdinal() {
            return DEFAULT;
        }

        @Override
        public void removeSectionLighting(int layer, boolean sky) {
        }

        @Override
        public Map<BlockVector3, FaweCompoundTag> tiles() {
            return Map.of();
        }

        @Override
        public FaweCompoundTag tile(int x, int y, int z) {
            return null;
        }

        @Override
        public Collection<FaweCompoundTag> entities() {
            return List.of();
        }

        @Override
        public BiomeType getBiomeType(int x, int y, int z) {
            return null;
        }

    }

}
