package com.fastasyncworldedit.core.queue.implementation.chunk;

import com.fastasyncworldedit.core.extent.filter.block.ChunkFilterBlock;
import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.fastasyncworldedit.core.queue.implementation.blocks.DataArray;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * NOT public API. Allows prevention of a {@link ChunkHolder} instance being cached "locally" whilst it has been
 * called/submitted, causing issues with processing/postprocessing, etc.
 * The function {@link WrapperChunk#invalidate(ChunkHolder)} is called in
 * {@link ChunkHolder#call(IQueueExtent, IChunkSet, Runnable)} which means the next access to the wrapped chunk will call the
 * supplier given on instantiation.
 *
 * @param <T> type of wrapped chunk
 * @since 2.15.0
 */
@SuppressWarnings("removal")
@ApiStatus.Internal
public class WrapperChunk<T extends IChunk> implements IChunk {

    private final Supplier<T> supplier;
    private volatile T chunk;

    /**
     * New instance.
     *
     * @param initialValue Initial chunk to be wrapped
     * @param supplier     Supplier to provide "replacement" chunk if this {@link WrapperChunk} is invalidated
     */
    public WrapperChunk(T initialValue, Supplier<T> supplier) {
        this.chunk = initialValue;
        this.supplier = supplier;
    }

    private T getWrapped() {
        T c = this.chunk;
        if (c != null) {
            return c;
        }
        synchronized (this) {
            if (this.chunk == null) {
                this.chunk = supplier.get();
            }
            return this.chunk;
        }
    }

    /**
     * Get the chunk currently cached.
     *
     * @return cached chunk
     */
    public T get() {
        return chunk;
    }

    /**
     * Invalidate the currently stored chunk (set to null) if the existing {@link ChunkHolder} matches the expected.
     *
     * @param expected The {@link ChunkHolder} instance we expect.
     * @return true if the expected {@link ChunkHolder} is the currently wrapped chunk, false otherwise (and does not invalidate).
     */
    boolean invalidate(ChunkHolder<?> expected) {
        T c = this.chunk;
        if (c != null && expected != c) {
            return false;
        }
        synchronized (this) {
            this.chunk = null;
        }
        return true;
    }

    /**
     * Set a new chunk that this instance should wrap. Calls {@link ChunkHolder#setWrapper(WrapperChunk)} if the chunk to wrap
     * is of type {@link ChunkHolder}.
     *
     * @param chunk chunk to wrap
     */
    public void setWrapped(T chunk) {
        synchronized (this) {
            this.chunk = Objects.requireNonNull(chunk);
        }
    }

    @Override
    public <V extends IChunk> void init(final IQueueExtent<V> extent, final int x, final int z) {
        getWrapped().init(extent, x, z);
    }

    @Override
    public int getX() {
        return getWrapped().getX();
    }

    @Override
    public int getZ() {
        return getWrapped().getZ();
    }

    @Override
    public byte[] toByteArray(final boolean full, final boolean stretched) {
        return getWrapped().toByteArray(full, stretched);
    }

    @Override
    public byte[] toByteArray(final byte[] buffer, final int bitMask, final boolean full, final boolean stretched) {
        return getWrapped().toByteArray(buffer, bitMask, full, stretched);
    }

    @Override
    public BlockVector3 getChunkBlockCoord() {
        return getWrapped().getChunkBlockCoord();
    }

    @Override
    public IChunk getRoot() {
        return getWrapped().getRoot();
    }

    @Override
    public void filterBlocks(
            final Filter filter,
            final ChunkFilterBlock block,
            @Nullable final Region region,
            final boolean full
    ) {
        getWrapped().filterBlocks(filter, block, region, full);
    }

    @Override
    public boolean setBiome(final int x, final int y, final int z, final BiomeType biome) {
        return getWrapped().setBiome(x, y, z, biome);
    }

    @Override
    public boolean setBiome(final BlockVector3 position, final BiomeType biome) {
        return getWrapped().setBiome(position, biome);
    }

    @Override
    public void setBlockLight(final BlockVector3 position, final int value) {
        getWrapped().setBlockLight(position, value);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(final BlockVector3 position, final T block) throws
            WorldEditException {
        return getWrapped().setBlock(position, block);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(final int x, final int y, final int z, final T holder) {
        return getWrapped().setBlock(x, y, z, holder);
    }

    @Override
    public void setBlocks(final int layer, final DataArray data) {
        getWrapped().setBlocks(layer, data);
    }

    @Override
    public boolean isEmpty() {
        return getWrapped().isEmpty();
    }

    @Override
    public boolean setTile(final int x, final int y, final int z, final CompoundTag tile) throws WorldEditException {
        return getWrapped().setTile(x, y, z, tile);
    }

    @Override
    public boolean tile(final int x, final int y, final int z, final FaweCompoundTag tag) {
        return getWrapped().tile(x, y, z, tag);
    }

    @Override
    public boolean fullySupports3DBiomes() {
        return getWrapped().fullySupports3DBiomes();
    }

    @Override
    public boolean setBiome(final BlockVector2 position, final BiomeType biome) {
        return getWrapped().setBiome(position, biome);
    }

    @Override
    public void setBlockLight(final int x, final int y, final int z, final int value) {
        getWrapped().setBlockLight(x, y, z, value);
    }

    @Override
    public void setSkyLight(final BlockVector3 position, final int value) {
        getWrapped().setSkyLight(position, value);
    }

    @Override
    public void setSkyLight(final int x, final int y, final int z, final int value) {
        getWrapped().setSkyLight(x, y, z, value);
    }

    @Override
    public void setHeightMap(final HeightMapType type, final int[] heightMap) {
        getWrapped().setHeightMap(type, heightMap);
    }

    @Override
    public void setLightLayer(final int layer, final char[] toSet) {
        getWrapped().setLightLayer(layer, toSet);
    }

    @Override
    public void setSkyLightLayer(final int layer, final char[] toSet) {
        getWrapped().setSkyLightLayer(layer, toSet);
    }

    @Override
    public void setFullBright(final int layer) {
        getWrapped().setFullBright(layer);
    }

    @Override
    public void setEntity(final CompoundTag tag) {
        getWrapped().setEntity(tag);
    }

    @Override
    public void entity(final FaweCompoundTag tag) {
        getWrapped().entity(tag);
    }

    @Override
    public void removeEntity(final UUID uuid) {
        getWrapped().removeEntity(uuid);
    }

    @Override
    public Set<UUID> getEntityRemoves() {
        return getWrapped().getEntityRemoves();
    }

    @Override
    public BiomeType[][] getBiomes() {
        return getWrapped().getBiomes();
    }

    @Override
    public boolean hasBiomes() {
        return getWrapped().hasBiomes();
    }

    @Override
    public char[][] getLight() {
        return getWrapped().getLight();
    }

    @Override
    public char[][] getSkyLight() {
        return getWrapped().getSkyLight();
    }

    @Override
    public boolean hasLight() {
        return getWrapped().hasLight();
    }

    @Override
    public void setFastMode(final boolean fastMode) {
        getWrapped().setFastMode(fastMode);
    }

    @Override
    public boolean isFastMode() {
        return getWrapped().isFastMode();
    }

    @Override
    public void setBitMask(final int bitMask) {
        getWrapped().setBitMask(bitMask);
    }

    @Override
    public int getBitMask() {
        return getWrapped().getBitMask();
    }

    @Override
    public void removeSectionLighting(final int layer, final boolean sky) {
        getWrapped().removeSectionLighting(layer, sky);
    }

    @Override
    public boolean trim(final boolean aggressive, final int layer) {
        return getWrapped().trim(aggressive, layer);
    }

    @Override
    public Map<HeightMapType, int[]> getHeightMaps() {
        return getWrapped().getHeightMaps();
    }

    @Override
    public IChunk reset() {
        return getWrapped().reset();
    }

    @Override
    public int getSectionCount() {
        return getWrapped().getSectionCount();
    }

    @Override
    public int getMaxSectionPosition() {
        return getWrapped().getMaxSectionPosition();
    }

    @Override
    public int getMinSectionPosition() {
        return getWrapped().getMinSectionPosition();
    }

    @Override
    public @Nullable Operation commit() {
        return getWrapped().commit();
    }

    @Override
    public boolean hasBiomes(final int layer) {
        return getWrapped().hasBiomes(layer);
    }

    @Override
    public @NotNull IChunkSet createCopy() {
        return getWrapped().createCopy();
    }

    @Override
    public void setSideEffectSet(@NotNull final SideEffectSet sideEffectSet) {
        getWrapped().setSideEffectSet(sideEffectSet);
    }

    @Override
    public @NotNull SideEffectSet getSideEffectSet() {
        return getWrapped().getSideEffectSet();
    }

    @Override
    public BaseBlock getFullBlock(final int x, final int y, final int z) {
        return getWrapped().getFullBlock(x, y, z);
    }

    @Override
    public BiomeType getBiome(final BlockVector2 position) {
        return getWrapped().getBiome(position);
    }

    @Override
    public BiomeType getBiomeType(final int x, final int y, final int z) {
        return getWrapped().getBiomeType(x, y, z);
    }

    @Override
    public BiomeType getBiome(final BlockVector3 position) {
        return getWrapped().getBiome(position);
    }

    @Override
    public int getEmittedLight(final BlockVector3 position) {
        return getWrapped().getEmittedLight(position);
    }

    @Override
    public boolean hasSection(final int layer) {
        return getWrapped().hasSection(layer);
    }

    @Override
    public boolean hasNonEmptySection(final int layer) {
        return getWrapped().hasNonEmptySection(layer);
    }

    @Override
    public DataArray load(final int layer) {
        return getWrapped().load(layer);
    }

    @Override
    public @Nullable DataArray loadIfPresent(final int layer) {
        return getWrapped().loadIfPresent(layer);
    }

    @Override
    public BlockState getBlock(final BlockVector3 position) {
        return getWrapped().getBlock(position);
    }

    @Override
    public BlockState getBlock(final int x, final int y, final int z) {
        return getWrapped().getBlock(x, y, z);
    }

    @Override
    public BaseBlock getFullBlock(final BlockVector3 position) {
        return getWrapped().getFullBlock(position);
    }

    @Override
    public Map<BlockVector3, CompoundTag> getTiles() {
        return getWrapped().getTiles();
    }

    @Override
    public Map<BlockVector3, FaweCompoundTag> tiles() {
        return getWrapped().tiles();
    }

    @Override
    public int getSkyLight(final int x, final int y, final int z) {
        return getWrapped().getSkyLight(x, y, z);
    }

    @Override
    public int getBrightness(final MutableBlockVector3 position) {
        return getWrapped().getBrightness(position);
    }

    @Override
    public int getBrightness(final int x, final int y, final int z) {
        return getWrapped().getBrightness(x, y, z);
    }

    @Override
    public int getOpacity(final MutableBlockVector3 position) {
        return getWrapped().getOpacity(position);
    }

    @Override
    public int getOpacity(final int x, final int y, final int z) {
        return getWrapped().getOpacity(x, y, z);
    }

    @Override
    public int getEmittedLight(final int x, final int y, final int z) {
        return getWrapped().getEmittedLight(x, y, z);
    }

    @Override
    public int getSkyLight(final MutableBlockVector3 position) {
        return getWrapped().getSkyLight(position);
    }

    @Override
    public int[] getHeightMap(final HeightMapType type) {
        return getWrapped().getHeightMap(type);
    }

    @Override
    public void optimize() {
        getWrapped().optimize();
    }

    @Override
    public <T extends Future<T>> T call(
            final IQueueExtent<?> owner,
            final IChunkSet set,
            final Runnable finalize
    ) {
        return getWrapped().call(owner, set, finalize);
    }

    @Override
    public CompoundTag getEntity(final UUID uuid) {
        return getWrapped().getEntity(uuid);
    }

    @Override
    public @Nullable FaweCompoundTag entity(final UUID uuid) {
        return getWrapped().entity(uuid);
    }

    @Override
    public CompoundTag getTile(final int x, final int y, final int z) {
        return getWrapped().getTile(x, y, z);
    }

    @Override
    public @Nullable FaweCompoundTag tile(final int x, final int y, final int z) {
        return getWrapped().tile(x, y, z);
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return getWrapped().getEntities();
    }

    @Override
    public Collection<FaweCompoundTag> entities() {
        return getWrapped().entities();
    }

    @Override
    public Set<Entity> getFullEntities() {
        return getWrapped().getFullEntities();
    }

    @Override
    public boolean isCreateCopy() {
        return getWrapped().isCreateCopy();
    }

    @Override
    public int setCreateCopy(final boolean createCopy) {
        return getWrapped().setCreateCopy(createCopy);
    }

    @Override
    public @Nullable IChunkGet getCopy(final int key) {
        return getWrapped().getCopy(key);
    }

    @Override
    public void lockCall() {
        getWrapped().lockCall();
    }

    @Override
    public void unlockCall() {
        getWrapped().unlockCall();
    }

    @Override
    public void setLightingToGet(final char[][] lighting, final int startSectionIndex, final int endSectionIndex) {
        getWrapped().setLightingToGet(lighting, startSectionIndex, endSectionIndex);
    }

    @Override
    public void setSkyLightingToGet(final char[][] lighting, final int startSectionIndex, final int endSectionIndex) {
        getWrapped().setSkyLightingToGet(lighting, startSectionIndex, endSectionIndex);
    }

    @Override
    public void setHeightmapToGet(final HeightMapType type, final int[] data) {
        getWrapped().setHeightmapToGet(type, data);
    }

    @Override
    public int getMaxY() {
        return getWrapped().getMaxY();
    }

    @Override
    public int getMinY() {
        return getWrapped().getMinY();
    }

    @Override
    public boolean trim(final boolean aggressive) {
        return getWrapped().trim(aggressive);
    }

    @Override
    public void recycle() {
        getWrapped().recycle();
    }


}
