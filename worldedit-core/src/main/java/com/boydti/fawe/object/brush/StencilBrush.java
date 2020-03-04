package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.brush.heightmap.HeightMap;
import com.boydti.fawe.object.mask.AdjacentAnyMask;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.util.Location;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class StencilBrush extends HeightBrush {
    private final boolean onlyWhite;

    public StencilBrush(InputStream stream, int rotation, double yscale, boolean onlyWhite, Clipboard clipboard) {
        super(stream, rotation, yscale, false, true, clipboard);
        this.onlyWhite = onlyWhite;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double sizeDouble) throws MaxChangedBlocksException {
        final int cx = position.getBlockX();
        final int cy = position.getBlockY();
        final int cz = position.getBlockZ();
        int size = (int) sizeDouble;
        int size2 = (int) (sizeDouble * sizeDouble);
        int maxY = editSession.getMaxY();
        int add;
        if (yscale < 0) {
            add = maxY;
        } else {
            add = 0;
        }
        final HeightMap map = getHeightMap();
        map.setSize(size);
        int cutoff = onlyWhite ? maxY : 0;
        final SolidBlockMask solid = new SolidBlockMask(editSession);
        
        Location loc = editSession.getPlayer().getLocation();
        float yaw = loc.getYaw();
        float pitch = loc.getPitch();
        AffineTransform transform = new AffineTransform().rotateY((-yaw) % 360).rotateX(pitch - 90).inverse();

        double scale = (yscale / sizeDouble) * (maxY + 1);
        RecursiveVisitor visitor = new RecursiveVisitor(new AbstractExtentMask(editSession) {
            private final MutableVector3 mutable = new MutableVector3();
            @Override
            public boolean test(Extent extent, BlockVector3 vector) {
                if (solid.test(vector)) {
                    int dx = vector.getBlockX() - cx;
                    int dy = vector.getBlockY() - cy;
                    int dz = vector.getBlockZ() - cz;

                    Vector3 srcPos = transform.apply(mutable.setComponents(dx, dy, dz));
                    dx = MathMan.roundInt(srcPos.getX());
                    dz = MathMan.roundInt(srcPos.getZ());

                    int distance = dx * dx + dz * dz;
                    if (distance > size2 || Math.abs(dx) > 256 || Math.abs(dz) > 256) return false;

                    double raise = map.getHeight(dx, dz);
                    int val = (int) Math.ceil(raise * scale) + add;
                    if (val < cutoff) {
                        return true;
                    }
                    if (val >= 255 || ThreadLocalRandom.current().nextInt(maxY) < val) {
                        editSession.setBlock(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ(), pattern);
                    }
                    return true;
                }
                return false;
            }
        }, vector -> true, Integer.MAX_VALUE);
        visitor.setDirections(Arrays.asList(BreadthFirstSearch.DIAGONAL_DIRECTIONS));
        visitor.visit(position);
        Operations.completeBlindly(visitor);
    }
}
