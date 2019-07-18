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
import com.boydti.fawe.command.FawePrimitiveBinding;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.brush.BlendBall;
import com.boydti.fawe.object.brush.BlobBrush;
import com.boydti.fawe.object.brush.BrushSettings;
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
import com.boydti.fawe.object.brush.sweep.SweepBrush;
import com.boydti.fawe.object.clipboard.MultiClipboardHolder;
import com.boydti.fawe.object.mask.IdMask;
import com.boydti.fawe.util.ColorUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.image.ImageUtil;
import org.enginehub.piston.inject.InjectedValueAccess;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.minecraft.util.commands.Step;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.command.tool.brush.ButcherBrush;
import com.sk89q.worldedit.command.tool.brush.ClipboardBrush;
import com.sk89q.worldedit.command.tool.brush.CylinderBrush;
import com.sk89q.worldedit.command.tool.brush.GravityBrush;
import com.sk89q.worldedit.command.tool.brush.HollowCylinderBrush;
import com.sk89q.worldedit.command.tool.brush.HollowSphereBrush;
import com.sk89q.worldedit.command.tool.brush.SmoothBrush;
import com.sk89q.worldedit.command.tool.brush.SphereBrush;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.command.util.CreatureButcher;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.SingleBlockTypeMask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.util.command.CallableProcessor;
import com.sk89q.worldedit.util.command.CommandCallable;
import com.sk89q.worldedit.util.command.InvalidUsageException;
import com.sk89q.worldedit.util.command.parametric.AParametricCallable;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.util.command.parametric.ParameterException;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;

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
            name = "blendball",
            aliases = {"bb", "blend"},
            desc = "Smooths and blends terrain",
            descFooter = "Smooths and blends terrain\n" +
                    "Pic: https://i.imgur.com/cNUQUkj.png -> https://i.imgur.com/hFOFsNf.png"
    )
    @CommandPermissions("worldedit.brush.blendball")
    public BrushSettings blendBallBrush(Player player, LocalSession session,
        @Arg(desc = "The radius to sample for blending", def = "5")
        double radius, InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        Brush brush = new BlendBall();
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius);
    }

    @Command(
            name = "erode",
            desc = "Erodes terrain"
    )
    @CommandPermissions("worldedit.brush.erode")
    public BrushSettings erodeBrush(Player player, LocalSession session,
        @Arg(desc = "The radius for eroding", def = "5")
            double radius, InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        Brush brush = new ErodeBrush();
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius);
    }

    @Command(
            name = "pull",
            desc = "Pull terrain towards you"
    )
    @CommandPermissions("worldedit.brush.pull")
    public BrushSettings pullBrush(Player player, LocalSession session,
        @Arg(desc = "The radius to sample for blending", def = "5")
        double radius, InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        Brush brush = new RaiseBrush();
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius);
    }

    @Command(
            name = "circle",
            desc = "Creates a circle which revolves around your facing direction"
    )
    @CommandPermissions("worldedit.brush.sphere")
    public BrushSettings circleBrush(Player player, EditSession editSession, LocalSession session, Pattern fill,
        @Arg(desc = "The radius to sample for blending", def = "5")
        double radius, InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        Brush brush = new CircleBrush(player);
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius).setFill(fill);
    }

    @Command(
            name = "recursive",
            aliases = {"recurse", "r"},
            desc = "Set all connected blocks",
            descFooter = "Set all connected blocks\n" +
                    "The -d flag Will apply in depth first order\n" +
                    "Note: Set a mask to recurse along specific blocks"
    )
    @CommandPermissions("worldedit.brush.recursive")
    public BrushSettings recursiveBrush(Player player, LocalSession session, EditSession editSession, Pattern fill,
        @Arg(desc = "The radius to sample for blending", def = "5")
            double radius, @Switch(name='d', desc = "TODO") boolean depthFirst, InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        Brush brush = new RecurseBrush(depthFirst);
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius).setFill(fill).setMask(new IdMask(editSession));
    }

    @Command(
            name = "line",
            aliases = {"l"},
            desc = "Create lines",
            descFooter = "Create lines.\n" +
                   "The -h flag creates only a shell\n" +
                   "The -s flag selects the clicked point after drawing\n" +
                   "The -f flag creates a flat line"
    )
    @CommandPermissions("worldedit.brush.line")
    public BrushSettings lineBrush(Player player, EditSession editSession, LocalSession session, Pattern fill,
        @Arg(desc = "The radius to sample for blending", def = "0")
        double radius, @Switch(name='h', desc = "TODO") boolean shell, @Switch(name='s', desc = "TODO") boolean select, @Switch(name='f', desc = "TODO") boolean flat, InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        Brush brush = new LineBrush(shell, select, flat);
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius).setFill(fill);
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
    public BrushSettings splineBrush(Player player, EditSession editSession, LocalSession session, Pattern fill,
        @Arg(desc = "The radius to sample for blending", def = "25")
        double radius, InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        player.print(BBC.BRUSH_SPLINE.f(radius));
        Brush brush = new SplineBrush(player, session);
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius).setFill(fill);
    }

    // Adapted from: https://github.com/Rafessor/VaeronTools
    @Command(
            name = "sweep",
            aliases = {"sw", "vaesweep"},
            desc = "Sweep your clipboard content along a curve",
            descFooter = "Sweeps your clipboard content along a curve.\n" +
                   "Define a curve by selecting the individual points with a brush\n" +
                   "Set [copies] to a value > 0 if you want to have your selection pasted a limited amount of times equally spaced on the curve"
    )
    @CommandPermissions("worldedit.brush.sweep")
    public BrushSettings sweepBrush(Player player, LocalSession session, EditSession editSession, @Arg(name = "copies", desc = "int", def = "-1") int copies, InjectedValueAccess context) throws WorldEditException {
        player.print(BBC.BRUSH_SPLINE.s());
        Brush brush = new SweepBrush(copies);
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush);
    }

    @Command(
            name = "catenary",
            aliases = {"cat", "gravityline", "saggedline"},
            desc = "Create a hanging line between two points",
            descFooter = "Create a hanging line between two points.\n" +
                    "The lengthFactor controls how long the line is\n" +
                    "The -h flag creates only a shell\n" +
                    "The -s flag selects the clicked point after drawing\n" +
                    "The -d flag sags the catenary toward the facing direction\n"
    )
    @CommandPermissions("worldedit.brush.spline")
    public BrushSettings catenaryBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Optional("1.2") @Range(min=1) double lengthFactor,
        @Arg(desc = "The radius to sample for blending", def = "0")
        double radius, @Switch(name='h', desc = "TODO") boolean shell, @Switch(name='s', desc = "TODO") boolean select, @Switch(name='d', desc = "TODO") boolean facingDirection, InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        Brush brush = new CatenaryBrush(shell, select, facingDirection, lengthFactor);
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius).setFill(fill);
    }

    @Command(
            name = "sspl",
            aliases = {"sspline", "surfacespline"},
            desc = "Draws a spline (curved line) on the surface",
            descFooter = "Create a spline on the surface\n" +
                   "Video: https://www.youtube.com/watch?v=zSN-2jJxXlM"
    )
    @CommandPermissions("worldedit.brush.surfacespline") // 0, 0, 0, 10, 0,
    public BrushSettings surfaceSpline(Player player, EditSession editSession, LocalSession session, Pattern fill,
        @Arg(desc = "The radius to sample for blending", def = "0")
            double radius, @Arg(name = "tension", desc = "double", def = "0") double tension, @Arg(name = "bias", desc = "double", def = "0") double bias, @Arg(name = "continuity", desc = "double", def = "0") double continuity, @Arg(name = "quality", desc = "double", def = "10") double quality, InjectedValueAccess context) throws WorldEditException {
        player.print(BBC.BRUSH_SPLINE.f(radius));
        worldEdit.checkMaxBrushRadius(radius);
        Brush brush = new SurfaceSpline(tension, bias, continuity, quality);
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius).setFill(fill);
    }

    @Command(
            name = "rock",
            aliases = {"blob"},
            desc = "Creates a distorted sphere"
    )
    @CommandPermissions("worldedit.brush.rock")
    public BrushSettings blobBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Arg(name = "radius", desc = "Vector3", def = "10") Vector3 radius, @Arg(name = "sphericity", desc = "double", def = "100") double sphericity, @Arg(name = "frequency", desc = "double", def = "30") double frequency, @Arg(name = "amplitude", desc = "double", def = "50") double amplitude, InjectedValueAccess context) throws WorldEditException {
        double max = MathMan.max(radius.getX(), radius.getY(), radius.getZ());
        worldEdit.checkMaxBrushRadius(max);
        Brush brush = new BlobBrush(radius.divide(max), frequency / 100, amplitude / 100, sphericity / 100);
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(max).setFill(fill);
    }

    @Command(
        name = "sphere",
        aliases = { "s" },
        desc = "Choose the sphere brush"
    )
    @CommandPermissions("worldedit.brush.sphere")
    public BrushSettings sphereBrush(Player player, LocalSession session,
        @Arg(desc = "The pattern of blocks to set")
            Pattern pattern,
        @Arg(desc = "The radius of the sphere", def = "2")
            double radius,
        @Switch(name = 'h', desc = "Create hollow spheres instead")
            boolean hollow,
        @Switch(name = 'f', desc = "Create falling spheres instead")
            boolean falling, InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        Brush brush;
        if (hollow) {
            brush = new HollowSphereBrush();
        } else {
            if (fill instanceof BlockStateHolder) {
                BlockType type = ((BlockStateHolder) fill).getBlockType();
                int internalId = type.getInternalId();
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
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius).setFill(pattern);
    }

    @Command(
            name = "shatter",
            aliases = {"partition", "split"},
            desc = "Creates random lines to break the terrain into pieces",
            descFooter = "Creates uneven lines separating terrain into multiple pieces\n" +
                   "Pic: https://i.imgur.com/2xKsZf2.png"
    )
    @CommandPermissions("worldedit.brush.shatter")
    public BrushSettings shatterBrush(Player player, EditSession editSession, LocalSession session, Pattern fill,
        @Arg(desc = "The radius to sample for blending", def = "10")
        double radius,
        @Arg(desc = "Lines", def = "10") int count, InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        Brush brush = new ShatterBrush(count);
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius).setFill(fill).setMask(new ExistingBlockMask(editSession));
    }

    @Command(
            name = "stencil",
            desc = "Use a height map to paint a surface",
            descFooter = "Use a height map to paint any surface.\n" +
                   "The -w flag will only apply at maximum saturation\n" +
                   "The -r flag will apply random rotation"
)
    @CommandPermissions("worldedit.brush.stencil")
    public BrushSettings stencilBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Arg(name = "radius", desc = "Expression", def = "5") Expression radius, @Arg(name = "image", desc = "String", def = "") String image, @Optional("0") @Step(90) @Range(min=0, max=360) final int rotation, @Arg(name = "yscale", desc = "double", def = "1") final double yscale, @Switch(name='w', desc = "TODO") boolean onlyWhite, @Switch(name='r', desc = "TODO") boolean randomRotate, InjectedValueAccess context) throws WorldEditException, FileNotFoundException, ParameterException {
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
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == ((Brush) brush).getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius).setFill(fill);
    }

    @Command(
            name = "image",
            aliases = {"color"},
            desc = "Use a height map to paint a surface",
            descFooter = "Use a height map to paint any surface.\n" +
                   "The -a flag will use image alpha\n" +
                   "The -f blends the image with the existing terrain"
)
    @CommandPermissions("worldedit.brush.stencil")
    public BrushSettings imageBrush(Player player, EditSession editSession, LocalSession session, @Arg(name = "radius", desc = "Expression", def = "5") Expression radius, FawePrimitiveBinding.ImageUri imageUri, @Optional("1") @Range(min=Double.MIN_NORMAL) final double yscale, @Switch(name='a', desc = "TODO") boolean alpha, @Switch(name='f', desc = "TODO") boolean fadeOut, InjectedValueAccess context) throws WorldEditException, IOException, ParameterException {
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
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == ((Brush) brush).getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius);
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
    public BrushSettings surfaceBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Arg(name = "radius", desc = "Expression", def = "5") Expression radius, InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        Brush brush = new SurfaceSphereBrush();
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setFill(fill).setSize(radius);
    }

    @Command(
            name = "scatter",
            desc = "Scatter a pattern on a surface",
            descFooter = "Set a number of blocks randomly on a surface each a certain distance apart.\n" +
                   " The -o flag will overlay the block\n" +
                   "Video: https://youtu.be/RPZIaTbqoZw?t=34s"
)
    @CommandPermissions("worldedit.brush.scatter")
    public BrushSettings scatterBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Arg(name = "radius", desc = "Expression", def = "5") Expression radius, @Arg(name = "points", desc = "double", def = "5") double points, @Arg(name = "distance", desc = "double", def = "1") double distance, @Switch(name='o', desc = "TODO") boolean overlay, InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        Brush brush;
        if (overlay) {
            brush = new ScatterOverlayBrush((int) points, (int) distance);
        } else {
            brush = new ScatterBrush((int) points, (int) distance);
        }
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius).setFill(fill);
    }

    @Command(
            name = "populateschematic",
            aliases = {"populateschem", "popschem", "pschem", "ps"},
            desc = "Scatter a schematic on a surface",
            descFooter = "Chooses the scatter schematic brush.\n" +
                   "The -r flag will apply random rotation"
    )
    @CommandPermissions("worldedit.brush.populateschematic")
    public BrushSettings scatterSchemBrush(Player player, EditSession editSession, LocalSession session, Mask mask, String clipboard, @Arg(name = "radius", desc = "Expression", def = "30") Expression radius, @Arg(name = "density", desc = "double", def = "50") double density, @Switch(name='r', desc = "TODO") boolean rotate, InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);


        try {
            MultiClipboardHolder clipboards = ClipboardFormats.loadAllFromInput(player, clipboard, null, true);
            if (clipboards == null) {
                BBC.SCHEMATIC_NOT_FOUND.send(player, clipboard);
                return null;
            }
            List<ClipboardHolder> holders = clipboards.getHolders();
            if (holders == null) {
                BBC.SCHEMATIC_NOT_FOUND.send(player, clipboard);
                return null;
            }

            Brush brush = new PopulateSchem(mask, holders, (int) density, rotate);
            CommandLocals locals = context.getLocals();
            BrushSettings bs = new BrushSettings();

            BrushTool tool = session.getBrushTool(player, false);
            if (tool != null) {
                BrushSettings currentContext = tool.getContext();
                if (currentContext != null) {
                    Brush currentBrush = currentContext.getBrush();
                    if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                        bs = currentContext;
                    }
                }
            }

            CommandCallable callable = locals.get(CommandCallable.class);
            String[] perms;
            if (callable instanceof AParametricCallable) {
                perms = ((AParametricCallable) callable).getPermissions();
            } else {
                perms = getPermissions();
            }
            bs.addPermissions(perms);

            String args = (String) locals.get("arguments");
            if (args != null) {
                bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
            }
            return bs.setBrush(brush).setSize(radius);
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
    public BrushSettings surfaceLayer(Player player, EditSession editSession, LocalSession session, Expression radius, InjectedValueAccess args, InjectedValueAccess context) throws WorldEditException, InvalidUsageException {
        worldEdit.checkMaxBrushRadius(radius);
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
                blocks.add(worldEdit.getBlockFactory().parseFromInput(arg, parserContext));
            }
        }
        Brush brush = new LayerBrush(blocks.toArray(new BlockStateHolder[0]));
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args1 = (String) locals.get("arguments");
        if (args1 != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args1.substring(args1.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius);
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
    public BrushSettings splatterBrush(Player player, EditSession editSession, LocalSession session, Pattern fill, @Arg(name = "radius", desc = "Expression", def = "5") Expression radius, @Arg(name = "points", desc = "double", def = "1") double points, @Arg(name = "recursion", desc = "double", def = "5") double recursion, @Arg(name = "solid", desc = "boolean", def = "true") boolean solid, InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        Brush brush = new SplatterBrush((int) points, (int) recursion, solid);
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius).setFill(fill);
    }

    @Command(
            name = "scmd",
            aliases = {"scattercmd", "scattercommand", "scommand"},
            desc = "Run commands at random points on a surface",
            descFooter =
                    "Run commands at random points on a surface\n" +
                            " - The scatter radius is the min distance between each point\n" +
                            " - Your selection will be expanded to the specified size around each point\n" +
                            " - Placeholders: {x}, {y}, {z}, {world}, {size}"
    )
    @CommandPermissions("worldedit.brush.scattercommand")
    public BrushSettings scatterCommandBrush(Player player, EditSession editSession, LocalSession session, Expression radius, double points, double distance, InjectedValueAccess args, InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        Brush brush = new ScatterCommand((int) points, (int) distance, args.getJoinedStrings(3));
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args1 = (String) locals.get("arguments");
        if (args1 != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args1.substring(args1.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius);
    }

    @Command(
        name = "cylinder",
        aliases = { "cyl", "c" },
        desc = "Choose the cylinder brush"
    )
    @CommandPermissions("worldedit.brush.cylinder")
    public BrushSettings cylinderBrush(Player player, LocalSession session,
        @Arg(desc = "The pattern of blocks to set")
            Pattern pattern,
        @Arg(desc = "The radius of the cylinder", def = "2")
            double radius,
        @Arg(desc = "The height of the cylinder", def = "1")
            int height,
        @Switch(name = 'h', desc = "Create hollow cylinders instead")
            boolean hollow,
        InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        worldEdit.checkMaxBrushRadius(height);

        BrushSettings settings;
        if (hollow) {
            Brush brush = new HollowCylinderBrush(height);
            CommandLocals locals = context.getLocals();
            BrushSettings bs = new BrushSettings();

            BrushTool tool = session.getBrushTool(player, false);
            if (tool != null) {
                BrushSettings currentContext = tool.getContext();
                if (currentContext != null) {
                    Brush currentBrush = currentContext.getBrush();
                    if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                        bs = currentContext;
                    }
                }
            }

            CommandCallable callable = locals.get(CommandCallable.class);
            String[] perms;
            if (callable instanceof AParametricCallable) {
                perms = ((AParametricCallable) callable).getPermissions();
            } else {
                perms = getPermissions();
            }
            bs.addPermissions(perms);

            String args = (String) locals.get("arguments");
            if (args != null) {
                bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
            }
            settings = bs.setBrush(brush);
        } else {
            Brush brush = new CylinderBrush(height);
            CommandLocals locals = context.getLocals();
            BrushSettings bs = new BrushSettings();

            BrushTool tool = session.getBrushTool(player, false);
            if (tool != null) {
                BrushSettings currentContext = tool.getContext();
                if (currentContext != null) {
                    Brush currentBrush = currentContext.getBrush();
                    if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                        bs = currentContext;
                    }
                }
            }

            CommandCallable callable = locals.get(CommandCallable.class);
            String[] perms;
            if (callable instanceof AParametricCallable) {
                perms = ((AParametricCallable) callable).getPermissions();
            } else {
                perms = getPermissions();
            }
            bs.addPermissions(perms);

            String args = (String) locals.get("arguments");
            if (args != null) {
                bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
            }
            settings = bs.setBrush(brush);
        }
        settings.setSize(radius).setFill(pattern);
        return settings;
    }

    @Command(
            name = "clipboard",
            aliases = { "copy" },
            desc = "Choose the clipboard brush (Recommended: `/br copypaste`)",
            descFooter = "Chooses the clipboard brush.\n" +
                   "The -a flag makes it not paste air.\n" +
                   "Without the -p flag, the paste will appear centered at the target location. " +
                   "With the flag, then the paste will appear relative to where you had " +
                   "stood relative to the copied area when you copied it."
    )
    @CommandPermissions("worldedit.brush.clipboard")
    public BrushSettings clipboardBrush(Player player, LocalSession session,
                               @Switch(name = 'a', desc = "Don't paste air from the clipboard")
                                   boolean ignoreAir,
                               @Switch(name = 'o', desc = "Paste starting at the target location, instead of centering on it")
                                   boolean usingOrigin,
                               @Switch(name = 'e', desc = "Skip paste entities if available")
                                   boolean skipEntities,
                               @Switch(name = 'b', desc = "Paste biomes if available")
                                   boolean pasteBiomes,
                               @ArgFlag(name = 'm', desc = "Skip blocks matching this mask in the clipboard", def = "")
                                   Mask sourceMask,
        InjectedValueAccess context) throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();

        BlockVector3 size = clipboard.getDimensions();

        worldEdit.checkMaxBrushRadius(size.getBlockX());
        worldEdit.checkMaxBrushRadius(size.getBlockY());
        worldEdit.checkMaxBrushRadius(size.getBlockZ());
        Brush brush = new ClipboardBrush(holder, ignoreAir, usingOrigin, !skipEntities, pasteBiomes, sourceMask);
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush);
    }

    @Command(
        name = "smooth",
        desc = "Choose the terrain softener brush",
        descFooter = "Example: '/brush smooth 2 4 grass_block,dirt,stone'"
    )
    @CommandPermissions("worldedit.brush.smooth")
    public BrushSettings smoothBrush(Player player, LocalSession session, EditSession editSession,
                            @Arg(desc = "The radius to sample for softening", def = "2")
                                double radius,
                            @Arg(desc = "The number of iterations to perform", def = "4")
                                int iterations,
                            @Arg(desc = "The mask of blocks to use for the heightmap", def = "")
                                Mask mask, InjectedValueAccess context) throws WorldEditException {

        worldEdit.checkMaxBrushRadius(radius);

        FawePlayer fp = FawePlayer.wrap(player);
        FaweLimit limit = Settings.IMP.getLimit(fp);
        iterations = Math.min(limit.MAX_ITERATIONS, iterations);

        BrushTool tool = session.getBrushTool(player, false);
        Brush brush = new SmoothBrush(iterations, mask);
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius);
    }

    @Command(
        name = "extinguish",
        aliases = { "ex" },
        desc = "Shortcut fire extinguisher brush"
    )
    @CommandPermissions("worldedit.brush.ex")
    public BrushSettings extinguishBrush(Player player, LocalSession session, EditSession editSession,
                                @Arg(desc = "The radius to extinguish", def = "5")
                                    double radius,
        InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player, false);
        Pattern fill = BlockTypes.AIR.getDefaultState();
        Brush brush = new SphereBrush();
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush)
            .setSize(radius).setFill(fill).setMask(new SingleBlockTypeMask(editSession, BlockTypes.FIRE));
    }

    @Command(
        name = "gravity",
        aliases = { "grav" },
        desc = "Gravity brush, simulates the effect of gravity"
    )
    @CommandPermissions("worldedit.brush.gravity")
    public BrushSettings gravityBrush(Player player, LocalSession session,
        @Arg(desc = "The radius to apply gravity in", def = "5")
        double radius,
        @Switch(name = 'h', desc = "Affect blocks starting at max Y, rather than the target location Y + radius")
            boolean fromMaxY,
        InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player, false);
        Brush brush = new GravityBrush(fromMaxY);
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius);
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
    public BrushSettings heightBrush(Player player, LocalSession session, @Arg(name = "radius", desc = "Expression", def = "5") Expression radius, @Arg(name = "image", desc = "String", def = "") final String image, @Optional("0") @Step(90) @Range(min=0, max=360) final int rotation, @Arg(name = "yscale", desc = "double", def = "1") final double yscale, @Switch(name='r', desc = "TODO") boolean randomRotate, @Switch(name='l', desc = "TODO") boolean layers, @Switch(name='s', desc = "TODO") boolean dontSmooth, InjectedValueAccess context) throws WorldEditException, FileNotFoundException, ParameterException {
        return terrainBrush(player, session, radius, image, rotation, yscale, false, randomRotate, layers, !dontSmooth, ScalableHeightMap.Shape.CONE, context);
    }

    @Command(
            name = "cliff",
            aliases = {"flatcylinder"},
            desc = "Cliff brush",
            descFooter = "This brush flattens terrain and creates cliffs.\n" +
                   " - The `-r` flag enables random off-axis rotation\n" +
                   " - The `-l` flag will work on snow layers\n" +
                   " - The `-s` flag disables smoothing"
    )
    @CommandPermissions("worldedit.brush.height")
    public BrushSettings cliffBrush(Player player, LocalSession session, @Arg(name = "radius", desc = "Expression", def = "5") Expression radius, @Arg(name = "image", desc = "String", def = "") String image, @Optional("0") @Step(90) @Range(min=0, max=360) final int rotation, @Arg(name = "yscale", desc = "double", def = "1") final double yscale, @Switch(name='r', desc = "TODO") boolean randomRotate, @Switch(name='l', desc = "TODO") boolean layers, @Switch(name='s', desc = "TODO") boolean dontSmooth, InjectedValueAccess context) throws WorldEditException, FileNotFoundException, ParameterException {
        return terrainBrush(player, session, radius, image, rotation, yscale, true, randomRotate, layers, !dontSmooth, ScalableHeightMap.Shape.CYLINDER, context);
    }

    @Command(
            name = "flatten",
            aliases = {"flatmap", "flat"},
            descFooter = "Flatten brush flattens terrain\n" +
                   " - The `-r` flag enables random off-axis rotation\n" +
                   " - The `-l` flag will work on snow layers\n" +
                   " - The `-s` flag disables smoothing",
            desc = "This brush raises or lowers land towards the clicked point"
    )
    @CommandPermissions("worldedit.brush.height")
    public BrushSettings flattenBrush(Player player, LocalSession session, @Arg(name = "radius", desc = "Expression", def = "5") Expression radius, @Arg(name = "image", desc = "String", def = "") final String image, @Optional("0") @Step(90) @Range(min=0, max=360) final int rotation, @Arg(name = "yscale", desc = "double", def = "1") final double yscale, @Switch(name='r', desc = "TODO") boolean randomRotate, @Switch(name='l', desc = "TODO") boolean layers, @Switch(name='s', desc = "TODO") boolean dontSmooth, InjectedValueAccess context) throws WorldEditException, FileNotFoundException, ParameterException {
        return terrainBrush(player, session, radius, image, rotation, yscale, true, randomRotate, layers, !dontSmooth, ScalableHeightMap.Shape.CONE, context);
    }

    private BrushSettings terrainBrush(Player player, LocalSession session, Expression radius, String image, int rotation, double yscale, boolean flat, boolean randomRotate, boolean layers, boolean smooth, ScalableHeightMap.Shape shape, InjectedValueAccess context) throws WorldEditException, FileNotFoundException, ParameterException {
        worldEdit.checkMaxBrushRadius(radius);
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
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == ((Brush) brush).getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius);
    }

    private InputStream getHeightmapStream(String filename) throws FileNotFoundException, ParameterException {
        if (filename == null) return null;
        String filenamePng = (filename.endsWith(".png") ? filename : filename + ".png");
        File file = new File(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HEIGHTMAP + File.separator + filenamePng);
        if (file.exists()) return new FileInputStream(file);
        URI uri = ImageUtil.getImageURI(filename);
        return ImageUtil.getInputStream(uri);
    }


    @Command(
            name = "copypaste",
            aliases = {"copy", "paste", "cp", "copypasta"},
            desc = "Copy Paste brush",
            descFooter = "Left click the base of an object to copy.\n" +
                   "Right click to paste\n" +
                   "The -r flag Will apply random rotation on paste\n" +
                   "The -a flag Will apply auto view based rotation on paste\n" +
                   "Note: Works well with the clipboard scroll action\n" +
                   "Video: https://www.youtube.com/watch?v=RPZIaTbqoZw"
    )
    @CommandPermissions("worldedit.brush.copy")
    public BrushSettings copy(Player player, LocalSession session, @Arg(name = "radius", desc = "Expression", def = "5") Expression radius, @Switch(name='r', desc = "TODO") boolean randomRotate, @Switch(name='a', desc = "TODO") boolean autoRotate, InjectedValueAccess context) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        player.print(BBC.BRUSH_COPY.f(radius));

        Brush brush = new CopyPastaBrush(player, session, randomRotate, autoRotate);
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args = (String) locals.get("arguments");
        if (args != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius);
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
    public BrushSettings command(Player player, LocalSession session, Expression radius, InjectedValueAccess args, InjectedValueAccess context) throws WorldEditException {
        String cmd = args.getJoinedStrings(1);
        Brush brush = new CommandBrush(cmd);
        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool(player, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args1 = (String) locals.get("arguments");
        if (args1 != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args1.substring(args1.indexOf(' ') + 1));
        }
        return bs.setBrush(brush).setSize(radius);
    }

    @Command(
        name = "butcher",
        aliases = { "kill" },
        desc = "Butcher brush, kills mobs within a radius"
    )
    @CommandPermissions("worldedit.brush.butcher")
    public BrushSettings butcherBrush(Player player, LocalSession session, InjectedValueAccess args,
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
            BBC.TOOL_RADIUS_ERROR.send(player, maxRadius);
            return null;
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

        BrushTool tool = session.getBrushTool(player);
        tool.setSize(radius);
        ButcherBrush brush = new ButcherBrush(flags);
        tool.setBrush(brush, "worldedit.brush.butcher");

        CommandLocals locals = context.getLocals();
        BrushSettings bs = new BrushSettings();

        BrushTool tool1 = session.getBrushTool(player, false);
        if (tool1 != null) {
            BrushSettings currentContext = tool1.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == ((Brush) brush).getClass()) {
                    bs = currentContext;
                }
            }
        }

        CommandCallable callable = locals.get(CommandCallable.class);
        String[] perms;
        if (callable instanceof AParametricCallable) {
            perms = ((AParametricCallable) callable).getPermissions();
        } else {
            perms = getPermissions();
        }
        bs.addPermissions(perms);

        String args1 = (String) locals.get("arguments");
        if (args1 != null) {
            bs.addSetting(BrushSettings.SettingType.BRUSH, args1.substring(args1.indexOf(' ') + 1));
        }
        return bs.setBrush(brush);
    }

    @Override
    public BrushSettings process(CommandLocals locals, BrushSettings settings) throws WorldEditException {
        Actor actor = locals.get(Actor.class);
        LocalSession session = worldEdit.getSessionManager().get(actor);
        session.setTool((Player) actor, null);
        BrushTool tool = session.getBrushTool((Player) actor);
        if (tool != null) {
            tool.setPrimary(settings);
            tool.setSecondary(settings);
            BBC.BRUSH_EQUIPPED.send(actor, ((String) locals.get("arguments")).split(" ")[1]);
        }
        return null;
    }

}
