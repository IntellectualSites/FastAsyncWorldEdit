package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;

public abstract class SelectTransform extends ResettableExtent {
    public SelectTransform() {
        super(new NullExtent());
    }

    public abstract AbstractDelegateExtent getExtent(int x, int y, int z);

    public abstract AbstractDelegateExtent getExtent(int x, int z);

    public Extent getExtent(Vector pos) {
        return getExtent(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
    }

    public Extent getExtent(Vector2D pos) {
        return getExtent(pos.getBlockX(), pos.getBlockZ());
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        return getExtent(x, y, z).setBlock(x, y, z, block);
    }

    @Override
    public boolean setBlock(Vector position, BlockStateHolder block) throws WorldEditException {
        return getExtent(position).setBlock(position, block);
    }

    @Nullable
    @Override
    public Entity createEntity(Location position, BaseEntity entity) {
        return getExtent(position.getBlockX(), position.getBlockY(), position.getBlockZ()).createEntity(position, entity);
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        return getExtent(position).setBiome(position, biome);
    }
}
