package com.sk89q.worldedit.nukkit;

import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.fastasyncworldedit.core.queue.implementation.blocks.CharGetBlocks;
import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.fastasyncworldedit.nukkit.NukkitNbtConverter;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplAdapter;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplLoader;
import com.fastasyncworldedit.nukkit.mapping.BiomeMapping;
import com.fastasyncworldedit.nukkit.mapping.BlockMapping;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinStringTag;
import org.enginehub.linbus.tree.LinTagType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Nukkit chunk data access for FAWE's async editing system.
 */
public class NukkitGetBlocks extends CharGetBlocks {

    private static final int WATER_ID = 8;
    private static final int STILL_WATER_ID = 9;

    private final Level level;
    private final int chunkX;
    private final int chunkZ;
    private final int minY;
    private final int maxY;
    private final ReentrantLock callLock = new ReentrantLock();
    private final ConcurrentHashMap<Integer, IChunkGet> copies = new ConcurrentHashMap<>();
    private boolean createCopy = false;
    private int copyKey = 0;

    public NukkitGetBlocks(Level level, int chunkX, int chunkZ) {
        super(level.getMinBlockY() >> 4, level.getMaxBlockY() >> 4);
        this.level = level;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.minY = level.getMinBlockY();
        this.maxY = level.getMaxBlockY();
    }

    @Override
    public int getX() {
        return chunkX;
    }

    @Override
    public int getZ() {
        return chunkZ;
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        BaseFullChunk chunk = level.getChunk(chunkX, chunkZ, true);
        if (chunk == null) {
            return BiomeTypes.PLAINS;
        }
        int biomeId = chunk.getBiomeId(x & 0xF, z & 0xF);
        String jeBiome = BiomeMapping.beToJe(biomeId);
        BiomeType type = BiomeTypes.get(jeBiome);
        return type != null ? type : BiomeTypes.PLAINS;
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        BaseFullChunk chunk = level.getChunk(chunkX, chunkZ, true);
        if (chunk == null) {
            return BlockTypesCache.states[BlockTypesCache.ReservedIDs.AIR];
        }
        int fullId = chunk.getFullBlock(x & 0xF, y, z & 0xF);
        char ordinal = BlockMapping.fullIdToJeOrdinal(fullId);
        if (ordinal == Character.MAX_VALUE) {
            return BlockTypesCache.states[BlockTypesCache.ReservedIDs.AIR];
        }
        BlockState state = BlockTypesCache.states[ordinal];
        // Check layer 1 for waterlogged
        if (state.getBlockType().hasProperty(PropertyKey.WATERLOGGED)) {
            int layer1Id = NukkitImplLoader.get().getBlockId(chunk, x & 0xF, y, z & 0xF, 1);
            if (layer1Id == WATER_ID || layer1Id == STILL_WATER_ID) {
                state = state.with(PropertyKey.WATERLOGGED, true);
            }
        }
        return state;
    }

    @Override
    public char[] update(int layer, char[] data, boolean aggressive) {
        BaseFullChunk chunk = level.getChunk(chunkX, chunkZ, true);
        if (chunk == null) {
            return data;
        }
        if (data == null) {
            data = new char[4096];
        }

        int baseY = layer << 4;
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int index = (y << 8) | (z << 4) | x;
                    int fullId = chunk.getFullBlock(x, baseY + y, z);
                    char ordinal = BlockMapping.fullIdToJeOrdinal(fullId);
                    if (ordinal == Character.MAX_VALUE) {
                        data[index] = BlockTypesCache.ReservedIDs.AIR;
                    } else {
                        // Apply waterlogged state from layer 1
                        BlockState state = BlockTypesCache.states[ordinal];
                        if (state != null && state.getBlockType().hasProperty(PropertyKey.WATERLOGGED)) {
                            int layer1Id = NukkitImplLoader.get().getBlockId(chunk, x, baseY + y, z, 1);
                            if (layer1Id == WATER_ID || layer1Id == STILL_WATER_ID) {
                                state = state.with(PropertyKey.WATERLOGGED, true);
                                ordinal = state.getOrdinalChar();
                            }
                        }
                        data[index] = ordinal;
                    }
                }
            }
        }
        return data;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T extends Future<T>> T call(IQueueExtent<? extends IChunk> owner, IChunkSet set, Runnable finalizer) {
        BaseFullChunk chunk = level.getChunk(chunkX, chunkZ, true);
        if (chunk == null) {
            if (finalizer != null) {
                finalizer.run();
            }
            return (T) (Future) CompletableFuture.completedFuture(null);
        }

        // Create snapshot copy for undo if requested
        NukkitGetBlocks_Copy copy = null;
        if (createCopy) {
            copy = new NukkitGetBlocks_Copy(chunkX, chunkZ, minY, maxY);
            for (int layer = set.getMinSectionPosition(); layer <= set.getMaxSectionPosition(); layer++) {
                if (!set.hasSection(layer)) {
                    continue;
                }
                // Store current blocks before modification
                int baseY = layer << 4;
                char[] sectionData = new char[4096];
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            int index = (y << 8) | (z << 4) | x;
                            int fullId = chunk.getFullBlock(x, baseY + y, z);
                            char ordinal = BlockMapping.fullIdToJeOrdinal(fullId);
                            if (ordinal == Character.MAX_VALUE) {
                                sectionData[index] = BlockTypesCache.ReservedIDs.AIR;
                            } else {
                                // Apply waterlogged state from layer 1
                                BlockState state = BlockTypesCache.states[ordinal];
                                if (state != null && state.getBlockType().hasProperty(PropertyKey.WATERLOGGED)) {
                                    int layer1Id = NukkitImplLoader.get().getBlockId(chunk, x, baseY + y, z, 1);
                                    if (layer1Id == WATER_ID || layer1Id == STILL_WATER_ID) {
                                        state = state.with(PropertyKey.WATERLOGGED, true);
                                        ordinal = state.getOrdinalChar();
                                    }
                                }
                                sectionData[index] = ordinal;
                            }
                        }
                    }
                }
                copy.storeSection(layer, sectionData);
            }
            // Store existing block entities in affected area
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                int beY = be.getFloorY();
                int layer = beY >> 4;
                if (layer >= set.getMinSectionPosition() && layer <= set.getMaxSectionPosition()
                        && set.hasSection(layer)) {
                    BlockVector3 pos = BlockVector3.at(be.getFloorX(), beY, be.getFloorZ());
                    copy.storeTile(pos, NukkitNbtConverter.toFawe(be.namedTag));
                }
            }
            // Store current biomes before modification
            if (set.hasBiomes()) {
                for (int y = set.getMinSectionPosition() << 4; y <= (set.getMaxSectionPosition() << 4) + 15; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            if (set.getBiomeType(x, y, z) != null) {
                                int biomeId = chunk.getBiomeId(x & 0xF, z & 0xF);
                                String jeBiome = BiomeMapping.beToJe(biomeId);
                                BiomeType type = BiomeTypes.get(jeBiome);
                                copy.storeBiome(x, y, z, type != null ? type : BiomeTypes.PLAINS);
                            }
                        }
                    }
                }
            }
        }

        // Apply block changes
        for (int layer = set.getMinSectionPosition(); layer <= set.getMaxSectionPosition(); layer++) {
            if (!set.hasSection(layer)) {
                continue;
            }
            char[] setBlocks = set.loadIfPresent(layer);
            if (setBlocks == null) {
                continue;
            }
            int baseY = layer << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        int index = (y << 8) | (z << 4) | x;
                        char ordinal = setBlocks[index];
                        if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                            continue;
                        }
                        // Check for waterlogged and handle layer 1
                        BlockState state = BlockTypesCache.states[ordinal];
                        boolean waterlogged = false;
                        if (state != null && state.getBlockType().hasProperty(PropertyKey.WATERLOGGED)) {
                            Property<Boolean> waterloggedProp = state.getBlockType().getProperty(PropertyKey.WATERLOGGED);
                            if (waterloggedProp != null && state.getState(waterloggedProp) == Boolean.TRUE) {
                                waterlogged = true;
                                state = state.with(waterloggedProp, false);
                                ordinal = state.getOrdinalChar();
                            }
                        }
                        NukkitImplAdapter adapter = NukkitImplLoader.get();
                        int dataBits = adapter.getBlockDataBits();
                        int dataMask = adapter.getBlockDataMask();
                        int fullId = BlockMapping.jeOrdinalToFullId(ordinal);
                        int blockId = fullId >> dataBits;
                        int meta = fullId & dataMask;
                        adapter.setFullBlockId(
                                chunk, x, baseY + y, z, 0,
                                (blockId << dataBits) | meta
                        );
                        // Set or clear layer 1 water
                        if (waterlogged) {
                            adapter.setFullBlockId(
                                    chunk, x, baseY + y, z, 1,
                                    STILL_WATER_ID << dataBits
                            );
                        } else if (state != null && state.getBlockType().hasProperty(PropertyKey.WATERLOGGED)) {
                            // Clear water from layer 1 if block supports waterlogged but isn't
                            adapter.setFullBlockId(chunk, x, baseY + y, z, 1, 0);
                        }
                    }
                }
            }
        }

        // Apply biome changes
        if (set.hasBiomes()) {
            for (int y = set.getMinSectionPosition() << 4; y <= (set.getMaxSectionPosition() << 4) + 15; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BiomeType biome = set.getBiomeType(x, y, z);
                        if (biome != null) {
                            int beBiomeId = BiomeMapping.jeToBe(biome.id());
                            chunk.setBiomeId(x, z, (byte) beBiomeId);
                        }
                    }
                }
            }
        }

        // Apply block entity changes
        Map<BlockVector3, FaweCompoundTag> setTiles = set.tiles();
        if (!setTiles.isEmpty()) {
            for (Map.Entry<BlockVector3, FaweCompoundTag> entry : setTiles.entrySet()) {
                BlockVector3 pos = entry.getKey();
                CompoundTag nbt = NukkitNbtConverter.toNukkit(entry.getValue());
                nbt.putInt("x", pos.x());
                nbt.putInt("y", pos.y());
                nbt.putInt("z", pos.z());

                // Remove existing block entity at this position
                BlockEntity existing = chunk.getTile(pos.x() & 0xF, pos.y(), pos.z() & 0xF);
                if (existing != null) {
                    existing.close();
                }

                // Create new block entity from NBT
                if (nbt.contains("id")) {
                    String id = nbt.getString("id").replaceFirst("BlockEntity", "");
                    BlockEntity.createBlockEntity(id, chunk, nbt);
                }
            }
        }

        // Apply entity removals
        Set<UUID> entityRemoves = set.getEntityRemoves();
        if (entityRemoves != null && !entityRemoves.isEmpty()) {
            Map<Long, cn.nukkit.entity.Entity> chunkEntities = level.getChunkEntities(chunkX, chunkZ);
            Set<UUID> entitiesRemoved = new HashSet<>();
            NukkitImplAdapter uuidAdapter = NukkitImplLoader.get();
            for (cn.nukkit.entity.Entity entity : chunkEntities.values()) {
                if (entity instanceof cn.nukkit.Player) {
                    continue;
                }
                UUID entityUUID = uuidAdapter.getEntityUUID(entity);
                if (entityRemoves.contains(entityUUID)) {
                    if (copy != null) {
                        copy.storeEntity(entity, entityUUID);
                    }
                    entity.close();
                    entitiesRemoved.add(entityUUID);
                }
            }
            set.getEntityRemoves().clear();
            set.getEntityRemoves().addAll(entitiesRemoved);
        }

        // Apply entity creations
        Collection<FaweCompoundTag> setEntities = set.entities();
        if (setEntities != null && !setEntities.isEmpty()) {
            for (FaweCompoundTag nativeTag : setEntities) {
                LinCompoundTag linTag = nativeTag.linTag();
                LinStringTag idTag = linTag.findTag("Id", LinTagType.stringTag());
                if (idTag == null) {
                    idTag = linTag.findTag("id", LinTagType.stringTag());
                }
                if (idTag == null) {
                    continue;
                }
                CompoundTag nukkitNbt = NukkitNbtConverter.toNukkit(nativeTag);
                cn.nukkit.entity.Entity created = cn.nukkit.entity.Entity.createEntity(
                        idTag.value(), chunk, nukkitNbt
                );
                if (created != null) {
                    created.spawnToAll();
                }
            }
        }

        // Store copy for undo
        if (copy != null) {
            copies.put(copyKey, copy);
        }

        chunk.setChanged(true);
        for (cn.nukkit.Player player : level.getChunkPlayers(chunkX, chunkZ).values()) {
            level.requestChunk(chunkX, chunkZ, player);
        }

        if (finalizer != null) {
            finalizer.run();
        }
        return (T) (Future) CompletableFuture.completedFuture(null);
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        BaseFullChunk chunk = level.getChunk(chunkX, chunkZ, true);
        if (chunk == null) {
            return 15;
        }
        return chunk.getBlockSkyLight(x & 0xF, y, z & 0xF);
    }

    @Override
    public int getEmittedLight(int x, int y, int z) {
        BaseFullChunk chunk = level.getChunk(chunkX, chunkZ, true);
        if (chunk == null) {
            return 0;
        }
        return chunk.getBlockLight(x & 0xF, y, z & 0xF);
    }

    @Override
    public int[] getHeightMap(HeightMapType type) {
        return new int[256];
    }

    @Nullable
    @Override
    public FaweCompoundTag tile(int x, int y, int z) {
        BaseFullChunk chunk = level.getChunk(chunkX, chunkZ, true);
        if (chunk == null) {
            return null;
        }
        BlockEntity blockEntity = chunk.getTile(x & 0xF, y, z & 0xF);
        if (blockEntity == null) {
            return null;
        }
        return NukkitNbtConverter.toFawe(blockEntity.namedTag);
    }

    @Override
    public Map<BlockVector3, FaweCompoundTag> tiles() {
        BaseFullChunk chunk = level.getChunk(chunkX, chunkZ, true);
        if (chunk == null) {
            return Collections.emptyMap();
        }
        Map<Long, BlockEntity> blockEntities = chunk.getBlockEntities();
        if (blockEntities.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<BlockVector3, FaweCompoundTag> result = new HashMap<>();
        for (BlockEntity be : blockEntities.values()) {
            BlockVector3 pos = BlockVector3.at(be.getFloorX(), be.getFloorY(), be.getFloorZ());
            result.put(pos, NukkitNbtConverter.toFawe(be.namedTag));
        }
        return result;
    }

    @Override
    public Collection<FaweCompoundTag> entities() {
        Map<Long, cn.nukkit.entity.Entity> chunkEntities = level.getChunkEntities(chunkX, chunkZ);
        if (chunkEntities.isEmpty()) {
            return Collections.emptyList();
        }
        NukkitImplAdapter adapter = NukkitImplLoader.get();
        List<FaweCompoundTag> result = new ArrayList<>();
        for (cn.nukkit.entity.Entity entity : chunkEntities.values()) {
            if (entity instanceof cn.nukkit.Player) {
                continue;
            }
            entity.saveNBT();
            // Ensure UUID is stored in NBT (NKX entities don't save it by default)
            if (!entity.namedTag.contains("uuid")) {
                entity.namedTag.putString("uuid", adapter.getEntityUUID(entity).toString());
            }
            result.add(NukkitNbtConverter.toFawe(entity.namedTag));
        }
        return result;
    }

    @Override
    public Set<Entity> getFullEntities() {
        Map<Long, cn.nukkit.entity.Entity> chunkEntities = level.getChunkEntities(chunkX, chunkZ);
        if (chunkEntities.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Entity> result = new HashSet<>();
        for (cn.nukkit.entity.Entity entity : chunkEntities.values()) {
            if (entity instanceof cn.nukkit.Player) {
                continue;
            }
            result.add(new NukkitEntity(entity));
        }
        return result;
    }

    @Nullable
    @Override
    public FaweCompoundTag entity(UUID uuid) {
        Map<Long, cn.nukkit.entity.Entity> chunkEntities = level.getChunkEntities(chunkX, chunkZ);
        NukkitImplAdapter adapter = NukkitImplLoader.get();
        for (cn.nukkit.entity.Entity entity : chunkEntities.values()) {
            if (entity instanceof cn.nukkit.Player) {
                continue;
            }
            if (uuid.equals(adapter.getEntityUUID(entity))) {
                entity.saveNBT();
                if (!entity.namedTag.contains("uuid")) {
                    entity.namedTag.putString("uuid", uuid.toString());
                }
                return NukkitNbtConverter.toFawe(entity.namedTag);
            }
        }
        return null;
    }

    @Override
    public void setLightingToGet(char[][] lighting, int startSectionIndex, int endSectionIndex) {
        // Lighting managed by Nukkit
    }

    @Override
    public void setSkyLightingToGet(char[][] lighting, int startSectionIndex, int endSectionIndex) {
        // Lighting managed by Nukkit
    }

    @Override
    public void setHeightmapToGet(HeightMapType type, int[] data) {
        // Heightmap managed by Nukkit
    }

    @Override
    public void removeSectionLighting(int layer, boolean sky) {
        // Lighting managed by Nukkit
    }

    @Override
    public boolean isCreateCopy() {
        return createCopy;
    }

    @Override
    public int setCreateCopy(boolean createCopy) {
        if (!callLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Not call-locked");
        }
        this.createCopy = createCopy;
        return ++this.copyKey;
    }

    @Override
    public IChunkGet getCopy(final int key) {
        return copies.remove(key);
    }

    @Override
    public void lockCall() {
        this.callLock.lock();
    }

    @Override
    public void unlockCall() {
        this.callLock.unlock();
    }

    @Override
    public int getMaxY() {
        return maxY;
    }

    @Override
    public int getMinY() {
        return minY;
    }

}
