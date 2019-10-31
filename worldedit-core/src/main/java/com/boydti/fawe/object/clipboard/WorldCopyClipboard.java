package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector2;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorldCopyClipboard extends ReadOnlyClipboard {

    public final int mx, my, mz;
    private final boolean hasBiomes;
    private final boolean hasEntities;
    private MutableBlockVector2 MutableBlockVector2 = new MutableBlockVector2();
    public final Extent extent;

    public WorldCopyClipboard(Extent editSession, Region region) {
        this(editSession, region, true, false);
    }

    public WorldCopyClipboard(Extent editSession, Region region, boolean hasEntities, boolean hasBiomes) {
        super(region);
        this.hasBiomes = hasBiomes;
        this.hasEntities = hasEntities;
        final BlockVector3 origin = region.getMinimumPoint();
        this.mx = origin.getBlockX();
        this.my = origin.getBlockY();
        this.mz = origin.getBlockZ();
        this.extent = editSession;
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return extent.getFullBlock(BlockVector3.at(mx + x, my + y, mz + z));
    }

    public BaseBlock getBlockAbs(int x, int y, int z) {
        return extent.getFullBlock(BlockVector3.at(x, y, z));
    }

    @Override
    public BiomeType getBiomeType(int x, int z) {
        return extent.getBiome(MutableBlockVector2.setComponents(mx + x, mz + z));
    }

    @Override
    public List<? extends Entity> getEntities() {
        if (!hasEntities) return new ArrayList<>();
        return extent.getEntities(getRegion());
    }

    @Override
    public boolean hasBiomes() {
        return hasBiomes;
    }

    @Override
    public void close() throws IOException {

    }
}
