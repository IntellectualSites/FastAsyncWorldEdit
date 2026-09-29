package com.fastasyncworldedit.core.util;


import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockType;

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
    public TextureUtil fork() {
        try {
            return new DelegateTextureUtil(parent.fork());
        } catch (FileNotFoundException e) {
            // This should never happen
            throw new RuntimeException(e);
        }
    }

    @Override
    public BlockType getNearestBlock(int color) {
        return parent.getNearestBlock(color);
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
    public BlockType getLighterBlock(final int color) {
        return parent.getLighterBlock(color);
    }

    @Override
    public BlockType getDarkerBlock(final int color) {
        return parent.getDarkerBlock(color);
    }

    @Override
    public int getColor(BlockType block) {
        return parent.getColor(block);
    }

    @Override
    public int getColor(final BiomeType biome) {
        return parent.getColor(biome);
    }

    @Override
    public boolean getIsBlockCloserThanBiome(char[] blockAndBiomeIdOutput, int color, int biomePriority) {
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
    public void calculateLayerArrays() {
        parent.calculateLayerArrays();
    }

    @Override
    public void loadModTextures() throws IOException {
        parent.loadModTextures();
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

}
