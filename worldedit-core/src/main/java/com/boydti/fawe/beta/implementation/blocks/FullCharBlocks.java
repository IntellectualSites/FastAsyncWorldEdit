package com.boydti.fawe.beta.implementation.blocks;

import com.boydti.fawe.beta.IBlocks;

// TODO implement
public class FullCharBlocks implements IBlocks {
    public final boolean[] hasSections = new boolean[16];
    public final char[] blocks = new char[65536];

    @Override
    public boolean hasSection(int layer) {
        return false;
    }

    @Override
    public void reset() {

    }

    @Override
    public boolean trim(boolean aggressive) {
        return false;
    }
}