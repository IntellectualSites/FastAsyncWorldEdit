package com.fastasyncworldedit.core.extent.clipboard.io;

import com.fastasyncworldedit.core.function.visitor.Order;
import com.fastasyncworldedit.core.util.IOUtil;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import org.enginehub.linbus.tree.LinCompoundTag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Faster, stream-based implementation of {@link com.sk89q.worldedit.extent.clipboard.io.sponge.SpongeSchematicV3Writer} for
 * writing schematics conforming the sponge schematic v3 format.
 *
 * @since 2.11.1
 */
@SuppressWarnings("removal") // Yes, JNBT is deprecated - we know
public class FastSchematicWriterV3 implements ClipboardWriter {

    public static final int CURRENT_VERSION = 3;

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
            throw new IllegalArgumentException("Region height too large for schematic: " + region.getHeight());
        }
        if (region.getLength() > MAX_SIZE) {
            throw new IllegalArgumentException("Region length too large for schematic: " + region.getLength());
        }

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

        schematic.writeNamedTag("Width", (short) region.getWidth());
        schematic.writeNamedTag("Height", (short) region.getHeight());
        schematic.writeNamedTag("Length", (short) region.getLength());

        schematic.writeNamedTag("Offset", new int[]{
                offset.x(), offset.y(), offset.z()
        });

        schematic.writeLazyCompoundTag("Blocks", out -> this.writeBlocks(out, clipboard));
        if (clipboard.hasBiomes()) {
            schematic.writeLazyCompoundTag("Biomes", out -> this.writeBiomes(out, clipboard));
        }
        // Some clipboards have quite heavy operations on the getEntities method - only call once
        List<? extends Entity> entities;
        if (!(entities = clipboard.getEntities()).isEmpty()) {
            schematic.writeNamedTagName("Entities", NBTConstants.TYPE_LIST);
            schematic.write(NBTConstants.TYPE_COMPOUND);
            schematic.writeInt(entities.size());
            for (final Entity entity : entities) {
                this.writeEntity(schematic, clipboard, entity);
            }
        }
    }

    private void writeBlocks(NBTOutputStream blocks, Clipboard clipboard) throws IOException {
        final int[] tiles = new int[]{0};
        final ByteArrayOutputStream tileBytes = new ByteArrayOutputStream();
        try (LZ4BlockOutputStream lz4Stream = new LZ4BlockOutputStream(tileBytes);
             NBTOutputStream tileOut = new NBTOutputStream(lz4Stream)) {
            this.writePalette(
                    blocks,
                    BlockTypesCache.states.length,
                    pos -> {
                        BaseBlock block = pos.getFullBlock(clipboard);
                        LinCompoundTag tag;
                        if ((tag = block.getNbt()) != null) {
                            tiles[0]++;
                            try {
                                tileOut.writeNamedTag("Id", block.getNbtId());
                                tileOut.writeNamedTag("Pos", new int[]{
                                        pos.x() - clipboard.getMinimumPoint().x(),
                                        pos.y() - clipboard.getMinimumPoint().y(),
                                        pos.z() - clipboard.getMinimumPoint().z()
                                });
                                //noinspection deprecation
                                tileOut.writeNamedTag("Data", new CompoundTag(tag));
                                tileOut.write(NBTConstants.TYPE_END);
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to write tile data", e);
                            }
                        }
                        return block;
                    },
                    block -> {
                        char ordinal = block.getOrdinalChar();
                        if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                            ordinal = BlockTypesCache.ReservedIDs.AIR;
                        }
                        return ordinal;
                    },
                    BlockStateHolder::getAsString,
                    clipboard
            );
            lz4Stream.finish();
        } finally {
            // Write Tiles
            if (tiles[0] > 0) {
                blocks.writeNamedTagName("BlockEntities", NBTConstants.TYPE_LIST);
                blocks.write(NBTConstants.TYPE_COMPOUND);
                blocks.writeInt(tiles[0]);
                // Decompress cached data again
                try (LZ4BlockInputStream reader = new LZ4BlockInputStream(new ByteArrayInputStream(tileBytes.toByteArray()))) {
                    IOUtil.copy(reader, blocks.getOutputStream());
                }
            }
        }
    }

    private void writeBiomes(NBTOutputStream biomes, Clipboard clipboard) throws IOException {
        this.writePalette(
                biomes, BiomeType.REGISTRY.size(),
                pos -> pos.getBiome(clipboard),
                biome -> (char) biome.getInternalId(),
                BiomeType::id,
                clipboard
        );
    }

    private void writeEntity(NBTOutputStream out, Clipboard clipboard, Entity entity) throws IOException {
        final BaseEntity state = entity.getState();
        if (state == null) {
            throw new IOException("Entity has no state");
        }
        out.writeNamedTag("Id", state.getType().id());

        out.writeNamedTagName("Pos", NBTConstants.TYPE_LIST);
        out.write(NBTConstants.TYPE_DOUBLE);
        out.writeInt(3);
        out.writeDouble(entity.getLocation().x() - clipboard.getMinimumPoint().x());
        out.writeDouble(entity.getLocation().y() - clipboard.getMinimumPoint().y());
        out.writeDouble(entity.getLocation().z() - clipboard.getMinimumPoint().z());

        out.writeLazyCompoundTag("Data", data -> {
            //noinspection deprecation
            CompoundTag nbt = state.getNbtData();
            if (nbt != null) {
                nbt.getValue().forEach((s, tag) -> {
                    if (s.equals("id") || s.equals("Rotation")) {
                        return;
                    }
                    try {
                        data.writeNamedTag(s, tag);
                    } catch (IOException e) {
                        throw new RuntimeException("failed to write entity data", e);
                    }
                });
            }

            // Write rotation list
            data.writeNamedTagName("Rotation", NBTConstants.TYPE_LIST);
            data.write(NBTConstants.TYPE_FLOAT);
            data.writeInt(2);
            data.writeFloat(entity.getLocation().getYaw());
            data.writeFloat(entity.getLocation().getPitch());
        });

        out.write(NBTConstants.TYPE_END); // End the compound
    }

    private <T> void writePalette(
            NBTOutputStream out, int capacity,
            Function<BlockVector3, T> objectResolver,
            Function<T, Character> ordinalResolver,
            Function<T, String> paletteEntryResolver,
            Clipboard clipboard
    ) throws IOException {
        int dataBytesUsed = 0;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (LZ4BlockOutputStream dataOut = new LZ4BlockOutputStream(bytes)) {
            int index = 0;
            char[] palette = new char[capacity];
            Arrays.fill(palette, Character.MAX_VALUE);
            final Iterator<BlockVector3> iterator = clipboard.iterator(Order.YZX);
            // Start Palette tag
            out.writeNamedTagName("Palette", NBTConstants.TYPE_COMPOUND);
            while (iterator.hasNext()) {
                BlockVector3 pos = iterator.next();
                T obj = objectResolver.apply(pos);
                char ordinal = ordinalResolver.apply(obj);
                char value = palette[ordinal];
                if (value == Character.MAX_VALUE) {
                    palette[ordinal] = value = (char) index++;
                    if (index >= palette.length) {
                        throw new IOException("insufficient palette capacity: " + palette.length + ", index: " + index);
                    }
                    out.writeNamedTag(paletteEntryResolver.apply(obj), value);
                }
                if ((value & -128) != 0) {
                    dataBytesUsed++;
                    dataOut.write(value & 127 | 128);
                    value >>>= 7;
                }
                dataOut.write(value);
                dataBytesUsed++;
            }
            // End Palette tag
            out.write(NBTConstants.TYPE_END);
            dataOut.finish();
        } finally {
            // Write Data tag
            if (dataBytesUsed > 0) {
                try (LZ4BlockInputStream reader = new LZ4BlockInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                    out.writeNamedTagName("Data", NBTConstants.TYPE_BYTE_ARRAY);
                    out.writeInt(dataBytesUsed);
                    IOUtil.copy(reader, (DataOutput) out);
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
