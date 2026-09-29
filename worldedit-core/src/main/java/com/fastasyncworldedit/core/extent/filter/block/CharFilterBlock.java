package com.fastasyncworldedit.core.extent.filter.block;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.queue.FilterBlockMask;
import com.fastasyncworldedit.core.queue.IBlocks;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.implementation.Flood;
import com.fastasyncworldedit.core.queue.implementation.blocks.CharGetBlocks;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.sk89q.worldedit.world.block.BlockTypesCache.states;

@ApiStatus.NonExtendable
public class CharFilterBlock extends ChunkFilterBlock {

    private static final SetDelegate FULL = (block, value) -> block.setArr[block.index] = value;
    private static final SetDelegate NULL = (block, value) -> block.initSet().set(block, value);

    private int maxLayer;
    private int minLayer;
    protected CharGetBlocks get;
    protected IChunkSet set;
    protected char[] getArr;
    @Nullable
    protected char[] setArr;
    protected SetDelegate delegate;
    // local
    protected int layer;
    private int index;
    private int x;
    private int y;
    private int z;
    private int xx;
    private int yy;
    private int zz;
    private int chunkX;
    private int chunkZ;

    public CharFilterBlock(Extent extent) {
        super(extent);
    }

    @Override
    public synchronized final ChunkFilterBlock initChunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.xx = chunkX << 4;
        this.zz = chunkZ << 4;
        return this;
    }

    @Override
    public synchronized final ChunkFilterBlock initLayer(IBlocks iget, IChunkSet iset, int layer) {
        this.get = (CharGetBlocks) iget;
        minLayer = this.get.getMinSectionPosition();
        maxLayer = this.get.getMaxSectionPosition();
        this.layer = layer;
        if (!iget.hasSection(layer)) {
            getArr = FaweCache.INSTANCE.EMPTY_CHAR_4096;
        } else {
            getArr = iget.load(layer);
        }
        this.set = iset;
        if (set.hasSection(layer)) {
            setArr = set.load(layer);
            delegate = FULL;
        } else {
            delegate = NULL;
            setArr = null;
        }
        this.yy = layer << 4;
        return this;
    }

    @Override
    public synchronized void flood(
            IChunkGet iget, IChunkSet iset, int layer, Flood flood,
            FilterBlockMask mask
    ) {
        final int maxDepth = flood.getMaxDepth();
        final boolean checkDepth = maxDepth < Character.MAX_VALUE;
        if (initLayer(iget, iset, layer) != null) { // TODO replace with hasSection
            while ((index = flood.poll()) != -1) {
                x = index & 15;
                z = index >> 4 & 15;
                y = index >> 8 & 15;

                if (mask.applyBlock(this)) {
                    int depth = index >> 12;

                    if (checkDepth && depth > maxDepth) {
                        continue;
                    }

                    flood.apply(x, y, z, depth);
                }
            }
        }
    }

    @Override
    public synchronized void filter(Filter filter, int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.index = x | z << 4 | y << 8;
        filter.applyBlock(this);
    }

    @Override
    public synchronized void filter(Filter filter, int startY, int endY) {
        for (y = startY, index = startY << 8; y <= endY; y++) {
            for (z = 0; z < 16; z++) {
                for (x = 0; x < 16; x++, index++) {
                    filter.applyBlock(this);
                }
            }
        }
    }

    @Override
    public synchronized void filter(Filter filter, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int yis = minY << 8;
        int zis = minZ << 4;
        int zie = (15 - maxZ) << 4;
        int xie = (15 - maxX);
        for (y = minY, index = yis; y <= maxY; y++) {
            index += zis;
            for (z = minZ; z <= maxZ; z++) {
                index += minX;
                for (x = minX; x <= maxX; x++, index++) {
                    filter.applyBlock(this);
                }
                index += xie;
            }
            index += zie;
        }
    }

    @Override
    public synchronized final void filter(Filter filter, Region region) {
        for (y = 0, index = 0; y < 16; y++) {
            int absY = yy + y;
            for (z = 0; z < 16; z++) {
                int absZ = zz + z;
                for (x = 0; x < 16; x++, index++) {
                    int absX = xx + x;
                    if (region.contains(absX, absY, absZ)) {
                        filter.applyBlock(this);
                    }
                }
            }
        }
    }

    @Override
    public synchronized void filter(Filter filter) {
        for (y = 0, index = 0; y < 16; y++) {
            for (z = 0; z < 16; z++) {
                for (x = 0; x < 16; x++, index++) {
                    filter.applyBlock(this);
                }
            }
        }
    }

    @Override
    public void setBiome(BiomeType biome) {
        set.setBiome(x, y, z, biome);
    }

    @Override
    public BiomeType getBiome() {
        return get.getBiomeType(x, y, z);
    }

    @Override
    public final int x() {
        return xx + x;
    }

    @Override
    public final int y() {
        return yy + y;
    }

    @Override
    public final int z() {
        return zz + z;
    }

    @Override
    public final int getLocalX() {
        return x;
    }

    @Override
    public final int getLocalY() {
        return y;
    }

    @Override
    public final int getLocalZ() {
        return z;
    }

    @Override
    public final int getChunkX() {
        return chunkX;
    }

    @Override
    public final int getChunkZ() {
        return chunkZ;
    }

    public final char getOrdinalChar() {
        return getArr[index];
    }

    @Override
    public final int getOrdinal() {
        return getArr[index];
    }

    @Override
    public void setOrdinal(int ordinal) {
        delegate.set(this, (char) ordinal);
    }

    @Override
    public final BlockState getBlock() {
        final int ordinal = getArr[index];
        return BlockTypesCache.states[ordinal];
    }

    @Override
    public void setBlock(BlockState state) {
        delegate.set(this, state.getOrdinalChar());
    }

    @Override
    public final BaseBlock getFullBlock() {
        final BlockState state = getBlock();
        final BlockMaterial material = state.getMaterial();
        if (material.hasContainer()) {
            final FaweCompoundTag tag = get.tile(x, y + yy, z);
            return state.toBaseBlock(tag == null ? null : tag.linTag());
        }
        return state.toBaseBlock();
    }

    @Override
    public void setFullBlock(BaseBlock block) {
        delegate.set(this, block.getOrdinalChar());
        final LazyReference<LinCompoundTag> nbt = block.getNbtReference();
        if (nbt != null) { // TODO optimize check via ImmutableBaseBlock
            set.tile(x, yy + y, z, FaweCompoundTag.of(nbt));
        }
    }

    @Override
    public @Nullable LinCompoundTag getNbt() {
        final FaweCompoundTag tile = get.tile(x, y + yy, z);
        if (tile == null) {
            return null;
        }
        return tile.linTag();
    }

    @Override
    public void setNbt(@Nullable final LinCompoundTag nbtData) {
        if (nbtData != null) {
            set.tile(x, y + yy, z, FaweCompoundTag.of(nbtData));
        }
    }

    /*
    NORTH(Vector3.at(0, 0, -1), Flag.CARDINAL, 3, 1),
    EAST(Vector3.at(1, 0, 0), Flag.CARDINAL, 0, 2),
    SOUTH(Vector3.at(0, 0, 1), Flag.CARDINAL, 1, 3),
    WEST(Vector3.at(-1, 0, 0), Flag.CARDINAL, 2, 0),
     */

    @Override
    public void setNbtReference(@Nullable final LazyReference<LinCompoundTag> nbtData) {
        if (nbtData != null) {
            set.tile(x, y + yy, z, FaweCompoundTag.of(nbtData));
        }
    }

    @Override
    public boolean hasNbtData() {
        final BlockState state = getBlock();
        final BlockMaterial material = state.getMaterial();
        return material.hasContainer();
    }

    @Override
    public final BlockState getBlockNorth() {
        if (z > 0) {
            return states[getArr[index - 16]];
        }
        return getExtent().getBlock(x(), y(), z() - 1);
    }

    @Override
    public final BlockState getBlockEast() {
        if (x < 15) {
            return states[getArr[index + 1]];
        }
        return getExtent().getBlock(x() + 1, y(), z());
    }

    @Override
    public final BlockState getBlockSouth() {
        if (z < 15) {
            return states[getArr[index + 16]];
        }
        return getExtent().getBlock(x(), y(), z() + 1);
    }

    @Override
    public final BlockState getBlockWest() {
        if (x > 0) {
            return states[getArr[index - 1]];
        }
        return getExtent().getBlock(x() - 1, y(), z());
    }

    @Override
    public final BlockState getBlockBelow() {
        if (y > 0) {
            return states[getArr[index - 256]];
        }
        if (layer > minLayer) {
            final int newLayer = layer - 1;
            return states[get.get(newLayer, index + 3840)];
        }
        return BlockTypes.__RESERVED__.getDefaultState();
    }

    @Override
    public final BlockState getBlockAbove() {
        if (y < 16) {
            return states[getArr[index + 256]];
        }
        if (layer < maxLayer) {
            final int newLayer = layer + 1;
            return states[get.get(newLayer, index - 3840)];
        }
        return BlockTypes.__RESERVED__.getDefaultState();
    }

    @Override
    public final BlockState getBlockRelativeY(int y) {
        final int newY = this.y + y;
        final int layerAdd = newY >> 4;
        if (layerAdd == 0) {
            return states[getArr[this.index + (y << 8)]];
        } else if ((layerAdd > 0 && layerAdd < (maxLayer - layer)) || (layerAdd < 0 && layerAdd < (minLayer - layer))) {
            final int newLayer = layer + layerAdd;
            final int index = (this.index + ((y & 15) << 8)) & 4095;
            return states[get.get(newLayer, index)];
        }
        return BlockTypes.__RESERVED__.getDefaultState();
    }

    /*
    Extent
     */
    @Override
    public char getOrdinalChar(Extent orDefault) {
        return getOrdinalChar();
    }

    //Set delegate
    protected final SetDelegate initSet() {
        setArr = set.load(layer);
        return delegate = FULL;
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        if (x >> 4 == chunkX && z >> 4 == chunkZ) {
            return get.getBiomeType(x & 15, y, z & 15);
        }
        return getExtent().getBiomeType(x, y, z);
    }

    @Override
    public BiomeType getBiome(final BlockVector3 position) {
        return this.getBiomeType(position.x(), position.y(), position.z());
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block)
            throws WorldEditException {
        return getExtent().setBlock(x, y, z, block);
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        return setBiome(position.x(), position.y(), position.z(), biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        if (x >> 4 == chunkX && z >> 4 == chunkZ) {
            return set.setBiome(x & 15, y, z & 15, biome);
        }
        return getExtent().setBiome(x, y, z, biome);
    }

    @ApiStatus.Internal
    protected interface SetDelegate {

        void set(@Nonnull CharFilterBlock block, char value);

    }

}
