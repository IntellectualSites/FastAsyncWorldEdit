package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;

public class ScaleTransform extends ResettableExtent {
    private transient MutableBlockVector mutable = new MutableBlockVector();
    private transient int maxy;
    private transient Vector min;

    private final double dx, dy, dz;


    public ScaleTransform(Extent parent, double dx, double dy, double dz) {
        super(parent);
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.maxy = parent.getMaximumPoint().getBlockY();
    }

    @Override
    public ResettableExtent setExtent(Extent extent) {
        min = null;
        maxy = extent.getMaximumPoint().getBlockY();
        mutable = new MutableBlockVector();
        return super.setExtent(extent);
    }

    private Vector getPos(Vector pos) {
        if (min == null) {
            min = new Vector(pos);
        }
        mutable.mutX((min.getX() + (pos.getX() - min.getX()) * dx));
        mutable.mutY((min.getY() + (pos.getY() - min.getY()) * dy));
        mutable.mutZ((min.getZ() + (pos.getZ() - min.getZ()) * dz));
        return mutable;
    }

    private Vector getPos(int x, int y, int z) {
        if (min == null) {
            min = new Vector(x, y, z);
        }
        mutable.mutX((min.getX() + (x - min.getX()) * dx));
        mutable.mutY((min.getY() + (y - min.getY()) * dy));
        mutable.mutZ((min.getZ() + (z - min.getZ()) * dz));
        return mutable;
    }


    @Override
    public boolean setBlock(Vector location, BlockStateHolder block) throws WorldEditException {
        boolean result = false;
        Vector pos = getPos(location);
        double sx = pos.getX();
        double sy = pos.getY();
        double sz = pos.getZ();
        double ex = sx + dx;
        double ey = Math.min(maxy, sy + dy);
        double ez = sz + dz;
        for (pos.mutY(sy); pos.getY() < ey; pos.mutY(pos.getY() + 1)) {
            for (pos.mutZ(sz); pos.getZ() < ez; pos.mutZ(pos.getZ() + 1)) {
                for (pos.mutX(sx); pos.getX() < ex; pos.mutX(pos.getX() + 1)) {
                    result |= super.setBlock(pos, block);
                }
            }
        }
        return result;
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        boolean result = false;
        Vector pos = getPos(position.getBlockX(), 0, position.getBlockZ());
        double sx = pos.getX();
        double sz = pos.getZ();
        double ex = pos.getX() + dx;
        double ez = pos.getZ() + dz;
        for (pos.mutZ(sz); pos.getZ() < ez; pos.mutZ(pos.getZ() + 1)) {
            for (pos.mutX(sx); pos.getX() < ex; pos.mutX(pos.getX() + 1)) {
                result |= super.setBiome(pos.toVector2D(), biome);
            }
        }
        return result;
    }

    @Override
    public boolean setBlock(int x1, int y1, int z1, BlockStateHolder block) throws WorldEditException {
        boolean result = false;
        Vector pos = getPos(x1, y1, z1);
        double sx = pos.getX();
        double sy = pos.getY();
        double sz = pos.getZ();
        double ex = pos.getX() + dx;
        double ey = Math.min(maxy, sy + dy);
        double ez = pos.getZ() + dz;
        for (pos.mutY(sy); pos.getY() < ey; pos.mutY(pos.getY() + 1)) {
            for (pos.mutZ(sz); pos.getZ() < ez; pos.mutZ(pos.getZ() + 1)) {
                for (pos.mutX(sx); pos.getX() < ex; pos.mutX(pos.getX() + 1)) {
                    result |= super.setBlock(pos, block);
                }
            }
        }
        return result;
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        Location newLoc = new Location(location.getExtent(), getPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()), location.getYaw(), location.getPitch());
        return super.createEntity(newLoc, entity);
    }
}
