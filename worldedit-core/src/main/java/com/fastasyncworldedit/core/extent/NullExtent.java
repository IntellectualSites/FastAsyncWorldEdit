package com.fastasyncworldedit.core.extent;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.function.generator.GenBase;
import com.fastasyncworldedit.core.function.generator.Resource;
import com.fastasyncworldedit.core.internal.exception.FaweException;
import com.fastasyncworldedit.core.limit.FaweLimit;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

//todo This should be removed in favor of com.sk89q.worldedit.extent.NullExtent
public final class NullExtent extends FaweRegionExtent implements IBatchProcessor {

    private final FaweException reason;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public NullExtent(Extent extent, Component failReason) {
        this(extent, new FaweException(failReason));
    }

    public NullExtent(Extent extent, FaweException exception) {
        super(extent, FaweLimit.MAX);
        this.reason = exception;
    }

    public NullExtent() {
        this(new com.sk89q.worldedit.extent.NullExtent(), FaweCache.MANUAL);
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return BlockVector3.ZERO;
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return BlockVector3.ZERO;
    }

    @Override
    public List<Entity> getEntities(Region region) {
        throw reason;
    }

    @Override
    public List<Entity> getEntities() {
        throw reason;
    }

    @Nullable
    @Override
    public Entity createEntity(Location arg0, BaseEntity arg1) {
        throw reason;
    }

    @Nullable
    @Override
    public Entity createEntity(Location arg0, BaseEntity arg1, UUID arg2) {
        throw reason;
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        throw reason;
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        throw reason;
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        throw reason;
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        throw reason;
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        throw reason;
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        throw reason;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block) throws WorldEditException {
        throw reason;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block)
            throws WorldEditException {
        throw reason;
    }

    @Override
    public ResettableExtent setExtent(Extent extent) {
        throw reason;
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        throw reason;
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        throw reason;
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        throw reason;
    }

    @Override
    public boolean isQueueEnabled() {
        throw reason;
    }

    @Override
    public void enableQueue() {
        throw reason;
    }

    @Override
    public void disableQueue() {
        throw reason;
    }

    @Override
    public boolean isWorld() {
        throw reason;
    }

    @Override
    public boolean regenerateChunk(int x, int z, @Nullable BiomeType type, @Nullable Long seed) {
        throw reason;
    }

    @Override
    public int getHighestTerrainBlock(int x, int z, int minY, int maxY) {
        throw reason;
    }

    @Override
    public int getHighestTerrainBlock(int x, int z, int minY, int maxY, Mask filter) {
        throw reason;
    }

    @Override
    public boolean contains(int x, int z) {
        throw reason;
    }

    @Override
    public boolean contains(int x, int y, int z) {
        throw reason;
    }

    @Override
    public Collection<Region> getRegions() {
        throw reason;
    }

    @Nullable
    @Override
    public Operation commit() {
        return null;
    }

    @Override
    public boolean cancel() {
        throw reason;
    }

    @Override
    public int getMaxY() {
        throw reason;
    }

    @Override
    public int getMinY() {
        throw reason;
    }

    @Override
    public Extent addProcessor(final IBatchProcessor processor) {
        return this;
    }

    @Override
    public Extent addPostProcessor(final IBatchProcessor processor) {
        return this;
    }

    @Override
    public BlockArrayClipboard lazyCopy(Region region) {
        throw reason;
    }

    @Override
    public int countBlocks(Region region, Set<BaseBlock> searchBlocks) {
        throw reason;
    }

    @Override
    public int countBlocks(Region region, Mask searchMask) {
        throw reason;
    }

    @Override
    public <B extends BlockStateHolder<B>> int setBlocks(Region region, B block) throws MaxChangedBlocksException {
        throw reason;
    }

    @Override
    public int setBlocks(Region region, Pattern pattern) throws MaxChangedBlocksException {
        throw reason;
    }

    @Override
    public <B extends BlockStateHolder<B>> int replaceBlocks(Region region, Set<BaseBlock> filter, B replacement) throws
            MaxChangedBlocksException {
        throw reason;
    }

    @Override
    public int replaceBlocks(Region region, Set<BaseBlock> filter, Pattern pattern) throws MaxChangedBlocksException {
        throw reason;
    }

    @Override
    public int replaceBlocks(Region region, Mask mask, Pattern pattern) throws MaxChangedBlocksException {
        throw reason;
    }

    @Override
    public int center(Region region, Pattern pattern) throws MaxChangedBlocksException {
        throw reason;
    }

    @Override
    public int setBlocks(Set<BlockVector3> vset, Pattern pattern) {
        throw reason;
    }

    @Override
    public int getNearestSurfaceLayer(int x, int z, int y, int minY, int maxY) {
        throw reason;
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax, Mask mask) {
        throw reason;
    }

    @Override
    public int getNearestSurfaceTerrainBlock(
            int x,
            int z,
            int y,
            int minY,
            int maxY,
            int failedMin,
            int failedMax,
            boolean ignoreAir
    ) {
        throw reason;
    }

    @Override
    public void addCaves(Region region) throws WorldEditException {
        throw reason;
    }

    @Override
    public void generate(Region region, GenBase gen) throws WorldEditException {
        throw reason;
    }

    @Override
    public void addSchems(Region region, Mask mask, List<ClipboardHolder> clipboards, int rarity, boolean rotate) throws
            WorldEditException {
        throw reason;
    }

    @Override
    public void spawnResource(Region region, Resource gen, int rarity, int frequency) throws WorldEditException {
        throw reason;
    }

    @Override
    public void addOre(Region region, Mask mask, Pattern material, int size, int frequency, int rarity, int minY, int maxY) throws
            WorldEditException {
        throw reason;
    }

    @Override
    public void addOres(Region region, Mask mask) throws WorldEditException {
        throw reason;
    }

    @Override
    public List<Countable<BlockType>> getBlockDistribution(Region region) {
        throw reason;
    }

    @Override
    public List<Countable<BlockState>> getBlockDistributionWithData(Region region) {
        throw reason;
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        throw reason;
    }

    @Override
    public Future<?> postProcessSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        throw reason;
    }

    @Override
    public void postProcess(IChunk chunk, IChunkGet get, IChunkSet set) {
        throw reason;
    }

    @Override
    public boolean processGet(int chunkX, int chunkZ) {
        throw reason;
    }

    @Override
    public Extent construct(Extent child) {
        throw reason;
    }

    @Override
    public ProcessorScope getScope() {
        throw reason;
    }

}
