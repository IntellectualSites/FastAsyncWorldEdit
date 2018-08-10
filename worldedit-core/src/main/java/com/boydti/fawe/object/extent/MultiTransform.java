package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.Collection;
import javax.annotation.Nullable;

public class MultiTransform extends RandomTransform {
    private ResettableExtent[] extents;

    public MultiTransform(Collection<ResettableExtent> extents) {
        for (ResettableExtent extent : extents) add(extent, 1);
    }

    public MultiTransform() {
        this.extents = new ResettableExtent[0];
    }

    @Override
    public void add(ResettableExtent extent, double chance) {
        super.add(extent, chance);
        this.extents = getExtents().toArray(new ResettableExtent[getExtents().size()]);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        boolean result = false;
        for (AbstractDelegateExtent extent : extents) result |= extent.setBlock(x, y, z, block);
        return result;
    }

    @Override
    public boolean setBlock(BlockVector3 location, BlockStateHolder block) throws WorldEditException {
        boolean result = false;
        for (AbstractDelegateExtent extent : extents) result |= extent.setBlock(location, block);
        return result;
    }

    @Override
    public boolean setBiome(BlockVector2 position, BaseBiome biome) {
        boolean result = false;
        for (AbstractDelegateExtent extent : extents) result |= extent.setBiome(position, biome);
        return result;
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        Entity created = null;
        for (AbstractDelegateExtent extent : extents) created = extent.createEntity(location, entity);
        return created;
    }
}
