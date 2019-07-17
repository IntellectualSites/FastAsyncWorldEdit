package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

public class TemporalExtent extends AbstractDelegateExtent {
    private int x, y, z = Integer.MAX_VALUE;
    private BlockStateHolder<?> block = BlockTypes.AIR.getDefaultState();

    private int bx, bz = Integer.MAX_VALUE;
    private BiomeType biome = null;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public TemporalExtent(Extent extent) {
        super(extent);
    }


    public <B extends BlockStateHolder<B>> void set(int x, int y, int z, B block) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = block;
    }

    public void set(int x, int z, BiomeType biome) {
        this.bx = x;
        this.bz = z;
        this.biome = biome;
    }

    @Override
    public int getBrightness(int x, int y, int z) {
        if (this.x == x && this.y == y && this.z == z) {
            return Math.min(15, block.getBlockType().getMaterial().getLightValue());
        }
        return super.getBrightness(x, y, z);
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        if (position.getX() == x && position.getY() == y && position.getZ() == z) {
            return block.toImmutableState();
        }
        return super.getBlock(position);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        if (this.x == x && this.y == y && this.z == z) {
            return block.toImmutableState();
        }
        return super.getBlock(x, y, z);
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        if (position.getX() == x && position.getY() == y && position.getZ() == z) {
            if(block instanceof BaseBlock) {
            	return (BaseBlock)block;
            }else {
            	return block.toBaseBlock();
            }
        }
        return super.getFullBlock(position);
    }

    @Override
    public BiomeType getBiome(BlockVector2 position) {
        if (position.getX() == bx && position.getZ() == bz) {
            return biome;
        }
        return super.getBiome(position);
    }
}
