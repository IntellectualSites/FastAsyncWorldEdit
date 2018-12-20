package com.sk89q.worldedit.function.operation;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import java.util.List;

public class BackwardsExtentBlockCopy implements Operation {
    private final Region region;
    private final Transform transform;
    private final Extent destination;
    private final Extent source;
    private final RegionFunction function;
    private final Vector origin;

    private Vector mutable = new MutableBlockVector();

    public BackwardsExtentBlockCopy(Extent source, Region region, Extent destination, Vector origin, Transform transform, RegionFunction function) {
        this.source = source;
        this.region = region;
        this.destination = destination;
        this.transform = transform;
        this.function = function;
        this.origin = origin;
    }

    @Override
    public Operation resume(RunContext run) throws WorldEditException {
        CuboidRegion destRegion = transform(this.transform, this.region);
        Transform inverse = this.transform.inverse();
        for (Vector pt : destRegion) {
            Vector copyFrom = transform(inverse, pt);
            if (region.contains(copyFrom)) {
                function.apply(pt);
            }
        }
        return null;
    }

    private CuboidRegion transform(Transform transform, Region region) {
        Vector min = new MutableBlockVector(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        Vector max = new MutableBlockVector(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        Vector pos1 = region.getMinimumPoint();
        Vector pos2 = region.getMaximumPoint();
        for (int x : new int[] { pos1.getBlockX(), pos2.getBlockX() }) {
            for (int y : new int[] { pos1.getBlockY(), pos2.getBlockY() }) {
                for (int z : new int[] { pos1.getBlockZ(), pos2.getBlockZ() }) {
                    Vector pt = transform(transform, new Vector(x, y, z)).toBlockVector();
                    min = Vector.getMinimum(min, pt);
                    max = Vector.getMaximum(max, pt);
                }
            }
        }
        return new CuboidRegion(min, max);
    }

    private Vector transform(Transform transform, Vector pt) {
        mutable.mutX(((pt.getBlockX() - origin.getBlockX())));
        mutable.mutY(((pt.getBlockY() - origin.getBlockY())));
        mutable.mutZ(((pt.getBlockZ() - origin.getBlockZ())));
        Vector tmp = transform.apply(mutable);
        tmp.mutX((tmp.getBlockX() + origin.getBlockX()));
        tmp.mutY((tmp.getBlockY() + origin.getBlockY()));
        tmp.mutZ((tmp.getBlockZ() + origin.getBlockZ()));
        return tmp;
    }

    @Override
    public void cancel() {

    }

    @Override
    public void addStatusMessages(List<String> messages) {

    }
}
