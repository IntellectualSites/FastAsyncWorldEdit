package com.boydti.fawe.jnbt.anvil.generator;

import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.object.schematic.Schematic;
import com.sk89q.worldedit.math.MutableBlockVector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import java.util.List;
import java.util.Random;

public class SchemGen extends Resource {

    private final Extent extent;
    private final List<ClipboardHolder> clipboards;
    private final boolean randomRotate;
    private final Mask mask;

    private MutableBlockVector mutable = new MutableBlockVector();

    public SchemGen(Mask mask, Extent extent, List<ClipboardHolder> clipboards, boolean randomRotate) {
        this.mask = mask;
        this.extent = extent;
        this.clipboards = clipboards;
        this.randomRotate = randomRotate;
    }

    @Override
    public boolean spawn(Random random, int x, int z) throws WorldEditException {
        mutable.mutX(x);
        mutable.mutZ(z);
        int y = extent.getNearestSurfaceTerrainBlock(x, z, mutable.getBlockY(), 0, 255);
        if (y == -1) return false;
        mutable.mutY(y);
        if (!mask.test(mutable.toBlockVector3())) {
            return false;
        }
        mutable.mutY(y + 1);
        ClipboardHolder holder = clipboards.get(PseudoRandom.random.random(clipboards.size()));
        if (randomRotate) {
            holder.setTransform(new AffineTransform().rotateY(PseudoRandom.random.random(4) * 90));
        }
        Clipboard clipboard = holder.getClipboard();
        Schematic schematic = new Schematic(clipboard);
        Transform transform = holder.getTransform();
        if (transform.isIdentity()) {
            schematic.paste(extent, mutable.toBlockVector3(), false);
        } else {
            schematic.paste(extent, mutable.toBlockVector3(), false, transform);
        }
        mutable.mutY(y);
        return true;
    }
}
