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

package com.sk89q.worldedit.world.storage;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldedit.world.World;
import org.enginehub.linbus.stream.LinBinaryIO;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinRootEntry;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class McRegionChunkStore extends ChunkStore {

    /**
     * Get the filename of a region file.
     *
     * @param position chunk position
     * @return the filename
     */
    public static String getFilename(BlockVector2 position) {
        int x = position.x();
        int z = position.z();

        return "r." + (x >> 5) + "." + (z >> 5) + ".mca";
    }

    //FAWE start - biome and entity restore
    protected McRegionReader getReader(BlockVector2 pos, String worldname, @Nullable String folderOverride) throws DataException,
            IOException {
        //FAWE end
        String filename = getFilename(pos);
        //FAWE start - biome and entity restore
        InputStream stream = getInputStream(filename, worldname, folderOverride);
        //FAWE end
        //curFilename = filename;
        return new McRegionReader(stream);
    }

    @Override
    public CompoundTag getChunkTag(BlockVector2 position, World world) throws DataException, IOException {
        return ChunkStoreHelper.readCompoundTag(() -> {
            McRegionReader reader = getReader(position, world.getName(), null);

            return reader.getChunkInputStream(position);
        });
    }

    //FAWE start - biome and entity restore
    @Override
    public CompoundTag getEntitiesTag(BlockVector2 position, World world) {
        try {
            return ChunkStoreHelper.readCompoundTag(() -> {
                McRegionReader reader = getReader(position, world.getName(), "entities");

                return reader.getChunkInputStream(position);
            });
        } catch (DataException | IOException e) {
            return null;
        }
    }
    //FAWE end

    /**
     * Get the input stream for a chunk file.
     *
     * @param name           the name of the chunk file
     * @param worldName      the world name
     * @param folderOverride override folder to check. "entities" used for getting entities in 1.17+
     * @return an input stream
     * @throws IOException if there is an error getting the chunk data
     */
    //FAWE start - biome and entity restore
    protected abstract InputStream getInputStream(String name, String worldName, @Nullable String folderOverride) throws
            IOException, DataException;
    //FAWE end

}
