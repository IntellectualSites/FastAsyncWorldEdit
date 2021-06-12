package com.fastasyncworldedit.core.beta.implementation.filter;

import com.fastasyncworldedit.core.beta.FilterBlockMask;
import com.fastasyncworldedit.core.beta.implementation.filter.block.FilterBlock;

import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;

public class ArrayImageMask implements FilterBlockMask {

    private final ThreadLocalRandom random;
    private final boolean white;
    private final BufferedImage image;

    public ArrayImageMask(BufferedImage image, boolean white) {
        this.image = image;
        this.white = white;
        this.random = ThreadLocalRandom.current();
    }

    @Override
    public boolean applyBlock(FilterBlock block) {
        int height = image.getRGB(block.getX(), block.getZ()) & 0xFF;
        return height == 255 || height > 0 && !white && random.nextInt(256) <= height;
    }
}
