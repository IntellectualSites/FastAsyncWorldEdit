package com.fastasyncworldedit.core.queue;

import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.util.NbtUtils;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinDoubleTag;
import org.enginehub.linbus.tree.LinListTag;
import org.enginehub.linbus.tree.LinStringTag;
import org.enginehub.linbus.tree.LinTag;
import org.enginehub.linbus.tree.LinTagType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IChunkExtent<T extends IChunk> extends Extent {

    /**
     * Get the IChunk at a position (and cache it if it's not already)
     *
     * @return IChunk
     */
    T getOrCreateChunk(int chunkX, int chunkZ);

    @Override
    default <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B state) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.setBlock(x & 15, y, z & 15, state);
    }

    @Override
    default boolean tile(int x, int y, int z, FaweCompoundTag tile) throws WorldEditException {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.tile(x & 15, y, z & 15, tile);
    }

    @Override
    default boolean setBiome(int x, int y, int z, BiomeType biome) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.setBiome(x & 15, y, z & 15, biome);
    }

    @Override
    default boolean setBiome(BlockVector3 position, BiomeType biome) {
        final IChunk chunk = getOrCreateChunk(position.x() >> 4, position.z() >> 4);
        return chunk.setBiome(position.x() & 15, position.y(), position.z() & 15, biome);
    }

    @Override
    default BlockState getBlock(int x, int y, int z) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.getBlock(x & 15, y, z & 15);
    }

    @Override
    default BaseBlock getFullBlock(int x, int y, int z) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.getFullBlock(x & 15, y, z & 15);
    }

    @Override
    default BiomeType getBiomeType(int x, int y, int z) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.getBiomeType(x & 15, y, z & 15);
    }

    @Override
    default BiomeType getBiome(BlockVector3 position) {
        final IChunk chunk = getOrCreateChunk(position.x() >> 4, position.z() >> 4);
        return chunk.getBiomeType(position.x() & 15, position.y(), position.z() & 15);
    }

    @Override
    default void setSkyLight(int x, int y, int z, int value) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        chunk.setSkyLight(x & 15, y, z & 15, value);
    }

    @Override
    default void setBlockLight(int x, int y, int z, int value) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        chunk.setSkyLight(x & 15, y, z & 15, value);
    }

    @Override
    default int getSkyLight(int x, int y, int z) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.getSkyLight(x & 15, y, z & 15);
    }

    @Override
    default int getEmittedLight(int x, int y, int z) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.getEmittedLight(x & 15, y, z & 15);
    }

    @Override
    default int getBrightness(int x, int y, int z) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.getBrightness(x & 15, y, z & 15);
    }

    @Override
    default int getOpacity(int x, int y, int z) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.getOpacity(x & 15, y, z & 15);
    }

    @Override
    default Entity createEntity(Location location, BaseEntity entity) {
        return createEntity(location, entity, UUID.randomUUID());
    }

    @Override
    default Entity createEntity(Location location, BaseEntity entity, UUID uuid) {
        final IChunk chunk = getOrCreateChunk(location.getBlockX() >> 4, location.getBlockZ() >> 4);
        Map<String, LinTag<?>> map = new HashMap<>(entity.getNbt().value()); //do not modify original entity data
        map.put("Id", LinStringTag.of(entity.getType().getName()));

        //Set pos
        List<LinDoubleTag> posList = new ArrayList<>();
        posList.add(LinDoubleTag.of(location.x()));
        posList.add(LinDoubleTag.of(location.y()));
        posList.add(LinDoubleTag.of(location.z()));
        map.put("Pos", LinListTag.of(LinTagType.doubleTag(), posList));
        // Todo: Add again rotation
        NbtUtils.addUUIDToMap(map, uuid);

        chunk.entity(FaweCompoundTag.of(LinCompoundTag.of(map)));
        return new IChunkEntity(this, location, uuid, entity);
    }

    @Override
    default void removeEntity(int x, int y, int z, UUID uuid) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        chunk.removeEntity(uuid);
    }

    record IChunkEntity(Extent extent, Location location, UUID uuid, BaseEntity base) implements Entity {

        @Override
        public BaseEntity getState() {
            return base;
        }

        @Override
        public boolean remove() {
            extent.removeEntity(location.getBlockX(), location.getBlockY(), location.getBlockZ(), uuid);
            return true;
        }

        @Override
        public <T> T getFacet(Class<? extends T> cls) {
            return null;
        }

        @Override
        public Location getLocation() {
            return location;
        }

        @Override
        public boolean setLocation(Location location) {
            return false;
        }

        @Override
        public Extent getExtent() {
            return extent;
        }

    }

}
