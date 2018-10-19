package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector;
import com.sk89q.worldedit.math.MutableVector;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.registry.BlockRegistry;

public class TransformExtent extends BlockTransformExtent {

    private final MutableBlockVector mutable = new MutableBlockVector();
    private BlockVector3 min;
    private int maxy;

    public TransformExtent(Extent parent) {
        super(parent);
        this.maxy = parent.getMaximumPoint().getBlockY();
    }

    @Override
    public ResettableExtent setExtent(Extent extent) {
        min = null;
        maxy = extent.getMaximumPoint().getBlockY();
        return super.setExtent(extent);
    }

    @Override
    public BlockVector3 getMinimumPoint() {
    	BlockVector3 pos1 = getPos(super.getMinimumPoint());
    	BlockVector3 pos2 = getPos(super.getMaximumPoint());
        return pos1.getMinimum(pos2);
    }

    @Override
    public BlockVector3 getMaximumPoint() {
    	BlockVector3 pos1 = getPos(super.getMinimumPoint());
    	BlockVector3 pos2 = getPos(super.getMaximumPoint());
        return pos1.getMaximum(pos2);
    }

    @Override
    public void setOrigin(BlockVector3 pos) {
        this.min = pos;
    }

    public BlockVector3 getPos(BlockVector3 pos) {
        if (min == null) {
            min = pos;
        }
        mutable.mutX(((pos.getX() - min.getX())));
        mutable.mutY(((pos.getY() - min.getY())));
        mutable.mutZ(((pos.getZ() - min.getZ())));
        MutableVector tmp = new MutableVector(getTransform().apply(mutable.toVector3()));
        tmp.mutX((tmp.getX() + min.getX()));
        tmp.mutY((tmp.getY() + min.getY()));
        tmp.mutZ((tmp.getZ() + min.getZ()));
        return tmp.toBlockPoint();
    }

    public BlockVector3 getPos(int x, int y, int z) {
        if (min == null) {
            min = BlockVector3.at(x, y, z);
        }
        mutable.mutX(((x - min.getX())));
        mutable.mutY(((y - min.getY())));
        mutable.mutZ(((z - min.getZ())));
        MutableVector tmp = new MutableVector(getTransform().apply(mutable.toVector3()));
        tmp.mutX((tmp.getX() + min.getX()));
        tmp.mutY((tmp.getY() + min.getY()));
        tmp.mutZ((tmp.getZ() + min.getZ()));
        return tmp.toBlockPoint();
    }

    @Override
    public BlockState getLazyBlock(int x, int y, int z) {
        return transformFast(super.getLazyBlock(getPos(x, y, z)));
    }

    @Override
    public BlockState getLazyBlock(BlockVector3 position) {
        return transformFast(super.getLazyBlock(getPos(position)));
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return transformFast(super.getBlock(getPos(position)));
    }

    @Override
    public BaseBiome getBiome(BlockVector2 position) {
        mutable.mutX(position.getBlockX());
        mutable.mutZ(position.getBlockZ());
        mutable.mutY(0);
        return super.getBiome(getPos(mutable.toBlockVector3()).toBlockVector2());
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        return super.setBlock(getPos(x, y, z), transformFastInverse((BlockState) block));
    }


    @Override
    public boolean setBlock(BlockVector3 location, BlockStateHolder block) throws WorldEditException {
        return super.setBlock(getPos(location), transformFastInverse((BlockState) block));
    }

    @Override
    public boolean setBiome(BlockVector2 position, BaseBiome biome) {
        mutable.mutX(position.getBlockX());
        mutable.mutZ(position.getBlockZ());
        mutable.mutY(0);
        return super.setBiome(getPos(mutable.toBlockVector3()).toBlockVector2(), biome);
    }
}
