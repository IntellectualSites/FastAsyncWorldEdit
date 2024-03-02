package com.fastasyncworldedit.core.queue.implementation.chunk;

import com.fastasyncworldedit.core.extent.filter.block.ChunkFilterBlock;
import com.fastasyncworldedit.core.extent.processor.EmptyBatchProcessor;
import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.IQueueChunk;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.fastasyncworldedit.core.queue.implementation.ParallelQueueExtent;
import com.fastasyncworldedit.core.util.MemUtil;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An abstract {@link IChunk} class that implements basic get/set blocks.
 */
@SuppressWarnings("rawtypes")
public class ChunkHolder<T extends Future<T>> implements IQueueChunk<T> {
    private static final Logger LOGGER = LogManagerCompat.getLogger();

    public static ChunkHolder newInstance() {
        return new ChunkHolder();
    }

    private volatile IChunkGet chunkExisting; // The existing chunk (e.g. a clipboard, or the world, before changes)
    private volatile IChunkSet chunkSet; // The blocks to be set to the chunkExisting
    private IBlockDelegate delegate; // delegate handles the abstraction of the chunk layers
    private IQueueExtent<? extends IChunk> extent; // the parent queue extent which has this chunk
    private int chunkX;
    private int chunkZ;
    private boolean fastmode;
    private int bitMask = -1; // Allow forceful setting of bitmask (for lighting)
    private boolean isInit = false; // Lighting handles queue differently. It relies on the chunk cache and not doing init.
    private boolean createCopy = false;
    private long initTime = -1L;

    private ChunkHolder() {
        this.delegate = NULL;
    }

    public void init(IBlockDelegate delegate) {
        this.delegate = delegate;
    }

    private static final AtomicBoolean recycleWarning = new AtomicBoolean(false);
    @Override
    public void recycle() {
        if (!recycleWarning.getAndSet(true)) {
            LOGGER.warn("ChunkHolder should not be recycled.", new Exception());
        }
    }

    public long initAge() {
        return System.currentTimeMillis() - initTime;
    }

    public synchronized IBlockDelegate getDelegate() {
        return delegate;
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        return delegate.set(this).setTile(x, y, z, tag);
    }

    @Override
    public CompoundTag getTile(int x, int y, int z) {
        return delegate.set(this).getTile(x, y, z);
    }

    @Override
    public void setEntity(CompoundTag tag) {
        delegate.set(this).setEntity(tag);
    }

    @Override
    public void removeEntity(UUID uuid) {
        delegate.set(this).removeEntity(uuid);
    }

    @Override
    public Set<UUID> getEntityRemoves() {
        return delegate.set(this).getEntityRemoves();
    }

    @Override
    public BiomeType[][] getBiomes() {
        // Uses set as this method is only used to retrieve biomes that have been set to the extent/chunk.
        return delegate.set(this).getBiomes();
    }

    @Override
    public char[][] getLight() {
        return delegate.set(this).getLight();
    }

    @Override
    public char[][] getSkyLight() {
        return delegate.set(this).getSkyLight();
    }

    @Override
    public void setBlocks(int layer, char[] data) {
        delegate.set(this).setBlocks(layer, data);
    }

    @Override
    public char[] load(int layer) {
        return getOrCreateGet().load(layer);
    }

    @Nullable
    @Override
    public char[] loadIfPresent(final int layer) {
        if (chunkExisting == null) {
            return null;
        }
        return chunkExisting.loadIfPresent(layer);
    }

    @Override
    public boolean isFastMode() {
        return fastmode;
    }

    @Override
    public void setFastMode(boolean fastmode) {
        this.fastmode = fastmode;
    }

    public void setBitMask(int bitMask) {
        this.bitMask = bitMask;
    }

    public int getBitMask() {
        return bitMask;
    }

    @Override
    public boolean hasBiomes(final int layer) {
        // No need to go through delegate. hasBiomes is SET only.
        return chunkSet != null && chunkSet.hasBiomes(layer);
    }

    public boolean isInit() {
        return isInit;
    }

    @Override
    public CompoundTag getEntity(UUID uuid) {
        return delegate.get(this).getEntity(uuid);
    }

    @Override
    public int setCreateCopy(boolean createCopy) {
        this.createCopy = createCopy;
        return -1;
    }

    @Override
    public boolean isCreateCopy() {
        return createCopy;
    }

    @Override
    public void setLightingToGet(char[][] lighting, int minSectionPosition, int maxSectionPosition) {
        delegate.setLightingToGet(this, lighting);
    }

    @Override
    public void setSkyLightingToGet(char[][] lighting, int minSectionPosition, int maxSectionPosition) {
        delegate.setSkyLightingToGet(this, lighting);
    }

    @Override
    public void setHeightmapToGet(HeightMapType type, int[] data) {
        delegate.setHeightmapToGet(this, type, data);
    }

    @Override
    public int getMaxY() {
        return getOrCreateGet().getMaxY();
    }

    @Override
    public int getMinY() {
        return getOrCreateGet().getMinY();
    }

    @Override
    public int getMaxSectionPosition() {
        return getOrCreateGet().getMaxSectionPosition();
    }

    @Override
    public int getMinSectionPosition() {
        return getOrCreateGet().getMinSectionPosition();
    }

    public void flushLightToGet() {
        delegate.flushLightToGet(this);
    }

    private static final IBlockDelegate BOTH = new IBlockDelegate() {
        @Override
        public IChunkGet get(ChunkHolder chunk) {
            return chunk.chunkExisting;
        }

        @Override
        public IChunkSet set(ChunkHolder chunk) {
            return chunk.chunkSet;
        }

        @Override
        public boolean setBiome(
                ChunkHolder chunk, int x, int y, int z,
                BiomeType biome
        ) {
            return chunk.chunkSet.setBiome(x, y, z, biome);
        }

        @Override
        public <B extends BlockStateHolder<B>> boolean setBlock(
                ChunkHolder chunk, int x, int y, int z,
                B block
        ) {
            return chunk.chunkSet.setBlock(x, y, z, block);
        }

        @Override
        public void setSkyLight(ChunkHolder chunk, int x, int y, int z, int value) {
            chunk.chunkSet.setSkyLight(x, y, z, value);
        }

        @Override
        public void setBlockLight(ChunkHolder chunk, int x, int y, int z, int value) {
            chunk.chunkSet.setBlockLight(x, y, z, value);
        }

        @Override
        public void removeSectionLighting(ChunkHolder chunk, int layer, boolean sky) {
            chunk.chunkSet.removeSectionLighting(layer, sky);
            chunk.chunkExisting.removeSectionLighting(layer, sky);
        }

        @Override
        public void setFullBright(ChunkHolder chunk, int layer) {
            chunk.chunkSet.setFullBright(layer);
        }

        @Override
        public void setLightLayer(ChunkHolder chunk, int layer, char[] toSet) {
            chunk.chunkSet.setLightLayer(layer, toSet);
        }

        @Override
        public void setSkyLightLayer(ChunkHolder chunk, int layer, char[] toSet) {
            chunk.chunkSet.setSkyLightLayer(layer, toSet);
        }

        @Override
        public void setHeightMap(ChunkHolder chunk, HeightMapType type, int[] heightMap) {
            chunk.chunkSet.setHeightMap(type, heightMap);
        }

        @Override
        public BiomeType getBiome(ChunkHolder chunk, int x, int y, int z) {
            return chunk.chunkExisting.getBiomeType(x, y, z);
        }

        @Override
        public BlockState getBlock(ChunkHolder chunk, int x, int y, int z) {
            return chunk.chunkExisting.getBlock(x, y, z);
        }

        @Override
        public BaseBlock getFullBlock(ChunkHolder chunk, int x, int y, int z) {
            return chunk.chunkExisting.getFullBlock(x, y, z);
        }

        @Override
        public int getSkyLight(ChunkHolder chunk, int x, int y, int z) {
            if (chunk.chunkSet.getSkyLight() != null) {
                int layer = y >> 4;
                layer -= chunk.chunkSet.getMinSectionPosition();
                if (layer >= 0 && layer < chunk.chunkSet.getSectionCount()) {
                    if (chunk.chunkSet.getSkyLight()[layer] != null) {
                        int setLightValue = chunk.chunkSet.getSkyLight()[layer][(y & 15) << 8 | (z & 15) << 4 | (x & 15)];
                        if (setLightValue < 16) {
                            return setLightValue;
                        }
                    }
                }
            }
            return chunk.chunkExisting.getSkyLight(x, y, z);
        }

        @Override
        public int getEmittedLight(ChunkHolder chunk, int x, int y, int z) {
            if (chunk.chunkSet.getLight() != null) {
                int layer = y >> 4;
                layer -= chunk.chunkSet.getMinSectionPosition();
                if (layer >= 0 && layer < chunk.chunkSet.getSectionCount()) {
                    if (chunk.chunkSet.getLight()[layer] != null) {
                        int setLightValue = chunk.chunkSet.getLight()[layer][(y & 15) << 8 | (z & 15) << 4 | (x & 15)];
                        if (setLightValue < 16) {
                            return setLightValue;
                        }
                    }
                }
            }
            return chunk.chunkExisting.getEmittedLight(x, y, z);
        }

        @Override
        public int getBrightness(ChunkHolder chunk, int x, int y, int z) {
            return chunk.chunkExisting.getBrightness(x, y, z);
        }

        @Override
        public int getOpacity(ChunkHolder chunk, int x, int y, int z) {
            return chunk.chunkExisting.getOpacity(x, y, z);
        }

        @Override
        public int[] getHeightMap(ChunkHolder chunk, HeightMapType type) {
            return chunk.chunkExisting.getHeightMap(type);
        }

        @Override
        public void flushLightToGet(ChunkHolder chunk) {
            chunk.chunkExisting.setLightingToGet(chunk.chunkSet.getLight(), chunk.chunkSet.getMinSectionPosition(),
                    chunk.chunkSet.getMaxSectionPosition()
            );
            chunk.chunkExisting.setSkyLightingToGet(chunk.chunkSet.getSkyLight(), chunk.chunkSet.getMinSectionPosition(),
                    chunk.chunkSet.getMaxSectionPosition()
            );
        }

        @Override
        public void setLightingToGet(ChunkHolder chunk, char[][] lighting) {
            chunk.chunkExisting.setLightingToGet(
                    lighting,
                    chunk.chunkSet.getMinSectionPosition(),
                    chunk.chunkSet.getMaxSectionPosition()
            );
        }

        @Override
        public void setSkyLightingToGet(ChunkHolder chunk, char[][] lighting) {
            chunk.chunkExisting.setSkyLightingToGet(
                    lighting,
                    chunk.chunkSet.getMinSectionPosition(),
                    chunk.chunkSet.getMaxSectionPosition()
            );
        }

        @Override
        public void setHeightmapToGet(ChunkHolder chunk, HeightMapType type, int[] data) {
            chunk.chunkExisting.setHeightmapToGet(type, data);
        }
    };

    private static final IBlockDelegate GET = new IBlockDelegate() {
        @Override
        public IChunkGet get(ChunkHolder chunk) {
            return chunk.chunkExisting;
        }

        @Override
        public IChunkSet set(ChunkHolder chunk) {
            chunk.getOrCreateSet();
            chunk.delegate = BOTH;
            return chunk.chunkSet;
        }

        @Override
        public boolean setBiome(
                ChunkHolder chunk, int x, int y, int z,
                BiomeType biome
        ) {
            chunk.getOrCreateSet();
            chunk.delegate = BOTH;
            return chunk.setBiome(x, y, z, biome);
        }

        @Override
        public <B extends BlockStateHolder<B>> boolean setBlock(
                ChunkHolder chunk, int x, int y, int z,
                B block
        ) {
            chunk.getOrCreateSet();
            chunk.delegate = BOTH;
            return chunk.setBlock(x, y, z, block);
        }

        @Override
        public void setSkyLight(ChunkHolder chunk, int x, int y, int z, int value) {
            chunk.getOrCreateSet();
            chunk.delegate = BOTH;
            chunk.setSkyLight(x, y, z, value);
        }

        @Override
        public void setBlockLight(ChunkHolder chunk, int x, int y, int z, int value) {
            chunk.getOrCreateSet();
            chunk.delegate = BOTH;
            chunk.setBlockLight(x, y, z, value);
        }

        @Override
        public void removeSectionLighting(ChunkHolder chunk, int layer, boolean sky) {
            chunk.getOrCreateSet();
            chunk.delegate = BOTH;
            chunk.removeSectionLighting(layer, sky);
        }

        @Override
        public void setFullBright(ChunkHolder chunk, int layer) {
            chunk.getOrCreateSet();
            chunk.delegate = BOTH;
            chunk.setFullBright(layer);
        }

        @Override
        public void setLightLayer(ChunkHolder chunk, int layer, char[] toSet) {
            chunk.getOrCreateSet();
            chunk.delegate = BOTH;
            chunk.setLightLayer(layer, toSet);
        }

        @Override
        public void setSkyLightLayer(ChunkHolder chunk, int layer, char[] toSet) {
            chunk.getOrCreateSet();
            chunk.delegate = BOTH;
            chunk.setSkyLightLayer(layer, toSet);
        }

        @Override
        public void setHeightMap(ChunkHolder chunk, HeightMapType type, int[] heightMap) {
            chunk.getOrCreateSet();
            chunk.delegate = BOTH;
            chunk.setHeightMap(type, heightMap);
        }

        @Override
        public BiomeType getBiome(ChunkHolder chunk, int x, int y, int z) {
            return chunk.chunkExisting.getBiomeType(x, y, z);
        }

        @Override
        public BlockState getBlock(ChunkHolder chunk, int x, int y, int z) {
            return chunk.chunkExisting.getBlock(x, y, z);
        }

        @Override
        public BaseBlock getFullBlock(
                ChunkHolder chunk, int x, int y,
                int z
        ) {
            return chunk.chunkExisting.getFullBlock(x, y, z);
        }

        @Override
        public int getSkyLight(ChunkHolder chunk, int x, int y, int z) {
            return chunk.chunkExisting.getSkyLight(x, y, z);
        }

        @Override
        public int getEmittedLight(ChunkHolder chunk, int x, int y, int z) {
            return chunk.chunkExisting.getEmittedLight(x, y, z);
        }

        @Override
        public int getBrightness(ChunkHolder chunk, int x, int y, int z) {
            return chunk.chunkExisting.getBrightness(x, y, z);
        }

        @Override
        public int getOpacity(ChunkHolder chunk, int x, int y, int z) {
            return chunk.chunkExisting.getOpacity(x, y, z);
        }

        @Override
        public int[] getHeightMap(ChunkHolder chunk, HeightMapType type) {
            return chunk.chunkExisting.getHeightMap(type);
        }

        @Override
        public void flushLightToGet(ChunkHolder chunk) {
            // Do nothing as no lighting to flush to GET
        }

        @Override
        public void setLightingToGet(ChunkHolder chunk, char[][] lighting) {
            chunk.chunkExisting.setLightingToGet(
                    lighting,
                    chunk.chunkSet.getMinSectionPosition(),
                    chunk.chunkSet.getMaxSectionPosition()
            );
        }

        @Override
        public void setSkyLightingToGet(ChunkHolder chunk, char[][] lighting) {
            chunk.chunkExisting.setSkyLightingToGet(
                    lighting,
                    chunk.chunkSet.getMinSectionPosition(),
                    chunk.chunkSet.getMaxSectionPosition()
            );
        }

        @Override
        public void setHeightmapToGet(ChunkHolder chunk, HeightMapType type, int[] data) {
            chunk.chunkExisting.setHeightmapToGet(type, data);
        }
    };

    private static final IBlockDelegate SET = new IBlockDelegate() {
        @Override
        public IChunkGet get(ChunkHolder chunk) {
            chunk.getOrCreateGet();
            chunk.delegate = BOTH;
            return chunk.chunkExisting;
        }

        @Override
        public IChunkSet set(ChunkHolder chunk) {
            return chunk.chunkSet;
        }

        @Override
        public boolean setBiome(
                ChunkHolder chunk, int x, int y, int z,
                BiomeType biome
        ) {
            return chunk.chunkSet.setBiome(x, y, z, biome);
        }

        @Override
        public <B extends BlockStateHolder<B>> boolean setBlock(
                ChunkHolder chunk,
                int x,
                int y,
                int z,
                B block
        ) {
            return chunk.chunkSet.setBlock(x, y, z, block);
        }

        @Override
        public void setSkyLight(ChunkHolder chunk, int x, int y, int z, int value) {
            chunk.chunkSet.setSkyLight(x, y, z, value);
        }

        @Override
        public void setBlockLight(ChunkHolder chunk, int x, int y, int z, int value) {
            chunk.chunkSet.setBlockLight(x, y, z, value);
        }

        @Override
        public void removeSectionLighting(ChunkHolder chunk, int layer, boolean sky) {
            chunk.getOrCreateGet();
            chunk.delegate = BOTH;
            chunk.removeSectionLighting(layer, sky);
        }

        @Override
        public void setFullBright(ChunkHolder chunk, int layer) {
            chunk.chunkSet.setFullBright(layer);
        }

        @Override
        public void setLightLayer(ChunkHolder chunk, int layer, char[] toSet) {
            chunk.chunkSet.setLightLayer(layer, toSet);
        }

        @Override
        public void setSkyLightLayer(ChunkHolder chunk, int layer, char[] toSet) {
            chunk.chunkSet.setSkyLightLayer(layer, toSet);
        }

        @Override
        public void setHeightMap(ChunkHolder chunk, HeightMapType type, int[] heightMap) {
            chunk.chunkSet.setHeightMap(type, heightMap);
        }

        @Override
        public BiomeType getBiome(ChunkHolder chunk, int x, int y, int z) {
            chunk.getOrCreateGet();
            chunk.delegate = BOTH;
            return chunk.getBiomeType(x, y, z);
        }

        @Override
        public BlockState getBlock(ChunkHolder chunk, int x, int y, int z) {
            chunk.getOrCreateGet();
            chunk.delegate = BOTH;
            return chunk.getBlock(x, y, z);
        }

        @Override
        public BaseBlock getFullBlock(
                ChunkHolder chunk, int x, int y,
                int z
        ) {
            chunk.getOrCreateGet();
            chunk.delegate = BOTH;
            return chunk.getFullBlock(x, y, z);
        }

        @Override
        public int getSkyLight(ChunkHolder chunk, int x, int y, int z) {
            if (chunk.chunkSet.getSkyLight() != null) {
                int layer = y >> 4;
                layer -= chunk.chunkSet.getMinSectionPosition();
                if (layer >= 0 && layer < chunk.chunkSet.getSectionCount()) {
                    if (chunk.chunkSet.getSkyLight()[layer] != null) {
                        int setLightValue = chunk.chunkSet.getSkyLight()[layer][(y & 15) << 8 | (z & 15) << 4 | (x & 15)];
                        if (setLightValue < 16) {
                            return setLightValue;
                        }
                    }
                }
            }
            chunk.getOrCreateGet();
            chunk.delegate = BOTH;
            return chunk.getSkyLight(x, y, z);
        }

        @Override
        public int getEmittedLight(ChunkHolder chunk, int x, int y, int z) {
            if (chunk.chunkSet.getLight() != null) {
                int layer = y >> 4;
                layer -= chunk.chunkSet.getMinSectionPosition();
                if (layer >= 0 && layer < chunk.chunkSet.getSectionCount()) {
                    if (chunk.chunkSet.getLight()[layer] != null) {
                        int setLightValue = chunk.chunkSet.getLight()[layer][(y & 15) << 8 | (z & 15) << 4 | (x & 15)];
                        if (setLightValue < 16) {
                            return setLightValue;
                        }
                    }
                }
            }
            chunk.getOrCreateGet();
            chunk.delegate = BOTH;
            return chunk.getEmittedLight(x, y, z);
        }

        @Override
        public int getBrightness(ChunkHolder chunk, int x, int y, int z) {
            chunk.getOrCreateGet();
            chunk.delegate = BOTH;
            return chunk.getBrightness(x, y, z);
        }

        @Override
        public int getOpacity(ChunkHolder chunk, int x, int y, int z) {
            chunk.getOrCreateGet();
            chunk.delegate = BOTH;
            return chunk.getOpacity(x, y, z);
        }

        @Override
        public int[] getHeightMap(ChunkHolder chunk, HeightMapType type) {
            chunk.getOrCreateGet();
            chunk.delegate = BOTH;
            return chunk.getHeightMap(type);
        }

        @Override
        public void flushLightToGet(ChunkHolder chunk) {
            chunk.getOrCreateGet();
            chunk.delegate = BOTH;
            chunk.flushLightToGet();
        }

        @Override
        public void setLightingToGet(ChunkHolder chunk, char[][] lighting) {
            chunk.getOrCreateGet();
            chunk.delegate = BOTH;
            chunk.setLightingToGet(lighting, chunk.chunkSet.getMinSectionPosition(), chunk.chunkSet.getMaxSectionPosition());
        }

        @Override
        public void setSkyLightingToGet(ChunkHolder chunk, char[][] lighting) {
            chunk.getOrCreateGet();
            chunk.delegate = BOTH;
            chunk.setSkyLightingToGet(lighting, chunk.chunkSet.getMinSectionPosition(), chunk.chunkSet.getMaxSectionPosition());
        }

        @Override
        public void setHeightmapToGet(ChunkHolder chunk, HeightMapType type, int[] data) {
            chunk.getOrCreateGet();
            chunk.delegate = BOTH;
            chunk.setHeightmapToGet(type, data);
        }
    };

    private static final IBlockDelegate NULL = new IBlockDelegate() {
        @Override
        public IChunkGet get(ChunkHolder chunk) {
            chunk.getOrCreateGet();
            chunk.delegate = GET;
            return chunk.chunkExisting;
        }

        @Override
        public IChunkSet set(ChunkHolder chunk) {
            chunk.getOrCreateSet();
            chunk.delegate = SET;
            return chunk.chunkSet;
        }

        @Override
        public boolean setBiome(ChunkHolder chunk, int x, int y, int z, BiomeType biome) {
            chunk.getOrCreateSet();
            chunk.delegate = SET;
            return chunk.setBiome(x, y, z, biome);
        }

        @Override
        public <B extends BlockStateHolder<B>> boolean setBlock(ChunkHolder chunk, int x, int y, int z, B block) {
            chunk.getOrCreateSet();
            chunk.delegate = SET;
            return chunk.setBlock(x, y, z, block);
        }

        @Override
        public BiomeType getBiome(ChunkHolder chunk, int x, int y, int z) {
            chunk.getOrCreateGet();
            chunk.delegate = GET;
            return chunk.getBiomeType(x, y, z);
        }

        @Override
        public BlockState getBlock(ChunkHolder chunk, int x, int y, int z) {
            chunk.getOrCreateGet();
            chunk.delegate = GET;
            return chunk.getBlock(x, y, z);
        }

        @Override
        public BaseBlock getFullBlock(
                ChunkHolder chunk, int x, int y,
                int z
        ) {
            chunk.getOrCreateGet();
            chunk.delegate = GET;
            return chunk.getFullBlock(x, y, z);
        }

        @Override
        public void setSkyLight(ChunkHolder chunk, int x, int y, int z, int value) {
            chunk.getOrCreateSet();
            chunk.delegate = SET;
            chunk.setSkyLight(x, y, z, value);
        }

        @Override
        public void setBlockLight(ChunkHolder chunk, int x, int y, int z, int value) {
            chunk.getOrCreateSet();
            chunk.delegate = SET;
            chunk.setBlockLight(x, y, z, value);
        }

        @Override
        public void removeSectionLighting(ChunkHolder chunk, int layer, boolean sky) {
            chunk.getOrCreateGet();
            chunk.getOrCreateSet();
            chunk.delegate = BOTH;
            chunk.removeSectionLighting(layer, sky);
        }

        @Override
        public void setFullBright(ChunkHolder chunk, int layer) {
            chunk.getOrCreateSet();
            chunk.delegate = SET;
            chunk.setFullBright(layer);
        }

        @Override
        public void setLightLayer(ChunkHolder chunk, int layer, char[] toSet) {
            chunk.getOrCreateSet();
            chunk.delegate = SET;
            chunk.setLightLayer(layer, toSet);
        }

        @Override
        public void setSkyLightLayer(ChunkHolder chunk, int layer, char[] toSet) {
            chunk.getOrCreateSet();
            chunk.delegate = SET;
            chunk.setSkyLightLayer(layer, toSet);
        }

        @Override
        public void setHeightMap(ChunkHolder chunk, HeightMapType type, int[] heightMap) {
            chunk.getOrCreateSet();
            chunk.delegate = SET;
            chunk.setHeightMap(type, heightMap);
        }

        @Override
        public int getSkyLight(ChunkHolder chunk, int x, int y, int z) {
            chunk.getOrCreateGet();
            chunk.delegate = GET;
            return chunk.getSkyLight(x, y, z);
        }

        @Override
        public int getEmittedLight(ChunkHolder chunk, int x, int y, int z) {
            chunk.getOrCreateGet();
            chunk.delegate = GET;
            return chunk.getEmittedLight(x, y, z);
        }

        @Override
        public int getBrightness(ChunkHolder chunk, int x, int y, int z) {
            chunk.getOrCreateGet();
            chunk.delegate = GET;
            return chunk.getBrightness(x, y, z);
        }

        @Override
        public int getOpacity(ChunkHolder chunk, int x, int y, int z) {
            chunk.getOrCreateGet();
            chunk.delegate = GET;
            return chunk.getOpacity(x, y, z);
        }

        @Override
        public int[] getHeightMap(ChunkHolder chunk, HeightMapType type) {
            chunk.getOrCreateGet();
            chunk.delegate = GET;
            return chunk.getHeightMap(type);
        }

        @Override
        public void flushLightToGet(ChunkHolder chunk) {
            // Do nothing as no light to flush
        }

        @Override
        public void setLightingToGet(ChunkHolder chunk, char[][] lighting) {
            chunk.getOrCreateGet();
            chunk.delegate = GET;
            chunk.setLightingToGet(lighting, chunk.chunkSet.getMinSectionPosition(), chunk.chunkSet.getMaxSectionPosition());
        }

        @Override
        public void setSkyLightingToGet(ChunkHolder chunk, char[][] lighting) {
            chunk.getOrCreateGet();
            chunk.delegate = GET;
            chunk.setSkyLightingToGet(lighting, chunk.chunkSet.getMinSectionPosition(), chunk.chunkSet.getMaxSectionPosition());
        }

        @Override
        public void setHeightmapToGet(ChunkHolder chunk, HeightMapType type, int[] data) {
            chunk.getOrCreateGet();
            chunk.delegate = GET;
            chunk.setHeightmapToGet(type, data);
        }
    };

    @Override
    public Map<BlockVector3, CompoundTag> getTiles() {
        return delegate.get(this).getTiles();
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return delegate.get(this).getEntities();
    }

    @Override
    public boolean hasSection(int layer) {
        return chunkExisting != null && chunkExisting.hasSection(layer);
    }

    @Override
    public synchronized void filterBlocks(Filter filter, ChunkFilterBlock block, @Nullable Region region, boolean full) {
        final IChunkGet get = getOrCreateGet();
        final IChunkSet set = getOrCreateSet();
        set.setFastMode(fastmode);
        try {
            block.filter(this, get, set, filter, region, full);
        } finally {
            filter.finishChunk(this);
        }
    }

    @Override
    public synchronized boolean trim(boolean aggressive) {
        // always trim GET. It could be cached elsewhere.
        chunkExisting.trim(aggressive);
        if (chunkSet != null) {
            final boolean result = chunkSet.trim(aggressive);
            if (result) {
                delegate = NULL;
                chunkExisting = null;
                chunkSet.recycle();
                chunkSet = null;
                return true;
            }
        }
        if (aggressive) {
            chunkExisting = null;
            if (delegate == BOTH) {
                delegate = SET;
            } else if (delegate == GET) {
                delegate = NULL;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean trim(boolean aggressive, int layer) {
        return this.trim(aggressive);
    }

    @Override
    public int getSectionCount() {
        return getOrCreateGet().getSectionCount();
    }

    @Override
    public boolean isEmpty() {
        return chunkSet == null || chunkSet.isEmpty();
    }

    /**
     * Get or create the existing part of this chunk.
     */
    public final IChunkGet getOrCreateGet() {
        if (chunkExisting == null) {
            chunkExisting = newWrappedGet();
            chunkExisting.trim(MemUtil.isMemoryLimited());
        }
        return chunkExisting;
    }

    /**
     * Get or create the settable part of this chunk.
     */
    public final IChunkSet getOrCreateSet() {
        if (chunkSet == null) {
            chunkSet = newWrappedSet();
        }
        return chunkSet;
    }

    /**
     * Create a wrapped set object
     * - The purpose of wrapping is to allow different extents to intercept / alter behavior
     * - e.g., caching, optimizations, filtering
     */
    private IChunkSet newWrappedSet() {
        return extent.getCachedSet(chunkX, chunkZ);
    }

    /**
     * Create a wrapped get object
     * - The purpose of wrapping is to allow different extents to intercept / alter behavior
     * - e.g., caching, optimizations, filtering
     */
    private IChunkGet newWrappedGet() {
        return extent.getCachedGet(chunkX, chunkZ);
    }

    @Override
    public synchronized <V extends IChunk> void init(IQueueExtent<V> extent, int chunkX, int chunkZ) {
        this.initTime = System.currentTimeMillis();
        this.extent = extent;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        if (chunkSet != null) {
            chunkSet.reset();
            delegate = SET;
        } else {
            delegate = NULL;
        }
        chunkExisting = null;
        isInit = true;
    }

    @Override
    public synchronized T call() {
        if (chunkSet != null && !chunkSet.isEmpty()) {
            chunkSet.setBitMask(bitMask);
            IChunkSet copy = chunkSet.createCopy();
            return this.call(copy, () -> {
                // Do nothing
            });
        }
        return null;
    }

    /**
     * This method should never be called from outside ChunkHolder
     */
    @Override
    public synchronized T call(IChunkSet set, Runnable finalize) {
        if (set != null) {
            IChunkGet get = getOrCreateGet();
            try {
                get.lockCall();
                trackExtent();
                boolean postProcess = !(getExtent().getPostProcessor() instanceof EmptyBatchProcessor);
                final int copyKey = get.setCreateCopy(postProcess);
                final IChunkSet iChunkSet = getExtent().processSet(this, get, set);
                Runnable finalizer;
                if (postProcess) {
                    finalizer = () -> {
                        getExtent().postProcess(this, get.getCopy(copyKey), iChunkSet);
                        finalize.run();
                    };
                } else {
                    finalizer = finalize;
                }
                return get.call(set, finalizer);
            } finally {
                get.unlockCall();
                untrackExtent();
            }
        }
        return null;
    }

    // "call" can be called by QueueHandler#blockingExecutor. In such case, we still want the other thread
    // to use this SingleThreadQueueExtent. Otherwise, many threads might end up locking on **one** STQE.
    // This way, locking is spread across multiple STQEs, allowing for better performance

    private void trackExtent() {
            ParallelQueueExtent.setCurrentExtent(extent);
    }

    private void untrackExtent() {
        ParallelQueueExtent.clearCurrentExtent();
    }

    /**
     * Get the extent this chunk is in.
     */
    public IQueueExtent<? extends IChunk> getExtent() {
        return extent;
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
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return delegate.setBiome(this, x, y, z, biome);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) {
        return delegate.setBlock(this, x, y, z, block);
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        return delegate.getBiome(this, x, y, z);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return delegate.getBlock(this, x, y, z);
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return delegate.getFullBlock(this, x, y, z);
    }

    @Override
    public void setSkyLight(int x, int y, int z, int value) {
        delegate.setSkyLight(this, x, y, z, value);
    }

    @Override
    public void setHeightMap(HeightMapType type, int[] heightMap) {
        delegate.setHeightMap(this, type, heightMap);
    }

    @Override
    public void removeSectionLighting(int layer, boolean sky) {
        delegate.removeSectionLighting(this, layer, sky);
    }

    @Override
    public void setFullBright(int layer) {
        delegate.setFullBright(this, layer);
    }

    @Override
    public void setBlockLight(int x, int y, int z, int value) {
        delegate.setBlockLight(this, x, y, z, value);
    }

    @Override
    public void setLightLayer(int layer, char[] toSet) {
        delegate.setLightLayer(this, layer, toSet);
    }

    @Override
    public void setSkyLightLayer(int layer, char[] toSet) {
        delegate.setSkyLightLayer(this, layer, toSet);
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        return delegate.getSkyLight(this, x, y, z);
    }

    @Override
    public int getEmittedLight(int x, int y, int z) {
        return delegate.getEmittedLight(this, x, y, z);
    }

    @Override
    public int getBrightness(int x, int y, int z) {
        return delegate.getBrightness(this, x, y, z);
    }

    @Override
    public int getOpacity(int x, int y, int z) {
        return delegate.getOpacity(this, x, y, z);
    }

    @Override
    public int[] getHeightMap(HeightMapType type) {
        return delegate.getHeightMap(this, type);
    }

    public interface IBlockDelegate {

        <C extends Future<C>> IChunkGet get(ChunkHolder<C> chunk);

        IChunkSet set(ChunkHolder chunk);

        boolean setBiome(ChunkHolder chunk, int x, int y, int z, BiomeType biome);

        <T extends BlockStateHolder<T>> boolean setBlock(ChunkHolder chunk, int x, int y, int z, T holder);

        BiomeType getBiome(ChunkHolder chunk, int x, int y, int z);

        BlockState getBlock(ChunkHolder chunk, int x, int y, int z);

        BaseBlock getFullBlock(ChunkHolder chunk, int x, int y, int z);

        void setSkyLight(ChunkHolder chunk, int x, int y, int z, int value);

        void setBlockLight(ChunkHolder chunk, int x, int y, int z, int value);

        void removeSectionLighting(ChunkHolder chunk, int layer, boolean sky);

        void setFullBright(ChunkHolder chunk, int layer);

        void setLightLayer(ChunkHolder chunk, int layer, char[] toSet);

        void setSkyLightLayer(ChunkHolder chunk, int layer, char[] toSet);

        void setHeightMap(ChunkHolder chunk, HeightMapType type, int[] heightMap);

        int getSkyLight(ChunkHolder chunk, int x, int y, int z);

        int getEmittedLight(ChunkHolder chunk, int x, int y, int z);

        int getBrightness(ChunkHolder chunk, int x, int y, int z);

        int getOpacity(ChunkHolder chunk, int x, int y, int z);

        int[] getHeightMap(ChunkHolder chunk, HeightMapType type);

        void flushLightToGet(ChunkHolder chunk);

        void setLightingToGet(ChunkHolder chunk, char[][] lighting);

        void setSkyLightingToGet(ChunkHolder chunk, char[][] lighting);

        void setHeightmapToGet(ChunkHolder chunk, HeightMapType type, int[] data);

    }

}
