package com.sk89q.worldedit.math.transform;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;

public class RoundedTransform implements Transform{
    private final Transform transform;
//    private MutableBlockVector3 mutable = new MutableBlockVector3();

    public RoundedTransform(Transform transform) {
        this.transform = transform;
    }

    @Override
    public boolean isIdentity() {
        return transform.isIdentity();
    }

    @Override
    public Vector3 apply(Vector3 input) {
        Vector3 val = transform.apply(input);
//        mutable.mutX((int) Math.floor(val.getX() + 0.5));
//        mutable.mutY((int) Math.floor(val.getY() + 0.5));
//        mutable.mutZ((int) Math.floor(val.getZ() + 0.5));
        return Vector3.at(Math.floor(val.getX() + 0.5), Math.floor(val.getY() + 0.5), Math.floor(val.getY() + 0.5));
    }

    @Override
    public RoundedTransform inverse() {
        return new RoundedTransform(transform.inverse());
    }

    @Override
    public RoundedTransform combine(Transform other) {
        return new RoundedTransform(transform.combine(other));
    }

    public Transform getTransform() {
        return transform;
    }
}
