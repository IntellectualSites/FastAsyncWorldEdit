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

package com.sk89q.worldedit.world.chunk;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinIntArrayTag;
import org.enginehub.linbus.tree.LinTagType;

/**
 * The chunk format for Minecraft 1.15 and newer
 */
public class AnvilChunk15 extends AnvilChunk13 {

    /**
     * Construct the chunk with a compound tag.
     *
     * @param tag the tag to read
     * @throws DataException on a data error
     * @deprecated Use {@link #AnvilChunk15(LinCompoundTag)}
     */
    @Deprecated
    public AnvilChunk15(CompoundTag tag) throws DataException {
        super(tag);
    }

    /**
     * Construct the chunk with a compound tag.
     *
     * @param tag the tag to read
     * @throws DataException on a data error
     */
    public AnvilChunk15(LinCompoundTag tag) throws DataException {
        super(tag);
    }

    @Override
    public BiomeType getBiome(final BlockVector3 position) throws DataException {
        if (biomes == null) {
            populateBiomes();
        }
        int x = (position.x() & 15) >> 2;
        int y = position.y() >> 2;
        int z = (position.z() & 15) >> 2;
        return biomes[y << 4 | z << 2 | x];
    }

    private void populateBiomes() throws DataException {
        biomes = new BiomeType[1024];
        LinIntArrayTag biomeTag = rootTag.findTag("Biomes", LinTagType.intArrayTag());
        if (biomeTag == null) {
            return;
        }
        int[] stored = biomeTag.value();
        for (int i = 0; i < 1024; i++) {
            biomes[i] = BiomeTypes.getLegacy(stored[i]);
        }
    }

}
