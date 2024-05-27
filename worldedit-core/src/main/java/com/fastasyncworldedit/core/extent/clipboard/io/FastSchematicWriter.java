package com.fastasyncworldedit.core.extent.clipboard.io;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.function.visitor.Order;
import com.fastasyncworldedit.core.internal.io.FaweOutputStream;
import com.fastasyncworldedit.core.jnbt.streamer.IntValueReader;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.util.IOUtil;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntArrayTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Writes schematic files using the Sponge schematic format.
 */
public class FastSchematicWriter implements ClipboardWriter {

    private static final int CURRENT_VERSION = 2;

    private static final int MAX_SIZE = Short.MAX_VALUE - Short.MIN_VALUE;
    private final NBTOutputStream outputStream;
    private boolean brokenEntities = false;

    /**
     * Create a new schematic writer.
     *
     * @param outputStream the output stream to write to
     */
    public FastSchematicWriter(NBTOutputStream outputStream) {
        checkNotNull(outputStream);
        this.outputStream = outputStream;
    }

    public void setBrokenEntities(boolean brokenEntities) {
        this.brokenEntities = brokenEntities;
    }

    @Override
    public void write(Clipboard clipboard) throws IOException {
        clipboard.flush();
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
            out.writeNamedTag(
                    "DataVersion",
                    WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).getDataVersion()
            );
            out.writeNamedTag("Version", CURRENT_VERSION);
            out.writeNamedTag("Width", (short) width);
            out.writeNamedTag("Height", (short) height);
            out.writeNamedTag("Length", (short) length);

            // The Sponge format Offset refers to the 'min' points location in the world. That's our 'Origin'
            out.writeNamedTag("Offset", new int[]{
                    min.x(),
                    min.y(),
                    min.z(),
            });

            out.writeLazyCompoundTag("Metadata", out1 -> {
                out1.writeNamedTag("WEOffsetX", offset.x());
                out1.writeNamedTag("WEOffsetY", offset.y());
                out1.writeNamedTag("WEOffsetZ", offset.z());
                out1.writeNamedTag("FAWEVersion", Fawe.instance().getVersion().build);
            });

            ByteArrayOutputStream blocksCompressed = new ByteArrayOutputStream();
            FaweOutputStream blocksOut = new FaweOutputStream(new DataOutputStream(new LZ4BlockOutputStream(blocksCompressed)));

            ByteArrayOutputStream tilesCompressed = new ByteArrayOutputStream();
            NBTOutputStream tilesOut = new NBTOutputStream(new LZ4BlockOutputStream(tilesCompressed));

            List<Integer> paletteList = new ArrayList<>();
            char[] palette = new char[BlockTypesCache.states.length];
            Arrays.fill(palette, Character.MAX_VALUE);
            int paletteMax = 0;
            int numTiles = 0;
            Clipboard finalClipboard;
            if (clipboard instanceof BlockArrayClipboard) {
                finalClipboard = ((BlockArrayClipboard) clipboard).getParent();
            } else {
                finalClipboard = clipboard;
            }
            Iterator<BlockVector3> iterator = finalClipboard.iterator(Order.YZX);
            while (iterator.hasNext()) {
                BlockVector3 pos = iterator.next();
                BaseBlock block = pos.getFullBlock(finalClipboard);
                CompoundTag nbt = block.getNbtData();
                if (nbt != null) {
                    Map<String, Tag> values = new HashMap<>(nbt.getValue());

                    // Positions are kept in NBT, we don't want that.
                    values.remove("x");
                    values.remove("y");
                    values.remove("z");
                    values.put("Id", new StringTag(block.getNbtId()));

                    // Remove 'id' if it exists. We want 'Id'.
                    // Do this after we get "getNbtId" cos otherwise "getNbtId" doesn't work.
                    // Dum.
                    values.remove("id");
                    values.put("Pos", new IntArrayTag(new int[]{
                            pos.x(),
                            pos.y(),
                            pos.z()
                    }));
                    numTiles++;

                    tilesOut.writeTagPayload(new CompoundTag(values));
                }

                int ordinal = block.getOrdinal();
                if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                    ordinal = BlockTypesCache.ReservedIDs.AIR;
                }
                char value = palette[ordinal];
                if (value == Character.MAX_VALUE) {
                    int size = paletteMax++;
                    palette[ordinal] = value = (char) size;
                    paletteList.add(ordinal);
                }
                blocksOut.writeVarInt(value);
            }
            // close
            tilesOut.close();
            blocksOut.close();

            out.writeNamedTag("PaletteMax", paletteMax);

            out.writeLazyCompoundTag("Palette", out12 -> {
                for (int i = 0; i < paletteList.size(); i++) {
                    int stateOrdinal = paletteList.get(i);
                    BlockState state = BlockTypesCache.states[stateOrdinal];
                    out12.writeNamedTag(state.getAsString(), i);
                }
            });

            out.writeNamedTagName("BlockData", NBTConstants.TYPE_BYTE_ARRAY);
            rawStream.writeInt(blocksOut.size());
            try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(blocksCompressed.toByteArray()))) {
                IOUtil.copy(in, rawStream);
            }

            if (numTiles != 0) {
                out.writeNamedTagName("BlockEntities", NBTConstants.TYPE_LIST);
                rawStream.write(NBTConstants.TYPE_COMPOUND);
                rawStream.writeInt(numTiles);
                try (LZ4BlockInputStream in = new LZ4BlockInputStream(new ByteArrayInputStream(tilesCompressed.toByteArray()))) {
                    IOUtil.copy(in, rawStream);
                }
            } else {
                out.writeNamedEmptyList("BlockEntities");
            }

            if (finalClipboard.hasBiomes()) {
                writeBiomes(finalClipboard, out);
            }

            List<Tag> entities = new ArrayList<>();
            for (Entity entity : finalClipboard.getEntities()) {
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
                    Location loc = entity.getLocation();
                    if (!brokenEntities) {
                        loc = loc.setPosition(loc.add(min.toVector3()));
                    }
                    values.put("Id", new StringTag(state.getType().getId()));
                    values.put("Pos", writeVector(loc));
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
        IntValueReader task = new IntValueReader() {
            @Override
            public void applyInt(int index, int ordinal) {
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
        BlockVector3 min = clipboard.getMinimumPoint();
        int width = clipboard.getRegion().getWidth();
        int length = clipboard.getRegion().getLength();
        MutableBlockVector3 mutable = new MutableBlockVector3();
        for (int z = 0, i = 0; z < length; z++) {
            int z0 = min.z() + z;
            for (int x = 0; x < width; x++, i++) {
                int x0 = min.x() + x;
                BiomeType biome = clipboard.getBiome(mutable.setComponents(x0, min.y(), z0));
                task.applyInt(i, biome.getInternalId());
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

    @Override
    public void close() throws IOException {
        outputStream.close();
    }

}
