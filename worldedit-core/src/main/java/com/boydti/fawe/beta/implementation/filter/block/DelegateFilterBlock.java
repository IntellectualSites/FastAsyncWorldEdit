package com.boydti.fawe.beta.implementation.filter.block;

import com.sk89q.jnbt.CompoundTag;
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
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

public class DelegateFilterBlock extends FilterBlock {

    private final FilterBlock parent;

    public DelegateFilterBlock(FilterBlock parent) {
        this.parent = parent;
    }

    public static BlockVector3 at(double x, double y, double z) {
        return BlockVector3.at(x, y, z);
    }

    public static BlockVector3 at(int x, int y, int z) {
        return BlockVector3.at(x, y, z);
    }

    public static Comparator<BlockVector3> sortByCoordsYzx() {
        return BlockVector3.sortByCoordsYzx();
    }

    @Override
    public Extent getExtent() {
        return parent.getExtent();
    }

    @Override
    public boolean hasNbtData() {
        return parent.hasNbtData();
    }

    @Override
    public void setBiome(BiomeType biome) {
        parent.setBiome(biome);
    }

    @Override
    public int getOrdinal() {
        return parent.getOrdinal();
    }

    @Override
    public void setOrdinal(int ordinal) {
        parent.setOrdinal(ordinal);
    }

    @Override
    public BlockState getBlock() {
        return parent.getBlock();
    }

    @Override
    public void setBlock(BlockState state) {
        parent.setBlock(state);
    }

    @Override
    public BaseBlock getFullBlock() {
        return parent.getFullBlock();
    }

    @Override
    public void setFullBlock(BaseBlock block) {
        parent.setFullBlock(block);
    }

    @Override
    public CompoundTag getNbtData() {
        return parent.getNbtData();
    }

    @Override
    public void setNbtData(@Nullable CompoundTag nbtData) {
        parent.setNbtData(nbtData);
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return parent.getMinimumPoint();
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return parent.getMaximumPoint();
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return parent.getBlock(x, y, z);
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return parent.getFullBlock(x, y, z);
    }

    @Override
    public BlockState getBlockBelow() {
        return parent.getBlockBelow();
    }

    @Override
    public BlockState getBlockAbove() {
        return parent.getBlockAbove();
    }

    @Override
    public BlockState getBlockNorth() {
        return parent.getBlockNorth();
    }

    @Override
    public BlockState getBlockEast() {
        return parent.getBlockEast();
    }

    @Override
    public BlockState getBlockSouth() {
        return parent.getBlockSouth();
    }

    @Override
    public BlockState getBlockWest() {
        return parent.getBlockWest();
    }

    @Override
    public BlockState getBlockRelativeY(int y) {
        return parent.getBlockRelativeY(y);
    }

    @Override
    public int getX() {
        return parent.getX();
    }

    @Override
    public int getY() {
        return parent.getY();
    }

    @Override
    public int getZ() {
        return parent.getZ();
    }

    @Override
    public int getLocalX() {
        return parent.getLocalX();
    }

    @Override
    public int getLocalY() {
        return parent.getLocalY();
    }

    @Override
    public int getLocalZ() {
        return parent.getLocalZ();
    }

    @Override
    public int getChunkX() {
        return parent.getChunkX();
    }

    @Override
    public int getChunkZ() {
        return parent.getChunkZ();
    }

    @Override
    public boolean setOrdinal(Extent orDefault, int ordinal) {
        return parent.setOrdinal(orDefault, ordinal);
    }

    @Override
    public boolean setBlock(Extent orDefault, BlockState state) {
        return parent.setBlock(orDefault, state);
    }

    @Override
    public boolean setFullBlock(Extent orDefault, BaseBlock block) {
        return parent.setFullBlock(orDefault, block);
    }

    @Override
    public boolean setBiome(Extent orDefault, BiomeType biome) {
        return parent.setBiome(orDefault, biome);
    }

    @Override
    public int getOrdinal(Extent orDefault) {
        return parent.getOrdinal(orDefault);
    }

    @Override
    public BlockState getBlock(Extent orDefault) {
        return parent.getBlock(orDefault);
    }

    @Override
    public BaseBlock getFullBlock(Extent orDefault) {
        return parent.getFullBlock(orDefault);
    }

    @Override
    public CompoundTag getNbtData(Extent orDefault) {
        return parent.getNbtData(orDefault);
    }

    @Override
    public BlockState getOrdinalBelow(Extent orDefault) {
        return parent.getOrdinalBelow(orDefault);
    }

    @Override
    public BlockState getStateAbove(Extent orDefault) {
        return parent.getStateAbove(orDefault);
    }

    @Override
    public BlockState getStateRelativeY(Extent orDefault, int y) {
        return parent.getStateRelativeY(orDefault, y);
    }

    @Override
    public MutableBlockVector3 setComponents(double x, double y, double z) {
        return parent.setComponents(x, y, z);
    }

    @Override
    public MutableBlockVector3 setComponents(int x, int y, int z) {
        return parent.setComponents(x, y, z);
    }

    @Override
    public MutableBlockVector3 mutX(double x) {
        return parent.mutX(x);
    }

    @Override
    public MutableBlockVector3 mutY(double y) {
        return parent.mutY(y);
    }

    @Override
    public MutableBlockVector3 mutZ(double z) {
        return parent.mutZ(z);
    }

    @Override
    public MutableBlockVector3 mutX(int x) {
        return parent.mutX(x);
    }

    @Override
    public MutableBlockVector3 mutY(int y) {
        return parent.mutY(y);
    }

    @Override
    public MutableBlockVector3 mutZ(int z) {
        return parent.mutZ(z);
    }

    @Override
    public BlockVector3 toImmutable() {
        return parent.toImmutable();
    }

//    @Override
//    public BlockVector3 north() {
//        return parent.north();
//    }
//
//    @Override
//    public BlockVector3 east() {
//        return parent.east();
//    }
//
//    @Override
//    public BlockVector3 south() {
//        return parent.south();
//    }
//
//    @Override
//    public BlockVector3 west() {
//        return parent.west();
//    }

    @Override
    public int getBlockX() {
        return parent.getBlockX();
    }

    @Override
    public BlockVector3 withX(int x) {
        return parent.withX(x);
    }

    @Override
    public int getBlockY() {
        return parent.getBlockY();
    }

    @Override
    public BlockVector3 withY(int y) {
        return parent.withY(y);
    }

    @Override
    public int getBlockZ() {
        return parent.getBlockZ();
    }

    @Override
    public BlockVector3 withZ(int z) {
        return parent.withZ(z);
    }

    @Override
    public BlockVector3 add(BlockVector3 other) {
        return parent.add(other);
    }

    @Override
    public BlockVector3 add(int x, int y, int z) {
        return parent.add(x, y, z);
    }

    @Override
    public BlockVector3 add(BlockVector3... others) {
        return parent.add(others);
    }

    @Override
    public BlockVector3 subtract(BlockVector3 other) {
        return parent.subtract(other);
    }

    @Override
    public BlockVector3 subtract(int x, int y, int z) {
        return parent.subtract(x, y, z);
    }

    @Override
    public BlockVector3 subtract(BlockVector3... others) {
        return parent.subtract(others);
    }

    @Override
    public BlockVector3 multiply(BlockVector3 other) {
        return parent.multiply(other);
    }

    @Override
    public BlockVector3 multiply(int x, int y, int z) {
        return parent.multiply(x, y, z);
    }

    @Override
    public BlockVector3 multiply(BlockVector3... others) {
        return parent.multiply(others);
    }

    @Override
    public BlockVector3 multiply(int n) {
        return parent.multiply(n);
    }

    @Override
    public BlockVector3 divide(BlockVector3 other) {
        return parent.divide(other);
    }

    @Override
    public BlockVector3 divide(int x, int y, int z) {
        return parent.divide(x, y, z);
    }

    @Override
    public BlockVector3 divide(int n) {
        return parent.divide(n);
    }

    @Override
    public BlockVector3 shr(int x, int y, int z) {
        return parent.shr(x, y, z);
    }

    @Override
    public BlockVector3 shr(int n) {
        return parent.shr(n);
    }

    @Override
    public BlockVector3 shl(int x, int y, int z) {
        return parent.shl(x, y, z);
    }

    @Override
    public BlockVector3 shl(int n) {
        return parent.shl(n);
    }

    @Override
    public double length() {
        return parent.length();
    }

    @Override
    public int lengthSq() {
        return parent.lengthSq();
    }

    @Override
    public double distance(BlockVector3 other) {
        return parent.distance(other);
    }

    @Override
    public int distanceSq(BlockVector3 other) {
        return parent.distanceSq(other);
    }

    @Override
    public BlockVector3 normalize() {
        return parent.normalize();
    }

    @Override
    public double dot(BlockVector3 other) {
        return parent.dot(other);
    }

    @Override
    public BlockVector3 cross(BlockVector3 other) {
        return parent.cross(other);
    }

    @Override
    public boolean containedWithin(BlockVector3 min, BlockVector3 max) {
        return parent.containedWithin(min, max);
    }

    @Override
    public BlockVector3 clampY(int min, int max) {
        return parent.clampY(min, max);
    }

    @Override
    public BlockVector3 floor() {
        return parent.floor();
    }

    @Override
    public BlockVector3 ceil() {
        return parent.ceil();
    }

    @Override
    public BlockVector3 round() {
        return parent.round();
    }

    @Override
    public BlockVector3 abs() {
        return parent.abs();
    }

    @Override
    public BlockVector3 transform2D(double angle, double aboutX, double aboutZ, double translateX,
        double translateZ) {
        return parent.transform2D(angle, aboutX, aboutZ, translateX, translateZ);
    }

    @Override
    public double toPitch() {
        return parent.toPitch();
    }

    @Override
    public double toYaw() {
        return parent.toYaw();
    }

    @Override
    public BlockVector3 getMinimum(BlockVector3 v2) {
        return parent.getMinimum(v2);
    }

    @Override
    public BlockVector3 getMaximum(BlockVector3 v2) {
        return parent.getMaximum(v2);
    }

    @Override
    public char getOrdinalChar(Extent orDefault) {
        return parent.getOrdinalChar(orDefault);
    }

    @Override
    public BlockVector2 toBlockVector2() {
        return parent.toBlockVector2();
    }

    @Override
    public Vector3 toVector3() {
        return parent.toVector3();
    }

    @Override
    public boolean equals(Object obj) {
        return parent.equals(obj);
    }

    @Override
    public int hashCode() {
        return parent.hashCode();
    }

    @Override
    public String toString() {
        return parent.toString();
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return parent.getEntities(region);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return parent.getEntities();
    }

    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity entity) {
        return parent.createEntity(location, entity);
    }

    @Override
    @Nullable
    public void removeEntity(int x, int y, int z, UUID uuid) {
        parent.removeEntity(x, y, z, uuid);
    }

    @Override
    public boolean isQueueEnabled() {
        return parent.isQueueEnabled();
    }

    @Override
    public void enableQueue() {
        parent.enableQueue();
    }

    @Override
    public void disableQueue() {
        parent.disableQueue();
    }

    @Override
    public boolean isWorld() {
        return parent.isWorld();
    }

    @Override
    public boolean regenerateChunk(int x, int z, @Nullable BiomeType type, @Nullable Long seed) {
        return parent.regenerateChunk(x, z, type, seed);
    }

    @Override
    public int getHighestTerrainBlock(int x, int z, int minY, int maxY) {
        return parent.getHighestTerrainBlock(x, z, minY, maxY);
    }

    @Override
    public int getHighestTerrainBlock(int x, int z, int minY, int maxY, Mask filter) {
        return parent.getHighestTerrainBlock(x, z, minY, maxY, filter);
    }

    @Override
    public int getNearestSurfaceLayer(int x, int z, int y, int minY, int maxY) {
        return parent.getNearestSurfaceLayer(x, z, y, minY, maxY);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY,
        boolean ignoreAir) {
        return parent.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, ignoreAir);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY) {
        return parent.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin,
        int failedMax) {
        return parent.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin,
        int failedMax, Mask mask) {
        return parent
            .getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax, mask);
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin,
        int failedMax, boolean ignoreAir) {
        return parent
            .getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax, ignoreAir);
    }

    @Override
    public void addCaves(Region region) throws WorldEditException {
        parent.addCaves(region);
    }

    @Override
    public void generate(Region region, GenBase gen) throws WorldEditException {
        parent.generate(region, gen);
    }

    @Override
    public void addSchems(Region region, Mask mask, List<ClipboardHolder> clipboards, int rarity,
        boolean rotate) throws WorldEditException {
        parent.addSchems(region, mask, clipboards, rarity, rotate);
    }

    @Override
    public void spawnResource(Region region, Resource gen, int rarity, int frequency)
        throws WorldEditException {
        parent.spawnResource(region, gen, rarity, frequency);
    }

    @Override
    public boolean contains(BlockVector3 pt) {
        return parent.contains(pt);
    }

    @Override
    public void addOre(Region region, Mask mask, Pattern material, int size, int frequency,
        int rarity, int minY, int maxY) throws WorldEditException {
        parent.addOre(region, mask, material, size, frequency, rarity, minY, maxY);
    }

    @Override
    public void addOres(Region region, Mask mask) throws WorldEditException {
        parent.addOres(region, mask);
    }

    @Override
    public List<Countable<BlockType>> getBlockDistribution(Region region) {
        return parent.getBlockDistribution(region);
    }

    @Override
    public List<Countable<BlockState>> getBlockDistributionWithData(Region region) {
        return parent.getBlockDistributionWithData(region);
    }

    @Override
    public BlockArrayClipboard lazyCopy(Region region) {
        return parent.lazyCopy(region);
    }

    @Override
    public int countBlocks(Region region, Set<BaseBlock> searchBlocks) {
        return parent.countBlocks(region, searchBlocks);
    }

    @Override
    public int countBlocks(Region region, Mask searchMask) {
        return parent.countBlocks(region, searchMask);
    }

    @Override
    public <B extends BlockStateHolder<B>> int setBlocks(Region region, B block) throws MaxChangedBlocksException {
        return parent.setBlocks(region, block);
    }

    @Override
    public int setBlocks(Region region, Pattern pattern) throws MaxChangedBlocksException {
        return parent.setBlocks(region, pattern);
    }

    @Override
    public <B extends BlockStateHolder<B>> int replaceBlocks(Region region, Set<BaseBlock> filter, B replacement) throws MaxChangedBlocksException {
        return parent.replaceBlocks(region, filter, replacement);
    }

    @Override
    public int replaceBlocks(Region region, Set<BaseBlock> filter, Pattern pattern) throws MaxChangedBlocksException {
        return parent.replaceBlocks(region, filter, pattern);
    }

    @Override
    public int replaceBlocks(Region region, Mask mask, Pattern pattern) throws MaxChangedBlocksException {
        return parent.replaceBlocks(region, mask, pattern);
    }

    @Override
    public int center(Region region, Pattern pattern) throws MaxChangedBlocksException {
        return parent.center(region, pattern);
    }

    @Override
    public int setBlocks(Set<BlockVector3> vset, Pattern pattern) {
        return parent.setBlocks(vset, pattern);
    }

    @Override
    @Nullable
    public Operation commit() {
        return parent.commit();
    }

    @Override
    public boolean cancel() {
        return parent.cancel();
    }

    @Override
    public int getMaxY() {
        return parent.getMaxY();
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return parent.getBlock(position);
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return parent.getFullBlock(position);
    }

    @Override
    public BiomeType getBiome(BlockVector2 position) {
        return parent.getBiome(position);
    }

    @Override
    public BiomeType getBiomeType(int x, int z) {
        return parent.getBiomeType(x, z);
    }

    @Deprecated
    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block)
        throws WorldEditException {
        return parent.setBlock(position, block);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block)
        throws WorldEditException {
        return parent.setBlock(x, y, z, block);
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        return parent.setTile(x, y, z, tile);
    }

    @Override
    public boolean setBiome(BlockVector2 position, BiomeType biome) {
        return parent.setBiome(position, biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return parent.setBiome(x, y, z, biome);
    }

    @Override
    public String getNbtId() {
        return parent.getNbtId();
    }
}
