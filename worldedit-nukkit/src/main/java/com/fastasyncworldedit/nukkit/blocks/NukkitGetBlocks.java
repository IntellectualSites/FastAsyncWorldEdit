package com.fastasyncworldedit.nukkit.blocks;

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
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.nukkit.NukkitEntity;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.apache.logging.log4j.Logger;
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
 * <p>
 * This class bridges FAWE's section-based chunk model to Nukkit's flat
 * {@code BaseFullChunk} representation. Unlike Bukkit, where chunks are
 * composed of {@code LevelChunkSection} arrays that FAWE can cache and
 * modify directly, Nukkit stores blocks in a flat structure with no real
 * section abstraction. We therefore read and write blocks by iterating
 * coordinates rather than copying whole sections.
 * <p>
 * Chunk caching is done lazily via {@link #getChunk()} rather than in the
 * constructor because Nukkit chunks may load asynchronously or be null
 * during world initialization. Caching eagerly could fail or stall the
 * queue thread.
 * <p>
 * Waterlogging is handled via layer 1 block access. Blocks that support
 * the WATERLOGGED property have their layer 1 checked; if water is present,
 * the state is updated accordingly. On write, waterlogged blocks split
 * the state into layer 0 (block) and layer 1 (water).
 * <p>
 * Lighting is fully delegated to Nukkit. All lighting methods are no-op
 * because Nukkit recalculates light internally when blocks change. This
 * differs from Bukkit, where FAWE must manually invoke NMS relighting.
 * <p>
 * Key differences from Bukkit:
 * <ul>
 *   <li>No chunk section abstraction; flat BaseFullChunk access</li>
 *   <li>Lazy chunk caching via synchronized getter</li>
 *   <li>Waterlogging via dual-layer block storage</li>
 *   <li>Lighting managed by Nukkit; no manual relighting</li>
 * </ul>
 *
 * @see NukkitGetBlocks_Copy
 * @see com.fastasyncworldedit.core.queue.IChunkGet
 */
public class NukkitGetBlocks extends CharGetBlocks {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private static final int WATER_ID = 8;
    private static final int STILL_WATER_ID = 9;

    private final Level level;
    private final int chunkX;
    private final int chunkZ;
    private final int minY;
    private final int maxY;
    private final ReentrantLock callLock = new ReentrantLock();
    private final ConcurrentHashMap<Integer, IChunkGet> copies = new ConcurrentHashMap<>();
    private BaseFullChunk cachedChunk;
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

    private BaseFullChunk getChunk() {
        BaseFullChunk chunk = this.cachedChunk;
        if (chunk == null) {
            synchronized (this) {
                chunk = this.cachedChunk;
                if (chunk == null) {
                    this.cachedChunk = chunk = level.getChunk(chunkX, chunkZ, true);
                }
            }
        }
        return chunk;
    }

    private static int blockIndex(int x, int y, int z) {
        return ((y & 0xF) << 8) | ((z & 0xF) << 4) | (x & 0xF);
    }

    private BlockVector3 toWorldPosition(int localX, int y, int localZ) {
        return BlockVector3.at((chunkX << 4) + (localX & 0xF), y, (chunkZ << 4) + (localZ & 0xF));
    }

    private BlockVector3 toWorldPosition(BlockVector3 localPosition) {
        return toWorldPosition(localPosition.x(), localPosition.y(), localPosition.z());
    }

    private int normalizedHeight(int y) {
        return y - (getMinSectionPosition() << 4) + 1;
    }

    private char ordinalFor(BaseFullChunk chunk, int x, int y, int z) {
        return ordinalFor(chunk, NukkitImplLoader.get(), x, y, z);
    }

    private char ordinalFor(BaseFullChunk chunk, NukkitImplAdapter adapter, int x, int y, int z) {
        int fullId = chunk.getFullBlock(x & 0xF, y, z & 0xF);
        char ordinal = BlockMapping.fullIdToJeOrdinal(fullId);
        if (ordinal == Character.MAX_VALUE) {
            throw new UnsupportedOperationException(
                    "No Java block mapping for Nukkit full block id " + fullId
                            + " at " + toWorldPosition(x, y, z)
                            + " in chunk " + chunkX + "," + chunkZ + "."
            );
        }
        BlockState state = BlockTypesCache.states[ordinal];
        if (state != null && state.getBlockType().hasProperty(PropertyKey.WATERLOGGED)) {
            int layer1Id = adapter.getBlockId(chunk, x & 0xF, y, z & 0xF, 1);
            if (layer1Id == WATER_ID || layer1Id == STILL_WATER_ID) {
                state = state.with(PropertyKey.WATERLOGGED, true);
                ordinal = state.getOrdinalChar();
            }
        }
        return ordinal;
    }

    private int[] computeHeightMap(BaseFullChunk chunk, HeightMapType type) {
        int[] heightMap = new int[256];
        int found = 0;
        NukkitImplAdapter adapter = NukkitImplLoader.get();
        for (int y = maxY; y >= minY; y--) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int index = (z << 4) | x;
                    if (heightMap[index] != 0) {
                        continue;
                    }
                    BlockState state = BlockTypesCache.states[ordinalFor(chunk, adapter, x, y, z)];
                    if (state != null && type.includes(state)) {
                        heightMap[index] = normalizedHeight(y);
                        if (++found == 256) {
                            return heightMap;
                        }
                    }
                }
            }
        }
        return heightMap;
    }

    private void storeSectionSnapshot(NukkitGetBlocks_Copy copy, BaseFullChunk chunk, NukkitImplAdapter adapter, int layer) {
        int baseY = layer << 4;
        char[] sectionData = new char[4096];
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    sectionData[blockIndex(x, y, z)] = ordinalFor(chunk, adapter, x, baseY + y, z);
                }
            }
        }
        copy.storeSection(layer, sectionData);
    }

    private void removeFailedEntities(Collection<FaweCompoundTag> setEntities, List<FaweCompoundTag> failedEntities) {
        if (failedEntities.isEmpty()) {
            return;
        }
        boolean removed;
        try {
            removed = setEntities.removeAll(failedEntities);
        } catch (UnsupportedOperationException e) {
            throw new UnsupportedOperationException(
                    "Nukkit failed to create " + failedEntities.size()
                            + " entity/entities and the chunk set entities collection is immutable. "
                            + "History cannot be corrected safely.",
                    e
            );
        }
        if (!removed) {
            throw new UnsupportedOperationException(
                    "Nukkit failed to create " + failedEntities.size()
                            + " entity/entities, but their tags were not present in the chunk set. "
                            + "History cannot be corrected safely."
            );
        }
    }

    private void validateBlockMappings(IChunkSet set) {
        for (int layer = set.getMinSectionPosition(); layer <= set.getMaxSectionPosition(); layer++) {
            if (!set.hasSection(layer)) {
                continue;
            }
            char[] setBlocks = set.loadIfPresent(layer);
            if (setBlocks == null) {
                continue;
            }
            for (char ordinal : setBlocks) {
                if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                    continue;
                }
                BlockState state = BlockTypesCache.states[ordinal];
                if (state != null && state.getBlockType().hasProperty(PropertyKey.WATERLOGGED)) {
                    Property<Boolean> waterloggedProp = state.getBlockType().getProperty(PropertyKey.WATERLOGGED);
                    if (waterloggedProp != null && state.getState(waterloggedProp) == Boolean.TRUE) {
                        ordinal = state.with(waterloggedProp, false).getOrdinalChar();
                    }
                }
                BlockMapping.jeOrdinalToFullId(ordinal);
            }
        }
    }

    private Set<UUID> validateEntityRemoves(IChunkSet set) {
        Set<UUID> entityRemoves = set.getEntityRemoves();
        if (entityRemoves == null || entityRemoves.isEmpty()) {
            return entityRemoves;
        }
        Set<UUID> requested = new HashSet<>(entityRemoves);
        try {
            entityRemoves.clear();
            entityRemoves.addAll(requested);
        } catch (UnsupportedOperationException e) {
            throw new UnsupportedOperationException(
                    "Nukkit entity removals require a mutable entity-removal set so history can be corrected.",
                    e
            );
        }
        return entityRemoves;
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        BaseFullChunk chunk = getChunk();
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
        BaseFullChunk chunk = getChunk();
        if (chunk == null) {
            return BlockTypesCache.states[BlockTypesCache.ReservedIDs.AIR];
        }
        NukkitImplAdapter adapter = NukkitImplLoader.get();
        char ordinal = ordinalFor(chunk, adapter, x, y, z);
        if (ordinal == Character.MAX_VALUE) {
            return BlockTypesCache.states[BlockTypesCache.ReservedIDs.AIR];
        }
        return BlockTypesCache.states[ordinal];
    }

    @Override
    public char[] update(int layer, char[] data, boolean aggressive) {
        BaseFullChunk chunk = getChunk();
        if (chunk == null) {
            return data;
        }
        if (data == null) {
            data = new char[4096];
        }

        int baseY = layer << 4;
        NukkitImplAdapter adapter = NukkitImplLoader.get();
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    data[blockIndex(x, y, z)] = ordinalFor(chunk, adapter, x, baseY + y, z);
                }
            }
        }
        return data;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T extends Future<T>> T call(IQueueExtent<? extends IChunk> owner, IChunkSet set, Runnable finalizer) {
        if (!callLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Attempted to call chunk GET but chunk was not call-locked.");
        }

        try {
            BaseFullChunk chunk = getChunk();
            if (chunk == null) {
                return (T) (Future) CompletableFuture.completedFuture(null);
            }
            NukkitImplAdapter adapter = NukkitImplLoader.get();
            int dataBits = adapter.getBlockDataBits();
            int dataMask = adapter.getBlockDataMask();

            // Create snapshot copy for undo if requested
            NukkitGetBlocks_Copy copy = null;
            if (createCopy) {
                if (copies.containsKey(copyKey)) {
                    throw new IllegalStateException("Copy key already used.");
                }
                copy = new NukkitGetBlocks_Copy(chunkX, chunkZ, minY, maxY);
                for (int layer = set.getMinSectionPosition(); layer <= set.getMaxSectionPosition(); layer++) {
                    if (!set.hasSection(layer)) {
                        continue;
                    }
                    // Store current blocks before modification
                    storeSectionSnapshot(copy, chunk, adapter, layer);
                }
                // Store existing block entities in affected block sections
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    int beY = be.getFloorY();
                    int layer = beY >> 4;
                    if (layer >= set.getMinSectionPosition() && layer <= set.getMaxSectionPosition()
                            && set.hasSection(layer)) {
                        BlockVector3 pos = BlockVector3.at(be.getFloorX(), beY, be.getFloorZ());
                        copy.storeTile(pos, NukkitNbtConverter.toFawe(be.namedTag));
                    }
                }
                // Store existing block entities for tile-only changes.
                for (BlockVector3 localPos : set.tiles().keySet()) {
                    BlockEntity existing = chunk.getTile(localPos.x() & 0xF, localPos.y(), localPos.z() & 0xF);
                    if (existing != null) {
                        copy.storeTile(toWorldPosition(localPos), NukkitNbtConverter.toFawe(existing.namedTag));
                    }
                }
                // Store current biomes before modification.
                if (set.hasBiomes()) {
                    for (int layer = set.getMinSectionPosition(); layer <= set.getMaxSectionPosition(); layer++) {
                        if (!set.hasBiomes(layer)) {
                            continue;
                        }
                        int baseY = layer << 4;
                        for (int y = 0; y < 16; y += 4) {
                            for (int z = 0; z < 16; z += 4) {
                                for (int x = 0; x < 16; x += 4) {
                                    if (set.getBiomeType(x, baseY + y, z) != null) {
                                        int biomeId = chunk.getBiomeId(x, z);
                                        String jeBiome = BiomeMapping.beToJe(biomeId);
                                        BiomeType type = BiomeTypes.get(jeBiome);
                                        copy.storeBiome(x, baseY + y, z, type != null ? type : BiomeTypes.PLAINS);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Map<BlockVector3, FaweCompoundTag> setTiles = set.tiles();
            Map<BlockVector3, CompoundTag> nukkitTiles = new HashMap<>();
            for (Map.Entry<BlockVector3, FaweCompoundTag> entry : setTiles.entrySet()) {
                BlockVector3 localPos = entry.getKey();
                BlockVector3 worldPos = toWorldPosition(localPos);
                LinStringTag idTag = entry.getValue().linTag().findTag("id", LinTagType.stringTag());
                if (idTag == null) {
                    throw new UnsupportedOperationException(
                            "Cannot apply Nukkit block entity at " + worldPos
                                    + " in chunk " + chunkX + "," + chunkZ
                                    + " because the NBT tag does not contain an id."
                    );
                }
                CompoundTag nbt = NukkitNbtConverter.toNukkit(entry.getValue());
                nbt.putInt("x", worldPos.x());
                nbt.putInt("y", worldPos.y());
                nbt.putInt("z", worldPos.z());
                nukkitTiles.put(localPos, nbt);
            }
            BiomeType[] biomeColumns = null;
            if (set.hasBiomes()) {
                biomeColumns = new BiomeType[256];
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BiomeType selected = null;
                        for (int layer = set.getMinSectionPosition(); layer <= set.getMaxSectionPosition(); layer++) {
                            if (!set.hasBiomes(layer)) {
                                continue;
                            }
                            int baseY = layer << 4;
                            for (int y = 0; y < 16; y += 4) {
                                BiomeType biome = set.getBiomeType(x, baseY + y, z);
                                if (biome == null) {
                                    continue;
                                }
                                if (selected != null && !selected.equals(biome)) {
                                    throw new UnsupportedOperationException(
                                            "Nukkit only supports 2D biome columns; received conflicting 3D biome updates "
                                                    + "in chunk " + chunkX + "," + chunkZ + " at local column " + x + "," + z
                                    );
                                }
                                selected = biome;
                            }
                        }
                        biomeColumns[(z << 4) | x] = selected;
                    }
                }
            }
            validateBlockMappings(set);
            Set<UUID> entityRemoves = validateEntityRemoves(set);
            if (copy != null) {
                copies.put(copyKey, copy);
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
                            int index = blockIndex(x, y, z);
                            char ordinal = setBlocks[index];
                            if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                                continue;
                            }
                            BlockEntity existingTile = chunk.getTile(x, baseY + y, z);
                            if (existingTile != null) {
                                existingTile.close();
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

            // Apply biome changes. Nukkit stores biomes in 2D columns.
            if (biomeColumns != null) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BiomeType selected = biomeColumns[(z << 4) | x];
                        if (selected != null) {
                            int beBiomeId = BiomeMapping.jeToBe(selected.id());
                            chunk.setBiomeId(x, z, (byte) beBiomeId);
                        }
                    }
                }
            }

            // Apply block entity changes
            if (!nukkitTiles.isEmpty()) {
                for (Map.Entry<BlockVector3, CompoundTag> entry : nukkitTiles.entrySet()) {
                    BlockVector3 localPos = entry.getKey();
                    BlockVector3 worldPos = toWorldPosition(localPos);
                    CompoundTag nbt = entry.getValue();
                    String id = nbt.getString("id").replaceFirst("BlockEntity", "");

                    // Remove existing block entity at this position
                    BlockEntity existing = chunk.getTile(localPos.x() & 0xF, localPos.y(), localPos.z() & 0xF);
                    if (existing != null) {
                        existing.close();
                    }

                    BlockEntity created = BlockEntity.createBlockEntity(id, chunk, nbt);
                    if (created == null) {
                        throw new UnsupportedOperationException(
                                "Nukkit failed to create block entity `" + id + "` at " + worldPos
                                        + " in chunk " + chunkX + "," + chunkZ + "."
                        );
                    }
                }
            }

            // Apply entity removals
            if (entityRemoves != null && !entityRemoves.isEmpty()) {
                Map<Long, cn.nukkit.entity.Entity> chunkEntities = level.getChunkEntities(chunkX, chunkZ);
                Set<UUID> entitiesRemoved = new HashSet<>();
                for (cn.nukkit.entity.Entity entity : chunkEntities.values()) {
                    if (entity instanceof cn.nukkit.Player) {
                        continue;
                    }
                    UUID entityUUID = adapter.getEntityUUID(entity);
                    if (entityRemoves.contains(entityUUID)) {
                        if (copy != null) {
                            copy.storeEntity(entity, entityUUID);
                        }
                        entity.close();
                        entitiesRemoved.add(entityUUID);
                    }
                }
                entityRemoves.clear();
                entityRemoves.addAll(entitiesRemoved);
            }

            // Apply entity creations
            Collection<FaweCompoundTag> setEntities = set.entities();
            if (setEntities != null && !setEntities.isEmpty()) {
                List<FaweCompoundTag> failedEntities = new ArrayList<>();
                for (FaweCompoundTag nativeTag : setEntities) {
                    LinCompoundTag linTag = nativeTag.linTag();
                    LinStringTag idTag = linTag.findTag("Id", LinTagType.stringTag());
                    if (idTag == null) {
                        idTag = linTag.findTag("id", LinTagType.stringTag());
                    }
                    if (idTag == null) {
                        LOGGER.warn("Skipping Nukkit entity without Id tag in chunk {},{}: {}", chunkX, chunkZ, nativeTag);
                        failedEntities.add(nativeTag);
                        continue;
                    }
                    CompoundTag nukkitNbt = NukkitNbtConverter.toNukkit(nativeTag);
                    cn.nukkit.entity.Entity created = cn.nukkit.entity.Entity.createEntity(
                            idTag.value(), chunk, nukkitNbt
                    );
                    if (created != null) {
                        created.spawnToAll();
                    } else {
                        LOGGER.warn("Failed to create Nukkit entity `{}` in chunk {},{}", idTag.value(), chunkX, chunkZ);
                        failedEntities.add(nativeTag);
                    }
                }
                removeFailedEntities(setEntities, failedEntities);
            }

            chunk.setChanged(true);
            for (cn.nukkit.Player player : level.getChunkPlayers(chunkX, chunkZ).values()) {
                level.requestChunk(chunkX, chunkZ, player);
            }
            return (T) (Future) CompletableFuture.completedFuture(null);
        } finally {
            if (finalizer != null) {
                finalizer.run();
            }
        }
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        BaseFullChunk chunk = getChunk();
        if (chunk == null) {
            return 15;
        }
        return chunk.getBlockSkyLight(x & 0xF, y, z & 0xF);
    }

    @Override
    public int getEmittedLight(int x, int y, int z) {
        BaseFullChunk chunk = getChunk();
        if (chunk == null) {
            return 0;
        }
        return chunk.getBlockLight(x & 0xF, y, z & 0xF);
    }

    @Override
    public int[] getHeightMap(HeightMapType type) {
        BaseFullChunk chunk = getChunk();
        if (chunk == null) {
            return new int[256];
        }
        return computeHeightMap(chunk, type);
    }

    @Nullable
    @Override
    public FaweCompoundTag tile(int x, int y, int z) {
        BaseFullChunk chunk = getChunk();
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
        BaseFullChunk chunk = getChunk();
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
        // Nukkit owns persisted heightmaps; FAWE-computed arrays are not written through this adapter.
    }

    @Override
    public boolean hasNonEmptySection(int layer) {
        return layer >= getMinSectionPosition() && layer <= getMaxSectionPosition() && getChunk() != null;
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
    public boolean trim(boolean aggressive) {
        if (aggressive) {
            synchronized (this) {
                cachedChunk = null;
            }
        }
        return super.trim(aggressive);
    }

    @Override
    public boolean trim(boolean aggressive, int layer) {
        if (aggressive) {
            synchronized (this) {
                cachedChunk = null;
            }
        }
        return super.trim(aggressive, layer);
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
