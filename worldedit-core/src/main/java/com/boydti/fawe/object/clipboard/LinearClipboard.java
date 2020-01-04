package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.beta.implementation.filter.block.AbstractFilterBlock;
import com.boydti.fawe.jnbt.streamer.IntValueReader;
import com.google.common.collect.ForwardingIterator;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.visitor.Order;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.io.Closeable;
import java.util.Collection;
import java.util.Iterator;

/**
 * Best used when clipboard selections are small, or using legacy formats
 * (Small being < Integer.MAX_VALUE/BLOCK_SIZE_BYTES blocks)
 */
public abstract class LinearClipboard extends SimpleClipboard implements Clipboard, Closeable {
    public LinearClipboard(BlockVector3 dimensions) {
        super(dimensions);
    }

    public abstract <B extends BlockStateHolder<B>> boolean setBlock(int i, B block);

    public abstract BaseBlock getFullBlock(int i);

    public abstract BlockState getBlock(int i);

    public abstract void setBiome(int index, BiomeType biome);

    public abstract BiomeType getBiome(int index);

    /**
     * The locations provided are relative to the clipboard min
     *
     * @param task
     * @param air
     */
    public abstract void streamBiomes(IntValueReader task);

    public abstract Collection<CompoundTag> getTileEntities();

    public void close() {}

    public void flush() {}

    @Override
    protected void finalize() {
        close();
    }

    @Override
    public Iterator<BlockVector3> iterator() {
        return iterator(Order.YZX);
    }

    @Override
    public Iterator<BlockVector3> iterator(Order order) {
        Region region = getRegion();
        switch (order) {
            case YZX:
                if (region instanceof CuboidRegion) {
                    Iterator<BlockVector3> iter = ((CuboidRegion) region).iterator_old();
                    LinearFilter filter = new LinearFilter();

                    return new ForwardingIterator<BlockVector3>() {
                        @Override
                        protected Iterator<BlockVector3> delegate() {
                            return iter;
                        }

                        @Override
                        public BlockVector3 next() {
                            return filter.next(super.next());
                        }
                    };
                }
            default:
                return order.create(region);
        }

    }

    private class LinearFilter extends AbstractFilterBlock {
        private int index = -1;
        private BlockVector3 position;

        private LinearFilter next(BlockVector3 position) {
            this.position = position;
            index++;
            return this;
        }
        @Override
        public BaseBlock getFullBlock() {
            return LinearClipboard.this.getFullBlock(index);
        }

        @Override
        public void setFullBlock(BaseBlock block) {
            LinearClipboard.this.setBlock(index, block);
        }

        @Override
        public BlockVector3 getPosition() {
            return position;
        }

        @Override
        public Extent getExtent() {
            return LinearClipboard.this;
        }
    }
}
