/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.command;

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.brush.BlendBall;
import com.boydti.fawe.object.brush.BlobBrush;
import com.boydti.fawe.object.brush.CatenaryBrush;
import com.boydti.fawe.object.brush.CircleBrush;
import com.boydti.fawe.object.brush.CommandBrush;
import com.boydti.fawe.object.brush.CopyPastaBrush;
import com.boydti.fawe.object.brush.ErodeBrush;
import com.boydti.fawe.object.brush.FallingSphere;
import com.boydti.fawe.object.brush.FlattenBrush;
import com.boydti.fawe.object.brush.HeightBrush;
import com.boydti.fawe.object.brush.ImageBrush;
import com.boydti.fawe.object.brush.LayerBrush;
import com.boydti.fawe.object.brush.LineBrush;
import com.boydti.fawe.object.brush.PopulateSchem;
import com.boydti.fawe.object.brush.RaiseBrush;
import com.boydti.fawe.object.brush.RecurseBrush;
import com.boydti.fawe.object.brush.ScatterBrush;
import com.boydti.fawe.object.brush.ScatterCommand;
import com.boydti.fawe.object.brush.ScatterOverlayBrush;
import com.boydti.fawe.object.brush.ShatterBrush;
import com.boydti.fawe.object.brush.SplatterBrush;
import com.boydti.fawe.object.brush.SplineBrush;
import com.boydti.fawe.object.brush.StencilBrush;
import com.boydti.fawe.object.brush.SurfaceSphereBrush;
import com.boydti.fawe.object.brush.SurfaceSpline;
import com.boydti.fawe.object.brush.heightmap.ScalableHeightMap;
import com.boydti.fawe.object.brush.heightmap.ScalableHeightMap.Shape;
import com.boydti.fawe.object.brush.sweep.SweepBrush;
import com.boydti.fawe.object.clipboard.MultiClipboardHolder;
import com.boydti.fawe.object.mask.BlockTypeMask;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.image.ImageUtil;
import com.sk89q.minecraft.util.commands.Step;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.factory.TreeGeneratorFactory;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.command.tool.brush.ButcherBrush;
import com.sk89q.worldedit.command.tool.brush.ClipboardBrush;
import com.sk89q.worldedit.command.tool.brush.CylinderBrush;
import com.sk89q.worldedit.command.tool.brush.GravityBrush;
import com.sk89q.worldedit.command.tool.brush.HollowCylinderBrush;
import com.sk89q.worldedit.command.tool.brush.HollowSphereBrush;
import com.sk89q.worldedit.command.tool.brush.OperationFactoryBrush;
import com.sk89q.worldedit.command.tool.brush.SmoothBrush;
import com.sk89q.worldedit.command.tool.brush.SphereBrush;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.command.util.CreatureButcher;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.binding.ProvideBindings;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.Contextual;
import com.sk89q.worldedit.function.factory.Deform;
import com.sk89q.worldedit.function.factory.Paint;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.SingleBlockTypeMask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.annotation.ClipboardMask;
import com.sk89q.worldedit.internal.annotation.Range;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.factory.RegionFactory;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.block.BlockID;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;
import org.enginehub.piston.inject.InjectedValueAccess;

/**
 * Commands to set brush shape.
 */
@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class BrushCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public BrushCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
        name = "none",
        aliases = "unbind",
        desc = "Unbind a bound brush from your current item"
    )
    void none(Player player, LocalSession session) throws WorldEditException {
        ToolCommands.setToolNone(player, session);
    }

    @Command(
            name = "blendball",
            aliases = {"bb", "blend"},
            desc = "Smooths and blends terrain",
            descFooter = "Smooths and blends terrain\n" +
                         "Pic: https://i.imgur.com/cNUQUkj.png -> https://i.imgur.com/hFOFsNf.png"
    )
    @CommandPermissions("worldedit.brush.blendball")
    public void blendBallBrush(Player player, LocalSession session,
        @Arg(desc = "The radius to sample for blending", def = "5")
            double radius) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setBrush(new BlendBall(), "worldedit.brush.blendball");
        tool.setSize(radius);
    }

    @Command(
            name = "erode",
            desc = "Erodes terrain"
    )
    @CommandPermissions("worldedit.brush.erode")
    public void erodeBrush(Player player, LocalSession session,
        @Arg(desc = "The radius for eroding", def = "5")
            double radius) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setBrush(new ErodeBrush(), "worldedit.brush.erode");
        tool.setSize(radius);
    }

    @Command(
            name = "pull",
            desc = "Pull terrain towards you"
    )
    @CommandPermissions("worldedit.brush.pull")
    public void pullBrush(Player player, LocalSession session,
        @Arg(desc = "The radius to sample for blending", def = "5")
            double radius) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setSize(radius);
        tool.setBrush(new RaiseBrush(), "worledit.brush.pull");
    }

    @Command(
            name = "circle",
            desc = "Creates a circle which revolves around your facing direction"
    )
    @CommandPermissions("worldedit.brush.sphere")
    public void circleBrush(Player player, EditSession editSession, LocalSession session,
        Pattern fill,
        @Arg(desc = "The radius to sample for blending", def = "5")
            double radius) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setSize(radius);
        tool.setFill(fill);
        tool.setBrush(new CircleBrush(player), "worldedit.brush.sphere");
    }

    @Command(
            name = "recursive",
            aliases = {"recurse", "r"},
            desc = "Set all connected blocks",
            descFooter = "Set all connected blocks\n" +
                         "Note: Set a mask to recurse along specific blocks"
    )
    @CommandPermissions("worldedit.brush.recursive")
    public void recursiveBrush(Player player, LocalSession session, EditSession editSession,
        Pattern fill,
        @Arg(desc = "The radius to sample for blending", def = "5")
            double radius,
        @Switch(name = 'd', desc = "Apply in depth first order")
            boolean depthFirst) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setSize(radius);
        tool.setFill(fill);
        tool.setBrush(new RecurseBrush(depthFirst), "worldedit.brush.recursive");
        tool.setMask(new BlockTypeMask(editSession));
    }

    @Command(
            name = "line",
            aliases = {"l"},
            desc = "Create lines"
    )
    @CommandPermissions("worldedit.brush.line")
    public void lineBrush(Player player, LocalSession session, Pattern fill,
        @Arg(desc = "The radius to sample for blending", def = "0")
            double radius,
        @Switch(name = 'h', desc = "Create only a shell")
            boolean shell,
        @Switch(name = 's', desc = "Selects the clicked point after drawing")
            boolean select,
        @Switch(name = 'f', desc = "Create a flat line")
            boolean flat) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setBrush(new LineBrush(shell, select, flat), "worldedit.brush.line");
        tool.setSize(radius);
        tool.setFill(fill);
    }

    @Command(
            name = "spline",
            aliases = {"spl", "curve"},
            desc = "Join multiple objects together in a curve",
            descFooter = "Click to select some objects,click the same block twice to connect the objects.\n" +
                    "Insufficient brush radius, or clicking the the wrong spot will result in undesired shapes. The shapes must be simple lines or loops.\n" +
                    "Pic1: http://i.imgur.com/CeRYAoV.jpg -> http://i.imgur.com/jtM0jA4.png\n" +
                    "Pic2: http://i.imgur.com/bUeyc72.png -> http://i.imgur.com/tg6MkcF.png" +
                    "Tutorial: https://www.planetminecraft.com/blog/fawe-tutorial/"
    )
    @CommandPermissions("worldedit.brush.spline")
    public void splineBrush(Player player, EditSession editSession, LocalSession session,
        Pattern fill,
        @Arg(desc = "The radius to sample for blending", def = "25")
            double radius) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        player.print(BBC.BRUSH_SPLINE.format(radius));

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setSize(radius);
        tool.setFill(fill);
        tool.setBrush(new SplineBrush(player), "worldedit.brush.spline");
    }

    @Command(
            name = "sweep",
            aliases = {"sw", "vaesweep"},
            desc = "Sweep your clipboard content along a curve",
            descFooter = "Sweeps your clipboard content along a curve.\n" +
                   "Define a curve by selecting the individual points with a brush\n" +
                   "Set [copies] to a value > 0 if you want to have your selection pasted a limited amount of times equally spaced on the curve"
    )
    @CommandPermissions("worldedit.brush.sweep")
    public void sweepBrush(Player player, LocalSession session,
        @Arg(desc = "int", def = "-1")
            int copies) throws WorldEditException {
        player.print(BBC.BRUSH_SPLINE.s());

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setBrush(new SweepBrush(copies), "worldedit.brush.sweep");
    }

    @Command(
            name = "catenary",
            aliases = {"cat", "gravityline", "saggedline"},
            desc = "Create a hanging line between two points"
    )
    @CommandPermissions("worldedit.brush.spline")
    public void catenaryBrush(Player player, LocalSession session, Pattern fill,
        @Arg(def = "1.2", desc = "Length of wire compared to distance between points") @Range(min = 1) double lengthFactor,
        @Arg(desc = "The radius to sample for blending", def = "0")
            double radius,
        @Switch(name = 'h', desc = "Create only a shell")
            boolean shell,
        @Switch(name = 's', desc = "Select the clicked point after drawing")
            boolean select,
        @Switch(name = 'd', desc = "sags the catenary toward the facing direction")
            boolean facingDirection) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setBrush(new CatenaryBrush(shell, select, facingDirection, lengthFactor), "worldedit.brush.spline");
        tool.setSize(radius);
        tool.setFill(fill);
    }

    @Command(
            name = "surfacespline",
            aliases = {"sspline", "sspl"},
            desc = "Draws a spline (curved line) on the surface",
            descFooter = "Create a spline on the surface\n" +
                   "Video: https://www.youtube.com/watch?v=zSN-2jJxXlM"
    )
    @CommandPermissions("worldedit.brush.surfacespline") // 0, 0, 0, 10, 0,
    public void surfaceSpline(Player player, LocalSession session, Pattern fill,
        @Arg(desc = "The radius to sample for blending", def = "0")
            double radius,
        @Arg(desc = "double", def = "0")
            double tension,
        @Arg(desc = "double", def = "0")
            double bias,
        @Arg(desc = "double", def = "0")
            double continuity,
        @Arg(desc = "double", def = "10")
            double quality) throws WorldEditException {
        player.print(BBC.BRUSH_SPLINE.format(radius));
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setBrush(new SurfaceSpline(tension,bias, continuity, quality), "worldedit.brush.surfacespline");
        tool.setSize(radius);
        tool.setFill(fill);

    }

    @Command(
            name = "rock",
            aliases = {"blob"},
            desc = "Creates a distorted sphere"
    )
    @CommandPermissions("worldedit.brush.rock")
    public void blobBrush(Player player, LocalSession session, Pattern fill,
        @Arg(name = "radius", desc = "Vector3", def = "10")
            Vector3 radius,
        @Arg(name = "sphericity", desc = "double", def = "100")
            double sphericity,
        @Arg(name = "frequency", desc = "double", def = "30")
            double frequency,
        @Arg(name = "amplitude", desc = "double", def = "50")
            double amplitude) throws WorldEditException {
        double max = MathMan.max(radius.getX(), radius.getY(), radius.getZ());
        worldEdit.checkMaxBrushRadius(max);
        Brush brush = new BlobBrush(radius.divide(max), frequency / 100, amplitude / 100, sphericity / 100);

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setBrush(brush, "worldedit.brush.rock");
        tool.setSize(max);
        tool.setFill(fill);
    }

    @Command(
        name = "sphere",
        aliases = { "s" },
        desc = "Choose the sphere brush"
    )
    @CommandPermissions("worldedit.brush.sphere")
    public void sphereBrush(Player player, LocalSession session,
        @Arg(desc = "The pattern of blocks to set")
            Pattern pattern,
        @Arg(desc = "The radius of the sphere", def = "2")
            double radius,
        @Switch(name = 'h', desc = "Create hollow spheres instead")
            boolean hollow,
        @Switch(name = 'f', desc = "Create falling spheres instead")
            boolean falling) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setFill(pattern);
        tool.setSize(radius);

        if (hollow) {
            tool.setBrush(new HollowSphereBrush(), "worldedit.brush.sphere");
        } else {
            if (pattern instanceof BlockStateHolder) {
                BlockType type = ((BlockStateHolder) pattern).getBlockType();
                switch (type.getInternalId()) {
                    case BlockID.SAND:
                    case BlockID.GRAVEL:
                        BBC.BRUSH_TRY_OTHER.send(player);
                        falling = true;
                }
            }
            if (falling) {
                tool.setBrush(new FallingSphere(), "worldedit.brush.sphere");
            } else {
                tool.setBrush(new SphereBrush(), "worldedit.brush.sphere");
            }
        }
        player.print(String.format("Sphere brush shape equipped (%.0f).", radius));

    }

    @Command(
            name = "shatter",
            aliases = {"partition", "split"},
            desc = "Creates random lines to break the terrain into pieces",
            descFooter = "Creates uneven lines separating terrain into multiple pieces\n" +
                         "Pic: https://i.imgur.com/2xKsZf2.png"
    )
    @CommandPermissions("worldedit.brush.shatter")
    public void shatterBrush(Player player, EditSession editSession, LocalSession session,
        Pattern fill,
        @Arg(desc = "The radius to sample for blending", def = "10")
            double radius,
        @Arg(desc = "Lines", def = "10")
            int count) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setFill(fill);
        tool.setSize(radius);
        tool.setBrush(new ShatterBrush(count), "worldedit.brush.shatter");
        tool.setMask(new ExistingBlockMask(editSession));
    }

    @Command(
            name = "stencil",
            desc = "Use a height map to paint a surface",
            descFooter = "Use a height map to paint any surface.\n"
)
    @CommandPermissions("worldedit.brush.stencil")
    public void stencilBrush(Player player, LocalSession session, Pattern fill,
        @Arg(desc = "Expression", def = "5") double radius,
        @Arg(name = "image", desc = "String", def = "") String image,
        @Arg(def = "0", desc = "rotation") @Range(min = 0, max = 360) int rotation,
        @Arg(name = "yscale", desc = "double", def = "1") double yscale,
        @Switch(name = 'w', desc = "Apply at maximum saturation") boolean onlyWhite,
        @Switch(name = 'r', desc = "Apply random rotation") boolean randomRotate) throws WorldEditException, FileNotFoundException {
        worldEdit.checkMaxBrushRadius(radius);
        InputStream stream = getHeightmapStream(image);
        HeightBrush brush;
        try {
            brush = new StencilBrush(stream, rotation, yscale, onlyWhite, "#clipboard".equalsIgnoreCase(image) ? session.getClipboard().getClipboard() : null);
        } catch (EmptyClipboardException ignore) {
            brush = new StencilBrush(stream, rotation, yscale, onlyWhite, null);
        }
        if (randomRotate) {
            brush.setRandomRotate(true);
        }

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setBrush(brush, "worldedit.brush.stencil");
        tool.setSize(radius);
        tool.setFill(fill);
    }

    @Command(
            name = "image",
            aliases = {"color"},
            desc = "Use a height map to paint a surface",
            descFooter = "Use a height map to paint any surface.\n")
    @CommandPermissions("worldedit.brush.stencil")
    public void imageBrush(Player player, LocalSession session,
        @Arg(desc = "Expression", def = "5")
            double radius,
        ProvideBindings.ImageUri imageUri,
        @Arg(def = "1", desc = "scale height") @Range(min = Double.MIN_NORMAL)
            double yscale,
        @Switch(name = 'a', desc = "Use image Alpha")
            boolean alpha,
        @Switch(name = 'f', desc = "Blend the image with existing terrain")
            boolean fadeOut) throws WorldEditException, IOException {
        BufferedImage image = imageUri.load();
        worldEdit.checkMaxBrushRadius(radius);
        if (yscale != 1) {
            ImageUtil.scaleAlpha(image, yscale);
            alpha = true;
        }
        if (fadeOut) {
            ImageUtil.fadeAlpha(image);
            alpha = true;
        }
        ImageBrush brush = new ImageBrush(image, session, alpha);

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setBrush(brush, "worldedit.brush.stencil");
        tool.setSize(radius);
    }

    @Command(
            name = "surface",
            aliases = {"surf"},
            desc = "Use a height map to paint a surface",
            descFooter = "Use a height map to paint any surface.\n" +
                   "The -w flag will only apply at maximum saturation\n" +
                   "The -r flag will apply random rotation"
    )
    @CommandPermissions("worldedit.brush.surface")
    public void surfaceBrush(Player player, LocalSession session, Pattern fill,
        @Arg(desc = "Expression", def = "5")
            double radius) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setSize(radius);
        tool.setFill(fill);
        tool.setBrush(new SurfaceSphereBrush(), "worldedit.brush.surface");
    }

    @Command(
            name = "scatter",
            desc = "Scatter a pattern on a surface",
            descFooter = "Set a number of blocks randomly on a surface each a certain distance apart.\n" +
                   "Video: https://youtu.be/RPZIaTbqoZw?t=34s"
    )
    @CommandPermissions("worldedit.brush.scatter")
    public void scatterBrush(Player player, LocalSession session, Pattern fill,
        @Arg(desc = "double", def = "5") double radius,
        @Arg(name = "points", desc = "double", def = "5") double pointsOpt,
        @Arg(name = "distance", desc = "double", def = "1") double distanceOpt,
        @Switch(name = 'o', desc = "Overlay the block") boolean overlay) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        Brush brush;
        if (overlay) {
            brush = new ScatterOverlayBrush((int) pointsOpt, (int) distanceOpt);
        } else {
            brush = new ScatterBrush((int) pointsOpt, (int) distanceOpt);
        }

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setBrush(brush, "worldedit.brush.shatter");
        tool.setSize(radius);
        tool.setFill(fill);
    }

    @Command(
            name = "populateschematic",
            aliases = {"populateschem", "popschem", "pschem", "ps"},
            desc = "Scatter a schematic on a surface"
    )
    @CommandPermissions("worldedit.brush.populateschematic")
    public void scatterSchemBrush(Player player, LocalSession session, Mask mask,
        @Arg(name = "clipboard", desc = "Clipboard uri") String clipboardStr,
        @Arg(desc = "Expression", def = "30") double radius,
        @Arg(desc = "double", def = "50") double density,
        @Switch(name = 'r', desc = "Apply random rotation") boolean rotate) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        try {
            MultiClipboardHolder clipboards = ClipboardFormats.loadAllFromInput(player, clipboardStr, null, true);
            if (clipboards == null) {
                BBC.SCHEMATIC_NOT_FOUND.send(player, clipboardStr);
                return;
            }
            List<ClipboardHolder> holders = clipboards.getHolders();
            if (holders == null) {
                BBC.SCHEMATIC_NOT_FOUND.send(player, clipboardStr);
                return;
            }

            BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
            tool.setBrush(new PopulateSchem(mask, holders, (int) density, rotate), "worldedit.brush.populateschematic");
            tool.setSize(radius);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Command(
            name = "layer",
            desc = "Replaces terrain with a layer.",
            descFooter = "Replaces terrain with a layer.\n" +
                         "Example: /br layer 5 95:1 95:2 35:15 - Places several layers on a surface\n" +
                         "Pic: https://i.imgur.com/XV0vYoX.png"
    )
    @CommandPermissions("worldedit.brush.layer")
    public void surfaceLayer(Player player, LocalSession session,
                            @Arg(desc = "Expression") double radius,
                            List<BlockState> blockLayers) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setBrush(new LayerBrush(blockLayers.toArray(new BlockState[0])), "worldedit.brush.layer");
        tool.setSize(radius);
    }

    @Command(
            name = "splatter",
            desc = "Splatter a pattern on a surface",
            descFooter = "Sets a bunch of blocks randomly on a surface.\n" +
                   "Pic: https://i.imgur.com/hMD29oO.png\n" +
                   "Example: /br splatter stone,dirt 30 15\n" +
                   "Note: The seeds define how many splotches there are, recursion defines how large, solid defines whether the pattern is applied per seed, else per block."
    )
    @CommandPermissions("worldedit.brush.splatter")
    public void splatterBrush(Player player, LocalSession session, Pattern fill,
        @Arg(desc = "Expression", def = "5") double radius,
        @Arg(name = "points", desc = "double", def = "1") double pointsOpt,
        @Arg(desc = "double", def = "5") double recursion,
        @Arg(desc = "boolean", def = "true") boolean solid) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());

        tool.setBrush(new SplatterBrush((int) pointsOpt, (int) recursion, solid), "worldedit.brush.splatter");
        tool.setSize(radius);
        tool.setFill(fill);
    }

    @Command(
            name = "scattercommand",
            aliases = {"scattercmd", "scmd", "scommand"},
            desc = "Run commands at random points on a surface",
            descFooter =
                    "Run commands at random points on a surface\n" +
                            " - The scatter radius is the min distance between each point\n" +
                            " - Your selection will be expanded to the specified size around each point\n" +
                            " - Placeholders: {x}, {y}, {z}, {world}, {size}"
    )
    @CommandPermissions("worldedit.brush.scattercommand")
    public void scatterCommandBrush(Player player, EditSession editSession, LocalSession session,
        @Arg(desc = "Expression")
            double radius, double points,
        double distance, List<String> commandStr) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setBrush(new ScatterCommand(
            (int) points, (int) distance, StringMan.join(commandStr, " ")), "worldedit.brush.scattercommand");
        tool.setSize(radius);
    }

    @Command(
        name = "cylinder",
        aliases = { "cyl", "c" },
        desc = "Choose the cylinder brush"
    )
    @CommandPermissions("worldedit.brush.cylinder")
    public void cylinderBrush(Player player, LocalSession session,
        @Arg(desc = "The pattern of blocks to set")
            Pattern pattern,
        @Arg(desc = "The radius of the cylinder", def = "2")
            double radius,
        @Arg(desc = "The height of the cylinder", def = "1")
            int height,
        @Switch(name = 'h', desc = "Create hollow cylinders instead")
            boolean hollow) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        worldEdit.checkMaxBrushRadius(height);

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setFill(pattern);
        tool.setSize(radius);
        if (hollow) {
            tool.setBrush(new HollowCylinderBrush(height), "worldedit.brush.cylinder");
        } else {
            tool.setBrush(new CylinderBrush(height), "worldedit.brush.cylinder");
        }
        player.print(String.format("Cylinder brush shape equipped (%.0f by %d).", radius, height));
    }

    @Command(
            name = "clipboard",
            desc = "@Deprecated use instead: `/br copypaste`)",
            descFooter = "Choose the clipboard brush"
    )
    @CommandPermissions("worldedit.brush.clipboard")
    public void clipboardBrush(Player player, LocalSession session,
        @Switch(name = 'a', desc = "Don't paste air from the clipboard")
            boolean ignoreAir,
        @Switch(name = 'o', desc = "Paste starting at the target location, instead of centering on it")
            boolean usingOrigin,
        @Switch(name = 'e', desc = "Skip entities if available")
            boolean skipEntities,
        @Switch(name = 'b', desc = "Paste biomes if available")
            boolean pasteBiomes,
        @ArgFlag(name = 'm', desc = "Skip blocks matching this mask in the clipboard", def = "")
        @ClipboardMask
            Mask sourceMask) throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();

        Clipboard clipboard = holder.getClipboard();
        ClipboardHolder newHolder = new ClipboardHolder(clipboard);
        newHolder.setTransform(holder.getTransform());

        BlockVector3 size = clipboard.getDimensions();

        worldEdit.checkMaxBrushRadius(size.getBlockX() / 2D - 1);
        worldEdit.checkMaxBrushRadius(size.getBlockY() / 2D - 1);
        worldEdit.checkMaxBrushRadius(size.getBlockZ() / 2D - 1);

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setBrush(new ClipboardBrush(newHolder, ignoreAir, usingOrigin, !skipEntities, pasteBiomes, sourceMask), "worldedit.brush.clipboard");

        player.print("Clipboard brush shape equipped.");
    }

    @Command(
        name = "smooth",
        desc = "Choose the terrain softener brush",
        descFooter = "Example: '/brush smooth 2 4 grass_block,dirt,stone'"
    )
    @CommandPermissions("worldedit.brush.smooth")
    public void smoothBrush(Player player, LocalSession session,
        @Arg(desc = "The radius to sample for softening", def = "2")
            double radius,
        @Arg(desc = "The number of iterations to perform", def = "4")
            int iterations,
        @Arg(desc = "The mask of blocks to use for the heightmap", def = "")
            Mask maskOpt) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setSize(radius);
        FaweLimit limit = Settings.IMP.getLimit(player);
        iterations = Math.min(limit.MAX_ITERATIONS, iterations);
        tool.setBrush(new SmoothBrush(iterations, maskOpt), "worldedit.brush.smooth");

        player.print(String.format("Smooth brush equipped (%.0f x %dx, using %s).", radius, iterations, maskOpt == null ? "any block" : "filter"));
    }

    @Command(
        name = "extinguish",
        aliases = { "ex" },
        desc = "Shortcut fire extinguisher brush"
    )
    @CommandPermissions("worldedit.brush.ex")
    public void extinguishBrush(Player player, LocalSession session, EditSession editSession,
                                @Arg(desc = "The radius to extinguish", def = "5")
                                    double radius) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setFill(BlockTypes.AIR.getDefaultState());
        tool.setSize(radius);
        tool.setMask(new SingleBlockTypeMask(editSession, BlockTypes.FIRE));
        tool.setBrush(new SphereBrush(), "worldedit.brush.ex");
        player.print(String.format("Extinguisher equipped (%.0f).", radius));
    }

    @Command(
        name = "gravity",
        aliases = { "grav" },
        desc = "Gravity brush, simulates the effect of gravity"
    )
    @CommandPermissions("worldedit.brush.gravity")
    public void gravityBrush(Player player, LocalSession session,
        @Arg(desc = "The radius to apply gravity in", def = "5")
            double radius,
        @Switch(name = 'h', desc = "Affect blocks starting at max Y, rather than the target location Y + radius")
            boolean fromMaxY) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());

        tool.setSize(radius);
        tool.setBrush(new GravityBrush(fromMaxY), "worldedit.brush.gravity");

        player.print(String.format("Gravity brush equipped (%.0f).",
            radius));
    }

    @Command(
            name = "height",
            aliases = {"heightmap"},
            desc = "Raise or lower terrain using a heightmap",
            descFooter = "This brush raises and lowers land.\n" +
                   " - The `-r` flag enables random off-axis rotation\n" +
                   " - The `-l` flag will work on snow layers\n" +
                   " - The `-s` flag disables smoothing\n" +
                   "Note: Use a negative yscale to reduce height\n" +
                   "Snow Pic: https://i.imgur.com/Hrzn0I4.png"
    )
    @CommandPermissions("worldedit.brush.height")
    public void heightBrush(Player player, LocalSession session, @Arg(desc = "Expression", def = "5") double radius, @Arg(name = "image", desc = "String", def = "") String image, @Arg(def = "0", desc = "rotation") @Range(min = 0, max = 360) int rotation, @Arg(name = "yscale", desc = "double", def = "1") double yscale, @Switch(name = 'r', desc = "TODO") boolean randomRotate, @Switch(name = 'l', desc = "TODO") boolean layers, @Switch(name = 's', desc = "TODO") boolean dontSmooth, InjectedValueAccess context) throws WorldEditException, FileNotFoundException {
        terrainBrush(player, session, radius, image, rotation, yscale, false, randomRotate, layers, !dontSmooth, ScalableHeightMap.Shape.CONE);
    }

    @Command(
            name = "cliff",
            aliases = {"flatcylinder"},
            desc = "Cliff brush",
            descFooter = "This brush flattens terrain and creates cliffs."
    )
    @CommandPermissions("worldedit.brush.height")
    public void cliffBrush(Player player, LocalSession session,
        @Arg(desc = "Expression", def = "5")
            double radius,
        @Arg(name = "image", desc = "String", def = "")
            String image,
        @Arg(def = "0", desc = "rotation") @Step(90) @Range(min = 0, max = 360)
            int rotation,
        @Arg(name = "yscale", desc = "double", def = "1")
            double yscale,
        @Switch(name = 'r', desc = "Enables random off-axis rotation")
            boolean randomRotate,
        @Switch(name = 'l', desc = "Will work on snow layers")
            boolean layers,
        @Switch(name = 's', desc = "Disables smoothing")
            boolean dontSmooth, InjectedValueAccess context) throws WorldEditException, FileNotFoundException {
        terrainBrush(player, session, radius, image, rotation, yscale, true, randomRotate, layers, !dontSmooth, ScalableHeightMap.Shape.CYLINDER);
    }

    @Command(
            name = "flatten",
            aliases = {"flatmap", "flat"},
            desc = "This brush raises or lowers land towards the clicked point"
    )
    @CommandPermissions("worldedit.brush.height")
    public void flattenBrush(Player player, LocalSession session,
        @Arg(desc = "Expression", def = "5")
            double radius,
        @Arg(name = "image", desc = "String", def = "")
            String image,
        @Arg(def = "0", desc = "rotation")
        @Step(90)
            int rotation,
        @Arg(name = "yscale", desc = "double", def = "1")
            double yscale,
        @Switch(name = 'r', desc = "Enables random off-axis rotation")
            boolean randomRotate,
        @Switch(name = 'l', desc = "Will work on snow layers")
            boolean layers,
        @Switch(name = 's', desc = "Disables smoothing")
            boolean dontSmooth, InjectedValueAccess context) throws WorldEditException, FileNotFoundException {
        terrainBrush(player, session, radius, image, rotation, yscale, true, randomRotate, layers, !dontSmooth, ScalableHeightMap.Shape.CONE);
    }

    private void terrainBrush(Player player, LocalSession session,
        @Arg(name = "radius", desc = "Expression")
            double radius, String image, int rotation,
        double yscale, boolean flat, boolean randomRotate, boolean layers, boolean smooth,
        Shape shape) throws WorldEditException, FileNotFoundException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        InputStream stream = getHeightmapStream(image);
        HeightBrush brush;
        if (flat) {
            try {
                brush = new FlattenBrush(stream, rotation, yscale, layers, smooth, "#clipboard".equalsIgnoreCase(image) ? session.getClipboard().getClipboard() : null, shape);
            } catch (EmptyClipboardException ignore) {
                brush = new FlattenBrush(stream, rotation, yscale, layers, smooth, null, shape);
            }
        } else {
            try {
                brush = new HeightBrush(stream, rotation, yscale, layers, smooth, "#clipboard".equalsIgnoreCase(image) ? session.getClipboard().getClipboard() : null);
            } catch (EmptyClipboardException ignore) {
                brush = new HeightBrush(stream, rotation, yscale, layers, smooth, null);
            }
        }
        if (randomRotate) {
            brush.setRandomRotate(true);
        }
        tool.setBrush(brush, "worldedit.brush.height");
        tool.setSize(radius);

    }

    private InputStream getHeightmapStream(String filename) throws FileNotFoundException {
        if (filename == null) return null;
        String filenamePng = filename.endsWith(".png") ? filename : filename + ".png";
        File file = new File(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HEIGHTMAP + File.separator + filenamePng);
        if (file.exists()) return new FileInputStream(file);
        URI uri = ImageUtil.getImageURI(filename);
        return ImageUtil.getInputStream(uri);
    }


    @Command(
            name = "copypaste",
            aliases = {"cp", "copypasta"},
            desc = "Copy Paste brush",
            descFooter = "Left click the base of an object to copy.\n" +
                   "Right click to paste\n" +
                   "Note: Works well with the clipboard scroll action\n" +
                   "Video: https://www.youtube.com/watch?v=RPZIaTbqoZw"
    )
    @CommandPermissions("worldedit.brush.copy")
    public void copy(Player player, LocalSession session,
        @Arg(desc = "Expression", def = "5") double radius,
        @Switch(name = 'r', desc = "Apply random rotation on paste") boolean randomRotate,
        @Switch(name = 'a', desc = "Apply auto view based rotation on paste") boolean autoRotate) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        player.print(BBC.BRUSH_COPY.format(radius));
        tool.setBrush(new CopyPastaBrush(
            player, session, randomRotate, autoRotate), "worldedit.brush.copy");
        tool.setSize(radius);

    }

    @Command(
            name = "command",
            aliases = {"cmd"},
            desc = "Command brush",
            descFooter = "Run the commands at the clicked position.\n" +
                   " - Your selection will be expanded to the specified size around each point\n" +
                   " - Placeholders: {x}, {y}, {z}, {world}, {size}"
    )
    @CommandPermissions("worldedit.brush.command")
    public void command(Player player, LocalSession session,
        @Arg(desc = "Expression") double radius,
        @Arg(desc = "Command to run") List<String> input) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        String cmd = StringMan.join(input, " ");
        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setBrush(new CommandBrush(cmd), "worldedit.brush.command");
        tool.setSize(radius);

    }

    @Command(
        name = "butcher",
        aliases = { "kill" },
        desc = "Butcher brush, kills mobs within a radius"
    )
    @CommandPermissions("worldedit.brush.butcher")
    public void butcherBrush(Player player, LocalSession session,
        @Arg(desc = "Radius to kill mobs in", def = "5")
            double radius,
        @Switch(name = 'p', desc = "Also kill pets")
            boolean killPets,
        @Switch(name = 'n', desc = "Also kill NPCs")
            boolean killNpcs,
        @Switch(name = 'g', desc = "Also kill golems")
            boolean killGolems,
        @Switch(name = 'a', desc = "Also kill animals")
            boolean killAnimals,
        @Switch(name = 'b', desc = "Also kill ambient mobs")
            boolean killAmbient,
        @Switch(name = 't', desc = "Also kill mobs with name tags")
            boolean killWithName,
        @Switch(name = 'f', desc = "Also kill all friendly mobs (Applies the flags `-abgnpt`)")
            boolean killFriendly,
        @Switch(name = 'r', desc = "Also destroy armor stands")
            boolean killArmorStands) throws WorldEditException {
        LocalConfiguration config = worldEdit.getConfiguration();

        double maxRadius = config.maxBrushRadius;
        // hmmmm not horribly worried about this because -1 is still rather efficient,
        // the problem arises when butcherMaxRadius is some really high number but not infinite
        // - original idea taken from https://github.com/sk89q/worldedit/pull/198#issuecomment-6463108
        if (player.hasPermission("worldedit.butcher")) {
            maxRadius = Math.max(config.maxBrushRadius, config.butcherMaxRadius);
        }
        if (radius > maxRadius) {
            player.printError("Maximum allowed brush radius: " + maxRadius);
            return;
        }

        CreatureButcher flags = new CreatureButcher(player);
        flags.or(CreatureButcher.Flags.FRIENDLY      , killFriendly); // No permission check here. Flags will instead be filtered by the subsequent calls.
        flags.or(CreatureButcher.Flags.PETS          , killPets, "worldedit.butcher.pets");
        flags.or(CreatureButcher.Flags.NPCS          , killNpcs, "worldedit.butcher.npcs");
        flags.or(CreatureButcher.Flags.GOLEMS        , killGolems, "worldedit.butcher.golems");
        flags.or(CreatureButcher.Flags.ANIMALS       , killAnimals, "worldedit.butcher.animals");
        flags.or(CreatureButcher.Flags.AMBIENT       , killAmbient, "worldedit.butcher.ambient");
        flags.or(CreatureButcher.Flags.TAGGED        , killWithName, "worldedit.butcher.tagged");
        flags.or(CreatureButcher.Flags.ARMOR_STAND   , killArmorStands, "worldedit.butcher.armorstands");
        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());

        tool.setSize(radius);
        tool.setBrush(new ButcherBrush(flags), "worldedit.brush.butcher");
        player.print(String.format("Butcher brush equipped (%.0f).", radius));

    }

    @Command(
        name = "forest",
        desc = "Forest brush, creates a forest in the area"
    )
    @CommandPermissions("worldedit.brush.forest")
    public void forest(Player player, LocalSession localSession,
                       @Arg(desc = "The shape of the region")
                           RegionFactory shape,
                       @Arg(desc = "The size of the brush", def = "5")
                           double radius,
                       @Arg(desc = "The density of the brush", def = "20")
                           double density,
                       @Arg(desc = "The type of tree to use")
                           TreeGenerator.TreeType type) throws WorldEditException {
        setOperationBasedBrush(player, localSession, radius,
            new Paint(new TreeGeneratorFactory(type), density / 100), shape, "worldedit.brush.forest");
    }

    @Command(
        name = "raise",
        desc = "Raise brush, raise all blocks by one"
    )
    @CommandPermissions("worldedit.brush.raise")
    public void raise(Player player, LocalSession localSession,
                      @Arg(desc = "The shape of the region")
                          RegionFactory shape,
                      @Arg(desc = "The size of the brush", def = "5")
                          double radius) throws WorldEditException {
        setOperationBasedBrush(player, localSession, radius,
            new Deform("y-=1"), shape, "worldedit.brush.raise");
    }

    @Command(
        name = "lower",
        desc = "Lower brush, lower all blocks by one"
    )
    @CommandPermissions("worldedit.brush.lower")
    public void lower(Player player, LocalSession localSession,
                      @Arg(desc = "The shape of the region")
                          RegionFactory shape,
                      @Arg(desc = "The size of the brush", def = "5")
                          double radius) throws WorldEditException {
        setOperationBasedBrush(player, localSession, radius,
            new Deform("y+=1"), shape, "worldedit.brush.lower");
    }

    static void setOperationBasedBrush(Player player, LocalSession session, double radius,
                                        Contextual<? extends Operation> factory,
                                        RegionFactory shape,
                                        String permission) throws WorldEditException {
        WorldEdit.getInstance().checkMaxBrushRadius(radius);
        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setSize(radius);
        tool.setFill(null);
        tool.setBrush(new OperationFactoryBrush(factory, shape, session), permission);

        player.print("Set brush to " + factory);
    }
}
