package cn.nukkit.level.format.generic;

import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.level.format.FullChunk;

import java.util.Collections;
import java.util.Map;

public class BaseFullChunk extends FullChunk {

    public int getBiomeId(int x, int z) {
        return 0;
    }

    public int getFullBlock(int x, int y, int z) {
        return 0;
    }

    public int getBlockSkyLight(int x, int y, int z) {
        return 15;
    }

    public int getBlockLight(int x, int y, int z) {
        return 0;
    }

    public BlockEntity getTile(int x, int y, int z) {
        return null;
    }

    public Map<Long, BlockEntity> getBlockEntities() {
        return Collections.emptyMap();
    }

    public void setBiomeId(int x, int z, byte biomeId) {
    }

    public void setChanged(boolean changed) {
    }
}
