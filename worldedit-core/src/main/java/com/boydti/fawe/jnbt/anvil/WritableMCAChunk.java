package com.boydti.fawe.jnbt.anvil;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.util.MathMan;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockID;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WritableMCAChunk extends FaweChunk<Void> {
    public final boolean[] hasSections = new boolean[16];
    public final byte[] skyLight = new byte[65536];
    public final byte[] blockLight = new byte[65536];

    public boolean hasBiomes = false;
    public final int[] biomes = new int[256];

    public final int[] blocks = new int[65536];

    public Map<Short, CompoundTag> tiles = new HashMap<>();
    public Map<UUID, CompoundTag> entities = new HashMap<>();
    public long inhabitedTime = System.currentTimeMillis();
    public long lastUpdate;

    public int modified;
    public boolean deleted;

    public int chunkX, chunkZ;

    protected WritableMCAChunk() {
        super(null, 0, 0);
    }

    public int getX() {
        return chunkX;
    }

    public int getZ() {
        return chunkZ;
    }

    public void setLoc(int X, int Z) {
        this.chunkX = X;
        this.chunkZ = Z;
    }

    public void clear(int X, int Z) {
        this.chunkX = X;
        this.chunkZ = Z;
        if (!tiles.isEmpty()) {
            tiles.clear();
        }
        if (!entities.isEmpty()) {
            entities.clear();
        }
        modified = 0;
        deleted = false;
        hasBiomes = false;
        // TODO optimize
        for (int i = 0; i < 65536; i++) {
            blocks[i] = BlockID.AIR;
        }
        Arrays.fill(hasSections, false);
    }

    public void write(NBTOutputStream nbtOut) throws IOException {
        int[] blockToPalette = FaweCache.BLOCK_TO_PALETTE.get();
        int[] paletteToBlock = FaweCache.PALETTE_TO_BLOCK.get();
        long[] blockstates = FaweCache.BLOCK_STATES.get();
        int[] blocksCopy = FaweCache.SECTION_BLOCKS.get();

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
            out.writeNamedTag("Biomes", biomes);
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
                        int stateId = blocks[i];
                        int ordinal = BlockState.getFromInternalId(stateId).getOrdinal(); // TODO fixme Remove all use of BlockTypes.BIT_OFFSET so that this conversion isn't necessary
                        int palette = blockToPalette[ordinal];
                        if (palette == Integer.MAX_VALUE) {
                            BlockState state = BlockTypes.states[ordinal];
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
                        BlockState state = BlockTypes.states[ordinal];
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


                    out.writeNamedTagName("BlockLight", NBTConstants.TYPE_BYTE_ARRAY);
                    out.writeInt(2048);
                    out.write(blockLight, layer << 11, 1 << 11);

                    out.writeNamedTagName("SkyLight", NBTConstants.TYPE_BYTE_ARRAY);
                    out.writeInt(2048);
                    out.write(skyLight, layer << 11, 1 << 11);


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

    public byte[] toBytes(byte[] buffer) throws IOException {
        if (buffer == null) {
            buffer = new byte[8192];
        }
        FastByteArrayOutputStream buffered = new FastByteArrayOutputStream(buffer);
        DataOutputStream dataOut = new DataOutputStream(buffered);
        try (NBTOutputStream nbtOut = new NBTOutputStream((DataOutput) dataOut)) {
            write(nbtOut);
        }
        return buffered.toByteArray();
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

    public boolean isModified() {
        return modified != 0;
    }

    public int getModified() {
        return modified;
    }

    public final void setModified() {
        this.modified++;
    }

    public int getBitMask() {
        int bitMask = 0;
        for (int section = 0; section < hasSections.length; section++) {
            if (hasSections[section]) {
                bitMask += 1 << section;
            }
        }
        return bitMask;
    }

    public void setTile(int x, int y, int z, CompoundTag tile) {
        setModified();
        short pair = MathMan.tripleBlockCoord(x, y, z);
        if (tile != null) {
            tiles.put(pair, tile);
        } else {
            tiles.remove(pair);
        }
    }

    public void setEntity(CompoundTag entityTag) {
        setModified();
        long least = entityTag.getLong("UUIDLeast");
        long most = entityTag.getLong("UUIDMost");
        entities.put(new UUID(most, least), entityTag);
    }

    public void setBiome(int x, int z, BiomeType biome) {
        setModified();
        biomes[x + (z << 4)] = biome.getInternalId();
    }

    public Set<CompoundTag> getEntities() {
        return new HashSet<>(entities.values());
    }

    public Map<Short, CompoundTag> getTiles() {
        return tiles == null ? new HashMap<>() : tiles;
    }

    public CompoundTag getTile(int x, int y, int z) {
        if (tiles == null || tiles.isEmpty()) {
            return null;
        }
        short pair = MathMan.tripleBlockCoord(x, y, z);
        return tiles.get(pair);
    }

    public boolean doesSectionExist(int cy) {
        return hasSections[cy];
    }

    private final int getIndex(int x, int y, int z) {
        return x | (z << 4) | (y << 8);
    }

    public int getBlockCombinedId(int x, int y, int z) {
        return blocks[x | (z << 4) | (y << 8)];
    }

    public BiomeType[] getBiomeArray() {
        return null;
    }

    public Set<UUID> getEntityRemoves() {
        return new HashSet<>();
    }

    public void setSkyLight(int x, int y, int z, int value) {
        setNibble(getIndex(x, y, z), skyLight, value);
    }

    public void setBlockLight(int x, int y, int z, int value) {
        setNibble(getIndex(x, y, z), blockLight, value);
    }

    public int getSkyLight(int x, int y, int z) {
        if (!hasSections[y >> 4]) {
            return 0;
        }
        return getNibble(getIndex(x, y, z), skyLight);
    }

    public int getBlockLight(int x, int y, int z) {
        if (!hasSections[y >> 4]) {
            return 0;
        }
        return getNibble(getIndex(x, y, z), blockLight);
    }

    public void setFullbright() {
        for (int layer = 0; layer < 16; layer++) {
            if (hasSections[layer]) {
                Arrays.fill(skyLight, layer << 7, ((layer + 1) << 7), (byte) 255);
            }
        }
    }

    public void removeLight() {
        for (int i = 0; i < 16; i++) {
            removeLight(i);
        }
    }

    public void removeLight(int i) {
        if (hasSections[i]) {
            Arrays.fill(skyLight, i << 7, ((i + 1) << 7), (byte) 0);
            Arrays.fill(blockLight, i << 7, ((i + 1) << 7), (byte) 0);
        }
    }

    public int getNibble(int index, byte[] array) {
        int indexShift = index >> 1;
        if ((index & 1) == 0) {
            return array[indexShift] & 15;
        } else {
            return array[indexShift] >> 4 & 15;
        }
    }

    public void setNibble(int index, byte[] array, int value) {
        int indexShift = index >> 1;
        byte existing = array[indexShift];
        int valueShift = value << 4;
        if (existing == value + valueShift) {
            return;
        }
        if ((index & 1) == 0) {
            array[indexShift] = (byte) (existing & 240 | value);
        } else {
            array[indexShift] = (byte) (existing & 15 | valueShift);
        }
    }

    public void setBlock(int x, int y, int z, int combinedId) {
        blocks[getIndex(x, y, z)] = combinedId;
    }

    public void setBiome(BiomeType biome) {
        Arrays.fill(biomes, (byte) biome.getInternalId());
    }

    @Override
    public FaweChunk<Void> copy(boolean shallow) {
        throw new UnsupportedOperationException("Unsupported");
    }

    public void removeEntity(UUID uuid) {
        entities.remove(uuid);
    }

    public Void getChunk() {
        throw new UnsupportedOperationException("Not applicable for this");
    }

    public FaweChunk call() {
        throw new UnsupportedOperationException("Not supported");
    }
}
