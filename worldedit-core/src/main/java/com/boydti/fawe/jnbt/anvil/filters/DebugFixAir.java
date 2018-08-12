package com.boydti.fawe.jnbt.anvil.filters;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAFile;
import com.boydti.fawe.jnbt.anvil.MCAFilterCounter;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.number.MutableLong;

// TODO FIXME
public class DebugFixAir extends MCAFilterCounter {
    @Override
    public MCAChunk applyChunk(MCAChunk chunk, MutableLong cache) {
        none:
        {
            some:
            {
                for (int layer = 0; layer < chunk.ids.length; layer++) {
                    byte[] idLayer = chunk.ids[layer];
                    if (idLayer == null) continue;
                    for (int i = 0; i < 4096; i++) {
                        if (idLayer[i] != 0) {
                            if (layer != 0) break some;
                            break none;
                        }
                    }
                    { // Possibly dead code depending on the generator
                        chunk.ids[layer] = null;
                        chunk.data[layer] = null;
                        chunk.setModified();
                    }
                }
                cache.add(Character.MAX_VALUE);
                chunk.setDeleted(true);
                return null;
            }
            return null;
        }

        for (int i = 0; i < 5; i++) {
            if (chunk.ids[i] == null) return null;
        }
//        // layer 0
//        boolean modified = false;
//        byte[] ids0 = chunk.ids[0];
//        for (int i = 0; i < 256; i++) {
//            if (ids0[i] == 0) {
//                if (!modified) {
//                    modified = true;
//                }
//                for (int layer = 0; layer < 4; layer++) {
//                    byte[] arr = chunk.ids[layer];
//                    for (int y = i; y < 4096; y += 256) {
//                        arr[y] = BlockTypes.DIRT;
//                    }
//                }
//                ids0[i] = BlockTypes.BEDROCK;
//                if (chunk.ids[4][i] == 0) chunk.ids[4][i] = BlockTypes.GRASS;
//                cache.add(256);
//            }
//        }
//        if (modified) {
//            Arrays.fill(chunk.skyLight[4], (byte) 255);
//            chunk.setModified();
//        }
        return null;
    }

    @Override
    public void finishFile(MCAFile file, MutableLong cache) {
        Fawe.debug(" - apply " + file.getFile());
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
