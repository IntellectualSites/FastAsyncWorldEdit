package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.jnbt.streamer.IntValueReader;
import com.boydti.fawe.object.IntegerTrio;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard.ClipboardEntity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class MemoryOptimizedClipboard extends LinearClipboard {

    private static final int BLOCK_SIZE = 1048576 * 2;
    private static final int BLOCK_MASK = 1048575;
    private static final int BLOCK_SHIFT = 20;

    private byte[][] states;

    private byte[] buffer = new byte[MainUtil.getMaxCompressedLength(BLOCK_SIZE)];
    private byte[] biomes = null;

    private final HashMap<IntegerTrio, CompoundTag> nbtMapLoc;
    private final HashMap<Integer, CompoundTag> nbtMapIndex;


    private int lastCombinedIdsI = -1;

    private byte[] lastCombinedIds;

    private boolean saveCombinedIds = false;

    private int compressionLevel;

    public MemoryOptimizedClipboard(BlockVector3 dimensions) {
        this(dimensions, Settings.IMP.CLIPBOARD.COMPRESSION_LEVEL);
    }

    public MemoryOptimizedClipboard(BlockVector3 dimensions, int compressionLevel) {
        super(dimensions);
        states = new byte[1 + (getVolume() >> BLOCK_SHIFT)][];
        nbtMapLoc = new HashMap<>();
        nbtMapIndex = new HashMap<>();
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
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        setBiome(getIndex(x, 0, z), biome);
        return true;
    }

    @Override
    public void setBiome(int index, BiomeType biome) {
        if (biomes == null) {
            biomes = new byte[getArea()];
        }
        biomes[index] = (byte) biome.getInternalId();
    }

    @Override
    public void streamBiomes(IntValueReader task) {
        if (!hasBiomes()) return;
        int index = 0;
        try {
            for (int z = 0; z < getLength(); z++) {
                for (int x = 0; x < getWidth(); x++, index++) {
                    task.applyInt(index, biomes[index] & 0xFF);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public BiomeType getBiome(int index) {
        if (!hasBiomes()) {
            return null;
        }
        return BiomeTypes.get(biomes[index]);
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        return getBiome(getIndex(x, 0, z));
    }

    private CompoundTag getTag(int index) {
        convertTilesToIndex();
        return nbtMapIndex.get(index);
    }

    public int getOrdinal(int index) {
        int i = index >> BLOCK_SHIFT;
        int li = (index & BLOCK_MASK) << 1;
        if (i != lastCombinedIdsI) {
            saveCombinedIds();
            byte[] compressed = states[lastCombinedIdsI = i];
            if (compressed != null) {
                lastCombinedIds = MainUtil.decompress(compressed, lastCombinedIds, BLOCK_SIZE, compressionLevel);
            } else {
                lastCombinedIds = null;
                return 0;
            }
        }
        if (lastCombinedIds == null) {
            return 0;
        }
        int ordinal = ((lastCombinedIds[li] << 8) + lastCombinedIds[li + 1]);
        return ordinal;
    }

    private void saveCombinedIds() {
        if (saveCombinedIds && lastCombinedIds != null) {
            states[lastCombinedIdsI] = MainUtil.compress(lastCombinedIds, buffer, compressionLevel);
        }
        saveCombinedIds = false;
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
            BlockType bt = BlockTypes.getFromStateId(v);
            if (bt.getMaterial().isAir()) {
                return;
            }
            lastCombinedIds = new byte[BLOCK_SIZE];
        }
        int li = (index & BLOCK_MASK) << 1;
        lastCombinedIds[li] = (byte) ((v >>> 8) & 0xFF);
        lastCombinedIds[li + 1] = (byte) (v & 0xFF);
        saveCombinedIds = true;
    }

    @Override
    public Collection<CompoundTag> getTileEntities() {
        convertTilesToIndex();
        for (Map.Entry<Integer, CompoundTag> entry : nbtMapIndex.entrySet()) {
            int index = entry.getKey();
            CompoundTag tag = entry.getValue();
            Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
            if (!values.containsKey("x")) {
                int y = index / getArea();
                index -= y * getArea();
                int z = index / getWidth();
                int x = index - (z * getWidth());
                values.put("x", new IntTag(x));
                values.put("y", new IntTag(y));
                values.put("z", new IntTag(z));
            }
        }
        return nbtMapIndex.values();
    }

    private int ylast;
    private int ylasti;
    private int zlast;
    private int zlasti;

    public int getIndex(int x, int y, int z) {
        return x + ((ylast == y) ? ylasti : (ylasti = (ylast = y) * getArea())) + ((zlast == z) ? zlasti : (zlasti = (zlast = z) * getWidth()));
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        int index = getIndex(x, y, z);
        return getFullBlock(index);
    }

    @Override
    public BaseBlock getFullBlock(int index) {
        BlockState block = getBlock(index);
        if (block.getMaterial().hasContainer()) {
            CompoundTag nbt = getTag(index);
            if (nbt != null) {
                return block.toBaseBlock(nbt);
            }
        }
        return block.toBaseBlock();
    }

    @Override
    public BlockState getBlock(int index) {
        int ordinal = getOrdinal(index);
        return BlockState.getFromOrdinal(ordinal);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return getBlock(getIndex(x, y, z));
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
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) {
        return setBlock(getIndex(x, y, z), block);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int index, B block) {
        int combinedId = block.getInternalId();
        setCombinedId(index, combinedId);
        boolean hasNbt = block instanceof BaseBlock && block.hasNbtData();
        if (hasNbt) {
            setTile(index, block.getNbtData());
        }
        return true;
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        BlockArrayClipboard.ClipboardEntity ret = new BlockArrayClipboard.ClipboardEntity(location, entity);
        entities.add(ret);
        return ret;
    }

    @Override
    public List<? extends Entity> getEntities() {
        return new ArrayList<>(entities);
    }

    @Override
    public void removeEntity(Entity entity) {
        if (entity instanceof ClipboardEntity) {
            this.entities.remove(entity);
        }
    }

}
