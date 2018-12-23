package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector2D;
import com.sk89q.worldedit.world.biome.BaseBiome;

public class BiomeMask extends AbstractExtentMask implements ResettableMask {
    private final BaseBiome biome;
    private transient MutableBlockVector2D mutable = new MutableBlockVector2D();

    public BiomeMask(Extent extent, BaseBiome biome) {
        super(extent);
        this.biome = biome;
    }

    @Override
    public void reset() {
        mutable = new MutableBlockVector2D();
    }

    @Override
    public boolean test(BlockVector3 vector) {
    	BlockVector2 pos = mutable.setComponents(vector.getBlockX(), vector.getBlockZ());
        return getExtent().getBiome(pos).getId() == biome.getId();
    }
}
