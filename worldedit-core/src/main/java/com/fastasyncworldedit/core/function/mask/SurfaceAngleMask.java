package com.fastasyncworldedit.core.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;

import java.util.ArrayList;

public class SurfaceAngleMask extends AbstractExtentMask {

    private final double min;
    private final double max;
    private final int size;

    public SurfaceAngleMask(Extent extent, double min, double max, int size) {
        super(extent);
        this.min = min;
        this.max = max;
        this.size = size;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        if (!vector.getBlock(getExtent()).isAir() && nextToAir(vector)) {
            double angle = 1 - getAverageAirDirection(vector.toVector3(), size).y();
            return (angle >= (min / 90.0) && angle <= (max / 90.0));
        }
        return false;
    }

    @Override
    public boolean test(Extent extent, BlockVector3 vector) {
        if (!vector.getBlock(getExtent()).isAir() && nextToAir(vector)) {
            double angle = 1 - getAverageAirDirection(vector.toVector3(), size).y();
            return (angle >= (min / 90.0) && angle <= (max / 90.0));
        }
        return false;
    }

    private Vector3 getAverageAirDirection(Vector3 currentLocation, int size) {
        ArrayList<Vector3> airDirections = new ArrayList<>();
        for (int i = -size; i <= size; i++) {
            for (int j = -size; j <= size; j++) {
                for (int k = -size; k <= size; k++) {
                    Vector3 block = Vector3.at(currentLocation.x(), currentLocation.y(), currentLocation.z()).add(
                            0.5,
                            0.5,
                            0.5
                    ).add(i, j, k);
                    if (block
                            .toBlockPoint()
                            .clampY(getExtent().getMinY(), getExtent().getMaxY())
                            .getBlock(getExtent())
                            .getMaterial()
                            .isAir()) {
                        airDirections.add(block.subtract(currentLocation.add(0.5, 0.5, 0.5)));
                    }
                }
            }
        }

        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        for (Vector3 vector3 : airDirections) {
            x += vector3.x();
            y += vector3.y();
            z += vector3.z();
        }

        Vector3 averageAirDirection = Vector3.at(x / airDirections.size(), y / airDirections.size(), z / airDirections.size());
        return (Double.isNaN(averageAirDirection.y()) ? Vector3.ZERO : averageAirDirection.normalize());
    }

    @Override
    public Mask copy() {
        return new SurfaceAngleMask(super.getExtent(), min, max, size);
    }

    private boolean nextToAir(BlockVector3 blockVector3) {
        if (getExtent().getBlock(blockVector3.add(1, 0, 0)).toBaseBlock().getMaterial().isAir()) {
            return true;
        }
        if (getExtent().getBlock(blockVector3.add(-1, 0, 0)).toBaseBlock().getMaterial().isAir()) {
            return true;
        }
        if (getExtent().getBlock(blockVector3.add(0, 1, 0)).toBaseBlock().getMaterial().isAir()) {
            return true;
        }
        if (getExtent().getBlock(blockVector3.add(0, -1, 0)).toBaseBlock().getMaterial().isAir()) {
            return true;
        }
        if (getExtent().getBlock(blockVector3.add(0, 0, 1)).toBaseBlock().getMaterial().isAir()) {
            return true;
        }
        return getExtent().getBlock(blockVector3.add(0, 0, -1)).toBaseBlock().getMaterial().isAir();
    }

}
