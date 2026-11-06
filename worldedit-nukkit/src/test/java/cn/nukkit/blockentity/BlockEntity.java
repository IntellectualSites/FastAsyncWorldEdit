package cn.nukkit.blockentity;

import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.nbt.tag.CompoundTag;

public class BlockEntity {

    public CompoundTag namedTag = new CompoundTag();

    public int getFloorX() {
        return 0;
    }

    public int getFloorY() {
        return 0;
    }

    public int getFloorZ() {
        return 0;
    }

    public void close() {
    }

    public static BlockEntity createBlockEntity(String id, BaseFullChunk chunk, CompoundTag nbt) {
        return new BlockEntity();
    }
}
