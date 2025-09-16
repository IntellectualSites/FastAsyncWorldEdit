package com.fastasyncworldedit.core.extent.clipboard.io;

import com.fastasyncworldedit.core.extent.clipboard.LinearClipboard;
import com.fastasyncworldedit.core.extent.clipboard.SimpleClipboard;
import com.fastasyncworldedit.core.internal.io.ResettableFileInputStream;
import com.fastasyncworldedit.core.internal.io.VarIntStreamIterator;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.util.IOUtil;
import com.fastasyncworldedit.core.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.sponge.ReaderUtil;
import com.sk89q.worldedit.extent.clipboard.io.sponge.VersionedDataFixer;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import com.sk89q.worldedit.world.entity.EntityType;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinIntArrayTag;
import org.enginehub.linbus.tree.LinTagType;
import org.jetbrains.annotations.ApiStatus;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

/**
 * ClipboardReader for the Sponge Schematic Format v3.
 * Not necessarily much faster than {@link com.sk89q.worldedit.extent.clipboard.io.sponge.SpongeSchematicV3Reader}, but uses a
 * stream based approach to keep the memory overhead minimal (especially in larger schematics)
 *
 * @since 2.11.1
 */
@SuppressWarnings("removal") // JNBT
public class FastSchematicReaderV3 implements ClipboardReader {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private static final byte CACHE_IDENTIFIER_END = 0x00;
    private static final byte CACHE_IDENTIFIER_BLOCK = 0x01;
    private static final byte CACHE_IDENTIFIER_BIOMES = 0x02;
    private static final byte CACHE_IDENTIFIER_ENTITIES = 0x03;
    private static final byte CACHE_IDENTIFIER_BLOCK_TILE_ENTITIES = 0x04;

    private final InputStream parentStream;
    private final MutableBlockVector3 dimensions = MutableBlockVector3.at(0, 0, 0);
    private final Set<Byte> remainingTags;

    private DataInputStream dataInputStream;
    private NBTInputStream nbtInputStream;

    private VersionedDataFixer dataFixer;
    private BlockVector3 offset;
    private BlockVector3 origin = BlockVector3.ZERO;
    private BlockState[] blockPalette;
    private BiomeType[] biomePalette;
    private int dataVersion = -1;

    // Only used if the InputStream is not file based (and therefor does not support resets based on FileChannels)
    // and the file is unordered
    // Data and Palette cache is separated, as the data requires a fully populated palette - and the order is not guaranteed
    private byte[] dataCache;
    private byte[] paletteCache;
    private OutputStream dataCacheWriter;
    private OutputStream paletteCacheWriter;


    public FastSchematicReaderV3(@NonNull InputStream stream) {
        Objects.requireNonNull(stream, "stream");
        if (stream instanceof ResettableFileInputStream) {
            stream.mark(Integer.MAX_VALUE);
            this.remainingTags = new HashSet<>();
        } else if (stream instanceof FileInputStream fileInputStream) {
            stream = new ResettableFileInputStream(fileInputStream);
            stream.mark(Integer.MAX_VALUE);
            this.remainingTags = new HashSet<>();
        } else if (stream instanceof FastBufferedInputStream || stream instanceof BufferedInputStream) {
            this.remainingTags = null;
        } else {
            stream = new FastBufferedInputStream(stream);
            this.remainingTags = null;
        }
        this.parentStream = stream;
    }

    @Override
    public Clipboard read(final UUID uuid, final Function<BlockVector3, Clipboard> createOutput) throws IOException {
        Clipboard clipboard = null;

        this.setSubStreams();
        skipHeader(this.dataInputStream);

        byte type;
        String tag;
        while ((type = dataInputStream.readByte()) != NBTConstants.TYPE_END) {
            tag = this.dataInputStream.readUTF();
            switch (tag) {
                case "DataVersion" -> {
                    final Platform platform =
                            WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING);
                    this.dataVersion = this.dataInputStream.readInt();
                    this.dataFixer = ReaderUtil.getVersionedDataFixer(this.dataVersion, platform, platform.getDataVersion());
                }
                case "Metadata" -> {
                    LinCompoundTag metadataCompoundTag =
                            (LinCompoundTag) this.nbtInputStream.readTagPayload(NBTConstants.TYPE_COMPOUND, 0).toLinTag();

                    LinCompoundTag worldEditTag = metadataCompoundTag.findTag("WorldEdit", LinTagType.compoundTag());
                    if (worldEditTag != null) { // allowed to be optional
                        LinIntArrayTag originTag = worldEditTag.findTag("Origin", LinTagType.intArrayTag());
                        if (originTag != null) { // allowed to be optional
                            int[] parts = originTag.value();

                            if (parts.length != 3) {
                                throw new IOException("`Metadata > WorldEdit > Origin` int array length is invalid.");
                            }

                            this.origin = BlockVector3.at(parts[0], parts[1], parts[2]);
                        }
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
                case "Width" -> this.dimensions.mutX(this.dataInputStream.readShort() & 0xFFFF);
                case "Height" -> this.dimensions.mutY(this.dataInputStream.readShort() & 0xFFFF);
                case "Length" -> this.dimensions.mutZ(this.dataInputStream.readShort() & 0xFFFF);
                case "Blocks" -> readBlocks(clipboard);
                case "Biomes" -> readBiomes(clipboard);
                case "Entities" -> readEntities(clipboard);
                default -> this.nbtInputStream.readTagPayloadLazy(type, 0);
            }
            if (clipboard == null && this.areDimensionsAvailable()) {
                clipboard = createOutput.apply(this.dimensions);
            }
        }

        if (clipboard == null) {
            throw new IOException("Invalid schematic - missing dimensions");
        }
        if (dataFixer == null) {
            throw new IOException("Invalid schematic - missing DataVersion");
        }

        if (this.supportsReset() && !remainingTags.isEmpty()) {
            readRemainingDataReset(clipboard);
        } else if (this.dataCacheWriter != null || this.paletteCacheWriter != null) {
            readRemainingDataCache(clipboard);
        }

        clipboard.setOrigin(this.offset.multiply(-1));
        if (clipboard instanceof SimpleClipboard simpleClipboard && !this.offset.equals(BlockVector3.ZERO)) {
            clipboard = new BlockArrayClipboard(simpleClipboard, this.offset.add(this.origin));
        }
        return clipboard;
    }


    /**
     * Reads all locally cached data (due to reset not being available) and applies them to the clipboard.
     * <p>
     * Firstly, closes all cache writers (which adds the END identifier to each and fills the cache byte arrays on this instance)
     * If required, creates all missing palettes first (as needed by all remaining data).
     * At last writes all missing data (block states, tile entities, biomes, entities).
     *
     * @param clipboard The clipboard to write into.
     * @throws IOException on I/O error.
     */
    private void readRemainingDataCache(Clipboard clipboard) throws IOException {
        byte identifier;
        if (this.paletteCacheWriter != null) {
            this.paletteCacheWriter.close();
        }
        if (this.dataCacheWriter != null) {
            this.dataCacheWriter.close();
        }
        if (this.paletteCache != null) {
            try (final DataInputStream cacheStream = new DataInputStream(new FastBufferedInputStream(
                    new LZ4BlockInputStream(new FastBufferedInputStream(new ByteArrayInputStream(this.paletteCache)))))) {
                while ((identifier = cacheStream.readByte()) != CACHE_IDENTIFIER_END) {
                    if (identifier == CACHE_IDENTIFIER_BLOCK) {
                        this.readPaletteMap(cacheStream, this.provideBlockPaletteInitializer());
                        continue;
                    }
                    if (identifier == CACHE_IDENTIFIER_BIOMES) {
                        this.readPaletteMap(cacheStream, this.provideBiomePaletteInitializer());
                        continue;
                    }
                    throw new IOException("invalid cache state - got identifier: 0x" + identifier);
                }
            }
        }
        try (final DataInputStream cacheStream = new DataInputStream(new FastBufferedInputStream(
                new LZ4BlockInputStream(new FastBufferedInputStream(new ByteArrayInputStream(this.dataCache)))));
             final NBTInputStream cacheNbtIn = new NBTInputStream(cacheStream)) {
            while ((identifier = cacheStream.readByte()) != CACHE_IDENTIFIER_END) {
                switch (identifier) {
                    case CACHE_IDENTIFIER_BLOCK -> this.readPaletteData(cacheStream, this.getBlockWriter(clipboard));
                    case CACHE_IDENTIFIER_BIOMES -> this.readPaletteData(cacheStream, this.getBiomeWriter(clipboard));
                    case CACHE_IDENTIFIER_ENTITIES -> {
                        cacheStream.skipNBytes(1); // list child type (TAG_Compound)
                        this.readEntityContainers(
                                cacheStream,
                                cacheNbtIn,
                                DataFixer.FixTypes.ENTITY,
                                this.provideEntityTransformer(clipboard)
                        );
                    }
                    case CACHE_IDENTIFIER_BLOCK_TILE_ENTITIES -> {
                        cacheStream.skipNBytes(1); // list child type (TAG_Compound)
                        this.readEntityContainers(
                                cacheStream,
                                cacheNbtIn,
                                DataFixer.FixTypes.BLOCK_ENTITY,
                                this.provideTileEntityTransformer(clipboard)
                        );
                    }
                    default -> throw new IOException("invalid cache state - got identifier: 0x" + identifier);
                }
            }
        }
    }

    /**
     * Reset the main stream of this clipboard and reads all remaining data that could not be read or fixed yet.
     * Might need two iterations if the DataVersion tag is after the Blocks tag while the Palette inside the Blocks tag is not
     * at the first position.
     *
     * @param clipboard The clipboard to write into.
     * @throws IOException on I/O error.
     */
    private void readRemainingDataReset(Clipboard clipboard) throws IOException {
        byte type;
        String tag;
        outer:
        while (!this.remainingTags.isEmpty()) {
            this.reset();
            skipHeader(this.dataInputStream);
            while ((type = dataInputStream.readByte()) != NBTConstants.TYPE_END) {
                tag = dataInputStream.readUTF();
                byte b = tag.equals("Blocks") ? CACHE_IDENTIFIER_BLOCK :
                        tag.equals("Biomes") ? CACHE_IDENTIFIER_BIOMES :
                                tag.equals("Entities") ? CACHE_IDENTIFIER_ENTITIES :
                                        CACHE_IDENTIFIER_END;
                if (!this.remainingTags.remove(b)) {
                    this.nbtInputStream.readTagPayloadLazy(type, 0);
                    continue;
                }
                switch (tag) {
                    case "Blocks" -> readBlocks(clipboard);
                    case "Biomes" -> readBiomes(clipboard);
                    case "Entities" -> readEntities(clipboard);
                    default -> this.nbtInputStream.readTagPayloadLazy(type, 0); // Should never happen, but just in case
                }
                if (this.remainingTags.isEmpty()) {
                    break outer;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Requires {@link #read()}, {@link #read(UUID)} or {@link #read(UUID, Function)} to be called before.
     */
    @Override
    public OptionalInt getDataVersion() {
        return this.dataVersion > -1 ? OptionalInt.of(this.dataVersion) : OptionalInt.empty();
    }

    private void readBlocks(Clipboard target) throws IOException {
        this.blockPalette = new BlockState[BlockTypesCache.states.length];
        readPalette(
                target != null,
                CACHE_IDENTIFIER_BLOCK,
                () -> this.blockPalette[0] != null,
                this.provideBlockPaletteInitializer(),
                this.getBlockWriter(target),
                (type, tag) -> {
                    if (!tag.equals("BlockEntities")) {
                        try {
                            this.nbtInputStream.readTagPayloadLazy(NBTConstants.TYPE_LIST, 0);
                        } catch (IOException e) {
                            LOGGER.error("Failed to skip additional tag", e);
                        }
                        return;
                    }
                    try {
                        this.readTileEntities(target);
                    } catch (IOException e) {
                        LOGGER.warn("Failed to read tile entities", e);
                    }
                }
        );
    }

    private void readBiomes(Clipboard target) throws IOException {
        this.biomePalette = new BiomeType[BiomeType.REGISTRY.size()];
        readPalette(
                target != null,
                CACHE_IDENTIFIER_BIOMES,
                () -> this.biomePalette[0] != null,
                this.provideBiomePaletteInitializer(),
                this.getBiomeWriter(target),
                (type, tag) -> {
                    try {
                        this.nbtInputStream.readTagPayloadLazy(type, 0);
                    } catch (IOException e) {
                        LOGGER.error("Failed to skip additional tag in biome container: {}", tag, e);
                    }
                }
        );
    }

    private void readEntities(@Nullable Clipboard target) throws IOException {
        if (target == null || this.dataFixer == null) {
            if (supportsReset()) {
                this.remainingTags.add(CACHE_IDENTIFIER_ENTITIES);
                this.nbtInputStream.readTagPayloadLazy(NBTConstants.TYPE_LIST, 0);
                return;
            }
            // Easier than streaming for now
            final NBTOutputStream cacheStream = new NBTOutputStream(this.getDataCacheWriter());
            cacheStream.writeByte(CACHE_IDENTIFIER_ENTITIES);
            cacheStream.writeTagPayload(this.nbtInputStream.readTagPayload(NBTConstants.TYPE_LIST, 0));
            return;
        }
        if (this.dataInputStream.read() != NBTConstants.TYPE_COMPOUND) {
            throw new IOException("Expected a compound block for entity");
        }
        this.readEntityContainers(
                this.dataInputStream, this.nbtInputStream, DataFixer.FixTypes.ENTITY, this.provideEntityTransformer(target)
        );
    }

    private void readTileEntities(Clipboard target) throws IOException {
        if (target == null || this.dataFixer == null) {
            if (supportsReset()) {
                this.remainingTags.add(CACHE_IDENTIFIER_BLOCK); // use block identifier, as this method will be called by
                // readBlocks again
                this.nbtInputStream.readTagPayloadLazy(NBTConstants.TYPE_LIST, 0);
                return;
            }
            // Easier than streaming for now
            final NBTOutputStream cacheStream = new NBTOutputStream(this.getDataCacheWriter());
            cacheStream.writeByte(CACHE_IDENTIFIER_BLOCK_TILE_ENTITIES);
            cacheStream.writeTagPayload(this.nbtInputStream.readTagPayload(NBTConstants.TYPE_LIST, 0));
            return;
        }
        if (this.dataInputStream.read() != NBTConstants.TYPE_COMPOUND) {
            throw new IOException("Expected a compound block for tile entity");
        }
        this.readEntityContainers(
                this.dataInputStream,
                this.nbtInputStream,
                DataFixer.FixTypes.BLOCK_ENTITY,
                this.provideTileEntityTransformer(target)
        );
    }

    private void readEntityContainers(
            DataInputStream stream,
            NBTInputStream nbtStream,
            DataFixer.FixType<LinCompoundTag> fixType,
            EntityTransformer transformer
    ) throws IOException {
        double x, y, z;
        LinCompoundTag tag;
        String id;
        byte type;
        int count = stream.readInt();
        while (count-- > 0) {
            x = -1;
            y = -1;
            z = -1;
            tag = null;
            id = null;
            while ((type = stream.readByte()) != NBTConstants.TYPE_END) {
                switch (type) {
                    // Depending on the type of entity container (tile vs "normal") the pos consists of either doubles or ints
                    case NBTConstants.TYPE_INT_ARRAY -> {
                        if (!stream.readUTF().equals("Pos")) {
                            throw new IOException("Expected INT_ARRAY tag to be Pos");
                        }
                        stream.skipNBytes(4); // count of following ints - for pos = 3
                        x = stream.readInt();
                        y = stream.readInt();
                        z = stream.readInt();
                    }
                    case NBTConstants.TYPE_LIST -> {
                        if (!stream.readUTF().equals("Pos")) {
                            throw new IOException("Expected LIST tag to be Pos");
                        }
                        if (stream.readByte() != NBTConstants.TYPE_DOUBLE) {
                            throw new IOException("Expected LIST Pos tag to contain DOUBLE");
                        }
                        stream.skipNBytes(4); // count of following doubles - for pos = 3
                        x = stream.readDouble();
                        y = stream.readDouble();
                        z = stream.readDouble();
                    }
                    case NBTConstants.TYPE_STRING -> {
                        if (!stream.readUTF().equals("Id")) {
                            throw new IOException("Expected STRING tag to be Id");
                        }
                        id = stream.readUTF();
                    }
                    case NBTConstants.TYPE_COMPOUND -> {
                        if (!stream.readUTF().equals("Data")) {
                            throw new IOException("Expected COMPOUND tag to be Data");
                        }
                        if (!(nbtStream.readTagPayload(NBTConstants.TYPE_COMPOUND, 0).toLinTag() instanceof LinCompoundTag lin)) {
                            throw new IOException("Data tag could not be read into LinCompoundTag");
                        }
                        tag = lin;
                    }
                    default -> throw new IOException("Unexpected tag in compound: " + type);
                }
            }
            if (id == null) {
                throw new IOException("Missing Id tag in compound");
            }
            if (x < 0 || y < 0 || z < 0) {
                throw new IOException("Missing position for entity " + id);
            }
            if (tag == null) {
                transformer.transform(x, y, z, id, LinCompoundTag.of(Map.of()));
                continue;
            }
            tag = tag.toBuilder().putString("id", id).remove("Id").build();
            tag = this.dataFixer.fixUp(fixType, tag);
            if (tag == null) {
                LOGGER.warn("Failed to fix-up entity for {} @ {},{},{} - skipping", id, x, y, z);
                continue;
            }
            transformer.transform(x, y, z, id, tag);
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
            boolean hasClipboard,
            byte paletteType,
            BooleanSupplier paletteAlreadyInitialized,
            PaletteInitializer paletteInitializer,
            PaletteDataApplier paletteDataApplier,
            AdditionalTagConsumer additionalTag
    ) throws IOException {
        boolean hasPalette = paletteAlreadyInitialized.getAsBoolean();
        byte type;
        String tag;
        while ((type = this.dataInputStream.readByte()) != NBTConstants.TYPE_END) {
            tag = this.dataInputStream.readUTF();
            if (tag.equals("Palette")) {
                if (hasPalette) {
                    // Skip palette, as already exists
                    this.nbtInputStream.readTagPayloadLazy(NBTConstants.TYPE_COMPOUND, 0);
                    continue;
                }
                if (!this.readPaletteMap(this.dataInputStream, paletteInitializer)) {
                    if (this.supportsReset()) {
                        // Couldn't read - skip palette for now
                        this.remainingTags.add(paletteType);
                        this.nbtInputStream.readTagPayloadLazy(NBTConstants.TYPE_COMPOUND, 0);
                        continue;
                    }
                    // Reset not possible, write into cache
                    final NBTOutputStream cacheWriter = new NBTOutputStream(this.getPaletteCacheWriter());
                    cacheWriter.write(paletteType);
                    cacheWriter.writeTagPayload(this.nbtInputStream.readTagPayload(NBTConstants.TYPE_COMPOUND, 0));
                    continue;
                }
                hasPalette = true;
                continue;
            }
            if (tag.equals("Data")) {
                // No palette or dimensions are yet available
                if (!hasPalette || this.dataFixer == null || !hasClipboard) {
                    if (this.supportsReset()) {
                        this.remainingTags.add(paletteType);
                        this.nbtInputStream.readTagPayloadLazy(NBTConstants.TYPE_BYTE_ARRAY, 0);
                        continue;
                    }
                    // Reset not possible, write into cache
                    int byteLen = this.dataInputStream.readInt();
                    final DataOutputStream cacheWriter = new DataOutputStream(this.getDataCacheWriter());
                    cacheWriter.write(paletteType);
                    cacheWriter.writeInt(byteLen);
                    IOUtil.copy(this.dataInputStream, cacheWriter, byteLen);
                    continue;
                }
                this.readPaletteData(this.dataInputStream, paletteDataApplier);
                continue;
            }
            additionalTag.accept(type, tag);
        }
    }

    private void readPaletteData(DataInputStream stream, PaletteDataApplier applier) throws IOException {
        int length = stream.readInt();
        // Write data into clipboard
        int i = 0;
        if (needsVarIntReading(length)) {
            for (var iter = new VarIntStreamIterator(stream, length); iter.hasNext(); i++) {
                applier.apply(i, (char) iter.nextInt());
            }
            return;
        }
        while (i < length) {
            applier.apply(i++, (char) stream.readUnsignedByte());
        }
    }

    /**
     * Reads the CompoundTag containing the palette mapping ({@code index: value}) and passes each entry to the
     * {@link PaletteInitializer}.
     * <p>
     * This method expects that the identifier ({@link NBTConstants#TYPE_COMPOUND}) is already consumed from the stream.
     *
     * @param stream      The stream to read the data from.
     * @param initializer The initializer called for each entry with its index and backed value.
     * @return {@code true} if the mapping could be read, {@code false} otherwise (e.g. DataFixer is not yet available).
     * @throws IOException on I/O error.
     */
    private boolean readPaletteMap(DataInputStream stream, PaletteInitializer initializer) throws IOException {
        if (this.dataFixer == null) {
            return false;
        }
        while (stream.readByte() != NBTConstants.TYPE_END) {
            String value = stream.readUTF();
            char index = (char) stream.readInt();
            initializer.initialize(index, value);
        }
        return true;
    }

    private void indexToPosition(int index, PositionConsumer supplier) {
        int y = index / (dimensions.x() * dimensions.z());
        int remainder = index - (y * dimensions.x() * dimensions.z());
        int z = remainder / dimensions.x();
        int x = remainder - z * dimensions.x();
        supplier.accept(x, y, z);
    }

    private PaletteDataApplier getBlockWriter(Clipboard target) {
        if (target instanceof LinearClipboard linearClipboard) {
            return (index, ordinal) -> linearClipboard.setBlock(index, this.blockPalette[ordinal]);
        }
        return (index, ordinal) -> indexToPosition(index, (x, y, z) -> target.setBlock(x, y, z, this.blockPalette[ordinal]));
    }

    private PaletteDataApplier getBiomeWriter(Clipboard target) {
        return (index, ordinal) -> indexToPosition(index, (x, y, z) -> target.setBiome(x, y, z, this.biomePalette[ordinal]));
    }

    private PaletteInitializer provideBlockPaletteInitializer() {
        return (index, value) -> {
            if (this.dataFixer == null) {
                throw new IllegalStateException("Can't read block palette map if DataFixer is not yet available");
            }
            value = dataFixer.fixUp(DataFixer.FixTypes.BLOCK_STATE, value);
            try {
                this.blockPalette[index] = BlockState.get(value);
            } catch (InputParseException e) {
                LOGGER.warn("Invalid BlockState in palette: {}. Block will be replaced with air.", value);
                this.blockPalette[index] = BlockTypes.AIR.getDefaultState();
            }
        };
    }

    private PaletteInitializer provideBiomePaletteInitializer() {
        return (index, value) -> {
            if (this.dataFixer == null) {
                throw new IllegalStateException("Can't read biome palette map if DataFixer is not yet available");
            }
            value = dataFixer.fixUp(DataFixer.FixTypes.BIOME, value);
            BiomeType biomeType = BiomeTypes.get(value);
            if (biomeType == null) {
                biomeType = BiomeTypes.PLAINS;
                LOGGER.warn("Invalid biome type in palette: {}. Biome will be replaced with plains.", value);
            }
            this.biomePalette[index] = biomeType;
        };
    }

    private EntityTransformer provideEntityTransformer(Clipboard clipboard) {
        return (x, y, z, id, tag) -> {
            EntityType type = EntityType.REGISTRY.get(id);
            if (type == null) {
                LOGGER.warn("Invalid entity id: {} - skipping", id);
                return;
            }
            clipboard.createEntity(
                    new Location(clipboard, Location.at(x, y, z).add(clipboard.getMinimumPoint().toVector3())),
                    new BaseEntity(type, LazyReference.computed(tag))
            );
        };
    }

    private EntityTransformer provideTileEntityTransformer(Clipboard clipboard) {
        return (x, y, z, id, tag) -> clipboard.tile(
                MathMan.roundInt(x + clipboard.getMinimumPoint().x()),
                MathMan.roundInt(y + clipboard.getMinimumPoint().y()),
                MathMan.roundInt(z + clipboard.getMinimumPoint().z()),
                FaweCompoundTag.of(tag)
        );
    }

    /**
     * @return {@code true} if {@code Width}, {@code Length} and {@code Height} are already read from the stream
     */
    private boolean areDimensionsAvailable() {
        return this.dimensions.x() != 0 && this.dimensions.y() != 0 && this.dimensions.z() != 0;
    }

    /**
     * Closes this reader instance and all underlying resources.
     *
     * @throws IOException on I/O error.
     */
    @Override
    public void close() throws IOException {
        parentStream.close(); // closes all underlying resources implicitly
    }

    /**
     * Resets the main stream to the previously marked position ({@code 0}), if supported (see {@link #supportsReset()}).
     * If the stream is reset, the sub streams (for DataInput and NBT) are re-created to respect the new position.
     *
     * @throws IOException on I/O error.
     */
    private void reset() throws IOException {
        if (this.supportsReset()) {
            this.parentStream.reset();
            this.parentStream.mark(Integer.MAX_VALUE);
            this.setSubStreams();
        }
    }

    /**
     * @return {@code true} if the stream used while instantiating the reader supports resets (without memory overhead).
     */
    private boolean supportsReset() {
        return this.remainingTags != null;
    }

    /**
     * Overwrites the DataInput- and NBT-InputStreams (e.g. when the marker of the backed stream updated).
     *
     * @throws IOException on I/O error.
     */
    private void setSubStreams() throws IOException {
        final FastBufferedInputStream buffer = new FastBufferedInputStream(new GZIPInputStream(this.parentStream));
        this.dataInputStream = new DataInputStream(buffer);
        this.nbtInputStream = new NBTInputStream(buffer);
    }

    /**
     * Creates a new cache writer for non-palette data, if none exists yet.
     * Returns either the already created or new one.
     *
     * @return the output stream for non-palette cache data.
     */
    private OutputStream getDataCacheWriter() {
        if (this.dataCacheWriter == null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(512);
            this.dataCacheWriter = new FastBufferedOutputStream(new LZ4BlockOutputStream(byteArrayOutputStream)) {
                @Override
                public void close() throws IOException {
                    this.write(CACHE_IDENTIFIER_END);
                    super.close();
                    FastSchematicReaderV3.this.dataCache = byteArrayOutputStream.toByteArray();
                }
            };
        }
        return this.dataCacheWriter;
    }

    /**
     * Creates a new cache writer for palette data, if none exists yet.
     * Returns either the already created or new one.
     *
     * @return the output stream for palette cache data.
     */
    private OutputStream getPaletteCacheWriter() {
        if (this.paletteCacheWriter == null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(256);
            this.paletteCacheWriter = new FastBufferedOutputStream(new LZ4BlockOutputStream(byteArrayOutputStream)) {
                @Override
                public void close() throws IOException {
                    this.write(CACHE_IDENTIFIER_END);
                    super.close();
                    FastSchematicReaderV3.this.paletteCache = byteArrayOutputStream.toByteArray();
                }
            };
        }
        return this.paletteCacheWriter;
    }

    private boolean needsVarIntReading(int byteArrayLength) {
        return byteArrayLength > this.dimensions.x() * this.dimensions.y() * this.dimensions.z();
    }

    /**
     * Skips the schematic header including the root compound (empty name) and the root's child compound ("Schematic")
     *
     * @param dataInputStream The stream containing the schematic data to skip
     * @throws IOException on I/O error
     */
    private static void skipHeader(DataInputStream dataInputStream) throws IOException {
        dataInputStream.skipNBytes(1 + 2); // 1 Byte = TAG_Compound, 2 Bytes = Short (Length of tag name = "")
        dataInputStream.skipNBytes(1 + 2 + 9); // as above + 9 bytes = "Schematic"
    }

    @ApiStatus.Internal
    @FunctionalInterface
    private interface PositionConsumer {

        /**
         * Called with block location coordinates.
         *
         * @param x the x coordinate.
         * @param y the y coordinate.
         * @param z the z coordinate.
         */
        void accept(int x, int y, int z);

    }

    @ApiStatus.Internal
    @FunctionalInterface
    private interface EntityTransformer {

        /**
         * Called for each entity from the Schematics {@code Entities} compound list.
         *
         * @param x   the relative x coordinate of the entity.
         * @param y   the relative y coordinate of the entity.
         * @param z   the relative z coordinate of the entity.
         * @param id  the entity id as a resource location (e.g. {@code minecraft:sheep}).
         * @param tag the - already fixed, if required - nbt data of the entity.
         */
        void transform(double x, double y, double z, String id, LinCompoundTag tag);

    }

    @ApiStatus.Internal
    @FunctionalInterface
    private interface PaletteInitializer {

        /**
         * Called for each palette entry (the mapping part, not data).
         *
         * @param index the index of the entry, as used in the Data byte array.
         * @param value the value for this entry (either biome type as resource location or the block state as a string).
         */
        void initialize(char index, String value);

    }

    @ApiStatus.Internal
    @FunctionalInterface
    private interface PaletteDataApplier {

        /**
         * Called for each palette data entry (not the mapping part, but the var-int byte array).
         *
         * @param index   The index of this data entry (due to var-int behaviour not necessarily the index in the data byte array).
         * @param ordinal The ordinal of this entry as defined in the palette mapping.
         */
        void apply(int index, char ordinal);

    }

    @ApiStatus.Internal
    @FunctionalInterface
    private interface AdditionalTagConsumer {

        /**
         * Called for each unknown nbt tag.
         *
         * @param type The type of the tag (as defined by the constants in {@link NBTConstants}).
         * @param name The name of the tag.
         */
        void accept(byte type, String name);

    }

}
