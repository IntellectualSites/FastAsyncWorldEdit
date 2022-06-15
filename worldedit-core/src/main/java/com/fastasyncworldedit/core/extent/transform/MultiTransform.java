package com.fastasyncworldedit.core.extent.transform;

import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;

public class MultiTransform extends RandomTransform {

    private ResettableExtent[] extents;

    /**
     * New instance
     *
     * @param extents list of extents to set blocks to
     */
    public MultiTransform(Collection<ResettableExtent> extents) {
        for (ResettableExtent extent : extents) {
            add(extent, 1);
        }
    }

    public MultiTransform() {
        this.extents = new ResettableExtent[0];
    }

    @Override
    public void add(ResettableExtent extent, double chance) {
        super.add(extent, chance);
        this.extents = getExtents().toArray(new ResettableExtent[0]);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block)
            throws WorldEditException {
        // don't use streams for each block place, it'd be incredibly slow
        boolean result = false;
        for (AbstractDelegateExtent extent : extents) {
            result |= extent.setBlock(x, y, z, block);
        }
        return result;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block)
            throws WorldEditException {
        // don't use streams for each block place, it'd be incredibly slow
        boolean result = false;
        for (AbstractDelegateExtent extent : extents) {
            result |= extent.setBlock(location, block);
        }
        return result;
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        // don't use streams for each block place, it'd be incredibly slow
        boolean result = false;
        for (AbstractDelegateExtent extent : extents) {
            result |= extent.setBiome(position, biome);
        }
        return result;
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        // don't use streams for each block place, it'd be incredibly slow
        boolean result = false;
        for (AbstractDelegateExtent extent : extents) {
            result |= extent.setBiome(x, y, z, biome);
        }
        return result;
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        Entity created = null;
        for (AbstractDelegateExtent extent : extents) {
            created = extent.createEntity(location, entity);
        }
        return created;
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity, UUID uuid) {
        Entity created = null;
        for (AbstractDelegateExtent extent : extents) {
            created = extent.createEntity(location, entity, uuid);
        }
        return created;
    }

}
