package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2;

import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.queue.IBlocks;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.google.common.base.Suppliers;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.nbt.PaperweightLazyCompoundTag;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

public class PaperweightGetBlocks_Copy implements IChunkGet {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private final Map<BlockVector3, CompoundTag> tiles = new HashMap<>();
    private final Set<CompoundTag> entities = new HashSet<>();
    private final char[][] blocks;
    private final int minHeight;
    private final int maxHeight;
    final ServerLevel serverLevel;
    final LevelChunk levelChunk;
    private PalettedContainer<Holder<Biome>>[] biomes = null;

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
                new PaperweightLazyCompoundTag(Suppliers.memoize(blockEntity::saveWithId))
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
        entity.save(compoundTag);
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
    public void setCreateCopy(boolean createCopy) {
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

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        Holder<Biome> biome = biomes[(y >> 4) - getMinSectionPosition()].get(x >> 2, (y & 15) >> 2, z >> 2);
        return PaperweightPlatformAdapter.adapt(biome, serverLevel);
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

    protected void storeBiomes(int layer, PalettedContainerRO<Holder<Biome>> biomeData) {
        if (biomes == null) {
            biomes = new PalettedContainer[getSectionCount()];
        }
        if (biomeData instanceof PalettedContainer<Holder<Biome>> palettedContainer) {
            biomes[layer] = palettedContainer.copy();
        } else {
            LOGGER.error(
                    "Cannot correctly save biomes to history. Expected class type {} but got {}",
                    PalettedContainer.class.getSimpleName(),
                    biomeData.getClass().getSimpleName()
            );
        }
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
