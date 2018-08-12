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

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.brush.*;
import com.boydti.fawe.object.brush.heightmap.ScalableHeightMap;
import com.boydti.fawe.object.brush.sweep.SweepBrush;
import com.boydti.fawe.object.clipboard.MultiClipboardHolder;
import com.boydti.fawe.object.mask.IdMask;
import com.boydti.fawe.util.ColorUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.image.ImageUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Step;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.command.tool.brush.*;
import com.sk89q.worldedit.command.util.CreatureButcher;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.SingleBlockTypeMask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.command.InvalidUsageException;
import com.sk89q.worldedit.util.command.binding.Range;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.world.block.*;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Commands to set brush shape.
 */
@Command(aliases = {"brush", "br", "tool"},
        desc = "Commands to build and draw from far away. [More Info](https://git.io/vSPYf)"
)
public class BrushCommands extends BrushProcessor {

    public BrushCommands(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Command(
            aliases = {"blendball", "bb", "blend"},
            usage = "[radius=5]",
            desc = "Smooths and blends terrain",
            help = "Smooths and blends terrain\n" +
                    "Pic: https://i.imgur.com/cNUQUkj.png -> https://i.imgur.com/hFOFsNf.png",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.blendball")
    public BrushSettings blendBallBrush(Player player, LocalSession session, @Optional("5") double radius, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);
        return set(session, context, new BlendBall()).setSize(radius);
    }

    @Command(
            aliases = {"erode", "e"},
            usage = "[radius=5]",
            desc = "Erodes terrain",
            help = "Erodes terrain",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.erode")
    public BrushSettings erodeBrush(Player player, LocalSession session, @Optional("5") double radius, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);
        return set(session, context, new ErodeBrush()).setSize(radius);
    }

    @Command(
            aliases = {"pull"},
            usage = "[radius=5]",
            desc = "Pull terrain towards you",
            help = "Pull terrain towards you",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.pull")
    public BrushSettings pullBrush(Player player, LocalSession session, @Optional("5") double radius, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);
        return set(session, context, new RaiseBrush()).setSize(radius);
    }

    @Command(
            aliases = {"circle"},
            usage = "<pattern> [radius=5]",
            desc = "Creates a circle which revolves around your facing direction",
            help = "Creates a circle which revolves around your facing direction.\n" +
                    "Note: Decrease brush radius, and enabled visualization to assist with placement mid-air",
            min = 1,
            max = 2
    )
    @CommandPermissions("worldedit.brush.sphere")
    public BrushSettings circleBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("5") double radius, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);
        return set(session, context, new CircleBrush(player)).setSize(radius).setFill(fill);
    }

    @Command(
            aliases = {"recursive", "recurse", "r"},
            usage = "<pattern-to> [radius=5]",
            desc = "Set all connected blocks",
            help = "Set all connected blocks\n" +
                    "The -d flag Will apply in depth first order\n" +
                    "Note: Set a mask to recurse along specific blocks",
            min = 0,
            max = 3
    )
    @CommandPermissions("worldedit.brush.recursive")
    public BrushSettings recursiveBrush(Player player, LocalSession session, EditSession editSession, Pattern fill, @Optional("5") double radius, @Switch('d') boolean depthFirst, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);
        return set(session, context,
                new RecurseBrush(depthFirst))
                .setSize(radius)
                .setFill(fill)
                .setMask(new IdMask(editSession));
    }

    @Command(
            aliases = {"line", "l"},
            usage = "<pattern> [radius=0]",
            flags = "hsf",
            desc = "Create lines",
            help =
                    "Create lines.\n" +
                            "The -h flag creates only a shell\n" +
                            "The -s flag selects the clicked point after drawing\n" +
                            "The -f flag creates a flat line",
            min = 1,
            max = 2
    )
    @CommandPermissions("worldedit.brush.line")
    public BrushSettings lineBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("0") double radius, @Switch('h') boolean shell, @Switch('s') boolean select, @Switch('f') boolean flat, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);
        return set(session, context,
                new LineBrush(shell, select, flat))
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"spline", "spl", "curve"},
            usage = "<pattern>",
            desc = "Join multiple objects together in a curve",
            help = "Click to select some objects,click the same block twice to connect the objects.\n" +
                    "Insufficient brush radius, or clicking the the wrong spot will result in undesired shapes. The shapes must be simple lines or loops.\n" +
                    "Pic1: http://i.imgur.com/CeRYAoV.jpg -> http://i.imgur.com/jtM0jA4.png\n" +
                    "Pic2: http://i.imgur.com/bUeyc72.png -> http://i.imgur.com/tg6MkcF.png" +
                    "Tutorial: https://www.planetminecraft.com/blog/fawe-tutorial/",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.brush.spline")
    public BrushSettings splineBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("25") double radius, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);
        player.print(BBC.getPrefix() + BBC.BRUSH_SPLINE.f(radius));
        return set(session, context,
                new SplineBrush(player, session))
                .setSize(radius)
                .setFill(fill);
    }

    // Adapted from: https://github.com/Rafessor/VaeronTools
    @Command(
            aliases = {"sweep", "sw", "vaesweep"},
            usage = "[copies=-1]",
            desc = "Sweep your clipboard content along a curve",
            help = "Sweeps your clipboard content along a curve.\n" +
                    "Define a curve by selecting the individual points with a brush\n" +
                    "Set [copies] to a value > 0 if you want to have your selection pasted a limited amount of times equally spaced on the curve",
            max = 1
    )
    @CommandPermissions("worldedit.brush.sweep")
    public BrushSettings sweepBrush(Player player, LocalSession session, EditSession editSession, @Optional("-1") int copies, CommandContext context) throws WorldEditException {
        player.print(BBC.getPrefix() + BBC.BRUSH_SPLINE.s());
        return set(session, context, new SweepBrush(copies));
    }

    @Command(
            aliases = {"catenary", "cat", "gravityline", "saggedline"},
            usage = "<pattern> [lengthFactor=1.2] [size=0]",
            desc = "Create a hanging line between two points",
            help = "Create a hanging line between two points.\n" +
                    "The lengthFactor controls how long the line is\n" +
                    "The -h flag creates only a shell\n" +
                    "The -s flag selects the clicked point after drawing\n" +
                    "The -d flag sags the catenary toward the facing direction\n",
            min = 1,
            max = 3
    )
    @CommandPermissions("worldedit.brush.spline")
    public BrushSettings catenaryBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("1.2") @Range(min=1) double lengthFactor, @Optional("0") double radius, @Switch('h') boolean shell, @Switch('s') boolean select, @Switch('d') boolean facingDirection, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);
        return set(session, context,
                new CatenaryBrush(shell, select, facingDirection, lengthFactor))
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"sspl", "sspline", "surfacespline"},
            usage = "<pattern> [size=0] [tension=0] [bias=0] [continuity=0] [quality=10]",
            desc = "Draws a spline (curved line) on the surface",
            help = "Create a spline on the surface\n" +
                    "Video: https://www.youtube.com/watch?v=zSN-2jJxXlM",
            min = 0,
            max = 6
    )
    @CommandPermissions("worldedit.brush.surfacespline") // 0, 0, 0, 10, 0,
    public BrushSettings surfaceSpline(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("0") double radius, @Optional("0") double tension, @Optional("0") double bias, @Optional("0") double continuity, @Optional("10") double quality, CommandContext context) throws WorldEditException {
        player.print(BBC.getPrefix() + BBC.BRUSH_SPLINE.f(radius));
        getWorldEdit().checkMaxBrushRadius(radius);
        return set(session, context,
                new SurfaceSpline(tension, bias, continuity, quality))
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"rock", "blob"},
            usage = "<pattern> [radius=10] [roundness=100] [frequency=30] [amplitude=50]",
            flags = "h",
            desc = "Creates a distorted sphere",
            min = 1,
            max = 5
    )
    @CommandPermissions("worldedit.brush.rock")
    public BrushSettings blobBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("10") Vector radius, @Optional("100") double sphericity, @Optional("30") double frequency, @Optional("50") double amplitude, CommandContext context) throws WorldEditException {
        double max = MathMan.max(radius.getBlockX(), radius.getBlockY(), radius.getBlockZ());
        getWorldEdit().checkMaxBrushRadius(max);
        Brush brush = new BlobBrush(radius.divide(max), frequency / 100, amplitude / 100, sphericity / 100);
        return set(session, context,
                brush)
                .setSize(max)
                .setFill(fill);
    }

    @Command(
            aliases = {"sphere", "s"},
            usage = "<pattern> [radius=2]",
            flags = "hf",
            desc = "Creates a sphere",
            help =
                    "Creates a sphere.\n" +
                            "The -h flag creates hollow spheres instead." +
                            "The -f flag creates falling spheres.",
            min = 1,
            max = 2
    )
    @CommandPermissions("worldedit.brush.sphere")
    public BrushSettings sphereBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("2") @Range(min=0) double radius, @Switch('h') boolean hollow, @Switch('f') boolean falling, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);

        Brush brush;
        if (hollow) {
            brush = new HollowSphereBrush();
        } else {
            if (fill instanceof BlockStateHolder) {
                BlockType type = ((BlockStateHolder) fill).getBlockType();
                if (type == BlockTypes.SAND || type == BlockTypes.GRAVEL) {
                    BBC.BRUSH_TRY_OTHER.send(player);
                    falling = true;
                }
            }
            if (falling) {
                brush = new FallingSphere();
            } else {
                brush = new SphereBrush();
            }

        }
        return set(session, context,
                brush)
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"shatter", "partition", "split"},
            usage = "<pattern> [radius=10] [count=10]",
            desc = "Creates random lines to break the terrain into pieces",
            help =
                    "Creates uneven lines separating terrain into multiple pieces\n" +
                            "Pic: https://i.imgur.com/2xKsZf2.png",
            min = 1,
            max = -1
    )
    @CommandPermissions("worldedit.brush.shatter")
    public BrushSettings shatterBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("10") double radius, @Optional("10") int count, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);
        return set(session, context,
                new ShatterBrush(count))
                .setSize(radius)
                .setFill(fill)
                .setMask(new ExistingBlockMask(editSession));
    }

    @Command(
            aliases = {"stencil"},
            usage = "<pattern> [radius=5] [file|#clipboard|imgur=null] [rotation=360] [yscale=1.0]",
            desc = "Use a height map to paint a surface",
            help =
                    "Use a height map to paint any surface.\n" +
                            "The -w flag will only apply at maximum saturation\n" +
                            "The -r flag will apply random rotation",
            min = 1,
            max = -1
    )
    @CommandPermissions("worldedit.brush.stencil")
    public BrushSettings stencilBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("5") double radius, @Optional("") final String image, @Optional("0") @Step(90) @Range(min=0, max=360) final int rotation, @Optional("1") final double yscale, @Switch('w') boolean onlyWhite, @Switch('r') boolean randomRotate, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);
        InputStream stream = getHeightmapStream(image);
        HeightBrush brush;
        try {
            brush = new StencilBrush(stream, rotation, yscale, onlyWhite, image.equalsIgnoreCase("#clipboard") ? session.getClipboard().getClipboard() : null);
        } catch (EmptyClipboardException ignore) {
            brush = new StencilBrush(stream, rotation, yscale, onlyWhite, null);
        }
        if (randomRotate) {
            brush.setRandomRotate(true);
        }
        return set(session, context,
                brush)
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"image", "color"},
            usage = "<radius> <image> [yscale=1]",
            desc = "Use a height map to paint a surface",
            flags = "a",
            help =
                    "Use a height map to paint any surface.\n" +
                            "The -a flag will use image alpha\n" +
                            "The -f blends the image with the existing terrain",
            min = 1,
            max = -1
    )
    @CommandPermissions("worldedit.brush.stencil")
    public BrushSettings imageBrush(Player player, EditSession editSession, LocalSession session, @Optional("5") double radius, BufferedImage image, @Optional("1") @Range(min=Double.MIN_NORMAL) final double yscale, @Switch('a') boolean alpha, @Switch('f') boolean fadeOut, CommandContext context) throws WorldEditException, IOException {
        getWorldEdit().checkMaxBrushRadius(radius);
        if (yscale != 1) {
            ImageUtil.scaleAlpha(image, yscale);
            alpha = true;
        }
        if (fadeOut) {
            ImageUtil.fadeAlpha(image);
            alpha = true;
        }
        ImageBrush brush = new ImageBrush(image, session, alpha);
        return set(session, context,
                brush)
                .setSize(radius);
    }

    @Command(
            aliases = {"surface", "surf"},
            usage = "<pattern> [radius=5]",
            desc = "Use a height map to paint a surface",
            help =
                    "Use a height map to paint any surface.\n" +
                            "The -w flag will only apply at maximum saturation\n" +
                            "The -r flag will apply random rotation",
            min = 1,
            max = -1
    )
    @CommandPermissions("worldedit.brush.surface")
    public BrushSettings surfaceBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("5") double radius, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);
        return set(session, context, new SurfaceSphereBrush()).setFill(fill).setSize(radius);
    }

    @Command(
            aliases = {"scatter", "scat"},
            usage = "<pattern> [radius=5] [points=5] [distance=1]",
            desc = "Scatter a pattern on a surface",
            help =
                    "Set a number of blocks randomly on a surface each a certain distance apart.\n" +
                            " The -o flag will overlay the block\n" +
                            "Video: https://youtu.be/RPZIaTbqoZw?t=34s",
            flags = "o",
            min = 1,
            max = 4
    )
    @CommandPermissions("worldedit.brush.scatter")
    public BrushSettings scatterBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("5") double radius, @Optional("5") double points, @Optional("1") double distance, @Switch('o') boolean overlay, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);
        Brush brush;
        if (overlay) {
            brush = new ScatterOverlayBrush((int) points, (int) distance);
        } else {
            brush = new ScatterBrush((int) points, (int) distance);
        }
        return set(session, context,
                brush)
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"populateschematic", "populateschem", "popschem", "pschem", "ps"},
            usage = "<mask> <file|folder|url> [radius=30] [points=5]",
            desc = "Scatter a schematic on a surface",
            help =
                    "Chooses the scatter schematic brush.\n" +
                            "The -r flag will apply random rotation",
            flags = "r",
            min = 2,
            max = 4
    )
    @CommandPermissions("worldedit.brush.populateschematic")
    public BrushSettings scatterSchemBrush(Player player, EditSession editSession, LocalSession session, Mask mask, String clipboard, @Optional("30") double radius, @Optional("50") double density, @Switch('r') boolean rotate, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);


        try {
            MultiClipboardHolder clipboards = ClipboardFormat.SCHEMATIC.loadAllFromInput(player, clipboard, true);
            if (clipboards == null) {
                BBC.SCHEMATIC_NOT_FOUND.send(player, clipboard);
                return null;
            }
            List<ClipboardHolder> holders = clipboards.getHolders();
            if (holders == null) {
                BBC.SCHEMATIC_NOT_FOUND.send(player, clipboard);
                return null;
            }

            return set(session, context,
                    new PopulateSchem(mask, holders, (int) density, rotate))
                    .setSize(radius);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Command(
            aliases = {"layer"},
            usage = "<radius> [color|<pattern1> <patern2>...]",
            desc = "Replaces terrain with a layer.",
            help = "Replaces terrain with a layer.\n" +
                    "Example: /br layer 5 95:1 95:2 35:15 - Places several layers on a surface\n" +
                    "Pic: https://i.imgur.com/XV0vYoX.png",
            min = 0,
            max = 999
    )
    @CommandPermissions("worldedit.brush.layer")
    public BrushSettings surfaceLayer(Player player, EditSession editSession, LocalSession session, double radius, CommandContext args, CommandContext context) throws WorldEditException, InvalidUsageException {
        getWorldEdit().checkMaxBrushRadius(radius);
        ParserContext parserContext = new ParserContext();
        parserContext.setActor(player);
        parserContext.setWorld(player.getWorld());
        parserContext.setSession(session);
        parserContext.setExtent(editSession);
        List<BlockStateHolder> blocks = new ArrayList<>();
        if (args.argsLength() < 2) {
            throw new InvalidUsageException(getCallable());
        }
        try {
            Color color = ColorUtil.parseColor(args.getString(1));
            BlockType[] glassLayers = Fawe.get().getTextureUtil().getNearestLayer(color.getRGB());
            for (BlockType layer : glassLayers) {
                blocks.add(layer.getDefaultState());
            }
        } catch (IllegalArgumentException ignore) {
            for (int i = 1; i < args.argsLength(); i++) {
                String arg = args.getString(i);
                blocks.add(getWorldEdit().getBlockFactory().parseFromInput(arg, parserContext));
            }
        }
        return set(session, context,
                new LayerBrush(blocks.toArray(new BlockStateHolder[blocks.size()])))
                .setSize(radius);
    }

    @Command(
            aliases = {"splatter", "splat"},
            usage = "<pattern> [radius=5] [seeds=1] [recursion=5] [solid=true]",
            desc = "Splatter a pattern on a surface",
            help = "Sets a bunch of blocks randomly on a surface.\n" +
                    "Pic: https://i.imgur.com/hMD29oO.png\n" +
                    "Example: /br splatter stone,dirt 30 15\n" +
                    "Note: The seeds define how many splotches there are, recursion defines how large, solid defines whether the pattern is applied per seed, else per block.",
            min = 1,
            max = 5
    )
    @CommandPermissions("worldedit.brush.splatter")
    public BrushSettings splatterBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("5") double radius, @Optional("1") double points, @Optional("5") double recursion, @Optional("true") boolean solid, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);
        return set(session, context,
                new SplatterBrush((int) points, (int) recursion, solid))
                .setSize(radius)
                .setFill(fill);
    }

    @Command(
            aliases = {"scmd", "scattercmd", "scattercommand", "scommand"},
            usage = "<scatter-radius> <points> <cmd-radius=1> <cmd1;cmd2...>",
            desc = "Run commands at random points on a surface",
            help =
                    "Run commands at random points on a surface\n" +
                            " - The scatter radius is the min distance between each point\n" +
                            " - Your selection will be expanded to the specified size around each point\n" +
                            " - Placeholders: {x}, {y}, {z}, {world}, {size}",
            min = 1,
            max = -1
    )
    @CommandPermissions("worldedit.brush.scattercommand")
    public BrushSettings scatterCommandBrush(Player player, EditSession editSession, LocalSession session, double radius, double points, double distance, CommandContext args, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);
        return set(session, context,
                new ScatterCommand((int) points, (int) distance, args.getJoinedStrings(3)))
                .setSize(radius);
    }

    @Command(
            aliases = {"cylinder", "cyl", "c", "disk", "disc"},
            usage = "<pattern> [radius=2] [height=1]",
            flags = "h",
            desc = "Creates a cylinder",
            help =
                    "Creates a cylinder.\n" +
                            "The -h flag creates hollow cylinders instead.",
            min = 1,
            max = 3
    )
    @CommandPermissions("worldedit.brush.cylinder")
    public BrushSettings cylinderBrush(Player player, EditSession editSession, LocalSession session, Pattern fill,
                                       @Optional("2") double radius, @Optional("1") int height, @Switch('h') boolean hollow, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);
        getWorldEdit().checkMaxBrushRadius(height);

        BrushSettings settings;
        if (hollow) {
            settings = set(session, context, new HollowCylinderBrush(height));
        } else {
            settings = set(session, context, new CylinderBrush(height));
        }
        settings.setSize(radius)
                .setFill(fill);
        return settings;
    }

    @Command(
            aliases = {"clipboard"},
            usage = "",
            desc = "Choose the clipboard brush (Recommended: `/br copypaste`)",
            help =
                    "Chooses the clipboard brush.\n" +
                            "The -a flag makes it not paste air.\n" +
                            "Without the -p flag, the paste will appear centered at the target location. " +
                            "With the flag, then the paste will appear relative to where you had " +
                            "stood relative to the copied area when you copied it."
    )
    @CommandPermissions("worldedit.brush.clipboard")
    public BrushSettings clipboardBrush(Player player, LocalSession session, @Switch('a') boolean ignoreAir, @Switch('p') boolean usingOrigin, CommandContext context) throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();

        Vector size = clipboard.getDimensions();

        getWorldEdit().checkMaxBrushRadius(size.getBlockX());
        getWorldEdit().checkMaxBrushRadius(size.getBlockY());
        getWorldEdit().checkMaxBrushRadius(size.getBlockZ());
        return set(session, context, new ClipboardBrush(holder, ignoreAir, usingOrigin));
    }

    @Command(
            aliases = {"smooth"},
            usage = "[size=2] [iterations=4]",
            flags = "n",
            desc = "Smooths terrain (Recommended: `/br blendball`)",
            help =
                    "Chooses the terrain softener brush.\n" +
                            "The -n flag makes it only consider naturally occurring blocks.",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.brush.smooth")
    public BrushSettings smoothBrush(Player player, LocalSession session, EditSession editSession,
                                     @Optional("2") double radius, @Optional("4") int iterations, CommandContext context) throws WorldEditException {

        getWorldEdit().checkMaxBrushRadius(radius);

        FawePlayer fp = FawePlayer.wrap(player);
        FaweLimit limit = Settings.IMP.getLimit(fp);
        iterations = Math.min(limit.MAX_ITERATIONS, iterations);

        return set(session, context,
                new SmoothBrush(iterations))
                .setSize(radius);
    }

    @Command(
            aliases = {"ex", "extinguish"},
            usage = "[radius=5]",
            desc = "Shortcut fire extinguisher brush",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.ex")
    public BrushSettings extinguishBrush(Player player, LocalSession session, EditSession editSession, @Optional("5") double radius, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);

        Pattern fill = BlockTypes.AIR.getDefaultState();
        return set(session, context,
                new SphereBrush())
                .setSize(radius)
                .setFill(fill)
                .setMask(new SingleBlockTypeMask(editSession, BlockTypes.FIRE));
    }

    @Command(
            aliases = {"gravity", "grav"},
            usage = "[radius=5]",
            flags = "h",
            desc = "Gravity brush",
            help =
                    "This brush simulates the affect of gravity.\n" +
                            "The -h flag makes it affect blocks starting at the world's max y, " +
                            "instead of the clicked block's y + radius.",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.gravity")
    public BrushSettings gravityBrush(Player player, LocalSession session, @Optional("5") double radius, @Switch('h') boolean fromMaxY, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);

        return set(session, context,
                new GravityBrush(fromMaxY))
                .setSize(radius);
    }

    @Command(
            aliases = {"height", "heightmap"},
            usage = "[radius=5] [file|#clipboard|imgur=null] [rotation=0] [yscale=1.00]",
            flags = "h",
            desc = "Raise or lower terrain using a heightmap",
            help =
                    "This brush raises and lowers land.\n" +
                            " - The `-r` flag enables random off-axis rotation\n" +
                            " - The `-l` flag will work on snow layers\n" +
                            " - The `-s` flag disables smoothing\n" +
                            "Note: Use a negative yscale to reduce height\n" +
                            "Snow Pic: https://i.imgur.com/Hrzn0I4.png",
            min = 1,
            max = 4
    )
    @CommandPermissions("worldedit.brush.height")
    public BrushSettings heightBrush(Player player, LocalSession session, @Optional("5") double radius, @Optional("") final String image, @Optional("0") @Step(90) @Range(min=0, max=360) final int rotation, @Optional("1") final double yscale, @Switch('r') boolean randomRotate, @Switch('l') boolean layers, @Switch('s') boolean dontSmooth, CommandContext context) throws WorldEditException {
        return terrainBrush(player, session, radius, image, rotation, yscale, false, randomRotate, layers, !dontSmooth, ScalableHeightMap.Shape.CONE, context);
    }

    @Command(
            aliases = {"cliff", "flatcylinder"},
            usage = "[radius=5] [file|#clipboard|imgur=null] [rotation=0] [yscale=1.00]",
            flags = "h",
            desc = "Cliff brush",
            help =
                    "This brush flattens terrain and creates cliffs.\n" +
                            " - The `-r` flag enables random off-axis rotation\n" +
                            " - The `-l` flag will work on snow layers\n" +
                            " - The `-s` flag disables smoothing",
            min = 1,
            max = 4
    )
    @CommandPermissions("worldedit.brush.height")
    public BrushSettings cliffBrush(Player player, LocalSession session, @Optional("5") double radius, @Optional("") final String image, @Optional("0") @Step(90) @Range(min=0, max=360) final int rotation, @Optional("1") final double yscale, @Switch('r') boolean randomRotate, @Switch('l') boolean layers, @Switch('s') boolean dontSmooth, CommandContext context) throws WorldEditException {
        return terrainBrush(player, session, radius, image, rotation, yscale, true, randomRotate, layers, !dontSmooth, ScalableHeightMap.Shape.CYLINDER, context);
    }

    @Command(
            aliases = {"flatten", "flatmap", "flat"},
            usage = "[radius=5] [file|#clipboard|imgur=null] [rotation=0] [yscale=1.00]",
            flags = "h",
            help = "Flatten brush flattens terrain\n" +
                    " - The `-r` flag enables random off-axis rotation\n" +
                    " - The `-l` flag will work on snow layers\n" +
                    " - The `-s` flag disables smoothing",
            desc = "This brush raises or lowers land towards the clicked point",
            min = 1,
            max = 4
    )
    @CommandPermissions("worldedit.brush.height")
    public BrushSettings flattenBrush(Player player, LocalSession session, @Optional("5") double radius, @Optional("") final String image, @Optional("0") @Step(90) @Range(min=0, max=360) final int rotation, @Optional("1") final double yscale, @Switch('r') boolean randomRotate, @Switch('l') boolean layers, @Switch('s') boolean dontSmooth, CommandContext context) throws WorldEditException {
        return terrainBrush(player, session, radius, image, rotation, yscale, true, randomRotate, layers, !dontSmooth, ScalableHeightMap.Shape.CONE, context);
    }

    private BrushSettings terrainBrush(Player player, LocalSession session, double radius, String image, int rotation, double yscale, boolean flat, boolean randomRotate, boolean layers, boolean smooth, ScalableHeightMap.Shape shape, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);
        InputStream stream = getHeightmapStream(image);
        HeightBrush brush;
        if (flat) {
            try {
                brush = new FlattenBrush(stream, rotation, yscale, layers, smooth, image.equalsIgnoreCase("#clipboard") ? session.getClipboard().getClipboard() : null, shape);
            } catch (EmptyClipboardException ignore) {
                brush = new FlattenBrush(stream, rotation, yscale, layers, smooth, null, shape);
            }
        } else {
            try {
                brush = new HeightBrush(stream, rotation, yscale, layers, smooth, image.equalsIgnoreCase("#clipboard") ? session.getClipboard().getClipboard() : null);
            } catch (EmptyClipboardException ignore) {
                brush = new HeightBrush(stream, rotation, yscale, layers, smooth, null);
            }
        }
        if (randomRotate) {
            brush.setRandomRotate(true);
        }
        return set(session, context,
                brush)
                .setSize(radius);
    }

    private InputStream getHeightmapStream(String filename) {
        String filenamePng = (filename.endsWith(".png") ? filename : filename + ".png");
        File file = new File(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HEIGHTMAP + File.separator + filenamePng);
        if (!file.exists()) {
            if (!filename.equals("#clipboard") && filename.length() >= 7) {
                try {
                    URL url;
                    if (filename.startsWith("http")) {
                        url = new URL(filename);
                        if (!url.getHost().equals("i.imgur.com")) {
                            throw new FileNotFoundException(filename);
                        }
                    } else {
                        url = new URL("https://i.imgur.com/" + filenamePng);
                    }
                    ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                    return Channels.newInputStream(rbc);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (!filename.equalsIgnoreCase("#clipboard")) {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }


    @Command(
            aliases = {"copypaste", "copy", "paste", "cp", "copypasta"},
            usage = "[depth=5]",
            desc = "Copy Paste brush",
            help = "Left click the base of an object to copy.\n" +
                    "Right click to paste\n" +
                    "The -r flag Will apply random rotation on paste\n" +
                    "The -a flag Will apply auto view based rotation on paste\n" +
                    "Note: Works well with the clipboard scroll action\n" +
                    "Video: https://www.youtube.com/watch?v=RPZIaTbqoZw",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.copy")
    public BrushSettings copy(Player player, LocalSession session, @Optional("5") double radius, @Switch('r') boolean randomRotate, @Switch('a') boolean autoRotate, CommandContext context) throws WorldEditException {
        getWorldEdit().checkMaxBrushRadius(radius);
        player.print(BBC.getPrefix() + BBC.BRUSH_COPY.f(radius));

        return set(session, context,
                new CopyPastaBrush(player, session, randomRotate, autoRotate))
                .setSize(radius);
    }

    @Command(
            aliases = {"command", "cmd"},
            usage = "<radius> [cmd1;cmd2...]",
            desc = "Command brush",
            help =
                    "Run the commands at the clicked position.\n" +
                            " - Your selection will be expanded to the specified size around each point\n" +
                            " - Placeholders: {x}, {y}, {z}, {world}, {size}",

            min = 2,
            max = 99
    )
    @CommandPermissions("worldedit.brush.command")
    public BrushSettings command(Player player, LocalSession session, double radius, CommandContext args, CommandContext context) throws WorldEditException {
        String cmd = args.getJoinedStrings(1);
        return set(session, context,
                new CommandBrush(cmd, radius))
                .setSize(radius);
    }

    @Command(
            aliases = {"butcher", "kill"},
            usage = "[radius=5]",
            flags = "plangbtfr",
            desc = "Butcher brush",
            help = "Kills nearby mobs within the specified radius.\n" +
                    "Flags:\n" +
                    "  -p also kills pets.\n" +
                    "  -n also kills NPCs.\n" +
                    "  -g also kills Golems.\n" +
                    "  -a also kills animals.\n" +
                    "  -b also kills ambient mobs.\n" +
                    "  -t also kills mobs with name tags.\n" +
                    "  -f compounds all previous flags.\n" +
                    "  -r also destroys armor stands.\n" +
                    "  -l currently does nothing.",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.butcher")
    public BrushSettings butcherBrush(Player player, LocalSession session, CommandContext args, CommandContext context) throws WorldEditException {
        LocalConfiguration config = getWorldEdit().getConfiguration();

        double radius = args.argsLength() > 0 ? args.getDouble(0) : 5;
        double maxRadius = config.maxBrushRadius;
        // hmmmm not horribly worried about this because -1 is still rather efficient,
        // the problem arises when butcherMaxRadius is some really high number but not infinite
        // - original idea taken from https://github.com/sk89q/worldedit/pull/198#issuecomment-6463108
        if (player.hasPermission("worldedit.butcher")) {
            maxRadius = Math.max(config.maxBrushRadius, config.butcherMaxRadius);
        }
        if (radius > maxRadius && maxRadius != -1) {
            BBC.TOOL_RADIUS_ERROR.send(player, maxRadius);
            return null;
        }

        CreatureButcher flags = new CreatureButcher(player);
        flags.fromCommand(args);

        return set(session, context,
                new ButcherBrush(flags))
                .setSize(radius);
    }


}
