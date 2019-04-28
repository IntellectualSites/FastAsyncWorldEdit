package com.boydti.fawe.beta.implementation.blocks;

import com.boydti.fawe.beta.IBlocks;

public class FullCharBlocks implements IBlocks {
    public final boolean[] hasSections = new boolean[16];
    public final char[] blocks = new char[65536];
}
