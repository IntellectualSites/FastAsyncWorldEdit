package com.boydti.fawe.object.extent;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.exception.FaweException;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.generator.GenBase;
import com.sk89q.worldedit.function.generator.Resource;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class NullExtent extends FaweRegionExtent {

    private final FaweException reason;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public NullExtent(Extent extent, BBC failReason) {
        this(extent, new FaweException(failReason));
    }

    public NullExtent(Extent extent, FaweException exception) {
        super(extent, FaweLimit.MAX);
        this.reason = exception;
    }

    public NullExtent() {
        this(new com.sk89q.worldedit.extent.NullExtent(), FaweException.MANUAL);
    }

    @Override
    public ResettableExtent setExtent(Extent extent) {
        return this;
    }

    @Override
    public BiomeType getBiome(final BlockVector2 arg0) {
        throw reason;
    }

    @Override
    public BiomeType getBiomeType(int x, int z) {
        throw reason;
    }

    @Override
    public BlockState getBlock(final BlockVector3 arg0) {
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
        if(reason != null) {
            throw reason;
        }
        return null;
    }

    @Override
    public boolean setBiome(final BlockVector2 arg0, final BiomeType arg1) {
        throw reason;
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        throw reason;
    }

    @Override
    public boolean setBlock(final BlockVector3 arg0, final BlockStateHolder arg1) throws WorldEditException {
        throw reason;
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        throw reason;
    }

    @Nullable
    @Override
    public Entity createEntity(final Location arg0, final BaseEntity arg1) {
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
    public List<? extends Entity> getEntities() {
        throw reason;
    }

    @Override
    public List<? extends Entity> getEntities(final Region arg0) {
        throw reason;
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return BlockVector3.ZERO;
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return BlockVector3.ZERO;
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
        throw reason;
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
    public <B extends BlockStateHolder<B>> int replaceBlocks(Region region, Set<BaseBlock> filter, B replacement) throws MaxChangedBlocksException {
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
    public World getWorld() {
        throw reason;
    }

    @Override
    public int getNearestSurfaceLayer(int x, int z, int y, int minY, int maxY) {
        throw reason;
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, boolean ignoreAir) {
        throw reason;
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY) {
        throw reason;
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax) {
        throw reason;
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax, Mask mask) {
        throw reason;
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax, boolean ignoreAir) {
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
    public void addSchems(Region region, Mask mask, List<ClipboardHolder> clipboards, int rarity, boolean rotate) throws WorldEditException {
        throw reason;
    }

    @Override
    public void spawnResource(Region region, Resource gen, int rarity, int frequency) throws WorldEditException {
        throw reason;
    }

    @Override
    public void addOre(Region region, Mask mask, Pattern material, int size, int frequency, int rarity, int minY, int maxY) throws WorldEditException {
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
}
