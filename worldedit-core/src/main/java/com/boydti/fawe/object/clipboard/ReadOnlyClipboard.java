package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.jnbt.NBTStreamer;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;
import java.util.List;

public abstract class ReadOnlyClipboard extends SimpleClipboard {
    public final Region region;

    public ReadOnlyClipboard(Region region) {
        super(region.getDimensions());
        this.region = region;
    }

    public static ReadOnlyClipboard of(final Extent editSession, final Region region) {
        return of(editSession, region, true, false);
    }

    public static ReadOnlyClipboard of(final Extent editSession, final Region region, boolean copyEntities, boolean copyBiomes) {
        return new WorldCopyClipboard(editSession, region, copyEntities, copyBiomes);
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public abstract List<? extends Entity> getEntities();

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public void removeEntity(Entity entity) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }
}
