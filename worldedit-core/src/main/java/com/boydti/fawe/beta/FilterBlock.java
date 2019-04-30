package com.boydti.fawe.beta;

import com.boydti.fawe.beta.implementation.blocks.CharGetBlocks;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

public interface FilterBlock {
    void init(IQueueExtent queue);

    void init(int X, int Z, IGetBlocks chunk);

    int getOrdinal();

    BlockState getState();

    BaseBlock getBaseBlock();

    CompoundTag getTag();

    default BlockState getOrdinalBelow() {
        return getStateRelative(0, -1, 0);
    }

    default BlockState getStateAbove() {
        return getStateRelative(0, 1, 0);
    }

    default BlockState getStateRelativeY(int y) {
        return getStateRelative(0, y, 0);
    }

    int getX();

    int getY();

    int getZ();

    default int getLocalX() {
        return getX() & 15;
    }

    default int getLocalY() {
        return getY() & 15;
    }

    default int getLocalZ() {
        return getZ() & 15;
    }

    default int getChunkX() {
        return getX() >> 4;
    }

    default int getChunkZ() {
        return getZ() >> 4;
    }

    BlockState getStateRelative(final int x, final int y, final int z);
}
