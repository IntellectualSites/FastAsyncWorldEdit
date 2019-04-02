package com.boydti.fawe.object.extent;

import com.boydti.fawe.util.ExtentTraverser;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;


import static com.google.common.base.Preconditions.checkNotNull;

public class ResettableExtent extends AbstractDelegateExtent implements Serializable {
    public ResettableExtent(Extent parent) {
        super(parent);
    }

    public final void init(BlockVector3 pos) {
        Extent extent = getExtent();
        if (extent instanceof ResettableExtent && extent != this) {
            ((ResettableExtent) extent).init(pos);
        }
        setOrigin(pos);
    }

    protected void setOrigin(BlockVector3 pos) {

    }

    public ResettableExtent setExtent(Extent extent) {
        checkNotNull(extent);
        Extent next = getExtent();
        if (!(next instanceof NullExtent) && !(next instanceof World) && next instanceof ResettableExtent) {
            ((ResettableExtent) next).setExtent(extent);
        } else {
            new ExtentTraverser(this).setNext(new AbstractDelegateExtent(extent));
        }
        return this;
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        Extent extent = getExtent();
        boolean next = extent instanceof ResettableExtent;
        stream.writeBoolean(next);
        if (next) {
            stream.writeObject(extent);
        }
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        if (stream.readBoolean()) {
            try {
                Field field = AbstractDelegateExtent.class.getDeclaredField("extent");
                ReflectionUtils.setFailsafeFieldValue(field, this, stream.readObject());
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}