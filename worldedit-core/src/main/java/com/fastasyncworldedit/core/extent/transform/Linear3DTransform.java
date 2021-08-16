package com.fastasyncworldedit.core.extent.transform;

import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;

public class Linear3DTransform extends SelectTransform {

    private final ResettableExtent[] extentsArray;

    /**
     * New instance
     *
     * @param extents list of extents to choose from
     */
    public Linear3DTransform(ResettableExtent[] extents) {
        this.extentsArray = extents;
    }

    @Override
    public ResettableExtent setExtent(Extent extent) {
        super.setExtent(extent);
        for (ResettableExtent cur : extentsArray) {
            cur.setExtent(extent);
        }
        return this;
    }

    @Override
    public AbstractDelegateExtent getExtent(int x, int y, int z) {
        int index = (x + y + z) % extentsArray.length;
        if (index < 0) {
            index += extentsArray.length;
        }
        return extentsArray[index];
    }

    @Override
    public AbstractDelegateExtent getExtent(int x, int z) {
        return getExtent(x, 0, z);
    }

}
