package com.boydti.fawe.object.brush.heightmap;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;

public class RotatableHeightMap extends AbstractDelegateHeightMap {
    private AffineTransform transform;
    private final MutableVector3 mutable;

    public RotatableHeightMap(HeightMap parent) {
        super(parent);
        mutable = new MutableVector3();
        this.transform = new AffineTransform();
    }

    public void rotate(double angle) {
        this.transform = transform.rotateY(angle);
    }

    @Override
    public double getHeight(int x, int z) {
        mutable.mutX(x);
        mutable.mutZ(z);
        BlockVector3 pos = transform.apply(mutable.setComponents(x, 0, z)).toBlockPoint();
        return super.getHeight(pos.getBlockX(), pos.getBlockZ());
    }
}
