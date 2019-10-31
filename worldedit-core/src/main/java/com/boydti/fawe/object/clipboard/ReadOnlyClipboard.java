package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.jnbt.NBTStreamer;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import java.util.List;

public abstract class ReadOnlyClipboard extends LinearClipboard {
    public final Region region;

    public ReadOnlyClipboard(Region region) {
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
    public BlockVector3 getDimensions() {
        return region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
    }

    @Override
    public void setDimensions(BlockVector3 dimensions) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public BaseBlock getBlock(int index) {
        throw new UnsupportedOperationException("World based clipboards do not provide index access");
    }

    @Override
    public BiomeType getBiome(int index) {
        throw new UnsupportedOperationException("World based clipboards do not provide index access");
    }

    @Override
    public boolean setBiome(int x, int z, BiomeType biome) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public void setBiome(int index, BiomeType biome) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public void streamBiomes(NBTStreamer.ByteReader task) {
        BlockVector3 dim = getDimensions();
        int index = 0;
        for (int z = 0; z <= dim.getBlockZ(); z++) {
            for (int x = 0; x <= dim.getBlockX(); x++, index++) {
                task.run(index, getBiome(x, z).getInternalId());
            }
        }
    }

    @Override
    public abstract BaseBlock getBlock(int x, int y, int z);

    @Override
    public abstract BiomeType getBiome(int x, int z);

    @Override
    public abstract List<? extends Entity> getEntities();

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public boolean setBlock(int index, BlockStateHolder block) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public Entity createEntity(Extent world, double x, double y, double z, float yaw, float pitch, BaseEntity entity) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public boolean remove(ClipboardEntity clipboardEntity) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }
}
