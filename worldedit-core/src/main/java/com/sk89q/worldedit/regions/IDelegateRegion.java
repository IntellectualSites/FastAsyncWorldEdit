package com.sk89q.worldedit.regions;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.world.World;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

public interface IDelegateRegion extends Region {

    public Region getRegion();

    @Override
    default Iterator<BlockVector> iterator() {
        return getRegion().iterator();
    }

    @Override
    default Vector getMinimumPoint() {
        return getRegion().getMinimumPoint();
    }

    @Override
    default Vector getMaximumPoint() {
        return getRegion().getMaximumPoint();
    }

    @Override
    default Vector getCenter() {
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
    default void expand(Vector... changes) throws RegionOperationException {
        getRegion().expand(changes);
    }

    @Override
    default void contract(Vector... changes) throws RegionOperationException {
        getRegion().contract(changes);
    }

    @Override
    default void shift(Vector change) throws RegionOperationException {
        getRegion().shift(change);
    }

    @Override
    default boolean contains(Vector position) {
        return getRegion().contains(position);
    }

    @Override
    default Set<Vector2D> getChunks() {
        return getRegion().getChunks();
    }

    @Override
    default Set<Vector> getChunkCubes() {
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
    default List<BlockVector2D> polygonize(int maxPoints) {
        return getRegion().polygonize(maxPoints);
    }
}
