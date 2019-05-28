package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.math.MutableVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class TransformExtent extends BlockTransformExtent {

    private final MutableVector3 mutable1 = new MutableVector3();
    private final MutableBlockVector3 mutable2 = new MutableBlockVector3();
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
        mutable1.mutX(((pos.getX() - min.getX())));
        mutable1.mutY(((pos.getY() - min.getY())));
        mutable1.mutZ(((pos.getZ() - min.getZ())));
        Vector3 tmp = getTransform().apply(mutable1);
        mutable2.mutX((tmp.getX() + min.getX()));
        mutable2.mutY((tmp.getY() + min.getY()));
        mutable2.mutZ((tmp.getZ() + min.getZ()));
        return mutable2;
    }

    public BlockVector3 getPos(int x, int y, int z) {
        if (min == null) {
            min = BlockVector3.at(x, y, z);
        }
        mutable1.mutX(((x - min.getX())));
        mutable1.mutY(((y - min.getY())));
        mutable1.mutZ(((z - min.getZ())));
        Vector3 tmp = getTransform().apply(mutable1);
        mutable2.mutX((tmp.getX() + min.getX()));
        mutable2.mutY((tmp.getY() + min.getY()));
        mutable2.mutZ((tmp.getZ() + min.getZ()));
        return tmp.toBlockPoint();
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        BlockVector3 p = getPos(x, y, z);
        return transform(super.getBlock(p.getX(), p.getY(), p.getZ()));
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
    	return transform(super.getFullBlock(getPos(position)));
    }

    @Override
    public BiomeType getBiome(int x, int z) {
        BlockVector3 p = getPos(x, 0, z);
        return super.getBiome(p.getX(), p.getZ());
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        return super.setBlock(getPos(x, y, z), transformInverse(block));
    }


    @Override
    public boolean setBlock(BlockVector3 location, BlockStateHolder block) throws WorldEditException {
        return super.setBlock(getPos(location), transformInverse(block));
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        BlockVector3 p = getPos(x, y, z);
        return super.setBiome(p.getX(), p.getY(), p.getZ(), biome);
    }
}
