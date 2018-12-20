package com.boydti.fawe.object.clipboard;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
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
        return new CuboidRegion(Vector.ZERO, Vector.ZERO);
    }

    @Override
    public Vector getDimensions() {
        return Vector.ZERO;
    }

    @Override
    public Vector getOrigin() {
        return Vector.ZERO;
    }

    @Override
    public void setOrigin(Vector origin) {
    }

    @Override
    public Vector getMinimumPoint() {
        return Vector.ZERO;
    }

    @Override
    public Vector getMaximumPoint() {
        return Vector.ZERO;
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
    public BlockState getFullBlock(Vector position) {
        return EditSession.nullBlock;
    }

    @Override
    public BlockState getLazyBlock(Vector position) {
        return EditSession.nullBlock;
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        return EditSession.nullBiome;
    }

    @Override
    public boolean setBlock(Vector position, BlockStateHolder block) throws WorldEditException {
        return false;
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        return false;
    }

    @Nullable
    @Override
    public Operation commit() {
        return null;
    }
}
