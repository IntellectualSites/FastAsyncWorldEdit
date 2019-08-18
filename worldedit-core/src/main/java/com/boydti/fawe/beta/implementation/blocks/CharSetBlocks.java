package com.boydti.fawe.beta.implementation.blocks;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CharSetBlocks extends CharBlocks implements IChunkSet {
    private static FaweCache.Pool<CharSetBlocks> POOL = FaweCache.IMP.registerPool(CharSetBlocks.class, CharSetBlocks::new, Settings.IMP.QUEUE.POOL);
    public static CharSetBlocks newInstance() {
        return POOL.poll();
    }

    public BiomeType[] biomes;
    public HashMap<Short, CompoundTag> tiles;
    public HashSet<CompoundTag> entities;
    public HashSet<UUID> entityRemoves;

    private CharSetBlocks() {}

    @Override
    public void recycle() {
        POOL.offer(this);
    }

    @Override
    public char[] getArray(int layer) {
        return sections[layer].get(this, layer);
    }

    @Override
    public BiomeType[] getBiomes() {
        return biomes;
    }

    @Override
    public Map<Short, CompoundTag> getTiles() {
        return tiles;
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return entities;
    }

    @Override
    public Set<UUID> getEntityRemoves() {
        return entityRemoves;
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        if (biomes == null) {
            biomes = new BiomeType[256];
        }
        biomes[x + (z << 4)] = biome;
        return true;
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return BlockTypes.states[get(x, y, z)];
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder holder) {
        set(x, y, z, holder.getOrdinalChar());
        return true;
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tile) {
        if (tiles == null) {
            tiles = new HashMap<>();
        }
        final short pair = MathMan.tripleBlockCoord(x, y, z);
        tiles.put(pair, tile);
        return true;
    }

    @Override
    public void setEntity(CompoundTag tag) {
        if (entities == null) {
            entities = new HashSet<>();
        }
        entities.add(tag);
    }

    @Override
    public void removeEntity(UUID uuid) {
        if (entityRemoves == null) {
            entityRemoves = new HashSet<>();
        }
        entityRemoves.add(uuid);
    }

    @Override
    public boolean isEmpty() {
        if (biomes != null) {
            return false;
        }
        for (int i = 0; i < 16; i++) {
            if (hasSection(i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public IChunkSet reset() {
        biomes = null;
        tiles = null;
        entities = null;
        entityRemoves = null;
        super.reset();
        return null;
    }
}
