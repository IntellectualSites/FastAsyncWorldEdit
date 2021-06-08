package com.boydti.fawe.beta.implementation.blocks;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.IBlocks;
import com.boydti.fawe.beta.IChunkSet;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Range;

public abstract class CharBlocks implements IBlocks {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    protected static final Section FULL = new Section() {
        @Override
        public final char[] get(CharBlocks blocks, int layer) {
            return blocks.blocks[layer];
        }

        @Override
        public final boolean isFull() {
            return true;
        }
    };
    protected final Section EMPTY = new Section() {
        @Override
        public final synchronized char[] get(CharBlocks blocks, int layer) {
            char[] arr = blocks.blocks[layer];
            if (arr == null) {
                arr = blocks.blocks[layer] = blocks.update(layer, null);
                if (arr == null) {
                    throw new IllegalStateException("Array cannot be null: " + blocks.getClass());
                }
            } else {
                blocks.blocks[layer] = blocks.update(layer, arr);
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
    public final char[][] blocks;
    public final Section[] sections;

    public CharBlocks() {
        blocks = new char[16][];
        sections = new Section[16];
        for (int i = 0; i < 16; i++) {
            sections[i] = EMPTY;
        }
    }

    @Override
    public synchronized boolean trim(boolean aggressive) {
        boolean result = true;
        for (int i = 0; i < 16; i++) {
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
        for (int i = 0; i < 16; i++) {
            sections[i] = EMPTY;
        }
        return null;
    }

    public synchronized void reset(@Range(from = 0, to = 15) int layer) {
        sections[layer] = EMPTY;
    }

    public synchronized char[] update(int layer, char[] data) {
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
    public boolean hasSection(@Range(from = 0, to = 15) int layer) {
        return sections[layer].isFull();
    }

    @Override
    public synchronized char[] load(@Range(from = 0, to = 15) int layer) {
        return sections[layer].get(this, layer);
    }

    @Override
    public synchronized BlockState getBlock(int x, int y, int z) {
        return BlockTypesCache.states[get(x, y, z)];
    }

    public synchronized char get(int x, @Range(from = 0, to = 255) int y, int z) {
        final int layer = y >> 4;
        final int index = (y & 15) << 8 | z << 4 | x;
        if (layer >= sections.length || layer < 0) {
            return 0;
        }
        return sections[layer].get(this, layer, index);
    }

    // Not synchronized as it refers to a synchronized method and includes nothing that requires synchronization
    public void set(int x, @Range(from = 0, to = 255) int y, int z, char value) {
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

    public synchronized final char get(@Range(from = 0, to = 15) int layer, int index) {
        return sections[layer].get(this, layer, index);
    }

    public synchronized final void set(@Range(from = 0, to = 15) int layer, int index, char value) throws ArrayIndexOutOfBoundsException {
        sections[layer].set(this, layer, index, value);
    }

    public abstract static class Section {

        public abstract char[] get(CharBlocks blocks, @Range(from = 0, to = 15) int layer);

        public abstract boolean isFull();

        public final char get(CharBlocks blocks, @Range(from = 0, to = 15) int layer, int index) {
            return get(blocks, layer)[index];
        }

        public final void set(CharBlocks blocks, @Range(from = 0, to = 15) int layer, int index, char value) {
            get(blocks, layer)[index] = value;
        }
    }
}
