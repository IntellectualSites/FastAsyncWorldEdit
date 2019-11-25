package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import java.util.Arrays;
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
        this.extents = getExtents().toArray(new ResettableExtent[0]);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) throws WorldEditException {
        return Arrays.stream(extents).map(extent -> extent.setBlock(x, y, z, block))
            .reduce(false, (a, b) -> a || b);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
        return Arrays.stream(extents).map(extent -> extent.setBlock(location, block))
            .reduce(false, (a, b) -> a || b);
    }

    @Override
    public boolean setBiome(BlockVector2 position, BiomeType biome) {
        return Arrays.stream(extents).map(extent -> extent.setBiome(position, biome))
            .reduce(false, (a, b) -> a || b);
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        Entity created = null;
        for (AbstractDelegateExtent extent : extents) created = extent.createEntity(location, entity);
        return created;
    }
}
