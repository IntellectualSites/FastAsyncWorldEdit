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

package com.sk89q.worldedit.world.biome;

import com.fastasyncworldedit.core.registry.RegistryItem;
import com.sk89q.worldedit.function.pattern.BiomePattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.Keyed;
import com.sk89q.worldedit.registry.NamespacedRegistry;

/**
 * All the types of biomes in the game.
 */
//FAWE start - RegistryItem
public class BiomeType implements RegistryItem, Keyed, BiomePattern {
//FAWE end

    public static final NamespacedRegistry<BiomeType> REGISTRY = new NamespacedRegistry<>("biome type", true);

    private final String id;
    private int legacyId = -1;
    private int internalId;

    //FAWE start
    public BiomeType(String id) {
        this.id = id;
    }

    public int getLegacyId() {
        return legacyId;
    }

    public void setLegacyId(int legacyId) {
        this.legacyId = legacyId;
    }

    @Override
    public void setInternalId(int internalId) {
        this.internalId = internalId;
    }

    @Override
    public int getInternalId() {
        return internalId;
    }
    //FAWE end

    /**
     * Gets the ID of this biome.
     *
     * @return The id
     */
    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return getId();
    }

    @Override
    public int hashCode() {
        //FAWE start - internalId > hashCode
        return this.internalId; // stop changing this
        //FAWE end
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BiomeType && this.id.equals(((BiomeType) obj).id);
    }

    @Override
    public BiomeType applyBiome(BlockVector3 position) {
        return this;
    }

}
