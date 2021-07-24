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

package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Tests true if the biome at applied points is the same as the one given.
 */
//FAWE start - AbstractExtentMask
public class BiomeMask extends AbstractExtentMask {
//FAWE end

    private final Set<BiomeType> biomes = new HashSet<>();

    /**
     * Create a new biome mask.
     *
     * @param extent the extent
     * @param biomes a list of biomes to match
     */
    public BiomeMask(Extent extent, Collection<BiomeType> biomes) {
        //FAWE start
        super(extent);
        //FAWE end
        checkNotNull(biomes);
        this.biomes.addAll(biomes);
    }

    /**
     * Create a new biome mask.
     *
     * @param extent the extent
     * @param biome  an array of biomes to match
     */
    public BiomeMask(Extent extent, BiomeType... biome) {
        this(extent, Arrays.asList(checkNotNull(biome)));
    }

    /**
     * Add the given biomes to the list of criteria.
     *
     * @param biomes a list of biomes
     */
    public void add(Collection<BiomeType> biomes) {
        checkNotNull(biomes);
        this.biomes.addAll(biomes);
    }

    /**
     * Add the given biomes to the list of criteria.
     *
     * @param biome an array of biomes
     */
    public void add(BiomeType... biome) {
        add(Arrays.asList(checkNotNull(biome)));
    }

    /**
     * Get the list of biomes that are tested with.
     *
     * @return a list of biomes
     */
    public Collection<BiomeType> getBiomes() {
        return biomes;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        BiomeType biome = getExtent().getBiome(vector);
        return biomes.contains(biome);
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }

    //FAWE start
    @Override
    public Mask copy() {
        return new BiomeMask(getExtent(), new HashSet<>(biomes));
    }
    //FAWE end

    @Override
    public boolean test(Extent extent, BlockVector3 position) {
        BiomeType biome = getExtent().getBiome(position);
        return biomes.contains(biome);
    }

}
