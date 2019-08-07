package com.boydti.fawe.beta.implementation.holder;

import com.boydti.fawe.beta.ChunkFilterBlock;
import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.FilterBlockMask;
import com.boydti.fawe.beta.Flood;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.implementation.SingleThreadQueueExtent;
import com.boydti.fawe.beta.implementation.blocks.CharSetBlocks;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * An abstract {@link IChunk} class that implements basic get/set blocks
 */
public abstract class ChunkHolder implements IChunk {

    public static final IBlockDelegate BOTH = new IBlockDelegate() {
        @Override
        public boolean setBiome(ChunkHolder chunk, int x, int y, int z,
            BiomeType biome) {
            return chunk.set.setBiome(x, y, z, biome);
        }

        @Override
        public boolean setBlock(ChunkHolder chunk, int x, int y, int z,
            BlockStateHolder block) {
            return chunk.set.setBlock(x, y, z, block);
        }

        @Override
        public BiomeType getBiome(ChunkHolder chunk, int x, int z) {
            return chunk.get.getBiomeType(x, z);
        }

        @Override
        public BlockState getBlock(ChunkHolder chunk, int x, int y, int z) {
            return chunk.get.getBlock(x, y, z);
        }

        @Override
        public BaseBlock getFullBlock(ChunkHolder chunk, int x, int y,
            int z) {
            return chunk.get.getFullBlock(x, y, z);
        }
    };
    public static final IBlockDelegate GET = new IBlockDelegate() {
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
            return chunk.get.getBiomeType(x, z);
        }

        @Override
        public BlockState getBlock(ChunkHolder chunk, int x, int y, int z) {
            return chunk.get.getBlock(x, y, z);
        }

        @Override
        public BaseBlock getFullBlock(ChunkHolder chunk, int x, int y,
            int z) {
            return chunk.get.getFullBlock(x, y, z);
        }
    };
    public static final IBlockDelegate SET = new IBlockDelegate() {
        @Override
        public boolean setBiome(ChunkHolder chunk, int x, int y, int z,
            BiomeType biome) {
            return chunk.set.setBiome(x, y, z, biome);
        }

        @Override
        public boolean setBlock(ChunkHolder chunk, int x, int y, int z,
            BlockStateHolder block) {
            return chunk.set.setBlock(x, y, z, block);
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
    private IChunkGet get;
    private IChunkSet set;
    private IBlockDelegate delegate;
    private IQueueExtent extent;
    private int chunkX;
    private int chunkZ;

    public ChunkHolder() {
        this.delegate = NULL;
    }

    public ChunkHolder(IBlockDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void flood(Flood flood, FilterBlockMask mask, ChunkFilterBlock block) {
//        block.flood(get, set, mask, block, );
    }

    @Override
    public CompoundTag getTag(int x, int y, int z) {
        return delegate.getFullBlock(this, x, y, z)
            .getNbtData(); // TODO NOT IMPLEMENTED (add getTag delegate)
    }

    @Override
    public boolean hasSection(int layer) {
        return get != null && get.hasSection(layer);
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
        if (set != null) {
            final boolean result = set.trim(aggressive);
            if (result) {
                delegate = NULL;
                get = null;
                set = null;
                return true;
            }
        }
        if (aggressive) {
            get = null;
            if (delegate == BOTH) {
                delegate = SET;
            } else if (delegate == GET) {
                delegate = NULL;
            }
        } else {
            get.trim(false);
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return set == null || set.isEmpty();
    }

    /**
     * Get or create the settable part of this chunk
     * @return
     */
    public final IChunkGet getOrCreateGet() {
        if (get == null) {
            get = newWrappedGet();
        }
        return get;
    }

    /**
     * Get or create the settable part of this chunk
     * @return
     */
    public final IChunkSet getOrCreateSet() {
        if (set == null) {
            set = newWrappedSet();
        }
        return set;
    }

    /**
     * Create the settable part of this chunk (defaults to a char array)
     * @return
     */
    public IChunkSet createSet() {
        return new CharSetBlocks();
    }

    /**
     * Create a wrapped set object
     *  - The purpose of wrapping is to allow different extents to intercept / alter behavior
     *  - E.g. caching, optimizations, filtering
     * @return
     */
    private IChunkSet newWrappedSet() {
        if (extent instanceof SingleThreadQueueExtent) {
            IChunkSet newSet = extent.getCachedSet(chunkX, chunkZ, this::createSet);
            if (newSet != null) {
                return newSet;
            }
        }
        return createSet();
    }

    /**
     * Create a wrapped get object
     *  - The purpose of wrapping is to allow different extents to intercept / alter behavior
     *  - E.g. caching, optimizations, filtering
     * @return
     */
    private IChunkGet newWrappedGet() {
        if (extent instanceof SingleThreadQueueExtent) {
            IChunkGet newGet = extent.getCachedGet(chunkX, chunkZ, this::get);
            if (newGet != null) {
                return newGet;
            }
        }
        return get();
    }

    public abstract IChunkGet get();

    @Override
    public void init(IQueueExtent extent, int chunkX, int chunkZ) {
        this.extent = extent;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        if (set != null) {
            set.reset();
            delegate = SET;
        } else {
            delegate = NULL;
        }
        get = null;
    }

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

        boolean setBiome(ChunkHolder chunk, int x, int y, int z,
            BiomeType biome);

        boolean setBlock(ChunkHolder chunk, int x, int y, int z,
            BlockStateHolder holder);

        BiomeType getBiome(ChunkHolder chunk, int x, int z);

        BlockState getBlock(ChunkHolder chunk, int x, int y, int z);

        BaseBlock getFullBlock(ChunkHolder chunk, int x, int y, int z);
    }
}
