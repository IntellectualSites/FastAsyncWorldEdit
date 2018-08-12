package com.boydti.fawe.jnbt.anvil;

import com.boydti.fawe.FaweCache;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import javax.annotation.Nullable;

/**
 * I'm aware this isn't OOP, but object creation is expensive
 */
public class MutableMCABackedBaseBlock extends BaseBlock {

    private MCAChunk chunk;
    private byte[] data;
    private byte[] ids;
    private int index;
    private int x;
    private int y;
    private int z;

    public MutableMCABackedBaseBlock() {
        super(0, null);
    }

    public void setChunk(MCAChunk chunk) {
        this.chunk = chunk;
    }

    public void setArrays(int layer) {
        ids = chunk.ids[layer];
        data = chunk.data[layer];
    }

    public MCAChunk getChunk() {
        return chunk;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    // TODO FIXME update to latest

//    @Override
//    public int getId() {
//        return Byte.toUnsignedInt(ids[index]);
//    }
//
//    @Override
//    public int getData() {
//        if (!FaweCache.hasData(ids[index] & 0xFF)) {
//            return 0;
//        } else {
//            int indexShift = index >> 1;
//            if ((index & 1) == 0) {
//                return data[indexShift] & 15;
//            } else {
//                return (data[indexShift] >> 4) & 15;
//            }
//        }
//    }

    @Nullable
    @Override
    public CompoundTag getNbtData() {
        return chunk.getTile(x, y, z);
    }

//    @Override
//    public void setId(int id) {
//        ids[index] = (byte) id;
//        chunk.setModified();
//    }
//
//    @Override
//    public void setData(int value) {
//        int indexShift = index >> 1;
//        if ((index & 1) == 0) {
//            data[indexShift] = (byte) (data[indexShift] & 240 | value & 15);
//        } else {
//            data[indexShift] = (byte) (data[indexShift] & 15 | (value & 15) << 4);
//        }
//        chunk.setModified();
//    }

    @Override
    public void setNbtData(@Nullable CompoundTag nbtData) {
        chunk.setTile(x, y, z, nbtData);
        chunk.setModified();
    }
}
