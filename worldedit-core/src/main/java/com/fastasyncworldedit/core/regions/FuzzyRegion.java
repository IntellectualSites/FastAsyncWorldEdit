package com.fastasyncworldedit.core.regions;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.math.BlockVectorSet;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.AbstractRegion;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.world.World;

import javax.annotation.Nonnull;
import java.util.Iterator;

public class FuzzyRegion extends AbstractRegion {

    private final Mask mask;
    private final BlockVectorSet set = new BlockVectorSet();
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;
    private Extent extent;

    {
        minX = minY = minZ = Integer.MAX_VALUE;
        maxX = maxY = maxZ = Integer.MIN_VALUE;
    }

    public FuzzyRegion(World world, Extent editSession, Mask mask) {
        super(world);
        this.extent = editSession;
        this.mask = mask;
    }

    public Mask getMask() {
        return mask;
    }

    @Override
    public int getArea() {
        return set.size();
    }

    /**
     * Add to the selection from the given position.
     */
    public void select(BlockVector3 position) {
        RecursiveVisitor search = new RecursiveVisitor(mask, p -> {
            setMinMax(p.x(), p.y(), p.z());
            return true;
        }, 256, extent.getMinY(), extent.getMaxY(), extent);
        search.setVisited(set);
        mask.test(position);
        search.visit(position);
        Operations.completeBlindly(search);
    }

    @Nonnull
    @Override
    public Iterator<BlockVector3> iterator() {
        return set.iterator();
    }

    private void setMinMax(int x, int y, int z) {
        if (x > maxX) {
            maxX = x;
        }
        if (x < minX) {
            minX = x;
        }
        if (z > maxZ) {
            maxZ = z;
        }
        if (z < minZ) {
            minZ = z;
        }
        if (y > maxY) {
            maxY = y;
        }
        if (y < minY) {
            minY = y;
        }
    }

    public final void set(int x, int y, int z) throws RegionOperationException {
        set.add(x, y, z);
        setMinMax(x, y, z);
    }

    @Override
    public boolean contains(int x, int y, int z) {
        return set.contains(x, y, z);
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return BlockVector3.at(minX, minY, minZ);
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return BlockVector3.at(maxX, maxY, maxZ);
    }

    @Override
    public void expand(BlockVector3... changes) throws RegionOperationException {
        throw new RegionOperationException(Caption.of("fawe.error.selection-expand"));
    }

    @Override
    public void contract(BlockVector3... changes) throws RegionOperationException {
        throw new RegionOperationException(Caption.of("fawe.error.selection-contract"));
    }

    @Override
    public boolean contains(BlockVector3 position) {
        return contains(position.x(), position.y(), position.z());
    }

    @Override
    public void shift(BlockVector3 change) throws RegionOperationException {
        throw new RegionOperationException(Caption.of("fawe.error.selection-shift"));
    }

    public void setExtent(EditSession extent) {
        this.extent = extent;
    }

    @Override
    public boolean containsEntireCuboid(int bx, int tx, int by, int ty, int bz, int tz) {
        // TODO optimize (switch from BlockVectorSet to the new bitset)
        return false;
    }

}
