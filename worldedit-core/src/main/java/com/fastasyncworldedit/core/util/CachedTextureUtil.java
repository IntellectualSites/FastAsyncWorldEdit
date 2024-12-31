package com.fastasyncworldedit.core.util;

import com.sk89q.worldedit.world.block.BlockType;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.FileNotFoundException;

public class CachedTextureUtil extends DelegateTextureUtil {

    protected final TextureUtil parent;
    protected final transient Int2ObjectOpenHashMap<BlockType> colorBlockMap;
    protected final transient Int2ObjectOpenHashMap<Integer> colorBiomeMap;
    protected final transient Int2ObjectOpenHashMap<BlockType[]> colorLayerMap;

    public CachedTextureUtil(TextureUtil parent) throws FileNotFoundException {
        super(parent);
        this.parent = parent;
        this.colorBlockMap = new Int2ObjectOpenHashMap<>();
        this.colorBiomeMap = new Int2ObjectOpenHashMap<>();
        this.colorLayerMap = new Int2ObjectOpenHashMap<>();
    }

    /**
     * Create a new instance
     *
     * @param parent        parent {@link TextureUtil}
     * @param colorBlockMap color block map to (copy and) use
     * @param colorBiomeMap color biome map to (copy and) use
     * @param colorLayerMap color layer map to (copy and) use
     * @throws FileNotFoundException
     * @since TODO
     */
    protected CachedTextureUtil(
            TextureUtil parent,
            Int2ObjectOpenHashMap<BlockType> colorBlockMap,
            Int2ObjectOpenHashMap<Integer> colorBiomeMap,
            Int2ObjectOpenHashMap<BlockType[]> colorLayerMap
    ) throws FileNotFoundException {
        super(parent);
        this.parent = parent;
        this.colorBlockMap = new Int2ObjectOpenHashMap<>(colorBlockMap);
        this.colorBiomeMap = new Int2ObjectOpenHashMap<>(colorBiomeMap);
        this.colorLayerMap = new Int2ObjectOpenHashMap<>(colorLayerMap);
    }

    @Override
    public TextureUtil fork() {
        try {
            return new CachedTextureUtil(parent.fork(), colorBlockMap, colorBiomeMap, colorLayerMap);
        } catch (FileNotFoundException e) {
            // This should never happen
            throw new RuntimeException(e);
        }
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
