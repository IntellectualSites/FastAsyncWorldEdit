package com.boydti.fawe.beta.implementation.blocks;

import com.boydti.fawe.beta.IBlocks;
import com.boydti.fawe.beta.IChunkSet;

public class CharBlocks implements IBlocks {

    public static final Section FULL = new Section() {
        @Override
        public final char[] get(CharBlocks blocks, int layer) {
            return blocks.blocks[layer];
        }
    };
    public static final Section EMPTY = new Section() {
        @Override
        public final char[] get(CharBlocks blocks, int layer) {
            blocks.sections[layer] = FULL;
            char[] arr = blocks.blocks[layer];
            if (arr == null) {
                arr = blocks.blocks[layer] = blocks.load(layer);
            } else {
                blocks.blocks[layer] = blocks.load(layer, arr);
            }
            return arr;
        }
    };
    public final char[][] blocks;
    public final Section[] sections;

    public CharBlocks(CharBlocks other) {
        this.blocks = other.blocks;
        this.sections = other.sections;
    }

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
            if (sections[i] == EMPTY) {
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

    public void reset(int layer) {
        sections[layer] = EMPTY;
    }

    public char[] load(int layer) {
        return new char[4096];
    }

    public char[] load(int layer, char[] data) {
        for (int i = 0; i < 4096; i++) {
            data[i] = 0;
        }
        return data;
    }

    @Override
    public boolean hasSection(int layer) {
        return sections[layer] == FULL;
    }

    public char get(int x, int y, int z) {
        final int layer = y >> 4;
        final int index = (y & 15) << 8 | z << 4 | x;
        return sections[layer].get(this, layer, index);
    }

    public void set(int x, int y, int z, char value) {
        final int layer = y >> 4;
        final int index = (y & 15) << 8 | z << 4 | x;
        set(layer, index, value);
    }

    /*
        Section
     */

    public final char get(int layer, int index) {
        return sections[layer].get(this, layer, index);
    }

    public final void set(int layer, int index, char value) {
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
