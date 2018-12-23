package com.sk89q.worldedit.regions;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.world.World;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

public interface IDelegateRegion extends Region {

    public Region getRegion();

    @Override
    default Iterator<BlockVector3> iterator() {
        return getRegion().iterator();
    }

    @Override
    default BlockVector3 getMinimumPoint() {
        return getRegion().getMinimumPoint();
    }

    @Override
    default BlockVector3 getMaximumPoint() {
        return getRegion().getMaximumPoint();
    }

    @Override
    default Vector3 getCenter() {
        return getRegion().getCenter();
    }

    @Override
    default int getArea() {
        return getRegion().getArea();
    }

    @Override
    default int getWidth() {
        return getRegion().getWidth();
    }

    @Override
    default int getHeight() {
        return getRegion().getHeight();
    }

    @Override
    default int getLength() {
        return getRegion().getLength();
    }

    @Override
    default void expand(BlockVector3... changes) throws RegionOperationException {
        getRegion().expand(changes);
    }

    @Override
    default void contract(BlockVector3... changes) throws RegionOperationException {
        getRegion().contract(changes);
    }

    @Override
    default void shift(BlockVector3 change) throws RegionOperationException {
        getRegion().shift(change);
    }

    @Override
    default boolean contains(BlockVector3 position) {
        return getRegion().contains(position);
    }

    @Override
    default Set<BlockVector2> getChunks() {
        return getRegion().getChunks();
    }

    @Override
    default Set<BlockVector3> getChunkCubes() {
        return getRegion().getChunkCubes();
    }

    @Override
    @Nullable
    default World getWorld() {
        return getRegion().getWorld();
    }

    @Override
    default void setWorld(@Nullable World world) {
        getRegion().setWorld(world);
    }

    @Override
    default Region clone() {
        return getRegion().clone();
    }

    @Override
    default List<BlockVector2> polygonize(int maxPoints) {
        return getRegion().polygonize(maxPoints);
    }
}
