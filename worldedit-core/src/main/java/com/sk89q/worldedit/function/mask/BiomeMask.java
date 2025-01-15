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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Tests true if the biome at applied points is the same as the one given.
 */
//FAWE start - AbstractExtentMask
public class BiomeMask extends AbstractExtentMask {
//FAWE end

    //FAWE start - avoid HashSet usage
    private final boolean[] biomes;
    //FAWE end

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
        //FAWE start - avoid HashSet usage
        this.biomes = new boolean[BiomeType.REGISTRY.size()];
        for (final BiomeType biome : biomes) {
            this.biomes[biome.getInternalId()] = true;
        }
        //FAWE end
    }

    private BiomeMask(Extent extent, boolean[] biomes) {
        super(extent);
        this.biomes = biomes;
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
        //FAWE start - avoid HashSet usage
        for (final BiomeType biome : biomes) {
            this.biomes[biome.getInternalId()] = true;
        }
        //FAWE end
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
        //FAWE start - avoid HashSet usage
        return BiomeType.REGISTRY.values().stream().filter(type -> biomes[type.getInternalId()]).toList();
        //FAWE end
    }

    @Override
    public boolean test(BlockVector3 vector) {
        //FAWE start - avoid HashSet usage
        BiomeType biome = vector.getBiome(getExtent());
        return biomes[biome.getInternalId()];
        //FAWE end
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }

    //FAWE start
    @Override
    public Mask copy() {
        return new BiomeMask(getExtent(), this.biomes.clone());
    }

    @Override
    public boolean test(Extent extent, BlockVector3 position) {
        BiomeType biome = getExtent().getBiome(position);
        return biomes[biome.getInternalId()];
    }
    //FAWE end

}
