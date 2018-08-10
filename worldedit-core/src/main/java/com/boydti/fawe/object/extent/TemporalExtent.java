package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.EditSession;

import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.registry.BundledBlockData;

public class TemporalExtent extends AbstractDelegateExtent {
    private int x, y, z = Integer.MAX_VALUE;
    private BlockState block = EditSession.nullBlock;

    private int bx, bz = Integer.MAX_VALUE;
    private BaseBiome biome = EditSession.nullBiome;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public TemporalExtent(Extent extent) {
        super(extent);
    }


    public void set(int x, int y, int z, BlockStateHolder block) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = (BlockState) block;
    }

    public void set(int x, int z, BaseBiome biome) {
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
            return block;
        }
        return super.getBlock(position);
    }

    @Override
    public BlockState getLazyBlock(BlockVector3 position) {
        if (position.getX() == x && position.getY() == y && position.getZ() == z) {
            return block;
        }
        return super.getLazyBlock(position);
    }

    @Override
    public BlockState getLazyBlock(int x, int y, int z) {
        if (this.x == x && this.y == y && this.z == z) {
            return block;
        }
        return super.getLazyBlock(x, y, z);
    }

    @Override
    public BaseBiome getBiome(BlockVector2 position) {
        if (position.getX() == bx && position.getZ() == bz) {
            return biome;
        }
        return super.getBiome(position);
    }
}
