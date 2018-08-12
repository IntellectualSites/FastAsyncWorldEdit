package com.boydti.fawe.object.brush.heightmap;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.math.transform.AffineTransform;

public class RotatableHeightMap extends AbstractDelegateHeightMap {
    private AffineTransform transform;
    private MutableBlockVector mutable;

    public RotatableHeightMap(HeightMap parent) {
        super(parent);
        mutable = new MutableBlockVector();
        this.transform = new AffineTransform();
    }

    public void rotate(double angle) {
        this.transform = transform.rotateY(angle);
    }

    @Override
    public double getHeight(int x, int z) {
        mutable.mutX(x);
        mutable.mutZ(z);
        Vector pos = transform.apply(mutable.setComponents(x, 0, z));
        return super.getHeight(pos.getBlockX(), pos.getBlockZ());
    }
}