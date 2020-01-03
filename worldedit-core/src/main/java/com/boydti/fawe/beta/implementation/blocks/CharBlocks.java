package com.boydti.fawe.beta.implementation.blocks;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.IBlocks;
import com.boydti.fawe.beta.IChunkSet;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.jetbrains.annotations.Range;

public abstract class CharBlocks implements IBlocks {

    public static final Section FULL = new Section() {
        @Override
        public final char[] get(CharBlocks blocks, int layer) {
            return blocks.blocks[layer];
        }
    };
    public static final Section EMPTY = new Section() {
        @Override
        public final char[] get(CharBlocks blocks, int layer) {
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
    public boolean trim(boolean aggressive) {
        boolean result = true;
        for (int i = 0; i < 16; i++) {
            if (sections[i] == EMPTY && blocks[i] != null) {
                blocks[i] = null;
            } else {
                result = false;
            }
        }
        return result;
    }

    @Override
    public IChunkSet reset() {
        for (int i = 0; i < 16; i++) {
            sections[i] = EMPTY;
        }
        return null;
    }

    public void reset(@Range(from = 0, to = 15) int layer) {
        sections[layer] = EMPTY;
    }

    public char[] update(int layer, char[] data) {
        if (data == null) {
            return new char[4096];
        }
        for (int i = 0; i < 4096; i++) {
            data[i] = 0;
        }
        return data;
    }

    @Override
    public boolean hasSection(@Range(from = 0, to = 15) int layer) {
        return sections[layer] == FULL;
    }

    @Override
    public char[] load(@Range(from = 0, to = 15) int layer) {
        return sections[layer].get(this, layer);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return BlockTypesCache.states[get(x, y, z)];
    }

    public char get(int x, int y, int z) {
        final int layer = y >> 4;
        final int index = (y & 15) << 8 | z << 4 | x;
        return sections[layer].get(this, layer, index);
    }

    public void set(int x, int y, int z, char value) {
        Fawe.imp().debug("Setting Block at x:" + x + ", y:" + y + " , z:" + z);
        final int layer = y >> 4;
        final int index = (y & 15) << 8 | z << 4 | x;
        set(layer, index, value);
    }

    /*
        Section
     */

    public final char get(@Range(from = 0, to = 15)int layer, int index) {
        return sections[layer].get(this, layer, index);
    }

    public final void set(@Range(from = 0, to = 15) int layer, int index, char value) {
        sections[layer].set(this, layer, index, value);
    }

    public static abstract class Section {

        public abstract char[] get(CharBlocks blocks, int layer);

        public final char get(CharBlocks blocks, int layer, int index) {
            return get(blocks, layer)[index];
        }

        public final void set(CharBlocks blocks, int layer, int index, char value) {
            get(blocks, layer)[index] = value;
        }
    }
}
