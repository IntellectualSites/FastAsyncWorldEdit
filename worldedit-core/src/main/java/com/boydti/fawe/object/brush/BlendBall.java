package com.boydti.fawe.object.brush;

import com.google.common.collect.Maps;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.Arrays;
import java.util.Map;

public class BlendBall implements Brush {

    @Override
    public void build(EditSession editSession, Vector position, Pattern pattern, double size) throws MaxChangedBlocksException {
        final int outsetSize = (int) (size + 1);
        double brushSizeSquared = size * size;

        int tx = position.getBlockX();
        int ty = position.getBlockY();
        int tz = position.getBlockZ();

//        Map<BlockState, Integer> frequency = Maps.newHashMap();
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
                    BlockStateHolder currentState = editSession.getBlock(x0, y0, z0);
                    BlockStateHolder highestState = currentState;
                    Arrays.fill(frequency, 0);
                    boolean tie = false;
                    for (int ox = -1; ox <= 1; ox++) {
                        for (int oz = -1; oz <= 1; oz++) {
                            for (int oy = -1; oy <= 1; oy++) {
                                if (oy + y0 < 0 || oy + y0 > maxY) {
                                    continue;
                                }
                                BlockStateHolder state = editSession.getBlock(x0 + ox, y0 + oy, z0 + oz);
                                Integer count = frequency[state.getInternalBlockTypeId()];
                                if (count == null) {
                                    count = 1;
                                } else {
                                    count++;
                                }
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
