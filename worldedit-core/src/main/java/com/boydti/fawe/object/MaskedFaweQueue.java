package com.boydti.fawe.object;

import com.boydti.fawe.object.extent.FaweRegionExtent;
import com.boydti.fawe.object.extent.HeightBoundExtent;
import com.boydti.fawe.object.extent.MultiRegionExtent;
import com.boydti.fawe.object.extent.SingleRegionExtent;
import com.boydti.fawe.object.queue.DelegateFaweQueue;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class MaskedFaweQueue extends DelegateFaweQueue {
    private FaweRegionExtent region;

    public MaskedFaweQueue(FaweQueue parent, Region[] mask) {
        super(parent);
        setMask(mask);
    }

    public void setMask(Region[] mask) {
        switch (mask.length) {
            case 0:
                region = new HeightBoundExtent(this, FaweLimit.MAX.copy(), 0, 255);
                break;
            case 1:
                region = new SingleRegionExtent(this, FaweLimit.MAX.copy(), mask[0]);
                break;
            default:
                region = new MultiRegionExtent(this, FaweLimit.MAX.copy(), mask);
                break;
        }
        if (mask.length == 1) {
            region = new SingleRegionExtent(this, FaweLimit.MAX.copy(), mask[0]);
        } else {
            region = new MultiRegionExtent(this, FaweLimit.MAX.copy(), mask);
        }
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tag) {
        if (region.contains(x, y, z)) {
            super.setTile(x, y, z, tag);
        }
    }

    @Override
    public void setEntity(int x, int y, int z, CompoundTag tag) {
        if (region.contains(x, y, z)) {
            super.setEntity(x, y, z, tag);
        }
    }

    @Override
    public boolean setBlock(int x, int y, int z, int combinedId) {
        if (region.contains(x, y, z)) {
            return super.setBlock(x, y, z, combinedId);
        }
        return false;
    }

    @Override
    public boolean setBlock(int x, int y, int z, int combinedId, CompoundTag nbt) {
        if (region.contains(x, y, z)) {
            return super.setBlock(x, y, z, combinedId, nbt);
        }
        return false;
    }

    @Override
    public boolean setBlock(Vector position, BlockStateHolder block) throws WorldEditException {
        if (region.contains(position.getBlockX(), position.getBlockZ())) {
            return super.setBlock(position, block);
        }
        return false;
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        if (region.contains(x, y, z)) {
            return super.setBlock(x, y, z, block);
        }
        return false;
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        if (region.contains(position.getBlockX(), position.getBlockZ())) {
            return super.setBiome(position, biome);
        }
        return false;
    }

    @Override
    public boolean setBiome(int x, int z, BaseBiome biome) {
        if (region.contains(x, z)) {
            return super.setBiome(x, z, biome);
        }
        return false;
    }
}
