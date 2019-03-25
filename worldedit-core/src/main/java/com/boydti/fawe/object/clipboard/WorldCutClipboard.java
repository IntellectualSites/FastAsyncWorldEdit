package com.boydti.fawe.object.clipboard;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.regions.Region;

public class WorldCutClipboard extends WorldCopyClipboard {
    public WorldCutClipboard(EditSession editSession, Region region, boolean copyEntities, boolean copyBiome) {
        super(editSession, region, copyEntities, copyBiome);
    }

    public WorldCutClipboard(EditSession editSession, Region region) {
        super(editSession, region);
    }

    @Override
    public BaseBlock getBlock(int x, int y, int z) {
        int xx = mx + x;
        int yy = my + y;
        int zz = mz + z;
        BaseBlock block = extent.getFullBlock(BlockVector3.at(xx, yy, zz));
        extent.setBlock(xx, yy, zz, EditSession.nullBlock);
        return block;
    }

    public BaseBlock getBlockAbs(int x, int y, int z) {
        BaseBlock block = extent.getFullBlock(BlockVector3.at(x, y, z));
        extent.setBlock(x, y, z, EditSession.nullBlock);
        return block;
    }

    @Override
    public void forEach(BlockReader task, boolean air) {
        super.forEach(task, air);
        if (extent instanceof EditSession) {
            ((EditSession) extent).flushQueue();
        } else {
            extent.commit();
        }
    }
}