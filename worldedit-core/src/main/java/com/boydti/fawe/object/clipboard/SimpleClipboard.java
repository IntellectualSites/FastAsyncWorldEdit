package com.boydti.fawe.object.clipboard;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

public abstract class SimpleClipboard implements Clipboard {
    private final BlockVector3 size;
    private final int area;
    private final int volume;
    private BlockVector3 origin;

    SimpleClipboard(BlockVector3 dimensions) {
        this.size = dimensions;
        long longVolume = (long) getWidth() * (long) getHeight() * (long) getLength();
        if (longVolume >= Integer.MAX_VALUE >> 2) {
            throw new IllegalArgumentException("Dimensions are too large for this clipboard format.");
        }
        this.area = getWidth() * getLength();
        this.volume = (int) longVolume;
        this.origin = BlockVector3.ZERO;
    }

    SimpleClipboard(Region region) {
        this(region.getDimensions());
    }

    @Override
    public void setOrigin(BlockVector3 offset) {
        this.origin = offset;
    }

    @Override
    public BlockVector3 getOrigin() {
        return origin;
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return BlockVector3.ZERO;
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return size.subtract(BlockVector3.ONE);
    }

    @Override
    public Region getRegion() {
        return new CuboidRegion(BlockVector3.ZERO, BlockVector3.at(getWidth() - 1, getHeight() - 1, getLength() - 1));
    }

    @Override
    public final BlockVector3 getDimensions() {
        return size;
    }

    @Override
    public final int getWidth() {
        return size.getBlockX();
    }

    @Override
    public final int getHeight() {
        return size.getBlockY();
    }

    @Override
    public final int getLength() {
        return size.getBlockZ();
    }

    @Override
    public int getArea() {
        return area;
    }

    @Override
    public int getVolume() {
        return volume;
    }
}
