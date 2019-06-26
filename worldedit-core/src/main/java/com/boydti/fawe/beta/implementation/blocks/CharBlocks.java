package com.boydti.fawe.beta.implementation.blocks;

import com.boydti.fawe.beta.IBlocks;
import com.boydti.fawe.beta.IChunkSet;

public class CharBlocks implements IBlocks {
    public final char[][] blocks;
    public final Section[] sections;

    public CharBlocks() {
        blocks = new char[16][];
        sections = new Section[16];
        for (int i = 0; i < 16; i++) sections[i] = EMPTY;
    }

    @Override
    public boolean trim(final boolean aggressive) {
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
        for (int i = 0; i < 16; i++) sections[i] = EMPTY;
        return null;
    }

    public void reset(final int layer) {
        sections[layer] = EMPTY;
    }

    public char[] load(final int layer) {
        return new char[4096];
    }

    public char[] load(final int layer, final char[] data) {
        for (int i = 0; i < 4096; i++) data[i] = 0;
        return data;
    }

    @Override
    public boolean hasSection(final int layer) {
        return sections[layer] == FULL;
    }

    public char get(final int x, final int y, final int z) {
        final int layer = y >> 4;
        final int index = ((y & 15) << 8) | (z << 4) | (x & 15);
        return sections[layer].get(this, layer, index);
    }

    public void set(final int x, final int y, final int z, final char value) {
        final int layer = y >> 4;
        final int index = ((y & 15) << 8) | (z << 4) | (x & 15);
        set(layer, index, value);
    }

    public final char get(final int layer, final int index) {
        return sections[layer].get(this, layer, index);
    }

    public final void set(final int layer, final int index, final char value) {
        sections[layer].set(this, layer, index, value);
    }

    /*
        Section
     */

    public static abstract class Section {
        public abstract char[] get(CharBlocks blocks, int layer);

        public final char get(final CharBlocks blocks, final int layer, final int index) {
            return get(blocks, layer)[index];
        }

        public final void set(final CharBlocks blocks, final int layer, final int index, final char value) {
            get(blocks, layer)[index] = value;
        }
    }

    public static final Section EMPTY = new Section() {
        @Override
        public final char[] get(final CharBlocks blocks, final int layer) {
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
        public final char[] get(final CharBlocks blocks, final int layer) {
            return blocks.blocks[layer];
        }
    };
}
