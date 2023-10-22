package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_17_R1_2;

import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.queue.IBlocks;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.google.common.base.Suppliers;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_17_R1_2.nbt.PaperweightLazyCompoundTag;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.LevelChunk;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

public class PaperweightGetBlocks_Copy implements IChunkGet {

    private final Map<BlockVector3, CompoundTag> tiles = new HashMap<>();
    private final Set<CompoundTag> entities = new HashSet<>();
    private final char[][] blocks;
    private final int minHeight;
    private final int maxHeight;
    final ServerLevel serverLevel;
    final LevelChunk levelChunk;
    private ChunkBiomeContainer chunkBiomeContainer;

    protected PaperweightGetBlocks_Copy(LevelChunk levelChunk) {
        this.levelChunk = levelChunk;
        this.serverLevel = levelChunk.level;
        this.minHeight = serverLevel.getMinBuildHeight();
        this.maxHeight = serverLevel.getMaxBuildHeight() - 1; // Minecraft max limit is exclusive.
        this.blocks = new char[getSectionCount()][];
    }

    protected void storeTile(BlockEntity blockEntity) {
        tiles.put(
                BlockVector3.at(
                        blockEntity.getBlockPos().getX(),
                        blockEntity.getBlockPos().getY(),
                        blockEntity.getBlockPos().getZ()
                ),
                new PaperweightLazyCompoundTag(Suppliers.memoize(() -> blockEntity.save(new net.minecraft.nbt.CompoundTag())))
        );
    }

    @Override
    public Map<BlockVector3, CompoundTag> getTiles() {
        return tiles;
    }

    @Override
    @Nullable
    public CompoundTag getTile(int x, int y, int z) {
        return tiles.get(BlockVector3.at(x, y, z));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void storeEntity(Entity entity) {
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        net.minecraft.nbt.CompoundTag compoundTag = new net.minecraft.nbt.CompoundTag();
        PaperweightPlatformAdapter.readEntityIntoTag(entity, compoundTag);
        entities.add((CompoundTag) adapter.toNative(compoundTag));
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return this.entities;
    }

    @Override
    public CompoundTag getEntity(UUID uuid) {
        for (CompoundTag tag : entities) {
            if (uuid.equals(tag.getUUID())) {
                return tag;
            }
        }
        return null;
    }

    @Override
    public boolean isCreateCopy() {
        return false;
    }

    @Override
    public int setCreateCopy(boolean createCopy) {
        return -1;
    }

    @Override
    public void setLightingToGet(char[][] lighting, int minSectionPosition, int maxSectionPosition) {
    }

    @Override
    public void setSkyLightingToGet(char[][] lighting, int minSectionPosition, int maxSectionPosition) {
    }

    @Override
    public void setHeightmapToGet(HeightMapType type, int[] data) {
    }

    @Override
    public int getMaxY() {
        return maxHeight;
    }

    @Override
    public int getMinY() {
        return minHeight;
    }

    @Override
    public int getMaxSectionPosition() {
        return maxHeight >> 4;
    }

    @Override
    public int getMinSectionPosition() {
        return minHeight >> 4;
    }

    protected void storeBiomes(ChunkBiomeContainer chunkBiomeContainer) {
        // The to do one line below is pre-paperweight and needs to be revised
        // TODO revisit last parameter, BiomeStorage[] *would* be more efficient
        this.chunkBiomeContainer = new ChunkBiomeContainer(chunkBiomeContainer.biomeRegistry, serverLevel,
                chunkBiomeContainer.writeBiomes()
        );
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        Biome biome = null;
        if (y == -1) {
            for (y = serverLevel.getMinBuildHeight(); y <= serverLevel.getMaxBuildHeight(); y += 4) {
                biome = this.chunkBiomeContainer.getNoiseBiome(x >> 2, y >> 2, z >> 2);
                if (biome != null) {
                    break;
                }
            }
        } else {
            biome = this.chunkBiomeContainer.getNoiseBiome(x >> 2, y >> 2, z >> 2);
        }
        return biome != null ? PaperweightPlatformAdapter.adapt(biome, serverLevel) : null;
    }

    @Override
    public void removeSectionLighting(int layer, boolean sky) {
    }

    @Override
    public boolean trim(boolean aggressive, int layer) {
        return false;
    }

    @Override
    public IBlocks reset() {
        return null;
    }

    @Override
    public int getSectionCount() {
        return serverLevel.getSectionsCount();
    }

    protected void storeSection(int layer, char[] data) {
        blocks[layer] = data;
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        BlockState state = BlockTypesCache.states[get(x, y, z)];
        return state.toBaseBlock(this, x, y, z);
    }

    @Override
    public boolean hasSection(int layer) {
        layer -= getMinSectionPosition();
        return blocks[layer] != null;
    }

    @Override
    public char[] load(int layer) {
        layer -= getMinSectionPosition();
        if (blocks[layer] == null) {
            blocks[layer] = new char[4096];
            Arrays.fill(blocks[layer], (char) BlockTypesCache.ReservedIDs.AIR);
        }
        return blocks[layer];
    }

    @Override
    public char[] loadIfPresent(int layer) {
        layer -= getMinSectionPosition();
        return blocks[layer];
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return BlockTypesCache.states[get(x, y, z)];
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        return 0;
    }

    @Override
    public int getEmittedLight(int x, int y, int z) {
        return 0;
    }

    @Override
    public int[] getHeightMap(HeightMapType type) {
        return new int[0];
    }

    @Override
    public <T extends Future<T>> T call(IChunkSet set, Runnable finalize) {
        return null;
    }

    public char get(int x, int y, int z) {
        final int layer = (y >> 4) - getMinSectionPosition();
        final int index = (y & 15) << 8 | z << 4 | x;
        return blocks[layer][index];
    }


    @Override
    public boolean trim(boolean aggressive) {
        return false;
    }

}
