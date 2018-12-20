package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public class EmptyExtent implements Extent {
    public EmptyExtent() {
    }

    public Vector getMinimumPoint() {
        return Vector.ZERO;
    }

    public Vector getMaximumPoint() {
        return Vector.ZERO;
    }

    public List<Entity> getEntities(Region region) {
        return Collections.emptyList();
    }

    public List<Entity> getEntities() {
        return Collections.emptyList();
    }

    @Nullable
    public Entity createEntity(Location location, BaseEntity entity) {
        return null;
    }

    @Override
    public BlockState getFullBlock(Vector position) {
        return EditSession.nullBlock;
    }

    @Override
    public BlockState getLazyBlock(Vector position) {
        return EditSession.nullBlock;
    }

    @Nullable
    public BaseBiome getBiome(Vector2D position) {
        return null;
    }

    @Override
    public boolean setBlock(Vector position, BlockStateHolder block) throws WorldEditException {
        return false;
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        return false;
    }

    @Override
    public boolean setBiome(int x, int y, int z, BaseBiome biome) {
        return false;
    }

    @Nullable
    public Operation commit() {
        return null;
    }
}
