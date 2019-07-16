package com.boydti.fawe.object.collection;

import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.AbstractRegion;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.world.World;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

public abstract class BlockSet extends AbstractRegion {
    private final int chunkOffsetX;
    private final int chunkOffsetZ;
    private final int blockOffsetX;
    private final int blockOffsetZ;

    public BlockSet(int offsetX, int offsetZ) {
        super(null);
        this.chunkOffsetX = offsetX;
        this.chunkOffsetZ = offsetZ;
        this.blockOffsetX = offsetX << 4;
        this.blockOffsetZ = offsetZ << 4;
    }

    @Override
    public boolean contains(Object o) {
        try {
            return contains((BlockVector3) o);
        } catch (ClassCastException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean contains(BlockVector3 obj) {
        return contains(obj.getX(), obj.getY(), obj.getZ());
    }

    protected final int lowestBit(long bitBuffer) {
        final long lowBit = Long.lowestOneBit(bitBuffer);
        return Long.bitCount(lowBit - 1);
    }

    protected final int highestBit(long bitBuffer) {
        final long lowBit = Long.highestOneBit(bitBuffer);
        return Long.bitCount(lowBit - 1);
    }

    @Override
    public boolean isGlobal() {
        return false;
    }

    public final int getBlockOffsetX() {
        return blockOffsetX;
    }

    public int getBlockOffsetZ() {
        return blockOffsetZ;
    }

    public int getChunkOffsetX() {
        return chunkOffsetX;
    }

    public int getChunkOffsetZ() {
        return chunkOffsetZ;
    }

    @Override
    public boolean add(BlockVector3 p) {
        return add(p.getX(), p.getY(), p.getZ());
    }

    public boolean remove(BlockVector3 p) {
        return remove(p.getX(), p.getY(), p.getZ());
    }

    @Override
    public boolean remove(Object o) {
        try {
            return remove((BlockVector3) o);
        } catch (ClassCastException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public abstract boolean contains(int x, int y, int z);
    public abstract boolean add(int x, int y, int z);
    public abstract void set(int x, int y, int z);
    public abstract void clear(int x, int y, int z);
    public abstract boolean remove(int x, int y, int z);
    public abstract Iterator<BlockVector3> iterator();
    public abstract Set<BlockVector2> getChunks();
    public abstract Set<BlockVector3> getChunkCubes();
    public abstract BlockVector3 getMaximumPoint();
    public abstract BlockVector3 getMinimumPoint();

    @Override
    public void expand(BlockVector3... changes) throws RegionOperationException {

    }

    @Override
    public void contract(BlockVector3... changes) throws RegionOperationException {

    }
}
