package com.fastasyncworldedit.core.extent.clipboard.io;

import com.fastasyncworldedit.core.extent.clipboard.LinearClipboard;
import com.fastasyncworldedit.core.extent.clipboard.SimpleClipboard;
import com.fastasyncworldedit.core.internal.io.ResettableFileInputStream;
import com.fastasyncworldedit.core.internal.io.VarIntStreamIterator;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.math.MutableVector3;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.sponge.VersionedDataFixer;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import com.sk89q.worldedit.world.entity.EntityType;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.TriConsumer;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

/**
 * ClipboardReader for the Sponge Schematic Format v3.
 * Not necessarily much faster than {@link com.sk89q.worldedit.extent.clipboard.io.sponge.SpongeSchematicV3Reader}, but uses a
 * stream based approach to keep the memory overhead minimal (especially in larger schematics)
 */
@SuppressWarnings("removal") // JNBT
public class FastSchematicReaderV3 implements ClipboardReader {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private final InputStream resetableInputStream;
    private final MutableBlockVector3 dimensions = MutableBlockVector3.at(0, 0, 0);

    private DataInputStream dataInputStream;
    private NBTInputStream nbtInputStream;

    private VersionedDataFixer dataFixer;
    private BlockVector3 offset;
    private BlockState[] blockPalette;
    private BiomeType[] biomePalette;
    private int dataVersion = -1;

    private boolean blocksWritten = false;
    private boolean biomesWritten = false;
    private boolean entitiesWritten = false;

    private boolean needAdditionalIterate = true;

    public FastSchematicReaderV3(@NonNull InputStream stream) throws IOException {
        Objects.requireNonNull(stream, "stream");
        if (stream instanceof FileInputStream fileInputStream) {
            stream = new ResettableFileInputStream(fileInputStream);
        } else if (!stream.markSupported()) {
            LOGGER.warn("InputStream does not support mark - will be wrapped using in memory buffer");
            stream = new BufferedInputStream(stream);
        }
        this.resetableInputStream = stream;
        this.resetableInputStream.mark(Integer.MAX_VALUE);
    }

    @Override
    public Clipboard read(final UUID uuid, final Function<BlockVector3, Clipboard> createOutput) throws IOException {
        Clipboard clipboard = null;

        while (needAdditionalIterate) {
            System.out.println("reset");
            this.needAdditionalIterate = false;
            this.resetableInputStream.reset();
            this.resetableInputStream.mark(Integer.MAX_VALUE);
            final FastBufferedInputStream buffer = new FastBufferedInputStream(new GZIPInputStream(this.resetableInputStream));
            this.dataInputStream = new DataInputStream(buffer);
            this.nbtInputStream = new NBTInputStream(buffer);

            // Skip header
            dataInputStream.skipNBytes(1 + 2); // 1 Byte = TAG_Compound, 2 Bytes = Short (Length of tag name = "")
            dataInputStream.skipNBytes(1 + 2 + 9); // as above + 9 bytes = "Schematic"

            byte type;
            while ((type = dataInputStream.readByte()) != NBTConstants.TYPE_END) {
                String tag = readTagName();
                System.out.println(type + ": " + tag);
                switch (tag) {
                    case "Version" -> this.dataInputStream.skipNBytes(4); // We know it's v3 (skip 4 byte version int)
                    case "DataVersion" -> {
                        this.dataVersion = this.dataInputStream.readInt();
                        this.dataFixer = new VersionedDataFixer(
                                this.dataVersion,
                                WorldEdit
                                        .getInstance()
                                        .getPlatformManager()
                                        .queryCapability(Capability.WORLD_EDITING)
                                        .getDataFixer()
                        );
                    }
                    case "Width", "Height", "Length" -> {
                        if (clipboard != null) {
                            this.dataInputStream.skipNBytes(2); // Skip short value
                            continue;
                        }
                        if (tag.equals("Width")) {
                            this.dimensions.mutX(this.dataInputStream.readShort() & 0xFFFF);
                        } else if (tag.equals("Height")) {
                            this.dimensions.mutY(this.dataInputStream.readShort() & 0xFFFF);
                        } else {
                            this.dimensions.mutZ(this.dataInputStream.readShort() & 0xFFFF);
                        }
                        if (areDimensionsAvailable()) {
                            clipboard = createOutput.apply(this.dimensions);
                        }
                    }
                    case "Offset" -> {
                        this.dataInputStream.skipNBytes(4); // Array Length field (4 byte int)
                        this.offset = BlockVector3.at(
                                this.dataInputStream.readInt(),
                                this.dataInputStream.readInt(),
                                this.dataInputStream.readInt()
                        );
                    }
                    case "Metadata" -> this.nbtInputStream.readTagPayloadLazy(NBTConstants.TYPE_COMPOUND, 0); // Skip metadata
                    case "Blocks" -> {
                        if (clipboard == null) {
                            needAdditionalIterate = true;
                            this.nbtInputStream.readTagPayloadLazy(NBTConstants.TYPE_COMPOUND, 0);
                        } else {
                            this.readBlocks(clipboard);
                        }
                    }
                    case "Biomes" -> {
                        if (biomesWritten) {
                            this.nbtInputStream.readTagPayloadLazy(NBTConstants.TYPE_COMPOUND, 0);
                            continue;
                        }
                        if (clipboard == null) {
                            needAdditionalIterate = true;
                        } else {
                            this.readBiomes(clipboard);
                        }
                    }
                    case "Entities" -> {
                        if (entitiesWritten) {
                            this.nbtInputStream.readTagPayloadLazy(NBTConstants.TYPE_LIST, 0);
                            continue;
                        }
                        if (clipboard == null) {
                            needAdditionalIterate = true;
                            this.nbtInputStream.readTagPayloadLazy(NBTConstants.TYPE_LIST, 0);
                        } else {
                            this.readEntities(clipboard);
                        }
                    }
                    default -> {
                        LOGGER.warn("Unknown tag {} with name {} - skipping", type, tag);
                        this.nbtInputStream.readTagPayloadLazy(type, 0);
                    }
                }
            }
        }
        if (clipboard == null) {
            throw new NullPointerException("Failed to read schematic: Clipboard is null");
        }
        clipboard.setOrigin(this.offset.multiply().multiply(-1));
        if (clipboard instanceof SimpleClipboard simpleClipboard && !this.offset.equals(BlockVector3.ZERO)) {
            clipboard = new BlockArrayClipboard(simpleClipboard, this.offset);
        }
        return clipboard;
    }

    @Override
    public OptionalInt getDataVersion() {
        return this.dataVersion > -1 ? OptionalInt.of(this.dataVersion) : OptionalInt.empty();
    }

    private void readBlocks(Clipboard target) throws IOException {
        BiConsumer<Integer, Character> blockStateApplier;
        if (target instanceof LinearClipboard linearClipboard) {
            blockStateApplier = (dataIndex, paletteIndex) -> linearClipboard.setBlock(dataIndex, this.blockPalette[paletteIndex]);
        } else {
            blockStateApplier = (dataIndex, paletteIndex) -> {
                int y = dataIndex / (dimensions.x() * dimensions.z());
                int remainder = dataIndex - (y * dimensions.x() * dimensions.z());
                int z = remainder / dimensions.x();
                int x = remainder - z * dimensions.x();
                target.setBlock(x, y, z, this.blockPalette[paletteIndex]);
            };
        }
        this.blockPalette = new BlockState[BlockTypesCache.states.length];
        readPalette(
                () -> this.blockPalette.length == 0,
                () -> this.blocksWritten,
                () -> this.blocksWritten = true,
                (value, index) -> {
                    value = dataFixer.fixUp(DataFixer.FixTypes.BLOCK_STATE, value);
                    try {
                        this.blockPalette[index] = BlockState.get(value);
                    } catch (InputParseException e) {
                        LOGGER.warn("Invalid BlockState in palette: {}. Block will be replaced with air.", value);
                        this.blockPalette[index] = BlockTypes.AIR.getDefaultState();
                    }
                },
                blockStateApplier,
                (type, tag) -> {
                    if (!tag.equals("BlockEntities")) {
                        LOGGER.warn("Found additional tag in block palette: {}. Will skip tag.", tag);
                        try {
                            this.nbtInputStream.readTagPayloadLazy(type, 0);
                        } catch (IOException e) {
                            LOGGER.error("Failed to skip additional tag", e);
                        }
                        return;
                    }
                    try {
                        // Can't write TileEntities if block itself are non-existent
                        if (!blocksWritten) {
                            needAdditionalIterate = true;
                            this.nbtInputStream.readTagPayloadLazy(NBTConstants.TYPE_LIST, 0);
                            return;
                        }
                        this.readTileEntities(target);
                    } catch (IOException e) {
                        LOGGER.warn("Failed to read tile entities", e);
                    }
                }
        );
    }

    private void readBiomes(Clipboard target) throws IOException {
        BiConsumer<Integer, Character> biomeApplier;
        if (target instanceof LinearClipboard linearClipboard) {
            biomeApplier = (dataIndex, paletteIndex) -> linearClipboard.setBiome(dataIndex, this.biomePalette[paletteIndex]);
        } else {
            biomeApplier = (dataIndex, paletteIndex) -> {
                int y = dataIndex / (dimensions.x() * dimensions.z());
                int remainder = dataIndex - (y * dimensions.x() * dimensions.z());
                int z = remainder / dimensions.x();
                int x = remainder - z * dimensions.x();
                target.setBiome(x, y, z, this.biomePalette[paletteIndex]);
            };
        }
        this.biomePalette = new BiomeType[BiomeType.REGISTRY.size()];
        readPalette(
                () -> this.biomePalette.length == 0,
                () -> this.biomesWritten,
                () -> this.biomesWritten = true,
                (value, index) -> {
                    value = dataFixer.fixUp(DataFixer.FixTypes.BIOME, value);
                    BiomeType biomeType = BiomeTypes.get(value);
                    if (biomeType == null) {
                        biomeType = BiomeTypes.PLAINS;
                        LOGGER.warn("Invalid biome type in palette: {}. Biome will be replaced with plains.", value);
                    }
                    this.biomePalette[index] = biomeType;
                },
                biomeApplier,
                (type, tag) -> {
                    LOGGER.warn("Found additional tag in biome palette: {}. Will skip tag.", tag);
                    try {
                        this.nbtInputStream.readTagPayloadLazy(type, 0);
                    } catch (IOException e) {
                        LOGGER.error("Failed to skip additional tag", e);
                    }
                }
        );
    }

    private void readEntities(Clipboard target) throws IOException {
        if (this.dataInputStream.read() != NBTConstants.TYPE_COMPOUND) {
            throw new IOException("Expected a compound block for entity");
        }
        readEntityContainers((id, pos, data) -> {
            final EntityType entityType = EntityType.REGISTRY.get(id);
            if (entityType == null) {
                LOGGER.warn("Unknown entity {} @ {},{},{} - skipping", id, pos.x(), pos.y(), pos.z());
                return;
            }
            // Back and forth conversion, because setTile only supports JNBT CompoundTag
            // whereas DataFixer can only handle BinaryTagCompound...
            CompoundBinaryTag tag = this.dataFixer.fixUp(DataFixer.FixTypes.ENTITY, data.asBinaryTag());
            if (tag == null) {
                LOGGER.warn("Failed to fix-up entity for {} @ {},{},{} - skipping", id, pos.x(), pos.y(), pos.z());
                return;
            }
            if (target.createEntity(new Location(target, pos), new BaseEntity(entityType, LazyReference.computed(tag))) == null) {
                LOGGER.warn("Failed to create entity - does the clipboard support entities?");
            }
        });
        this.entitiesWritten = true;
    }

    private void readTileEntities(Clipboard target) throws IOException {
        if (this.dataInputStream.read() != NBTConstants.TYPE_COMPOUND) {
            throw new IOException("Expected a compound block for tile entity");
        }
        readEntityContainers((id, pos, data) -> {
            // Back and forth conversion, because setTile only supports JNBT CompoundTag
            // whereas DataFixer can only handle BinaryTagCompound...
            CompoundBinaryTag tag = this.dataFixer.fixUp(DataFixer.FixTypes.BLOCK_ENTITY, data.asBinaryTag());
            if (tag == null) {
                LOGGER.warn("Failed to fix-up tile entity for {} @ {},{},{} - skipping",
                        id, pos.blockX(), pos.blockY(), pos.blockZ()
                );
                return;
            }
            if (!target.setTile(pos.blockX(), pos.blockY(), pos.blockZ(), new CompoundTag(tag))) {
                LOGGER.warn("Failed to set tile entity - does the clipboard support tile entities?");
            }
        });
    }

    private void readEntityContainers(TriConsumer<String, Vector3, CompoundTag> writer) throws IOException {
        MutableVector3 pos = new MutableVector3();
        CompoundTag tag;
        String id;
        int count = this.dataInputStream.readInt();
        byte type;
        while (count-- > 0) {
            pos.setComponents(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
            tag = null;
            id = null;
            while ((type = this.dataInputStream.readByte()) != NBTConstants.TYPE_END) {
                switch (type) {
                    // Depending on the type of entity container (tile vs "normal") the pos consists of either doubles or ints
                    case NBTConstants.TYPE_INT_ARRAY -> {
                        if (!readTagName().equals("Pos")) {
                            throw new IOException("Expected INT_ARRAY tag to be Pos");
                        }
                        this.dataInputStream.skipNBytes(4); // count of following ints - for pos = 3
                        pos.mutX(this.dataInputStream.readInt());
                        pos.mutY(this.dataInputStream.readInt());
                        pos.mutZ(this.dataInputStream.readInt());
                    }
                    case NBTConstants.TYPE_LIST -> {
                        if (!readTagName().equals("Pos")) {
                            throw new IOException("Expected LIST tag to be Pos");
                        }
                        if (this.dataInputStream.readByte() != NBTConstants.TYPE_DOUBLE) {
                            throw new IOException("Expected LIST Pos tag to contain DOUBLE");
                        }
                        this.dataInputStream.skipNBytes(4); // count of following doubles - for pos = 3
                        pos.mutX(this.dataInputStream.readDouble());
                        pos.mutY(this.dataInputStream.readDouble());
                        pos.mutZ(this.dataInputStream.readDouble());
                    }
                    case NBTConstants.TYPE_STRING -> {
                        if (!readTagName().equals("Id")) {
                            throw new IOException("Expected STRING tag to be Id");
                        }
                        id = this.dataInputStream.readUTF();
                    }
                    case NBTConstants.TYPE_COMPOUND -> {
                        if (!readTagName().equals("Data")) {
                            throw new IOException("Expected COMPOUND tag to be Data");
                        }
                        tag = (CompoundTag) this.nbtInputStream.readTagPayload(NBTConstants.TYPE_COMPOUND, 0);
                    }
                    default -> throw new IOException("Unexpected tag in compound: " + type);
                }
            }
            // Data can be actually null is not required somehow?
            if (tag == null) {
                continue;
            }
            if (id == null) {
                throw new IOException("Missing Id tag in compound");
            }
            writer.accept(id, pos, tag);
        }
    }

    /**
     * The `Palette` tag is required first, as that contains the information of the actual palette size.
     * Keeping the whole Data block in memory - which *could* be compressed - is just not it
     *
     * @param paletteInitializer Invoked for each 'Palette' entry using the actual palette value (e.g. block state) + index
     * @param paletteDataApplier Invoked for each 'Data' entry using the data index and the palette index at the data index
     */
    private void readPalette(
            BooleanSupplier paletteAlreadyInitialized,
            BooleanSupplier dataAlreadyWritten,
            Runnable firstWrite,
            BiConsumer<String, Character> paletteInitializer,
            BiConsumer<Integer, Character> paletteDataApplier,
            BiConsumer<Byte, String> additionalTag
    ) throws IOException {
        boolean hasPalette = paletteAlreadyInitialized.getAsBoolean();
        byte type;
        String tag;
        while ((type = this.dataInputStream.readByte()) != NBTConstants.TYPE_END) {
            tag = readTagName();
            if (tag.equals("Palette")) {
                if (hasPalette) {
                    // Skip palette, as already exists
                    this.nbtInputStream.readTagPayloadLazy(NBTConstants.TYPE_COMPOUND, 0);
                } else {
                    // Read all palette entries
                    while (this.dataInputStream.readByte() != NBTConstants.TYPE_END) {
                        String value = this.dataInputStream.readUTF();
                        char index = (char) this.dataInputStream.readInt();
                        paletteInitializer.accept(value, index);
                    }
                    hasPalette = true;
                }
                continue;
            }
            if (tag.equals("Data")) {
                if (dataAlreadyWritten.getAsBoolean()) {
                    // Skip data, as already written
                    this.nbtInputStream.readTagPayloadLazy(NBTConstants.TYPE_BYTE_ARRAY, 0);
                    continue;
                }
                // No palette or dimensions are yet available - will need to read Data next round
                if (!hasPalette || !areDimensionsAvailable()) {
                    this.needAdditionalIterate = true;
                    this.nbtInputStream.readTagPayloadLazy(NBTConstants.TYPE_BYTE_ARRAY, 0);
                    return;
                }
                int length = this.dataInputStream.readInt();
                // Write data into clipboard
                firstWrite.run();
                int i = 0;
                for (var iter = new VarIntStreamIterator(this.dataInputStream, length); iter.hasNext(); i++) {
                    paletteDataApplier.accept(i, (char) iter.nextInt());
                }
                continue;
            }
            additionalTag.accept(type, tag);
        }
    }

    private String readTagName() throws IOException {
        return dataInputStream.readUTF();
    }

    private boolean areDimensionsAvailable() {
        return this.dimensions.x() != 0 && this.dimensions.y() != 0 && this.dimensions.z() != 0;
    }

    @Override
    public void close() throws IOException {
        resetableInputStream.close(); // closes all underlying resources implicitly
    }

}
