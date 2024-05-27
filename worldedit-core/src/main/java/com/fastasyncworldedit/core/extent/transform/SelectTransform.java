package com.fastasyncworldedit.core.extent.transform;

import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;
import java.util.UUID;

public abstract class SelectTransform extends ResettableExtent {

    public SelectTransform() {
        super(new NullExtent());
    }

    public abstract AbstractDelegateExtent getExtent(int x, int y, int z);

    public abstract AbstractDelegateExtent getExtent(int x, int z);

    public Extent getExtent(BlockVector3 pos) {
        return getExtent(pos.x(), pos.y(), pos.z());
    }

    public Extent getExtent(BlockVector2 pos) {
        return getExtent(pos.x(), pos.z());
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block)
            throws WorldEditException {
        return getExtent(x, y, z).setBlock(x, y, z, block);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block)
            throws WorldEditException {
        return getExtent(position).setBlock(position, block);
    }

    @Nullable
    @Override
    public Entity createEntity(Location position, BaseEntity entity) {
        return getExtent(position.getBlockX(), position.getBlockY(), position.getBlockZ())
                .createEntity(position, entity);
    }

    @Nullable
    @Override
    public Entity createEntity(Location position, BaseEntity entity, UUID uuid) {
        return getExtent(position.getBlockX(), position.getBlockY(), position.getBlockZ())
                .createEntity(position, entity, uuid);
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        return getExtent(position).setBiome(position, biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return getExtent(x, y, z).setBiome(x, y, z, biome);
    }

}
