package com.boydti.fawe.jnbt.anvil;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public final class HeightMapMCADrawer {
    private final HeightMapMCAGenerator gen;
    private final TextureUtil tu;
    private final ForkJoinPool pool;

    public HeightMapMCADrawer(HeightMapMCAGenerator generator, TextureUtil textureUtil) {
        this.gen = generator;
        this.tu = textureUtil;
        this.pool = new ForkJoinPool();
    }

    public HeightMapMCADrawer(HeightMapMCAGenerator generator) {
        this(generator, Fawe.get().getCachedTextureUtil(false, 0, 100));
    }

    public BufferedImage draw() {
        BufferedImage img = new BufferedImage(gen.getWidth(), gen.getLength(), BufferedImage.TYPE_INT_RGB);
        final int[] overlay = gen.overlay == null ? gen.floor.get() : gen.overlay.get();
        final int[] floor = gen.floor.get();
        final int[] main = gen.main.get();
        final byte[] heights = gen.heights.get();
        final byte[] biomes = gen.biomes.get();
        final int waterHeight = gen.primtives.waterHeight;
        final int width = gen.getWidth();
        final int length = gen.getLength();

        int[] raw = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

        int parallelism = pool.getParallelism();
        int size = (heights.length + parallelism - 1) / parallelism;
        for (int i = 0; i < parallelism; i++) {
            int start = i * size;
            int end = Math.min(heights.length, start + size);
            pool.submit((Runnable) () -> {
                for (int index = start; index < end; index ++) {
                    int height = (heights[index] & 0xFF);
                    int combined;
                    if ((combined = overlay[index]) == 0) {
                        height--;
                        combined = floor[index];
                        if (BlockTypes.getFromStateId(combined).getMaterial().isAir()) {
                            height--;
                            combined = main[index];
                        }
                    }
                    // draw combined
                    int color;
                    switch (combined >> 4) {
                        case 2:
                            color = getAverageBiomeColor(biomes, width, index);
                            break;
                        case 78:
                            color = (0xDD << 16) + (0xDD << 8) + (0xDD << 0);
                            break;
                        default:
                            color = tu.getColor(BlockTypes.getFromStateId(combined));
                            break;
                    }
                    int slope = getSlope(heights, width, index, height);
                    if (slope != 0) {
                        slope = (slope << 3) + (slope << 2);
                        int r = MathMan.clamp(((color >> 16) & 0xFF) + slope, 0, 255);
                        int g = MathMan.clamp(((color >> 8) & 0xFF) + slope, 0, 255);
                        int b = MathMan.clamp(((color >> 0) & 0xFF) + slope, 0, 255);
                        color = (r << 16) + (g << 8) + (b << 0);
                    }
                    if (height + 1 < waterHeight) {
                        int waterId = gen.primtives.waterId;
                        int waterColor = 0;
                        BlockType waterType = BlockTypes.get(waterId);
                        switch (waterType.getResource().toUpperCase()) {
                            case "WATER":
                                color = tu.averageColor((0x11 << 16) + (0x66 << 8) + (0xCC), color);
                                break;
                            case "LAVA":
                                color = (0xCC << 16) + (0x33 << 8) + (0);
                                break;
                            default:
                                color = tu.getColor(waterType);
                                break;
                        }
                    }
                    raw[index] = color;
                }
            });
        }
        pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        pool.shutdownNow();
        return img;
    }

    private final int getAverageBiomeColor(byte[] biomes, int width, int index) {
        int c0 = tu.getBiome(biomes[index] & 0xFF).grassCombined;
        int c2 = getBiome(biomes, index + 1 + width, index);
        int c1 = getBiome(biomes, index - 1 - width, index);
//        int c3 = getBiome(biomes, index + width, index);
//        int c4 = getBiome(biomes, index - width, index);
        int r = ((c0 >> 16) & 0xFF) + ((c1 >> 16) & 0xFF) + ((c2 >> 16) & 0xFF);// + ((c3 >> 16) & 0xFF) + ((c4 >> 16) & 0xFF);
        int g = ((c0 >> 8) & 0xFF) + ((c1 >> 8) & 0xFF) + ((c2 >> 8) & 0xFF);// + ((c3 >> 8) & 0xFF) + ((c4 >> 8) & 0xFF);
        int b = ((c0) & 0xFF) + ((c1) & 0xFF) + ((c2) & 0xFF);// + ((c3) & 0xFF) + ((c4) & 0xFF);
        r = r * 85 >> 8;
        g = g * 85 >> 8;
        b = b * 85 >> 8;
        return (r << 16) + (g << 8) + (b);
    }

    private final int getBiome(byte[] biomes, int newIndex, int index) {
        if (newIndex < 0 || newIndex >= biomes.length) newIndex = index;
        int biome = biomes[newIndex] & 0xFF;
        return tu.getBiome(biome).grassCombined;
    }

    private int getSlope(byte[] heights, int width, int index, int height) {
        return (
                + getHeight(heights, index + 1, height)
//                + getHeight(heights, index + width, height)
                + getHeight(heights, index + width + 1, height)
                - getHeight(heights, index - 1, height)
//                - getHeight(heights, index - width, height)
                - getHeight(heights, index - width - 1, height)
        );
    }

    private int getHeight(byte[] heights, int index, int height) {
        if (index < 0 || index >= heights.length) return height;
        return heights[index] & 0xFF;
    }
}


