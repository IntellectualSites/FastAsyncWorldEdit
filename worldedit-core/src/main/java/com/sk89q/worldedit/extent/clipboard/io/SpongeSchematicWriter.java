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

import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.boydti.fawe.util.IOUtil;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntArrayTag;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
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
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Writes schematic files using the Sponge schematic format.
 */
public class SpongeSchematicWriter implements ClipboardWriter {

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
        write1(clipboard);
    }

    public void write1(Clipboard clipboard) throws IOException {
        // metadata
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
//<<<<<<< HEAD
        // output
        final DataOutput rawStream = outputStream.getOutputStream();
        outputStream.writeLazyCompoundTag("Schematic", out -> {
            out.writeNamedTag("Version", 1);
            out.writeNamedTag("Width",  (short) width);
            out.writeNamedTag("Height", (short) height);
            out.writeNamedTag("Length", (short) length);
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
            int[] numTiles = {0};

            List<Integer> paletteList = new ArrayList<>();
            char[] palette = new char[BlockTypes.states.length];
            Arrays.fill(palette, Character.MAX_VALUE);
            int[] paletteMax = {0};


            FaweClipboard.BlockReader reader = new FaweClipboard.BlockReader() {
                @Override
                public void run(int x, int y, int z, BlockState block) {
                    try {
                        CompoundTag tile = block.getNbtData();
                        if (tile != null) {
                            Map<String, Tag> values = tile.getValue();
                            values.remove("id"); // Remove 'id' if it exists. We want 'Id'
                            // Positions are kept in NBT, we don't want that.
                            values.remove("x");
                            values.remove("y");
                            values.remove("z");
                            if (!values.containsKey("Id")) values.put("Id", new StringTag(block.getNbtId()));
                            values.put("Pos", new IntArrayTag(new int[]{
                                    x,
                                    y,
                                    z
                            }));
                            numTiles[0]++;
                            tilesOut.writeTagPayload(tile);
//=======
//
//        Map<String, Tag> schematic = new HashMap<>();
//        schematic.put("Version", new IntTag(1));
//
//        Map<String, Tag> metadata = new HashMap<>();
//        metadata.put("WEOffsetX", new IntTag(offset.getBlockX()));
//        metadata.put("WEOffsetY", new IntTag(offset.getBlockY()));
//        metadata.put("WEOffsetZ", new IntTag(offset.getBlockZ()));
//
//        schematic.put("Metadata", new CompoundTag(metadata));
//
//        schematic.put("Width", new ShortTag((short) width));
//        schematic.put("Height", new ShortTag((short) height));
//        schematic.put("Length", new ShortTag((short) length));
//
//        // The Sponge format Offset refers to the 'min' points location in the world. That's our 'Origin'
//        schematic.put("Offset", new IntArrayTag(new int[]{
//                min.getBlockX(),
//                min.getBlockY(),
//                min.getBlockZ(),
//        }));
//
//        int paletteMax = 0;
//        Map<String, Integer> palette = new HashMap<>();
//
//        List<CompoundTag> tileEntities = new ArrayList<>();
//
//        ByteArrayOutputStream buffer = new ByteArrayOutputStream(width * height * length);
//
//        for (int y = 0; y < height; y++) {
//            int y0 = min.getBlockY() + y;
//            for (int z = 0; z < length; z++) {
//                int z0 = min.getBlockZ() + z;
//                for (int x = 0; x < width; x++) {
//                    int x0 = min.getBlockX() + x;
//                    BlockVector3 point = BlockVector3.at(x0, y0, z0);
//                    BaseBlock block = clipboard.getFullBlock(point);
//                    if (block.getNbtData() != null) {
//                        Map<String, Tag> values = new HashMap<>();
//                        for (Map.Entry<String, Tag> entry : block.getNbtData().getValue().entrySet()) {
//                            values.put(entry.getKey(), entry.getValue());
//>>>>>>> 2c8b2fe0... Move vectors to static creators, for caching
                        }
                        int ordinal = block.getOrdinal();
                        char value = palette[ordinal];
                        if (value == Character.MAX_VALUE) {
                            int size = paletteMax[0]++;
                            palette[ordinal] = value = (char) size;
                            paletteList.add(ordinal);
                        }
                        while ((value & -128) != 0) {
                            blocksOut.write(value & 127 | 128);
                            value >>>= 7;
                        }
                        blocksOut.write(value);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            if (clipboard instanceof BlockArrayClipboard) {
                ((BlockArrayClipboard) clipboard).IMP.forEach(reader, true);
            } else {
                for (BlockVector3 pt : region) {
                    BlockState block = clipboard.getBlock(pt);
                    int x = pt.getBlockX() - min.getBlockX();
                    int y = pt.getBlockY() - min.getBlockY();
                    int z = pt.getBlockZ() - min.getBlockY();
                    reader.run(x, y, z, block);
                }
            }
            // close
            tilesOut.close();
            blocksOut.close();
            // palette max
            out.writeNamedTag("PaletteMax", paletteMax[0]);
            // palette
            out.writeLazyCompoundTag("Palette", out12 -> {
                for (int i = 0; i < paletteList.size(); i++) {
                    int stateOrdinal = paletteList.get(i);
                    BlockState state = BlockTypes.states[stateOrdinal];
                    out12.writeNamedTag(state.getAsString(), i);
                }
            });
            // Block data
            out.writeNamedTagName("BlockData", NBTConstants.TYPE_BYTE_ARRAY);
            rawStream.writeInt(blocksOut.size());
            try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(blocksCompressed.toByteArray()))) {
                IOUtil.copy(in, rawStream);
            }
            // tiles
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
        });
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }
}