package com.boydti.fawe.jnbt.anvil.filters;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAFilterCounter;
import com.boydti.fawe.object.number.MutableLong;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

// TODO FIXME
public class CountFilter extends MCAFilterCounter {
//    private final boolean[] allowedId = new boolean[FaweCache.getId(Character.MAX_VALUE)];
//    private final boolean[] allowed = new boolean[Character.MAX_VALUE];

    public CountFilter() {
    }

    public CountFilter addBlock(BaseBlock block) {
//        addBlock(block.getId(), block.getData());
        return this;
    }

    public CountFilter addBlock(int id, int data) {
//        allowedId[id] = true;
//        allowed[FaweCache.getCombined(id, data)] = true;
        return this;
    }

    @Override
    public MCAChunk applyChunk(MCAChunk chunk, MutableLong count) {
//        for (int layer = 0; layer < chunk.ids.length; layer++) {
//            byte[] ids = chunk.ids[layer];
//            if (ids == null) {
//                continue;
//            }
//            byte[] datas = chunk.data[layer];
//            for (int i = 0; i < ids.length; i++) {
//                int id = ids[i] & 0xFF;
//                if (!allowedId[id]) {
//                    continue;
//                }
//                int combined = (id) << 4;
//                if (FaweCache.hasData(id)) {
//                    combined += chunk.getNibble(i, datas);
//                }
//                if (allowed[combined]) {
//                    count.increment();
//                }
//            }
//        }
        return null;
    }
}
