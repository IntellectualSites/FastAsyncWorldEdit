package com.fastasyncworldedit.core.extent;

import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.math.BlockVector3ChunkMap;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.util.ExtentTraverser;
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
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinTag;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class StripNBTExtent extends AbstractDelegateExtent implements IBatchProcessor {

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
        final LinCompoundTag nbt = localBlock.getNbt();
        if (nbt == null) {
            return block;
        }
        LinCompoundTag.Builder nbtBuilder = nbt.toBuilder();
        for (String key : strip) {
            nbtBuilder.remove(key);
        }
        return (B) localBlock.toBaseBlock(nbtBuilder.build());
    }

    public <T extends NbtValued> T stripEntityNBT(T entity) {
        LinCompoundTag nbt = entity.getNbt();
        if (nbt == null) {
            return entity;
        }
        LinCompoundTag.Builder nbtBuilder = nbt.toBuilder();
        for (String key : strip) {
            nbtBuilder.remove(key);
        }
        entity.setNbt(nbtBuilder.build());
        return entity;
    }

    @Override
    public IChunkSet processSet(final IChunk chunk, final IChunkGet get, final IChunkSet set) {
        Map<BlockVector3, FaweCompoundTag> tiles = set.tiles();
        Collection<FaweCompoundTag> entities = set.entities();
        if (tiles.isEmpty() && entities.isEmpty()) {
            return set;
        }
        boolean isBv3ChunkMap = tiles instanceof BlockVector3ChunkMap;
        for (final var entry : tiles.entrySet()) {
            FaweCompoundTag original = entry.getValue();
            FaweCompoundTag result = stripNbt(original);
            if (original != result) {
                if (isBv3ChunkMap) {
                    // Replace existing value with stripped value
                    tiles.put(entry.getKey(), result);
                } else {
                    entry.setValue(result);
                }
            }
        }
        Set<FaweCompoundTag> stripped = new HashSet<>();
        Iterator<FaweCompoundTag> iterator = entities.iterator();
        while (iterator.hasNext()) {
            FaweCompoundTag original = iterator.next();
            FaweCompoundTag result = stripNbt(original);
            if (original != result) {
                iterator.remove();
                stripped.add(result);
            }
        }
        // this relies on entities.addAll(...) not throwing an exception if empty+unmodifiable (=> stripped is empty too)
        entities.addAll(stripped);
        return set;
    }

    private FaweCompoundTag stripNbt(
            FaweCompoundTag compoundTag
    ) {
        LinCompoundTag.Builder builder = LinCompoundTag.builder();
        boolean stripped = false;
        for (var entry : compoundTag.linTag().value().entrySet()) {
            String k = entry.getKey();
            LinTag<?> v = entry.getValue();
            if (strip.contains(k.toLowerCase(Locale.ROOT))) {
                stripped = true;
            } else {
                builder.put(k, v);
            }
        }
        return stripped ? FaweCompoundTag.of(builder.build()) : compoundTag;
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
