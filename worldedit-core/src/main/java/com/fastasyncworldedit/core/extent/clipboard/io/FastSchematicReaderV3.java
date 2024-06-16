package com.fastasyncworldedit.core.extent.clipboard.io;

import com.fastasyncworldedit.core.extent.clipboard.LinearClipboard;
import com.fastasyncworldedit.core.extent.clipboard.SimpleClipboard;
import com.fastasyncworldedit.core.internal.io.VarIntStreamIterator;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.sponge.VersionedDataFixer;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * ClipboardReader for the Sponge Schematic Format v3.
 * Not necessarily much faster than {@link com.sk89q.worldedit.extent.clipboard.io.sponge.SpongeSchematicV3Reader}, but uses a
 * stream based approach to keep the memory overhead minimal (especially in larger schematics)
 */
@SuppressWarnings("removal") // JNBT
public class FastSchematicReaderV3 implements ClipboardReader {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private final DataInputStream dataInputStream;
    private final NBTInputStream nbtInputStream;

    private VersionedDataFixer dataFixer;
    private MutableBlockVector3 dimensions = MutableBlockVector3.at(0, 0, 0);
    private BlockVector3 offset;
    private BlockState[] blockPalette;
    private BiomeType[] biomePalette;
    private int dataVersion = -1;

    private boolean blocksWritten = false;
    private boolean biomesWritten = false;
    private boolean entitiesWritten = false;

    private boolean needAdditionalIterate = true;

    public FastSchematicReaderV3(
            DataInputStream dataInputStream,
            NBTInputStream nbtInputStream
    ) throws IOException {
        this.dataInputStream = Objects.requireNonNull(dataInputStream, "dataInputStream");
        this.nbtInputStream = Objects.requireNonNull(nbtInputStream, "nbtInputStream");
        if (!dataInputStream.markSupported()) {
            throw new IOException("InputStream does not support mark");
        }
    }

    @Override
    public Clipboard read(final UUID uuid, final Function<BlockVector3, Clipboard> createOutput) throws IOException {
        dataInputStream.skipNBytes(1 + 2); // 1 Byte = TAG_Compound, 2 Bytes = Short (Length of tag name = "")
        dataInputStream.skipNBytes(1 + 2 + 9); // as above + 9 bytes = "Schematic"
        this.dataInputStream.mark(Integer.MAX_VALUE); // allow resets to basically the start of stream (file) - just skip header

        Clipboard clipboard = null;

        while (needAdditionalIterate) {
            this.needAdditionalIterate = false;
            this.dataInputStream.reset();
            this.dataInputStream.mark(Integer.MAX_VALUE);

            while (dataInputStream.readByte() != NBTConstants.TYPE_END) {
                String tag = readTagName();
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
                        } else {
                            this.readBlocks(clipboard);
                        }
                    }
                    case "Biomes" -> {
                        if (clipboard == null) {
                            needAdditionalIterate = true;
                        } else {
                            this.readBiomes(clipboard);
                        }
                    }
                    case "Entities" -> {
                        if (clipboard == null) {
                            needAdditionalIterate = true;
                        } else {
                            this.readEntities(clipboard);
                        }
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

    // TODO: BlockEntities
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
                    // TODO: Process block entities
                    try {
                        this.nbtInputStream.readTagPayloadLazy(type, 0);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
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
        // TODO
        this.nbtInputStream.readTagPayloadLazy(NBTConstants.TYPE_LIST, 0);
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
        if (dataAlreadyWritten.getAsBoolean()) {
            return;
        }
        boolean hasPalette = paletteAlreadyInitialized.getAsBoolean();
        byte type;
        String tag;
        while ((type = this.dataInputStream.readByte()) != NBTConstants.TYPE_END) {
            tag = readTagName();
            if (tag.equals("Palette")) {
                if (hasPalette) {
                    // Skip data, as palette already exists
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
                // No palette or dimensions are yet available - will need to read Data next round
                if (!hasPalette || !areDimensionsAvailable()) {
                    this.needAdditionalIterate = true;
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
        nbtInputStream.close(); // closes the DataInputStream implicitly
    }

}
