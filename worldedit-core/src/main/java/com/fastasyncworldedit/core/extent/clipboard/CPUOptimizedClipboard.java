package com.fastasyncworldedit.core.extent.clipboard;

import com.fastasyncworldedit.core.jnbt.streamer.IntValueReader;
import com.fastasyncworldedit.core.math.IntTriple;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CPUOptimizedClipboard extends LinearClipboard {

    private BiomeType[] biomes = null;
    private final char[] states;

    private final HashMap<IntTriple, CompoundTag> nbtMapLoc;
    private final HashMap<Integer, CompoundTag> nbtMapIndex;


    public CPUOptimizedClipboard(Region region) {
        super(region.getDimensions(), region.getMinimumPoint());
        this.states = new char[getVolume()];
        nbtMapLoc = new HashMap<>();
        nbtMapIndex = new HashMap<>();
    }

    @Override
    public boolean hasBiomes() {
        return biomes != null;
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        return setBiome(position.x(), position.y(), position.z(), biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        setBiome(getBiomeIndex(x, y, z), biome);
        return true;
    }

    @Override
    public void setBiome(int index, BiomeType biome) {
        if (biomes == null) {
            biomes = new BiomeType[((getHeight() >> 2) + 1) * ((getLength() >> 2) + 1) * ((getWidth() >> 2) + 1)];
        }
        biomes[index] = biome;
    }

    @Override
    public void streamBiomes(IntValueReader task) {
        if (!hasBiomes()) {
            return;
        }
        try {
            for (int y = 0; y < getHeight(); y++) {
                for (int z = 0; z < getLength(); z++) {
                    for (int x = 0; x < getWidth(); x++) {
                        task.applyInt(getIndex(x, y, z), biomes[getBiomeIndex(x, y, z)].getInternalId());
                    }
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
        return getBiome(getBiomeIndex(x, y, z));
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        return getBiome(getBiomeIndex(position.x(), position.y(), position.z()));
    }

    public void convertTilesToIndex() {
        if (nbtMapLoc.isEmpty()) {
            return;
        }
        for (Map.Entry<IntTriple, CompoundTag> entry : nbtMapLoc.entrySet()) {
            IntTriple key = entry.getKey();
            setTile(getIndex(key.x(), key.y(), key.z()), entry.getValue());
        }
        nbtMapLoc.clear();
    }

    private CompoundTag getTag(int index) {
        convertTilesToIndex();
        return nbtMapIndex.get(index);
    }

    public int getBiomeIndex(int x, int y, int z) {
        return (x >> 2) + (y >> 2) * (getWidth() >> 2) * (getLength() >> 2) + (z >> 2) * (getWidth() >> 2);
    }

    public int getIndex(int x, int y, int z) {
        return x + y * getArea() + z * getWidth();
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
        nbtMapIndex.replaceAll((index, tag) -> {
            Map<String, Tag<?, ?>> values = new HashMap<>(tag.getValue());
            if (!values.containsKey("x")) {
                int y = index / getArea();
                index -= y * getArea();
                int z = index / getWidth();
                int x = index - (z * getWidth());
                values.put("x", new IntTag(x));
                values.put("y", new IntTag(y));
                values.put("z", new IntTag(z));
                return new CompoundTag(values);
            } else {
                return tag;
            }
        });
        return nbtMapIndex.values();
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        nbtMapLoc.put(new IntTriple(x, y, z), new CompoundTag(tag.getValue()));
        return true;
    }

    @Override
    public boolean tile(final int x, final int y, final int z, final FaweCompoundTag tile) throws WorldEditException {
        // TODO replace
        return setTile(x, y, z, new CompoundTag(tile.linTag()));
    }

    private boolean setTile(int index, CompoundTag tag) {
        final Map<String, Tag<?, ?>> values = new HashMap<>(tag.getValue());
        values.remove("x");
        values.remove("y");
        values.remove("z");
        nbtMapIndex.put(index, new CompoundTag(values));
        return true;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) {
        return setBlock(getIndex(x, y, z), block);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int index, B block) {
        char ordinal = block.getOrdinalChar();
        if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
            ordinal = BlockTypesCache.ReservedIDs.AIR;
        }
        states[index] = ordinal;
        boolean hasNbt = block instanceof BaseBlock && block.hasNbtData();
        if (hasNbt) {
            setTile(index, block.getNbtData());
        }
        return true;
    }

}
