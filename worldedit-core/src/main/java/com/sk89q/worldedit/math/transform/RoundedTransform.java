package com.sk89q.worldedit.math.transform;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;

public class RoundedTransform implements Transform{
    private final Transform transform;
    private MutableBlockVector mutable = new MutableBlockVector();

    public RoundedTransform(Transform transform) {
        this.transform = transform;
    }

    @Override
    public boolean isIdentity() {
        return transform.isIdentity();
    }

    @Override
    public Vector apply(Vector input) {
        Vector val = transform.apply(input);
        mutable.mutX((int) Math.floor(val.getX() + 0.5));
        mutable.mutY((int) Math.floor(val.getY() + 0.5));
        mutable.mutZ((int) Math.floor(val.getZ() + 0.5));
        return mutable;
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
