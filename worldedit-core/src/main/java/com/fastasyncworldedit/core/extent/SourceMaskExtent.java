package com.fastasyncworldedit.core.extent;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import static com.google.common.base.Preconditions.checkNotNull;

public class SourceMaskExtent extends TemporalExtent {

    private Mask mask;
    private final MutableBlockVector3 mutable = new MutableBlockVector3();

    public SourceMaskExtent(Extent extent, Mask mask) {
        super(extent);
        checkNotNull(mask);
        this.mask = mask;
    }

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

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
        set(location.x(), location.y(), location.z(), block);
        return mask.test(location) && super.setBlock(location.x(), location.y(), location.z(), block);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) throws WorldEditException {
        set(x, y, z, block);
        mutable.mutX(x);
        mutable.mutY(y);
        mutable.mutZ(z);
        return mask.test(mutable) && super.setBlock(x, y, z, block);
    }

}
