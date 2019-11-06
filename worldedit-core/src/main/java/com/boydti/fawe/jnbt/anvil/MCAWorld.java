package com.boydti.fawe.jnbt.anvil;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;

public class MCAWorld implements Extent {
    @Override
    public BlockVector3 getMinimumPoint() {
        return null;
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return null;
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        return false;
    }
}
