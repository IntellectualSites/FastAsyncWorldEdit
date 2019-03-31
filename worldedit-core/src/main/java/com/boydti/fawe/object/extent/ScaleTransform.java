package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;

public class ScaleTransform extends ResettableExtent {
    private transient MutableBlockVector mutable = new MutableBlockVector();
    private transient int maxy;
    private transient BlockVector3 min;

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

    private BlockVector3 getPos(BlockVector3 pos) {
        if (min == null) {
            min = pos;
        }
        mutable.mutX((min.getX() + (pos.getX() - min.getX()) * dx));
        mutable.mutY((min.getY() + (pos.getY() - min.getY()) * dy));
        mutable.mutZ((min.getZ() + (pos.getZ() - min.getZ()) * dz));
        return mutable.toBlockVector3();
    }

    private BlockVector3 getPos(int x, int y, int z) {
        if (min == null) {
            min = BlockVector3.at(x, y, z);
        }
        mutable.mutX((min.getX() + (x - min.getX()) * dx));
        mutable.mutY((min.getY() + (y - min.getY()) * dy));
        mutable.mutZ((min.getZ() + (z - min.getZ()) * dz));
        return mutable.toBlockVector3();
    }


    @Override
    public boolean setBlock(BlockVector3 location, BlockStateHolder block) throws WorldEditException {
        boolean result = false;
        MutableBlockVector pos = new MutableBlockVector(getPos(location));
        double sx = pos.getX();
        double sy = pos.getY();
        double sz = pos.getZ();
        double ex = sx + dx;
        double ey = Math.min(maxy, sy + dy);
        double ez = sz + dz;
        for (pos.mutY(sy); pos.getY() < ey; pos.mutY(pos.getY() + 1)) {
            for (pos.mutZ(sz); pos.getZ() < ez; pos.mutZ(pos.getZ() + 1)) {
                for (pos.mutX(sx); pos.getX() < ex; pos.mutX(pos.getX() + 1)) {
                    result |= super.setBlock(pos.toBlockVector3(), block);
                }
            }
        }
        return result;
    }

    @Override
    public boolean setBiome(BlockVector2 position, BaseBiome biome) {
        boolean result = false;
        MutableBlockVector pos = new MutableBlockVector(getPos(position.getBlockX(), 0, position.getBlockZ()));
        double sx = pos.getX();
        double sz = pos.getZ();
        double ex = pos.getX() + dx;
        double ez = pos.getZ() + dz;
        for (pos.mutZ(sz); pos.getZ() < ez; pos.mutZ(pos.getZ() + 1)) {
            for (pos.mutX(sx); pos.getX() < ex; pos.mutX(pos.getX() + 1)) {
                result |= super.setBiome(pos.toBlockVector2(), biome);
            }
        }
        return result;
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        Location newLoc = new Location(location.getExtent(), getPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()).toVector3(), location.getYaw(), location.getPitch());
        return super.createEntity(newLoc, entity);
    }

}
