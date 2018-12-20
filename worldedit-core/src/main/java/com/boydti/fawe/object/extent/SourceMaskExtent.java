package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.world.block.BlockStateHolder;


import static com.google.common.base.Preconditions.checkNotNull;

public class SourceMaskExtent extends TemporalExtent {
    private Mask mask;
    private MutableBlockVector mutable = new MutableBlockVector();


    /**
     * Get the mask.
     *
     * @return the mask
     */
    public Mask getMask() {
        return mask;
    }

    /**
     * Set a mask.
     *
     * @param mask a mask
     */
    public void setMask(Mask mask) {
        checkNotNull(mask);
        this.mask = mask;
    }

    public SourceMaskExtent(Extent extent, Mask mask) {
        super(extent);
        checkNotNull(mask);
        this.mask = mask;
    }

    @Override
    public boolean setBlock(Vector location, BlockStateHolder block) throws WorldEditException {
        set(location.getBlockX(), location.getBlockY(), location.getBlockZ(), block);
        return mask.test(location) && super.setBlock(location, block);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        set(x, y, z, block);
        mutable.mutX(x);
        mutable.mutY(y);
        mutable.mutZ(z);
        return mask.test(mutable) && super.setBlock(x, y, z, block);
    }
}
