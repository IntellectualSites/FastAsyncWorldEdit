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

package com.sk89q.worldedit.world.registry;

import com.google.common.io.Resources;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BlockMaterial;
import com.sk89q.worldedit.util.gson.VectorAdapter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * Provides block data based on the built-in block database that is bundled
 * with WorldEdit.
 *
 * <p>A new instance cannot be created. Use {@link #getInstance()} to get
 * an instance.</p>
 *
 * <p>The data is read from a JSON file that is bundled with WorldEdit. If
 * reading fails (which occurs when this class is first instantiated), then
 * the methods will return {@code null}s for all blocks.</p>
 */
public class BundledBlockData {

    private static final Logger log = Logger.getLogger(BundledBlockData.class.getCanonicalName());
    private static BundledBlockData INSTANCE;

    private final Map<String, BlockEntry> idMap = new HashMap<>();

    /**
     * Create a new instance.
     */
    private BundledBlockData() {
        try {
            loadFromResource();
        } catch (Throwable e) {
            log.log(Level.WARNING, "Failed to load the built-in block registry", e);
        }
    }

    /**
     * Attempt to load the data from file.
     *
     * @throws IOException thrown on I/O error
     */
    private void loadFromResource() throws IOException {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Vector.class, new VectorAdapter());
        gsonBuilder.registerTypeAdapter(int.class, new JsonDeserializer<Integer>() {
            @Override
            public Integer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                JsonPrimitive primitive = (JsonPrimitive) json;
                if (primitive.isString()) {
                    String value = primitive.getAsString();
                    if (value.charAt(0) == '#') return Integer.parseInt(value.substring(1), 16);
                    return Integer.parseInt(value);
                }
                return primitive.getAsInt();
            }
        });
        Gson gson = gsonBuilder.create();
        URL url = BundledBlockData.class.getResource("blocks.json");
        if (url == null) {
            throw new IOException("Could not find blocks.json");
        }
        String data = Resources.toString(url, Charset.defaultCharset());
        List<BlockEntry> entries = gson.fromJson(data, new TypeToken<List<BlockEntry>>() {}.getType());

        for (BlockEntry entry : entries) {
            idMap.put(entry.id, entry);
        }
    }

    /**
     * Return the entry for the given block ID.
     *
     * @param id the ID
     * @return the entry, or null
     */
    @Nullable
    public BlockEntry findById(String id) {
        // If it has no namespace, assume minecraft.
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        return idMap.get(id);
    }

    /**
     * Get the material properties for the given block.
     *
     * @param id the string ID
     * @return the material's properties, or null
     */
    @Nullable
    public BlockMaterial getMaterialById(String id) {
        BlockEntry entry = findById(id);
        if (entry != null) {
            return entry.material;
        } else {
            return null;
        }
    }

    /**
     * Get a singleton instance of this object.
     *
     * @return the instance
     */
    public static BundledBlockData getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BundledBlockData();
        }
        return INSTANCE;
    }

    public static class BlockEntry {
        public String id;
        public String localizedName;
        public SimpleBlockMaterial material = new SimpleBlockMaterial();
    }

}