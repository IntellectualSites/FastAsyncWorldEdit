package com.boydti.fawe.util;


import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DelegateTextureUtil extends TextureUtil {
    private final TextureUtil parent;

    public DelegateTextureUtil(TextureUtil parent) throws FileNotFoundException {
        super(parent.getFolder());
        this.parent = parent;
    }

    @Override
    public BlockType getNearestBlock(int color) {
        return parent.getNearestBlock(color);
    }

    @Override
    public BlockType getNearestBlock(BlockType block) {
        return parent.getNearestBlock(block);
    }

    @Override
    public BlockType getNextNearestBlock(int color) {
        return parent.getNextNearestBlock(color);
    }

    @Override
    public BlockType[] getNearestLayer(int color) {
        return parent.getNearestLayer(color);
    }

    @Override
    public BlockType getLighterBlock(BlockType block) {
        return parent.getLighterBlock(block);
    }

    @Override
    public BlockType getDarkerBlock(BlockType block) {
        return parent.getDarkerBlock(block);
    }

    @Override
    public int getColor(BlockType block) {
        return parent.getColor(block);
    }

    @Override
    public boolean getIsBlockCloserThanBiome(int[] blockAndBiomeIdOutput, int color, int biomePriority) {
        return parent.getIsBlockCloserThanBiome(blockAndBiomeIdOutput, color, biomePriority);
    }

    @Override
    public int getBiomeMix(int[] biomeIdsOutput, int color) {
        return parent.getBiomeMix(biomeIdsOutput, color);
    }

    @Override
    public BiomeColor getBiome(int biome) {
        return parent.getBiome(biome);
    }

    @Override
    public BiomeColor getNearestBiome(int color) {
        return parent.getNearestBiome(color);
    }

    @Override
    public File getFolder() {
        return parent.getFolder();
    }

    @Override
    public int combineTransparency(int top, int bottom) {
        return parent.combineTransparency(top, bottom);
    }

    @Override
    public void calculateLayerArrays() {
        parent.calculateLayerArrays();
    }

    @Override
    public void loadModTextures() throws IOException {
        parent.loadModTextures();
    }

    @Override
    public int multiplyColor(int c1, int c2) {
        return parent.multiplyColor(c1, c2);
    }

    @Override
    public BlockType getNearestBlock(BlockType block, boolean darker) {
        return parent.getNearestBlock(block, darker);
    }

    @Override
    public BlockType getNearestBlock(int color, boolean darker) {
        return parent.getNearestBlock(color, darker);
    }

    @Override
    public boolean hasAlpha(int color) {
        return parent.hasAlpha(color);
    }

    @Override
    public long colorDistance(int c1, int c2) {
        return parent.colorDistance(c1, c2);
    }

    @Override
    public long colorDistance(int red1, int green1, int blue1, int c2) {
        return parent.colorDistance(red1, green1, blue1, c2);
    }

    public static int hueDistance(int red1, int green1, int blue1, int red2, int green2, int blue2) {
        return TextureUtil.hueDistance(red1, green1, blue1, red2, green2, blue2);
    }

    @Override
    public long getDistance(BufferedImage image, int c1) {
        return parent.getDistance(image, c1);
    }
}
