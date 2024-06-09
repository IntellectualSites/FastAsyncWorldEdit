package com.fastasyncworldedit.core.extent.clipboard.io;

import com.fastasyncworldedit.core.function.visitor.Order;
import com.fastasyncworldedit.core.util.IOUtil;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("removal") // Yes, JNBT is deprecated - we know
public class FastSchematicWriterV3 implements ClipboardWriter {

    private static final int CURRENT_VERSION = 3;

    private static final int MAX_SIZE = Short.MAX_VALUE - Short.MIN_VALUE;
    private final NBTOutputStream outputStream;


    public FastSchematicWriterV3(final NBTOutputStream outputStream) {
        this.outputStream = Objects.requireNonNull(outputStream, "outputStream");
    }

    @Override
    public void write(final Clipboard clipboard) throws IOException {
        clipboard.flush();

        // Validate dimensions before starting to write into stream
        final Region region = clipboard.getRegion();
        if (region.getWidth() > MAX_SIZE) {
            throw new IllegalArgumentException("Region width too large for schematic: " + region.getWidth());
        }
        if (region.getHeight() > MAX_SIZE) {
            throw new IllegalArgumentException("Region height too large for schematic: " + region.getWidth());
        }
        if (region.getLength() > MAX_SIZE) {
            throw new IllegalArgumentException("Region length too large for schematic: " + region.getWidth());
        }

        /*
         * {
         *     "": {
         *         "Schematic": {
         *             //...
         *         }
         *     }
         * }
         */
        this.outputStream.writeLazyCompoundTag(
                "", root -> root.writeLazyCompoundTag("Schematic", out -> this.write2(out, clipboard))
        );
    }

    private void write2(NBTOutputStream schematic, Clipboard clipboard) throws IOException {
        final Region region = clipboard.getRegion();
        final BlockVector3 origin = clipboard.getOrigin();
        final BlockVector3 min = clipboard.getMinimumPoint();
        final BlockVector3 offset = min.subtract(origin);

        schematic.writeNamedTag("Version", CURRENT_VERSION);
        schematic.writeNamedTag(
                "DataVersion",
                WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).getDataVersion()
        );
        schematic.writeLazyCompoundTag("Metadata", out -> this.writeMetadata(out, clipboard));

        schematic.writeNamedTag("Width", region.getWidth());
        schematic.writeNamedTag("Height", region.getHeight());
        schematic.writeNamedTag("Length", region.getLength());

        schematic.writeNamedTag("Offset", new int[]{
                offset.x(), offset.y(), offset.z()
        });

        schematic.writeLazyCompoundTag("Blocks", out -> this.writeBlocks(out, clipboard, region));
        if (clipboard.hasBiomes()) {
            schematic.writeLazyCompoundTag("Biomes", out -> this.writeBiomes(out, clipboard));
        }
        // Some clipboards have quite heavy operations on the getEntities method - only call once
        List<? extends Entity> entities;
        if (!(entities = clipboard.getEntities()).isEmpty()) {
            schematic.writeLazyCompoundTag("Entities", out -> this.writeEntities(out, entities));
        }
    }

    private void writeBlocks(NBTOutputStream blocks, Clipboard clipboard, Region region) throws IOException {
        final Iterator<BlockVector3> iterator = clipboard.iterator(Order.YZX);
        char[] palette = new char[BlockTypesCache.states.length];
        int varIntBytesUsed = 0;
        int tiles = 0;

        try (ByteArrayOutputStream dataBytes = new ByteArrayOutputStream();
             ByteArrayOutputStream tileBytes = new ByteArrayOutputStream();
             LZ4BlockOutputStream dataBuf = new LZ4BlockOutputStream(dataBytes);
             NBTOutputStream tileBuf = new NBTOutputStream(new LZ4BlockOutputStream(dataBytes))) {

            // Write palette
            blocks.writeNamedTagName("Palette", NBTConstants.TYPE_COMPOUND);
            int index = 0;
            BlockVector3 pos;
            BaseBlock baseBlock;
            while (iterator.hasNext()) {
                pos = iterator.next();
                baseBlock = clipboard.getFullBlock(pos);

                // Make sure it's a valid ordinal or fallback to air
                char ordinal = baseBlock.getOrdinalChar();
                if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                    ordinal = BlockTypesCache.ReservedIDs.AIR;
                }

                // If ordinal (= state) is not already in palette, add to palette and assign new index
                char value = palette[ordinal];
                if (value == Character.MIN_VALUE) {
                    palette[ordinal] = value = (char) ++index;
                    // Write to palette
                    blocks.writeNamedTag(baseBlock.getAsString(), value);
                }

                // Write to cache for "Data" Tag

                while ((value & -128) != 0) {
                    dataBuf.write(value & 127 | 128);
                    value >>>= 7;
                }

                CompoundBinaryTag tag;
                if ((tag = baseBlock.getNbt()) != null) {
                    tiles++;
                    BlockVector3 posNormalized = pos.subtract(clipboard.getMinimumPoint());
                    tileBuf.writeNamedTag("Id", baseBlock.getNbtId());
                    tileBuf.writeNamedTag("Pos", new int[] {
                            posNormalized.x(), posNormalized.y(), posNormalized.z()
                    });
                    tileBuf.writeNamedTag("Data", new CompoundTag(tag));
                    tileBuf.write(NBTConstants.TYPE_END);
                }
            }
            // End "Palette" Compound
            blocks.writeByte(NBTConstants.TYPE_END);


            // Write data
            blocks.writeNamedTagName("Data", NBTConstants.TYPE_BYTE_ARRAY);
            blocks.writeInt(varIntBytesUsed);
            // Decompress cached data again
            try (LZ4BlockInputStream reader = new LZ4BlockInputStream(new ByteArrayInputStream(dataBytes.toByteArray()))) {
                IOUtil.copy(reader, blocks.getOutputStream());
            }

            // Write Tiles
            if (tiles > 0) {
                blocks.writeNamedTagName("BlockEntities", NBTConstants.TYPE_LIST);
                blocks.write(NBTConstants.TYPE_COMPOUND);
                blocks.writeInt(tiles);
                // Decompress cached data again
                try (LZ4BlockInputStream reader = new LZ4BlockInputStream(new ByteArrayInputStream(tileBytes.toByteArray()))) {
                    IOUtil.copy(reader, blocks.getOutputStream());
                }
            }
        }
    }

    private void writeMetadata(NBTOutputStream metadata, Clipboard clipboard) throws IOException {
        metadata.writeNamedTag("Date", System.currentTimeMillis());
        metadata.writeLazyCompoundTag("WorldEdit", out -> {
            out.writeNamedTag("Version", WorldEdit.getVersion());
            out.writeNamedTag(
                    "EditingPlatform",
                    WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).getId()
            );
            out.writeNamedTag("Origin", new int[]{
                    clipboard.getOrigin().x(), clipboard.getOrigin().y(), clipboard.getOrigin().z()
            });
            out.writeLazyCompoundTag("Platforms", platforms -> {
                for (final Platform platform : WorldEdit.getInstance().getPlatformManager().getPlatforms()) {
                    platforms.writeLazyCompoundTag(platform.getId(), p -> {
                        p.writeNamedTag("Name", platform.getPlatformName());
                        p.writeNamedTag("Version", platform.getPlatformVersion());
                    });
                }
            });
        });
    }

    @Override
    public void close() throws IOException {
        this.outputStream.close();
    }

}
