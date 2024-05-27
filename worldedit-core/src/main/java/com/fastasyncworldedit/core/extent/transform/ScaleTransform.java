package com.fastasyncworldedit.core.extent.transform;

import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.math.MutableVector3;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;
import java.util.UUID;

public class ScaleTransform extends ResettableExtent {

    private final double dx;
    private final double dy;
    private final double dz;
    private transient MutableVector3 mutable = new MutableVector3();
    private transient int minY;
    private transient int maxy;
    private transient BlockVector3 min;

    /**
     * New instance
     *
     * @param parent extent to set to
     * @param dx     x axis scaling
     * @param dy     y axis scaling
     * @param dz     z axis scaling
     */
    public ScaleTransform(Extent parent, double dx, double dy, double dz) {
        super(parent);
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.minY = parent.getMinY();
        this.maxy = parent.getMaxY();
    }

    @Override
    public ResettableExtent setExtent(Extent extent) {
        min = null;
        mutable = new MutableVector3();
        this.minY = extent.getMinY();
        this.maxy = extent.getMaxY();
        return super.setExtent(extent);
    }

    private MutableVector3 getPos(BlockVector3 pos) {
        if (min == null) {
            min = pos;
        }
        mutable.mutX(min.x() + (pos.x() - min.x()) * dx);
        mutable.mutY(min.y() + (pos.y() - min.y()) * dy);
        mutable.mutZ(min.z() + (pos.z() - min.z()) * dz);
        return new MutableVector3(mutable);
    }

    private MutableVector3 getPos(int x, int y, int z) {
        if (min == null) {
            min = BlockVector3.at(x, y, z);
        }
        mutable.mutX(min.x() + (x - min.x()) * dx);
        mutable.mutY(min.y() + (y - min.y()) * dy);
        mutable.mutZ(min.z() + (z - min.z()) * dz);
        return new MutableVector3(mutable);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block)
            throws WorldEditException {
        boolean result = false;
        MutableVector3 vector3 = getPos(location);
        MutableBlockVector3 pos = new MutableBlockVector3();
        double sx = vector3.x();
        double sy = vector3.y();
        double sz = vector3.z();
        double ex = sx + dx;
        double ey = Math.max(minY, Math.min(maxy, sy + dy));
        double ez = sz + dz;
        for (pos.mutY(sy); pos.y() < ey; pos.mutY(pos.y() + 1)) {
            for (pos.mutZ(sz); pos.z() < ez; pos.mutZ(pos.z() + 1)) {
                for (pos.mutX(sx); pos.x() < ex; pos.mutX(pos.x() + 1)) {
                    if (!getExtent().contains(pos)) {
                        continue;
                    }
                    result |= super.setBlock(pos, block);
                }
            }
        }
        return result;
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        boolean result = false;
        MutableVector3 vector3 = getPos(position);
        MutableBlockVector3 pos = new MutableBlockVector3();
        double sx = vector3.x();
        double sy = vector3.y();
        double sz = vector3.z();
        double ex = sx + dx;
        double ey = Math.max(minY, Math.min(maxy, sy + dy));
        double ez = sz + dz;
        for (pos.mutY(sy); pos.y() < ey; pos.mutY(pos.y() + 1)) {
            for (pos.mutZ(sz); pos.z() < ez; pos.mutZ(pos.z() + 1)) {
                for (pos.mutX(sx); pos.x() < ex; pos.mutX(pos.x() + 1)) {
                    if (!getExtent().contains(pos)) {
                        continue;
                    }
                    result |= super.setBiome(pos, biome);
                }
            }
        }
        return result;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x1, int y1, int z1, B block)
            throws WorldEditException {
        boolean result = false;
        MutableVector3 vector3 = getPos(x1, y1, z1);
        MutableBlockVector3 pos = new MutableBlockVector3();
        double sx = vector3.x();
        double sy = vector3.y();
        double sz = vector3.z();
        double ex = vector3.x() + dx;
        double ey = Math.min(maxy, sy + dy);
        double ez = vector3.z() + dz;
        for (pos.mutY(sy); pos.y() < ey; pos.mutY(pos.y() + 1)) {
            for (pos.mutZ(sz); pos.z() < ez; pos.mutZ(pos.z() + 1)) {
                for (pos.mutX(sx); pos.x() < ex; pos.mutX(pos.x() + 1)) {
                    if (!getExtent().contains(pos)) {
                        continue;
                    }
                    result |= super.setBlock(pos, block);
                }
            }
        }
        return result;
    }

    @Override
    public boolean setBiome(int x1, int y1, int z1, BiomeType biome) {
        boolean result = false;
        MutableVector3 vector3 = getPos(x1, y1, z1);
        MutableBlockVector3 pos = new MutableBlockVector3();
        double sx = vector3.x();
        double sy = vector3.y();
        double sz = vector3.z();
        double ex = sx + dx;
        double ey = Math.max(minY, Math.min(maxy, sy + dy));
        double ez = sz + dz;
        for (pos.mutY(sy); pos.y() < ey; pos.mutY(pos.y() + 1)) {
            for (pos.mutZ(sz); pos.z() < ez; pos.mutZ(pos.z() + 1)) {
                for (pos.mutX(sx); pos.x() < ex; pos.mutX(pos.x() + 1)) {
                    if (!getExtent().contains(pos)) {
                        continue;
                    }
                    result |= super.setBiome(pos, biome);
                }
            }
        }
        return result;
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        Location newLoc = new Location(location.getExtent(),
                getPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                location.getYaw(), location.getPitch()
        );
        if (!getExtent().contains(newLoc.toBlockPoint())) {
            return null;
        }
        return super.createEntity(newLoc, entity);
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity, UUID uuid) {
        Location newLoc = new Location(location.getExtent(),
                getPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                location.getYaw(), location.getPitch()
        );
        if (!getExtent().contains(newLoc.toBlockPoint())) {
            return null;
        }
        return super.createEntity(newLoc, entity, uuid);
    }

}
