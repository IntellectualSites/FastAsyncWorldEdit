package com.boydti.fawe.beta;

import com.boydti.fawe.beta.implementation.lighting.HeightMapType;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.OutputExtent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Interface for setting blocks
 */
public interface IChunkSet extends IBlocks, OutputExtent {

    @Override
    boolean setBiome(int x, int y, int z, BiomeType biome);

    @Override
    default boolean setBiome(BlockVector3 position, BiomeType biome) {
        return setBiome(position.getX(), position.getY(), position.getZ(), biome);
    }

    @Override
    <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T holder);

    void setBlocks(int layer, char[] data);

    boolean isEmpty();

    @Override
    boolean setTile(int x, int y, int z, CompoundTag tile);

    @Override
    void setBlockLight(int x, int y, int z, int value);

    @Override
    void setSkyLight(int x, int y, int z, int value);

    @Override
    void setHeightMap(HeightMapType type, int[] heightMap);

    void setLightLayer(int layer, char[] toSet);

    void setSkyLightLayer(int layer, char[] toSet);

    void removeSectionLighting(int layer, boolean sky);

    void setFullBright(int layer);

    void setEntity(CompoundTag tag);

    void removeEntity(UUID uuid);

    Set<UUID> getEntityRemoves();

    /**
     * This will return only biomes SET to the EXTENT or QUEUE. This will NOT return the current biomes in the world.
     * This is used for history purposes.
     *
     * @return Array of biomes set
     */
    BiomeType[] getBiomes();

    default boolean hasBiomes() {
        return getBiomes() != null;
    }

    char[][] getLight();

    char[][] getSkyLight();

    default boolean hasLight() {
        return getLight() != null;
    }

    // Default to avoid tricky child classes. We only need it in a few cases anyway.
    default void setFastMode(boolean fastMode) {}

    default boolean isFastMode() {
        return false;
    }

    // Allow setting for bitmask for flushing lighting. Default to avoid tricky child classes.
    default void setBitMask(int bitMask) {}

    default int getBitMask() {
        return -1;
    }

    default Map<HeightMapType, int[]> getHeightMaps() {
        return new HashMap<>();
    }

    @Override
    IChunkSet reset();

    @Nullable
    @Override
    default Operation commit() {
        return null;
    }
}
