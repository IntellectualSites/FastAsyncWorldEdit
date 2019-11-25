package com.boydti.fawe.beta.implementation.blocks;

import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.collection.BlockVector3ChunkMap;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CharSetBlocks extends CharBlocks implements IChunkSet {
    public BiomeType[] biomes;
    public BlockVector3ChunkMap<CompoundTag> tiles;
    public HashSet<CompoundTag> entities;
    public HashSet<UUID> entityRemoves;

    public CharSetBlocks(CharBlocks other) {
        super(other);
        if (other instanceof CharSetBlocks) {

        }
    }

    public CharSetBlocks() {

    }

    @Override
    public BiomeType[] getBiomes() {
        return biomes;
    }

    @Override
    public BiomeType getBiomeType(int x, int z) {
        if (biomes == null) return null;
        return biomes[(z << 4) | x];
    }

    @Override
    public Map<BlockVector3, CompoundTag> getTiles() {
        return tiles == null ? Collections.emptyMap() : tiles;
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return entities == null ? Collections.emptySet() : entities;
    }

    @Override
    public Set<UUID> getEntityRemoves() {
        return entityRemoves == null ? Collections.emptySet() : entityRemoves;
    }

    @Override
    public boolean setBiome(final int x, final int y, final int z, final BiomeType biome) {
        if (biomes == null) {
            biomes = new BiomeType[256];
        }
        biomes[x + (z << 4)] = biome;
        return true;
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return BlockTypesCache.states[get(x, y, z)];
    }

    @Override
    public boolean setBlock(final int x, final int y, final int z, final BlockStateHolder holder) {
        set(x, y, z, holder.getOrdinalChar());
        holder.applyTileEntity(this, x, y, z);
        return true;
    }

    @Override
    public void setBlocks(int layer, char[] data) {
        this.blocks[layer] = data;
        this.sections[layer] = data == null ? EMPTY : FULL;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block)
        throws WorldEditException {
        return setBlock(position.getX(), position.getY(), position.getZ(), block);
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tile) {
        if (tiles == null) {
            tiles = new BlockVector3ChunkMap<CompoundTag>();
        }
        tiles.put(x, y, z, tile);
        return true;
    }

    @Override
    public void setEntity(final CompoundTag tag) {
        if (entities == null) {
            entities = new HashSet<>();
        }
        entities.add(tag);
    }

    @Override
    public void removeEntity(final UUID uuid) {
        if (entityRemoves == null) {
            entityRemoves = new HashSet<>();
        }
        entityRemoves.add(uuid);
    }

    @Override
    public boolean isEmpty() {
        if (biomes != null) return false;
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
