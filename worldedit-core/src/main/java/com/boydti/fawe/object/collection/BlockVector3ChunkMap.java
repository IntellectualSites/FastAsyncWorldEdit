package com.boydti.fawe.object.collection;

import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import it.unimi.dsi.fastutil.shorts.Short2ObjectArrayMap;
import java.util.Map;

public class BlockVector3ChunkMap<T> implements IAdaptedMap<BlockVector3, T, Short, T> {
    private final Short2ObjectArrayMap<T> map = new Short2ObjectArrayMap<>();

    @Override
    public Map<Short, T> getParent() {
        return map;
    }

    @Override
    public Short adaptKey(BlockVector3 key) {
        return MathMan.tripleBlockCoord(key.getX(), key.getY(), key.getZ());
    }

    @Override
    public BlockVector3 adaptKey2(Short key) {
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
        short key = MathMan.tripleBlockCoord(x, y, z);
        return map.put(key, value);
    }


    public T get(int x, int y, int z) {
        short key = MathMan.tripleBlockCoord(x, y, z);
        return map.get(key);
    }

    public T remove(int x, int y, int z) {
        short key = MathMan.tripleBlockCoord(x, y, z);
        return map.remove(key);
    }

    public boolean contains(int x, int y, int z) {
        short key = MathMan.tripleBlockCoord(x, y, z);
        return map.containsKey(key);
    }
}
