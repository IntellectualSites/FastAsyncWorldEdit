package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
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

    public BlockVector3 getMinimumPoint() {
        return BlockVector3.ZERO;
    }

    public BlockVector3 getMaximumPoint() {
        return BlockVector3.ZERO;
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
    public BlockState getFullBlock(BlockVector3 position) {
        return EditSession.nullBlock;
    }

    @Override
    public BlockState getLazyBlock(BlockVector3 position) {
        return EditSession.nullBlock;
    }

    @Nullable
    public BaseBiome getBiome(BlockVector2 position) {
        return null;
    }

    @Override
    public boolean setBlock(BlockVector3 position, BlockStateHolder block) throws WorldEditException {
        return false;
    }

    @Override
    public boolean setBiome(BlockVector2 position, BaseBiome biome) {
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
