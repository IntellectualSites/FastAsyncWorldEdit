package com.boydti.fawe.jnbt.anvil;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.implementation.filter.block.ChunkFilterBlock;
import com.boydti.fawe.jnbt.streamer.StreamDelegate;
import com.boydti.fawe.jnbt.streamer.ValueReader;
import com.boydti.fawe.object.collection.BitArray4096;
import com.boydti.fawe.object.collection.BlockVector3ChunkMap;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockID;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import it.unimi.dsi.fastutil.io.FastByteArrayOutputStream;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

public class MCAChunk implements IChunk {
    public final boolean[] hasSections = new boolean[16];

    public boolean hasBiomes = false;
    public final BiomeType[] biomes = new BiomeType[1024];

    public final char[] blocks = new char[65536];

    public final BlockVector3ChunkMap<CompoundTag> tiles = new BlockVector3ChunkMap<>();
    public final Map<UUID, CompoundTag> entities = new HashMap<>();
    public long inhabitedTime = System.currentTimeMillis();
    public long lastUpdate;

    public int modified;
    public boolean deleted;

    public int chunkX;
    public int chunkZ;

    public MCAChunk() {}

    private boolean readLayer(Section section) {
        if (section.palette == null || section.layer == -1 || section.blocksLength == -1 || section.palette[section.palette.length - 1] == null || section.blocks == null) {
            // not initialized
            return false;
        }

        int bitsPerEntry = MathMan.log2nlz(section.palette.length - 1);
        BitArray4096 bitArray = new BitArray4096(section.blocks, bitsPerEntry);
        char[] buffer = FaweCache.INSTANCE.getSectionBitsToChar().get();
        bitArray.toRaw(buffer);
        int offset = section.layer << 12;
        for (int i = 0; i < buffer.length; i++) {
            BlockState block = section.palette[buffer[i]];
            blocks[offset + i] = block.getOrdinalChar();
        }

        section.layer = -1;
        section.blocksLength = -1;
        section.blocks = null;
        section.palette = null;
        return true;
    }

    private static class Section {
        public int layer = -1;
        public long[] blocks;
        public int blocksLength = -1;
        public BlockState[] palette;
    }

    public MCAChunk(NBTInputStream nis, int chunkX, int chunkZ, boolean readPos) throws IOException {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        read(nis, readPos);
    }

    @Override
    public <V extends IChunk> void init(IQueueExtent<V> extent, int x, int z) {
        if (x != chunkX || z != chunkZ) {
            throw new UnsupportedOperationException("Not reuse capable");
        }
    }

    public void read(NBTInputStream nis, boolean readPos) throws IOException {
        StreamDelegate root = createDelegate(nis, readPos);
        nis.readNamedTagLazy(root);
    }

    public StreamDelegate createDelegate(NBTInputStream nis, boolean readPos) {
        StreamDelegate root = new StreamDelegate();
        StreamDelegate level = root.add("").add("Level");

        level.add("InhabitedTime").withLong((i, v) -> inhabitedTime = v);
        level.add("LastUpdate").withLong((i, v) -> lastUpdate = v);

        if (readPos) {
            level.add("xPos").withInt((i, v) -> MCAChunk.this.chunkX = v);
            level.add("zPos").withInt((i, v) -> MCAChunk.this.chunkZ = v);
        }

        Section section = new Section();

        StreamDelegate layers = level.add("Sections");
        StreamDelegate layer = layers.add();
        layer.withInfo((length, type) -> {
            section.layer = -1;
            section.blocksLength = -1;
        });
        layer.add("Y").withInt((i, y) -> section.layer = y);
        layer.add("Palette").withElem((ValueReader<Map<String, Object>>) (index, map) -> {
            String name = (String) map.get("Name");
            BlockType type = BlockTypes.get(name);
            BlockState state = type.getDefaultState();
            Map<String, String> properties = (Map<String, String>) map.get("Properties");
            if (properties != null) {
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    Property<Object> property = type.getProperty(key);
                    state = state.with(property, property.getValueFor(value));
                }
            }
            section.palette[index] = state;
            readLayer(section);
        });
        StreamDelegate blockStates = layer.add("BlockStates");
        blockStates.withInfo((length, type) -> {
            if (section.blocks == null) {
                section.blocks = FaweCache.INSTANCE.getLONG_BUFFER_1024().get();
            }
            section.blocksLength = length;
        });
        blockStates.withLong((index, value) -> section.blocks[index] = value);
        level.add("TileEntities").withElem((ValueReader<Map<String, Object>>) (index, value) -> {
            CompoundTag tile = FaweCache.INSTANCE.asTag(value);
            int x = tile.getInt("x") & 15;
            int y = tile.getInt("y");
            int z = tile.getInt("z") & 15;
            tiles.put(x, y, z, tile);
        });
        level.add("Entities").withElem((ValueReader<Map<String, Object>>) (index, value) -> {
            CompoundTag entity = FaweCache.INSTANCE.asTag(value);
            entities.put(entity.getUUID(), entity);
        });
        level.add("Biomes").withInt((index, value) -> biomes[index] = BiomeTypes.getLegacy(value));

        return root;
    }

    @Override
    public int getX() {
        return chunkX;
    }

    @Override
    public int getZ() {
        return chunkZ;
    }

    @Override
    public boolean hasSection(int layer) {
        return hasSections[layer];
    }

    public void setPosition(int X, int Z) {
        this.chunkX = X;
        this.chunkZ = Z;
    }

    @Override
    public MCAChunk reset() {
        return this.reset(true);
    }

    public MCAChunk reset(boolean full) {
        if (!tiles.isEmpty()) {
            tiles.clear();
        }
        if (!entities.isEmpty()) {
            entities.clear();
        }
        modified = 0;
        deleted = false;
        hasBiomes = false;
        if (full) {
            for (int i = 0; i < 65536; i++) {
                blocks[i] = BlockID.AIR;
            }
        }
        Arrays.fill(hasSections, false);
        return this;
    }

    public void write(NBTOutputStream nbtOut) throws IOException {
        int[] blockToPalette = FaweCache.INSTANCE.getBLOCK_TO_PALETTE().get();
        int[] paletteToBlock = FaweCache.INSTANCE.getPALETTE_TO_BLOCK().get();
        long[] blockstates = FaweCache.INSTANCE.getBlockStates().get();
        int[] blocksCopy = FaweCache.INSTANCE.getSectionBlocks().get();

        nbtOut.writeNamedTagName("", NBTConstants.TYPE_COMPOUND);
        nbtOut.writeNamedTag("DataVersion", 1631);
        nbtOut.writeLazyCompoundTag("Level", out -> {
            out.writeNamedTag("Status", "decorated");
            out.writeNamedTag("xPos", getX());
            out.writeNamedTag("zPos", getZ());
            if (entities.isEmpty()) {
                out.writeNamedEmptyList("Entities");
            } else {
                out.writeNamedTag("Entities", new ListTag(CompoundTag.class, new ArrayList<>(entities.values())));
            }
            if (tiles.isEmpty()) {
                out.writeNamedEmptyList("TileEntities");
            } else {
                out.writeNamedTag("TileEntities", new ListTag(CompoundTag.class,
                        new ArrayList<>(tiles.values())));
            }
            out.writeNamedTag("InhabitedTime", inhabitedTime);
            out.writeNamedTag("LastUpdate", lastUpdate);
            if (hasBiomes) {
                int type = NBTConstants.TYPE_INT_ARRAY;
                out.writeNamedTagName("Biomes", type);
                out.writeInt(biomes.length);
                for (BiomeType biome : biomes) {
                    out.write(biome.getLegacyId());
                }
            }
            int len = 0;
            for (boolean hasSection : hasSections) {
                if (hasSection) {
                    len++;
                }
            }
            out.writeNamedTagName("Sections", NBTConstants.TYPE_LIST);
            nbtOut.writeByte(NBTConstants.TYPE_COMPOUND);
            nbtOut.writeInt(len);

            for (int layer = 0; layer < hasSections.length; layer++) {
                if (!hasSections[layer]) {
                    continue;
                }
                out.writeNamedTag("Y", (byte) layer);

                int blockIndexStart = layer << 12;
                int blockIndexEnd = blockIndexStart + 4096;
                int num_palette = 0;
                try {
                    for (int i = blockIndexStart, j = 0; i < blockIndexEnd; i++, j++) {
                        int ordinal = blocks[i];
                        int palette = blockToPalette[ordinal];
                        if (palette == Integer.MAX_VALUE) {
//                            BlockState state = BlockTypesCache.states[ordinal];
                            blockToPalette[ordinal] = palette = num_palette;
                            paletteToBlock[num_palette] = ordinal;
                            num_palette++;
                        }
                        blocksCopy[j] = palette;
                    }

                    for (int i = 0; i < num_palette; i++) {
                        blockToPalette[paletteToBlock[i]] = Integer.MAX_VALUE;
                    }

                    out.writeNamedTagName("Palette", NBTConstants.TYPE_LIST);
                    out.writeByte(NBTConstants.TYPE_COMPOUND);
                    out.writeInt(num_palette);

                    for (int i = 0; i < num_palette; i++) {
                        int ordinal = paletteToBlock[i];
                        BlockState state = BlockTypesCache.states[ordinal];
                        BlockType type = state.getBlockType();
                        out.writeNamedTag("Name", type.getId());

                        // Has no properties
                        if (type.getDefaultState() != state) {
                            // Write properties
                            out.writeNamedTagName("Properties", NBTConstants.TYPE_COMPOUND);
                            for (Property<?> property : type.getProperties()) {
                                String key = property.getName();
                                Object value = state.getState(property);
                                String valueStr = value.toString();
                                if (Character.isUpperCase(valueStr.charAt(0))) {
                                    System.out.println("Invalid uppercase value " + value);
                                    valueStr = valueStr.toLowerCase();
                                }
                                out.writeNamedTag(key, valueStr);
                            }
                            out.writeEndTag();
                        }
                        out.writeEndTag();
                    }


                    // BlockStates
                    int bitsPerEntry = MathMan.log2nlz(num_palette - 1);
                    int blockBitArrayEnd = (bitsPerEntry * 4096) >> 6;
                    if (num_palette == 1) {
                        // Set a value, because minecraft needs it for some  reason
                        blockstates[0] = 0;
                        blockBitArrayEnd = 1;
                    } else {
                        BitArray4096 bitArray = new BitArray4096(blockstates, bitsPerEntry);
                        bitArray.fromRaw(blocksCopy);
                    }

                    out.writeNamedTagName("BlockStates", NBTConstants.TYPE_LONG_ARRAY);
                    out.writeInt(blockBitArrayEnd);
                    for (int i = 0; i < blockBitArrayEnd; i++) {
                        out.writeLong(blockstates[i]);
                    }


//                    out.writeNamedTagName("BlockLight", NBTConstants.TYPE_BYTE_ARRAY);
//                    out.writeInt(2048);
//                    out.write(blockLight, layer << 11, 1 << 11);
//
//                    out.writeNamedTagName("SkyLight", NBTConstants.TYPE_BYTE_ARRAY);
//                    out.writeInt(2048);
//                    out.write(skyLight, layer << 11, 1 << 11);


                    out.writeEndTag();

                    // cleanup
                } catch (Throwable e) {
                    Arrays.fill(blockToPalette, Integer.MAX_VALUE);
                    e.printStackTrace();
                    throw e;
                }
            }
        });
        nbtOut.writeEndTag();
    }

    public FastByteArrayOutputStream toBytes(byte[] buffer) throws IOException {
        if (buffer == null) {
            buffer = new byte[8192];
        }
        FastByteArrayOutputStream buffered = new FastByteArrayOutputStream(buffer);
        try (NBTOutputStream nbtOut = new NBTOutputStream(buffered)) {
            write(nbtOut);
        }
        return buffered;
    }

    public long getInhabitedTime() {
        return inhabitedTime;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setInhabitedTime(long inhabitedTime) {
        this.inhabitedTime = inhabitedTime;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public void setDeleted(boolean deleted) {
        setModified();
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public boolean isEmpty() {
        if (deleted) return true;
        for (boolean hasSection : hasSections) {
            if (hasSection) return false;
        }
        return true;
    }

    public boolean isModified() {
        return modified != 0;
    }

    public int getModified() {
        return modified;
    }

    public final void setModified() {
        this.modified++;
    }

    @Override
    public int getBitMask() {
        int bitMask = 0;
        for (int section = 0; section < hasSections.length; section++) {
            if (hasSections[section]) {
                bitMask += 1 << section;
            }
        }
        return bitMask;
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tile) {
        setModified();
        if (tile != null) {
            tiles.put(x, y, z, tile);
        } else {
            return tiles.remove(x, y, z) != null;
        }
        return true;
    }

    @Override
    public void setEntity(CompoundTag entityTag) {
        setModified();
        long least = entityTag.getLong("UUIDLeast");
        long most = entityTag.getLong("UUIDMost");
        entities.put(new UUID(most, least), entityTag);
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        return this.biomes[(z << 4) | x];
    }

    @Override
    public BiomeType[] getBiomes() {
        return this.biomes;
    }

    @Override
    public boolean setBiome(BlockVector2 pos, BiomeType biome) {
        return this.setBiome(pos.getX(), 0, pos.getZ(), biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        setModified();
        biomes[x + (z << 4)] = biome;
        return true;
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return new HashSet<>(entities.values());
    }

    @Override
    public Map<BlockVector3, CompoundTag> getTiles() {
        return tiles == null ? Collections.emptyMap() : tiles;
    }

    public CompoundTag getTile(int x, int y, int z) {
        if (tiles == null || tiles.isEmpty()) {
            return null;
        }
        short pair = MathMan.tripleBlockCoord(x, y, z);
        return tiles.get(pair);
    }

    private final int getIndex(int x, int y, int z) {
        return x | (z << 4) | (y << 8);
    }

    public int getBlockOrdinal(int x, int y, int z) {
        return blocks[x | (z << 4) | (y << 8)];
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        int ordinal = getBlockOrdinal(x, y, z);
        return BlockState.getFromOrdinal(ordinal);
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        BlockState block = getBlock(x, y, z);
        return block.toBaseBlock(this, x, y, z);
    }

    @Override
    public Set<UUID> getEntityRemoves() {
        return new HashSet<>();
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B holder) {
        setBlock(x, y, z, holder.getOrdinalChar());
        holder.applyTileEntity(this, x, y, z);
        return true;
    }

    @Override
    public void setBlocks(int layer, char[] data) {
        int offset = layer << 12;
        System.arraycopy(data, 0, blocks, offset, 4096);
    }

    @Override
    public char[] load(int layer) {
        char[] tmp = FaweCache.INSTANCE.getSectionBitsToChar().get();
        int offset = layer << 12;
        System.arraycopy(blocks, offset, tmp, 0, 4096);
        return tmp;
    }

    public void setBlock(int x, int y, int z, char ordinal) {
        blocks[getIndex(x, y, z)] = ordinal;
    }

    public void setBiome(BiomeType biome) {
        Arrays.fill(this.biomes, biome);
    }

    @Override
    public void removeEntity(UUID uuid) {
        entities.remove(uuid);
    }

    @Override
    public boolean trim(boolean aggressive) {
        return isEmpty();
    }

    @Override
    public CompoundTag getEntity(UUID uuid) {
        return this.entities.get(uuid);
    }

    @Override
    public Future call(IChunkSet set, Runnable finalize) {
        return null;
    }

    @Override
    public void filterBlocks(Filter filter, ChunkFilterBlock block, @Nullable Region region, boolean full) {
        try {
            block.filter(this, this, this, filter, region, full);
        } finally {
            filter.finishChunk(this);
        }
    }
}
