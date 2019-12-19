package com.boydti.fawe.object.clipboard;

import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class WorldCopyClipboard extends ReadOnlyClipboard {

    private final boolean hasBiomes;
    private final boolean hasEntities;
    private Extent extent;
    private Supplier<Extent> supplier;

    public WorldCopyClipboard(Supplier<Extent> supplier, Region region) {
        this(supplier, region, true, false);
    }

    public WorldCopyClipboard(Supplier<Extent> supplier, Region region, boolean hasEntities, boolean hasBiomes) {
        super(region);
        this.hasBiomes = hasBiomes;
        this.hasEntities = hasEntities;
        this.supplier = supplier;
    }

    public Extent getExtent() {
        if (extent != null) {
            return extent;
        }
        extent = supplier.get();
        supplier = null;
        return extent;
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return getExtent().getFullBlock(x, y, z);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return getExtent().getBlock(x, y, z);
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        return getExtent().getBiomeType(x, y, z);
    }

    @Override
    public List<? extends Entity> getEntities() {
        if (!hasEntities) return new ArrayList<>();
        return getExtent().getEntities(getRegion());
    }

    @Override
    public boolean hasBiomes() {
        return hasBiomes;
    }

    @Override
    public void close() {

    }
}
