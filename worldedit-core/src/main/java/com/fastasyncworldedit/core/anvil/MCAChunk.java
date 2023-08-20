package com.fastasyncworldedit.core.anvil;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.extent.filter.block.ChunkFilterBlock;
import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.internal.io.FastByteArrayOutputStream;
import com.fastasyncworldedit.core.jnbt.streamer.StreamDelegate;
import com.fastasyncworldedit.core.jnbt.streamer.ValueReader;
import com.fastasyncworldedit.core.math.BitArrayUnstretched;
import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.NBTUtils;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import com.sk89q.worldedit.world.chunk.PackedIntArrayReader;
import com.sk89q.worldedit.world.storage.InvalidFormatException;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.zip.DeflaterOutputStream;

@SuppressWarnings({"deprecation", "removal"})
public class MCAChunk implements IChunk {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private static final BlockState __RESERVED__STATE = BlockTypes.__RESERVED__.getDefaultState();
    private static final BaseBlock __RESERVED__BASE = BlockTypes.__RESERVED__.getDefaultState().toBaseBlock();

    private final MCAFile mcaFile;
    private int chunkX;
    private int chunkZ;

    private final int[][] heightMaps = new int[HeightMapType.values().length][256];
    public Map<BlockVector3, CompoundTag> tiles = new HashMap<>(); // Stored with the world coordinates
    // Cache as may be unused
    private char[][] sectionPalettes;
    private BiomeType[][] biomePalettes;
    private long[][] sectionLongs;
    private long[][] biomeLongs;
    private char[][] blocks;
    private BiomeType[][] biomes;
    private long lastUpdate;
    private long inhabitedTime;
    private Status status;

    private final Map<String, Tag> retained = new HashMap<>();
    private boolean modified = false;
    private boolean deleted = false;
    private int minSectionPosition = Integer.MAX_VALUE;
    private int maxSectionPosition = Integer.MIN_VALUE;
    private int sectionCount;
    private Map<String, Tag>[] sectionsRetained;
    private boolean emptyChunk = false;

    private boolean loadedFromFile = false;

    public MCAChunk(MCAFile mcaFile, NBTInputStream nis, int x, int z, boolean readPos) throws IOException {
        this.mcaFile = mcaFile;
        this.chunkX = x;
        this.chunkZ = z;
        loadFromNIS(nis, readPos);
    }

    public MCAChunk(MCAFile mcaFile, int x, int z) {
        this.mcaFile = mcaFile;
        this.chunkX = x;
        this.chunkZ = z;
    }

    public synchronized void loadFromNIS(NBTInputStream inputStream, boolean readPos) throws IOException {
        if (emptyChunk) {
            return;
        }
        try (NBTInputStream nis = inputStream) {
            nis.mark(Integer.MAX_VALUE);
            StreamDelegate initial = new StreamDelegate();
            StreamDelegate empty = initial.add(null);
            StreamDelegate initialSectionsDelegate = empty.add("sections");
            initialSectionsDelegate.withInfo((length, type) -> {
                sectionCount = length;
                sectionPalettes = new char[length][];
                biomePalettes = new BiomeType[length][];
                sectionLongs = new long[length][];
                biomeLongs = new long[length][];
                blocks = new char[length][];
                biomes = new BiomeType[length][];
                sectionsRetained = (HashMap<String, Tag>[]) new HashMap[length];
            });
            empty.add("yPos").withInt((i, v) -> minSectionPosition = v);
            nis.readNamedTagLazyExceptionally(initial);
            // -2 because indices and empty section at bottom
            maxSectionPosition = minSectionPosition + sectionCount - 2;
            nis.reset();

            // reset
            StreamDelegate first = new StreamDelegate();
            StreamDelegate root = first.add(null).retainOthers();
            root.add("InhabitedTime").withLong((i, v) -> inhabitedTime = v);
            root.add("LastUpdate").withLong((i, v) -> lastUpdate = v);
            root.add("Status").withValue((ValueReader<String>) (i, v) -> status = Status.valueOf(v
                    .substring(v.indexOf(":") + 1)
                    .toUpperCase(Locale.ROOT)));
            root.add("xPos").withInt((i, v) -> {
                if (!readPos) {
                    if (chunkX != v) {
                        throw new IllegalStateException("Stored x position + `" + v + "` doesn't equal given x position `" + chunkX +
                                "`!");
                    }
                } else {
                    chunkX = v;
                }
            });
            root.add("zPos").withInt((i, v) -> {
                if (!readPos) {
                    if (chunkZ != v) {
                        throw new IllegalStateException("Stored z position doesn't equal given z position!");
                    }
                } else {
                    chunkZ = v;
                }
            });
            StreamDelegate sectionsDelegate = root.add("sections");
            sectionsDelegate.withElem((ValueReader<Map<String, Object>>) (i, v) -> {
                CompoundTag sectionTag = FaweCache.INSTANCE.asTag(v);

                Object yValue = sectionTag.getValue().get("Y").getValue(); // sometimes a byte, sometimes an int
                if (!(yValue instanceof Number)) {
                    LOGGER.warn("Y is not numeric: {}. Skipping.", yValue);
                    return;
                }
                int y = ((Number) yValue).intValue();
                if (y < minSectionPosition) {
                    // Bottom, empty, chunk section
                    return;
                }

                blocks:
                {
                    Tag rawBlockStatesTag = sectionTag
                            .getValue()
                            .get("block_states"); // null for sections outside of the world limits
                    if (rawBlockStatesTag instanceof CompoundTag blockStatesTag) {
                        // parse palette
                        List<CompoundTag> paletteEntries = blockStatesTag.getList("palette", CompoundTag.class);
                        int paletteSize = paletteEntries.size();
                        if (paletteSize == 0) {
                            blocks[y - minSectionPosition] = new char[]{BlockTypesCache.ReservedIDs.AIR};
                            break blocks;
                        }
                        char[] palette = new char[paletteSize];
                        for (int paletteEntryId = 0; paletteEntryId < paletteSize; paletteEntryId++) {
                            CompoundTag paletteEntry = paletteEntries.get(paletteEntryId);
                            BlockType type = BlockTypes.get(paletteEntry.getString("Name"));
                            if (type == null) {
                                LOGGER.warn("Invalid block type: {}. Using air", paletteEntry.getString("Name"));
                                palette[paletteEntryId] = BlockTypes.AIR.getDefaultState().getOrdinalChar();
                                continue;
                            }
                            BlockState blockState = type.getDefaultState();
                            if (paletteEntry.containsKey("Properties")) {
                                CompoundTag properties;
                                try {
                                    properties = NBTUtils.getChildTag(paletteEntry.getValue(), "Properties", CompoundTag.class);
                                    for (Property<?> property : blockState.getStates().keySet()) {
                                        if (properties.containsKey(property.getName())) {
                                            String value = properties.getString(property.getName());
                                            blockState = getBlockStateWith(blockState, property, value);
                                        }
                                    }
                                } catch (InvalidFormatException e) {
                                    LOGGER.warn(e.getMessage());
                                }
                            }
                            palette[paletteEntryId] = blockState.getOrdinalChar();
                        }

                        if (paletteSize == 1) {
                            blocks[y - minSectionPosition] = palette;
                        } else {
                            sectionPalettes[y - minSectionPosition] = palette;
                            sectionLongs[y - minSectionPosition] = blockStatesTag.getLongArray("data");
                        }
                    }
                }

                biomes:
                {
                    Tag rawBlockStatesTag = sectionTag.getValue().get("biomes"); // null for sections outside of the world limits
                    if (rawBlockStatesTag instanceof CompoundTag biomesTag) {
                        // parse palette
                        List<? extends Tag<?, ?>> paletteEntries = biomesTag.getList("palette");
                        int paletteSize = paletteEntries.size();
                        if (paletteSize == 0) {
                            break biomes;
                        }
                        BiomeType[] palette = new BiomeType[paletteSize];
                        for (int paletteEntryId = 0; paletteEntryId < paletteSize; paletteEntryId++) {
                            String paletteEntry = paletteEntries.get(paletteEntryId).getValue().toString();
                            BiomeType type = BiomeType.REGISTRY.get(paletteEntry);
                            if (type == null) {
                                LOGGER.warn("Invalid biome type: {}. Defaulting to plains.", paletteEntry);
                                palette[paletteEntryId] = BiomeTypes.PLAINS;
                                continue;
                            }
                            palette[paletteEntryId] = type;
                        }

                        if (paletteSize == 1) {
                            biomes[y - minSectionPosition] = palette;
                        } else {
                            biomePalettes[y - minSectionPosition] = palette;
                            biomeLongs[y - minSectionPosition] = biomesTag.getLongArray("data");
                        }
                    }
                }

                int index = y - minSectionPosition;
                HashMap<String, Tag> tmp = new HashMap<>();
                if (sectionsRetained[index] != null) {
                    tmp.putAll(sectionsRetained[index]);
                }
                sectionTag.getValue().forEach((key, val) -> {
                    if ("biomes".equals(key) || "block_states".equals(key) || "Y".equals(key)) {
                        return;
                    }
                    tmp.put(key, val);
                });
                sectionsRetained[index] = tmp;
            });
            StreamDelegate blockEntitiesDelegate = root.add("block_entities");
            blockEntitiesDelegate.withElem((ValueReader<Map<String, Object>>) (i, v) -> {
                CompoundTag tag = FaweCache.INSTANCE.asTag(v);
                int ex = tag.getInt("x");
                int ey = tag.getInt("y");
                int ez = tag.getInt("z");

                BlockVector3 vec = BlockVector3.at(ex & 15, ey, ez & 15);
                tiles.put(vec, tag);
            });
            StreamDelegate heightmaps = root.add("Heightmaps");
            for (HeightMapType type : HeightMapType.values()) {
                heightmaps.add(type.name()).withValue((ValueReader<long[]>) (i, v) -> new BitArrayUnstretched(9, 256, v).toRaw(
                        heightMaps[type.ordinal()]));
            }
            nis.readNamedTagLazyExceptionally(first);
            if (root.getRetained() != null) {
                retained.putAll(root.getRetained());
            }
            loadedFromFile = true;
        } catch (IOException e) {
            LOGGER.error("Couldn't read chunk data for {}:{},{}", mcaFile.getFile().getFileName(), chunkX, chunkZ, e);
            this.emptyChunk = true;
            throw e;
        }
    }

    public void setEmpty(boolean emptyChunk) {
        this.emptyChunk = emptyChunk;
    }

    private <V> BlockState getBlockStateWith(BlockState source, Property<V> property, String value) {
        return source.with(property, property.getValueFor(value));
    }

    private boolean populateBlocks(int y) {
        if (!loadedFromFile) {
            try {
                mcaFile.loadIntoChunkFromFile(this);
                if (this.emptyChunk) {
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        int index = y - minSectionPosition;
        if (sectionLongs[index] == null || sectionPalettes[index] == null) {
            return blocks[index] != null;
        }
        PackedIntArrayReader reader = new PackedIntArrayReader(sectionLongs[index]);
        blocks[index] = new char[4096];
        for (int blockPos = 0; blockPos < 4096; blockPos++) {
            int i = reader.get(blockPos);
            blocks[index][blockPos] = sectionPalettes[index][i];
        }
        sectionPalettes[index] = null;
        sectionLongs[index] = null;
        return true;
    }

    private boolean populateBiomes(int y) {
        int index = y - minSectionPosition;
        if (biomeLongs[index] == null || biomePalettes[index] == null) {
            return biomes[index] != null;
        }

        PackedIntArrayReader reader = new PackedIntArrayReader(biomeLongs[index], 64);
        biomes[index] = new BiomeType[64];
        for (int biomePos = 0; biomePos < 64; biomePos++) {
            int i = reader.get(biomePos);
            biomes[index][biomePos] = biomePalettes[index][i];
        }
        biomePalettes[index] = null;
        biomeLongs[index] = null;
        return true;
    }

    public byte[] toBytes(byte[] buffer) throws IOException {
        if (buffer == null) {
            buffer = new byte[8192];
        }
        FastByteArrayOutputStream buffered = new FastByteArrayOutputStream(buffer);
        DataOutputStream dataOut = new DataOutputStream(new DeflaterOutputStream(buffered));
        try (NBTOutputStream nbtOut = new NBTOutputStream((DataOutput) dataOut)) {
            write(nbtOut);
        }
        return buffered.toByteArray();
    }

    public void write(NBTOutputStream nbtOut) throws IOException {
        nbtOut.writeLazyCompoundTag("", out -> {
            for (Map.Entry<String, Tag> entry : retained.entrySet()) {
                out.writeNamedTag(entry.getKey(), entry.getValue());
            }
            out.writeNamedTag("V", (byte) 1);
            out.writeNamedTag("xPos", getX());
            out.writeNamedTag("yPos", minSectionPosition);
            out.writeNamedTag("zPos", getZ());
            if (tiles.isEmpty()) {
                out.writeNamedEmptyList("block_entities");
            } else {
                out.writeNamedTag("block_entities", new ListTag(CompoundTag.class, new ArrayList<>(tiles.values())));
            }
            out.writeNamedTag("InhabitedTime", inhabitedTime);
            out.writeNamedTag("LastUpdate", lastUpdate);
            out.writeNamedTag("Status", "minecraft:" + status.name().toLowerCase());
            out.writeLazyCompoundTag("HeightMaps", heightMapOut -> {
                for (int i = 0; i < heightMaps.length; i++) {
                    BitArrayUnstretched bitArray = new BitArrayUnstretched(9, 256);
                    bitArray.fromRaw(heightMaps[i]);
                    heightMapOut.writeNamedTag(HeightMapType.values()[i].name(), bitArray.getData());
                }
            });
            out.writeNamedTagName("sections", NBTConstants.TYPE_LIST);
            nbtOut.getOutputStream().writeByte(NBTConstants.TYPE_COMPOUND);
            int len = 0;
            for (int index = 0; index < sectionCount; index++) {
                if (hasSection(index + minSectionPosition)) {
                    len++;
                }
            }
            nbtOut.getOutputStream().writeInt(len);
            for (int i = 0; i < sectionCount; i++) {
                final int layer = i + minSectionPosition;
                final int index = i;
                if (!hasSection(layer)) {
                    continue;
                }
                out.writeLazyListedCompoundTag(sectionOut -> {
                    for (Map.Entry<String, Tag> entry : sectionsRetained[index].entrySet()) {
                        sectionOut.writeNamedTag(entry.getKey(), entry.getValue());
                    }
                    sectionOut.writeNamedTag("Y", layer);
                     if (biomes[index] != null && biomes[index].length == 1) {
                        sectionOut.writeLazyCompoundTag("biomes", biomesOut -> {
                            biomesOut.writeNamedTagName("palette", NBTConstants.TYPE_LIST);
                            nbtOut.getOutputStream().writeByte(NBTConstants.TYPE_STRING);
                            nbtOut.getOutputStream().writeInt(1);
                            biomesOut.writeUTF(biomes[index][0].getId());
                        });
                    } else {

                    }
                    sectionOut.writeLazyCompoundTag("block_states", blocksOut -> {
                        if (blocks == null || blocks[index] == null) {
                            blocksOut.writeNamedTagName("palette", NBTConstants.TYPE_LIST);
                            nbtOut.getOutputStream().writeByte(NBTConstants.TYPE_COMPOUND);
                            nbtOut.getOutputStream().writeInt(sectionPalettes[index].length);
                            for (int paletteIndex = 0; paletteIndex < sectionPalettes[index].length; paletteIndex++) {
                                final int finalPaletteIndex = paletteIndex;
                                blocksOut.writeLazyListedCompoundTag(paletteOut -> {
                                    int ordinal = sectionPalettes[index][finalPaletteIndex];
                                    final BlockState state = BlockTypesCache.states[ordinal];
                                    blocksOut.writeNamedTag("Name", state.getBlockType().getId());
                                    if (!state.getStates().isEmpty()) {
                                        blocksOut.writeLazyCompoundTag("Properties", propertiesOut -> {
                                            for (Map.Entry<Property<?>, Object> entry : state.getStates().entrySet()) {
                                                propertiesOut.writeNamedTag(
                                                        entry.getKey().getName(),
                                                        String.valueOf(entry.getValue())
                                                );
                                            }
                                        });
                                    }
                                });
                            }
                            blocksOut.writeNamedTag("data", sectionLongs[index]);
                        } else if (blocks[index].length == 1) {
                            blocksOut.writeNamedTagName("palette", NBTConstants.TYPE_LIST);
                            nbtOut.getOutputStream().writeByte(NBTConstants.TYPE_COMPOUND);
                            nbtOut.getOutputStream().writeInt(1);
                            setSinglePalette(index, blocksOut);
                        } else {
                            final int[] blockToPalette = FaweCache.INSTANCE.BLOCK_TO_PALETTE.get();
                            final int[] paletteToBlock = FaweCache.INSTANCE.PALETTE_TO_BLOCK.get();
                            final long[] blockStates = FaweCache.INSTANCE.BLOCK_STATES.get();
                            final int[] blocksCopy = FaweCache.INSTANCE.SECTION_BLOCKS.get();
                            try {
                                int num_palette = createPalette(blockToPalette, paletteToBlock, blocks[index], blocksCopy);
                                blocksOut.writeNamedTagName("palette", NBTConstants.TYPE_LIST);
                                nbtOut.getOutputStream().writeByte(NBTConstants.TYPE_COMPOUND);
                                nbtOut.getOutputStream().writeInt(num_palette);

                                if (num_palette == 1) {
                                    setSinglePalette(index, blocksOut);
                                    return;
                                }

                                int bitsPerEntry = MathMan.log2nlz(num_palette - 1);
                                if (bitsPerEntry > 0 && bitsPerEntry < 5) {
                                    bitsPerEntry = 4;
                                }

                                int bitsPerEntryNonZero = Math.max(bitsPerEntry, 1); // We do want to use zero sometimes
                                final int blocksPerLong = MathMan.floorZero((double) 64 / bitsPerEntryNonZero);
                                final int blockBitArrayEnd = MathMan.ceilZero((float) 4096 / blocksPerLong);

                                final BitArrayUnstretched bitArray = new BitArrayUnstretched(
                                        bitsPerEntryNonZero,
                                        4096,
                                        blockStates);
                                bitArray.fromRaw(blocksCopy);

                                //if (bitsPerEntry < 9) {
                                for (int paletteIndex = 0; paletteIndex < num_palette; paletteIndex++) {
                                    int finalPaletteIndex = paletteIndex;
                                    blocksOut.writeLazyListedCompoundTag(paletteOut -> {
                                        int ordinal = paletteToBlock[finalPaletteIndex];
                                        blockToPalette[ordinal] = Integer.MAX_VALUE;
                                        final BlockState state = BlockTypesCache.states[ordinal];
                                        blocksOut.writeNamedTag("Name", state.getBlockType().getId());
                                        if (!state.getStates().isEmpty()) {
                                            blocksOut.writeLazyCompoundTag("Properties", propertiesOut -> {
                                                for (Map.Entry<Property<?>, Object> entry : state.getStates().entrySet()) {
                                                    propertiesOut.writeNamedTag(entry.getKey().getName(),
                                                        String.valueOf(entry.getValue()));
                                                }
                                            });
                                        }
                                    });
                                }
                                //}

                                if (bitsPerEntry > 0) {
                                    final long[] bits = Arrays.copyOfRange(blockStates, 0, blockBitArrayEnd);
                                    blocksOut.writeNamedTag("data", bits);
                                }
                            } finally {
                                Arrays.fill(blockToPalette, Integer.MAX_VALUE);
                                Arrays.fill(paletteToBlock, Integer.MAX_VALUE);
                                Arrays.fill(blockStates, 0);
                                Arrays.fill(blocksCopy, 0);
                            }
                        }

                    });
                });
            }
        });
        nbtOut.writeEndTag();
    }

    private void setSinglePalette(int index, NBTOutputStream blocksOut) throws IOException {
        BlockState state = BlockState.getFromOrdinal(blocks[index][0]);
        blocksOut.writeLazyListedCompoundTag(paletteOut -> {
            paletteOut.writeNamedTag("Name", state.getBlockType().getId());
            if (!state.getStates().isEmpty()) {
                paletteOut.writeLazyCompoundTag("Properties", propertiesOut -> {
                    for (Map.Entry<Property<?>, Object> entry : state.getStates().entrySet()) {
                        propertiesOut.writeNamedTag(
                            entry.getKey().getName(),
                            String.valueOf(entry.getValue())
                        );
                    }
                });
            }
        });
    }

    private int createPalette(int[] blockToPalette, int[] paletteToBlock, char[] data, int[] dataCopy) {
        int num_palette = 0;
        for (int i = 0; i < 4096; i++) {
            char ordinal = data[i];
            if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                ordinal = BlockTypesCache.ReservedIDs.AIR;
            }
            int palette = blockToPalette[ordinal];
            if (palette == Integer.MAX_VALUE) {
                blockToPalette[ordinal] = num_palette;
                paletteToBlock[num_palette] = ordinal;
                num_palette++;
            }
        }
        int bitsPerEntry = MathMan.log2nlz(num_palette - 1);
        // If bits per entry is over 8, the game uses the global palette.
        if (bitsPerEntry > 8 && WorldEdit
                .getInstance()
                .getPlatformManager()
                .queryCapability(Capability.WORLD_EDITING)
                .getIbdToStateOrdinal() != null) {
            // Cannot System#array copy char[] -> int[];
            char[] ibdToStateOrdinal = WorldEdit
                    .getInstance()
                    .getPlatformManager()
                    .queryCapability(Capability.WORLD_EDITING)
                    .getIbdToStateOrdinal();
            //noinspection ConstantConditions - not null from if statement
            for (int i = 0; i < ibdToStateOrdinal.length; i++) {
                paletteToBlock[i] = ibdToStateOrdinal[i];
            }
            //noinspection ConstantConditions - not null if ibdToStateOrdinal is not null
            System.arraycopy(
                    WorldEdit
                            .getInstance()
                            .getPlatformManager()
                            .queryCapability(Capability.WORLD_EDITING)
                            .getOrdinalToIbdID(),
                    0,
                    blockToPalette,
                    0,
                    WorldEdit
                            .getInstance()
                            .getPlatformManager()
                            .queryCapability(Capability.WORLD_EDITING)
                            .getOrdinalToIbdID().length
            );
        }
        for (int i = 0; i < 4096; i++) {
            char ordinal = data[i];
            if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                ordinal = BlockTypesCache.ReservedIDs.AIR;
            }
            int palette = blockToPalette[ordinal];
            dataCopy[i] = palette;
        }
        return num_palette;
    }

    /**
     * Set the chunk as having been modified
     */
    public void setModified() {
        modified = true;
    }

    public boolean isModified() {
        return modified;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Status getStatus() {
        return status;
    }

    private void checkLoaded() {
        if (!loadedFromFile) {
            try {
                mcaFile.loadIntoChunkFromFile(this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private char getOrdinal(int x, int y, int z) {
        int layer = y >> 4;
        int index = layer - minSectionPosition;
        return blocks[index].length == 1 ? blocks[index][0] : blocks[index][(y & 15) << 8 | (z & 15) << 4 | (x & 15)];
    }

    @Override
    public boolean hasSection(final int layer) {
        checkLoaded();
        if (emptyChunk) {
            return false;
        }
        if (layer < minSectionPosition || layer > maxSectionPosition) {
            return false;
        }
        int index = layer - minSectionPosition;
        if (blocks[index] != null) {
            return true;
        }
        if (sectionLongs[index] != null) {
            return true;
        }
        if (biomes[index] != null) {
            return true;
        }
        return biomeLongs[index] != null;
    }

    @Override
    public char[] load(final int layer) {
        checkLoaded();
        if (emptyChunk || layer < minSectionPosition || layer > maxSectionPosition) {
            return new char[4096];
        }
        int index = layer - minSectionPosition;
        if (blocks[index] != null) {
            if (blocks[index].length == 4096) {
                return blocks[index];
            } else {
                char[] result = new char[4096];
                Arrays.fill(result, blocks[index][0]);
                return result;
            }
        }
        if (sectionLongs[index] == null) {
            return new char[4096];
        }
        populateBlocks(layer);
        if (blocks[index] != null) {
            if (blocks[index].length == 4096) {
                return blocks[index];
            } else {
                char[] result = new char[4096];
                Arrays.fill(result, blocks[index][0]);
                return result;
            }
        }
        return new char[4096];
    }

    @Nullable
    @Override
    public char[] loadIfPresent(final int layer) {
        checkLoaded();
        if (emptyChunk || layer < minSectionPosition || layer > maxSectionPosition) {
            return null;
        }
        int index = layer - minSectionPosition;
        if (blocks[index] != null) {
            if (blocks[index].length == 4096) {
                return blocks[index];
            } else {
                char[] result = new char[4096];
                Arrays.fill(result, blocks[index][0]);
                return result;
            }
        }
        if (sectionLongs[index] == null) {
            return null;
        }
        populateBlocks(layer);
        if (blocks[index] != null) {
            if (blocks[index].length == 4096) {
                return blocks[index];
            } else {
                char[] result = new char[4096];
                Arrays.fill(result, blocks[index][0]);
                return result;
            }
        }
        return null;
    }

    @Override
    public Map<BlockVector3, CompoundTag> getTiles() {
        checkLoaded();
        return tiles;
    }

    @Override
    public CompoundTag getTile(final int x, final int y, final int z) {
        checkLoaded();
        return tiles.get(BlockVector3.at((x & 15) + (chunkX << 4), y, (z & 15) + (chunkZ << 4)));
    }

    @Override
    public Set<CompoundTag> getEntities() {
        throw new UnsupportedOperationException("Not supported in Anvil queue mode.");
    }

    @Override
    public void removeSectionLighting(final int layer, final boolean sky) {
        throw new UnsupportedOperationException("Not supported in Anvil queue mode.");
    }

    @Override
    public boolean trim(final boolean aggressive, final int layer) {
        return true;
    }

    @Override
    public boolean setBiome(final int x, final int y, final int z, final BiomeType biome) {
        checkLoaded();
        int layer = y >> 4;
        if (emptyChunk | layer < minSectionPosition || layer > maxSectionPosition) {
            return false;
        }
        if (!populateBiomes(layer)) {
            return false;
        }
        if (emptyChunk) {
            return false;
        }
        int index = layer - minSectionPosition;
        int bx = (x & 15) >> 2;
        int by = (y & 15) >> 2;
        int bz = (z & 15) >> 2;
        if (biomes[index].length == 1) {
            if (biomes[index][0] == biome) {
                return true;
            }
            biomes[index] = new BiomeType[64];
            Arrays.fill(biomes[index], biomes[index][0]);
        }
        biomes[index][by << 4 | bz << 2 | bx] = biome;
        setModified();
        return true;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(final int x, final int y, final int z, final T holder) {
        checkLoaded();
        int layer = y >> 4;
        if (emptyChunk || layer < minSectionPosition || layer > maxSectionPosition) {
            return false;
        }
        if (!populateBlocks(layer)) {
            return false;
        }
        if (emptyChunk) {
            return false;
        }
        int index = layer - minSectionPosition;
        char toSet = holder.getOrdinalChar();
        if (blocks[index].length == 1) {
            if (blocks[index][0] == toSet) {
                return true;
            }
            blocks[index] = new char[4096];
            Arrays.fill(blocks[index], blocks[index][0]);
        }
        blocks[index][(y & 15) << 8 | (z & 15) << 4 | (x & 15)] = toSet;
        setModified();
        return true;
    }

    @Override
    public void setBlocks(final int layer, final char[] data) {
        checkLoaded();
        if (emptyChunk || layer < minSectionPosition || layer > maxSectionPosition) {
            return;
        }
        int index = layer - minSectionPosition;
        blocks[index] = data;
        setModified();
    }

    @Override
    public boolean isEmpty() {
        checkLoaded();
        if (emptyChunk) {
            return true;
        }
        for (int layer = minSectionPosition; layer <= maxSectionPosition; layer++) {
            if (hasSection(layer)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean setTile(final int x, final int y, final int z, final CompoundTag tile) {
        checkLoaded();
        if (emptyChunk) {
            return false;
        }
        tiles.put(BlockVector3.at((x & 15) + (chunkX << 4), y, (z & 15) + (chunkZ << 4)), tile);
        setModified();
        return true;
    }

    @Override
    public void setBlockLight(final int x, final int y, final int z, final int value) {
        throw new UnsupportedOperationException("Not supported by Anvil queue mode.");
    }

    @Override
    public void setSkyLight(final int x, final int y, final int z, final int value) {
        throw new UnsupportedOperationException("Not supported by Anvil queue mode.");
    }

    @Override
    public void setHeightMap(final HeightMapType type, final int[] heightMap) {
        checkLoaded();
        heightMaps[type.ordinal()] = heightMap;
    }

    @Override
    public void setLightLayer(final int layer, final char[] toSet) {
        throw new UnsupportedOperationException("Not supported by Anvil queue mode.");
    }

    @Override
    public void setSkyLightLayer(final int layer, final char[] toSet) {
        throw new UnsupportedOperationException("Not supported by Anvil queue mode.");
    }

    @Override
    public void setFullBright(final int layer) {
        throw new UnsupportedOperationException("Not supported by Anvil queue mode.");
    }

    @Override
    public void setEntity(final CompoundTag tag) {
        throw new UnsupportedOperationException("Not supported by Anvil queue mode.");
    }

    @Override
    public void removeEntity(final UUID uuid) {
        throw new UnsupportedOperationException("Not supported by Anvil queue mode.");
    }

    @Override
    public Set<UUID> getEntityRemoves() {
        return null;
    }

    @Override
    public BiomeType[][] getBiomes() {
        checkLoaded();
        if (emptyChunk) {
            return null;
        }
        for (int index = 0; index < biomes.length; index++) {
            BiomeType[] sectionBiomes = biomes[index];
            if (sectionBiomes.length == 1) {
                biomes[index] = new BiomeType[64];
                Arrays.fill(biomes[index], biomes[index][0]);
            }
        }
        return biomes;
    }

    @Override
    public char[][] getLight() {
        throw new UnsupportedOperationException("Not supported by Anvil queue mode.");
    }

    @Override
    public char[][] getSkyLight() {
        throw new UnsupportedOperationException("Not supported by Anvil queue mode.");
    }

    @Override
    public boolean hasBiomes(final int layer) {
        checkLoaded();
        if (emptyChunk || layer < minSectionPosition || layer > maxSectionPosition) {
            return false;
        }
        return biomes.length != 0 && biomes[layer - minSectionPosition] != null;
    }

    @Override
    public int getSectionCount() {
        checkLoaded();
        return sectionCount;
    }

    @Override
    public int getMaxSectionPosition() {
        checkLoaded();
        return maxSectionPosition;
    }

    @Override
    public int getMinSectionPosition() {
        checkLoaded();
        return minSectionPosition;
    }

    @Override
    public BaseBlock getFullBlock(final int x, final int y, final int z) {
        checkLoaded();
        if (emptyChunk) {
            return __RESERVED__BASE;
        }
        BlockState state = getBlock(x, y, z);
        if (state == null) {
            return __RESERVED__BASE;
        }
        CompoundTag tag = getTile(x, y, z);
        return tag == null ? state.toBaseBlock() : state.toBaseBlock(tag);
    }

    @Override
    public BiomeType getBiomeType(final int x, final int y, final int z) {
        checkLoaded();
        int layer = y >> 4;
        if (emptyChunk || layer < minSectionPosition || layer > maxSectionPosition) {
            return null;
        }
        if (populateBiomes(layer)) {
            if (emptyChunk) {
                return null;
            }
            int index = layer - minSectionPosition;
            int bx = (x & 15) >> 2;
            int by = (y & 15) >> 2;
            int bz = (z & 15) >> 2;
            return biomes[index][by << 4 | bz << 2 | bx];
        }
        return null;
    }

    @Override
    public BlockState getBlock(final int x, final int y, final int z) {
        checkLoaded();
        int layer = y >> 4;
        if (emptyChunk || layer < minSectionPosition || layer > maxSectionPosition) {
            return __RESERVED__STATE;
        }
        if (populateBlocks(layer)) {
            if (emptyChunk) {
                return __RESERVED__STATE;
            }
            return BlockState.getFromOrdinal(getOrdinal(x, y, z));
        }
        return __RESERVED__STATE;
    }

    @Override
    public int getSkyLight(final int x, final int y, final int z) {
        throw new UnsupportedOperationException("Not supported in Anvil queue mode.");
    }

    @Override
    public int getEmittedLight(final int x, final int y, final int z) {
        throw new UnsupportedOperationException("Not supported in Anvil queue mode.");
    }

    @Override
    public int[] getHeightMap(final HeightMapType type) {
        checkLoaded();
        return heightMaps[type.ordinal()];
    }

    @Override
    public <V extends Future<V>> V call(final IChunkSet set, final Runnable finalize) {
//        if (status != Status.FULL) {
//            return (V) Futures.immediateFuture(null);
//        }
        tiles.entrySet().removeIf(e -> {
            BlockVector3 pos = e.getKey();
            return set
                    .getBlock(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ())
                    .getOrdinalChar() != BlockTypesCache.ReservedIDs.__RESERVED__;
        });
        for (int layer = set.getMinSectionPosition(); layer <= set.getMaxSectionPosition(); layer++) {
            char[] toSet = set.loadIfPresent(layer);
            int index = layer - minSectionPosition;
            int setIndex = layer - set.getMinSectionPosition();
            if (toSet != null) {
                if (populateBlocks(layer)) {
                    if (blocks[index].length == 1) {
                        char c = blocks[index][0];
                        blocks[index] = new char[4096];
                        Arrays.fill(blocks[index], c);
                    }
                    for (int i = 0; i < 4096; i++) {
                        char c = toSet[i];
                        if (c != BlockTypesCache.ReservedIDs.__RESERVED__) {
                            blocks[index][i] = c;
                        }
                    }
                }
            }
            if (set.hasBiomes(layer)) {
                if (populateBiomes(layer)) {
                    if (biomes[index].length == 1) {
                        BiomeType b = biomes[index][0];
                        blocks[index] = new char[4096];
                        Arrays.fill(biomes[index], b);
                    }
                    for (int i = 0; i < 64; i++) {
                        BiomeType b = set.getBiomes()[setIndex][i];
                        if (b != null) {
                            biomes[index][i] = b;
                        }
                    }
                }
            }
            if (set.getTiles() != null) {
                tiles.putAll(set.getTiles());
            }
        }
        mcaFile.setChunk(this);

        //noinspection unchecked - required at compile time
        return (V) (Future) Fawe.instance().getQueueHandler().sync(finalize);
    }

    @Override
    public CompoundTag getEntity(final UUID uuid) {
        throw new UnsupportedOperationException("Not supported in Anvil queue mode.");
    }

    @Override
    public boolean isCreateCopy() {
        return false;
    }

    @Override
    public int setCreateCopy(final boolean createCopy) {
        if (createCopy) {
            throw new UnsupportedOperationException("Not supported in Anvil queue mode.");
        }
        return -1;
    }

    @Override
    public void setLightingToGet(final char[][] lighting, final int startSectionIndex, final int endSectionIndex) {
        throw new UnsupportedOperationException("Not supported in Anvil queue mode.");
    }

    @Override
    public void setSkyLightingToGet(final char[][] lighting, final int startSectionIndex, final int endSectionIndex) {
        throw new UnsupportedOperationException("Not supported in Anvil queue mode.");
    }

    @Override
    public void setHeightmapToGet(final HeightMapType type, final int[] data) {
        checkLoaded();
        heightMaps[type.ordinal()] = data;
    }

    @Override
    public int getMinY() {
        checkLoaded();
        return minSectionPosition >> 4;
    }

    @Override
    public int getMaxY() {
        checkLoaded();
        return (maxSectionPosition >> 4) + 15;
    }

    @Override
    public boolean trim(final boolean aggressive) {
        return false;
    }

    public int getX() {
        return chunkX;
    }

    public int getZ() {
        return chunkZ;
    }

    @Override
    public void filterBlocks(
            final Filter filter, final ChunkFilterBlock block, @Nullable final Region region, final boolean full
    ) {

    }

    public long getInhabitedTime() {
        return inhabitedTime;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(final long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public enum Status {
        EMPTY,
        STRUCTURE_STARTS,
        STRUCTURE_REFERENCES,
        BIOMES,
        NOISE,
        SURFACE,
        CARVERS,
        LIQUID_CARVERS,
        FEATURES,
        INITIALIZE_LIGHT,
        LIGHT,
        SPAWN,
        HEIGHTMAPS,
        FULL
    }

    @Override
    public int hashCode() {
        return MathMan.pairSearchCoords(chunkX, chunkZ);
    }

}
