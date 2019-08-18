package com.boydti.fawe.beta;

import com.boydti.fawe.beta.implementation.IChunkCache;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
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
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * Delegate for IQueueExtent
 */
public interface IDelegateQueueExtent extends IQueueExtent {

    IQueueExtent getParent();

    @Override
    default boolean isQueueEnabled() {
        return getParent().isQueueEnabled();
    }

    @Override
    default void clearBlockUpdates(Player... players) {
        getParent().clearBlockUpdates(players);
    }

    @Override
    default void sendBlockUpdates(Player... players) {
        getParent().sendBlockUpdates(players);
    }

    @Override
    default void enableQueue() {
        getParent().enableQueue();
    }

    @Override
    default void disableQueue() {
        getParent().disableQueue();
    }

    @Override
    default void init(Extent extent, IChunkCache<IChunkGet> get, IChunkCache<IChunkSet> set) {
        getParent().init(extent, get, set);
    }

    @Override
    default IChunkGet getCachedGet(int x, int z) {
        return getParent().getCachedGet(x, z);
    }

    @Override
    default IChunkSet getCachedSet(int x, int z) {
        return getParent().getCachedSet(x, z);
    }

    @Override
    default IChunk getCachedChunk(int x, int z) {
        return getParent().getCachedChunk(x, z);
    }

    @Override
    default <T extends Future<T>> T submit(IChunk<T> chunk) {
        return getParent().submit(chunk);
    }

    @Override
    default boolean setBlock(int x, int y, int z, BlockStateHolder state) {
        return getParent().setBlock(x, y, z, state);
    }

    @Override
    default boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        return getParent().setTile(x, y, z, tile);
    }

    @Override
    default boolean setBiome(int x, int y, int z, BiomeType biome) {
        return getParent().setBiome(x, y, z, biome);
    }

    @Override
    default BlockState getBlock(int x, int y, int z) {
        return getParent().getBlock(x, y, z);
    }

    @Override
    default BaseBlock getFullBlock(int x, int y, int z) {
        return getParent().getFullBlock(x, y, z);
    }

    @Override
    default BiomeType getBiome(int x, int z) {
        return getParent().getBiome(x, z);
    }

    @Override
    default BlockVector3 getMinimumPoint() {
        return getParent().getMinimumPoint();
    }

    @Override
    default BlockVector3 getMaximumPoint() {
        return getParent().getMaximumPoint();
    }

    @Override
    default IChunk create(boolean isFull) {
        return getParent().create(isFull);
    }

    @Override
    default IChunk wrap(IChunk root) {
        return getParent().wrap(root);
    }

    @Override
    default void flush() {
        getParent().flush();
    }

    @Override
    default ChunkFilterBlock initFilterBlock() {
        return getParent().initFilterBlock();
    }

    @Override
    default int size() {
        return getParent().size();
    }

    @Override
    default boolean isEmpty() {
        return getParent().isEmpty();
    }

    @Override
    default void sendChunk(int chunkX, int chunkZ, int bitMask) {
        getParent().sendChunk(chunkX, chunkZ, bitMask);
    }

    @Override
    default boolean trim(boolean aggressive) {
        return getParent().trim(aggressive);
    }

    @Override
    default void recycle() {
        getParent().recycle();
    }

    @Override
    default List<? extends Entity> getEntities(Region region) {
        return getParent().getEntities(region);
    }

    @Override
    default List<? extends Entity> getEntities() {
        return getParent().getEntities();
    }

    @Override
    @Nullable
    default Entity createEntity(Location location, BaseEntity entity) {
        return getParent().createEntity(location, entity);
    }

    @Override
    @Nullable
    default void removeEntity(int x, int y, int z, UUID uuid) {
        getParent().removeEntity(x, y, z, uuid);
    }

    @Override
    default boolean isWorld() {
        return getParent().isWorld();
    }

    @Override
    default boolean regenerateChunk(int x, int z, @Nullable BiomeType type, @Nullable Long seed) {
        return getParent().regenerateChunk(x, z, type, seed);
    }

    @Override
    default int getHighestTerrainBlock(int x, int z, int minY, int maxY) {
        return getParent().getHighestTerrainBlock(x, z, minY, maxY);
    }

    @Override
    default int getHighestTerrainBlock(int x, int z, int minY, int maxY, Mask filter) {
        return getParent().getHighestTerrainBlock(x, z, minY, maxY, filter);
    }

    @Override
    default int getNearestSurfaceLayer(int x, int z, int y, int minY, int maxY) {
        return getParent().getNearestSurfaceLayer(x, z, y, minY, maxY);
    }

    @Override
    default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, boolean ignoreAir) {
        return getParent().getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, ignoreAir);
    }

    @Override
    default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY) {
        return getParent().getNearestSurfaceTerrainBlock(x, z, y, minY, maxY);
    }

    @Override
    default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax) {
        return getParent().getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax);
    }

    @Override
    default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax, Mask mask) {
        return getParent().getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax, mask);
    }

    @Override
    default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax, boolean ignoreAir) {
        return getParent().getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax, ignoreAir);
    }

    @Override
    default void addCaves(Region region) throws WorldEditException {
        getParent().addCaves(region);
    }

    @Override
    default void generate(Region region, GenBase gen) throws WorldEditException {
        getParent().generate(region, gen);
    }

    @Override
    default void addSchems(Region region, Mask mask, List<ClipboardHolder> clipboards, int rarity, boolean rotate) throws WorldEditException {
        getParent().addSchems(region, mask, clipboards, rarity, rotate);
    }

    @Override
    default void spawnResource(Region region, Resource gen, int rarity, int frequency) throws WorldEditException {
        getParent().spawnResource(region, gen, rarity, frequency);
    }

    @Override
    default boolean contains(BlockVector3 pt) {
        return getParent().contains(pt);
    }

    @Override
    default void addOre(Region region, Mask mask, Pattern material, int size, int frequency, int rarity, int minY, int maxY) throws WorldEditException {
        getParent().addOre(region, mask, material, size, frequency, rarity, minY, maxY);
    }

    @Override
    default void addOres(Region region, Mask mask) throws WorldEditException {
        getParent().addOres(region, mask);
    }

    @Override
    default List<Countable<BlockType>> getBlockDistribution(Region region) {
        return getParent().getBlockDistribution(region);
    }

    @Override
    default List<Countable<BlockState>> getBlockDistributionWithData(Region region) {
        return getParent().getBlockDistributionWithData(region);
    }

    @Override
    @Nullable
    default Operation commit() {
        return getParent().commit();
    }

    @Override
    default boolean cancel() {
        return getParent().cancel();
    }

    @Override
    default int getMaxY() {
        return getParent().getMaxY();
    }

    @Override
    default BlockArrayClipboard lazyCopy(Region region) {
        return getParent().lazyCopy(region);
    }

    @Override
    default int countBlocks(Region region, Set<BaseBlock> searchBlocks) {
        return getParent().countBlocks(region, searchBlocks);
    }

    @Override
    default int countBlocks(Region region, Mask searchMask) {
        return getParent().countBlocks(region, searchMask);
    }

    @Override
    default <B extends BlockStateHolder<B>> int setBlocks(Region region, B block) throws MaxChangedBlocksException {
        return getParent().setBlocks(region, block);
    }

    @Override
    default int setBlocks(Region region, Pattern pattern) throws MaxChangedBlocksException {
        return getParent().setBlocks(region, pattern);
    }

    @Override
    default <B extends BlockStateHolder<B>> int replaceBlocks(Region region, Set<BaseBlock> filter, B replacement) throws MaxChangedBlocksException {
        return getParent().replaceBlocks(region, filter, replacement);
    }

    @Override
    default int replaceBlocks(Region region, Set<BaseBlock> filter, Pattern pattern) throws MaxChangedBlocksException {
        return getParent().replaceBlocks(region, filter, pattern);
    }

    @Override
    default int replaceBlocks(Region region, Mask mask, Pattern pattern) throws MaxChangedBlocksException {
        return getParent().replaceBlocks(region, mask, pattern);
    }

    @Override
    default int center(Region region, Pattern pattern) throws MaxChangedBlocksException {
        return getParent().center(region, pattern);
    }

    @Override
    default int setBlocks(Set<BlockVector3> vset, Pattern pattern) {
        return getParent().setBlocks(vset, pattern);
    }

    @Override
    default BlockState getBlock(BlockVector3 position) {
        return getParent().getBlock(position);
    }

    @Override
    default BaseBlock getFullBlock(BlockVector3 position) {
        return getParent().getFullBlock(position);
    }

    @Override
    default BiomeType getBiome(BlockVector2 position) {
        return getParent().getBiome(position);
    }

    @Override
    default BiomeType getBiomeType(int x, int z) {
        return getParent().getBiomeType(x, z);
    }

    @Override
    @Deprecated
    default <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block) throws WorldEditException {
        return getParent().setBlock(position, block);
    }

    @Override
    default boolean setBiome(BlockVector2 position, BiomeType biome) {
        return getParent().setBiome(position, biome);
    }
}
