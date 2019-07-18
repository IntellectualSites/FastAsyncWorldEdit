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

import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.boydti.fawe.util.IOUtil;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.Maps;
import com.sk89q.jnbt.*;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Writes schematic files using the Sponge schematic format.
 */
public class SpongeSchematicWriter implements ClipboardWriter {

    private static final int CURRENT_VERSION = 2;

    private static final int MAX_SIZE = Short.MAX_VALUE - Short.MIN_VALUE;
    private final NBTOutputStream outputStream;

    /**
     * Create a new schematic writer.
     *
     * @param outputStream the output stream to write to
     */
    public SpongeSchematicWriter(NBTOutputStream outputStream) {
        checkNotNull(outputStream);
        this.outputStream = outputStream;
    }

    @Override
    public void write(Clipboard clipboard) throws IOException {
        // For now always write the latest version. Maybe provide support for earlier if more appear.
        write2(clipboard);
    }

    /**
     * Writes a version 2 schematic file.
     *
     * @param clipboard The clipboard
     */
    private void write2(Clipboard clipboard) throws IOException {
        Region region = clipboard.getRegion();
        BlockVector3 origin = clipboard.getOrigin();
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 offset = min.subtract(origin);
        int width = region.getWidth();
        int height = region.getHeight();
        int length = region.getLength();

        if (width > MAX_SIZE) {
            throw new IllegalArgumentException("Width of region too large for a .schematic");
        }
        if (height > MAX_SIZE) {
            throw new IllegalArgumentException("Height of region too large for a .schematic");
        }
        if (length > MAX_SIZE) {
            throw new IllegalArgumentException("Length of region too large for a .schematic");
        }

        final DataOutput rawStream = outputStream.getOutputStream();
        outputStream.writeLazyCompoundTag("Schematic", out -> {
            out.writeNamedTag("DataVersion", WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).getDataVersion());
            out.writeNamedTag("Version", CURRENT_VERSION);
            out.writeNamedTag("Width", (short) width);
            out.writeNamedTag("Height", (short) height);
            out.writeNamedTag("Length", (short) length);

            // The Sponge format Offset refers to the 'min' points location in the world. That's our 'Origin'
            out.writeNamedTag("Offset", new int[]{
                    min.getBlockX(),
                    min.getBlockY(),
                    min.getBlockZ(),
            });

            out.writeLazyCompoundTag("Metadata", out1 -> {
                out1.writeNamedTag("WEOffsetX", offset.getBlockX());
                out1.writeNamedTag("WEOffsetY", offset.getBlockY());
                out1.writeNamedTag("WEOffsetZ", offset.getBlockZ());
            });

            ByteArrayOutputStream blocksCompressed = new ByteArrayOutputStream();
            DataOutputStream blocksOut = new DataOutputStream(new LZ4BlockOutputStream(blocksCompressed));

            ByteArrayOutputStream tilesCompressed = new ByteArrayOutputStream();
            NBTOutputStream tilesOut = new NBTOutputStream(new LZ4BlockOutputStream(tilesCompressed));

            List<Integer> paletteList = new ArrayList<>();
            char[] palette = new char[BlockTypes.states.length];
            Arrays.fill(palette, Character.MAX_VALUE);
            int[] paletteMax = {0};


            int[] numTiles = {0};
            FaweClipboard.BlockReader reader = new FaweClipboard.BlockReader() {
                @Override
                public <B extends BlockStateHolder<B>> void run(int x, int y, int z, B block) {
                    try {
                        if (block.hasNbtData()) {
                            CompoundTag nbt = block.getNbtData();
                            if (nbt != null) {
                                Map<String, Tag> values = nbt.getValue();

                                values.remove("id"); // Remove 'id' if it exists. We want 'Id'

                                // Positions are kept in NBT, we don't want that.
                                values.remove("x");
                                values.remove("y");
                                values.remove("z");
                                if (!values.containsKey("Id")) {
                                    values.put("Id", new StringTag(block.getNbtId()));
                                }
                                values.put("Pos", new IntArrayTag(new int[]{
                                        x,
                                        y,
                                        z
                                }));
                                numTiles[0]++;
                                tilesOut.writeTagPayload(block.getNbtData());
                            }
                        }
                        int ordinal = block.getOrdinal();
                        char value = palette[ordinal];
                        if (value == Character.MAX_VALUE) {
                            int size = paletteMax[0]++;
                            palette[ordinal] = value = (char) size;
                            paletteList.add(ordinal);
                        }
                        IOUtil.writeVarInt(blocksOut, value);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            if (clipboard instanceof BlockArrayClipboard) {
                ((BlockArrayClipboard) clipboard).IMP.forEach(reader, true);
            } else {
                for (BlockVector3 pt : region) {
                    BaseBlock block = clipboard.getFullBlock(pt);
                    int x = pt.getBlockX() - min.getBlockX();
                    int y = pt.getBlockY() - min.getBlockY();
                    int z = pt.getBlockZ() - min.getBlockY();
                    reader.run(x, y, z, block);
                }
            }
            // close
            tilesOut.close();
            blocksOut.close();

            out.writeNamedTag("PaletteMax", paletteMax[0]);

            out.writeLazyCompoundTag("Palette", out12 -> {
                for (int i = 0; i < paletteList.size(); i++) {
                    int stateOrdinal = paletteList.get(i);
                    BlockState state = BlockTypes.states[stateOrdinal];
                    out12.writeNamedTag(state.getAsString(), i);
                }
            });

            out.writeNamedTagName("BlockData", NBTConstants.TYPE_BYTE_ARRAY);
            rawStream.writeInt(blocksOut.size());
            try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(blocksCompressed.toByteArray()))) {
                IOUtil.copy(in, rawStream);
            }

            if (numTiles[0] != 0) {
                out.writeNamedTagName("TileEntities", NBTConstants.TYPE_LIST);
                rawStream.write(NBTConstants.TYPE_COMPOUND);
                rawStream.writeInt(numTiles[0]);
                try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(tilesCompressed.toByteArray()))) {
                    IOUtil.copy(in, rawStream);
                }
            } else {
                out.writeNamedEmptyList("TileEntities");
            }

            if (clipboard.hasBiomes()) {
                writeBiomes(clipboard, out);
            }

            List<Tag> entities = new ArrayList<>();
            for (Entity entity : clipboard.getEntities()) {
                BaseEntity state = entity.getState();

                if (state != null) {
                    Map<String, Tag> values = new HashMap<>();

                    // Put NBT provided data
                    CompoundTag rawTag = state.getNbtData();
                    if (rawTag != null) {
                        values.putAll(rawTag.getValue());
                    }

                    // Store our location data, overwriting any
                    values.remove("id");
                    values.put("Id", new StringTag(state.getType().getId()));
                    values.put("Pos", writeVector(entity.getLocation()));
                    values.put("Rotation", writeRotation(entity.getLocation()));

                    CompoundTag entityTag = new CompoundTag(values);
                    entities.add(entityTag);
                }
            }
            if (entities.isEmpty()) {
                out.writeNamedEmptyList("Entities");
            } else {
                out.writeNamedTag("Entities", new ListTag(CompoundTag.class, entities));
            }
        });
    }

    private void writeBiomes(Clipboard clipboard, NBTOutputStream out) throws IOException {
        ByteArrayOutputStream biomesCompressed = new ByteArrayOutputStream();
        DataOutputStream biomesOut = new DataOutputStream(new LZ4BlockOutputStream(biomesCompressed));

        List<Integer> paletteList = new ArrayList<>();
        int[] palette = new int[BiomeTypes.getMaxId() + 1];
        Arrays.fill(palette, Integer.MAX_VALUE);
        int[] paletteMax = {0};
        NBTStreamer.ByteReader task = new NBTStreamer.ByteReader() {
            @Override
            public void run(int index, int ordinal) {
                try {
                    int value = palette[ordinal];
                    if (value == Integer.MAX_VALUE) {
                        int size = paletteMax[0]++;
                        palette[ordinal] = value = size;
                        paletteList.add(ordinal);
                    }
                    IOUtil.writeVarInt(biomesOut, value);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        if (clipboard instanceof BlockArrayClipboard) {
            ((BlockArrayClipboard) clipboard).IMP.streamBiomes(task);
        } else {
            BlockVector3 min = clipboard.getMinimumPoint();
            int width = clipboard.getRegion().getWidth();
            int length = clipboard.getRegion().getLength();
            for (int z = 0, i = 0; z < length; z++) {
                int z0 = min.getBlockZ() + z;
                for (int x = 0; x < width; x++, i++) {
                    int x0 = min.getBlockX() + x;
                    BlockVector2 pt = BlockVector2.at(x0, z0);
                    BiomeType biome = clipboard.getBiome(pt);
                    task.run(i, biome.getInternalId());
                }
            }
        }
        biomesOut.close();

        out.writeNamedTag("BiomePaletteMax", paletteMax[0]);

        out.writeLazyCompoundTag("BiomePalette", out12 -> {
            for (int i = 0; i < paletteList.size(); i++) {
                int ordinal = paletteList.get(i);
                BiomeType state = BiomeTypes.get(ordinal);
                out12.writeNamedTag(state.getId(), i);
            }
        });

        out.writeNamedTagName("BiomeData", NBTConstants.TYPE_BYTE_ARRAY);
        out.writeInt(biomesOut.size());
        try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(biomesCompressed.toByteArray()))) {
            IOUtil.copy(in, (DataOutput) out);
        }
    }

    private void writeEntities(Clipboard clipboard, NBTOutputStream schematic) throws IOException {
        List<CompoundTag> entities = clipboard.getEntities().stream().map(e -> {
            BaseEntity state = e.getState();
            if (state == null) {
                return null;
            }
            Map<String, Tag> values = Maps.newHashMap();
            CompoundTag rawData = state.getNbtData();
            if (rawData != null) {
                values.putAll(rawData.getValue());
            }
            values.remove("id");
            values.put("Id", new StringTag(state.getType().getId()));
            values.put("Pos", writeVector(e.getLocation().toVector()));
            values.put("Rotation", writeRotation(e.getLocation()));

            return new CompoundTag(values);
        }).filter(Objects::nonNull).collect(Collectors.toList());
        if (entities.isEmpty()) {
            return;
        }
        schematic.writeNamedTag("Entities", new ListTag(CompoundTag.class, entities));
    }

    private static Tag writeVector(Vector3 vector) {
        List<DoubleTag> list = new ArrayList<>();
        list.add(new DoubleTag(vector.getX()));
        list.add(new DoubleTag(vector.getY()));
        list.add(new DoubleTag(vector.getZ()));
        return new ListTag(DoubleTag.class, list);
    }

    private static Tag writeRotation(Location location) {
        List<FloatTag> list = new ArrayList<>();
        list.add(new FloatTag(location.getYaw()));
        list.add(new FloatTag(location.getPitch()));
        return new ListTag(FloatTag.class, list);
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }
}
