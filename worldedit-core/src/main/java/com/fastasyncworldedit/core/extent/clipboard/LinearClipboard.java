package com.fastasyncworldedit.core.extent.clipboard;

import com.fastasyncworldedit.core.extent.filter.block.AbstractFilterBlock;
import com.fastasyncworldedit.core.function.visitor.Order;
import com.fastasyncworldedit.core.jnbt.streamer.IntValueReader;
import com.google.common.collect.ForwardingIterator;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTUtils;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard.ClipboardEntity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
        if (order == Order.YZX) {
            if (region instanceof CuboidRegion) {
                Iterator<BlockVector3> iter = ((CuboidRegion) region).iterator_old();
                LinearFilter filter = new LinearFilter();

                return new ForwardingIterator<>() {
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
        }
        return order.create(region);

    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        BlockArrayClipboard.ClipboardEntity ret = new BlockArrayClipboard.ClipboardEntity(location, entity);
        entities.add(ret);
        return ret;
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity, UUID uuid) {
        Map<String, Tag<?, ?>> map = new HashMap<>(entity.getNbtData().getValue());
        NBTUtils.addUUIDToMap(map, uuid);
        entity.setNbtData(new CompoundTag(map));
        BlockArrayClipboard.ClipboardEntity ret = new BlockArrayClipboard.ClipboardEntity(location, entity);
        entities.add(ret);
        return ret;
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

    @Override
    public List<? extends Entity> getEntities() {
        return new ArrayList<>(entities);
    }

    @Override
    public void removeEntity(Entity entity) {
        if (!(entity instanceof BlockArrayClipboard.ClipboardEntity)) {
            Location loc = entity.getLocation();
            removeEntity(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), entity.getState().getNbtData().getUUID());
        } else {
            this.entities.remove(entity);
        }
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return entities
                .stream()
                .filter(e -> region.contains(e.getLocation().toBlockPoint())).collect(Collectors.toList());
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
        public BiomeType getBiome() {
            return LinearClipboard.this.getBiome(position);
        }

        @Override
        public void setBiome(final BiomeType type) {
            LinearClipboard.this.setBiome(position, type);
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
