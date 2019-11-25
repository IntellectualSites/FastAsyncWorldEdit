package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.object.IntegerTrio;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class CPUOptimizedClipboard extends FaweClipboard {
    private int length;
    private int height;
    private int width;
    private int area;
    private int volume;

    private BiomeType[] biomes = null;
    private int[] states;

    private final HashMap<IntegerTrio, CompoundTag> nbtMapLoc;
    private final HashMap<Integer, CompoundTag> nbtMapIndex;

    private final HashSet<ClipboardEntity> entities;

    public CPUOptimizedClipboard(int width, int height, int length) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.area = width * length;
        this.volume = area * height;
        this.states = new int[volume];
        nbtMapLoc = new HashMap<>();
        nbtMapIndex = new HashMap<>();
        entities = new HashSet<>();
    }

    @Override
    public boolean hasBiomes() {
        return biomes != null;
    }

    @Override
    public boolean setBiome(int x, int z, BiomeType biome) {
        setBiome(getIndex(x, 0, z), biome);
        return true;
    }

    @Override
    public void setBiome(int index, BiomeType biome) {
        if (biomes == null) {
            biomes = new BiomeType[area];
        }
        biomes[index] = biome;
    }

    @Override
    public void streamBiomes(NBTStreamer.ByteReader task) {
        if (!hasBiomes()) return;
        int index = 0;
        for (int z = 0; z < length; z++) {
            for (int x = 0; x < width; x++, index++) {
                task.run(index, biomes[index].getInternalId());
            }
        }
    }

    @Override
    public BiomeType getBiome(int index) {
        if (!hasBiomes()) {
            return null;
        }
        return biomes[index];
    }

    @Override
    public BiomeType getBiome(int x, int z) {
        return getBiome(getIndex(x, 0, z));
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

    private CompoundTag getTag(int index) {
        convertTilesToIndex();
        return nbtMapIndex.get(index);
    }

    @Override
    public void setDimensions(BlockVector3 dimensions) {
        width = dimensions.getBlockX();
        height = dimensions.getBlockY();
        length = dimensions.getBlockZ();
        area = width * length;
        int newVolume = area * height;
        if (newVolume != volume) {
            volume = newVolume;
            states = new int[volume];
        }
    }

    @Override
    public BlockVector3 getDimensions() {
        return BlockVector3.at(width, height, length);
    }

    private int yLast;
    private int yLastI;
    private int zLast;
    private int zLastI;

    public int getIndex(int x, int y, int z) {
        return x + ((yLast == y) ? yLastI : (yLastI = (yLast = y) * area)) + ((zLast == z) ? zLastI
            : (zLastI = (zLast = z) * width));
    }

    @Override
    public BaseBlock getBlock(int x, int y, int z) {
        int index = getIndex(x, y, z);
        return getBlock(index);
    }

    @Override
    public BaseBlock getBlock(int index) {
        int combinedId = states[index];
        BlockType type = BlockTypes.getFromStateId(combinedId);
        BaseBlock base = type.withStateId(combinedId).toBaseBlock();
        if (type.getMaterial().hasContainer()) {
            CompoundTag nbt = getTag(index);
            if (nbt != null) {
                return base.toBaseBlock(nbt);
            }
        }
        return base;
    }

    @Override
    public void forEach(final BlockReader task, boolean air) {
        if (air) {
            for (int y = 0, index = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++, index++) {
                        BaseBlock block = getBlock(index);
                        task.run(x, y, z, block);
                    }
                }
            }
        } else {
            for (int y = 0, index = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++, index++) {
                        BaseBlock block = getBlock(index);
                        if (!block.getMaterial().isAir()) {
                            task.run(x, y, z, block);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void streamCombinedIds(NBTStreamer.ByteReader task) {
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    task.run(index, states[index++]);
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
        states[index] = block.getInternalId();
        boolean hasNbt = block instanceof BaseBlock && block.hasNbtData();
        if (hasNbt) {
            setTile(index, block.getNbtData());
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
