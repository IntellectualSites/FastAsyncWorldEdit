package com.sk89q.worldedit.extent;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.boydti.fawe.jnbt.anvil.generator.GenBase;
import com.boydti.fawe.jnbt.anvil.generator.Resource;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PassthroughExtent extends AbstractDelegateExtent {
    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public PassthroughExtent(Extent extent) {
        super(extent);
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return getExtent().getEntities(region);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return getExtent().getEntities();
    }

    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity entity) {
        return getExtent().createEntity(location, entity);
    }

    @Override
    @Nullable
    public void removeEntity(int x, int y, int z, UUID uuid) {
        getExtent().removeEntity(x, y, z, uuid);
    }

    public void enableQueue() {
        getExtent().enableQueue();
    }

    public void disableQueue() {
        getExtent().disableQueue();
    }

    @Override
    public boolean isWorld() {
        return getExtent().isWorld();
    }

    @Override
    public boolean regenerateChunk(int x, int z, @Nullable BiomeType type, @Nullable Long seed) {
        return getExtent().regenerateChunk(x, z, type, seed);
    }

    @Override
    public int getHighestTerrainBlock(int x, int z, int minY, int maxY) {
        return getExtent().getHighestTerrainBlock(x, z, minY, maxY);
    }

    @Override
    public int getHighestTerrainBlock(int x, int z, int minY, int maxY, Mask filter) {
        return getExtent().getHighestTerrainBlock(x, z, minY, maxY, filter);
    }

    @Override
    public int getNearestSurfaceLayer(int x, int z, int y, int minY, int maxY) {
        return getExtent().getNearestSurfaceLayer(x, z, y, minY, maxY);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, boolean ignoreAir) {
        return getExtent().getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, ignoreAir);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY) {
        return getExtent().getNearestSurfaceTerrainBlock(x, z, y, minY, maxY);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax) {
        return getExtent().getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax, Mask mask) {
        return getExtent().getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax, mask);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax, boolean ignoreAir) {
        return getExtent().getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax, ignoreAir);
    }

    @Override
    public void addCaves(Region region) throws WorldEditException {
        getExtent().addCaves(region);
    }

    @Override
    public void generate(Region region, GenBase gen) throws WorldEditException {
        getExtent().generate(region, gen);
    }

    @Override
    public void addSchems(Region region, Mask mask, List<ClipboardHolder> clipboards, int rarity, boolean rotate) throws WorldEditException {
        getExtent().addSchems(region, mask, clipboards, rarity, rotate);
    }

    @Override
    public void spawnResource(Region region, Resource gen, int rarity, int frequency) throws WorldEditException {
        getExtent().spawnResource(region, gen, rarity, frequency);
    }

    @Override
    public boolean contains(BlockVector3 pt) {
        return getExtent().contains(pt);
    }

    @Override
    public void addOre(Region region, Mask mask, Pattern material, int size, int frequency, int rarity, int minY, int maxY) throws WorldEditException {
        getExtent().addOre(region, mask, material, size, frequency, rarity, minY, maxY);
    }

    @Override
    public void addOres(Region region, Mask mask) throws WorldEditException {
        getExtent().addOres(region, mask);
    }

    @Override
    public List<Countable<BlockType>> getBlockDistribution(Region region) {
        return getExtent().getBlockDistribution(region);
    }

    @Override
    public List<Countable<BlockState>> getBlockDistributionWithData(Region region) {
        return getExtent().getBlockDistributionWithData(region);
    }

    @Override
    @Nullable
    public Operation commit() {
        return getExtent().commit();
    }

    @Override
    public int getMaxY() {
        return getExtent().getMaxY();
    }

    @Override
    public BlockArrayClipboard lazyCopy(Region region) {
        return getExtent().lazyCopy(region);
    }

    @Override
    public int countBlocks(Region region, Set<BaseBlock> searchBlocks) {
        return getExtent().countBlocks(region, searchBlocks);
    }

    @Override
    public int countBlocks(Region region, Mask searchMask) {
        return getExtent().countBlocks(region, searchMask);
    }

    @Override
    public <B extends BlockStateHolder<B>> int setBlocks(Region region, B block) throws MaxChangedBlocksException {
        return getExtent().setBlocks(region, block);
    }

    @Override
    public int setBlocks(Region region, Pattern pattern) throws MaxChangedBlocksException {
        return getExtent().setBlocks(region, pattern);
    }

    @Override
    public <B extends BlockStateHolder<B>> int replaceBlocks(Region region, Set<BaseBlock> filter, B replacement) throws MaxChangedBlocksException {
        return getExtent().replaceBlocks(region, filter, replacement);
    }

    @Override
    public int replaceBlocks(Region region, Set<BaseBlock> filter, Pattern pattern) throws MaxChangedBlocksException {
        return getExtent().replaceBlocks(region, filter, pattern);
    }

    @Override
    public int replaceBlocks(Region region, Mask mask, Pattern pattern) throws MaxChangedBlocksException {
        return getExtent().replaceBlocks(region, mask, pattern);
    }

    @Override
    public int center(Region region, Pattern pattern) throws MaxChangedBlocksException {
        return getExtent().center(region, pattern);
    }

    @Override
    public int setBlocks(Set<BlockVector3> vset, Pattern pattern) {
        return getExtent().setBlocks(vset, pattern);
    }

    public BlockState getBlock(BlockVector3 position) {
        return getExtent().getBlock(position);
    }

    public BlockState getBlock(int x, int y, int z) {
        return getExtent().getBlock(x, y, z);
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return getExtent().getFullBlock(position);
    }

    public BaseBlock getFullBlock(int x, int y, int z) {
        return getExtent().getFullBlock(x, y, z);
    }

    @Override
    public BiomeType getBiome(BlockVector2 position) {
        return getExtent().getBiome(position);
    }

    public BiomeType getBiomeType(int x, int z) {
        return getExtent().getBiomeType(x, z);
    }

    @Override
    @Deprecated
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block) throws WorldEditException {
        return getExtent().setBlock(position, block);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) throws WorldEditException {
        return getExtent().setBlock(x, y, z, block);
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        getExtent().setTile(x, y, z, tile);
    }

    @Override
    public boolean setBiome(BlockVector2 position, BiomeType biome) {
        return getExtent().setBiome(position, biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return getExtent().setBiome(x, y, z, biome);
    }
}
