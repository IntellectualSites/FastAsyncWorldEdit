package com.fastasyncworldedit.core.queue;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IBatchProcessorTest {

    @Nested
    @Isolated
    class trimY {

        private static final char[] CHUNK_DATA = new char[16 * 16 * 16];
        private final IBatchProcessor processor = new NoopBatchProcessor();

        static {
            Arrays.fill(CHUNK_DATA, (char) BlockTypesCache.ReservedIDs.AIR);
        }

        @ParameterizedTest
        @MethodSource("provideTrimYInBoundsParameters")
        void testFullChunkSelectedInBoundedRegion(int minY, int maxY) {
            final int minYSection = minY >> 4;
            final int maxYSection = maxY >> 4;
            final IChunkSet set = mock();

            when(set.getMinSectionPosition()).thenReturn(-64 >> 4);
            when(set.getMaxSectionPosition()).thenReturn(320 >> 4);
            when(set.hasSection(anyInt())).thenReturn(true);
            when(set.loadIfPresent(anyInt())).thenReturn(CHUNK_DATA); // Return fully populated chunk (AIR)
            doAnswer(invocationOnMock -> {
                int layer = invocationOnMock.getArgument(0);
                char[] blocks = invocationOnMock.getArgument(1);
                // when totally out of range (layer mismatch) no blocks should be set
                if (layer < minYSection || layer > maxYSection) {
                    if (blocks != null) {
                        fail("Expected null-array for out of range access at layer %d where minYLayer=%d and maxYLayer=%d"
                                .formatted(layer, minYSection, maxYSection)
                        );
                    }
                    return null;
                }
                assertNotNull(blocks, "expected non-null palette for in bound chunk section");
                assertEquals(blocks.length, CHUNK_DATA.length, "chunk section palette size diffs from returned get palette");

                // when working on the lowest layer, only blocks above minY should be set - otherwise __RESERVED__
                if (layer == minYSection) {
                    char[] expected = Arrays.copyOf(CHUNK_DATA, CHUNK_DATA.length);
                    Arrays.fill(expected, 0, (minY & 15) << 8, (char) BlockTypesCache.ReservedIDs.__RESERVED__);
                    assertArrayEquals(
                            expected, blocks,
                            "expected in-range blocks at layer=%d to be AIR - out-of-range __RESERVED__"
                                    .formatted(layer)
                    );
                    return null;
                }

                // kinda the same for the highest layer - just the other way around
                if (layer == maxYSection) {
                    char[] expected = Arrays.copyOf(CHUNK_DATA, CHUNK_DATA.length);
                    Arrays.fill(expected, ((maxY + 1) & 15) << 8, expected.length, (char) BlockTypesCache.ReservedIDs.__RESERVED__);
                    assertArrayEquals(
                            expected, blocks,
                            "expected in-range blocks at layer=%d to be AIR - out-of-range __RESERVED__"
                                    .formatted(layer)
                    );
                    return null;
                }
                assertArrayEquals(blocks, CHUNK_DATA, "full chunk should contain full data");
                return null;
            }).when(set).setBlocks(anyInt(), any());

            processor.trimY(set, minY, maxY, true);
        }

        private static Stream<Arguments> provideTrimYInBoundsParameters() {
            return Stream.of(
                    Arguments.of(64, 72),
                    Arguments.of(-64, 0),
                    Arguments.of(0, 128),
                    Arguments.of(16, 132),
                    Arguments.of(4, 144),
                    Arguments.of(12, 255),
                    Arguments.of(24, 103)
            );
        }

    }

    private static final class NoopBatchProcessor implements IBatchProcessor {

        @Override
        public IChunkSet processSet(final IChunk chunk, final IChunkGet get, final IChunkSet set) {
            return set;
        }

        @Override
        public @Nullable Extent construct(final Extent child) {
            return null;
        }

    }

}
