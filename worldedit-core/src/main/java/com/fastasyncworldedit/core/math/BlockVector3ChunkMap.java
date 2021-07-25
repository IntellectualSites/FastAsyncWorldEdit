package com.fastasyncworldedit.core.math;

import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.collection.IAdaptedMap;
import com.sk89q.worldedit.math.BlockVector3;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;

import java.util.Map;

public class BlockVector3ChunkMap<T> implements IAdaptedMap<BlockVector3, T, Integer, T> {

    private final Int2ObjectArrayMap<T> map = new Int2ObjectArrayMap<>();

    @Override
    public Map<Integer, T> getParent() {
        return map;
    }

    @Override
    public Integer adaptKey(BlockVector3 key) {
        return MathMan.tripleBlockCoord(key.getX(), key.getY(), key.getZ());
    }

    @Override
    public BlockVector3 adaptKey2(Integer key) {
        int x = MathMan.untripleBlockCoordX(key);
        int y = MathMan.untripleBlockCoordY(key);
        int z = MathMan.untripleBlockCoordZ(key);
        return MutableBlockVector3.get(x, y, z);
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
