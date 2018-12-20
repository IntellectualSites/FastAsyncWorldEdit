package com.boydti.fawe.jnbt.anvil;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.object.number.MutableLong;
import com.boydti.fawe.util.ArrayUtil;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.*;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;

public class MCAChunk extends FaweChunk<Void> {

//    ids: byte[16][4096]
//    data: byte[16][2048]
//    skylight: byte[16][2048]
//    blocklight: byte[16][2048]
//    entities: Map<Short, CompoundTag>
//    tiles: List<CompoundTag>
//    biomes: byte[256]
//    compressedSize: int
//    modified: boolean
//    deleted: boolean

    public byte[][] ids;
    public byte[][] data;
    public byte[][] skyLight;
    public byte[][] blockLight;
    public byte[] biomes;
    public Map<Short, CompoundTag> tiles = new HashMap<>();
    public Map<UUID, CompoundTag> entities = new HashMap<>();
    private long inhabitedTime;
    private long lastUpdate;
    private int[] heightMap;

    private int modified;
    private boolean deleted;

    public MCAChunk(FaweQueue queue, int x, int z) {
        super(queue, x, z);
        this.ids = new byte[16][];
        this.data = new byte[16][];
        this.skyLight = new byte[16][];
        this.blockLight = new byte[16][];
        this.biomes = new byte[256];
        this.tiles = new HashMap<>();
        this.entities = new HashMap<>();
        this.lastUpdate = System.currentTimeMillis();
        this.heightMap = new int[256];
        this.setModified();
    }

    public MCAChunk(MCAChunk parent, boolean shallow) {
        super(parent.getParent(), parent.getX(), parent.getZ());
        if (shallow) {
            this.ids = parent.ids;
            this.data = parent.data;
            this.skyLight = parent.skyLight;
            this.blockLight = parent.blockLight;
            this.biomes = parent.biomes;
            this.tiles = parent.tiles;
            this.entities = parent.entities;
            this.inhabitedTime = parent.inhabitedTime;
            this.lastUpdate = parent.lastUpdate;
            this.heightMap = parent.heightMap;
            this.modified = parent.modified;
            this.deleted = parent.deleted;
        } else {
            this.ids = (byte[][]) MainUtil.copyNd(parent.ids);
            this.data = (byte[][]) MainUtil.copyNd(parent.data);
            this.skyLight = (byte[][]) MainUtil.copyNd(parent.skyLight);
            this.blockLight = (byte[][]) MainUtil.copyNd(parent.blockLight);
            this.biomes = parent.biomes.clone();
            this.tiles = new HashMap<>(parent.tiles);
            this.entities = new HashMap<>(parent.entities);
            this.inhabitedTime = parent.inhabitedTime;
            this.lastUpdate = parent.lastUpdate;
            this.heightMap = parent.heightMap.clone();
            this.modified = parent.modified;
            this.deleted = parent.deleted;
        }
    }

    public void write(NBTOutputStream nbtOut) throws IOException {
        nbtOut.writeNamedTagName("", NBTConstants.TYPE_COMPOUND);
        nbtOut.writeLazyCompoundTag("Level", new NBTOutputStream.LazyWrite() {
            @Override
            public void write(NBTOutputStream out) throws IOException {
                out.writeNamedTag("V", (byte) 1);
                out.writeNamedTag("xPos", getX());
                out.writeNamedTag("zPos", getZ());
                out.writeNamedTag("LightPopulated", (byte) 0);
                out.writeNamedTag("TerrainPopulated", (byte) 1);
                if (entities.isEmpty()) {
                    out.writeNamedEmptyList("Entities");
                } else {
                    out.writeNamedTag("Entities", new ListTag(CompoundTag.class, new ArrayList<CompoundTag>(entities.values())));
                }
                if (tiles.isEmpty()) {
                    out.writeNamedEmptyList("TileEntities");
                } else {
                    out.writeNamedTag("TileEntities", new ListTag(CompoundTag.class, new ArrayList<CompoundTag>(tiles.values())));
                }
                out.writeNamedTag("InhabitedTime", inhabitedTime);
                out.writeNamedTag("LastUpdate", lastUpdate);
                if (biomes != null) {
                    out.writeNamedTag("Biomes", biomes);
                }
                out.writeNamedTag("HeightMap", heightMap);
                out.writeNamedTagName("Sections", NBTConstants.TYPE_LIST);
                nbtOut.getOutputStream().writeByte(NBTConstants.TYPE_COMPOUND);
                int len = 0;
                for (int layer = 0; layer < ids.length; layer++) {
                    if (ids[layer] != null) len++;
                }
                nbtOut.getOutputStream().writeInt(len);
                for (int layer = 0; layer < ids.length; layer++) {
                    byte[] idLayer = ids[layer];
                    if (idLayer == null) {
                        continue;
                    }
                    out.writeNamedTag("Y", (byte) layer);
                    out.writeNamedTag("BlockLight", blockLight[layer]);
                    out.writeNamedTag("SkyLight", skyLight[layer]);
                    out.writeNamedTag("Blocks", idLayer);
                    out.writeNamedTag("Data", data[layer]);
                    out.writeEndTag();
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

    public void copyFrom(MCAChunk other, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, int offsetX, int offsetY, int offsetZ) {
        minY = Math.max(-offsetY - minY, minY);
        maxY = Math.min(255 - offsetY, maxY);
        minZ = Math.max(-offsetZ - minZ, minZ);
        maxZ = Math.min(15 - offsetZ, maxZ);
        minX = Math.max(-offsetX - minX, minX);
        maxX = Math.min(15 - offsetX, maxX);
        if (minX > maxX || minZ > maxZ || minY > maxY) return;
        int startLayer = minY >> 4;
        int endLayer = maxY >> 4;
        for (int otherY = minY, thisY = minY + offsetY; otherY <= maxY; otherY++, thisY++) {
            int thisLayer = thisY >> 4;
            int otherLayer = otherY >> 4;
            byte[] thisIds = ids[thisLayer];
            byte[] otherIds = other.ids[otherLayer];
            if (otherIds == null) {
                if (thisIds != null) {
                    int indexY = (thisY & 15) << 8;
                    byte[] thisData = data[thisLayer];
                    byte[] thisSkyLight = skyLight[thisLayer];
                    byte[] thisBlockLight = blockLight[thisLayer];
                    for (int otherZ = minZ, thisZ = minZ + offsetZ; otherZ <= maxZ; otherZ++, thisZ++) {
                        int startIndex = indexY + (thisZ << 4) + minX + offsetX;
                        int endIndex = startIndex + maxX - minX;
                        ArrayUtil.fill(thisIds, startIndex, endIndex + 1, (byte) 0);
                        int startIndexShift = startIndex >> 1;
                        int endIndexShift = endIndex >> 1;
                        if ((startIndex & 1) != 0) {
                            startIndexShift++;
                            setNibble(startIndex, thisData, (byte) 0);
                            setNibble(startIndex, thisSkyLight, (byte) 0);
                            setNibble(startIndex, thisBlockLight, (byte) 0);
                        }
                        if ((endIndex & 1) != 1) {
                            endIndexShift--;
                            setNibble(endIndex, thisData, (byte) 0);
                            setNibble(endIndex, thisSkyLight, (byte) 0);
                            setNibble(endIndex, thisBlockLight, (byte) 0);
                        }
                        ArrayUtil.fill(thisData, startIndexShift, endIndexShift + 1, (byte) 0);
                        ArrayUtil.fill(thisSkyLight, startIndexShift, endIndexShift + 1, (byte) 0);
                        ArrayUtil.fill(thisBlockLight, startIndexShift, endIndexShift + 1, (byte) 0);
                    }
                }
                continue;
            } else if (thisIds == null) {
                ids[thisLayer] = thisIds = new byte[4096];
                data[thisLayer] = new byte[2048];
                skyLight[thisLayer] = new byte[2048];
                blockLight[thisLayer] = new byte[2048];
            }
            int indexY = (thisY & 15) << 8;
            int otherIndexY = (otherY & 15) << 8;
            byte[] thisData = data[thisLayer];
            byte[] thisSkyLight = skyLight[thisLayer];
            byte[] thisBlockLight = blockLight[thisLayer];
            byte[] otherData = other.data[otherLayer];
            byte[] otherSkyLight = other.skyLight[otherLayer];
            byte[] otherBlockLight = other.blockLight[otherLayer];
            for (int otherZ = minZ, thisZ = minZ + offsetZ; otherZ <= maxZ; otherZ++, thisZ++) {
                int startIndex = indexY + (thisZ << 4) + minX + offsetX;
                int endIndex = startIndex + maxX - minX;
                int otherStartIndex = otherIndexY + (otherZ << 4) + minX;
                int otherEndIndex = otherStartIndex + maxX - minX;
                System.arraycopy(otherIds, otherStartIndex, thisIds, startIndex, endIndex - startIndex + 1);
                if ((startIndex & 1) == (otherStartIndex & 1)) {
                    int startIndexShift = startIndex >> 1;
                    int endIndexShift = endIndex >> 1;
                    int otherStartIndexShift = otherStartIndex >> 1;
                    int otherEndIndexShift = otherEndIndex >> 1;
                    if ((startIndex & 1) != 0) {
                        startIndexShift++;
                        otherStartIndexShift++;
                        setNibble(startIndex, thisData, getNibble(otherStartIndex, otherData));
                        setNibble(startIndex, thisSkyLight, getNibble(otherStartIndex, otherSkyLight));
                        setNibble(startIndex, thisBlockLight, getNibble(otherStartIndex, otherBlockLight));
                    }
                    if ((endIndex & 1) != 1) {
                        endIndexShift--;
                        otherEndIndexShift--;
                        setNibble(endIndex, thisData, getNibble(otherEndIndex, otherData));
                        setNibble(endIndex, thisSkyLight, getNibble(otherEndIndex, otherSkyLight));
                        setNibble(endIndex, thisBlockLight, getNibble(otherEndIndex, otherBlockLight));
                    }
                    System.arraycopy(otherData, otherStartIndexShift, thisData, startIndexShift, endIndexShift - startIndexShift + 1);
                    System.arraycopy(otherSkyLight, otherStartIndexShift, thisSkyLight, startIndexShift, endIndexShift - startIndexShift + 1);
                    System.arraycopy(otherBlockLight, otherStartIndexShift, thisBlockLight, startIndexShift, endIndexShift - startIndexShift + 1);
                } else {
                    for (int thisIndex = startIndex, otherIndex = otherStartIndex; thisIndex <= endIndex; thisIndex++, otherIndex++) {
                        setNibble(thisIndex, thisData, getNibble(otherIndex, otherData));
                        setNibble(thisIndex, thisSkyLight, getNibble(otherIndex, otherSkyLight));
                        setNibble(thisIndex, thisBlockLight, getNibble(otherIndex, otherBlockLight));
                    }
                }
            }
        }
        if (!other.tiles.isEmpty()) {
            for (Map.Entry<Short, CompoundTag> entry : other.tiles.entrySet()) {
                int key = entry.getKey();
                int x = MathMan.untripleBlockCoordX(key);
                int y = MathMan.untripleBlockCoordY(key);
                int z = MathMan.untripleBlockCoordZ(key);
                if (x < minX || x > maxX) continue;
                if (z < minZ || z > maxZ) continue;
                if (y < minY || y > maxY) continue;
                x += offsetX;
                y += offsetY;
                z += offsetZ;
                short pair = MathMan.tripleBlockCoord(x, y, z);
                CompoundTag tag = entry.getValue();
                Map<String, Tag> map = ReflectionUtils.getMap(tag.getValue());
                map.put("x", new IntTag((x & 15) + (getX() << 4)));
                map.put("y", new IntTag(y));
                map.put("z", new IntTag((z & 15) + (getZ() << 4)));
                tiles.put(pair, tag);
            }
        }
    }

    public void copyFrom(MCAChunk other, int minY, int maxY, int offsetY) {
        minY = Math.max(-offsetY - minY, minY);
        maxY = Math.min(255 - offsetY, maxY);
        if (minY > maxY) return;
        if ((offsetY & 15) == 0) {
            int offsetLayer = offsetY >> 4;
            int startLayer = minY >> 4;
            int endLayer = maxY >> 4;
            for (int thisLayer = startLayer + offsetLayer, otherLayer = startLayer; thisLayer <= endLayer; thisLayer++, otherLayer++) {
                byte[] otherIds = other.ids[otherLayer];
                byte[] currentIds = ids[thisLayer];
                int by = otherLayer << 4;
                int ty = by + 15;
                if (by >= minY && ty <= maxY) {
                    if (otherIds != null) {
                        ids[thisLayer] = otherIds;
                        data[thisLayer] = other.data[otherLayer];
                        skyLight[thisLayer] = other.skyLight[otherLayer];
                        blockLight[thisLayer] = other.blockLight[otherLayer];
                    } else {
                        ids[thisLayer] = null;
                    }
                } else {
                    by = Math.max(by, minY) & 15;
                    ty = Math.min(ty, maxY) & 15;
                    int indexStart = by << 8;
                    int indexEnd = 256 + (ty << 8);
                    int indexStartShift = indexStart >> 1;
                    int indexEndShift = indexEnd >> 1;
                    if (otherIds == null) {
                        if (currentIds != null) {
                            ArrayUtil.fill(currentIds, indexStart, indexEnd, (byte) 0);
                            ArrayUtil.fill(data[thisLayer], indexStartShift, indexEndShift, (byte) 0);
                            ArrayUtil.fill(skyLight[thisLayer], indexStartShift, indexEndShift, (byte) 0);
                            ArrayUtil.fill(blockLight[thisLayer], indexStartShift, indexEndShift, (byte) 0);
                        }
                    } else {
                        if (currentIds == null) {
                            currentIds = this.ids[thisLayer] = new byte[4096];
                            this.data[thisLayer] = new byte[2048];
                            this.skyLight[thisLayer] = new byte[2048];
                            this.blockLight[thisLayer] = new byte[2048];
                        }
                        System.arraycopy(other.ids[otherLayer], indexStart, currentIds, indexStart, indexEnd - indexStart);
                        System.arraycopy(other.data[otherLayer], indexStartShift, data[thisLayer], indexStartShift, indexEndShift - indexStartShift);
                        System.arraycopy(other.skyLight[otherLayer], indexStartShift, skyLight[thisLayer], indexStartShift, indexEndShift - indexStartShift);
                        System.arraycopy(other.blockLight[otherLayer], indexStartShift, blockLight[thisLayer], indexStartShift, indexEndShift - indexStartShift);
                    }
                }
            }
        } else {
            for (int otherY = minY, thisY = minY + offsetY; otherY <= maxY; otherY++, thisY++) {
                int otherLayer = otherY >> 4;
                int thisLayer = thisY >> 4;
                byte[] thisIds = this.ids[thisLayer];
                byte[] otherIds = other.ids[otherLayer];
                int thisStartIndex = (thisY & 15) << 8;
                int thisStartIndexShift = thisStartIndex >> 1;
                if (otherIds == null) {
                    if (thisIds == null) {
                        continue;
                    }
                    ArrayUtil.fill(thisIds, thisStartIndex, thisStartIndex + 256, (byte) 0);
                    ArrayUtil.fill(this.data[thisLayer], thisStartIndexShift, thisStartIndexShift + 128, (byte) 0);
                    ArrayUtil.fill(this.skyLight[thisLayer], thisStartIndexShift, thisStartIndexShift + 128, (byte) 0);
                    ArrayUtil.fill(this.blockLight[thisLayer], thisStartIndexShift, thisStartIndexShift + 128, (byte) 0);
                    continue;
                } else if (thisIds == null) {
                    ids[thisLayer] = thisIds = new byte[4096];
                    data[thisLayer] = new byte[2048];
                    skyLight[thisLayer] = new byte[2048];
                    blockLight[thisLayer] = new byte[2048];
                }
                int otherStartIndex = (otherY & 15) << 8;
                int otherStartIndexShift = otherStartIndex >> 1;
                System.arraycopy(other.ids[otherLayer], otherStartIndex, thisIds, thisStartIndex, 256);
                System.arraycopy(other.data[otherLayer], otherStartIndexShift, data[thisLayer], thisStartIndexShift, 128);
                System.arraycopy(other.skyLight[otherLayer], otherStartIndexShift, skyLight[thisLayer], thisStartIndexShift, 128);
                System.arraycopy(other.blockLight[otherLayer], otherStartIndexShift, blockLight[thisLayer], thisStartIndexShift, 128);
            }
        }
        // Copy nbt
        int thisMinY = minY + offsetY;
        int thisMaxY = maxY + offsetY;
        if (!tiles.isEmpty()) {
            Iterator<Map.Entry<Short, CompoundTag>> iter = tiles.entrySet().iterator();
            while (iter.hasNext()) {
                int y = MathMan.untripleBlockCoordY(iter.next().getKey());
                if (y >= thisMinY && y <= thisMaxY) iter.remove();
            }
        }
        if (!other.tiles.isEmpty()) {
            for (Map.Entry<Short, CompoundTag> entry : other.tiles.entrySet()) {
                int key = entry.getKey();
                int y = MathMan.untripleBlockCoordY(key);
                if (y >= minY && y <= maxY) {
                    tiles.put((short) (key + offsetY), entry.getValue());
                }
            }
        }
        if (!other.entities.isEmpty()) {
            for (Map.Entry<UUID, CompoundTag> entry : other.entities.entrySet()) {
                // TODO
            }
        }
    }

    public int getMinLayer() {
        for (int layer = 0; layer < ids.length; layer++) {
            if (ids[layer] != null) {
                return layer;
            }
        }
        return Integer.MAX_VALUE;
    }

    public int getMaxLayer() {
        for (int layer = ids.length - 1; layer >= 0; layer--) {
            if (ids[layer] != null) {
                return layer;
            }
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Deprecated, use the toBytes method
     *
     * @return
     */
    @Deprecated
    public CompoundTag toTag() {
        if (deleted) {
            return null;
        }
        // e.g. by precalculating the length
        HashMap<String, Object> level = new HashMap<String, Object>();
        level.put("Entities", new ListTag(CompoundTag.class, new ArrayList<CompoundTag>(entities.values())));
        level.put("TileEntities", new ListTag(CompoundTag.class, new ArrayList<CompoundTag>(tiles.values())));
        level.put("InhabitedTime", inhabitedTime);
        level.put("LastUpdate", lastUpdate);
        level.put("LightPopulated", (byte) 0);
        level.put("TerrainPopulated", (byte) 1);
        level.put("V", (byte) 1);
        level.put("xPos", getX());
        level.put("zPos", getZ());
        if (biomes != null) {
            level.put("Biomes", biomes);
        }
        level.put("HeightMap", heightMap);
        ArrayList<HashMap<String, Object>> sections = new ArrayList<>();
        for (int layer = 0; layer < ids.length; layer++) {
            byte[] idLayer = ids[layer];
            if (idLayer == null) {
                continue;
            }
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("Y", (byte) layer);
            map.put("BlockLight", blockLight[layer]);
            map.put("SkyLight", skyLight[layer]);
            map.put("Blocks", idLayer);
            map.put("Data", data[layer]);
            sections.add(map);
        }
        level.put("Sections", sections);
        HashMap<String, Object> root = new HashMap<>();
        root.put("Level", level);
        return FaweCache.asTag(root);
    }

    public MCAChunk(NBTInputStream nis, FaweQueue parent, int x, int z, boolean readPos) throws IOException {
        super(parent, x, z);
        ids = new byte[16][];
        data = new byte[16][];
        skyLight = new byte[16][];
        blockLight = new byte[16][];
        NBTStreamer streamer = new NBTStreamer(nis);
        streamer.addReader(".Level.InhabitedTime", new BiConsumer<Integer, Long>() {
            @Override
            public void accept(Integer index, Long value) {
                inhabitedTime = value;
            }
        });
        streamer.addReader(".Level.LastUpdate", new BiConsumer<Integer, Long>() {
            @Override
            public void accept(Integer index, Long value) {
                lastUpdate = value;
            }
        });
        streamer.addReader(".Level.Sections.#", new BiConsumer<Integer, CompoundTag>() {
            @Override
            public void accept(Integer index, CompoundTag tag) {
                int layer = tag.getByte("Y");
                ids[layer] = tag.getByteArray("Blocks");
                data[layer] = tag.getByteArray("Data");
                skyLight[layer] = tag.getByteArray("SkyLight");
                blockLight[layer] = tag.getByteArray("BlockLight");
            }
        });
        streamer.addReader(".Level.TileEntities.#", new BiConsumer<Integer, CompoundTag>() {
            @Override
            public void accept(Integer index, CompoundTag tile) {
                int x = tile.getInt("x") & 15;
                int y = tile.getInt("y");
                int z = tile.getInt("z") & 15;
                short pair = MathMan.tripleBlockCoord(x, y, z);
                tiles.put(pair, tile);
            }
        });
        streamer.addReader(".Level.Entities.#", new BiConsumer<Integer, CompoundTag>() {
            @Override
            public void accept(Integer index, CompoundTag entityTag) {
                if (entities == null) {
                    entities = new HashMap<UUID, CompoundTag>();
                }
                long least = entityTag.getLong("UUIDLeast");
                long most = entityTag.getLong("UUIDMost");
                entities.put(new UUID(most, least), entityTag);
            }
        });
        streamer.addReader(".Level.Biomes", new BiConsumer<Integer, byte[]>() {
            @Override
            public void accept(Integer index, byte[] value) {
                biomes = value;
            }
        });
        streamer.addReader(".Level.HeightMap", new BiConsumer<Integer, int[]>() {
            @Override
            public void accept(Integer index, int[] value) {
                heightMap = value;
            }
        });
        if (readPos) {
            streamer.addReader(".Level.xPos", new BiConsumer<Integer, Integer>() {
                @Override
                public void accept(Integer index, Integer value) {
                    MCAChunk.this.setLoc(getParent(), value, getZ());
                }
            });
            streamer.addReader(".Level.zPos", new BiConsumer<Integer, Integer>() {
                @Override
                public void accept(Integer index, Integer value) {
                    MCAChunk.this.setLoc(getParent(), getX(), value);
                }
            });
        }
        streamer.readFully();
    }

    public long filterBlocks(MutableMCABackedBaseBlock mutableBlock, MCAFilter filter) {
        MutableLong result = new MutableLong();
        mutableBlock.setChunk(this);
        int bx = getX() << 4;
        int bz = getZ() << 4;
        int tx = bx + 15;
        int tz = bz + 15;
        for (int layer = 0; layer < ids.length; layer++) {
            if (doesSectionExist(layer)) {
                mutableBlock.setArrays(layer);
                int yStart = layer << 4;
                int yEnd = yStart + 15;
                for (int y = yStart, y0 = (yStart & 15); y <= yEnd; y++, y0++) {
                    int yIndex = ((y0) << 8);
                    mutableBlock.setY(y);
                    for (int z = bz, z0 = bz & 15; z <= tz; z++, z0++) {
                        int zIndex = yIndex + ((z0) << 4);
                        mutableBlock.setZ(z);
                        for (int x = bx, x0 = bx & 15; x <= tx; x++, x0++) {
                            int xIndex = zIndex + x0;
                            mutableBlock.setX(x);
                            mutableBlock.setIndex(xIndex);
                            filter.applyBlock(x, y, z, mutableBlock, result);
                        }
                    }
                }
            }
        }
        return result.get();
    }

    public int[] getHeightMapArray() {
        return heightMap;
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

    @Deprecated
    public final void setModified() {
        this.modified++;
    }

    @Override
    public int getBitMask() {
        int bitMask = 0;
        for (int section = 0; section < ids.length; section++) {
            if (ids[section] != null) {
                bitMask += 1 << section;
            }
        }
        return bitMask;
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tile) {
        setModified();
        short pair = MathMan.tripleBlockCoord(x, y, z);
        if (tile != null) {
            tiles.put(pair, tile);
        } else {
            tiles.remove(pair);
        }
    }

    @Override
    public void setEntity(CompoundTag entityTag) {
        setModified();
        long least = entityTag.getLong("UUIDLeast");
        long most = entityTag.getLong("UUIDMost");
        entities.put(new UUID(most, least), entityTag);
    }

    @Override
    public void setBiome(int x, int z, byte biome) {
        setModified();
        biomes[x + (z << 4)] = biome;
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return new HashSet<>(entities.values());
    }

    @Override
    public Map<Short, CompoundTag> getTiles() {
        return tiles == null ? new HashMap<Short, CompoundTag>() : tiles;
    }

    @Override
    public CompoundTag getTile(int x, int y, int z) {
        if (tiles == null || tiles.isEmpty()) {
            return null;
        }
        short pair = MathMan.tripleBlockCoord(x, y, z);
        return tiles.get(pair);
    }

    public boolean doesSectionExist(int cy) {
        return ids[cy] != null;
    }

    @Override
    public FaweChunk<Void> copy(boolean shallow) {
        return new MCAChunk(this, shallow);
    }

    @Override
    public int getBlockCombinedId(int x, int y, int z) {
        // TODO FIXME
        return 0;
//        int layer = y >> 4;
//        byte[] idLayer = ids[layer];
//        if (idLayer == null) {
//            return 0;
//        }
//        int j = FaweCache.CACHE_J[y][z & 15][x & 15];
//        int id = idLayer[j] & 0xFF;
//        if (FaweCache.hasData(id)) {
//            byte[] dataLayer = data[layer];
//            if (dataLayer != null) {
//                return (id << 4) + getNibble(j, dataLayer);
//            }
//        }
//        return id << 4;
    }

    @Override
    public byte[] getBiomeArray() {
        return this.biomes;
    }

    @Override
    public Set<UUID> getEntityRemoves() {
        return new HashSet<>();
    }

    public void setSkyLight(int x, int y, int z, int value) {
        setModified();
        int layer = y >> 4;
        byte[] skyLayer = skyLight[layer];
        if (skyLayer == null) {
            return;
        }
        int index = FaweCache.CACHE_J[y][z & 15][x & 15];
        setNibble(index, skyLayer, value);
    }

    public void setBlockLight(int x, int y, int z, int value) {
        setModified();
        int layer = y >> 4;
        byte[] blockLayer = blockLight[layer];
        if (blockLayer == null) {
            return;
        }
        int index = FaweCache.CACHE_J[y][z & 15][x & 15];
        setNibble(index, blockLayer, value);
    }

    public int getSkyLight(int x, int y, int z) {
        int layer = y >> 4;
        byte[] skyLayer = skyLight[layer];
        if (skyLayer == null) {
            return 0;
        }
        int index = FaweCache.CACHE_J[y][z & 15][x & 15];
        return getNibble(index, skyLayer);
    }

    public int getBlockLight(int x, int y, int z) {
        int layer = y >> 4;
        byte[] blockLayer = blockLight[layer];
        if (blockLayer == null) {
            return 0;
        }
        int index = FaweCache.CACHE_J[y][z & 15][x & 15];
        return getNibble(index, blockLayer);
    }

    public void setFullbright() {
        setModified();
        for (byte[] array : skyLight) {
            if (array != null) {
                Arrays.fill(array, (byte) 255);
            }
        }
    }

    public void removeLight() {
        for (int i = 0; i < skyLight.length; i++) {
            removeLight(i);
        }
    }

    public void removeLight(int i) {
        byte[] array1 = skyLight[i];
        if (array1 == null) {
            return;
        }
        byte[] array2 = blockLight[i];
        Arrays.fill(array1, (byte) 0);
        Arrays.fill(array2, (byte) 0);
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

    public void setIdUnsafe(byte[] idsLayer, int index, byte id) {
        idsLayer[index] = id;
    }

    public void setBlockUnsafe(byte[] idsLayer, byte[] dataLayer, int index, byte id, int data) {
        idsLayer[index] = id;
        setNibble(index, dataLayer, data);
    }

    @Override
    public void setBlock(int x, int y, int z, int combinedId) {
        // TODO FIXME
//        setModified();
//        int layer = y >> 4;
//        byte[] idsLayer = ids[layer];
//        if (idsLayer == null) {
//            idsLayer = this.ids[layer] = new byte[4096];
//            this.data[layer] = new byte[2048];
//            this.skyLight[layer] = new byte[2048];
//            this.blockLight[layer] = new byte[2048];
//        }
//        int j = FaweCache.CACHE_J[y][z & 15][x & 15];
//        idsLayer[j] = (byte) id;
//        byte[] dataLayer = this.data[layer];
//        setNibble(j, dataLayer, data);
    }

    @Override
    public void setBiome(byte biome) {
        Arrays.fill(biomes, biome);
    }

    @Override
    public void removeEntity(UUID uuid) {
        setModified();
        entities.remove(uuid);
    }

    private final boolean idsEqual(byte[] a, byte[] b) {
        // Assumes both are null, or none are (idsEqual - 2d array)
        // Assumes length is 4096
        if (a == b) return true;
        for (char i = 0; i < 4096; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }

    private final boolean idsEqual(byte[][] a, byte[][] b, boolean matchNullToAir) {
        // Assumes length is 16
        for (byte i = 0; i < 16; i++) {
            if ((a[i] == null) != (b[i] == null)) {
                if (matchNullToAir) {
                    if (b[i] != null) {
                        for (byte c : b[i]) {
                            if (c != 0) return false;
                        }
                    } else if (a[i] != null) {
                        for (byte c : a[i]) {
                            if (c != 0) return false;
                        }
                    }
                }
                return false;
            }
        }
        // Check the chunks close to the ground first
        for (byte i = 4; i < 8; i++) {
            if (!idsEqual(a[i], b[i])) return false;
        }
        for (byte i = 3; i >= 0; i--) {
            if (!idsEqual(a[i], b[i])) return false;
        }
        for (byte i = 8; i < 16; i++) {
            if (!idsEqual(a[i], b[i])) return false;
        }
        return true;
    }

    /**
     * Check if the ids match the ids in the other chunk
     * @param other
     * @param matchNullToAir
     * @return
     */
    public boolean idsEqual(MCAChunk other, boolean matchNullToAir) {
        return idsEqual(other.ids, this.ids, matchNullToAir);
    }

    @Override
    public Void getChunk() {
        throw new UnsupportedOperationException("Not applicable for this");
    }

    @Override
    public FaweChunk call() {
        throw new UnsupportedOperationException("Not supported");
    }
}
