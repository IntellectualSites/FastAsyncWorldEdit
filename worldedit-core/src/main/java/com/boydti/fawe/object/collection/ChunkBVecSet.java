package com.boydti.fawe.object.collection;

import com.sk89q.worldedit.math.BlockVector3;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class ChunkBVecSet implements Set<BlockVector3> {
    private final int offsetX, offsetZ;
    private final ChunkBitSet set;
    private int size = 0;

    public ChunkBVecSet(int size) {
        this(Integer.MAX_VALUE, Integer.MAX_VALUE, new ChunkBitSet(size));
    }

    public ChunkBVecSet(ChunkBitSet set, int offsetX, int offsetZ) {
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
        this.set = set;
    }

    public ChunkBitSet getBitSet() {
        return set;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        try {
            return contain((BlockVector3) o);
        } catch (ClassCastException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean contain(BlockVector3 obj) {
        return contain(obj.getX(), obj.getY(), obj.getZ());
    }

    public boolean contain(int x, int y, int z) {
        return set.get(x - offsetX, y, z - offsetZ);
    }

    @NotNull
    @Override
    public Iterator<BlockVector3> iterator() {
        return null;
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return null;
    }

    @Override
    public boolean add(BlockVector3 blockVector3) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends BlockVector3> c) {
        return false;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return false;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {

    }
}
