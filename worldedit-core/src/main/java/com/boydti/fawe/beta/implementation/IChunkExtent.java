package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.IntArrayTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Range;

public interface IChunkExtent<T extends IChunk> extends Extent {
    /**
     * Get the IChunk at a position (and cache it if it's not already)
     *
     * @param chunkX
     * @param chunkZ
     * @return IChunk
     */
    T getOrCreateChunk(int chunkX, int chunkZ);

    @Override
    default <B extends BlockStateHolder<B>> boolean setBlock(int x, @Range(from = 0, to = 255) int y, int z, B state) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.setBlock(x & 15, y, z & 15, state);
    }

    @Override
    default boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.setTile(x & 15, y, z & 15, tile);
    }

    @Override
    default boolean setBiome(int x, int y, int z, BiomeType biome) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.setBiome(x & 15, y, z & 15, biome);
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
    default int getEmmittedLight(int x, int y, int z) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        return chunk.getEmmittedLight(x & 15, y, z & 15);
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
        final IChunk chunk = getOrCreateChunk(location.getBlockX() >> 4, location.getBlockZ() >> 4);
        CompoundTag tag = entity.getNbtData();
        Map<String, Tag> map = new HashMap<>(tag.getValue()); //do not modify original entity data
        map.put("Id", new StringTag(entity.getType().getName()));
        
        //Set pos
        ListTag pos = (ListTag) map.get("Pos");
        List<Tag> posList;
        if (pos != null) {
            posList = ReflectionUtils.getList(pos.getValue());
        } else {
            posList = new ArrayList<>();
            pos = new ListTag(DoubleTag.class, posList);
            map.put("Pos", pos);
        }
        posList.set(0, new DoubleTag(location.getX()));
        posList.set(1, new DoubleTag(location.getY()));
        posList.set(2, new DoubleTag(location.getZ()));
        
        //set new uuid
        UUID newuuid = UUID.randomUUID();
        IntArrayTag uuid = (IntArrayTag) map.get("UUID");
        int[] uuidArray;
        if (uuid != null) {
            uuidArray = uuid.getValue();
        } else {
            uuidArray = new int[4];
            uuid = new IntArrayTag(uuidArray);
            map.put("UUID", uuid);
        }
        uuidArray[0] = (int) (newuuid.getMostSignificantBits() >> 32);
        uuidArray[1] = (int) newuuid.getMostSignificantBits();
        uuidArray[2] = (int) (newuuid.getLeastSignificantBits() >> 32);
        uuidArray[3] = (int) newuuid.getLeastSignificantBits();
        
        map.put("UUIDMost", new LongTag(newuuid.getMostSignificantBits()));
        map.put("UUIDLeast", new LongTag(newuuid.getLeastSignificantBits()));
        
        map.put("PersistentIDMSB", new LongTag(newuuid.getMostSignificantBits()));
        map.put("PersistentIDLSB", new LongTag(newuuid.getLeastSignificantBits()));
        
        chunk.setEntity(tag);
        return new IChunkEntity(this, location, newuuid, entity);
    }

    @Override
    default void removeEntity(int x, int y, int z, UUID uuid) {
        final IChunk chunk = getOrCreateChunk(x >> 4, z >> 4);
        chunk.removeEntity(uuid);
    }
    
    class IChunkEntity implements Entity {

        private final Extent extent;
        private final Location location;
        private final UUID uuid;
        private final BaseEntity base;

        public IChunkEntity(Extent extent, Location location, UUID uuid, BaseEntity base) {
            this.extent = extent;
            this.location = location;
            this.uuid = uuid;
            this.base = base;
        }
        
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
        public <T> T getFacet(Class<? extends T> cls)  {
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
