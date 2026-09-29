package com.sk89q.worldedit.function.operation;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.math.MutableVector3;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

import java.util.List;

public class BackwardsExtentBlockCopy extends RegionVisitor implements Operation {

    private final Region region;
    private final Transform transform;
    private final RegionFunction function;
    private final BlockVector3 origin;
    private int affected = 0;

    private final MutableBlockVector3 mutBV3 = new MutableBlockVector3();
    private final MutableVector3 mutV3 = new MutableVector3();

    BackwardsExtentBlockCopy(Region region, BlockVector3 origin, Transform transform, RegionFunction function) {
        super(region, function);
        this.region = region;
        this.transform = transform;
        this.function = function;
        this.origin = origin;
    }

    @Override
    public Operation resume(RunContext run) throws WorldEditException {
        CuboidRegion destRegion = transform(this.transform, this.region);
        Transform inverse = this.transform.inverse();
        for (BlockVector3 pt : destRegion) {
            BlockVector3 copyFrom = transform(inverse, pt);
            if (region.contains(copyFrom)) {
                if (function.apply(pt)) {
                    affected++;
                }
            }
        }
        return null;
    }

    private CuboidRegion transform(Transform transform, Region region) {
        BlockVector3 min = BlockVector3.at(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        BlockVector3 max = BlockVector3.at(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        BlockVector3 pos1 = region.getMinimumPoint();
        BlockVector3 pos2 = region.getMaximumPoint();
        for (int x : new int[]{pos1.x(), pos2.x()}) {
            for (int y : new int[]{pos1.y(), pos2.y()}) {
                for (int z : new int[]{pos1.z(), pos2.z()}) {
                    BlockVector3 pt = transform(transform, BlockVector3.at(x, y, z));
                    min = min.getMinimum(pt);
                    max = max.getMaximum(pt);
                }
            }
        }
        return new CuboidRegion(min, max);
    }

    private BlockVector3 transform(Transform transform, BlockVector3 pt) {
        mutV3.mutX(((pt.x() - origin.x())));
        mutV3.mutY(((pt.y() - origin.y())));
        mutV3.mutZ(((pt.z() - origin.z())));
        Vector3 tmp = transform.apply(mutV3);
        mutBV3.mutX((tmp.getBlockX() + origin.x()));
        mutBV3.mutY((tmp.getBlockY() + origin.y()));
        mutBV3.mutZ((tmp.getBlockZ() + origin.z()));
        return mutBV3;
    }

    public int getAffected() {
        return affected;
    }

    @Override
    public void cancel() {

    }

    @Override
    public void addStatusMessages(List<String> messages) {

    }

}
