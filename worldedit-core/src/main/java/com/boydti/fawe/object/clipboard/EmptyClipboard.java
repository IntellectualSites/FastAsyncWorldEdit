package com.boydti.fawe.object.clipboard;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public class EmptyClipboard implements Clipboard {

    public static final EmptyClipboard INSTANCE = new EmptyClipboard();

    private EmptyClipboard() {
    }

    @Override
    public Region getRegion() {
        return new CuboidRegion(BlockVector3.ZERO, BlockVector3.ZERO);
    }

    @Override
    public BlockVector3 getDimensions() {
        return BlockVector3.ZERO;
    }

    @Override
    public BlockVector3 getOrigin() {
        return BlockVector3.ZERO;
    }

    @Override
    public void setOrigin(BlockVector3 origin) {
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return BlockVector3.ZERO;
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return BlockVector3.ZERO;
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return Collections.emptyList();
    }

    @Override
    public List<? extends Entity> getEntities() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
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

    @Override
    public BaseBiome getBiome(BlockVector2 position) {
        return EditSession.nullBiome;
    }

    @Override
    public boolean setBlock(BlockVector3 position, BlockStateHolder block) throws WorldEditException {
        return false;
    }

    @Override
    public boolean setBiome(BlockVector2 position, BaseBiome biome) {
        return false;
    }

    @Nullable
    @Override
    public Operation commit() {
        return null;
    }
}
