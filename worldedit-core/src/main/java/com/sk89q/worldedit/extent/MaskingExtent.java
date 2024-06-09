/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.extent;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.extent.filter.block.CharFilterBlock;
import com.fastasyncworldedit.core.extent.filter.block.ChunkFilterBlock;
import com.fastasyncworldedit.core.extent.filter.block.FilterBlock;
import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.function.LongFunction;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Requires that all mutating methods pass a given {@link Mask}.
 */
public class MaskingExtent extends AbstractDelegateExtent implements IBatchProcessor, Filter {

    //FAWE start
    private final LongFunction<ChunkFilterBlock> getOrCreateFilterBlock;
    private AbstractExtentMask extentMask;
    private Mask mask;
    //FAWE end

    /**
     * Create a new instance.
     *
     * @param extent the extent
     * @param mask   the mask
     */
    public MaskingExtent(Extent extent, Mask mask) {
        super(extent);
        checkNotNull(mask);
        this.mask = mask;
        //FAWE start
        this.getOrCreateFilterBlock = FaweCache.INSTANCE.createMainThreadSafeCache(() -> new CharFilterBlock(getExtent()));
        //FAWE end
    }

    //FAWE start
    private MaskingExtent(Extent extent, Mask mask, LongFunction<ChunkFilterBlock> getOrCreateFilterBlock) {
        super(extent);
        checkNotNull(mask);
        this.mask = mask;
        this.getOrCreateFilterBlock = getOrCreateFilterBlock;
    }
    //FAWE end

    /**
     * Get the mask.
     *
     * @return the mask
     */
    public Mask getMask() {
        return this.mask;
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

    //FAWE start
    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block) throws WorldEditException {
        return this.mask.test(location) && super.setBlock(location, block);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return this.mask.test(BlockVector3.at(x, y, z)) && super.setBiome(x, y, z, biome);
    }

    @Override
    public IChunkSet processSet(final IChunk chunk, final IChunkGet get, final IChunkSet set) {
        final ChunkFilterBlock filter = getOrCreateFilterBlock.apply(Thread.currentThread().getId());
        return filter.filter(chunk, get, set, MaskingExtent.this);
    }

    @Override
    public void applyBlock(final FilterBlock block) {
        if (!this.mask.test(block)) {
            block.setOrdinal(0);
        }
    }

    @Override
    public Extent construct(Extent child) {
        if (child == getExtent()) {
            return this;
        }
        return new MaskingExtent(child, this.mask.copy(), this.getOrCreateFilterBlock);
    }

    @Override
    public Filter fork() {
        return new MaskingExtent(getExtent(), this.mask.copy(), this.getOrCreateFilterBlock);
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.REMOVING_BLOCKS;
    }
    //FAWE end
}
