package com.boydti.fawe.beta.implementation.blocks;

import com.boydti.fawe.beta.IBlocks;

public class CharBlocks implements IBlocks {
    public final char[][] blocks;
    public final Section[] sections;

    public CharBlocks() {
        blocks = new char[16][];
        sections = new Section[16];
        for (int i = 0; i < 16; i++) sections[i] = NULL;
    }

    @Override
    public boolean trim(boolean aggressive) {
        boolean result = true;
        for (int i = 0; i < 16; i++) {
            if (sections[i] == NULL) {
                blocks[i] = null;
            } else {
                result = false;
            }
        }
        return result;
    }

    @Override
    public void reset() {
        for (int i = 0; i < 16; i++) sections[i] = NULL;
    }

    public void reset(int layer) {
        sections[layer] = NULL;
    }

    protected char[] load(int layer) {
        return new char[4096];
    }

    protected char[] load(int layer, char[] data) {
        for (int i = 0; i < 4096; i++) data[i] = 0;
        return data;
    }

    @Override
    public boolean hasSection(int layer) {
        return sections[layer] == FULL;
    }

    public char get(int x, int y, int z) {
        int layer = y >> 4;
        int index = ((y & 15) << 8) | (z << 4) | (x & 15);
        return sections[layer].get(this, layer, index);
    }

    public char set(int x, int y, int z, char value) {
        int layer = y >> 4;
        int index = ((y & 15) << 8) | (z << 4) | (x & 15);
        return sections[layer].set(this, layer, index, value);
    }

    /*
        Section
     */

    public static abstract class Section {
        public abstract char[] get(CharBlocks blocks, int layer);

        public final char get(CharBlocks blocks, int layer, int index) {
            return get(blocks, layer)[index];
        }

        public final char set(CharBlocks blocks, int layer, int index, char value) {
            return get(blocks, layer)[index] = value;
        }
    }

    public static final Section NULL = new Section() {
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

    public static final Section FULL = new Section() {
        @Override
        public final char[] get(CharBlocks blocks, int layer) {
            return blocks.blocks[layer];
        }
    };
}
