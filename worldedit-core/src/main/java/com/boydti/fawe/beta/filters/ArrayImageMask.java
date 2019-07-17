package com.boydti.fawe.beta.filters;

import com.boydti.fawe.beta.DelegateFilter;
import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.FilterBlock;
import com.boydti.fawe.beta.FilterBlockMask;

import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;

public class ArrayImageMask implements FilterBlockMask {
    private final ThreadLocalRandom r;
    private final boolean white;
    private final BufferedImage img;

    public ArrayImageMask(BufferedImage img, boolean white) {
        this.img = img;
        this.white = white;
        this.r = ThreadLocalRandom.current();
    }
    @Override
    public boolean applyBlock(FilterBlock block) {
        int height = img.getRGB(block.getX(), block.getZ()) & 0xFF;
        return ((height == 255 || height > 0 && !white && r.nextInt(256) <= height));
    }
}
