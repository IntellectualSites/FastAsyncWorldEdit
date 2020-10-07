package com.boydti.fawe.beta.implementation.processors;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.implementation.filter.block.ExtentFilterBlock;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.exception.FaweException;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.generator.GenBase;
import com.sk89q.worldedit.function.generator.Resource;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

public class LimitExtent extends AbstractDelegateExtent {
    private final FaweLimit limit;

    /**
         * Create a new instance.
         *
         * @param extent the extent
         */
    public LimitExtent(Extent extent, FaweLimit limit) {
        super(extent);
        this.limit = limit;
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        limit.THROW_MAX_CHECKS(region.getVolume());
        try {
            return super.getEntities(region);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return Collections.emptyList();
        }
    }

    @Override
    public List<? extends Entity> getEntities() {
        limit.THROW_MAX_CHECKS();
        try {
            return super.getEntities();
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return Collections.emptyList();
        }
    }

    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity entity) {
        limit.THROW_MAX_CHANGES();
        limit.THROW_MAX_ENTITIES();
        try {
            return super.createEntity(location, entity);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return null;
        }
    }

    @Override
    public void removeEntity(int x, int y, int z, UUID uuid) {
        limit.THROW_MAX_CHANGES();
        limit.THROW_MAX_ENTITIES();
        try {
            super.removeEntity(x, y, z, uuid);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
        }
    }

    @Override
    public boolean regenerateChunk(int x, int z, @Nullable BiomeType type, @Nullable Long seed) {
        limit.THROW_MAX_CHANGES(Character.MAX_VALUE);
        try {
            return super.regenerateChunk(x, z, type, seed);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return false;
        }
    }

    @Override
    public int getHighestTerrainBlock(int x, int z, int minY, int maxY) {
        limit.THROW_MAX_CHECKS(FaweCache.IMP.WORLD_HEIGHT);
        try {
            return super.getHighestTerrainBlock(x, z, minY, maxY);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return 0;
        }
    }

    @Override
    public int getHighestTerrainBlock(int x, int z, int minY, int maxY, Mask filter) {
        limit.THROW_MAX_CHECKS(FaweCache.IMP.WORLD_HEIGHT);
        try {
            return super.getHighestTerrainBlock(x, z, minY, maxY, filter);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return 0;
        }
    }

    @Override
    public int getNearestSurfaceLayer(int x, int z, int y, int minY, int maxY) {
        limit.THROW_MAX_CHECKS(FaweCache.IMP.WORLD_HEIGHT);
        try {
            return super.getNearestSurfaceLayer(x, z, y, minY, maxY);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return 0;
        }
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, boolean ignoreAir) {
        limit.THROW_MAX_CHECKS(FaweCache.IMP.WORLD_HEIGHT);
        try {
            return super.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, ignoreAir);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return 0;
        }
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY) {
        limit.THROW_MAX_CHECKS(FaweCache.IMP.WORLD_HEIGHT);
        try {
            return super.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return 0;
        }
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax) {
        limit.THROW_MAX_CHECKS(FaweCache.IMP.WORLD_HEIGHT);
        try {
            return super.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return 0;
        }
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax, Mask mask) {
        limit.THROW_MAX_CHECKS(FaweCache.IMP.WORLD_HEIGHT);
        try {
            return super.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax, mask);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return 0;
        }
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax, boolean ignoreAir) {
        limit.THROW_MAX_CHECKS(FaweCache.IMP.WORLD_HEIGHT);
        try {
            return super.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax, ignoreAir);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return 0;
        }
    }

    @Override
    public void addCaves(Region region) throws WorldEditException {
        limit.THROW_MAX_CHECKS(region.getVolume());
        limit.THROW_MAX_CHANGES(region.getVolume());
        try {
            super.addCaves(region);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
        }
    }

    @Override
    public void generate(Region region, GenBase gen) throws WorldEditException {
        limit.THROW_MAX_CHECKS(region.getVolume());
        limit.THROW_MAX_CHANGES(region.getVolume());
        try {
            super.generate(region, gen);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
        }
    }

    @Override
    public void addSchems(Region region, Mask mask, List<ClipboardHolder> clipboards, int rarity, boolean rotate) throws WorldEditException {
        limit.THROW_MAX_CHECKS(region.getVolume());
        limit.THROW_MAX_CHANGES(region.getVolume());
        try {
            super.addSchems(region, mask, clipboards, rarity, rotate);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
        }
    }

    @Override
    public void spawnResource(Region region, Resource gen, int rarity, int frequency) throws WorldEditException {
        limit.THROW_MAX_CHECKS(region.getVolume());
        limit.THROW_MAX_CHANGES(region.getVolume());
        try {
            super.spawnResource(region, gen, rarity, frequency);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
        }
    }

    @Override
    public void addOre(Region region, Mask mask, Pattern material, int size, int frequency, int rarity, int minY, int maxY) throws WorldEditException {
        limit.THROW_MAX_CHECKS(region.getVolume());
        limit.THROW_MAX_CHANGES(region.getVolume());
        try {
            super.addOre(region, mask, material, size, frequency, rarity, minY, maxY);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
        }
    }

    @Override
    public void addOres(Region region, Mask mask) throws WorldEditException {
        limit.THROW_MAX_CHECKS(region.getVolume());
        limit.THROW_MAX_CHANGES(region.getVolume());
        try {
            super.addOres(region, mask);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
        }
    }

    @Override
    public List<Countable<BlockType>> getBlockDistribution(Region region) {
        limit.THROW_MAX_CHECKS(region.getVolume());
        try {
            return super.getBlockDistribution(region);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return Collections.emptyList();
        }
    }

    @Override
    public List<Countable<BlockState>> getBlockDistributionWithData(Region region) {
        limit.THROW_MAX_CHECKS(region.getVolume());
        try {
            return super.getBlockDistributionWithData(region);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return Collections.emptyList();
        }
    }

    @Override
    public int countBlocks(Region region, Set<BaseBlock> searchBlocks) {
        limit.THROW_MAX_CHECKS(region.getVolume());
        try {
            return super.countBlocks(region, searchBlocks);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return 0;
        }
    }

    @Override
    public int countBlocks(Region region, Mask searchMask) {
        limit.THROW_MAX_CHECKS(region.getVolume());
        try {
            return super.countBlocks(region, searchMask);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return 0;
        }
    }

    @Override
    public <B extends BlockStateHolder<B>> int setBlocks(Region region, B block) throws MaxChangedBlocksException {
        limit.THROW_MAX_CHANGES(region.getVolume());
        try {
            return super.setBlocks(region, block);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return 0;
        }
    }

    @Override
    public int setBlocks(Region region, Pattern pattern) throws MaxChangedBlocksException {
        limit.THROW_MAX_CHANGES(region.getVolume());
        try {
            return super.setBlocks(region, pattern);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return 0;
        }
    }

    @Override
    public <B extends BlockStateHolder<B>> int replaceBlocks(Region region, Set<BaseBlock> filter, B replacement) throws MaxChangedBlocksException {
        limit.THROW_MAX_CHECKS(region.getVolume());
        limit.THROW_MAX_CHANGES(region.getVolume());
        try {
            return super.replaceBlocks(region, filter, replacement);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return 0;
        }
    }

    @Override
    public int replaceBlocks(Region region, Set<BaseBlock> filter, Pattern pattern) throws MaxChangedBlocksException {
        limit.THROW_MAX_CHECKS(region.getVolume());
        limit.THROW_MAX_CHANGES(region.getVolume());
        try {
            return super.replaceBlocks(region, filter, pattern);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return 0;
        }
    }

    @Override
    public int replaceBlocks(Region region, Mask mask, Pattern pattern) throws MaxChangedBlocksException {
        limit.THROW_MAX_CHECKS(region.getVolume());
        limit.THROW_MAX_CHANGES(region.getVolume());
        try {
            return super.replaceBlocks(region, mask, pattern);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return 0;
        }
    }

    @Override
    public int center(Region region, Pattern pattern) throws MaxChangedBlocksException {
        limit.THROW_MAX_CHECKS(region.getVolume());
        limit.THROW_MAX_CHANGES(region.getVolume());
        try {
            return super.center(region, pattern);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return 0;
        }
    }

    @Override
    public int setBlocks(Set<BlockVector3> vset, Pattern pattern) {
        limit.THROW_MAX_CHANGES(vset.size());
        try {
            return super.setBlocks(vset, pattern);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return 0;
        }
    }

    @Override
    public <T extends Filter> T apply(Region region, T filter, boolean full) {
        limit.THROW_MAX_CHECKS(region.getVolume());
        limit.THROW_MAX_CHANGES(region.getVolume());
        try {
            return super.apply(region, filter, full);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return filter;
        }
    }

    @Override
    public <T extends Filter> T apply(Iterable<BlockVector3> positions, T filter) {
        int size;
        if (positions instanceof Collection) {
            size = ((Collection<BlockVector3>) positions).size();
        } else if (positions instanceof Region) {
            BlockVector3 dim = ((Region) positions).getDimensions();
            size = dim.getX() * dim.getY() * dim.getZ();
        } else if (positions instanceof Extent) {
            BlockVector3 min = ((Extent) positions).getMinimumPoint();
            BlockVector3 max = ((Extent) positions).getMinimumPoint();
            BlockVector3 dim = max.subtract(min).add(BlockVector3.ONE);
            size = dim.getX() * dim.getY() * dim.getZ();
        } else {
            ExtentFilterBlock block = new ExtentFilterBlock(this);
            for (BlockVector3 pos : positions) {
                limit.THROW_MAX_CHECKS();
                filter.applyBlock(block.init(pos));
            }
            return filter;
        }
        limit.THROW_MAX_CHECKS(size);
        limit.THROW_MAX_CHANGES(size);
        try {
            return super.apply(positions, filter);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return filter;
        }
    }

    public BlockState getBlock(BlockVector3 position) {
        limit.THROW_MAX_CHECKS();
        try {
            return super.getBlock(position);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return BlockTypes.AIR.getDefaultState();
        }
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        limit.THROW_MAX_CHECKS();
        try {
            return super.getBlock(x, y, z);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return BlockTypes.AIR.getDefaultState();
        }
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        limit.THROW_MAX_CHECKS();
        try {
            return super.getFullBlock(position);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return BlockTypes.AIR.getDefaultState().toBaseBlock();
        }
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        limit.THROW_MAX_CHECKS();
        try {
            return super.getFullBlock(x, y, z);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return BlockTypes.AIR.getDefaultState().toBaseBlock();
        }
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        limit.THROW_MAX_CHECKS();
        try {
            return super.getBiome(position);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return BiomeTypes.FOREST;
        }
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        limit.THROW_MAX_CHECKS();
        try {
            return super.getBiomeType(x, y, z);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return BiomeTypes.FOREST;
        }
    }

    @Override
    @Deprecated
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block) throws WorldEditException {
        limit.THROW_MAX_CHANGES();
        if (block.hasNbtData()) {
            limit.MAX_BLOCKSTATES();
        }
        try {
            return super.setBlock(position, block);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return false;
        }
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) throws WorldEditException {
        limit.THROW_MAX_CHANGES();
        if (block.hasNbtData()) {
            limit.MAX_BLOCKSTATES();
        }
        try {
            return super.setBlock(x, y, z, block);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return false;
        }
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        limit.THROW_MAX_CHANGES();
        limit.MAX_BLOCKSTATES();
        try {
            return super.setTile(x, y, z, tile);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return false;
        }
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        limit.THROW_MAX_CHANGES();
        try {
            return super.setBiome(position, biome);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return false;
        }
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        limit.THROW_MAX_CHANGES();
        try {
            return super.setBiome(x, y, z, biome);
        } catch (FaweException e) {
            if (!limit.MAX_FAILS()) {
                throw e;
            }
            return false;
        }
    }
}
