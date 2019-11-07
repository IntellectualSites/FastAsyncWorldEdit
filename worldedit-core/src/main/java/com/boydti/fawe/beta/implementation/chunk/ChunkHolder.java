package com.boydti.fawe.beta.implementation.chunk;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IQueueChunk;
import com.boydti.fawe.beta.implementation.filter.block.ChunkFilterBlock;
import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.config.Settings;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import javax.annotation.Nullable;

/**
 * An abstract {@link IChunk} class that implements basic get/set blocks
 */
public class ChunkHolder<T extends Future<T>> implements IQueueChunk {

    private static FaweCache.Pool<ChunkHolder> POOL = FaweCache.IMP.registerPool(ChunkHolder.class, ChunkHolder::new, Settings.IMP.QUEUE.POOL);

    public static ChunkHolder newInstance() {
        return POOL.poll();
    }

    private IChunkGet chunkExisting; // The existing chunk (e.g. a clipboard, or the world, before changes)
    private IChunkSet chunkSet; // The blocks to be set to the chunkExisting
    private IBlockDelegate delegate; // delegate handles the abstraction of the chunk layers
    private IQueueExtent extent; // the parent queue extent which has this chunk
    private int chunkX;
    private int chunkZ;

    public ChunkHolder() {
        this.delegate = NULL;
    }

    public void init(IBlockDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void recycle() {
        delegate = NULL;
    }

    public IBlockDelegate getDelegate() {
        return delegate;
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        return delegate.set(this).setTile(x, y, z, tag);
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
    public BiomeType[] getBiomes() {
        return delegate.set(this).getBiomes(); // TODO return get?
    }

    @Override
    public void setBlocks(int layer, char[] data) {
        delegate.set(this).setBlocks(layer, data);
    }

    @Override
    public char[] load(int layer) {
        return getOrCreateGet().load(layer);
    }

    @Override
    public CompoundTag getEntity(UUID uuid) {
        return delegate.get(this).getEntity(uuid);
    }

    public static final IBlockDelegate BOTH = new IBlockDelegate() {
        @Override
        public IChunkGet get(ChunkHolder chunk) {
            return chunk.chunkExisting;
        }

        @Override
        public IChunkSet set(ChunkHolder chunk) {
            return chunk.chunkSet;
        }

        @Override
        public boolean setBiome(ChunkHolder chunk, int x, int y, int z,
            BiomeType biome) {
            return chunk.chunkSet.setBiome(x, y, z, biome);
        }

        @Override
        public boolean setBlock(ChunkHolder chunk, int x, int y, int z,
            BlockStateHolder block) {
            return chunk.chunkSet.setBlock(x, y, z, block);
        }

        @Override
        public BiomeType getBiome(ChunkHolder chunk, int x, int z) {
            return chunk.chunkExisting.getBiomeType(x, z);
        }

        @Override
        public BlockState getBlock(ChunkHolder chunk, int x, int y, int z) {
            return chunk.chunkExisting.getBlock(x, y, z);
        }

        @Override
        public BaseBlock getFullBlock(ChunkHolder chunk, int x, int y,
            int z) {
            return chunk.chunkExisting.getFullBlock(x, y, z);
        }
    };
    public static final IBlockDelegate GET = new IBlockDelegate() {
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
        public boolean setBiome(ChunkHolder chunk, int x, int y, int z,
            BiomeType biome) {
            chunk.getOrCreateSet();
            chunk.delegate = BOTH;
            return chunk.setBiome(x, y, z, biome);
        }

        @Override
        public boolean setBlock(ChunkHolder chunk, int x, int y, int z,
            BlockStateHolder block) {
            chunk.getOrCreateSet();
            chunk.delegate = BOTH;
            return chunk.setBlock(x, y, z, block);
        }

        @Override
        public BiomeType getBiome(ChunkHolder chunk, int x, int z) {
            return chunk.chunkExisting.getBiomeType(x, z);
        }

        @Override
        public BlockState getBlock(ChunkHolder chunk, int x, int y, int z) {
            return chunk.chunkExisting.getBlock(x, y, z);
        }

        @Override
        public BaseBlock getFullBlock(ChunkHolder chunk, int x, int y,
            int z) {
            return chunk.chunkExisting.getFullBlock(x, y, z);
        }
    };
    public static final IBlockDelegate SET = new IBlockDelegate() {
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
        public boolean setBiome(ChunkHolder chunk, int x, int y, int z,
            BiomeType biome) {
            return chunk.chunkSet.setBiome(x, y, z, biome);
        }

        @Override
        public boolean setBlock(ChunkHolder chunk, int x, int y, int z,
            BlockStateHolder block) {
            return chunk.chunkSet.setBlock(x, y, z, block);
        }

        @Override
        public BiomeType getBiome(ChunkHolder chunk, int x, int z) {
            chunk.getOrCreateGet();
            chunk.delegate = BOTH;
            return chunk.getBiomeType(x, z);
        }

        @Override
        public BlockState getBlock(ChunkHolder chunk, int x, int y, int z) {
            chunk.getOrCreateGet();
            chunk.delegate = BOTH;
            return chunk.getBlock(x, y, z);
        }

        @Override
        public BaseBlock getFullBlock(ChunkHolder chunk, int x, int y,
            int z) {
            chunk.getOrCreateGet();
            chunk.delegate = BOTH;
            return chunk.getFullBlock(x, y, z);
        }
    };
    public static final IBlockDelegate NULL = new IBlockDelegate() {
        @Override
        public IChunkGet get(ChunkHolder chunk) {
            chunk.getOrCreateGet();
            chunk.delegate = BOTH;
            return chunk.chunkExisting;
        }

        @Override
        public IChunkSet set(ChunkHolder chunk) {
            chunk.getOrCreateSet();
            chunk.delegate = BOTH;
            return chunk.chunkSet;
        }

        @Override
        public boolean setBiome(ChunkHolder chunk, int x, int y, int z,
            BiomeType biome) {
            chunk.getOrCreateSet();
            chunk.delegate = SET;
            return chunk.setBiome(x, y, z, biome);
        }

        @Override
        public boolean setBlock(ChunkHolder chunk, int x, int y, int z,
            BlockStateHolder block) {
            chunk.getOrCreateSet();
            chunk.delegate = SET;
            return chunk.setBlock(x, y, z, block);
        }

        @Override
        public BiomeType getBiome(ChunkHolder chunk, int x, int z) {
            chunk.getOrCreateGet();
            chunk.delegate = GET;
            return chunk.getBiomeType(x, z);
        }

        @Override
        public BlockState getBlock(ChunkHolder chunk, int x, int y, int z) {
            chunk.getOrCreateGet();
            chunk.delegate = GET;
            return chunk.getBlock(x, y, z);
        }

        @Override
        public BaseBlock getFullBlock(ChunkHolder chunk, int x, int y,
            int z) {
            chunk.getOrCreateGet();
            chunk.delegate = GET;
            return chunk.getFullBlock(x, y, z);
        }
    };

//    @Override
//    public void flood(Flood flood, FilterBlockMask mask, ChunkFilterBlock block) {
////        block.flood(get, set, mask, block, );
//    }

    @Override
    public CompoundTag getTag(int x, int y, int z) {
        return delegate.getFullBlock(this, x, y, z)
            .getNbtData(); // TODO NOT IMPLEMENTED (add getTag delegate)
    }

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
    public void filterBlocks(Filter filter, ChunkFilterBlock block, @Nullable Region region) {
        final IChunkGet get = getOrCreateGet();
        final IChunkSet set = getOrCreateSet();
        try {
            if (region != null) {
                region.filter(this, filter, block, get, set);
            } else {
                block = block.init(chunkX, chunkZ, get);
                for (int layer = 0; layer < 16; layer++) {
                    if (!get.hasSection(layer) || !filter.appliesLayer(this, layer)) {
                        continue;
                    }
                    block.init(get, set, layer);
                    block.filter(filter);
                }
            }
        } finally {
            filter.finishChunk(this);
        }
    }

    @Override
    public boolean trim(boolean aggressive) {
        if (chunkSet != null) {
            final boolean result = chunkSet.trim(aggressive);
            if (result) {
                delegate = NULL;
                chunkExisting = null;
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
        } else {
            chunkExisting.trim(false);
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return chunkSet == null || chunkSet.isEmpty();
    }

    /**
     * Get or create the existing part of this chunk
     * @return
     */
    public final IChunkGet getOrCreateGet() {
        if (chunkExisting == null) {
            chunkExisting = newWrappedGet();
        }
        return chunkExisting;
    }

    /**
     * Get or create the settable part of this chunk
     * @return
     */
    public final IChunkSet getOrCreateSet() {
        if (chunkSet == null) {
            chunkSet = newWrappedSet();
        }
        return chunkSet;
    }

    /**
     * Create a wrapped set object
     *  - The purpose of wrapping is to allow different extents to intercept / alter behavior
     *  - E.g. caching, optimizations, filtering
     * @return
     */
    private IChunkSet newWrappedSet() {
        return extent.getCachedSet(chunkX, chunkZ);
    }

    /**
     * Create a wrapped get object
     *  - The purpose of wrapping is to allow different extents to intercept / alter behavior
     *  - E.g. caching, optimizations, filtering
     * @return
     */
    private IChunkGet newWrappedGet() {
        return extent.getCachedGet(chunkX, chunkZ);
    }

    @Override
    public void init(IQueueExtent extent, int chunkX, int chunkZ) {
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
    }

    @Override
    public synchronized T call() {
        if (chunkSet != null) {
            return this.call(chunkSet, this::recycle);
        }
        return null;
    }

    @Override
    public T call(IChunkSet set, Runnable finalize) {
        if (set != null) {
            IChunkGet get = getOrCreateGet();
            set = getExtent().processSet(this, get, set);
            if (set != null) {
                return get.call(set, finalize);
            }
        }
        return null;
    }

    /**
     * Get the extent this chunk is in
     * @return
     */
    public IQueueExtent getExtent() {
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
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) {
        return delegate.setBlock(this, x, y, z, block);
    }

    @Override
    public BiomeType getBiomeType(int x, int z) {
        return delegate.getBiome(this, x, z);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return delegate.getBlock(this, x, y, z);
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return delegate.getFullBlock(this, x, y, z);
    }

    public interface IBlockDelegate {
        IChunkGet get(ChunkHolder chunk);
        IChunkSet set(ChunkHolder chunk);

        boolean setBiome(ChunkHolder chunk, int x, int y, int z,
            BiomeType biome);

        boolean setBlock(ChunkHolder chunk, int x, int y, int z,
            BlockStateHolder holder);

        BiomeType getBiome(ChunkHolder chunk, int x, int z);

        BlockState getBlock(ChunkHolder chunk, int x, int y, int z);

        BaseBlock getFullBlock(ChunkHolder chunk, int x, int y, int z);
    }
}
