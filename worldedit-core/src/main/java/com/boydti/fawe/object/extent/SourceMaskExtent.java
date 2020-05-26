package com.boydti.fawe.object.extent;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class SourceMaskExtent extends TemporalExtent {
    private Mask mask;
    private Extent get;
    private MutableBlockVector3 mutable = new MutableBlockVector3();

    public SourceMaskExtent(Extent extent, Mask mask) {
        this(extent, extent, mask);
    }

    public SourceMaskExtent(Extent get, Extent set, Mask mask) {
        super(set);
        checkNotNull(get);
        checkNotNull(mask);
        this.get = get;
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
        set(location.getBlockX(), location.getBlockY(), location.getBlockZ(), block);
        return mask.test(get, location) && super.setBlock(location, block);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) throws WorldEditException {
        set(x, y, z, block);
        mutable.mutX(x);
        mutable.mutY(y);
        mutable.mutZ(z);
        return mask.test(get, mutable) && super.setBlock(x, y, z, block);
    }
}
