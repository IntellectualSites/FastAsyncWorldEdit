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

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.clipboard.CPUOptimizedClipboard;
import com.boydti.fawe.object.clipboard.DiskOptimizedClipboard;
import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.boydti.fawe.object.clipboard.MemoryOptimizedClipboard;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.object.io.FastByteArraysInputStream;
import com.boydti.fawe.util.IOUtil;
import com.google.common.collect.Maps;
import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntArrayTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NamedTag;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.legacycompat.NBTCompatibilityHandler;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reads schematic files using the Sponge Schematic Specification.
 */
public class SpongeSchematicReader extends NBTSchematicReader {

    private static final List<NBTCompatibilityHandler> COMPATIBILITY_HANDLERS = new ArrayList<>();

    static {
        // If NBT Compat handlers are needed - add them here.
    }

    private static final Logger log = Logger.getLogger(SpongeSchematicReader.class.getCanonicalName());
    private final NBTInputStream inputStream;

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
        return readVersion1(uuid);
    }
//    private Clipboard readVersion1(Map<String, Tag> schematic) throws IOException {
//        BlockVector3 origin;
//        Region region;
//
//        Map<String, Tag> metadata = requireTag(schematic, "Metadata", CompoundTag.class).getValue();
//    }
    private int width, height, length;
    private int offsetX, offsetY, offsetZ;
    private char[] palette;
    private BlockVector3 min;
    private FaweClipboard fc;

    private FaweClipboard setupClipboard(int size, UUID uuid) {
        if (fc != null) {
            if (fc.getDimensions().getX() == 0) {
                fc.setDimensions(BlockVector3.at(size, 1, 1));
            }
            return fc;
        }
//<<<<<<< HEAD
        if (Settings.IMP.CLIPBOARD.USE_DISK) {
            return fc = new DiskOptimizedClipboard(size, 1, 1, uuid);
        } else if (Settings.IMP.CLIPBOARD.COMPRESSION_LEVEL == 0) {
            return fc = new CPUOptimizedClipboard(size, 1, 1);
//=======
//
//        BlockVector3 min = BlockVector3.at(offsetParts[0], offsetParts[1], offsetParts[2]);
//
//        if (metadata.containsKey("WEOffsetX")) {
//            // We appear to have WorldEdit Metadata
//            int offsetX = requireTag(metadata, "WEOffsetX", IntTag.class).getValue();
//            int offsetY = requireTag(metadata, "WEOffsetY", IntTag.class).getValue();
//            int offsetZ = requireTag(metadata, "WEOffsetZ", IntTag.class).getValue();
//            BlockVector3 offset = BlockVector3.at(offsetX, offsetY, offsetZ);
//            origin = min.subtract(offset);
//            region = new CuboidRegion(min, min.add(width, height, length).subtract(BlockVector3.ONE));
//>>>>>>> 2c8b2fe0... Move vectors to static creators, for caching
        } else {
            return fc = new MemoryOptimizedClipboard(size, 1, 1);
        }
    }
//        BlockVector3 min = new BlockVector3(offsetParts[0], offsetParts[1], offsetParts[2]);
//
//        if (metadata.containsKey("WEOffsetX")) {
//            // We appear to have WorldEdit Metadata
//            int offsetX = requireTag(metadata, "WEOffsetX", IntTag.class).getValue();
//            int offsetY = requireTag(metadata, "WEOffsetY", IntTag.class).getValue();
//            int offsetZ = requireTag(metadata, "WEOffsetZ", IntTag.class).getValue();
//            BlockVector3 offset = new BlockVector3(offsetX, offsetY, offsetZ);
//            origin = min.subtract(offset);
//            region = new CuboidRegion(min, min.add(width, height, length).subtract(BlockVector3.ONE));
//        } else {
//            origin = min;
//            region = new CuboidRegion(origin, origin.add(width, height, length).subtract(BlockVector3.ONE));
//        }
//
//        int paletteMax = requireTag(schematic, "PaletteMax", IntTag.class).getValue();
//        Map<String, Tag> paletteObject = requireTag(schematic, "Palette", CompoundTag.class).getValue();
//        if (paletteObject.size() != paletteMax) {
//            throw new IOException("Differing given palette size to actual size");
//>>>>>>> 399e0ad5... Refactor vector system to be cleaner

//    private Clipboard readVersion1(UUID uuid) throws IOException {
//        width = height = length = offsetX = offsetY = offsetZ = Integer.MIN_VALUE;
//
//        final BlockArrayClipboard clipboard = new BlockArrayClipboard(new CuboidRegion(new Vector(0, 0, 0), new Vector(0, 0, 0)), fc);
//        FastByteArrayOutputStream blocksOut = new FastByteArrayOutputStream();
//        FastByteArrayOutputStream biomesOut = new FastByteArrayOutputStream();
//
//        NBTStreamer streamer = new NBTStreamer(inputStream);
//        streamer.addReader("Schematic.Width", (BiConsumer<Integer, Short>) (i, v) -> width = v);
//        streamer.addReader("Schematic.Height", (BiConsumer<Integer, Short>) (i, v) -> height = v);
//        streamer.addReader("Schematic.Length", (BiConsumer<Integer, Short>) (i, v) -> length = v);
//        streamer.addReader("Schematic.Offset", (BiConsumer<Integer, int[]>) (i, v) -> min = new BlockVector(v[0], v[1], v[2]));
//        streamer.addReader("Schematic.Metadata.WEOffsetX", (BiConsumer<Integer, Integer>) (i, v) -> offsetX = v);
//        streamer.addReader("Schematic.Metadata.WEOffsetY", (BiConsumer<Integer, Integer>) (i, v) -> offsetY = v);
//        streamer.addReader("Schematic.Metadata.WEOffsetZ", (BiConsumer<Integer, Integer>) (i, v) -> offsetZ = v);
//        streamer.addReader("Schematic.Palette", (BiConsumer<Integer, HashMap<String, Tag>>) (i, v) -> {
//            palette = new char[v.size()];
//            for (Map.Entry<String, Tag> entry : v.entrySet()) {
//                BlockState state = BlockState.get(entry.getKey());
//                int index = ((IntTag) entry.getValue()).getValue();
//                palette[index] = (char) state.getOrdinal();
//            }
//<<<<<<< HEAD
//        });
//        streamer.addReader("Schematic.BlockData.#", new NBTStreamer.LazyReader() {
//            @Override
//            public void accept(Integer arrayLen, DataInputStream dis) {
//                try (FaweOutputStream blocks = new FaweOutputStream(new LZ4BlockOutputStream(blocksOut))) {
//                    IOUtil.copy(dis, blocks, arrayLen);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//=======
//            palette.put(id, state);
//        }
//
//        byte[] blocks = requireTag(schematic, "BlockData", ByteArrayTag.class).getValue();
//
//        Map<BlockVector3, Map<String, Tag>> tileEntitiesMap = new HashMap<>();
//        try {
//            List<Map<String, Tag>> tileEntityTags = requireTag(schematic, "TileEntities", ListTag.class).getValue().stream()
//                    .map(tag -> (CompoundTag) tag)
//                    .map(CompoundTag::getValue)
//                    .collect(Collectors.toList());
//
//            for (Map<String, Tag> tileEntity : tileEntityTags) {
//                int[] pos = requireTag(tileEntity, "Pos", IntArrayTag.class).getValue();
//                tileEntitiesMap.put(new BlockVector3(pos[0], pos[1], pos[2]), tileEntity);
//>>>>>>> 399e0ad5... Refactor vector system to be cleaner
//            }
//        });
//        streamer.addReader("Schematic.Biomes.#", new NBTStreamer.LazyReader() {
//            @Override
//            public void accept(Integer arrayLen, DataInputStream dis) {
//                try (FaweOutputStream biomes = new FaweOutputStream(new LZ4BlockOutputStream(biomesOut))) {
//                    IOUtil.copy(dis, biomes, arrayLen);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        streamer.addReader("Schematic.TileEntities.#", new BiConsumer<Integer, CompoundTag>() {
//            @Override
//            public void accept(Integer index, CompoundTag value) {
//                if (fc == null) {
//                    setupClipboard(0, uuid);
//                }
//                int[] pos = value.getIntArray("Pos");
//                int x = pos[0];
//                int y = pos[1];
//                int z = pos[2];
//                fc.setTile(x, y, z, value);
//            }
//        });
//        streamer.addReader("Schematic.Entities.#", new BiConsumer<Integer, CompoundTag>() {
//            @Override
//            public void accept(Integer index, CompoundTag compound) {
//                if (fc == null) {
//                    setupClipboard(0, uuid);
//                }
//                String id = compound.getString("id");
//                if (id.isEmpty()) {
//                    return;
//                }
//                ListTag positionTag = compound.getListTag("Pos");
//                ListTag directionTag = compound.getListTag("Rotation");
//                EntityType type = EntityTypes.parse(id);
//                if (type != null) {
//                    compound.getValue().put("Id", new StringTag(type.getId()));
//                    BaseEntity state = new BaseEntity(type, compound);
//                    fc.createEntity(clipboard, positionTag.asDouble(0), positionTag.asDouble(1), positionTag.asDouble(2), (float) directionTag.asDouble(0), (float) directionTag.asDouble(1), state);
//                } else {
//                    Fawe.debug("Invalid entity: " + id);
//                }
//            }
//<<<<<<< HEAD
//        });
//        streamer.readFully();
//        if (fc == null) setupClipboard(length * width * height, uuid);
//        Vector origin = min;
//        CuboidRegion region;
//        if (offsetX != Integer.MIN_VALUE && offsetY != Integer.MIN_VALUE  && offsetZ != Integer.MIN_VALUE) {
//            origin = origin.subtract(new Vector(offsetX, offsetY, offsetZ));
//            region = new CuboidRegion(min, min.add(width, height, length).subtract(Vector.ONE));
//        } else {
//            region = new CuboidRegion(min, min.add(width, height, length).subtract(Vector.ONE));
//        }
//        if (blocksOut.getSize() != 0) {
//            try (FaweInputStream fis = new FaweInputStream(new LZ4BlockInputStream(new FastByteArraysInputStream(blocksOut.toByteArrays())))) {
//                int volume = width * height * length;
//                if (palette.length < 128) {
//                    for (int index = 0; index < volume; index++) {
//                        BlockState state = BlockTypes.states[palette[fis.read()]];
//                        fc.setBlock(index, state);
//=======
//            // index = (y * length + z) * width + x
//            int y = index / (width * length);
//            int z = (index % (width * length)) / width;
//            int x = (index % (width * length)) % width;
//            BlockState state = palette.get(value);
//            BlockVector3 pt = new BlockVector3(x, y, z);
//            try {
//                if (tileEntitiesMap.containsKey(pt)) {
//                    Map<String, Tag> values = Maps.newHashMap(tileEntitiesMap.get(pt));
//                    for (NBTCompatibilityHandler handler : COMPATIBILITY_HANDLERS) {
//                        if (handler.isAffectedBlock(state)) {
//                            handler.updateNBT(state, values);
//                        }
//>>>>>>> 399e0ad5... Refactor vector system to be cleaner
//                    }
//                } else {
//                    for (int index = 0; index < volume; index++) {
//                        BlockState state = BlockTypes.states[palette[fis.readVarInt()]];
//                        fc.setBlock(index, state);
//                    }
//                }
//            }
//        }
//        if (biomesOut.getSize() != 0) {
//            try (FaweInputStream fis = new FaweInputStream(new LZ4BlockInputStream(new FastByteArraysInputStream(biomesOut.toByteArrays())))) {
//                int volume = width * length;
//                for (int index = 0; index < volume; index++) {
//                    fc.setBiome(index, fis.read());
//                }
//            }
//        }
//        fc.setDimensions(new Vector(width, height, length));
//        clipboard.init(region, fc);
//        clipboard.setOrigin(origin);
//        return clipboard;
//    }
    
    private Clipboard readVersion1(UUID uuid) throws IOException {
        width = height = length = offsetX = offsetY = offsetZ = Integer.MIN_VALUE;

        final BlockArrayClipboard clipboard = new BlockArrayClipboard(new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(0, 0, 0)), fc);
        FastByteArrayOutputStream blocksOut = new FastByteArrayOutputStream();
        FastByteArrayOutputStream biomesOut = new FastByteArrayOutputStream();

        NBTStreamer streamer = new NBTStreamer(inputStream);
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
                BlockState state = BlockState.get(entry.getKey());
                int index = ((IntTag) entry.getValue()).getValue();
                palette[index] = (char) state.getOrdinal();
            }
//<<<<<<< HEAD
        });
        streamer.addReader("Schematic.BlockData.#", new NBTStreamer.LazyReader() {
            @Override
            public void accept(Integer arrayLen, DataInputStream dis) {
                try (FaweOutputStream blocks = new FaweOutputStream(new LZ4BlockOutputStream(blocksOut))) {
                    IOUtil.copy(dis, blocks, arrayLen);
                } catch (IOException e) {
                    e.printStackTrace();
                }
//=======
//            palette.put(id, state);
//        }
//
//        byte[] blocks = requireTag(schematic, "BlockData", ByteArrayTag.class).getValue();
//
//        Map<BlockVector3, Map<String, Tag>> tileEntitiesMap = new HashMap<>();
//        try {
//            List<Map<String, Tag>> tileEntityTags = requireTag(schematic, "TileEntities", ListTag.class).getValue().stream()
//                    .map(tag -> (CompoundTag) tag)
//                    .map(CompoundTag::getValue)
//                    .collect(Collectors.toList());
//
//            for (Map<String, Tag> tileEntity : tileEntityTags) {
//                int[] pos = requireTag(tileEntity, "Pos", IntArrayTag.class).getValue();
//                tileEntitiesMap.put(BlockVector3.at(pos[0], pos[1], pos[2]), tileEntity);
//>>>>>>> 2c8b2fe0... Move vectors to static creators, for caching
            }
        });
        streamer.addReader("Schematic.Biomes.#", new NBTStreamer.LazyReader() {
            @Override
            public void accept(Integer arrayLen, DataInputStream dis) {
                try (FaweOutputStream biomes = new FaweOutputStream(new LZ4BlockOutputStream(biomesOut))) {
                    IOUtil.copy(dis, biomes, arrayLen);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        streamer.addReader("Schematic.TileEntities.#", new BiConsumer<Integer, CompoundTag>() {
            @Override
            public void accept(Integer index, CompoundTag value) {
                if (fc == null) {
                    setupClipboard(0, uuid);
                }
                int[] pos = value.getIntArray("Pos");
                int x = pos[0];
                int y = pos[1];
                int z = pos[2];
                fc.setTile(x, y, z, value);
            }
//<<<<<<< HEAD
        });
        streamer.addReader("Schematic.Entities.#", new BiConsumer<Integer, CompoundTag>() {
            @Override
            public void accept(Integer index, CompoundTag compound) {
                if (fc == null) {
                    setupClipboard(0, uuid);
                }
                String id = compound.getString("id");
                if (id.isEmpty()) {
                    return;
                }
                ListTag positionTag = compound.getListTag("Pos");
                ListTag directionTag = compound.getListTag("Rotation");
                EntityType type = EntityTypes.parse(id);
                if (type != null) {
                    compound.getValue().put("Id", new StringTag(type.getId()));
                    BaseEntity state = new BaseEntity(type, compound);
                    fc.createEntity(clipboard, positionTag.asDouble(0), positionTag.asDouble(1), positionTag.asDouble(2), (float) directionTag.asDouble(0), (float) directionTag.asDouble(1), state);
                } else {
                    Fawe.debug("Invalid entity: " + id);
                }
            }
        });
        streamer.readFully();
        if (fc == null) setupClipboard(length * width * height, uuid);
        BlockVector3 origin = min;
        CuboidRegion region;
        if (offsetX != Integer.MIN_VALUE && offsetY != Integer.MIN_VALUE  && offsetZ != Integer.MIN_VALUE) {
            origin = origin.subtract(BlockVector3.at(offsetX, offsetY, offsetZ));
            region = new CuboidRegion(min, min.add(width, height, length).subtract(BlockVector3.ONE));
        } else {
            region = new CuboidRegion(min, min.add(width, height, length).subtract(BlockVector3.ONE));
        }
        if (blocksOut.getSize() != 0) {
            try (FaweInputStream fis = new FaweInputStream(new LZ4BlockInputStream(new FastByteArraysInputStream(blocksOut.toByteArrays())))) {
                int volume = width * height * length;
                if (palette.length < 128) {
                    for (int index = 0; index < volume; index++) {
                        BlockState state = BlockTypes.states[palette[fis.read()]];
                        fc.setBlock(index, state);
//=======
//            // index = (y * length + z) * width + x
//            int y = index / (width * length);
//            int z = (index % (width * length)) / width;
//            int x = (index % (width * length)) % width;
//            BlockState state = palette.get(value);
//            BlockVector3 pt = BlockVector3.at(x, y, z);
//            try {
//                if (tileEntitiesMap.containsKey(pt)) {
//                    Map<String, Tag> values = Maps.newHashMap(tileEntitiesMap.get(pt));
//                    for (NBTCompatibilityHandler handler : COMPATIBILITY_HANDLERS) {
//                        if (handler.isAffectedBlock(state)) {
//                            handler.updateNBT(state, values);
//                        }
//>>>>>>> 2c8b2fe0... Move vectors to static creators, for caching
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
                    fc.setBiome(index, fis.read());
                }
            }
        }
        fc.setDimensions(BlockVector3.at(width, height, length));
        clipboard.init(region, fc);
        clipboard.setOrigin(origin);
        return clipboard;
    }


    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}