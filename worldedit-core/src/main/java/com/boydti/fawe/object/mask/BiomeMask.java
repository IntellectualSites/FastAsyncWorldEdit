package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector2;
import com.sk89q.worldedit.world.biome.BiomeType;

public class BiomeMask extends AbstractExtentMask implements ResettableMask {
    private final BiomeType biome;
    private transient MutableBlockVector2 mutable = new MutableBlockVector2();

    public BiomeMask(Extent extent, BiomeType biome) {
        super(extent);
        this.biome = biome;
    }

    @Override
    public void reset() {
        mutable = new MutableBlockVector2();
    }

    @Override
    public boolean test(BlockVector3 vector) {
        BlockVector2 pos = mutable.setComponents(vector.getBlockX(), vector.getBlockZ());
        return getExtent().getBiome(pos).getId() == biome.getId();
    }
}
