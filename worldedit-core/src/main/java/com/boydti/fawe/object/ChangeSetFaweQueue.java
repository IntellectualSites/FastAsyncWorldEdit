package com.boydti.fawe.object;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.boydti.fawe.object.queue.DelegateIQueueExtent;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

public class ChangeSetIQueueExtent extends DelegateIQueueExtent {
    private FaweChangeSet set;

    public ChangeSetIQueueExtent(FaweChangeSet set, IQueueExtent parent) {
        super(parent);
        this.set = set;
    }

    public FaweChangeSet getChangeSet() {
        return set;
    }

    public void setChangeSet(FaweChangeSet set) {
        this.set = set;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 p, B block) throws WorldEditException {
        return setBlock(p.getX(), p.getY(), p.getZ(), block.getInternalId(), block.getNbtData());
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) throws WorldEditException {
        return setBlock(x, y, z, block.getInternalId(), block.getNbtData());
    }

    @Override
    public boolean setBlock(int x, int y, int z, int combinedId, CompoundTag nbt) {
        if (setBlock(x, y, z, combinedId)) {
            if (nbt != null) {
                set.addTileCreate(nbt);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean setBlock(int x, int y, int z, int combinedId) {
        if (super.setBlock(x, y, z, combinedId)) {
            int combinedFrom = getParent().getCombinedId4Data(x, y, z);
            BlockType typeFrom = BlockTypes.getFromStateId(combinedFrom);
            if (typeFrom.getMaterial().hasContainer()) {
                CompoundTag nbt = getParent().getTileEntity(x, y, z);
                if (nbt != null) {
                    set.addTileRemove(nbt);
                }
            }
            set.add(x, y, z, combinedFrom, combinedId);
            return true;
        }
        return false;
    }

    @Override
    public boolean setBiome(int x, int z, BiomeType biome) {
        if (super.setBiome(x, z, biome)) {
            BiomeType oldBiome = getParent().getBiomeType(x, z);
            if (oldBiome != biome) {
                set.addBiomeChange(x, z, oldBiome, biome);
                return true;
            }
        }
        return false;
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tag) {
        super.setTile(x, y, z, tag);
        set.addTileCreate(tag);
    }

    @Override
    public void setEntity(int x, int y, int z, CompoundTag tag) {
        super.setEntity(x, y, z, tag);
        set.addEntityCreate(tag);
    }
}
