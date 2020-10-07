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

package com.sk89q.worldedit.extent.clipboard.io;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.jnbt.streamer.StreamDelegate;
import com.boydti.fawe.jnbt.streamer.ValueReader;
import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.clipboard.LinearClipboard;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.object.io.FastByteArraysInputStream;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reads schematic files using the Sponge Schematic Specification.
 */
public class FastSchematicReader extends NBTSchematicReader {

    private static final Logger log = LoggerFactory.getLogger(FastSchematicReader.class);
    private final NBTInputStream inputStream;
    private DataFixer fixer = null;
    private int dataVersion = -1;
    private int version = -1;

    private FastByteArrayOutputStream blocksOut;
    private FaweOutputStream blocks;

    private FastByteArrayOutputStream biomesOut;
    private FaweOutputStream biomes;

    private List<Map<String, Object>> tiles;
    private List<Map<String, Object>> entities;

    private int width;
    private int height;
    private int length;
    private int offsetX;
    private int offsetY;
    private int offsetZ;
    private char[] palette;
    private char[] biomePalette;
    private BlockVector3 min = BlockVector3.ZERO;


    /**
     * Create a new instance.
     *
     * @param inputStream the input stream to read from
     */
    public FastSchematicReader(NBTInputStream inputStream) {
        checkNotNull(inputStream);
        this.inputStream = inputStream;
    }

    private String fix(String palettePart) {
        if (fixer == null || dataVersion == -1) {
            return palettePart;
        }
        return fixer.fixUp(DataFixer.FixTypes.BLOCK_STATE, palettePart, dataVersion);
    }

    private CompoundTag fixBlockEntity(CompoundTag tag) {
        if (fixer == null || dataVersion == -1) {
            return tag;
        }
        return fixer.fixUp(DataFixer.FixTypes.BLOCK_ENTITY, tag, dataVersion);
    }

    private CompoundTag fixEntity(CompoundTag tag) {
        if (fixer == null || dataVersion == -1) {
            return tag;
        }
        return fixer.fixUp(DataFixer.FixTypes.ENTITY, tag, dataVersion);
    }

    private String fixBiome(String biomePalettePart) {
        if (fixer == null || dataVersion == -1) {
            return biomePalettePart;
        }
        return fixer.fixUp(DataFixer.FixTypes.BIOME, biomePalettePart, dataVersion);
    }

    public StreamDelegate createDelegate() {
        StreamDelegate root = new StreamDelegate();
        StreamDelegate schematic = root.add("Schematic");
        schematic.add("DataVersion").withInt((i, v) -> dataVersion = v);
        schematic.add("Version").withInt((i, v) -> version = v);
        schematic.add("Width").withInt((i, v) -> width = v);
        schematic.add("Height").withInt((i, v) -> height = v);
        schematic.add("Length").withInt((i, v) -> length = v);
        schematic.add("Offset").withValue((ValueReader<int[]>) (index, v) -> min = BlockVector3.at(v[0], v[1], v[2]));

        StreamDelegate metadata = schematic.add("Metadata");
        metadata.add("WEOffsetX").withInt((i, v) -> offsetX = v);
        metadata.add("WEOffsetY").withInt((i, v) -> offsetY = v);
        metadata.add("WEOffsetZ").withInt((i, v) -> offsetZ = v);

        StreamDelegate paletteDelegate = schematic.add("Palette");
        paletteDelegate.withValue((ValueReader<Map<String, Object>>) (ignore, v) -> {
            palette = new char[v.size()];
            for (Entry<String, Object> entry : v.entrySet()) {
                BlockState state = null;
                try {
                    String palettePart = fix(entry.getKey());
                    state = BlockState.get(palettePart);
                } catch (InputParseException e) {
                    e.printStackTrace();
                }
                int index = (int) entry.getValue();
                palette[index] = (char) state.getOrdinal();
            }
        });
        StreamDelegate blockData = schematic.add("BlockData");
        blockData.withInfo((length, type) -> {
            blocksOut = new FastByteArrayOutputStream();
            blocks = new FaweOutputStream(new LZ4BlockOutputStream(blocksOut));
        });
        blockData.withInt((index, value) -> blocks.write(value));

        StreamDelegate tilesDelegate = schematic.add("BlockEntities");
        tilesDelegate.withInfo((length, type) -> tiles = new ArrayList<>(length));
        tilesDelegate.withElem((ValueReader<Map<String, Object>>) (index, tile) -> tiles.add(tile));

        // Keep this here so schematics created with FAWE before TileEntities was fixed to BlockEntities still work
        StreamDelegate compatTilesDelegate = schematic.add("TileEntities");
        compatTilesDelegate.withInfo((length, type) -> tiles = new ArrayList<>(length));
        compatTilesDelegate.withElem((ValueReader<Map<String, Object>>) (index, tile) -> tiles.add(tile));

        StreamDelegate entitiesDelegate = schematic.add("Entities");
        entitiesDelegate.withInfo((length, type) -> entities = new ArrayList<>(length));
        entitiesDelegate.withElem((ValueReader<Map<String, Object>>) (index, entity) -> entities.add(entity));

        StreamDelegate biomePaletteDelegate = schematic.add("BiomePalette");
        biomePaletteDelegate.withValue((ValueReader<Map<String, Object>>) (ignore, v) -> {
            biomePalette = new char[v.size()];
            for (Entry<String, Object> entry : v.entrySet()) {
                BiomeType biome = null;
                try {
                    String biomePalettePart = fixBiome(entry.getKey());
                    biome = BiomeTypes.get(biomePalettePart);
                } catch (InputParseException e) {
                    e.printStackTrace();
                }
                int index = (int) entry.getValue();
                biomePalette[index] = (char) biome.getInternalId();
            }
        });
        StreamDelegate biomeData = schematic.add("BiomeData");
        biomeData.withInfo((length, type) -> {
            biomesOut = new FastByteArrayOutputStream();
            biomes = new FaweOutputStream(new LZ4BlockOutputStream(biomesOut));
        });
        biomeData.withInt((index, value) -> biomes.write(value));
        return root;
    }

    private BlockState getBlockState(int id) {
        return BlockTypesCache.states[palette[id]];
    }

    private BiomeType getBiomeType(FaweInputStream fis) throws IOException {
        char biomeId = biomePalette[fis.readVarInt()];
        return BiomeTypes.get(biomeId);
    }

    @Override
    public Clipboard read(UUID uuid, Function<BlockVector3, Clipboard> createOutput) throws IOException {
        StreamDelegate root = createDelegate();
        inputStream.readNamedTagLazy(root);

        if (version != 1 && version != 2) {
            throw new IOException("This schematic version is currently not supported");
        }

        if (blocks != null) {
            blocks.close();
        }
        if (biomes != null) {
            biomes.close();
        }
        blocks = null;
        biomes = null;

        BlockVector3 dimensions = BlockVector3.at(width, height, length);
        BlockVector3 origin = BlockVector3.ZERO;
        if (offsetX != Integer.MIN_VALUE && offsetY != Integer.MIN_VALUE  && offsetZ != Integer.MIN_VALUE) {
            origin = BlockVector3.at(-offsetX, -offsetY, -offsetZ);
        }

        Clipboard clipboard = createOutput.apply(dimensions);

        if (blocksOut != null && blocksOut.getSize() != 0) {
            try (FaweInputStream fis = new FaweInputStream(new LZ4BlockInputStream(new FastByteArraysInputStream(blocksOut.toByteArrays())))) {
                if (clipboard instanceof LinearClipboard) {
                    LinearClipboard linear = (LinearClipboard) clipboard;
                    int volume = width * height * length;
                    if (palette.length < 128) {
                        for (int index = 0; index < volume; index++) {
                            int ordinal = fis.read();
                            linear.setBlock(index, getBlockState(ordinal));
                        }
                    } else {
                        for (int index = 0; index < volume; index++) {
                            int ordinal = fis.readVarInt();
                            linear.setBlock(index, getBlockState(ordinal));
                        }
                    }
                } else {
                    if (palette.length < 128) {
                        for (int y = 0; y < height; y++) {
                            for (int z = 0; z < length; z++) {
                                for (int x = 0; x < width; x++) {
                                    int ordinal = fis.read();
                                    clipboard.setBlock(x, y, z, getBlockState(ordinal));
                                }
                            }
                        }
                    } else {
                        for (int y = 0; y < height; y++) {
                            for (int z = 0; z < length; z++) {
                                for (int x = 0; x < width; x++) {
                                    int ordinal = fis.readVarInt();
                                    clipboard.setBlock(x, y, z, getBlockState(ordinal));
                                }
                            }
                        }
                    }
                }
            }
        }
        if (biomesOut != null && biomesOut.getSize() != 0) {
            try (FaweInputStream fis = new FaweInputStream(new LZ4BlockInputStream(new FastByteArraysInputStream(biomesOut.toByteArrays())))) {
                if (clipboard instanceof LinearClipboard) {
                    LinearClipboard linear = (LinearClipboard) clipboard;
                    int volume = width * length;
                    for (int index = 0; index < volume; index++) {
                        BiomeType biome = getBiomeType(fis);
                        linear.setBiome(index, biome);
                    }
                } else {
                    for (int z = 0; z < length; z++) {
                        for (int x = 0; x < width; x++) {
                            BiomeType biome = getBiomeType(fis);
                            clipboard.setBiome(x, 0, z, biome);
                        }
                    }
                }
            }
        }
        // tiles
        if (tiles != null && !tiles.isEmpty()) {
            for (Map<String, Object> tileRaw : tiles) {
                CompoundTag tile = FaweCache.IMP.asTag(tileRaw);

                int[] pos = tile.getIntArray("Pos");
                int x;
                int y;
                int z;
                if (pos.length != 3) {
                    if (!tile.containsKey("x") || !tile.containsKey("y") || !tile.containsKey("z")) {
                        return null;
                    }
                    x = tile.getInt("x");
                    y = tile.getInt("y");
                    z = tile.getInt("z");
                } else {
                    x = pos[0];
                    y = pos[1];
                    z = pos[2];
                }
                Map<String, Tag> values = tile.getValue();
                Tag id = values.get("Id");
                if (id != null) {
                    values.put("x", new IntTag(x));
                    values.put("y", new IntTag(y));
                    values.put("z", new IntTag(z));
                    values.put("id", id);
                }
                values.remove("Id");
                values.remove("Pos");

                tile = fixBlockEntity(tile);
                clipboard.setTile(x, y, z, tile);
            }
        }

        // entities
        if (entities != null && !entities.isEmpty()) {
            for (Map<String, Object> entRaw : entities) {
                CompoundTag ent = FaweCache.IMP.asTag(entRaw);

                Map<String, Tag> value = ent.getValue();
                StringTag id = (StringTag) value.get("Id");
                if (id == null) {
                    id = (StringTag) value.get("id");
                    if (id == null) {
                        return null;
                    }
                }
                value.put("id", id);
                value.remove("Id");

                EntityType type = EntityTypes.parse(id.getValue());
                if (type != null) {
                    ent = fixEntity(ent);
                    BaseEntity state = new BaseEntity(type, ent);
                    Location loc = ent.getEntityLocation(clipboard);
                    clipboard.createEntity(loc, state);
                } else {
                    log.debug("Invalid entity: " + id);
                }
            }
        }
        clipboard.setOrigin(origin);

        if (!min.equals(BlockVector3.ZERO)) {
            new BlockArrayClipboard(clipboard, min);
        }

        return clipboard;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
