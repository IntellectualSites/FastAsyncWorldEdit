package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.jnbt.streamer.IntValueReader;
import com.boydti.fawe.object.IntegerTrio;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CPUOptimizedClipboard extends LinearClipboard {

    private BiomeType[] biomes = null;
    private char[] states;

    private final HashMap<IntegerTrio, CompoundTag> nbtMapLoc;
    private final HashMap<Integer, CompoundTag> nbtMapIndex;


    public CPUOptimizedClipboard(Region region) {
        super(region.getDimensions());
        this.states = new char[getVolume()];
        nbtMapLoc = new HashMap<>();
        nbtMapIndex = new HashMap<>();
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
            biomes = new BiomeType[getArea()];
        }
        biomes[index] = biome;
    }

    @Override
    public void streamBiomes(IntValueReader task) {
        if (!hasBiomes()) return;
        int index = 0;
        try {
            for (int z = 0; z < getLength(); z++) {
                for (int x = 0; x < getWidth(); x++, index++) {
                    task.applyInt(index, biomes[index].getInternalId());
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
        return biomes[index];
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
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

    private int yLast;
    private int yLastI;
    private int zLast;
    private int zLastI;

    public int getIndex(int x, int y, int z) {
        return x + ((yLast == y) ? yLastI : (yLastI = (yLast = y) * getArea())) + ((zLast == z) ? zLastI
            : (zLastI = (zLast = z) * getWidth()));
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
        char ordinal = states[index];
        return BlockState.getFromOrdinal(ordinal);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return getBlock(getIndex(x, y, z));
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
        states[index] = block.getOrdinalChar();
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
        this.entities.remove(entity);
    }

}
