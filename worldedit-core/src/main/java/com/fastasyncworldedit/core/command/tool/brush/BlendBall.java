package com.fastasyncworldedit.core.command.tool.brush;

import com.fastasyncworldedit.core.extent.filter.block.FilterBlock;
import com.fastasyncworldedit.core.function.mask.CachedMask;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.queue.Filter;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nullable;
import java.util.Arrays;

public class BlendBall implements Brush {

    private static final BlockState AIR = BlockTypes.AIR.getDefaultState();

    private final int minFreqDiff;
    private final boolean onlyAir;
    @Nullable private final CachedMask mask;

    /**
     * Create a new {@link Brush} instance with default settings.
     */
    public BlendBall() {
        this(1, false, null);
    }

    /**
     * Create a new {@link Brush} instance.
     *
     * @param minFreqDiff Minimum difference between nearby blocks (3x3x3 cuboid centered on position) to alter the current block
     *                    at a position
     * @param onlyAir     Only consider air for comparing existing blocks, and for altering existing blocks
     * @param mask        Mask to limit the blocks being considered for alteration. Will also limit blocks types able to be
     *                    placed, and will consider blocks not meeting the mask as air
     * @since 2.4.0
     */
    public BlendBall(int minFreqDiff, boolean onlyAir, @Nullable CachedMask mask) {
        this.minFreqDiff = minFreqDiff;
        this.onlyAir = onlyAir;
        this.mask = mask;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws
            MaxChangedBlocksException {
        final int outsetSize = (int) (size + 1);
        double brushSizeSquared = size * size;

        int tx = position.x();
        int ty = position.y();
        int tz = position.z();

        final CuboidRegion region = new CuboidRegion(
                BlockVector3.at(tx - outsetSize, ty - outsetSize, tz - outsetSize),
                BlockVector3.at(tx + outsetSize, ty + outsetSize, tz + outsetSize)
        );

        editSession.apply(region, new BlendBallFilter((int) brushSizeSquared, tx, ty, tz), true);
    }

    private boolean maskFails(Extent extent, BlockVector3 pos) {
        return mask != null && !mask.test(extent, pos);
    }

    private class BlendBallFilter implements Filter {
        private final int[] frequency = new int[BlockTypes.size()];
        private final MutableBlockVector3 mutable = new MutableBlockVector3();
        private final int brushSizeSquared;
        private final int centerX;
        private final int centerY;
        private final int centerZ;

        private BlendBallFilter(final int brushSizeSquared, final int centerX, final int centerY, final int centerZ) {
            this.brushSizeSquared = brushSizeSquared;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
        }

        @Override
        public void applyBlock(final FilterBlock block) {
            final Extent extent = block.getExtent();
            final int gx = block.x();
            final int gy = block.y();
            final int gz = block.z();
            int dx = gx - this.centerX;
            int dy = gy - this.centerY;
            int dz = gz - this.centerZ;
            if (dx * dx + dy * dy + dz * dz >= brushSizeSquared || maskFails(extent, block)) {
                return;
            }
            int maxY = extent.getMaxY();
            int minY = extent.getMinY();
            int highest = 1, currentBlockFrequency = 1;
            BlockState currentState = block.getBlock();
            BlockState highestState = currentState;
            int currentStateID = currentState.getInternalBlockTypeId();
            Arrays.fill(frequency, 0);
            int air = 0;
            int total = 26;
            boolean tie = false;
            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    for (int oy = -1; oy <= 1; oy++) {
                        if (ox == 0 && oy == 0 && oz == 0) {
                            continue;
                        } else if (oy + gy < minY || oy + gy > maxY) {
                            total--;
                            continue;
                        }
                        boolean masked = maskFails(extent, mutable.setComponents(gx + ox, gy + oy, gz + oz));
                        BlockState state = masked ? AIR : block.getBlock(gx + ox, gy + oy, gz + oz);
                        if (state.getBlockType().getMaterial().isAir()) {
                            air++;
                        }
                        int internalID = state.getInternalBlockTypeId();
                        int count = frequency[internalID];
                        if (internalID == currentStateID) {
                            currentBlockFrequency++;
                        }
                        count++;
                        if (count - highest >= minFreqDiff) {
                            highest = count;
                            highestState = state;
                            tie = false;
                        } else if (count == highest) {
                            tie = true;
                        }
                        frequency[internalID] = count;
                    }
                }
            }
            if (onlyAir) {
                if (air * 2 - total >= minFreqDiff) {
                    if (!currentState.isAir()) {
                        block.setBlock(AIR);
                    }
                } else if (currentState.isAir() && total - 2 * air >= minFreqDiff) {
                    block.setBlock(highestState);
                }
                return;
            }
            if (highest - currentBlockFrequency >= minFreqDiff && !tie && currentState != highestState) {
                block.setBlock(highestState);
            }
        }

        @Override
        public Filter fork() {
            return new BlendBallFilter(brushSizeSquared, centerX, centerY, centerZ);
        }

    }

}
