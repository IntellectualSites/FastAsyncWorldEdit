package com.fastasyncworldedit.core.command.tool.brush;

import com.fastasyncworldedit.core.function.mask.CachedMask;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
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

        int[] frequency = new int[BlockTypes.size()];

        int maxY = editSession.getMaxY();
        int minY = editSession.getMinY();

        MutableBlockVector3 mutable = new MutableBlockVector3();
        for (int x = -outsetSize; x <= outsetSize; x++) {
            int x0 = x + tx;
            int xx = x * x;
            for (int y = -outsetSize; y <= outsetSize; y++) {
                int y0 = y + ty;
                if (y0 + 1 < minY || y0 - 1 > maxY) {
                    continue;
                }
                int yy = y * y;
                int xxyy = xx + yy;
                if (xxyy >= brushSizeSquared) {
                    continue;
                }
                for (int z = -outsetSize; z <= outsetSize; z++) {
                    int z0 = z + tz;
                    if (xxyy + z * z >= brushSizeSquared || maskFails(editSession, mutable.setComponents(x0, y0, z0))) {
                        continue;
                    }
                    int highest = 1, currentBlockFrequency = 1;
                    BlockState currentState = editSession.getBlock(x0, y0, z0);
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
                                } else if (oy + y0 < minY || oy + y0 > maxY) {
                                    total--;
                                    continue;
                                }
                                boolean masked = maskFails(editSession, mutable.setComponents(x0 + ox, y0 + oy, z0 + oz));
                                BlockState state = masked ? AIR : editSession.getBlock(x0 + ox, y0 + oy, z0 + oz);
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
                                editSession.setBlock(x0, y0, z0, AIR);
                            }
                        } else if (currentState.isAir() && total - 2 * air >= minFreqDiff) {
                            editSession.setBlock(x0, y0, z0, highestState);
                        }
                        continue;
                    }
                    if (highest - currentBlockFrequency >= minFreqDiff && !tie && currentState != highestState) {
                        editSession.setBlock(x0, y0, z0, highestState);
                    }
                }
            }
        }
    }

    private boolean maskFails(EditSession editSession, MutableBlockVector3 mutable) {
        return mask != null && !mask.test(editSession, mutable);
    }

}
