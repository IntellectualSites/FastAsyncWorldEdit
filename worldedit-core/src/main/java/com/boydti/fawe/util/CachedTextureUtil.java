package com.boydti.fawe.util;

import com.sk89q.worldedit.world.block.BlockType;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.FileNotFoundException;

public class CachedTextureUtil extends DelegateTextureUtil {
    private final TextureUtil parent;
    private transient Int2ObjectOpenHashMap<BlockType> colorBlockMap;
    private transient Int2ObjectOpenHashMap<Integer> colorBiomeMap;
    private transient Int2ObjectOpenHashMap<BlockType[]> colorLayerMap;

    public CachedTextureUtil(TextureUtil parent) throws FileNotFoundException {
        super(parent);
        this.parent = parent;
        this.colorBlockMap = new Int2ObjectOpenHashMap<>();
        this.colorLayerMap = new Int2ObjectOpenHashMap<>();
        this.colorBiomeMap = new Int2ObjectOpenHashMap<>();
    }

    @Override
    public BlockType[] getNearestLayer(int color) {
        BlockType[] closest = colorLayerMap.get(color);
        if (closest != null) {
            return closest;
        }
        closest = parent.getNearestLayer(color);
        if (closest != null) {
            colorLayerMap.put(color, closest.clone());
        }
        return closest;
    }

    @Override
    public BiomeColor getNearestBiome(int color) {
        Integer value = colorBiomeMap.get(color);
        if (value != null) {
            return getBiome(value);
        }
        BiomeColor result = parent.getNearestBiome(color);
        if (result != null) {
            colorBiomeMap.put(color, (Integer) result.id);
        }
        return result;
    }

    @Override
    public BlockType getNearestBlock(int color) {
        BlockType value = colorBlockMap.get(color);
        if (value != null) {
            return value;
        }
        BlockType result = parent.getNearestBlock(color);
        if (result != null) {
            colorBlockMap.put(color, result);
        }
        return result;
    }
}
