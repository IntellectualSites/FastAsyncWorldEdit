package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class PositionTransformExtent extends ResettableExtent {

    private transient MutableBlockVector mutable = new MutableBlockVector();
    private transient Vector min;
    private Transform transform;

    public PositionTransformExtent(Extent parent, Transform transform) {
        super(parent);
        this.transform = transform;
    }

    @Override
    public ResettableExtent setExtent(Extent extent) {
        mutable = new MutableBlockVector();
        min = null;
        return super.setExtent(extent);
    }

    public void setOrigin(Vector pos) {
        this.min = pos;
    }

    private Vector getPos(Vector pos) {
        if (min == null) {
            min = new Vector(pos);
        }
        mutable.mutX(((pos.getX() - min.getX())));
        mutable.mutY(((pos.getY() - min.getY())));
        mutable.mutZ(((pos.getZ() - min.getZ())));
        Vector tmp = transform.apply(mutable);
        tmp.mutX((tmp.getX() + min.getX()));
        tmp.mutY((tmp.getY() + min.getY()));
        tmp.mutZ((tmp.getZ() + min.getZ()));
        return tmp;
    }

    private Vector getPos(int x, int y, int z) {
        if (min == null) {
            min = new Vector(x, y, z);
        }
        mutable.mutX(((x - min.getX())));
        mutable.mutY(((y - min.getY())));
        mutable.mutZ(((z - min.getZ())));
        Vector tmp = transform.apply(mutable);
        tmp.mutX((tmp.getX() + min.getX()));
        tmp.mutY((tmp.getY() + min.getY()));
        tmp.mutZ((tmp.getZ() + min.getZ()));
        return tmp;
    }

    @Override
    public BlockState getLazyBlock(int x, int y, int z) {
        return super.getLazyBlock(getPos(x, y, z));
    }

    @Override
    public BlockState getLazyBlock(Vector position) {
        return super.getLazyBlock(getPos(position));
    }

    @Override
    public BlockState getBlock(Vector position) {
        return super.getBlock(getPos(position));
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        mutable.mutX(position.getBlockX());
        mutable.mutZ(position.getBlockZ());
        mutable.mutY(0);
        return super.getBiome(getPos(mutable).toVector2D());
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        return super.setBlock(getPos(x, y, z), block);
    }


    @Override
    public boolean setBlock(Vector location, BlockStateHolder block) throws WorldEditException {
        return super.setBlock(getPos(location), block);
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        mutable.mutX(position.getBlockX());
        mutable.mutZ(position.getBlockZ());
        mutable.mutY(0);
        return super.setBiome(getPos(mutable).toVector2D(), biome);
    }

    public void setTransform(Transform transform) {
        this.transform = transform;
    }
}
