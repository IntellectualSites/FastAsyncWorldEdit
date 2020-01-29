/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.extent;

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.implementation.filter.block.CharFilterBlock;
import com.boydti.fawe.beta.implementation.filter.block.ChunkFilterBlock;
import com.boydti.fawe.beta.implementation.filter.block.FilterBlock;
import com.boydti.fawe.util.ExtentTraverser;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Requires that all mutating methods pass a given {@link Mask}.
 */
public class MaskingExtent extends AbstractDelegateExtent implements IBatchProcessor, Filter {

    private Mask mask;
    private LoadingCache<Long, ChunkFilterBlock> threadIdToFilter = FaweCache.IMP.createCache(() -> new CharFilterBlock(getExtent()));

    /**
     * Create a new instance.
     *
     * @param extent the extent
     * @param mask the mask
     */
    public MaskingExtent(Extent extent, Mask mask) {
        super(extent);
        checkNotNull(mask);
        this.mask = mask;
    }

    /**
     * Get the mask.
     *
     * @return the mask
     */
    public Mask getMask() {
        return mask;
    }

    /**
     * Set a mask.
     *
     * @param mask a mask
     */
    public void setMask(Mask mask) {
        checkNotNull(mask);
        this.mask = mask;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block) throws WorldEditException {
        return mask.test(getExtent(), location) && super.setBlock(location, block);
    }

    @Override
    public boolean setBiome(BlockVector2 position, BiomeType biome) {
        return mask.test(getExtent(), position.toBlockVector3()) && super.setBiome(position, biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return mask.test(getExtent(), BlockVector3.at(x, y, z)) && super.setBiome(x, y, z, biome);
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        ChunkFilterBlock filter = threadIdToFilter.getUnchecked(Thread.currentThread().getId());
        return filter.filter(chunk, get, set, this);
    }

    @Override
    public void applyBlock(FilterBlock block) {
        int ordinal = block.getOrdinal();
        if (ordinal != 0 && !mask.test(getExtent(), block)) {
            block.setOrdinal(0);
        }
    }

    @Override
    public Extent construct(Extent child) {
        if (child == getExtent()) return this;
        return new MaskingExtent(child, mask);
    }
}
