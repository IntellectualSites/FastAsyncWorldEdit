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

import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A 16 by 16 block chunk.
 */
public interface Chunk {

    /**
     * Get a block.
     *
     * @param position the position of the block
     * @return block the block
     * @throws DataException thrown on data error
     */
    BaseBlock getBlock(BlockVector3 position) throws DataException;

    //FAWE start - biome and entity restore
    /**
     * Get a biome.
     *
     * @param position the position of the block
     * @return block the block
     * @throws DataException thrown on data error
     */
    default BiomeType getBiome(BlockVector3 position) throws DataException {
        return null;
    }

    /**
     * Get the stored entities.
     * @return list of stored entities
     */
    default List<BaseEntity> getEntities() throws DataException {
        return Collections.emptyList();
    }
    //FAWE end

}
