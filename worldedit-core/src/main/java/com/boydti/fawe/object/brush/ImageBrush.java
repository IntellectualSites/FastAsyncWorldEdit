package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.collection.SummedColorTable;
import com.boydti.fawe.object.mask.SurfaceMask;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;

public class ImageBrush implements Brush {
    private final LocalSession session;
    private final SummedColorTable table;
    private final int width, height;
    private final double centerX, centerZ;

    private final ColorFunction colorFunction;

    public ImageBrush(BufferedImage image, LocalSession session, boolean alpha /*, boolean glass */) throws IOException {
        this.session = session;
        this.table = new SummedColorTable(image, alpha);
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.centerX = width / 2d;
        this.centerZ = height / 2d;

        if (alpha) {
            colorFunction = (x1, z1, x2, z2, extent, pos) -> {
                int color = table.averageRGBA(x1, z1, x2, z2);
                int alpha1 = (color >> 24) & 0xFF;
                switch (alpha1) {
                    case 0:
                        return 0;
                    case 255:
                        return color;
                    default:
                        BlockStateHolder block = extent.getBlock(pos);
                        TextureUtil tu = session.getTextureUtil();
                        int existingColor = tu.getColor(block.getBlockType());
                        return tu.combineTransparency(color, existingColor);

                }
            };
        } else {
            colorFunction = (x1, z1, x2, z2, extent, pos) -> table.averageRGB(x1, z1, x2, z2);
        }
    }

    private interface ColorFunction {
        int call(int x1, int z1, int x2, int z2, Extent extent, BlockVector3 pos);
    }

    private interface BlockFunction {
        void apply(int color, Extent extent, BlockVector3 pos);
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double sizeDouble) throws MaxChangedBlocksException {
        TextureUtil texture = session.getTextureUtil();

        final int cx = position.getBlockX();
        final int cy = position.getBlockY();
        final int cz = position.getBlockZ();
        final Mask solid = new SurfaceMask(editSession);

        double scale = Math.max(width, height) / sizeDouble;

        Location loc = editSession.getPlayer().getPlayer().getLocation();
        float yaw = loc.getYaw();
        float pitch = loc.getPitch();
        AffineTransform transform = new AffineTransform().rotateY((-yaw) % 360).rotateX((pitch - 90) % 360).inverse();

        RecursiveVisitor visitor = new RecursiveVisitor(new Mask() {
            private final MutableVector3 mutable = new MutableVector3();
            @Override
            public boolean test(BlockVector3 vector) {
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

                    if (x1 >= width || x2 < 0 || z1 >= height || z2 < 0) return false;


                    int color = colorFunction.call(x1, z1, x2, z2, editSession, vector);
                    if (color != 0) {
                        BlockType block = texture.getNearestBlock(color);
                        if (block != null) {
                            editSession.setBlock(vector, block.getDefaultState());
                        }
                    }
                    return true;
                }
                return false;
            }
        }, vector -> true, Integer.MAX_VALUE);
        visitor.setDirections(Arrays.asList(visitor.DIAGONAL_DIRECTIONS));
        visitor.visit(position);
        Operations.completeBlindly(visitor);
    }

    private void apply(double val) {

    }
}