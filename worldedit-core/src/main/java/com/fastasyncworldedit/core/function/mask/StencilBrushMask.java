package com.fastasyncworldedit.core.function.mask;

import com.fastasyncworldedit.core.math.MutableVector3;
import com.fastasyncworldedit.core.math.heightmap.HeightMap;
import com.fastasyncworldedit.core.util.MathMan;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.Transform;

import java.util.concurrent.ThreadLocalRandom;

public class StencilBrushMask extends AbstractExtentMask {

    private final MutableVector3 mutable = new MutableVector3();
    private final EditSession editSession;
    private final Mask solid;
    private final BlockVector3 center;
    private final Transform transform;
    private final int size2;
    private final HeightMap map;
    private final double scale;
    private final int add;
    private final int cutoff;
    private final int maxY;
    private final Pattern pattern;

    public StencilBrushMask(
            EditSession editSession,
            Mask solid,
            BlockVector3 center,
            Transform transform,
            int size2,
            HeightMap map,
            double scale,
            int add,
            int cutoff,
            int maxY,
            Pattern pattern
    ) {
        super(editSession);
        this.editSession = editSession;
        this.solid = solid;
        this.center = center;
        this.transform = transform;
        this.size2 = size2;
        this.map = map;
        this.scale = scale;
        this.add = add;
        this.cutoff = cutoff;
        this.maxY = maxY;
        this.pattern = pattern;
    }

    @Override
    public boolean test(Extent extent, BlockVector3 vector) {
        return test(vector);
    }

    @Override
    public boolean test(BlockVector3 vector) {
        if (solid.test(vector)) {
            int dx = vector.x() - center.x();
            int dy = vector.y() - center.y();
            int dz = vector.z() - center.z();

            Vector3 srcPos = transform.apply(mutable.setComponents(dx, dy, dz));
            dx = MathMan.roundInt(srcPos.x());
            dz = MathMan.roundInt(srcPos.z());

            int distance = dx * dx + dz * dz;
            if (distance > size2 || Math.abs(dx) > 256 || Math.abs(dz) > 256) {
                return false;
            }

            double raise = map.getHeight(dx, dz);
            int val = (int) Math.ceil(raise * scale) + add;
            if (val < cutoff) {
                return true;
            }
            if (val >= 255 || ThreadLocalRandom.current().nextInt(maxY) < val) {
                editSession.setBlock(vector.x(), vector.y(), vector.z(), pattern);
            }
            return true;
        }
        return false;
    }

    @Override
    public Mask copy() {
        return new StencilBrushMask(
                editSession,
                solid.copy(),
                center.toImmutable(),
                transform,
                size2,
                map,
                scale,
                add,
                cutoff,
                maxY,
                pattern
        );
    }

}
