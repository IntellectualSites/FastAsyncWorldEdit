package com.fastasyncworldedit.core.extent;

import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.math.BlockVector3ChunkMap;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.google.common.collect.ImmutableMap;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.NbtValued;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class StripNBTExtent extends AbstractDelegateExtent implements IBatchProcessor {

    private final Set<String> strip;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public StripNBTExtent(Extent extent, Set<String> strip) {
        super(extent);
        this.strip = new HashSet<>(strip)
                .stream()
                .map(String::toLowerCase).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block) throws WorldEditException {
        return super.setBlock(location, stripBlockNBT(block));
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) throws WorldEditException {
        return super.setBlock(x, y, z, stripBlockNBT(block));
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        return super.createEntity(location, stripEntityNBT(entity));
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity, UUID uuid) {
        return super.createEntity(location, stripEntityNBT(entity), uuid);
    }

    @SuppressWarnings("unchecked")
    public <B extends BlockStateHolder<B>> B stripBlockNBT(B block) {
        if (!(block instanceof BaseBlock localBlock)) {
            return block;
        }
        if (!localBlock.hasNbtData()) {
            return block;
        }
        CompoundTag nbt = localBlock.getNbtData();
        Map<String, Tag> value = new HashMap<>(nbt.getValue());
        for (String key : strip) {
            value.remove(key);
        }
        return (B) localBlock.toBaseBlock(new CompoundTag(value));
    }

    public <T extends NbtValued> T stripEntityNBT(T entity) {
        if (!entity.hasNbtData()) {
            return entity;
        }
        CompoundTag nbt = entity.getNbtData();
        Map<String, Tag> value = new HashMap<>(nbt.getValue());
        for (String key : strip) {
            value.remove(key);
        }
        entity.setNbtData(new CompoundTag(value));
        return entity;
    }

    @Override
    public IChunkSet processSet(final IChunk chunk, final IChunkGet get, final IChunkSet set) {
        Map<BlockVector3, CompoundTag> tiles = set.getTiles();
        Set<CompoundTag> entities = set.getEntities();
        if (tiles.isEmpty() && entities.isEmpty()) {
            return set;
        }
        boolean isBv3ChunkMap = tiles instanceof BlockVector3ChunkMap;
        for (final Map.Entry<BlockVector3, CompoundTag> entry : tiles.entrySet()) {
            ImmutableMap.Builder<String, Tag> map = ImmutableMap.builder();
            final AtomicBoolean isStripped = new AtomicBoolean(false);
            entry.getValue().getValue().forEach((k, v) -> {
                if (strip.contains(k.toLowerCase())) {
                    isStripped.set(true);
                } else {
                    map.put(k, v);
                }
            });
            if (isStripped.get()) {
                if (isBv3ChunkMap) {
                    // Replace existing value with stripped value
                    tiles.put(entry.getKey(), new CompoundTag(map.build()));
                } else {
                    entry.setValue(new CompoundTag(map.build()));
                }
            }
        }
        Set<CompoundTag> stripped = new HashSet<>();
        Iterator<CompoundTag> iterator = entities.iterator();
        while (iterator.hasNext()) {
            CompoundTag entity = iterator.next();
            ImmutableMap.Builder<String, Tag> map = ImmutableMap.builder();
            final AtomicBoolean isStripped = new AtomicBoolean(false);
            entity.getValue().forEach((k, v) -> {
                if (strip.contains(k.toUpperCase(Locale.ROOT))) {
                    isStripped.set(true);
                } else {
                    map.put(k, v);
                }
            });
            if (isStripped.get()) {
                iterator.remove();
                stripped.add(new CompoundTag(map.build()));
            }
        }
        set.getEntities().addAll(stripped);
        return set;
    }

    @Nullable
    @Override
    public Extent construct(final Extent child) {
        if (getExtent() != child) {
            new ExtentTraverser<Extent>(this).setNext(child);
        }
        return this;
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.CHANGING_BLOCKS;
    }

}
