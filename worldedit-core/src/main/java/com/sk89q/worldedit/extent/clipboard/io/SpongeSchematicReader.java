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
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.jnbt.streamer.InfoReader;
import com.boydti.fawe.jnbt.streamer.IntValueReader;
import com.boydti.fawe.jnbt.streamer.StreamDelegate;
import com.boydti.fawe.jnbt.streamer.ValueReader;
import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.clipboard.CPUOptimizedClipboard;
import com.boydti.fawe.object.clipboard.DiskOptimizedClipboard;
import com.boydti.fawe.object.clipboard.LinearClipboard;
import com.boydti.fawe.object.clipboard.MemoryOptimizedClipboard;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.object.io.FastByteArraysInputStream;
import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
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
import com.sk89q.worldedit.world.block.BlockTypesCache;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extension.platform.Capability;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import com.sk89q.worldedit.world.storage.NBTConversions;
import java.util.OptionalInt;

/**
 * Reads schematic files using the Sponge Schematic Specification.
 */
public class SpongeSchematicReader extends NBTSchematicReader {

    private static final Logger log = LoggerFactory.getLogger(SpongeSchematicReader.class);
    private final NBTInputStream inputStream;
    private DataFixer fixer = null;
    private int dataVersion = -1;

    private FastByteArrayOutputStream blocksOut;
    private FaweOutputStream blocks;

    private FastByteArrayOutputStream biomesOut;
    private FaweOutputStream biomes;

    private List<Map<String, Object>> tiles;
    private List<Map<String, Object>> entities;

    private int width, height, length;
    private int offsetX, offsetY, offsetZ;
    private char[] palette, biomePalette;
    private BlockVector3 min = BlockVector3.ZERO;
    private int schematicVersion = -1;


    /**
     * Create a new instance.
     *
     * @param inputStream the input stream to read from
     */
    public SpongeSchematicReader(NBTInputStream inputStream) {
        checkNotNull(inputStream);
        this.inputStream = inputStream;
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

    public StreamDelegate createDelegate() {
        StreamDelegate root = new StreamDelegate();
        StreamDelegate schematic = root.add("Schematic");
        schematic.add("DataVersion").withInt((i, v) -> dataVersion = v);
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
                    System.out.println("Read " + palettePart);
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
        blockData.withInt((index, value) -> blocks.writeVarInt(value));
        StreamDelegate tilesDelegate = schematic.add("TileEntities");
        tilesDelegate.withInfo((length, type) -> tiles = new ArrayList<>(length));
        tilesDelegate.withElem((ValueReader<Map<String, Object>>) (index, tile) -> tiles.add(tile));

        StreamDelegate entitiesDelegate = schematic.add("Entities");
        entitiesDelegate.withInfo((length, type) -> entities = new ArrayList<>(length));
        entitiesDelegate.withElem((ValueReader<Map<String, Object>>) (index, entity) -> entities.add(entity));
        StreamDelegate biomeData = schematic.add("BiomeData");
        biomeData.withInfo((length, type) -> {
            biomesOut = new FastByteArrayOutputStream();
            biomes = new FaweOutputStream(new LZ4BlockOutputStream(blocksOut));
        });
        biomeData.withElem((IntValueReader) (index, value) -> {
            try {
                biomes.write(value); // byte of varInt
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        StreamDelegate biomePaletteDelegate = schematic.add("BiomePalette");
        biomePaletteDelegate.withInfo((length, type) -> biomePalette = new char[length]);
        biomePaletteDelegate.withElem(new ValueReader<Map.Entry<String, Number>>() {
            @Override
            public void apply(int index, Map.Entry<String, Number> palettePart) {
                String key = palettePart.getKey();
                if (fixer != null) {
                    key = fixer.fixUp(DataFixer.FixTypes.BIOME, key, dataVersion);
                }
                BiomeType biome = BiomeTypes.get(key);
                if (biome == null) {
                    System. out.println("Unknown biome " + key);
                    biome = BiomeTypes.FOREST;
                }
                int paletteIndex = palettePart.getValue().intValue();
                biomePalette[paletteIndex] = (char) biome.getInternalId();
            }
        });
        return root;
    }

    private BlockState getBlockState(int id) {
        return BlockTypesCache.states[palette[id]];
    }

    private BiomeType getBiomeType(FaweInputStream fis) throws IOException {
        char biomeId = biomePalette[fis.readVarInt()];
        BiomeType biome = BiomeTypes.get(biomeId);
        return biome;
    }

    @Override
    public Clipboard read() throws IOException {
        CompoundTag schematicTag = getBaseTag();
        Map<String, Tag> schematic = schematicTag.getValue();

        final Platform platform = WorldEdit.getInstance().getPlatformManager()
                .queryCapability(Capability.WORLD_EDITING);
        int liveDataVersion = platform.getDataVersion();

        if (schematicVersion == 1) {
            dataVersion = 1631; // this is a relatively safe assumption unless someone imports a schematic from 1.12, e.g. sponge 7.1-
            fixer = platform.getDataFixer();
            return readVersion1(schematicTag);
        } else if (schematicVersion == 2) {
            dataVersion = requireTag(schematic, "DataVersion", IntTag.class).getValue();
            if (dataVersion > liveDataVersion) {
                log.warn("Schematic was made in a newer Minecraft version ({} > {}). Data may be incompatible.",
                        dataVersion, liveDataVersion);
            } else if (dataVersion < liveDataVersion) {
                fixer = platform.getDataFixer();
                if (fixer != null) {
                    log.debug("Schematic was made in an older Minecraft version ({} < {}), will attempt DFU.",
                            dataVersion, liveDataVersion);
                } else {
                    log.info("Schematic was made in an older Minecraft version ({} < {}), but DFU is not available. Data may be incompatible.",
                            dataVersion, liveDataVersion);
                }
            }

            BlockArrayClipboard clip = readVersion1(schematicTag);
            return readVersion2(clip, schematicTag);
        }
        throw new IOException("This schematic version is currently not supported");
    }

    @Override
    public OptionalInt getDataVersion() {
        try {
            CompoundTag schematicTag = getBaseTag();
            Map<String, Tag> schematic = schematicTag.getValue();
            if (schematicVersion == 1) {
                return OptionalInt.of(1631);
            } else if (schematicVersion == 2) {
                return OptionalInt.of(requireTag(schematic, "DataVersion", IntTag.class).getValue());
            }
            return OptionalInt.empty();
        } catch (IOException e) {
            return OptionalInt.empty();
        }
    }

    @Override
    public Clipboard getBaseTag(UUID uuid, Function<BlockVector3, Clipboard> createOutput) throws IOException {
        StreamDelegate root = createDelegate();
        inputStream.readNamedTagLazy(root);
        if (blocks != null) blocks.close();
        if (biomes != null) biomes.close();
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
                            linear.setBlock(index, getBlockState(fis.read()));
                        }
                    } else {
                        for (int index = 0; index < volume; index++) {
                            linear.setBlock(index, getBlockState(fis.readVarInt()));
                        }
                    }
                } else {
                    if (palette.length < 128) {
                        for (int y = 0; y < height; y++) {
                            for (int z = 0; z < length; z++) {
                                for (int x = 0; x < width; x++) {
                                    clipboard.setBlock(x, y, z, getBlockState(fis.read()));
                                }
                            }
                        }
                    } else {
                        for (int y = 0; y < height; y++) {
                            for (int z = 0; z < length; z++) {
                                for (int x = 0; x < width; x++) {
                                    clipboard.setBlock(x, y, z, getBlockState(fis.readVarInt()));
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
                        linear.setBiome(index, getBiomeType(fis));
                    }
                } else {
                    for (int z = 0; z < length; z++) {
                        for (int x = 0; x < width; x++) {
                            clipboard.setBiome(x, 0, z, getBiomeType(fis));
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
                int x,y,z;
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

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
