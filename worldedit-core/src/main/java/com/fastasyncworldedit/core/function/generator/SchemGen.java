package com.fastasyncworldedit.core.function.generator;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.math.transform.MutatingOperationTransformHolder;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SchemGen implements Resource {

    private final Extent extent;
    private final List<ClipboardHolder> clipboards;
    private final boolean randomRotate;
    private final Mask mask;
    private final Region region;

    private final MutableBlockVector3 mutable = new MutableBlockVector3();

    /**
     * @deprecated Use {@link SchemGen#SchemGen(Mask, Extent, List, boolean, Region)}
     */
    @Deprecated(forRemoval = true, since = "2.11.1")
    public SchemGen(Mask mask, Extent extent, List<ClipboardHolder> clipboards, boolean randomRotate) {
        this.mask = mask;
        this.extent = extent;
        this.clipboards = clipboards;
        this.randomRotate = randomRotate;
        this.region = null;
    }

    /**
     * New instance. Places a schematic on terrain at a given x,z when appropriate
     *
     * @since 2.11.1
     */
    public SchemGen(Mask mask, Extent extent, List<ClipboardHolder> clipboards, boolean randomRotate, Region region) {
        this.mask = mask;
        this.extent = extent;
        this.clipboards = clipboards;
        this.randomRotate = randomRotate;
        this.region = region;
    }

    private int getY(int x, int z) {
        if (region == null) {
            return extent.getNearestSurfaceTerrainBlock(
                    x,
                    z,
                    mutable.y(),
                    this.extent.getMinY(),
                    this.extent.getMaxY(),
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE
            );
        } else {
            int y = extent.getHighestTerrainBlock(x, z, region.getMinimumY(), region.getMaximumY(), mask);
            if (y == region.getMinimumY() && !extent.getBlock(x, y, z).getMaterial().isMovementBlocker()) {
                return Integer.MIN_VALUE;
            }
            return y;
        }
    }

    @Override
    public boolean spawn(Random random, int x, int z) throws WorldEditException {
        mutable.mutX(x);
        mutable.mutZ(z);
        int y = getY(x, z);
        if (y == Integer.MIN_VALUE || y == Integer.MAX_VALUE) {
            return false;
        }
        mutable.mutY(y);
        if (!mask.test(mutable)) {
            return false;
        }
        mutable.mutY(y + 1);
        ClipboardHolder holder = clipboards.get(ThreadLocalRandom.current().nextInt(clipboards.size()));
        if (randomRotate) {
            holder.setTransform(new AffineTransform().rotateY(ThreadLocalRandom.current().nextInt(4) * 90));
        }
        Clipboard clipboard = holder.getClipboards().size() == 1 ? holder.getClipboard() :
                holder.getClipboards().get(ThreadLocalRandom.current().nextInt(clipboards.size()));
        Transform transform = holder.getTransform();
        if (transform.isIdentity()) {
            clipboard.paste(extent, mutable, false);
        } else {
            clipboard.paste(extent, mutable, false, MutatingOperationTransformHolder.transform(transform, true));
        }
        mutable.mutY(y);
        return true;
    }

}
