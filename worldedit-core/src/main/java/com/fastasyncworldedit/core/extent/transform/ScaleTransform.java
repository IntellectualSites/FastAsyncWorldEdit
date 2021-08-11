package com.fastasyncworldedit.core.extent.transform;

import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;

public class ScaleTransform extends ResettableExtent {

    private final double dx;
    private final double dy;
    private final double dz;
    private transient MutableBlockVector3 mutable = new MutableBlockVector3();
    private transient int maxy;
    private transient BlockVector3 min;


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
        mutable = new MutableBlockVector3();
        return super.setExtent(extent);
    }

    private void getPos(BlockVector3 pos) {
        if (min == null) {
            min = pos;
        }
        mutable.mutX(min.getX() + (pos.getX() - min.getX()) * dx);
        mutable.mutY(min.getY() + (pos.getY() - min.getY()) * dy);
        mutable.mutZ(min.getZ() + (pos.getZ() - min.getZ()) * dz);
    }

    private void getPos(int x, int y, int z) {
        if (min == null) {
            min = BlockVector3.at(x, y, z);
        }
        mutable.mutX(min.getX() + (x - min.getX()) * dx);
        mutable.mutY(min.getY() + (y - min.getY()) * dy);
        mutable.mutZ(min.getZ() + (z - min.getZ()) * dz);
    }


    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block)
            throws WorldEditException {
        boolean result = false;
        getPos(location);
        double sx = mutable.getX();
        double sy = mutable.getY();
        double sz = mutable.getZ();
        double ex = sx + dx;
        double ey = Math.min(maxy, sy + dy);
        double ez = sz + dz;
        for (mutable.mutY(sy); mutable.getY() < ey; mutable.mutY(mutable.getY() + 1)) {
            for (mutable.mutZ(sz); mutable.getZ() < ez; mutable.mutZ(mutable.getZ() + 1)) {
                for (mutable.mutX(sx); mutable.getX() < ex; mutable.mutX(mutable.getX() + 1)) {
                    result |= super.setBlock(mutable, block);
                }
            }
        }
        return result;
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        boolean result = false;
        getPos(position);
        double sx = mutable.getX();
        double sy = mutable.getY();
        double sz = mutable.getZ();
        double ex = sx + dx;
        double ey = Math.min(maxy, sy + dy);
        double ez = sz + dz;
        for (mutable.mutY(sy); mutable.getY() < ey; mutable.mutY(mutable.getY() + 1)) {
            for (mutable.mutZ(sz); mutable.getZ() < ez; mutable.mutZ(mutable.getZ() + 1)) {
                for (mutable.mutX(sx); mutable.getX() < ex; mutable.mutX(mutable.getX() + 1)) {
                    result |= super.setBiome(mutable, biome);
                }
            }
        }
        return result;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x1, int y1, int z1, B block)
            throws WorldEditException {
        boolean result = false;
        getPos(x1, y1, z1);
        double sx = mutable.getX();
        double sy = mutable.getY();
        double sz = mutable.getZ();
        double ex = mutable.getX() + dx;
        double ey = Math.min(maxy, sy + dy);
        double ez = mutable.getZ() + dz;
        for (mutable.mutY(sy); mutable.getY() < ey; mutable.mutY(mutable.getY() + 1)) {
            for (mutable.mutZ(sz); mutable.getZ() < ez; mutable.mutZ(mutable.getZ() + 1)) {
                for (mutable.mutX(sx); mutable.getX() < ex; mutable.mutX(mutable.getX() + 1)) {
                    result |= super.setBlock(mutable, block);
                }
            }
        }
        return result;
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        getPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        Location newLoc = new Location(location.getExtent(),
                mutable.toVector3(),
                location.getYaw(), location.getPitch()
        );
        return super.createEntity(newLoc, entity);
    }

}
