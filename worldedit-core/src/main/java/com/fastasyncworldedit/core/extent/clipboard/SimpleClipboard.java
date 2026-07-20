package com.fastasyncworldedit.core.extent.clipboard;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

public abstract class SimpleClipboard implements Clipboard {

    protected BlockVector3 offset;
    private final BlockVector3 size;
    private final int area;
    private final int volume;
    private BlockVector3 origin;

    /**
     * New {@link SimpleClipboard} instance for the given region with max volume of {@link Integer#MAX_VALUE}. Initial offset
     * is the clipboard minimum point.
     *
     * @param region dimensions of this clipboard instance.
     */
    SimpleClipboard(Region region) {
        this(region.getDimensions(), region.getMinimumPoint());
    }

    /**
     * New {@link SimpleClipboard} instance with max volume of {@link Integer#MAX_VALUE}.
     *
     * @param dimensions dimensions of this clipboard instance.
     * @param offset     initial offset of this clipboard.
     */
    SimpleClipboard(BlockVector3 dimensions, BlockVector3 offset) {
        this(dimensions, offset, Integer.MAX_VALUE);
    }

    /**
     * New {@link SimpleClipboard} instance for the given region with the given maximum volume.
     *
     * @param region  dimensions of this clipboard instance.
     * @param maxSize maximum allowable size of the clipboard implementation. A value of -1 implies infinite volume.
     * @since TODO
     */
    SimpleClipboard(Region region, long maxSize) {
        this(region.getDimensions(), region.getMinimumPoint(), maxSize);
    }

    /**
     * New {@link SimpleClipboard} instance with given maximum volume.
     *
     * @param dimensions dimensions of this clipboard instance.
     * @param offset     initial offset of this clipboard.
     * @param maxSize    maximum allowable size of the clipboard implementation. A value of -1 implies infinite volume.
     * @since TODO
     */
    SimpleClipboard(BlockVector3 dimensions, BlockVector3 offset, long maxSize) {
        this.size = dimensions;
        this.offset = offset;
        long longVolume = (long) getWidth() * (long) getHeight() * (long) getLength();
        if (maxSize != -1 && longVolume >= maxSize) {
            throw new IllegalArgumentException("Dimensions are too large for this clipboard format.");
        }
        this.area = getWidth() * getLength();
        this.volume = (int) longVolume;
        this.origin = BlockVector3.ZERO;
    }

    protected void setOffset(final BlockVector3 offset) {
        this.offset = offset;
    }

    @Override
    public BlockVector3 getOrigin() {
        return origin;
    }

    @Override
    public void setOrigin(BlockVector3 origin) {
        this.origin = origin;
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
        return new CuboidRegion(
                null,
                BlockVector3.ZERO,
                BlockVector3.at(getWidth() - 1, getHeight() - 1, getLength() - 1),
                false
        );
    }

    @Override
    public final BlockVector3 getDimensions() {
        return size;
    }

    @Override
    public final int getWidth() {
        return size.x();
    }

    @Override
    public final int getHeight() {
        return size.y();
    }

    @Override
    public final int getLength() {
        return size.z();
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
