package com.boydti.fawe.util;

import com.boydti.fawe.FaweCache;

import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.io.FileNotFoundException;
import java.util.Set;

public class FilteredTextureUtil extends TextureUtil {
    private final Set<BlockType> blocks;

    public FilteredTextureUtil(TextureUtil parent, Set<BlockType> blocks) throws FileNotFoundException {
        super(parent.getFolder());
        this.blocks = blocks;
        this.validMixBiomeColors = parent.validMixBiomeColors;
        this.validMixBiomeIds = parent.validMixBiomeIds;
        this.validBiomes = parent.validBiomes;
        this.blockColors = parent.blockColors;
        this.blockDistance = parent.blockDistance;
        this.distances = parent.distances;
        this.validColors = new int[distances.length];
        this.validBlockIds = new int[distances.length];
        int num = 0;
        for (int i = 0; i < parent.validBlockIds.length; i++) {
            BlockTypes block = BlockTypes.get(parent.validBlockIds[i]);
            if (blocks.contains(block)) num++;
        }
        this.validBlockIds = new int[num];
        this.validColors = new int[num];
        num = 0;
        for (int i = 0; i < parent.validBlockIds.length; i++) {
            BlockTypes block = BlockTypes.get(parent.validBlockIds[i]);
            if (blocks.contains(block)) {
                validBlockIds[num] = parent.validBlockIds[i];
                validColors[num++] = parent.validColors[i];
            }
        }
        this.calculateLayerArrays();
    }
}