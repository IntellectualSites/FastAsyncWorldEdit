package com.fastasyncworldedit.core.queue;

import com.fastasyncworldedit.core.queue.implementation.blocks.DataArray;
import com.sk89q.worldedit.extent.Extent;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;

import java.util.stream.Stream;

import static com.sk89q.worldedit.world.block.BlockTypesCache.ReservedIDs.AIR;
import static com.sk89q.worldedit.world.block.BlockTypesCache.ReservedIDs.__RESERVED__;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IBatchProcessorTest {

    @Nested
    @Isolated
    class trimY {

        static {
            // this must happen before DataArray/CharDataArray is initialized
            System.setProperty("fawe.test", "true");
        }

        private static final DataArray CHUNK_DATA = DataArray.createFilled(AIR);
        private final IBatchProcessor processor = new NoopBatchProcessor();

        @AfterAll
        static void tearDown() {
            // remove again
            System.getProperties().remove("fawe.test");
        }

        @ParameterizedTest
        @MethodSource("provideTrimYInBoundsParameters")
        void testFullChunkSelectedInBoundedRegion(int minY, int maxY, int minSection, int maxSection) {
            final IChunkSet set = mock();

            DataArray[] sections = new DataArray[(320 + 64) >> 4];
            for (int i = 0; i < sections.length; i++) {
                sections[i] = DataArray.createCopy(CHUNK_DATA);
            }

            when(set.getMinSectionPosition()).thenReturn(-64 >> 4);
            when(set.getMaxSectionPosition()).thenReturn(319 >> 4);
            when(set.hasSection(anyInt())).thenReturn(true);
            when(set.loadIfPresent(anyInt())).thenAnswer(invocationOnMock -> sections[invocationOnMock.<Integer>getArgument(0) + 4]);
            doAnswer(invocationOnMock -> {
                sections[invocationOnMock.<Integer>getArgument(0) + 4] = invocationOnMock.getArgument(1);
                return null;
            }).when(set).setBlocks(anyInt(), any());

            processor.trimY(set, minY, maxY, true);


            for (int section = -64 >> 4; section < 320 >> 4; section++) {
                int sectionIndex = section + 4;
                DataArray palette = sections[sectionIndex];
                if (section < minSection) {
                    assertNull(palette, "expected section below minimum section to be null");
                    continue;
                }
                if (section > maxSection) {
                    assertNull(palette, "expected section above maximum section to be null");
                    continue;
                }
                if (section == minSection) {
                    for (int slice = 0; slice < 16; slice++) {
                        boolean shouldContainBlocks = slice >= (minY % 16);
                        // If boundaries only span one section, the upper constraints have to be checked explicitly
                        if (section == maxSection) {
                            shouldContainBlocks &= slice <= (maxY % 16);
                        }
                        try {
                            assertSliceMatches(palette, slice << 8, (slice + 1) << 8, shouldContainBlocks ? AIR : __RESERVED__);
                        } catch (AssertionFailedError error) {
                            fail("[lower] slice %d (y=%d) expected to contain " + (shouldContainBlocks ? "air" : "nothing"), error);
                        }
                    }
                    continue;
                }
                if (section == maxSection) {
                    for (int slice = 0; slice < 16; slice++) {
                        boolean shouldContainBlocks = slice <= (maxY % 16);
                        try {
                            assertSliceMatches(palette, slice << 8, (slice + 1) << 8, shouldContainBlocks ? AIR : __RESERVED__);
                        } catch (AssertionFailedError error) {
                            fail("[upper] slice %d (y=%d) expected to contain " + (shouldContainBlocks ? "air" : "nothing"), error);
                        }
                    }
                    continue;
                }
                assertEquals(CHUNK_DATA, palette, "full captured chunk @ %d should contain full data".formatted(section));
            }

        }

        /**
         * Arguments explained:
         * 1. minimum y coordinate (inclusive)
         * 2. maximum y coordinate (inclusive)
         * 3. chunk section which contains minimum y coordinate
         * 4. chunk section which contains maximum y coordinate
         */
        private static Stream<Arguments> provideTrimYInBoundsParameters() {
            return Stream.of(
                    Arguments.of(64, 72, 4, 4),
                    Arguments.of(-64, 0, -4, 0),
                    Arguments.of(0, 128, 0, 8),
                    Arguments.of(16, 132, 1, 8),
                    Arguments.of(4, 144, 0, 9),
                    Arguments.of(12, 255, 0, 15),
                    Arguments.of(24, 103, 1, 6)
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

    private static void assertSliceMatches(DataArray dataArray, int sliceStart, int sliceEnd, int expectedValue) {
        for (int i = sliceStart; i < sliceEnd; i++) {
            assertEquals(expectedValue, dataArray.getAt(i), "mismatch at index " + i);
        }
    }

}
