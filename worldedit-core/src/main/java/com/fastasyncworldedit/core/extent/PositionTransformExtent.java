package com.fastasyncworldedit.core.extent;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.math.MutableVector3;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class PositionTransformExtent extends ResettableExtent {

    private transient MutableBlockVector3 mutable = new MutableBlockVector3();
    private transient BlockVector3 min;
    private Transform transform;

    public PositionTransformExtent(Extent parent, Transform transform) {
        super(parent);
        this.transform = transform;
    }

    @Override
    public ResettableExtent setExtent(Extent extent) {
        mutable = new MutableBlockVector3();
        min = null;
        return super.setExtent(extent);
    }

    @Override
    public void setOrigin(BlockVector3 pos) {
        this.min = pos;
    }

    private BlockVector3 getPos(BlockVector3 pos) {
        if (min == null) {
            min = pos;
        }
        mutable.mutX(pos.x() - min.x());
        mutable.mutY(pos.y() - min.y());
        mutable.mutZ(pos.z() - min.z());
        MutableVector3 tmp = new MutableVector3(transform.apply(mutable.toVector3()));
        return min.add(tmp.roundHalfUp().toBlockPoint());
    }

    private BlockVector3 getPos(int x, int y, int z) {
        if (min == null) {
            min = BlockVector3.at(x, y, z);
        }
        mutable.mutX(x - min.x());
        mutable.mutY(y - min.y());
        mutable.mutZ(z - min.z());
        MutableVector3 tmp = new MutableVector3(transform.apply(mutable.toVector3()));
        return min.add(tmp.roundHalfUp().toBlockPoint());
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return super.getBlock(getPos(x, y, z));
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return super.getBlock(getPos(position));
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return super.getFullBlock(getPos(position));
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        mutable.mutX(position.x());
        mutable.mutZ(position.z());
        mutable.mutY(position.y());
        return super.getBiome(getPos(mutable.x(), mutable.y(), mutable.z()));
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) throws WorldEditException {
        return this.setBlock(getPos(x, y, z), block);
    }


    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block) throws WorldEditException {
        return super.setBlock(getPos(location), block);
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        mutable.mutX(position.x());
        mutable.mutZ(position.z());
        mutable.mutY(position.y());
        return super.setBiome(getPos(mutable.x(), mutable.y(), mutable.z()), biome);
    }

    public void setTransform(Transform transform) {
        this.transform = transform;
    }

}
