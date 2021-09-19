package com.fastasyncworldedit.core.command.tool.brush;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.function.mask.ImageBrushMask;
import com.fastasyncworldedit.core.function.mask.SurfaceMask;
import com.fastasyncworldedit.core.util.TextureUtil;
import com.fastasyncworldedit.core.util.collection.SummedColorTable;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;

public class ImageBrush implements Brush {

    private final LocalSession session;
    private final SummedColorTable table;
    private final int width;
    private final int height;
    private final double centerImageX;
    private final double centerImageZ;

    private final ColorFunction colorFunction;

    public ImageBrush(BufferedImage image, LocalSession session, boolean alpha /*, boolean glass */) throws IOException {
        this.session = session;
        this.table = new SummedColorTable(image, alpha);
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.centerImageX = width / 2d;
        this.centerImageZ = height / 2d;

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
                        BlockState block = extent.getBlock(pos);
                        TextureUtil tu = session.getTextureUtil();
                        BlockType type = block.getBlockType();
                        int existingColor;
                        if (type == BlockTypes.GRASS_BLOCK) {
                            existingColor = tu.getColor(extent.getBiome(pos));
                        } else {
                            existingColor = tu.getColor(type);
                        }
                        return TextureUtil.combineTransparency(color, existingColor);

                }
            };
        } else {
            colorFunction = (x1, z1, x2, z2, extent, pos) -> table.averageRGB(x1, z1, x2, z2);
        }
    }

    public interface ColorFunction {

        int call(int x1, int z1, int x2, int z2, Extent extent, BlockVector3 pos);

    }

    @Override
    public void build(EditSession editSession, BlockVector3 center, Pattern pattern, double sizeDouble) throws
            MaxChangedBlocksException {
        final Mask solid = new SurfaceMask(editSession);

        double scale = Math.max(width, height) / sizeDouble;

        Actor actor = editSession.getActor();
        if (!(actor instanceof Player)) {
            throw FaweCache.PLAYER_ONLY;
        }
        Player player = (Player) actor;
        Location loc = player.getLocation();
        float yaw = loc.getYaw();
        float pitch = loc.getPitch();
        AffineTransform transform = new AffineTransform().rotateY((-yaw) % 360).rotateX((pitch - 90) % 360).inverse();
        RecursiveVisitor visitor = new RecursiveVisitor(
                new ImageBrushMask(
                        solid,
                        center,
                        transform,
                        scale,
                        centerImageX,
                        centerImageZ,
                        width,
                        height,
                        colorFunction,
                        editSession,
                        session.getTextureUtil()
                ), vector -> true, Integer.MAX_VALUE, editSession.getMinY(), editSession.getMaxY());
        visitor.setDirections(Arrays.asList(BreadthFirstSearch.DIAGONAL_DIRECTIONS));
        visitor.visit(center);
        Operations.completeBlindly(visitor);
    }

}
