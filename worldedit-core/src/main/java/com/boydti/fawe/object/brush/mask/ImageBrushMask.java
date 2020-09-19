package com.boydti.fawe.object.brush.mask;

import com.boydti.fawe.object.brush.ImageBrush;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.world.block.BlockType;

public class ImageBrushMask extends AbstractExtentMask {

    private final MutableVector3 mutable = new MutableVector3();
    private final Mask solid;
    private final int cx;
    private final int cy;
    private final int cz;
    private final Transform transform;
    private final double scale;
    private final double centerX;
    private final double centerZ;
    private final int width;
    private final int height;
    private final ImageBrush.ColorFunction colorFunction;
    private final EditSession session;
    private final TextureUtil texture;

    public ImageBrushMask(Mask solid,
                          int cx,
                          int cy,
                          int cz,
                          Transform transform,
                          double scale,
                          double centerX,
                          double centerZ,
                          int width,
                          int height,
                          ImageBrush.ColorFunction colorFunction,
                          EditSession session,
                          TextureUtil texture) {
        super(session);
        this.solid = solid;
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        this.transform = transform;
        this.scale = scale;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.width = width;
        this.height = height;
        this.colorFunction = colorFunction;
        this.session = session;
        this.texture = texture;
    }

    @Override public boolean test(BlockVector3 vector) {
        if (solid.test(vector)) {
            int dx = vector.getBlockX() - cx;
            int dy = vector.getBlockY() - cy;
            int dz = vector.getBlockZ() - cz;

            Vector3 pos1 = transform.apply(mutable.setComponents(dx - 0.5, dy - 0.5, dz - 0.5));
            int x1 = (int) (pos1.getX() * scale + centerX);
            int z1 = (int) (pos1.getZ() * scale + centerZ);

            Vector3 pos2 = transform.apply(mutable.setComponents(dx + 0.5, dy + 0.5, dz + 0.5));
            int x2 = (int) (pos2.getX() * scale + centerX);
            int z2 = (int) (pos2.getZ() * scale + centerZ);
            if (x2 < x1) {
                int tmp = x1;
                x1 = x2;
                x2 = tmp;
            }
            if (z2 < z1) {
                int tmp = z1;
                z1 = z2;
                z2 = tmp;
            }

            if (x1 >= width || x2 < 0 || z1 >= height || z2 < 0) {
                return false;
            }


            int color = colorFunction.call(x1, z1, x2, z2, session, vector);
            if (color != 0) {
                BlockType block = texture.getNearestBlock(color);
                if (block != null) {
                    session.setBlock(vector, block.getDefaultState());
                }
            }
            return true;
        }
        return false;
    }

    @Override public Mask clone() {
        return new ImageBrushMask(solid.clone(), cx, cy, cz, transform, scale, centerX, centerZ, width, height, colorFunction, session, texture);
    }
}
