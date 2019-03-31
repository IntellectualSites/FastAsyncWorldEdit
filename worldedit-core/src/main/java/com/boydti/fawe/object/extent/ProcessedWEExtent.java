package com.boydti.fawe.object.extent;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.util.WEManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.List;

public class ProcessedWEExtent extends AbstractDelegateExtent {
    private final FaweLimit limit;
    private final AbstractDelegateExtent extent;

    public ProcessedWEExtent(final Extent parent, FaweLimit limit) {
        super(parent);
        this.limit = limit;
        this.extent = (AbstractDelegateExtent) parent;
    }

    public void setLimit(FaweLimit other) {
        this.limit.set(other);
    }

    @Override
    public Entity createEntity(final Location location, final BaseEntity entity) {
        if (entity == null) {
            return null;
        }
        if (!limit.MAX_ENTITIES()) {
            WEManager.IMP.cancelEditSafe(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_ENTITIES);
            return null;
        }
        return super.createEntity(location, entity);
    }

    @Override
    public BaseBiome getBiome(final BlockVector2 position) {
        return super.getBiome(position);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return super.getEntities();
    }

    @Override
    public List<? extends Entity> getEntities(final Region region) {
        return super.getEntities(region);
    }

    @Override
    public BlockState getLazyBlock(int x, int y, int z) {
        if (!limit.MAX_CHECKS()) {
            WEManager.IMP.cancelEditSafe(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_CHECKS);
            return EditSession.nullBlock;
        } else {
            return extent.getLazyBlock(x, y, z);
        }
    }
    
    @Override
    public BaseBlock getFullBlock(BlockVector3 pos) {
        if (!limit.MAX_CHECKS()) {
            WEManager.IMP.cancelEditSafe(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_CHECKS);
            return EditSession.nullBlock.toBaseBlock();
        } else {
            return extent.getFullBlock(pos);
        }
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(final BlockVector3 location, final B block) throws WorldEditException {
        final int x = location.getBlockX();
        final int y = location.getBlockY();
        final int z = location.getBlockZ();
        boolean hasNbt = block instanceof BaseBlock && ((BaseBlock)block).hasNbtData();
        if (hasNbt) {
            if (!limit.MAX_BLOCKSTATES()) {
                WEManager.IMP.cancelEdit(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_TILES);
                return false;
            } else {
                if (!limit.MAX_CHANGES()) {
                    WEManager.IMP.cancelEdit(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_CHANGES);
                    return false;
                }
                return extent.setBlock(x, y, z, block);
            }
        }
        if (!limit.MAX_CHANGES()) {
            WEManager.IMP.cancelEdit(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_CHANGES);
            return false;
        } else {
            return extent.setBlock(x, y, z, block);
        }
    }

    @Override
    public BlockState getLazyBlock(BlockVector3 location) {
        return getLazyBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public boolean setBiome(final BlockVector2 position, final BaseBiome biome) {
        if (!limit.MAX_CHANGES()) {
            WEManager.IMP.cancelEditSafe(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_CHANGES);
            return false;
        }
        return super.setBiome(position, biome);
    }
}
