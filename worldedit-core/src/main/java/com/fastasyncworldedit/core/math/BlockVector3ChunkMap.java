package com.fastasyncworldedit.core.math;

import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.collection.IAdaptedMap;
import com.sk89q.worldedit.math.BlockVector3;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;

import java.util.Map;

public class BlockVector3ChunkMap<T> implements IAdaptedMap<BlockVector3, T, Integer, T> {

    private final Int2ObjectArrayMap<T> map;

    public BlockVector3ChunkMap() {
        map = new Int2ObjectArrayMap<>();
    }

    /**
     * Create a new instance that is a copy of an existing map
     *
     * @param map existing map to copy
     */
    public BlockVector3ChunkMap(BlockVector3ChunkMap<T> map) {
        this.map = new Int2ObjectArrayMap<>(map.getParent());
    }

    @Override
    public Map<Integer, T> getParent() {
        return map;
    }

    @Override
    public Integer adaptKey(BlockVector3 key) {
        return MathMan.tripleBlockCoord(key.x(), key.y(), key.z());
    }

    @Override
    public BlockVector3 adaptKey2(Integer key) {
        int x = MathMan.untripleBlockCoordX(key);
        int y = MathMan.untripleBlockCoordY(key);
        int z = MathMan.untripleBlockCoordZ(key);
        return BlockVector3.at(x, y, z);
    }

    @Override
    public T adaptValue2(T value) {
        return value;
    }

    @Override
    public T adaptValue(T value) {
        return value;
    }

    public T put(int x, int y, int z, T value) {
        int key = MathMan.tripleBlockCoord(x, y, z);
        return map.put(key, value);
    }


    public T get(int x, int y, int z) {
        int key = MathMan.tripleBlockCoord(x, y, z);
        return map.get(key);
    }

    public T remove(int x, int y, int z) {
        int key = MathMan.tripleBlockCoord(x, y, z);
        return map.remove(key);
    }

    public boolean contains(int x, int y, int z) {
        int key = MathMan.tripleBlockCoord(x, y, z);
        return map.containsKey(key);
    }

}
