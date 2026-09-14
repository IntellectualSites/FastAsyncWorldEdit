package com.fastasyncworldedit.forge1710.world;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.fastasyncworldedit.core.queue.implementation.blocks.CharGetBlocks;
import com.fastasyncworldedit.forge1710.registry.NativeBlockMapper;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import net.minecraft.block.Block;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Chunk reader/writer for 1.7.10.
 *
 * <p>Reads go through {@link ExtendedBlockStorage}'s block/metadata accessors, which EndlessIDs redirects to its
 * extended storage, so extended block ids work without touching the raw arrays. Writes are applied on the server
 * thread with {@code World.setBlock}; this is the correct-but-slow path (milestone M1).</p>
 */
public class Forge1710GetBlocks extends CharGetBlocks {

    private static final Logger LOGGER = LogManager.getLogger("FAWE-Forge1710");
    private static final int SECTIONS = 16;

    private final Forge1710World world;
    private final int chunkX;
    private final int chunkZ;
    private final ReentrantLock callLock = new ReentrantLock();
    private final ConcurrentHashMap<Integer, IChunkGet> copies = new ConcurrentHashMap<>();
    private boolean createCopy;
    private int copyKey;

    public Forge1710GetBlocks(Forge1710World world, int chunkX, int chunkZ) {
        super(0, SECTIONS - 1);
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    private Chunk chunk() {
        return world.getChunk(chunkX, chunkZ);
    }

    @Override
    public char[] update(int layer, char[] data, boolean aggressive) {
        if (data == null) {
            data = new char[4096];
        }
        readSection(chunk(), layer, data);
        return data;
    }

    static void readSection(Chunk chunk, int layer, char[] data) {
        ExtendedBlockStorage storage = layer >= 0 && layer < SECTIONS ? chunk.getBlockStorageArray()[layer] : null;
        if (storage == null) {
            Arrays.fill(data, (char) BlockTypesCache.ReservedIDs.AIR);
            return;
        }
        NativeBlockMapper mapper = NativeBlockMapper.get();
        int index = 0;
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    Block block = storage.getBlockByExtId(x, y, z);
                    int meta = storage.getExtBlockMetadata(x, y, z);
                    data[index++] = mapper.toOrdinal(Block.getIdFromBlock(block), meta);
                }
            }
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public synchronized <T extends Future<T>> T call(IQueueExtent<? extends IChunk> owner, IChunkSet set, Runnable finalizer) {
        if (!callLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Attempted to call chunk GET but chunk was not call-locked.");
        }
        final int key = copyKey;
        final boolean copy = createCopy;
        // Supplier (not Runnable): on the main thread QueueHandler runs it inline, and the Runnable overload would return a
        // cancelled future there.
        Supplier<Void> task = () -> {
            try {
                apply(set, key, copy);
            } catch (Throwable t) {
                LOGGER.error("Error applying FAWE changes to chunk {},{}", chunkX, chunkZ, t);
            }
            if (finalizer != null) {
                finalizer.run();
            }
            return null;
        };
        return (T) (Future) Fawe.instance().getQueueHandler().sync(task);
    }

    /**
     * Server thread only.
     */
    private void apply(IChunkSet set, int key, boolean copy) {
        WorldServer nmsWorld = world.getWorld();
        Chunk chunk = nmsWorld.getChunkFromChunkCoords(chunkX, chunkZ);
        Forge1710GetBlocksCopy snapshot = null;
        if (copy) {
            snapshot = new Forge1710GetBlocksCopy(chunkX, chunkZ);
            if (copies.putIfAbsent(key, snapshot) != null) {
                throw new IllegalStateException("Copy key already used.");
            }
        }
        SideEffectSet sideEffects = set.getSideEffectSet();
        boolean neighbors = sideEffects != null && sideEffects.shouldApply(SideEffect.NEIGHBORS);
        int flags = 2 | (neighbors ? 1 : 0);
        NativeBlockMapper mapper = NativeBlockMapper.get();
        int bx = chunkX << 4;
        int bz = chunkZ << 4;
        for (int layer = 0; layer < SECTIONS; layer++) {
            if (!set.hasSection(layer)) {
                continue;
            }
            char[] setArr = set.loadIfPresent(layer);
            if (setArr == null) {
                continue;
            }
            if (snapshot != null) {
                char[] before = new char[4096];
                readSection(chunk, layer, before);
                snapshot.storeSection(layer, before);
            }
            int by = layer << 4;
            for (int index = 0; index < 4096; index++) {
                char ordinal = setArr[index];
                if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                    continue;
                }
                int nativeId = mapper.toNative(ordinal);
                if (nativeId < 0) {
                    continue;
                }
                int x = bx + (index & 15);
                int y = by + (index >> 8);
                int z = bz + ((index >> 4) & 15);
                Block block = Block.getBlockById(nativeId >> 4);
                int meta = nativeId & 15;
                if (chunk.getBlock(x & 15, y, z & 15) == block && chunk.getBlockMetadata(x & 15, y, z & 15) == meta) {
                    continue;
                }
                nmsWorld.setBlock(x, y, z, block, meta, flags);
            }
            synchronized (sectionLocks[layer]) {
                blocks[layer] = null;
            }
        }
    }

    /**
     * Server thread only.
     */
    public void resendToWatchers() {
        Forge1710World.resendChunk(world.getWorld(), chunkX, chunkZ);
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        return world.getBiomeType((chunkX << 4) + (x & 15), y, (chunkZ << 4) + (z & 15));
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        return y < 0 || y > 255 ? 15 : chunk().getSavedLightValue(EnumSkyBlock.Sky, x & 15, y, z & 15);
    }

    @Override
    public int getEmittedLight(int x, int y, int z) {
        return y < 0 || y > 255 ? 0 : chunk().getSavedLightValue(EnumSkyBlock.Block, x & 15, y, z & 15);
    }

    @Override
    public int[] getHeightMap(HeightMapType type) {
        Chunk chunk = chunk();
        int[] result = new int[256];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                result[z << 4 | x] = chunk.getHeightValue(x, z);
            }
        }
        return result;
    }

    @Override
    public @Nullable FaweCompoundTag entity(UUID uuid) {
        return null;
    }

    @Override
    public Set<Entity> getFullEntities() {
        return Collections.emptySet();
    }

    @Override
    public boolean isCreateCopy() {
        return createCopy;
    }

    @Override
    public int setCreateCopy(boolean createCopy) {
        if (!callLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Attempting to set if chunk GET should create copy, but it is not call-locked.");
        }
        this.createCopy = createCopy;
        return ++this.copyKey;
    }

    @Override
    public IChunkGet getCopy(int key) {
        return copies.remove(key);
    }

    @Override
    public void lockCall() {
        callLock.lock();
    }

    @Override
    public void unlockCall() {
        callLock.unlock();
    }

    @Override
    public void setLightingToGet(char[][] lighting, int startSectionIndex, int endSectionIndex) {
    }

    @Override
    public void setSkyLightingToGet(char[][] lighting, int startSectionIndex, int endSectionIndex) {
    }

    @Override
    public void setHeightmapToGet(HeightMapType type, int[] data) {
    }

    @Override
    public int getMaxY() {
        return 255;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public Map<BlockVector3, FaweCompoundTag> tiles() {
        return Collections.emptyMap();
    }

    @Override
    public @Nullable FaweCompoundTag tile(int x, int y, int z) {
        return null;
    }

    @Override
    public Collection<FaweCompoundTag> entities() {
        return Collections.emptyList();
    }

    @Override
    public void removeSectionLighting(int layer, boolean sky) {
    }

    @Override
    public int getX() {
        return chunkX;
    }

    @Override
    public int getZ() {
        return chunkZ;
    }

}
