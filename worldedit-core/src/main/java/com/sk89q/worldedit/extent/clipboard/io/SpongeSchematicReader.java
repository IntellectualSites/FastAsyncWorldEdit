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

package com.sk89q.worldedit.extent.clipboard.io;

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.jnbt.NBTStreamer.LazyReader;
import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.clipboard.CPUOptimizedClipboard;
import com.boydti.fawe.object.clipboard.DiskOptimizedClipboard;
import com.boydti.fawe.object.clipboard.LinearClipboard;
import com.boydti.fawe.object.clipboard.MemoryOptimizedClipboard;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.object.io.FastByteArraysInputStream;
import com.boydti.fawe.util.IOUtil;
import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads schematic files using the Sponge Schematic Specification.
 */
public class SpongeSchematicReader extends NBTSchematicReader {

    private static final Logger log = LoggerFactory.getLogger(SpongeSchematicReader.class);
    private final NBTInputStream inputStream;
    private DataFixer fixer = null;
    private int dataVersion = -1;

    /**
     * Create a new instance.
     *
     * @param inputStream the input stream to read from
     */
    public SpongeSchematicReader(NBTInputStream inputStream) {
        checkNotNull(inputStream);
        this.inputStream = inputStream;
    }

    @Override
    public Clipboard read() throws IOException {
        return read(UUID.randomUUID());
    }

    @Override
    public Clipboard read(UUID uuid) throws IOException {
        return reader(uuid);
    }

    @Override
    public Clipboard read(UUID uuid, Function<BlockVector3, Clipboard> createOutput) {
        return null;
    }

    private int width, height, length;
    private int offsetX, offsetY, offsetZ;
    private char[] palette;
    private BlockVector3 min;
    private LinearClipboard fc;

    private LinearClipboard setupClipboard(int size, UUID uuid) {
        if (fc != null) {
            if (fc.getDimensions().getX() == 0) {
//                fc.setDimensions(BlockVector3.at(size, 1, 1));
            }
            return fc;
        }
        if (Settings.IMP.CLIPBOARD.USE_DISK) {
            return fc = new DiskOptimizedClipboard(BlockVector3.at(size, 1, 1), uuid);
        } else if (Settings.IMP.CLIPBOARD.COMPRESSION_LEVEL == 0) {
            return fc = new CPUOptimizedClipboard(BlockVector3.at(size, 1, 1));
        } else {
            return fc = new MemoryOptimizedClipboard(BlockVector3.at(size, 1, 1));
        }
    }

    private String fix(String palettePart) {
        if (fixer == null || dataVersion == -1) return palettePart;
        return fixer.fixUp(DataFixer.FixTypes.BLOCK_STATE, palettePart, dataVersion);
    }

    private CompoundTag fixBlockEntity(CompoundTag tag) {
        if (fixer == null || dataVersion == -1) return tag;
        return fixer.fixUp(DataFixer.FixTypes.BLOCK_ENTITY, tag, dataVersion);
    }

    private CompoundTag fixEntity(CompoundTag tag) {
        if (fixer == null || dataVersion == -1) return tag;
        return fixer.fixUp(DataFixer.FixTypes.ENTITY, tag, dataVersion);
    }

    private Clipboard reader(UUID uuid) throws IOException {
        width = height = length = offsetX = offsetY = offsetZ = Integer.MIN_VALUE;

        final BlockArrayClipboard clipboard = new BlockArrayClipboard(new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(0, 0, 0)), fc);
        FastByteArrayOutputStream blocksOut = new FastByteArrayOutputStream();
        FastByteArrayOutputStream biomesOut = new FastByteArrayOutputStream();


        NBTStreamer streamer = new NBTStreamer(inputStream);
        streamer.addReader("Schematic.DataVersion", (BiConsumer<Integer, Short>) (i, v) -> dataVersion = v);
        streamer.addReader("Schematic.Width", (BiConsumer<Integer, Short>) (i, v) -> width = v);
        streamer.addReader("Schematic.Height", (BiConsumer<Integer, Short>) (i, v) -> height = v);
        streamer.addReader("Schematic.Length", (BiConsumer<Integer, Short>) (i, v) -> length = v);
        streamer.addReader("Schematic.Offset", (BiConsumer<Integer, int[]>) (i, v) -> min = BlockVector3.at(v[0], v[1], v[2]));
        streamer.addReader("Schematic.Metadata.WEOffsetX", (BiConsumer<Integer, Integer>) (i, v) -> offsetX = v);
        streamer.addReader("Schematic.Metadata.WEOffsetY", (BiConsumer<Integer, Integer>) (i, v) -> offsetY = v);
        streamer.addReader("Schematic.Metadata.WEOffsetZ", (BiConsumer<Integer, Integer>) (i, v) -> offsetZ = v);
        streamer.addReader("Schematic.Palette", (BiConsumer<Integer, HashMap<String, Tag>>) (i, v) -> {
            palette = new char[v.size()];
            for (Map.Entry<String, Tag> entry : v.entrySet()) {
                BlockState state = null;
                try {
                    String palettePart = fix(entry.getKey());
                    state = BlockState.get(palettePart);
                } catch (InputParseException e) {
                    e.printStackTrace();
                }
                int index = ((IntTag) entry.getValue()).getValue();
                palette[index] = (char) state.getOrdinal();
            }
        });

        /// readBiomes

        streamer.addReader("Schematic.BlockData.#", (LazyReader) (arrayLen, dis) -> {
            try (FaweOutputStream blocks = new FaweOutputStream(new LZ4BlockOutputStream(blocksOut))) {
                IOUtil.copy(dis, blocks, arrayLen);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        streamer.addReader("Schematic.Biomes.#", (LazyReader) (arrayLen, dis) -> {
            try (FaweOutputStream biomes = new FaweOutputStream(new LZ4BlockOutputStream(biomesOut))) {
                IOUtil.copy(dis, biomes, arrayLen);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        streamer.addReader("Schematic.TileEntities.#", (BiConsumer<Integer, CompoundTag>) (index, value) -> {
            if (fc == null) {
                setupClipboard(0, uuid);
            }
            int[] pos = value.getIntArray("Pos");
            int x,y,z;
            if (pos.length != 3) {
                System.out.println("Invalid tile " + value);
                if (!value.containsKey("x") || !value.containsKey("y") || !value.containsKey("z")) {
                    return;
                }
                x = value.getInt("x");
                y = value.getInt("y");
                z = value.getInt("z");
            } else {
                x = pos[0];
                y = pos[1];
                z = pos[2];
            }
            Map<String, Tag> values = value.getValue();
            Tag id = values.get("Id");
            if (id != null) {
                values.put("x", new IntTag(x));
                values.put("y", new IntTag(y));
                values.put("z", new IntTag(z));
                values.put("id", id);
            }
            values.remove("Id");
            values.remove("Pos");
            value = fixBlockEntity(value);
            fc.setTile(x, y, z, value);
        });
        streamer.addReader("Schematic.Entities.#", (BiConsumer<Integer, CompoundTag>) (index, compound) -> {
            if (fc == null) {
                setupClipboard(0, uuid);
            }
            Map<String, Tag> value = compound.getValue();
            StringTag id = (StringTag) value.get("Id");
            if (id == null) {
                id = (StringTag) value.get("id");
                if (id == null) {
                    return;
                }
            } else {
                value.put("id", id);
                value.remove("Id");
            }

            EntityType type = EntityTypes.parse(id.getValue());
            if (type != null) {
                compound = fixEntity(compound);
                BaseEntity state = new BaseEntity(type, compound);
                Location loc = compound.getEntityLocation(fc);
                fc.createEntity(loc, state);
            } else {
                Fawe.debug("Invalid entity: " + id);
            }
        });
        streamer.readFully();
        if (fc == null) setupClipboard(length * width * height, uuid);
//        fc.setDimensions(BlockVector3.at(width, height, length));
        BlockVector3 origin = min;
        CuboidRegion region;
        if (offsetX != Integer.MIN_VALUE && offsetY != Integer.MIN_VALUE  && offsetZ != Integer.MIN_VALUE) {
            origin = origin.subtract(BlockVector3.at(offsetX, offsetY, offsetZ));
        }
        region = new CuboidRegion(min, min.add(width, height, length).subtract(BlockVector3.ONE));
        if (blocksOut.getSize() != 0) {
            try (FaweInputStream fis = new FaweInputStream(new LZ4BlockInputStream(new FastByteArraysInputStream(blocksOut.toByteArrays())))) {
                int volume = width * height * length;
                if (palette.length < 128) {
                    for (int index = 0; index < volume; index++) {
                        BlockState state = BlockTypes.states[palette[fis.read()]];
                        fc.setBlock(index, state);
                    }
                } else {
                    for (int index = 0; index < volume; index++) {
                        BlockState state = BlockTypes.states[palette[fis.readVarInt()]];
                        fc.setBlock(index, state);
                    }
                }
            }
        }
        if (biomesOut.getSize() != 0) {
            try (FaweInputStream fis = new FaweInputStream(new LZ4BlockInputStream(new FastByteArraysInputStream(biomesOut.toByteArrays())))) {
                int volume = width * length;
                for (int index = 0; index < volume; index++) {
                    fc.setBiome(index, BiomeTypes.get(fis.read()));
                }
            }
        }
//        clipboard.init(region, fc);
        clipboard.setOrigin(origin);
        return clipboard;
    }

    /*
    private Clipboard readVersion2(BlockArrayClipboard version1, CompoundTag schematicTag) throws IOException {
        Map<String, Tag> schematic = schematicTag.getValue();
        if (schematic.containsKey("BiomeData")) {
            readBiomes(version1, schematic);
        }
        if (schematic.containsKey("Entities")) {
            readEntities(version1, schematic);
        }
        return version1;
    }
    */

    private void readBiomes(BlockArrayClipboard clipboard, Map<String, Tag> schematic) throws IOException {
        ByteArrayTag dataTag = requireTag(schematic, "BiomeData", ByteArrayTag.class);
        IntTag maxTag = requireTag(schematic, "BiomePaletteMax", IntTag.class);
        CompoundTag paletteTag = requireTag(schematic, "BiomePalette", CompoundTag.class);

        Map<Integer, BiomeType> palette = new HashMap<>();
        if (maxTag.getValue() != paletteTag.getValue().size()) {
            throw new IOException("Biome palette size does not match expected size.");
        }

        for (Entry<String, Tag> palettePart : paletteTag.getValue().entrySet()) {
            String key = palettePart.getKey();
            if (fixer != null) {
                key = fixer.fixUp(DataFixer.FixTypes.BIOME, key, dataVersion);
            }
            BiomeType biome = BiomeTypes.get(key);
            if (biome == null) {
                log.warn("Unknown biome type :" + key +
                        " in palette. Are you missing a mod or using a schematic made in a newer version of Minecraft?");
            }
            Tag idTag = palettePart.getValue();
            if (!(idTag instanceof IntTag)) {
                throw new IOException("Biome mapped to non-Int tag.");
            }
            palette.put(((IntTag) idTag).getValue(), biome);
        }

        int width = clipboard.getDimensions().getX();

        byte[] biomes = dataTag.getValue();
        int biomeIndex = 0;
        int biomeJ = 0;
        int bVal;
        int varIntLength;
        BlockVector2 min = clipboard.getMinimumPoint().toBlockVector2();
        while (biomeJ < biomes.length) {
            bVal = 0;
            varIntLength = 0;

            while (true) {
                bVal |= (biomes[biomeJ] & 127) << (varIntLength++ * 7);
                if (varIntLength > 5) {
                    throw new IOException("VarInt too big (probably corrupted data)");
                }
                if (((biomes[biomeJ] & 128) != 128)) {
                    biomeJ++;
                    break;
                }
                biomeJ++;
            }
            int z = biomeIndex / width;
            int x = biomeIndex % width;
            BiomeType type = palette.get(bVal);
            clipboard.setBiome(min.add(x, z), type);
            biomeIndex++;
        }
    }

    /*
    private void readEntities(BlockArrayClipboard clipboard, Map<String, Tag> schematic) throws IOException {
        List<Tag> entList = requireTag(schematic, "Entities", ListTag.class).getValue();
        if (entList.isEmpty()) {
            return;
        }
        for (Tag et : entList) {
            if (!(et instanceof CompoundTag)) {
                continue;
            }
            CompoundTag entityTag = (CompoundTag) et;
            Map<String, Tag> tags = entityTag.getValue();
            String id = requireTag(tags, "Id", StringTag.class).getValue();
            entityTag = entityTag.createBuilder().putString("id", id).remove("Id").build();

            if (fixer != null) {
                entityTag = fixer.fixUp(DataFixer.FixTypes.ENTITY, entityTag, dataVersion);
            }

            EntityType entityType = EntityTypes.get(id);
            if (entityType != null) {
                Location location = NBTConversions.toLocation(clipboard,
                        requireTag(tags, "Pos", ListTag.class),
                        requireTag(tags, "Rotation", ListTag.class));
                BaseEntity state = new BaseEntity(entityType, entityTag);
                clipboard.createEntity(location, state);
            } else {
                log.warn("Unknown entity when pasting schematic: " + id);
            }
        }
    }
    */
    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
