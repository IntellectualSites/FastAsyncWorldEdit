package com.fastasyncworldedit.core.queue.implementation.blocks;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.queue.IBlocks;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.apache.logging.log4j.Logger;

public abstract class CharBlocks implements IBlocks {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    protected static final Section FULL = new Section() {
        @Override
        public final char[] get(CharBlocks blocks, int layer) {
            return blocks.blocks[layer];
        }

        // Ignore aggressive switch here.
        @Override
        public char[] get(CharBlocks blocks, int layer, boolean aggressive) {
            return blocks.blocks[layer];
        }

        @Override
        public final boolean isFull() {
            return true;
        }
    };
    protected final Section empty = new Section() {
        @Override
        public final synchronized char[] get(CharBlocks blocks, int layer) {
            // Defaults to aggressive as it should only be avoided where we know we've reset a chunk during an edit
            return get(blocks, layer, true);
        }

        @Override
        public synchronized char[] get(CharBlocks blocks, int layer, boolean aggressive) {
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

        @Override
        public final boolean isFull() {
            return false;
        }
    };
    public char[][] blocks;
    public Section[] sections;
    protected int minLayer;
    protected int maxLayer;
    protected int layers;

    public CharBlocks(int minLayer, int maxLayer) {
        this.minLayer = minLayer;
        this.maxLayer = maxLayer;
        this.layers = maxLayer - minLayer + 1;
        blocks = new char[layers][];
        sections = new Section[layers];
        for (int i = 0; i < layers; i++) {
            sections[i] = empty;
        }
    }

    @Override
    public synchronized boolean trim(boolean aggressive) {
        boolean result = true;
        for (int i = 0; i < layers; i++) {
            if (!sections[i].isFull() && blocks[i] != null) {
                blocks[i] = null;
            } else {
                result = false;
            }
        }
        return result;
    }

    @Override
    public synchronized boolean trim(boolean aggressive, int layer) {
        boolean result = true;
        if (!sections[layer].isFull() && blocks[layer] != null) {
            blocks[layer] = null;
        } else {
            result = false;
        }
        return result;
    }

    @Override
    public synchronized IChunkSet reset() {
        for (int i = 0; i < layers; i++) {
            sections[i] = empty;
        }
        return null;
    }

    public synchronized void reset(int layer) {
        layer -= minLayer;
        sections[layer] = empty;
    }

    public synchronized char[] update(int layer, char[] data, boolean aggressive) {
        if (data == null) {
            return new char[4096];
        }
        for (int i = 0; i < 4096; i++) {
            data[i] = 0;
        }
        return data;
    }

    // Not synchronized as any subsequent methods called from this class will be, or the section shouldn't appear as loaded anyway.
    @Override
    public boolean hasSection(int layer) {
        layer -= minLayer;
        return sections[layer].isFull();
    }

    @Override
    public char[] load(int layer) {
        layer -= minLayer;
        synchronized (sections[layer]) {
            return sections[layer].get(this, layer);
        }
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return BlockTypesCache.states[get(x, y, z)];
    }

    public char get(int x, int y, int z) {
        final int layer = y >> 4;
        final int index = (y & 15) << 8 | z << 4 | x;
        if (layer > maxLayer || layer < minLayer) {
            return 0;
        }
        return sections[layer].get(this, layer, index);
    }

    // Not synchronized as it refers to a synchronized method and includes nothing that requires synchronization
    public void set(int x, int y, int z, char value) {
        final int layer = y >> 4;
        final int index = (y & 15) << 8 | z << 4 | x;
        try {
            set(layer, index, value);
        } catch (ArrayIndexOutOfBoundsException exception) {
            LOGGER.error("Tried setting block at coordinates (" + x + "," + y + "," + z + ")");
            assert Fawe.imp() != null;
            LOGGER.error("Layer variable was = {}", layer, exception);
        }
    }

    /*
        Section
     */

    public final char get(int layer, int index) {
        layer -= minLayer;
        return sections[layer].get(this, layer, index);
    }

    public synchronized final void set(int layer, int index, char value) throws
            ArrayIndexOutOfBoundsException {
        layer -= minLayer;
        sections[layer].set(this, layer, index, value);
    }

    public abstract static class Section {

        public abstract char[] get(CharBlocks blocks, int layer);

        public abstract char[] get(CharBlocks blocks, int layer, boolean aggressive);

        public abstract boolean isFull();

        public final char get(CharBlocks blocks, int layer, int index) {
            char[] section = get(blocks, layer);
            if (section == null) {
                blocks.reset(layer);
                section = blocks.empty.get(blocks, layer, false);
            }
            return section[index];
        }

        public final void set(CharBlocks blocks, int layer, int index, char value) {
            get(blocks, layer)[index] = value;
        }

    }

}
