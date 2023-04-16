package com.fastasyncworldedit.core.queue.implementation.blocks;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.queue.IBlocks;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

public abstract class DataArrayBlocks implements IBlocks {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    protected static final Section FULL = new Section() {
        @Override
        public DataArray get(DataArrayBlocks blocks, int layer, DataArray arr) {
            return arr;
        }

        // Ignore aggressive switch here.
        @Override
        public DataArray get(DataArrayBlocks blocks, int layer, DataArray arr, boolean aggressive) {
            return arr;
        }
    };
    protected static final Section EMPTY = new Section() {
        @Override
        public DataArray get(DataArrayBlocks blocks, int layer, DataArray arr) {
            // Defaults to aggressive as it should only be avoided where we know we've reset a chunk during an edit
            return get(blocks, layer, arr, true);
        }

        @Override
        public DataArray get(DataArrayBlocks blocks, int layer, DataArray arr, boolean aggressive) {
            synchronized (blocks.sectionLocks[layer]) {
                return update(blocks, layer, aggressive);
            }
        }
    };
    public DataArray[] blocks;
    public Object[] sectionLocks;
    protected int minSectionPosition;
    protected int maxSectionPosition;
    protected int sectionCount;
    private int chunkX;
    private int chunkZ;

    /**
     * New instance given initial min/max section indices. Can be negative.
     */
    public DataArrayBlocks(int minSectionPosition, int maxSectionPosition) {
        this.minSectionPosition = minSectionPosition;
        this.maxSectionPosition = maxSectionPosition;
        this.sectionCount = maxSectionPosition - minSectionPosition + 1;
        blocks = new DataArray[sectionCount];
        sectionLocks = new Object[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            sectionLocks[i] = new Object();
        }
    }

    public void init(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    @Override
    public boolean trim(boolean aggressive) {
        for (int i = 0; i < sectionCount; i++) {
            synchronized (sectionLocks[i]) {
                if (blocks[i] != null) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean trim(boolean aggressive, int layer) {
        synchronized (sectionLocks[layer]) {
            return blocks[layer] == null;
        }
    }

    @Override
    public IChunkSet reset() {
        for (int i = 0; i < sectionCount; i++) {
            synchronized (sectionLocks[i]) {
                blocks[i] = null;
            }
        }
        return null;
    }

    public void reset(int layer) {
        layer -= minSectionPosition;
        synchronized (sectionLocks[layer]) {
            blocks[layer] = null;
        }
    }

    public DataArray update(int layer, DataArray data, boolean aggressive) {
        if (data == null) {
            return DataArray.createEmpty();
        }
        data.setAll(defaultOrdinal());
        return data;
    }

    @Override
    public boolean hasSection(int layer) {
        layer -= minSectionPosition;
        return layer >= 0 && layer < blocks.length && blocks[layer] != null;
    }

    @Override
    public DataArray load(int layer) {
        layer -= minSectionPosition;
        DataArray data = blocks[layer];
        return (data == null ? EMPTY : FULL).get(this, layer, data);
    }

    @Nullable
    @Override
    public DataArray loadIfPresent(int layer) {
        if (layer < minSectionPosition || layer > maxSectionPosition) {
            return null;
        }
        layer -= minSectionPosition;
        return blocks[layer];
    }

    @Override
    public int getSectionCount() {
        return sectionCount;
    }

    @Override
    public int getMaxSectionPosition() {
        return maxSectionPosition;
    }

    @Override
    public int getMinSectionPosition() {
        return minSectionPosition;
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return BlockTypesCache.states[get(x, y, z)];
    }

    public int get(int x, int y, int z) {
        int layer = y >> 4;
        final int index = (y & 15) << 8 | z << 4 | x;
        if (layer > maxSectionPosition || layer < minSectionPosition) {
            return defaultOrdinal();
        }
        return get(layer, index);
    }

    @Override
    public int getX() {
        return chunkX;
    }

    @Override
    public int getZ() {
        return chunkZ;
    }

    /**
     * Default char value to be used when "updating"/resetting data arrays
     */
    protected abstract char defaultOrdinal();

    // Not synchronized as it refers to a synchronized method and includes nothing that requires synchronization
    public void set(int x, int y, int z, int value) {
        final int layer = y >> 4;
        final int index = (y & 15) << 8 | z << 4 | x;
        try {
            set(layer, index, value);
        } catch (ArrayIndexOutOfBoundsException exception) {
            LOGGER.error("Tried setting block at coordinates (" + x + "," + y + "," + z + ")");
            assert Fawe.platform() != null;
            LOGGER.error("Layer variable was = {}", layer, exception);
        }
    }

    /*
        Section
     */

    public final int get(int layer, int index) {
        DataArray data = blocks[layer - minSectionPosition];
        return (data == null ? EMPTY : FULL).get(this, layer, index, data);
    }

    public final void set(int layer, int index, int value) throws ArrayIndexOutOfBoundsException {
        DataArray data = blocks[layer - minSectionPosition];
        (data == null ? EMPTY : FULL).set(this, layer, index, value, data);
    }

    public abstract static class Section {

        static DataArray update(DataArrayBlocks blocks, int layer, boolean aggressive) {
            DataArray arr = blocks.blocks[layer];
            if (arr == null) {
                arr = blocks.blocks[layer] = blocks.update(layer, null, aggressive);
                if (arr == null) {
                    throw new IllegalStateException("Array cannot be null: " + blocks.getClass());
                }
            } else {
                blocks.blocks[layer] = blocks.update(layer, arr, aggressive);
                if (blocks.blocks[layer] == null) {
                    throw new IllegalStateException("Array cannot be null (update): " + blocks.getClass());
                }
            }
            return arr;
        }

        abstract DataArray get(DataArrayBlocks blocks, int layer, DataArray data);

        abstract DataArray get(DataArrayBlocks blocks, int layer, DataArray data, boolean aggressive);

        public final int get(DataArrayBlocks blocks, int layer, int index, DataArray data) {
            int normalized = layer - blocks.minSectionPosition;
            DataArray section = get(blocks, normalized, data);
            return section.getAt(index);
        }

        public final void set(DataArrayBlocks blocks, int layer, int index, int value, DataArray data) {
            layer -= blocks.minSectionPosition;
            get(blocks, layer, data).setAt(index, value);
        }

    }

    static BiomeType getBiomeType(
            final int x,
            final int y,
            final int z,
            final BiomeType[][] biomes,
            final int minSectionPosition,
            final int maxSectionPosition
    ) {
        int layer;
        if (biomes == null || (y >> 4) < minSectionPosition || (y >> 4) > maxSectionPosition) {
            return null;
        } else if (biomes[(layer = (y >> 4) - minSectionPosition)] == null) {
            return null;
        }
        return biomes[layer][(y & 15) >> 2 | (z >> 2) << 2 | x >> 2];
    }

}
