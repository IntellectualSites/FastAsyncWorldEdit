package com.boydti.fawe.jnbt.anvil.filters;

import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAFile;
import com.boydti.fawe.jnbt.anvil.MCAFilterCounter;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.number.MutableLong;
import com.sk89q.worldedit.world.block.BlockTypes;

public class TrimAirFilter extends MCAFilterCounter {
    @Override
    public MCAChunk applyChunk(MCAChunk chunk, MutableLong cache) {
        for (int layer = 0; layer < chunk.ids.length; layer++) {
            int[] idLayer = chunk.ids[layer];
            if (idLayer == null) continue;
            for (int i = 0; i < 4096; i++) {
                if (!BlockTypes.getFromStateId(idLayer[i]).getMaterial().isAir()) {
                    return null;
                }
            }
            { // Possibly dead code depending on the generator
                chunk.ids[layer] = null;
                chunk.setModified();
            }
        }
        cache.add(Character.MAX_VALUE);
        chunk.setDeleted(true);
        return null;
    }

    @Override
    public void finishFile(MCAFile file, MutableLong cache) {
        boolean[] deleteFile = { true };
        file.forEachCachedChunk(new RunnableVal<MCAChunk>() {
            @Override
            public void run(MCAChunk value) {
                if (!value.isDeleted()) {
                    deleteFile[0] = false;
                }
            }
        });
        if (deleteFile[0]) {
            file.setDeleted(true);
        }
    }
}
