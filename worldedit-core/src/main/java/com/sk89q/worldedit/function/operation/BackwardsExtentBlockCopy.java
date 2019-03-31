package com.sk89q.worldedit.function.operation;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
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
    private final BlockVector3 origin;

//    private Vector mutable = new MutableBlockVector3();

    public BackwardsExtentBlockCopy(Extent source, Region region, Extent destination, BlockVector3 origin, Transform transform, RegionFunction function) {
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
        for (BlockVector3 pt : destRegion) {
        	BlockVector3 copyFrom = transform(inverse, pt);
            if (region.contains(copyFrom)) {
                function.apply(pt);
            }
        }
        return null;
    }

    private CuboidRegion transform(Transform transform, Region region) {
    	BlockVector3 min = BlockVector3.at(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    	BlockVector3 max = BlockVector3.at(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    	BlockVector3 pos1 = region.getMinimumPoint();
    	BlockVector3 pos2 = region.getMaximumPoint();
        for (int x : new int[] { pos1.getBlockX(), pos2.getBlockX() }) {
            for (int y : new int[] { pos1.getBlockY(), pos2.getBlockY() }) {
                for (int z : new int[] { pos1.getBlockZ(), pos2.getBlockZ() }) {
                	BlockVector3 pt = transform(transform, BlockVector3.at(x, y, z));
                    min = min.getMinimum(pt);
                    max = max.getMaximum(pt);
                }
            }
        }
        return new CuboidRegion(min, max);
    }

    private BlockVector3 transform(Transform transform, BlockVector3 pt) {
//        mutable.mutX(((pt.getBlockX() - origin.getBlockX())));
//        mutable.mutY(((pt.getBlockY() - origin.getBlockY())));
//        mutable.mutZ(((pt.getBlockZ() - origin.getBlockZ())));
//        BlockVector3 tmp = transform.apply(new Vector3(pt.getBlockX() - origin.getBlockX(), pt.getBlockY() - origin.getBlockY(), pt.getBlockZ() - origin.getBlockZ())).toBlockPoint();
//        tmp.mutX((tmp.getBlockX() + origin.getBlockX()));
//        tmp.mutY((tmp.getBlockY() + origin.getBlockY()));
//        tmp.mutZ((tmp.getBlockZ() + origin.getBlockZ()));
//        return tmp;
    	return transform.apply(Vector3.at(pt.getBlockX() - origin.getBlockX(), pt.getBlockY() - origin.getBlockY(), pt.getBlockZ() - origin.getBlockZ())).toBlockPoint().add(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
    }

    @Override
    public void cancel() {

    }

    @Override
    public void addStatusMessages(List<String> messages) {

    }
}
