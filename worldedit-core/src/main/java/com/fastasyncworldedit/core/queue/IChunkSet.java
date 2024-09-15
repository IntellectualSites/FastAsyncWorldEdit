package com.fastasyncworldedit.core.queue;

import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.OutputExtent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Interface for setting blocks
 */
public interface IChunkSet extends IBlocks, OutputExtent {

    @Override
    boolean setBiome(int x, int y, int z, BiomeType biome);

    @Override
    default boolean setBiome(BlockVector3 position, BiomeType biome) {
        return setBiome(position.x(), position.y(), position.z(), biome);
    }

    @Override
    <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T holder);

    void setBlocks(int layer, char[] data);

    boolean isEmpty();

    @Override
    @Deprecated(forRemoval = true, since = "2.11.2")
    default boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        return tile(x, y, z, FaweCompoundTag.of(tile.toLinTag()));
    }

    boolean tile(int x, int y, int z, FaweCompoundTag tag);

    @Override
    void setBlockLight(int x, int y, int z, int value);

    @Override
    void setSkyLight(int x, int y, int z, int value);

    @Override
    void setHeightMap(HeightMapType type, int[] heightMap);

    void setLightLayer(int layer, char[] toSet);

    void setSkyLightLayer(int layer, char[] toSet);

    void setFullBright(int layer);

    @Deprecated(forRemoval = true, since = "2.11.2")
    default void setEntity(CompoundTag tag) {
        entity(FaweCompoundTag.of(tag::toLinTag));
    }

    void entity(FaweCompoundTag tag);

    void removeEntity(UUID uuid);

    Set<UUID> getEntityRemoves();

    /**
     * This will return only biomes SET to the EXTENT or QUEUE. This will NOT return the current biomes in the world.
     * This is used for history purposes.
     *
     * @return Array of biomes set
     */
    BiomeType[][] getBiomes();

    default boolean hasBiomes() {
        return getBiomes() != null;
    }

    char[][] getLight();

    char[][] getSkyLight();

    default boolean hasLight() {
        return getLight() != null;
    }

    // Default to avoid tricky child classes. We only need it in a few cases anyway.
    default void setFastMode(boolean fastMode) {
    }

    default boolean isFastMode() {
        return false;
    }

    // Allow setting for bitmask for flushing lighting. Default to avoid tricky child classes.
    default void setBitMask(int bitMask) {
    }

    default int getBitMask() {
        return -1;
    }

    default Map<HeightMapType, int[]> getHeightMaps() {
        return new EnumMap<>(HeightMapType.class);
    }

    @Override
    IChunkSet reset();

    @Nullable
    @Override
    default Operation commit() {
        return null;
    }

    /**
     * If the given layer has biomes stored to be set to the world. Can be negative
     *
     * @param layer layer to check
     * @return if the layer has biomes stored to be set to the world
     */
    boolean hasBiomes(int layer);

    /**
     * Create an entirely distinct copy of this SET instance. All mutable data must be copied to sufficiently prevent leakage
     * between the copy and the original.
     *
     * @return distinct new {@link IChunkSet instance}
     */
    @Nonnull
    default IChunkSet createCopy() {
        return this;
    }

}
