package com.boydti.fawe.beta.implementation.holder;

import com.boydti.fawe.beta.implementation.blocks.CharGetBlocks;
import com.boydti.fawe.beta.implementation.blocks.CharSetBlocks;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IGetBlocks;
import com.boydti.fawe.beta.ISetBlocks;
import com.boydti.fawe.beta.implementation.SingleThreadQueueExtent;
import com.boydti.fawe.beta.implementation.WorldChunkCache;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.function.Supplier;

public abstract class ChunkHolder<T, V extends SingleThreadQueueExtent> implements IChunk<T, V>, Supplier<IGetBlocks> {
    private IGetBlocks get;
    private ISetBlocks set;
    private IBlockDelegate delegate;
    private SingleThreadQueueExtent extent;
    private int X,Z;

    public ChunkHolder() {
        this.delegate = NULL;
    }

    public ChunkHolder(IBlockDelegate delegate) {
        this.delegate = delegate;
    }

    public final IGetBlocks cachedGet() {
        if (get == null) get = newGet();
        return get;
    }

    public final ISetBlocks cachedSet() {
        if (set == null) set = set();
        return set;
    }

    public ISetBlocks set() {
        return new CharSetBlocks();
    }

    private IGetBlocks newGet() {
        WorldChunkCache cache = extent.getCache();
        cache.get(MathMan.pairInt(X, Z), this);
        return new CharGetBlocks();
    }

    public void init(final SingleThreadQueueExtent extent, final int X, final int Z) {
        this.extent = extent;
        this.X = X;
        this.Z = Z;
        set = null;
        if (delegate == BOTH) {
            delegate = GET;
        } else if (delegate == SET) {
            delegate = NULL;
        }
    }

    public V getExtent() {
        return (V) extent;
    }

    @Override
    public int getX() {
        return X;
    }

    @Override
    public int getZ() {
        return Z;
    }

    @Override
    public boolean setBiome(final int x, final int y, final int z, final BiomeType biome) {
        return delegate.setBiome(this, x, y, z, biome);
    }

    @Override
    public boolean setBlock(final int x, final int y, final int z, final BlockStateHolder block) {
        return delegate.setBlock(this, x, y, z, block);
    }

    @Override
    public BiomeType getBiome(final int x, final int z) {
        return delegate.getBiome(this, x, z);
    }

    @Override
    public BlockState getBlock(final int x, final int y, final int z) {
        return delegate.getBlock(this, x, y, z);
    }

    @Override
    public BaseBlock getFullBlock(final int x, final int y, final int z) {
        return delegate.getFullBlock(this, x, y, z);
    }

    public interface IBlockDelegate {
        boolean setBiome(final ChunkHolder chunk, final int x, final int y, final int z, final BiomeType biome);

        boolean setBlock(final ChunkHolder chunk, final int x, final int y, final int z, final BlockStateHolder holder);

        BiomeType getBiome(final ChunkHolder chunk, final int x, final int z);

        BlockState getBlock(final ChunkHolder chunk, final int x, final int y, final int z);

        BaseBlock getFullBlock(final ChunkHolder chunk, final int x, final int y, final int z);
    }

    public static final IBlockDelegate NULL = new IBlockDelegate() {
        @Override
        public boolean setBiome(final ChunkHolder chunk, final int x, final int y, final int z, final BiomeType biome) {
            chunk.cachedSet();
            chunk.delegate = SET;
            return chunk.setBiome(x, y, z, biome);
        }

        @Override
        public boolean setBlock(final ChunkHolder chunk, final int x, final int y, final int z, final BlockStateHolder block) {
            chunk.cachedSet();
            chunk.delegate = SET;
            return chunk.setBlock(x, y, z, block);
        }

        @Override
        public BiomeType getBiome(final ChunkHolder chunk, final int x, final int z) {
            chunk.cachedGet();
            chunk.delegate = GET;
            return chunk.getBiome(x, z);
        }

        @Override
        public BlockState getBlock(final ChunkHolder chunk, final int x, final int y, final int z) {
            chunk.cachedGet();
            chunk.delegate = GET;
            return chunk.getBlock(x, y, z);
        }

        @Override
        public BaseBlock getFullBlock(final ChunkHolder chunk, final int x, final int y, final int z) {
            chunk.cachedGet();
            chunk.delegate = GET;
            return chunk.getFullBlock(x, y, z);
        }
    };

    public static final IBlockDelegate GET = new IBlockDelegate() {
        @Override
        public boolean setBiome(final ChunkHolder chunk, final int x, final int y, final int z, final BiomeType biome) {
            chunk.cachedSet();
            chunk.delegate = BOTH;
            return chunk.setBiome(x, y, z, biome);
        }

        @Override
        public boolean setBlock(final ChunkHolder chunk, final int x, final int y, final int z, final BlockStateHolder block) {
            chunk.cachedSet();
            chunk.delegate = BOTH;
            return chunk.setBlock(x, y, z, block);
        }

        @Override
        public BiomeType getBiome(final ChunkHolder chunk, final int x, final int z) {
            return chunk.get.getBiome(x, z);
        }

        @Override
        public BlockState getBlock(final ChunkHolder chunk, final int x, final int y, final int z) {
            return chunk.get.getBlock(x, y, z);
        }

        @Override
        public BaseBlock getFullBlock(final ChunkHolder chunk, final int x, final int y, final int z) {
            return chunk.get.getFullBlock(x, y, z);
        }
    };

    public static final IBlockDelegate SET = new IBlockDelegate() {
        @Override
        public boolean setBiome(final ChunkHolder chunk, final int x, final int y, final int z, final BiomeType biome) {
            return chunk.set.setBiome(x, y, z, biome);
        }

        @Override
        public boolean setBlock(final ChunkHolder chunk, final int x, final int y, final int z, final BlockStateHolder block) {
            return chunk.set.setBlock(x, y, z, block);
        }

        @Override
        public BiomeType getBiome(final ChunkHolder chunk, final int x, final int z) {
            chunk.cachedGet();
            chunk.delegate = BOTH;
            return chunk.getBiome(x, z);
        }

        @Override
        public BlockState getBlock(final ChunkHolder chunk, final int x, final int y, final int z) {
            chunk.cachedGet();
            chunk.delegate = BOTH;
            return chunk.getBlock(x, y, z);
        }

        @Override
        public BaseBlock getFullBlock(final ChunkHolder chunk, final int x, final int y, final int z) {
            chunk.cachedGet();
            chunk.delegate = BOTH;
            return chunk.getFullBlock(x, y, z);
        }
    };

    public static final IBlockDelegate BOTH = new IBlockDelegate() {
        @Override
        public boolean setBiome(final ChunkHolder chunk, final int x, final int y, final int z, final BiomeType biome) {
            return chunk.set.setBiome(x, y, z, biome);
        }

        @Override
        public boolean setBlock(final ChunkHolder chunk, final int x, final int y, final int z, final BlockStateHolder block) {
            return chunk.set.setBlock(x, y, z, block);
        }

        @Override
        public BiomeType getBiome(final ChunkHolder chunk, final int x, final int z) {
            return chunk.get.getBiome(x, z);
        }

        @Override
        public BlockState getBlock(final ChunkHolder chunk, final int x, final int y, final int z) {
            return chunk.get.getBlock(x, y, z);
        }

        @Override
        public BaseBlock getFullBlock(final ChunkHolder chunk, final int x, final int y, final int z) {
            return chunk.get.getFullBlock(x, y, z);
        }
    };
}
