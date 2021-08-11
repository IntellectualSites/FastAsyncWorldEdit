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

package com.sk89q.worldedit.extent.buffer;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.pattern.BiomePattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.AbstractFlatRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Buffers changes to an {@link Extent} and allows later retrieval for
 * actual application of the changes.
 *
 * <p>This buffer will not attempt to return results from the buffer when
 * accessor methods (such as {@link #getBlock(BlockVector3)}) are called.</p>
 */
public class ForgetfulExtentBuffer extends AbstractDelegateExtent implements Pattern, BiomePattern {

    private final Map<BlockVector3, BaseBlock> buffer = new LinkedHashMap<>();
    private final Map<BlockVector3, BiomeType> biomeBuffer = new LinkedHashMap<>();
    private final Mask mask;
    private BlockVector3 min = null;
    private BlockVector3 max = null;

    /**
     * Create a new extent buffer that will buffer every change.
     *
     * @param delegate the delegate extent for {@link Extent#getBlock(BlockVector3)}, etc. calls
     */
    public ForgetfulExtentBuffer(Extent delegate) {
        this(delegate, Masks.alwaysTrue());
    }

    //FAWE start
    @Override
    public boolean isQueueEnabled() {
        return true;
    }
    //FAWE end

    /**
     * Create a new extent buffer that will buffer changes that meet the criteria
     * of the given mask.
     *
     * @param delegate the delegate extent for {@link Extent#getBlock(BlockVector3)}, etc. calls
     * @param mask     the mask
     */
    public ForgetfulExtentBuffer(Extent delegate, Mask mask) {
        super(delegate);
        checkNotNull(mask);
        this.mask = mask;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block) throws WorldEditException {
        // Update minimum
        if (min == null) {
            min = location;
        } else {
            min = min.getMinimum(location);
        }

        // Update maximum
        if (max == null) {
            max = location;
        } else {
            max = max.getMaximum(location);
        }

        //FAWE start
        if (mask.test(location)) {
            buffer.put(location, block.toBaseBlock());
            return true;
        } else {
            return getExtent().setBlock(location, block);
        }
        //FAWE end
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        //FAWE start
        // Update minimum
        if (min == null) {
            min = position;
        } else {
            min = min.getMinimum(position);
        }

        // Update maximum
        if (max == null) {
            max = position;
        } else {
            max = max.getMaximum(position);
        }

        if (mask.test(position)) {
            biomeBuffer.put(position, biome);
            return true;
        } else {
            return getExtent().setBiome(position, biome);
        }
        //FAWE end
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        //FAWE start
        // Update minimum
        if (min == null) {
            min = BlockVector3.at(x, y, z);
        } else {
            min = min.getMinimum(BlockVector3.at(x, y, z));
        }

        // Update maximum
        if (max == null) {
            max = BlockVector3.at(x, y, z);
        } else {
            max = max.getMaximum(BlockVector3.at(x, y, z));
        }

        BlockVector3 pos = BlockVector3.at(x, y, z);
        if (mask.test(pos)) {
            biomeBuffer.put(pos, biome);
            return true;
        } else {
            return getExtent().setBiome(x, y, z, biome);
        }
        //FAWE end
    }


    @Override
    public BaseBlock applyBlock(BlockVector3 pos) {
        BaseBlock block = buffer.get(pos);
        if (block != null) {
            return block;
        } else {
            return BlockTypes.AIR.getDefaultState().toBaseBlock();
        }
    }

    @Override
    public BiomeType applyBiome(BlockVector3 pos) {
        BiomeType biome = biomeBuffer.get(pos);
        if (biome != null) {
            return biome;
        } else {
            return BiomeTypes.OCEAN;
        }
    }

    /**
     * Return a region representation of this buffer.
     *
     * @return a region
     */
    public Region asRegion() {
        return new AbstractFlatRegion(null) {
            @Override
            public BlockVector3 getMinimumPoint() {
                return min != null ? min : BlockVector3.ZERO;
            }

            @Override
            public BlockVector3 getMaximumPoint() {
                return max != null ? max : BlockVector3.ZERO;
            }

            @Override
            public void expand(BlockVector3... changes) throws RegionOperationException {
                throw new UnsupportedOperationException("Cannot change the size of this region");
            }

            @Override
            public void contract(BlockVector3... changes) throws RegionOperationException {
                throw new UnsupportedOperationException("Cannot change the size of this region");
            }

            @Override
            public boolean contains(BlockVector3 position) {
                return buffer.containsKey(position);
            }

            @Override
            public Iterator<BlockVector3> iterator() {
                return buffer.keySet().iterator();
            }

            @Override
            public Iterable<BlockVector2> asFlatRegion() {
                return biomeBuffer.keySet()
                        .stream()
                        .map(BlockVector3::toBlockVector2)
                        .collect(Collectors.toSet());
            }
        };
    }

}
