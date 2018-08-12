package com.boydti.fawe.object.brush.heightmap;

public class AbstractDelegateHeightMap implements HeightMap {

    private final HeightMap parent;

    public AbstractDelegateHeightMap(HeightMap parent) {
        this.parent = parent;
    }

    @Override
    public double getHeight(int x, int z) {
        return parent.getHeight(x, z);
    }

    @Override
    public void setSize(int size) {
        parent.setSize(size);
    }
}
