package com.boydti.fawe.object.brush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.Arrays;

public class BlendBall implements Brush {

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        final int outsetSize = (int) (size + 1);
        double brushSizeSquared = size * size;

        int tx = position.getBlockX();
        int ty = position.getBlockY();
        int tz = position.getBlockZ();

        int[] frequency = new int[BlockTypes.size()];

        int maxY = editSession.getMaximumPoint().getBlockY();

        for (int x = -outsetSize; x <= outsetSize; x++) {
            int x0 = x + tx;
            for (int y = -outsetSize; y <= outsetSize; y++) {
                int y0 = y + ty;
                for (int z = -outsetSize; z <= outsetSize; z++) {
                    if (x * x + y * y + z * z >= brushSizeSquared) {
                        continue;
                    }
                    int z0 = z + tz;
                    int highest = 1;
                    BlockState currentState = editSession.getBlock(x0, y0, z0);
                    BlockState highestState = currentState;
                    Arrays.fill(frequency, 0);
                    boolean tie = false;
                    for (int ox = -1; ox <= 1; ox++) {
                        for (int oz = -1; oz <= 1; oz++) {
                            for (int oy = -1; oy <= 1; oy++) {
                                if (oy + y0 < 0 || oy + y0 > maxY) {
                                    continue;
                                }
                                BlockState state = editSession.getBlock(x0 + ox, y0 + oy, z0 + oz);
                                int count = frequency[state.getInternalBlockTypeId()];
                                count++;
                                if (count > highest) {
                                    highest = count;
                                    highestState = state;
                                    tie = false;
                                } else if (count == highest) {
                                    tie = true;
                                }
                                frequency[state.getInternalBlockTypeId()] = count;
                            }
                        }
                    }
                    if (!tie && currentState != highestState) {
                        editSession.setBlock(x0, y0, z0, highestState);
                    }
                }
            }
        }
    }


}
