package com.fastasyncworldedit.core.queue;

import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.InputExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * An interface for getting blocks.
 */
public interface IChunkGet extends IBlocks, Trimable, InputExtent, ITileInput {

    @Override
    BaseBlock getFullBlock(int x, int y, int z);

    @Override
    BiomeType getBiomeType(int x, int y, int z);

    @Override
    default BiomeType getBiome(BlockVector3 position) {
        return getBiomeType(position.x(), position.y(), position.z());
    }

    @Override
    BlockState getBlock(int x, int y, int z);

    @Override
    int getSkyLight(int x, int y, int z);

    @Override
    int getEmittedLight(int x, int y, int z);

    @Override
    int[] getHeightMap(HeightMapType type);

    default void optimize() {

    }

    <T extends Future<T>> T call(IChunkSet set, Runnable finalize);

    @Deprecated(forRemoval = true, since = "2.11.2")
    default CompoundTag getEntity(UUID uuid) {
        final FaweCompoundTag entity = entity(uuid);
        if (entity == null) {
            return null;
        }
        return new CompoundTag(entity.linTag());
    }

    /**
     * {@return the compound tag describing the entity with the given UUID, if any}
     * @param uuid the uuid of the entity
     */
    @Nullable FaweCompoundTag entity(UUID uuid);

    @Override
    @Deprecated(forRemoval = true, since = "2.11.2")
    default CompoundTag getTile(int x, int y, int z) {
        return IBlocks.super.getTile(x, y, z);
    }

    /**
     * Get the entities in the chunk as "full" entities.
     *
     * @since TODO;
     */
    Set<Entity> getFullEntities();

    boolean isCreateCopy();

    /**
     * Not for external API use. Internal use only.
     */
    int setCreateCopy(boolean createCopy);

    @Nullable
    default IChunkGet getCopy(int key) {
        return null;
    }

    /**
     * Lock the {@link IChunkGet#call(IChunkSet, Runnable)} method to the current thread using a reentrant lock. Also locks
     * related methods e.g. {@link IChunkGet#setCreateCopy(boolean)}
     *
     * @since 2.8.2
     */
    default void lockCall() {}

    /**
     * Unlock {@link IChunkGet#call(IChunkSet, Runnable)} (and other related methods) to executions from other threads
     *
     * @since 2.8.2
     */
    default void unlockCall() {}

    /**
     * Flush the block lighting array (section*blocks) to the chunk GET between the given section indices. Negative allowed.
     *
     * @param lighting          lighting array
     * @param startSectionIndex lowest section index
     * @param endSectionIndex   highest section index
     */
    void setLightingToGet(char[][] lighting, int startSectionIndex, int endSectionIndex);

    /**
     * Flush the sky lighting array (section*blocks) to the chunk GET between the given section indices. Negative allowed.
     *
     * @param lighting          sky lighting array
     * @param startSectionIndex lowest section index
     * @param endSectionIndex   highest section index
     */
    void setSkyLightingToGet(char[][] lighting, int startSectionIndex, int endSectionIndex);

    void setHeightmapToGet(HeightMapType type, int[] data);

    /**
     * Max y value for the chunk's world (inclusive)
     */
    int getMaxY();

    /**
     * Min y value for the chunk's world (inclusive)
     */
    int getMinY();

}
