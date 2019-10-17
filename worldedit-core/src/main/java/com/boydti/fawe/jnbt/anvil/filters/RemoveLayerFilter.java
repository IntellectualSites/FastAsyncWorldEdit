package com.boydti.fawe.jnbt.anvil.filters;

import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAFilterCounter;
import com.boydti.fawe.object.number.MutableLong;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.Arrays;

public class RemoveLayerFilter extends MCAFilterCounter {
    private final int startLayer;
    private final int endLayer;
    private final int minY, maxY;
    private final int id;

    public RemoveLayerFilter(int minY, int maxY, int id) {
        this.minY = minY;
        this.maxY = maxY;
        this.startLayer = minY >> 4;
        this.endLayer = maxY >> 4;
        this.id = id;
    }


    @Override
    public MCAChunk applyChunk(MCAChunk chunk, MutableLong cache) {
        for (int layer = startLayer; layer <= endLayer; layer++) {
            int[] ids = chunk.ids[layer];
            if (ids == null) {
                return null;
            }
            int startY = Math.max(minY, layer << 4) & 15;
            int endY = Math.min(maxY, 15 + (layer << 4)) & 15;
            for (int y = startY; y <= endY; y++) {
                int indexStart = y << 8;
                int indexEnd = indexStart + 255;
                for (int index = indexStart; index <= indexEnd; index++) {
                    if (ids[index] != id) {
                        return null;
                    }
                }
            }
            for (int y = startY; y <= endY; y++) {
                int indexStart = y << 8;
                int indexEnd = indexStart + 255;
                Arrays.fill(ids, indexStart, indexEnd + 1, BlockTypes.AIR.getInternalId());
            }
            chunk.setModified();
        }
        return null;
    }
}
