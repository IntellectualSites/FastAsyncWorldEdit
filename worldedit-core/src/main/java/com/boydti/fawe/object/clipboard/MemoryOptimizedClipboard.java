package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.object.IntegerTrio;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.jpountz.util.SafeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MemoryOptimizedClipboard extends FaweClipboard {

    public static final int BLOCK_SIZE = 1048576 * 4;
    public static final int BLOCK_MASK = 1048575;
    public static final int BLOCK_SHIFT = 20;

    private int length;
    private int height;
    private int width;
    private int area;
    private int volume;

    private byte[][] states;

    private byte[] buffer = new byte[MainUtil.getMaxCompressedLength(BLOCK_SIZE)];
    private byte[] biomes = null;

    private final HashMap<IntegerTrio, CompoundTag> nbtMapLoc;
    private final HashMap<Integer, CompoundTag> nbtMapIndex;

    private final HashSet<ClipboardEntity> entities;

    private int lastCombinedIdsI = -1;

    private byte[] lastCombinedIds;

    private boolean saveCombinedIds = false;

    private int compressionLevel;

    public MemoryOptimizedClipboard(int width, int height, int length) {
        this(width, height, length, Settings.IMP.CLIPBOARD.COMPRESSION_LEVEL);
    }

    public MemoryOptimizedClipboard(int width, int height, int length, int compressionLevel) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.area = width * length;
        this.volume = area * height;
        states = new byte[1 + (volume >> BLOCK_SHIFT)][];
        nbtMapLoc = new HashMap<>();
        nbtMapIndex = new HashMap<>();
        entities = new HashSet<>();
        this.compressionLevel = compressionLevel;
    }

    public void convertTilesToIndex() {
        if (nbtMapLoc.isEmpty()) {
            return;
        }
        for (Map.Entry<IntegerTrio, CompoundTag> entry : nbtMapLoc.entrySet()) {
            IntegerTrio key = entry.getKey();
            setTile(getIndex(key.x, key.y, key.z), entry.getValue());
        }
        nbtMapLoc.clear();
    }

    @Override
    public boolean hasBiomes() {
        return biomes != null;
    }

    @Override
    public boolean setBiome(int x, int z, int biome) {
        setBiome(getIndex(x, 0, z), biome);
        return true;
    }

    @Override
    public void setBiome(int index, int biome) {
        if (biomes == null) {
            biomes = new byte[area];
        }
        biomes[index] = (byte) biome;
    }

    @Override
    public void streamBiomes(NBTStreamer.ByteReader task) {
        if (!hasBiomes()) return;
        int index = 0;
        for (int z = 0; z < length; z++) {
            for (int x = 0; x < width; x++, index++) {
                task.run(index, biomes[index] & 0xFF);
            }
        }
    }

    @Override
    public BaseBiome getBiome(int index) {
        if (!hasBiomes()) {
            return EditSession.nullBiome;
        }
        return FaweCache.CACHE_BIOME[biomes[index] & 0xFF];
    }

    @Override
    public BaseBiome getBiome(int x, int z) {
        return getBiome(getIndex(x, 0, z));
    }

    private CompoundTag getTag(int index) {
        convertTilesToIndex();
        return nbtMapIndex.get(index);
    }

    public int getCombinedId(int index) {
        int i = index >> BLOCK_SHIFT;
        if (i == lastCombinedIdsI) {
            if (lastCombinedIds == null) {
                return 0;
            }
            return lastCombinedIds[index & BLOCK_MASK] & 0xFF;
        }
        saveCombinedIds();
        byte[] compressed = states[lastCombinedIdsI = i];
        if (compressed == null) {
            lastCombinedIds = null;
            return BlockTypes.AIR.getInternalId();
        }
        lastCombinedIds = MainUtil.decompress(compressed, lastCombinedIds, BLOCK_SIZE, compressionLevel);
        return SafeUtils.readIntBE(lastCombinedIds, index & BLOCK_MASK);
    }

    private void saveCombinedIds() {
        if (saveCombinedIds && lastCombinedIds != null) {
            states[lastCombinedIdsI] = MainUtil.compress(lastCombinedIds, buffer, compressionLevel);
        }
        saveCombinedIds = false;
    }

    @Override
    public void setDimensions(Vector dimensions) {
        width = dimensions.getBlockX();
        height = dimensions.getBlockY();
        length = dimensions.getBlockZ();
        area = width * length;
        int newVolume = area * height;
        if (newVolume != volume) {
            volume = newVolume;
            states = new byte[1 + (volume >> BLOCK_SHIFT)][];
            lastCombinedIdsI = -1;
            saveCombinedIds = false;
        }
    }

    @Override
    public Vector getDimensions() {
        return new Vector(width, height, length);
    }

    private int lastI;
    private int lastIMin;
    private int lastIMax;

    public int getLocalIndex(int index) {
        if (index < lastIMin || index > lastIMax) {
            lastI = index >> BLOCK_SHIFT;
            lastIMin = lastI << BLOCK_SHIFT;
            lastIMax = lastIMin + BLOCK_MASK;
        }
        return lastI;
    }

    public void setCombinedId(int index, int v) {
        int i = getLocalIndex(index);
        if (i != lastCombinedIdsI) {
            saveCombinedIds();
            byte[] compressed = states[lastCombinedIdsI = i];
            if (compressed != null) {
                lastCombinedIds = MainUtil.decompress(compressed, lastCombinedIds, BLOCK_SIZE, compressionLevel);
            } else {
                lastCombinedIds = null;
            }
        }
        if (lastCombinedIds == null) {
            switch (BlockTypes.getFromStateId(v)) {
                case AIR:
                case CAVE_AIR:
                case VOID_AIR:
                    return;
            }
            lastCombinedIds = new byte[BLOCK_SIZE];
        }
        int li = (index & BLOCK_MASK) << 2;
        lastCombinedIds[li] = (byte) ((v >>> 24) & 0xFF);
        lastCombinedIds[li + 1] = (byte) ((v >>> 16) & 0xFF);
        lastCombinedIds[li + 2] = (byte) ((v >>>  8) & 0xFF);
        lastCombinedIds[li + 3] = (byte) ((v >>>  0) & 0xFF);
        saveCombinedIds = true;
    }

    @Override
    public void streamCombinedIds(NBTStreamer.ByteReader task) {
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int id = getCombinedId(index);
                    task.run(index++, id);
                }
            }
        }
    }

    @Override
    public List<CompoundTag> getTileEntities() {
        convertTilesToIndex();
        for (Map.Entry<Integer, CompoundTag> entry : nbtMapIndex.entrySet()) {
            int index = entry.getKey();
            CompoundTag tag = entry.getValue();
            Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
            if (!values.containsKey("x")) {
                int y = index / area;
                index -= y * area;
                int z = index / width;
                int x = index - (z * width);
                values.put("x", new IntTag(x));
                values.put("y", new IntTag(y));
                values.put("z", new IntTag(z));
            }
        }
        return new ArrayList<>(nbtMapIndex.values());
    }

    private int ylast;
    private int ylasti;
    private int zlast;
    private int zlasti;

    public int getIndex(int x, int y, int z) {
        return x + ((ylast == y) ? ylasti : (ylasti = (ylast = y) * area)) + ((zlast == z) ? zlasti : (zlasti = (zlast = z) * width));
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        int index = getIndex(x, y, z);
        return getBlock(index);
    }

    @Override
    public BlockState getBlock(int index) {
        int combinedId = getCombinedId(index);
        BlockTypes type = BlockTypes.getFromStateId(combinedId);
        BlockState state = type.withStateId(combinedId);
        if (type.getMaterial().hasContainer()) {
            CompoundTag nbt = getTag(index);
            if (nbt != null) {
                return new BaseBlock(state, nbt);
            }
        }
        return state;
    }

    @Override
    public void forEach(final BlockReader task, final boolean air) {
        if (air) {
            for (int y = 0, index = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++, index++) {
                        BlockState block = getBlock(index);
                        task.run(x, y, z, block);
                    }
                }
            }
        } else {
            for (int y = 0, index = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++, index++) {
                        BlockState block = getBlock(index);
                        switch (block.getBlockType()) {
                            case AIR:
                            case CAVE_AIR:
                            case VOID_AIR:
                                continue;
                            default:
                                task.run(x, y, z, block);
                        }
                    }
                }
            }
        }
    }

    public int size() {
        saveCombinedIds();
        int total = 0;
        for (byte[] array : states) {
            if (array != null) {
                total += array.length;
            }
        }
        return total;
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        nbtMapLoc.put(new IntegerTrio(x, y, z), tag);
        return true;
    }

    public boolean setTile(int index, CompoundTag tag) {
        nbtMapIndex.put(index, tag);
        Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
        values.remove("x");
        values.remove("y");
        values.remove("z");
        return true;
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) {
        return setBlock(getIndex(x, y, z), block);
    }

    @Override
    public boolean setBlock(int index, BlockStateHolder block) {
        int combinedId = block.getInternalId();
        setCombinedId(index, combinedId);
        CompoundTag tile = block.getNbtData();
        if (tile != null) {
            setTile(index, tile);
        }
        return true;
    }

    @Override
    public Entity createEntity(Extent world, double x, double y, double z, float yaw, float pitch, BaseEntity entity) {
        FaweClipboard.ClipboardEntity ret = new ClipboardEntity(world, x, y, z, yaw, pitch, entity);
        entities.add(ret);
        return ret;
    }

    @Override
    public List<? extends Entity> getEntities() {
        return new ArrayList<>(entities);
    }

    @Override
    public boolean remove(ClipboardEntity clipboardEntity) {
        return entities.remove(clipboardEntity);
    }
}
