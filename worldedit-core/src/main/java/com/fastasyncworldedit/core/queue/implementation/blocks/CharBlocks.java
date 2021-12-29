package com.fastasyncworldedit.core.queue.implementation.blocks;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.queue.IBlocks;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

public abstract class CharBlocks implements IBlocks {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    protected static final Section FULL = new Section() {
        @Override
        public char[] get(CharBlocks blocks, int layer) {
            return blocks.blocks[layer];
        }

        // Ignore aggressive switch here.
        @Override
        public char[] get(CharBlocks blocks, int layer, boolean aggressive) {
            return blocks.blocks[layer];
        }

        @Override
        public boolean isFull() {
            return true;
        }
    };
    protected final Section empty = new Section() {
        @Override
        public char[] get(CharBlocks blocks, int layer) {
            // Defaults to aggressive as it should only be avoided where we know we've reset a chunk during an edit
            return get(blocks, layer, true);
        }

        @Override
        public char[] get(CharBlocks blocks, int layer, boolean aggressive) {
            synchronized (blocks.sectionLocks[layer]) {
                char[] arr = blocks.blocks[layer];
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
                if (blocks.blocks[layer] != null) {
                    blocks.sections[layer] = FULL;
                }
                return arr;
            }
        }

        @Override
        public boolean isFull() {
            return false;
        }
    };
    public char[][] blocks;
    public Section[] sections;
    public Object[] sectionLocks;
    protected int minSectionPosition;
    protected int maxSectionPosition;
    protected int sectionCount;

    /**
     * New instance given initial min/max section indices. Can be negative.
     */
    public CharBlocks(int minSectionPosition, int maxSectionPosition) {
        this.minSectionPosition = minSectionPosition;
        this.maxSectionPosition = maxSectionPosition;
        this.sectionCount = maxSectionPosition - minSectionPosition + 1;
        blocks = new char[sectionCount][];
        sections = new Section[sectionCount];
        sectionLocks = new Object[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            sections[i] = empty;
            sectionLocks[i] = new Object();
        }
    }

    @Override
    public synchronized boolean trim(boolean aggressive) {
        boolean result = true;
        for (int i = 0; i < sectionCount; i++) {
            if (!sections[i].isFull() && blocks[i] != null) {
                blocks[i] = null;
            } else {
                result = false;
            }
        }
        return result;
    }

    @Override
    public boolean trim(boolean aggressive, int layer) {
        boolean result = true;
        synchronized (sectionLocks[layer]) {
            if (!sections[layer].isFull() && blocks[layer] != null) {
                blocks[layer] = null;
            } else {
                result = false;
            }
        }
        return result;
    }

    @Override
    public synchronized IChunkSet reset() {
        for (int i = 0; i < sectionCount; i++) {
            sections[i] = empty;
        }
        return null;
    }

    public void reset(int layer) {
        layer -= minSectionPosition;
        synchronized (sectionLocks[layer]) {
            sections[layer] = empty;
        }
    }

    public char[] update(int layer, char[] data, boolean aggressive) {
        if (data == null) {
            return new char[4096];
        }
        for (int i = 0; i < 4096; i++) {
            data[i] = defaultOrdinal();
        }
        return data;
    }

    // Not synchronized as any subsequent methods called from this class will be, or the section shouldn't appear as loaded anyway.
    @Override
    public boolean hasSection(int layer) {
        layer -= minSectionPosition;
        return layer >= 0 && layer < sections.length && sections[layer].isFull();
    }

    @Override
    public char[] load(int layer) {
        layer -= minSectionPosition;
        synchronized (sectionLocks[layer]) {
            return sections[layer].get(this, layer);
        }
    }

    @Nullable
    @Override
    public char[] loadIfPresent(int layer) {
        if (layer < minSectionPosition || layer > maxSectionPosition) {
            return null;
        }
        layer -= minSectionPosition;
        return sections[layer].isFull() ? blocks[layer] : null;
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

    public char get(int x, int y, int z) {
        int layer = y >> 4;
        final int index = (y & 15) << 8 | z << 4 | x;
        if (layer > maxSectionPosition || layer < minSectionPosition) {
            return defaultOrdinal();
        }
        return get(layer, index);
    }

    /**
     * Default char value to be used when "updating"/resetting data arrays
     */
    protected abstract char defaultOrdinal();

    // Not synchronized as it refers to a synchronized method and includes nothing that requires synchronization
    public void set(int x, int y, int z, char value) {
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

    public final char get(int layer, int index) {
        return sections[layer - minSectionPosition].get(this, layer, index);
    }

    public final void set(int layer, int index, char value) throws ArrayIndexOutOfBoundsException {
        sections[layer - minSectionPosition].set(this, layer, index, value);
    }

    public abstract static class Section {

        abstract char[] get(CharBlocks blocks, int layer);

        abstract char[] get(CharBlocks blocks, int layer, boolean aggressive);

        public abstract boolean isFull();

        public final char get(CharBlocks blocks, int layer, int index) {
            int normalized = layer - blocks.minSectionPosition;
            char[] section = get(blocks, normalized);
            if (section == null) {
                blocks.reset(layer);
                section = blocks.empty.get(blocks, normalized, false);
            }
            return section[index];
        }

        public final synchronized void set(CharBlocks blocks, int layer, int index, char value) {
            layer -= blocks.minSectionPosition;
            get(blocks, layer)[index] = value;
        }

    }

}
