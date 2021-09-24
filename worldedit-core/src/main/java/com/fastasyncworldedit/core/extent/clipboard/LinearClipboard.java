package com.fastasyncworldedit.core.extent.clipboard;

import com.fastasyncworldedit.core.extent.filter.block.AbstractFilterBlock;
import com.fastasyncworldedit.core.function.visitor.Order;
import com.fastasyncworldedit.core.jnbt.streamer.IntValueReader;
import com.google.common.collect.ForwardingIterator;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard.ClipboardEntity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;

public abstract class LinearClipboard extends SimpleClipboard {

    protected final HashSet<ClipboardEntity> entities = new HashSet<>();

    public LinearClipboard(BlockVector3 dimensions, BlockVector3 offset) {
        super(dimensions, offset);
    }

    // We shouldn't expose methods that directly reference the index as people cannot be trusted to use it properly.
    public abstract <B extends BlockStateHolder<B>> boolean setBlock(int i, B block);

    public abstract BaseBlock getFullBlock(int i);

    public abstract BlockState getBlock(int i);

    public abstract void setBiome(int index, BiomeType biome);

    public abstract BiomeType getBiome(int index);

    /**
     * The locations provided are relative to the clipboard min
     *
     * @param task
     */
    public abstract void streamBiomes(IntValueReader task);

    public abstract Collection<CompoundTag> getTileEntities();

    @Override
    protected void finalize() {
        close();
    }

    @Nonnull
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

    @Override
    public void removeEntity(int x, int y, int z, UUID uuid) {
        Iterator<ClipboardEntity> iter = this.entities.iterator();
        while (iter.hasNext()) {
            ClipboardEntity entity = iter.next();
            UUID entUUID = entity.getState().getNbtData().getUUID();
            if (uuid.equals(entUUID)) {
                iter.remove();
                return;
            }
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
