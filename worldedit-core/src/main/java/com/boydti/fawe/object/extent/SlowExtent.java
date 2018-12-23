package com.boydti.fawe.object.extent;

import com.boydti.fawe.Fawe;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class SlowExtent extends AbstractDelegateExtent {
    private final long sleep;

    public SlowExtent(Extent extent, long sleep) {
        super(extent);
        this.sleep = sleep;
    }

    @Override
    public boolean setBlock(BlockVector3 location, BlockStateHolder block) throws WorldEditException {
        if (!Fawe.isMainThread()) try {
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return super.setBlock(location, block);
    }
}
