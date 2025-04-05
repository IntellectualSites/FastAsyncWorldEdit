/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.command;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.command.tool.brush.BlendBall;
import com.fastasyncworldedit.core.command.tool.brush.BlobBrush;
import com.fastasyncworldedit.core.command.tool.brush.BrushSettings;
import com.fastasyncworldedit.core.command.tool.brush.CatenaryBrush;
import com.fastasyncworldedit.core.command.tool.brush.CircleBrush;
import com.fastasyncworldedit.core.command.tool.brush.CommandBrush;
import com.fastasyncworldedit.core.command.tool.brush.CopyPastaBrush;
import com.fastasyncworldedit.core.command.tool.brush.ErodeBrush;
import com.fastasyncworldedit.core.command.tool.brush.FallingSphere;
import com.fastasyncworldedit.core.command.tool.brush.FlattenBrush;
import com.fastasyncworldedit.core.command.tool.brush.HeightBrush;
import com.fastasyncworldedit.core.command.tool.brush.ImageBrush;
import com.fastasyncworldedit.core.command.tool.brush.LayerBrush;
import com.fastasyncworldedit.core.command.tool.brush.LineBrush;
import com.fastasyncworldedit.core.command.tool.brush.PopulateSchem;
import com.fastasyncworldedit.core.command.tool.brush.RaiseBrush;
import com.fastasyncworldedit.core.command.tool.brush.RecurseBrush;
import com.fastasyncworldedit.core.command.tool.brush.ScatterBrush;
import com.fastasyncworldedit.core.command.tool.brush.ScatterCommand;
import com.fastasyncworldedit.core.command.tool.brush.ScatterOverlayBrush;
import com.fastasyncworldedit.core.command.tool.brush.ShatterBrush;
import com.fastasyncworldedit.core.command.tool.brush.SplatterBrush;
import com.fastasyncworldedit.core.command.tool.brush.SplineBrush;
import com.fastasyncworldedit.core.command.tool.brush.StencilBrush;
import com.fastasyncworldedit.core.command.tool.brush.SurfaceSphereBrush;
import com.fastasyncworldedit.core.command.tool.brush.SurfaceSpline;
import com.fastasyncworldedit.core.command.tool.sweep.SweepBrush;
import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.clipboard.MultiClipboardHolder;
import com.fastasyncworldedit.core.function.mask.CachedMask;
import com.fastasyncworldedit.core.function.mask.IdMask;
import com.fastasyncworldedit.core.function.mask.SingleBlockTypeMask;
import com.fastasyncworldedit.core.limit.FaweLimit;
import com.fastasyncworldedit.core.math.heightmap.ScalableHeightMap;
import com.fastasyncworldedit.core.math.heightmap.ScalableHeightMap.Shape;
import com.fastasyncworldedit.core.util.MainUtil;
import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.StringMan;
import com.fastasyncworldedit.core.util.image.ImageUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.argument.Arguments;
import com.sk89q.worldedit.command.factory.ReplaceFactory;
import com.sk89q.worldedit.command.factory.TreeGeneratorFactory;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.InvalidToolBindException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.command.tool.brush.ButcherBrush;
import com.sk89q.worldedit.command.tool.brush.ClipboardBrush;
import com.sk89q.worldedit.command.tool.brush.CylinderBrush;
import com.sk89q.worldedit.command.tool.brush.GravityBrush;
import com.sk89q.worldedit.command.tool.brush.HollowCylinderBrush;
import com.sk89q.worldedit.command.tool.brush.HollowSphereBrush;
import com.sk89q.worldedit.command.tool.brush.OperationFactoryBrush;
import com.sk89q.worldedit.command.tool.brush.SmoothBrush;
import com.sk89q.worldedit.command.tool.brush.SnowSmoothBrush;
import com.sk89q.worldedit.command.tool.brush.SphereBrush;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.command.util.CreatureButcher;
import com.sk89q.worldedit.command.util.annotation.PatternList;
import com.sk89q.worldedit.command.util.annotation.Step;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.Contextual;
import com.sk89q.worldedit.function.factory.Apply;
import com.sk89q.worldedit.function.factory.ApplyLayer;
import com.sk89q.worldedit.function.factory.Deform;
import com.sk89q.worldedit.function.factory.Paint;
import com.sk89q.worldedit.function.factory.Snow;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.annotation.ClipboardMask;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.factory.CylinderRegionFactory;
import com.sk89q.worldedit.regions.factory.RegionFactory;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.anarres.parallelgzip.ParallelGZIPOutputStream;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;
import org.jetbrains.annotations.Range;

import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.worldedit.internal.command.CommandUtil.checkCommandArgument;

/**
 * Commands to set brush shape.
 */
@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class BrushCommands {

    private final WorldEdit worldEdit;

    private static final Component UNBIND_COMMAND_COMPONENT = TextComponent.builder("/brush unbind")
            .clickEvent(ClickEvent.suggestCommand("/brush unbind"))
            .build();

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public BrushCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    //FAWE start
    @Command(name = "blendball",
            aliases = {
                    "bb",
                    "blend"
            },
            desc = "Smooths and blends terrain",
            descFooter = "Smooths and blends terrain\n"
                    + "Pic: https://i.imgur.com/cNUQUkj.png → https://i.imgur.com/hFOFsNf.png"
    )
    @CommandPermissions("worldedit.brush.blendball")
    public void blendBallBrush(
            InjectedValueAccess context,
            @Arg(desc = "The radius to sample for blending", def = "5")
                    Expression radius,
            @Arg(desc = "Minimum difference in frequency to change block", def = "1")
                    int minFreqDiff,
            @Switch(name = 'a', desc = "Compare only air vs existing blocks")
                    boolean onlyAir,
            @ArgFlag(name = 'm', desc = "Mask to limit blocks being considered", def = "")
                    Mask mask
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        checkCommandArgument(minFreqDiff >= 0 && minFreqDiff <= 26, "minFreqDiff not in range 0 <= value <= 26");
        if (mask != null && !(mask instanceof CachedMask)) {
            mask = new CachedMask(mask, false);
        }
        set(context, new BlendBall(minFreqDiff, onlyAir, (CachedMask) mask), "worldedit.brush.blendball").setSize(radius);
    }

    @Command(
            name = "erode",
            desc = "Erodes terrain"
    )
    @CommandPermissions("worldedit.brush.erode")
    public void erodeBrush(
            InjectedValueAccess context,
            @Arg(desc = "The radius for eroding", def = "5")
                    Expression radius,
            @Arg(desc = "erodeFaces", def = "2")
                    int erodefaces,
            @Arg(desc = "erodeRec", def = "1")
                    int erodeRec,
            @Arg(desc = "fillFaces", def = "5")
                    int fillFaces,
            @Arg(desc = "fillRec", def = "1")
                    int fillRec
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        set(context, new ErodeBrush(erodefaces, erodeRec, fillFaces, fillRec), "worldedit.brush.erode").setSize(radius);
    }

    @Command(
            name = "pull",
            desc = "Pull terrain towards you"
    )
    @CommandPermissions("worldedit.brush.pull")
    public void pullBrush(
            InjectedValueAccess context,
            @Arg(desc = "The radius to sample for blending", def = "5")
                    Expression radius,
            @Arg(desc = "erodeFaces", def = "6")
                    int erodefaces,
            @Arg(desc = "erodeRec", def = "0")
                    int erodeRec,
            @Arg(desc = "fillFaces", def = "1")
                    int fillFaces,
            @Arg(desc = "fillRec", def = "1")
                    int fillRec
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        set(context, new RaiseBrush(erodefaces, erodeRec, fillFaces, fillRec), "worldedit.brush.pull").setSize(radius);
    }

    @Command(
            name = "circle",
            desc = "Creates a circle, which revolves around your facing direction"
    )
    @CommandPermissions("worldedit.brush.sphere")
    public void circleBrush(
            InjectedValueAccess context,
            @Arg(desc = "Pattern")
                    Pattern fill,
            @Arg(desc = "The radius to sample for blending", def = "5")
                    Expression radius,
            @Arg(name = "filled", desc = "Whether the circle should be filled", def = "false")
                    boolean filled
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        set(context, new CircleBrush(filled), "worldedit.brush.sphere").setSize(radius).setFill(fill);
    }

    @Command(
            name = "recursive",
            aliases = {
                    "recurse",
                    "r"
            },
            desc = "Set all connected blocks",
            descFooter = "Set all connected blocks\n"
                    + "Note: Set a mask to recurse along specific blocks"
    )
    @CommandPermissions("worldedit.brush.recursive")
    public void recursiveBrush(
            InjectedValueAccess context, EditSession editSession,
            @Arg(desc = "Pattern")
                    Pattern fill,
            @Arg(desc = "The radius to sample for blending", def = "5")
                    Expression radius,
            @Switch(name = 'd', desc = "Apply in depth first order")
                    boolean depthFirst
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        set(context, new RecurseBrush(depthFirst), "worldedit.brush.recursive").setSize(radius).setFill(fill)
                .setMask(new IdMask(editSession));
    }

    @Command(
            name = "line",
            aliases = {"l"},
            desc = "Create lines"
    )
    @CommandPermissions("worldedit.brush.line")
    public void lineBrush(
            InjectedValueAccess context,
            @Arg(desc = "Pattern")
                    Pattern fill,
            @Arg(desc = "The radius to sample for blending", def = "0")
                    Expression radius,
            @Switch(name = 'h', desc = "Create only a shell")
                    boolean shell,
            @Switch(name = 's', desc = "Selects the clicked point after drawing") boolean select,
            @Switch(name = 'f', desc = "Create a flat line")
                    boolean flat
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        set(context, new LineBrush(shell, select, flat), "worldedit.brush.line").setSize(radius).setFill(fill);
    }

    @Command(
            name = "spline",
            aliases = {
                    "spl",
                    "curve"
            },
            desc = "Join multiple objects together in a curve",
            descFooter =
                    """
                            Click to select some objects,click the same block twice to connect the objects.
                            Insufficient brush radius, or clicking the the wrong spot will result in undesired shapes. The shapes must be simple lines or loops.
                            Pic1: http://i.imgur.com/CeRYAoV.jpg -> http://i.imgur.com/jtM0jA4.png
                            Pic2: http://i.imgur.com/bUeyc72.png -> http://i.imgur.com/tg6MkcF.pngTutorial: https://www.planetminecraft.com/blog/fawe-tutorial/"""
    )
    @CommandPermissions("worldedit.brush.spline")
    public void splineBrush(
            Player player, InjectedValueAccess context,
            @Arg(desc = "Pattern")
                    Pattern fill,
            @Arg(desc = "The radius to sample for blending", def = "25")
                    Expression radius
    )
            throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        player.print(Caption.of("fawe.worldedit.brush.brush.spline", (radius)));
        set(context, new SplineBrush(player), "worldedit.brush.spline").setSize(radius).setFill(fill);
    }

    @Command(
            name = "sweep",
            aliases = {
                    "sw",
                    "vaesweep"
            },
            desc = "Sweep your clipboard content along a curve",
            descFooter = """
                    Sweeps your clipboard content along a curve.
                    Define a curve by selecting the individual points with a brush
                    Set [copies] to a value > 0 if you want to have your selection pasted a limited amount of times equally spaced on the curve"""
    )
    @CommandPermissions("worldedit.brush.sweep")
    public void sweepBrush(
            Player player, InjectedValueAccess context,
            @Arg(desc = "int", def = "-1")
                    int copies
    ) throws WorldEditException {
        player.print(Caption.of("fawe.worldedit.brush.brush.spline"));
        set(context, new SweepBrush(copies), "worldedit.brush.sweep");
    }

    @Command(
            name = "catenary",
            aliases = {
                    "cat",
                    "gravityline",
                    "saggedline"
            },
            desc = "Create a hanging line between two points"
    )
    @CommandPermissions("worldedit.brush.spline")
    public void catenaryBrush(
            InjectedValueAccess context,
            @Arg(desc = "Pattern")
                    Pattern fill,
            @Arg(def = "1.2", desc = "Length of wire compared to distance between points")
            @Range(from = 1, to = Integer.MAX_VALUE)
                    double lengthFactor,
            @Arg(desc = "The radius to sample for blending", def = "0")
                    Expression radius,
            @Switch(name = 'h', desc = "Create only a shell")
                    boolean shell,
            @Switch(name = 's', desc = "Select the clicked point after drawing")
                    boolean select,
            @Switch(name = 'd', desc = "sags the catenary toward the facing direction")
                    boolean facingDirection
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        set(context, new CatenaryBrush(shell, select, facingDirection, lengthFactor), "worldedit.brush.spline")
                .setSize(radius).setFill(fill);
    }

    @Command(
            name = "surfacespline",
            aliases = {
                    "sspline",
                    "sspl"
            },
            desc = "Draws a spline (curved line) on the surface",
            descFooter = "Create a spline on the surface\n"
                    + "Video: https://www.youtube.com/watch?v=CukvVOi4jv8"
    )
    @CommandPermissions("worldedit.brush.surfacespline") // 0, 0, 0, 10, 0,
    public void surfaceSpline(
            Player player, InjectedValueAccess context,
            @Arg(desc = "Pattern")
                    Pattern fill,
            @Arg(desc = "The radius to sample for blending", def = "0")
                    Expression radius,
            @Arg(desc = "double", def = "0")
                    double tension,
            @Arg(desc = "double", def = "0")
                    double bias,
            @Arg(desc = "double", def = "0")
                    double continuity,
            @Arg(desc = "double", def = "10")
                    double quality
    ) throws WorldEditException {
        player.print(Caption.of("fawe.worldedit.brush.brush.spline", (radius)));
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        set(context, new SurfaceSpline(tension, bias, continuity, quality), "surfacespline").setSize(radius)
                .setFill(fill);
    }

    @Command(
            name = "rock",
            aliases = {"blob"},
            desc = "Creates a distorted sphere"
    )
    @CommandPermissions("worldedit.brush.rock")
    public void blobBrush(
            InjectedValueAccess context,
            @Arg(desc = "Pattern")
                    Pattern fill,
            @Arg(desc = "radii to multiply x,y,z by", def = "10")
                    Vector3 radius,
            @Arg(name = "roundness", desc = "roundness", def = "100")
                    double sphericity,
            @Arg(desc = "double", def = "30")
                    double frequency,
            @Arg(desc = "double", def = "50")
                    double amplitude
    ) throws WorldEditException {
        double max = MathMan.max(radius.x(), radius.y(), radius.z());
        worldEdit.checkMaxBrushRadius(
                max,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        Brush brush = new BlobBrush(radius.divide(max), frequency / 100, amplitude / 100, sphericity / 100);
        set(context, brush, "worldedit.brush.rock").setSize(max).setFill(fill);
    }

    @Command(
            name = "shatter",
            aliases = {"partition", "split"},
            desc = "Creates random lines to break the terrain into pieces",
            descFooter = "Creates uneven lines separating terrain into multiple pieces\n"
                    + "Pic: https://i.imgur.com/2xKsZf2.png"
    )
    @CommandPermissions("worldedit.brush.shatter")
    public void shatterBrush(
            EditSession editSession, InjectedValueAccess context,
            @Arg(desc = "Pattern")
                    Pattern fill,
            @Arg(desc = "The radius to sample for blending", def = "10")
                    Expression radius,
            @Arg(desc = "Lines", def = "10")
                    int count
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        set(context, new ShatterBrush(count), "worldedit.brush.shatter").setSize(radius).setFill(fill)
                .setMask(new ExistingBlockMask(editSession));
    }

    @Command(
            name = "stencil",
            desc = "Use a height map to paint a surface",
            descFooter = "Use a height map to paint any surface."
    )
    @CommandPermissions("worldedit.brush.stencil")
    public void stencilBrush(
            Player player,
            LocalSession session, InjectedValueAccess context,
            @Arg(desc = "Pattern")
                    Pattern fill,
            @Arg(desc = "Expression", def = "5")
                    Expression radius,
            @Arg(desc = "String", def = "")
                    String image,
            @Arg(def = "0", desc = "rotation")
            @Range(from = 0, to = 360)
                    int rotation,
            @Arg(desc = "double", def = "1")
                    double yscale,
            @Switch(name = 'w', desc = "Apply at maximum saturation")
                    boolean onlyWhite,
            @Switch(name = 'r', desc = "Apply random rotation")
                    boolean randomRotate
    )
            throws WorldEditException, FileNotFoundException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        InputStream stream = getHeightmapStream(image);
        HeightBrush brush;
        int minY = player.getWorld().getMinY();
        int maxY = player.getWorld().getMaxY();
        try {
            brush = new StencilBrush(stream, rotation, yscale, onlyWhite,
                    isClipboard(image)
                            ? session.getClipboard().getClipboard() : null, minY, maxY
            );
        } catch (EmptyClipboardException ignored) {
            brush = new StencilBrush(stream, rotation, yscale, onlyWhite, null, minY, maxY);
        }
        if (randomRotate) {
            brush.setRandomRotate(true);
        }
        set(context, brush, "worldedit.brush.stencil").setSize(radius).setFill(fill);
    }

    @Command(name = "image",
            desc = "Use a height map to paint a surface",
            descFooter = "Use a height map to paint any surface.\n")
    @CommandPermissions("worldedit.brush.image")
    public void imageBrush(
            LocalSession session,
            InjectedValueAccess context,
            @Arg(desc = "Image URL (imgur only)") String imageURL,
            @Arg(desc = "The size of the brush", def = "5") Expression radius,
            @Arg(def = "1", desc = "scale height") double yscale,
            @Switch(name = 'a', desc = "Use image Alpha") boolean alpha,
            @Switch(name = 'f', desc = "Blend the image with existing terrain") boolean fadeOut
    )
            throws WorldEditException, IOException, URISyntaxException {
        URL url = new URL(imageURL);
        MainUtil.checkImageHost(url.toURI());
        BufferedImage image = MainUtil.readImage(url);
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        if (yscale != 1) {
            ImageUtil.scaleAlpha(image, yscale);
            alpha = true;
        }
        if (fadeOut) {
            ImageUtil.fadeAlpha(image);
            alpha = true;
        }
        ImageBrush brush = new ImageBrush(image, session, alpha);
        set(context, brush, "worldedit.brush.image").setSize(radius);
    }

    @Command(
            name = "surface",
            aliases = {"surf"},
            desc = "Use a height map to paint a surface",
            descFooter = "Use a height map to paint any surface."
    )
    @CommandPermissions("worldedit.brush.surface")
    public void surfaceBrush(
            InjectedValueAccess context,
            @Arg(desc = "Pattern")
                    Pattern fill,
            @Arg(desc = "Expression", def = "5")
                    Expression radius
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        set(context, new SurfaceSphereBrush(), "worldedit.brush.surface").setFill(fill).setSize(radius);
    }

    @Command(
            name = "scatter",
            desc = "Scatter a pattern on a surface",
            descFooter =
                    "Set a number of blocks randomly on a surface each a certain distance apart.\n"
                            + "Video: https://youtu.be/RPZIaTbqoZw?t=34s"
    )
    @CommandPermissions("worldedit.brush.scatter")
    public void scatterBrush(
            InjectedValueAccess context,
            @Arg(desc = "Pattern")
                    Pattern fill,
            @Arg(desc = "radius", def = "5")
                    Expression radius,
            @Arg(desc = "points", def = "5")
                    double points,
            @Arg(desc = "distance", def = "1")
                    double distance,
            @Switch(name = 'o', desc = "Overlay the block")
                    boolean overlay
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        Brush brush;
        if (overlay) {
            brush = new ScatterOverlayBrush((int) points, (int) distance);
        } else {
            brush = new ScatterBrush((int) points, (int) distance);
        }
        set(context, brush, "worldedit.brush.scatter").setSize(radius).setFill(fill);
    }

    @Command(
            name = "populateschematic",
            aliases = {
                    "populateschem",
                    "popschem",
                    "pschem",
                    "ps"
            },
            desc = "Scatter a schematic on a surface"
    )
    @CommandPermissions("worldedit.brush.populateschematic")
    public void scatterSchemBrush(
            Player player, InjectedValueAccess context,
            @Arg(name = "clipboard", desc = "Clipboard uri")
                    String clipboardStr,
            @Arg(desc = "Mask of block to place on. Defaults to solid blocks.", def = "")
                    Mask mask,
            @Arg(desc = "Expression", def = "30")
                    Expression radius,
            @Arg(desc = "double", def = "50")
                    double density,
            @Switch(name = 'r', desc = "Apply random rotation")
                    boolean rotate
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        try {
            MultiClipboardHolder clipboards =
                    ClipboardFormats.loadAllFromInput(player, clipboardStr, null, true);
            if (clipboards == null) {
                player.print(Caption.of("fawe.error.schematic.not.found", clipboardStr));
                return;
            }
            List<ClipboardHolder> holders = clipboards.getHolders();
            if (holders == null) {
                player.print(Caption.of("fawe.error.schematic.not.found", clipboardStr));
                return;
            }

            set(context, new PopulateSchem(mask, holders, (int) density, rotate), "worldedit.brush.populateschematic").setSize(
                    radius);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Command(
            name = "layer",
            desc = "Replaces terrain with a layer.",
            descFooter = """
                    Replaces terrain with a layer.
                    Example: /br layer 5 oak_planks,orange_stained_glass,magenta_stained_glass,black_wool - Places several layers on a surface
                    Pic: https://i.imgur.com/XV0vYoX.png"""
    )
    @CommandPermissions("worldedit.brush.layer")
    public void surfaceLayer(
            InjectedValueAccess context,
            @Arg(desc = "Expression")
                    Expression radius,
            @Arg(desc = "List of comma-separated patterns")
            @PatternList()
                    List<Pattern> patternLayers
    )
            throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        set(context, new LayerBrush(patternLayers.toArray(new Pattern[0])), "worldedit.brush.layer").setSize(radius);
    }

    @Command(
            name = "splatter",
            desc = "Splatter a pattern on a surface",
            descFooter = """
                    Sets a bunch of blocks randomly on a surface.
                    Pic: https://i.imgur.com/hMD29oO.png
                    Example: /br splatter stone,dirt 30 15
                    Note: The seeds define how many splotches there are, recursion defines how large, solid defines whether the pattern is applied per seed, else per block."""
    )
    @CommandPermissions("worldedit.brush.splatter")
    public void splatterBrush(
            InjectedValueAccess context,
            @Arg(desc = "Pattern")
                    Pattern fill,
            @Arg(desc = "Expression", def = "5")
                    Expression radius,
            @Arg(desc = "double", def = "1")
                    double points,
            @Arg(desc = "double", def = "5")
                    double recursion,
            @Arg(desc = "boolean", def = "true")
                    boolean solid
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        set(context, new SplatterBrush((int) points, (int) recursion, solid), "worldedit.brush.splatter").setSize(radius)
                .setFill(fill);
    }

    @Command(
            name = "scattercommand",
            aliases = {
                    "scattercmd",
                    "scmd",
                    "scommand"
            },
            desc = "Run commands at random points on a surface",
            descFooter = """
                    Run commands at random points on a surface
                     - Your selection will be expanded to the specified size around each point
                     - Placeholders: {x}, {y}, {z}, {world}, {size}"""
    )
    @CommandPermissions("worldedit.brush.scattercommand")
    public void scatterCommandBrush(
            Player player, InjectedValueAccess context,
            @Arg(desc = "The minimum distance between each point")
                    Expression radius,
            @Arg(desc = "double", def = "1")
                    double points,
            @Arg(desc = "double", def = "1")
                    double distance,
            @Arg(desc = "List of comma-separated commands")
                    List<String> commandStr,
            @Switch(name = 'p', desc = "Show any printed output")
                    boolean print
    )
            throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        set(
                context,
                new ScatterCommand((int) points, (int) distance, StringMan.join(commandStr, " "), print),
                "worldedit.brush.scattercommand"
        )
                .setSize(radius);
    }

    @Command(
            name = "height",
            aliases = {"heightmap"},
            desc = "Raise or lower terrain using a heightmap",
            descFooter = """
                    This brush raises and lowers land.
                    Note: Use a negative yscale to reduce height
                    Snow Pic: https://i.imgur.com/Hrzn0I4.png"""
    )
    @CommandPermissions("worldedit.brush.height")
    public void heightBrush(
            Player player,
            LocalSession session,
            @Arg(desc = "Expression", def = "5")
                    Expression radius,
            @Arg(desc = "double", def = "1")
                    double yscale,
            @Arg(desc = "String", def = "")
                    String image,
            @Arg(def = "0", desc = "rotation")
            @Range(from = 0, to = 360)
                    int rotation,
            @Switch(name = 'r', desc = "Random off-axis rotation")
                    boolean randomRotate,
            @Switch(name = 'l', desc = "Work on snow layers")
                    boolean layers,
            @Switch(name = 's', desc = "Disable smoothing")
                    boolean dontSmooth, InjectedValueAccess context
    )
            throws WorldEditException, FileNotFoundException {
        terrainBrush(player, session, radius, image, rotation, yscale, false, randomRotate, layers,
                !dontSmooth, ScalableHeightMap.Shape.CONE, context
        );
    }

    @Command(
            name = "cliff",
            aliases = {"flatcylinder"},
            desc = "Cliff brush",
            descFooter = "This brush flattens terrain and creates cliffs."
    )
    @CommandPermissions("worldedit.brush.height")
    public void cliffBrush(
            Player player,
            LocalSession session,
            @Arg(desc = "Expression", def = "5")
                    Expression radius,
            @Arg(desc = "String", def = "")
                    String image,
            @Arg(def = "0", desc = "rotation")
            @Step(90)
            @Range(from = 0, to = 360)
                    int rotation,
            @Arg(desc = "double", def = "1")
                    double yscale,
            @Switch(name = 'r', desc = "Enables random off-axis rotation")
                    boolean randomRotate,
            @Switch(name = 'l', desc = "Will work on snow layers")
                    boolean layers,
            @Switch(name = 's', desc = "Disables smoothing")
                    boolean dontSmooth, InjectedValueAccess context
    )
            throws WorldEditException, FileNotFoundException {
        terrainBrush(player, session, radius, image, rotation, yscale, true, randomRotate, layers,
                !dontSmooth, ScalableHeightMap.Shape.CYLINDER, context
        );
    }

    @Command(
            name = "flatten",
            aliases = {
                    "flatmap",
                    "flat"
            },
            desc = "This brush raises or lowers land towards the clicked point"
    )
    @CommandPermissions("worldedit.brush.height")
    public void flattenBrush(
            Player player,
            LocalSession session,
            @Arg(desc = "Expression", def = "5")
                    Expression radius,
            @Arg(desc = "String", def = "")
                    String image,
            @Arg(def = "0", desc = "rotation")
            @Step(90)
            @Range(from = 0, to = 360)
                    int rotation,
            @Arg(desc = "double", def = "1")
                    double yscale,
            @Switch(name = 'r', desc = "Enables random off-axis rotation")
                    boolean randomRotate,
            @Switch(name = 'l', desc = "Will work on snow layers")
                    boolean layers,
            @Switch(name = 's', desc = "Disables smoothing")
                    boolean dontSmooth, InjectedValueAccess context
    )
            throws WorldEditException, FileNotFoundException {
        terrainBrush(player, session, radius, image, rotation, yscale, true, randomRotate, layers,
                !dontSmooth, ScalableHeightMap.Shape.CONE, context
        );
    }

    private void terrainBrush(
            Player player,
            LocalSession session,
            Expression radius,
            String image,
            int rotation,
            double yscale,
            boolean flat,
            boolean randomRotate,
            boolean layers,
            boolean smooth,
            Shape shape,
            InjectedValueAccess context
    )
            throws WorldEditException, FileNotFoundException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        InputStream stream = getHeightmapStream(image);
        HeightBrush brush;
        int minY = player.getWorld().getMinY();
        int maxY = player.getWorld().getMaxY();
        if (flat) {
            brush = new FlattenBrush(stream, rotation, yscale, layers, smooth,
                    isClipboard(image)
                            ? session.getClipboard().getClipboard() : null, shape, minY, maxY
            );
        } else {
            brush = new HeightBrush(stream, rotation, yscale, layers, smooth,
                    isClipboard(image)
                            ? session.getClipboard().getClipboard() : null, minY, maxY
            );
        }
        if (randomRotate) {
            brush.setRandomRotate(true);
        }
        set(context, brush, "worldedit.brush.height").setSize(radius);
    }

    private static boolean isClipboard(String image) {
        return "#clipboard".equalsIgnoreCase(image);
    }

    private InputStream getHeightmapStream(String filename) throws FileNotFoundException {
        if (filename == null || "none".equalsIgnoreCase(filename) || isClipboard(filename)) {
            return null;
        }
        String filenamePng = filename.endsWith(".png") ? filename : filename + ".png";
        File file = new File(
                Fawe.platform().getDirectory(),
                Settings.settings().PATHS.HEIGHTMAP + File.separator + filenamePng
        );
        if (file.exists()) {
            return new FileInputStream(file);
        }
        URI uri = ImageUtil.getImageURI(filename);
        return ImageUtil.getInputStream(uri);
    }


    @Command(
            name = "copypaste",
            aliases = {
                    "cp",
                    "copypasta"
            },
            desc = "Copy Paste brush",
            descFooter = """
                    Left click the base of an object to copy.
                    Right click to paste
                    Note: Works well with the clipboard scroll action
                    Video: https://www.youtube.com/watch?v=RPZIaTbqoZw"""
    )
    @CommandPermissions("worldedit.brush.copy")
    public void copy(
            Player player, LocalSession session, InjectedValueAccess context,
            @Arg(desc = "Expression", def = "5")
                    Expression radius,
            @Switch(name = 'r', desc = "Apply random rotation on paste")
                    boolean randomRotate,
            @Switch(name = 'a', desc = "Apply auto view based rotation on paste")
                    boolean autoRotate
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        player.print(Caption.of("fawe.worldedit.brush.brush.copy", (radius)));

        set(context, new CopyPastaBrush(player, session, randomRotate, autoRotate), "worldedit.brush.copy").setSize(radius);
    }

    @Command(
            name = "command",
            aliases = {"cmd"},
            desc = "Command brush",
            descFooter = """
                    Run the commands at the clicked position.
                     - Your selection will be expanded to the specified size around each point
                     - Placeholders: {x}, {y}, {z}, {world}, {size}"""
    )
    @CommandPermissions("worldedit.brush.command")
    public void command(
            InjectedValueAccess context,
            @Arg(desc = "Expression")
                    Expression radius,
            @Arg(desc = "Command to run")
                    List<String> input,
            @Switch(name = 'h', desc = "Hide any printed output")
                    boolean hide
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        String cmd = StringMan.join(input, " ");
        set(context, new CommandBrush(cmd, !hide), "worldedit.brush.command").setSize(radius);
    }

    @Command(
            name = "savebrush",
            aliases = {"save"},
            desc = "Save your current brush"
    )
    @CommandPermissions("worldedit.brush.save")
    public void saveBrush(
            Player player, LocalSession session,
            @Arg(desc = "String name")
                    String name,
            @Switch(name = 'g', desc = "Save the brush globally")
                    boolean root
    ) throws WorldEditException, IOException {
        BrushTool tool = session.getBrushTool(player);
        if (tool != null) {
            root |= name.startsWith("../");
            name = FileSystems.getDefault().getPath(name).getFileName().toString();
            File folder = MainUtil.getFile(Fawe.platform().getDirectory(), "brushes");
            name = name.endsWith(".jsgz") ? name : name + ".jsgz";
            File file;
            if (root && player.hasPermission("worldedit.brush.save.other")) {
                file = new File(folder, name);
            } else {
                file = new File(folder, player.getUniqueId() + File.separator + name);
            }
            File parent = file.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            file.createNewFile();
            try (DataOutputStream out = new DataOutputStream(
                    new ParallelGZIPOutputStream(new FileOutputStream(file)))) {
                out.writeUTF(tool.toString());
            } catch (Throwable e) {
                e.printStackTrace();
            }
            player.print(Caption.of("fawe.worldedit.schematic.schematic.saved", name));
        } else {
            player.print(Caption.of("fawe.worldedit.brush.brush.none"));
        }
    }

    // TODO: Write a Brush standard format.
    /*    @Command(
                name = "loadbrush",
                aliases = {"load"},
                desc = "Load a brush"
        )
        @CommandPermissions("worldedit.brush.load")
        public void loadBrush(Player player, LocalSession session, @Arg(desc = "String name") String name)
                throws WorldEditException, IOException {
            name = FileSystems.getDefault().getPath(name).getFileName().toString();
            File folder = MainUtil.getFile(Fawe.platform().getDirectory(), "brushes");
            name = name.endsWith(".jsgz") ? name : name + ".jsgz";
            File file = new File(folder, player.getUniqueId() + File.separator + name);
            if (!file.exists()) {
                file = new File(folder, name);
            }
            if (!file.exists()) {
                File[] files = folder.listFiles(pathname -> false);
                player.print(Caption.of("fawe.error.brush.not.found", name));
                return;
            }
            try (DataInputStream in = new DataInputStream(
                    new GZIPInputStream(new FileInputStream(file)))) {
                String json = in.readUTF();
                BrushTool tool = BrushTool.fromString(player, session, json);
                BaseItem item = player.getItemInHand(HandSide.MAIN_HAND);
                session.setTool(item, tool, player);
                player.print(Caption.of("fawe.worldedit.brush.brush.equipped", name));
            } catch (Throwable e) {
                e.printStackTrace();
                player.print(Caption.of("fawe.error.brush.incompatible"));
            }
        } */

    @Command(
            name = "/listbrush",
            desc = "List saved brushes",
            descFooter = "List all brushes in the brush directory")
    @CommandPermissions("worldedit.brush.list")
    public void list(
            Actor actor, InjectedValueAccess args,
            @ArgFlag(name = 'p', desc = "Prints the requested page", def = "0")
                    int page
    ) throws WorldEditException {
        String baseCmd = "/brush loadbrush";
        File dir = MainUtil.getFile(Fawe.platform().getDirectory(), "brushes");
        // TODO NOT IMPLEMENTED
        //        UtilityCommands.list(dir, actor, args, page, null, true, baseCmd);
    }
    //FAWE end

    static void setOperationBasedBrush(
            Player player,
            LocalSession session,
            //FAWE start - Expression > double
            Expression radius,
            //FAWE end
            Contextual<? extends Operation> factory,
            RegionFactory shape,
            String permission
    )
            throws WorldEditException {
        WorldEdit.getInstance().checkMaxBrushRadius(radius, player);
        BrushTool tool = session.getBrushTool(player);
        tool.setSize(radius);
        tool.setFill(null);
        tool.setBrush(new OperationFactoryBrush(factory, shape, session), permission);

        player.print(Caption.of("worldedit.brush.operation.equip", TextComponent.of(factory.toString())));
        ToolCommands.sendUnbindInstruction(player, UNBIND_COMMAND_COMPONENT);
    }

    @Command(
            name = "deform",
            desc = "Deform brush, applies an expression to an area"
    )
    @CommandPermissions("worldedit.brush.deform")
    public void deform(
            Player player, LocalSession localSession,
            @Arg(desc = "The shape of the region")
                    RegionFactory shape,
            @Arg(desc = "The size of the brush", def = "5")
                    double radius,
            @Arg(desc = "Expression to apply", def = "y-=0.2")
                    String expression,
            @Switch(name = 'r', desc = "Use the game's coordinate origin")
                    boolean useRawCoords,
            @Switch(name = 'o', desc = "Use the placement position as the origin")
                    boolean usePlacement
    ) throws WorldEditException {
        Deform deform = new Deform(expression);
        if (useRawCoords) {
            deform.setMode(Deform.Mode.RAW_COORD);
        } else if (usePlacement) {
            deform.setMode(Deform.Mode.OFFSET);
            deform.setOffset(localSession.getPlacementPosition(player).toVector3());
        }
        setOperationBasedBrush(player, localSession, radius,
                deform, shape, "worldedit.brush.deform"
        );
    }

    @Command(
            name = "set",
            desc = "Set brush, sets all blocks in the area"
    )
    @CommandPermissions("worldedit.brush.set")
    public void set(
            Player player, LocalSession localSession,
            @Arg(desc = "The shape of the region")
                    RegionFactory shape,
            @Arg(desc = "The size of the brush", def = "5")
                    Expression radius,
            @Arg(desc = "The pattern of blocks to set")
                    Pattern pattern
    ) throws WorldEditException {
        setOperationBasedBrush(player, localSession, radius,
                new Apply(new ReplaceFactory(pattern)), shape, "worldedit.brush.set"
        );
    }

    @Command(
            name = "forest",
            desc = "Forest brush, creates a forest in the area"
    )
    @CommandPermissions("worldedit.brush.forest")
    public void forest(
            Player player, LocalSession localSession,
            @Arg(desc = "The shape of the region")
                    RegionFactory shape,
            @Arg(desc = "The size of the brush", def = "5")
                    Expression radius,
            @Arg(desc = "The density of the brush", def = "20")
                    double density,
            @Arg(desc = "The type of tree to use")
                    TreeGenerator.TreeType type
    ) throws WorldEditException {
        setOperationBasedBrush(player, localSession, radius,
                new Paint(new TreeGeneratorFactory(type), density / 100), shape, "worldedit.brush.forest"
        );
    }

    @Command(
            name = "raise",
            desc = "Raise brush, raise all blocks by one"
    )
    @CommandPermissions("worldedit.brush.raise")
    public void raise(
            Player player, LocalSession localSession,
            @Arg(desc = "The shape of the region")
                    RegionFactory shape,
            @Arg(desc = "The size of the brush", def = "5")
                    Expression radius
    ) throws WorldEditException {
        setOperationBasedBrush(player, localSession, radius,
                new Deform("y-=1", Deform.Mode.RAW_COORD), shape, "worldedit.brush.raise"
        );
    }

    @Command(
            name = "lower",
            desc = "Lower brush, lower all blocks by one"
    )
    @CommandPermissions("worldedit.brush.lower")
    public void lower(
            Player player, LocalSession localSession,
            @Arg(desc = "The shape of the region")
                    RegionFactory shape,
            @Arg(desc = "The size of the brush", def = "5")
                    Expression radius
    ) throws WorldEditException {
        setOperationBasedBrush(player, localSession, radius,
                new Deform("y+=1", Deform.Mode.RAW_COORD), shape, "worldedit.brush.lower"
        );
    }

    @Command(
            name = "snow",
            desc = "Snow brush, sets snow in the area"
    )
    @CommandPermissions("worldedit.brush.snow")
    public void snow(
            Player player, LocalSession localSession,
            @Arg(desc = "The shape of the region")
                    RegionFactory shape,
            @Arg(desc = "The size of the brush", def = "5")
                    double radius,
            @Switch(name = 's', desc = "Whether to stack snow")
                    boolean stack
    ) throws WorldEditException {

        if (shape instanceof CylinderRegionFactory) {
            shape = new CylinderRegionFactory(radius);
        }

        setOperationBasedBrush(player, localSession, radius,
                new ApplyLayer(new Snow(stack)), shape, "worldedit.brush.snow"
        );
    }

    @Command(
            name = "sphere",
            aliases = {"s"},
            desc = "Choose the sphere brush"
    )
    @CommandPermissions("worldedit.brush.sphere")
    public void sphereBrush(
            Player player, InjectedValueAccess context,
            @Arg(desc = "The pattern of blocks to set")
                    Pattern pattern,
            @Arg(desc = "The radius of the sphere", def = "2")
                    Expression radius,
            @Switch(name = 'h', desc = "Create hollow spheres instead")
                    boolean hollow,
            @Switch(name = 'f', desc = "Create falling spheres instead")
                    boolean falling
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        Brush brush;
        if (hollow) {
            brush = new HollowSphereBrush();
        } else {
            //FAWE start - Suggest different brush material if sand or gravel is used
            if (pattern instanceof BlockStateHolder<?> holder) {
                BlockType type = holder.getBlockType();
                if (type == BlockTypes.SAND || type == BlockTypes.GRAVEL) {
                    player.print(
                            Caption.of("fawe.worldedit.brush.brush.try.other"));
                    falling = true;
                }
            }
            if (falling) {
                brush = new FallingSphere();
            } else {
                brush = new SphereBrush();
            }

        }
        //FAWE end
        set(context, brush, "worldedit.brush.sphere").setSize(radius).setFill(pattern);
    }

    @Command(
            name = "cylinder",
            aliases = {"cyl", "c"},
            desc = "Choose the cylinder brush"
    )
    @CommandPermissions("worldedit.brush.cylinder")
    public void cylinderBrush(
            InjectedValueAccess context,
            @Arg(desc = "The pattern of blocks to set")
                    Pattern pattern,
            @Arg(desc = "The radius of the cylinder", def = "2")
                    Expression radius,
            @Arg(desc = "The height of the cylinder", def = "1")
                    int height,
            //FAWE start - hcyl thickness
            @Arg(desc = "The thickness of the cylinder. Requires -h switch be given. 0 creates a standard hollow cylinder.",
                    def = "0")
                    double thickness,
            //FAWE end
            @Switch(name = 'h', desc = "Create hollow cylinders instead")
                    boolean hollow
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        worldEdit.checkMaxBrushRadius(
                height,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );

        BrushSettings settings;
        if (hollow) {
            //FAWE start - hcyl thickness
            settings = set(context, new HollowCylinderBrush(height, thickness), "worldedit.brush.cylinder");
            //FAWE end
        } else {
            settings = set(context, new CylinderBrush(height), "worldedit.brush.cylinder");
        }
        settings.setSize(radius).setFill(pattern);
    }

    @Command(
            name = "clipboard",
            desc = "Paste your clipboard at the brush location. Includes any transforms.",
            descFooter = "Choose the clipboard brush.\n"
                    + "Without the -o flag, the paste will appear centered at the target location. "
                    + "With the flag, then the paste will appear relative to where you had "
                    + "stood relative to the copied area when you copied it."
    )
    @CommandPermissions("worldedit.brush.clipboard")
    public void clipboardBrush(
            Player player, LocalSession session,
            @Switch(name = 'a', desc = "Don't paste air from the clipboard")
                    boolean ignoreAir,
            @Switch(name = 'o', desc = "Paste starting at the target location, instead of centering on it")
                    boolean usingOrigin,
            @Switch(name = 'e', desc = "Paste entities if available")
                    boolean pasteEntities,
            @Switch(name = 'b', desc = "Paste biomes if available")
                    boolean pasteBiomes,
            @ArgFlag(name = 'm', desc = "Skip blocks matching this mask in the clipboard")
            @ClipboardMask
                    Mask sourceMask, InjectedValueAccess context,
            //FAWE start - random rotation
            @Switch(name = 'r', desc = "Apply random rotation on paste, combines with existing clipboard transforms")
                    boolean randomRotate
            //FAWE end
    ) throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();

        Clipboard clipboard = holder.getClipboard();
        ClipboardHolder newHolder = new ClipboardHolder(clipboard);
        newHolder.setTransform(holder.getTransform());

        BlockVector3 size = clipboard.getDimensions();

        worldEdit.checkMaxBrushRadius(
                size.x() / 2D - 1,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        worldEdit.checkMaxBrushRadius(
                size.y() / 2D - 1,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );
        worldEdit.checkMaxBrushRadius(
                size.z() / 2D - 1,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );

        set(
                context,
                //FAWE start - random rotation
                new ClipboardBrush(newHolder, ignoreAir, usingOrigin, pasteEntities, pasteBiomes, sourceMask, randomRotate),
                //FAWE end
                "worldedit.brush.clipboard"
        );
    }

    @Command(
            name = "smooth",
            desc = "Choose the terrain softener brush",
            descFooter = "Example: '/brush smooth 2 4 grass_block,dirt,stone'"
    )
    @CommandPermissions("worldedit.brush.smooth")
    public void smoothBrush(
            Player player, LocalSession session,
            //FAWE start - Expression > double
            @Arg(desc = "The radius to sample for softening", def = "2")
                    Expression radius,
            //FAWE end
            @Arg(desc = "The number of iterations to perform", def = "4")
                    int iterations,
            @Arg(desc = "The mask of blocks to use for the heightmap", def = "")
                    Mask mask,
            InjectedValueAccess context
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );

        //FAWE start
        FaweLimit limit = Settings.settings().getLimit(player);
        iterations = Math.min(limit.MAX_ITERATIONS.get(), iterations);
        //FAWE end

        set(context, new SmoothBrush(iterations, mask), "worldedit.brush.smooth").setSize(radius);
        player.print(Caption.of(
                "worldedit.brush.smooth.equip",
                radius,
                iterations,
                Caption.of("worldedit.brush.smooth." + (mask == null ? "no" : "") + "filter")
        ));
    }

    @Command(
            name = "snowsmooth",
            desc = "Choose the snow terrain softener brush",
            descFooter = "Example: '/brush snowsmooth 5 1 -l 3'"
    )
    @CommandPermissions("worldedit.brush.snowsmooth")
    public void snowSmoothBrush(
            Player player, LocalSession session,
            //FAWE start - Expression > double, default iteration number 1 is much better.
            @Arg(desc = "The radius to sample for softening", def = "2")
                    Expression radius,
            @Arg(desc = "The number of iterations to perform", def = "1")
                    int iterations,
            //FAWE end
            @ArgFlag(name = 'l', desc = "The number of snow blocks under snow", def = "1")
                    int snowBlockCount,
            @ArgFlag(name = 'm', desc = "The mask of blocks to use for the heightmap")
                    Mask mask, InjectedValueAccess context
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );

        //FAWE start
        FaweLimit limit = Settings.settings().getLimit(player);
        iterations = Math.min(limit.MAX_ITERATIONS.get(), iterations);
        //FAWE end

        set(context, new SnowSmoothBrush(iterations, snowBlockCount, mask), "worldedit.brush.snowsmooth").setSize(radius);
        player.print(Caption.of(
                "worldedit.brush.smooth.equip",
                radius,
                iterations,
                Caption.of("worldedit.brush.smooth." + (mask == null ? "no" : "") + "filter")
        ));
    }

    @Command(
            name = "extinguish",
            aliases = {"ex"},
            desc = "Shortcut fire extinguisher brush"
    )
    @CommandPermissions("worldedit.brush.ex")
    public void extinguishBrush(
            InjectedValueAccess context, EditSession editSession,
            @Arg(desc = "The radius to extinguish", def = "5")
                    Expression radius
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );

        set(context, new SphereBrush(), "worldedit.brush.ex").setSize(radius).setFill(BlockTypes.AIR.getDefaultState())
                .setMask(new SingleBlockTypeMask(editSession, BlockTypes.FIRE));
    }

    @Command(
            name = "gravity",
            aliases = {"grav"},
            desc = "Gravity brush, simulates the effect of gravity"
    )
    @CommandPermissions("worldedit.brush.gravity")
    public void gravityBrush(
            InjectedValueAccess context,
            @Arg(desc = "The radius to apply gravity in", def = "5")
                    Expression radius,
            @Switch(name = 'h', desc = "Affect blocks starting at max Y, rather than the target location Y + radius")
                    boolean fromMaxY
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );

        set(context, new GravityBrush(fromMaxY), "worldedit.brush.gravity").setSize(radius);
    }

    @Command(
            name = "butcher",
            aliases = {"kill"},
            desc = "Butcher brush, kills mobs within a radius"
    )
    @CommandPermissions("worldedit.brush.butcher")
    public void butcherBrush(
            Player player, LocalSession session,
            @Arg(desc = "Radius to kill mobs in", def = "5")
                    Expression radius,
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
                    boolean killArmorStands,
            @Switch(name = 'w', desc = "Also kill water mobs")
                    boolean killWater, InjectedValueAccess context
    ) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(
                radius,
                context.injectedValue(Key.of(Player.class)).orElseThrow(() -> new IllegalStateException("No player"))
        );

        CreatureButcher flags = new CreatureButcher(player);
        flags.or(
                CreatureButcher.Flags.FRIENDLY,
                killFriendly
        ); // No permission check here. Flags will instead be filtered by the subsequent calls.
        flags.or(CreatureButcher.Flags.PETS, killPets, "worldedit.butcher.pets");
        flags.or(CreatureButcher.Flags.NPCS, killNpcs, "worldedit.butcher.npcs");
        flags.or(CreatureButcher.Flags.GOLEMS, killGolems, "worldedit.butcher.golems");
        flags.or(CreatureButcher.Flags.ANIMALS, killAnimals, "worldedit.butcher.animals");
        flags.or(CreatureButcher.Flags.AMBIENT, killAmbient, "worldedit.butcher.ambient");
        flags.or(CreatureButcher.Flags.TAGGED, killWithName, "worldedit.butcher.tagged");
        flags.or(CreatureButcher.Flags.ARMOR_STAND, killArmorStands, "worldedit.butcher.armorstands");
        flags.or(CreatureButcher.Flags.WATER, killWater, "worldedit.butcher.water");

        set(context, new ButcherBrush(flags), "worldedit.brush.butcher").setSize(radius);
    }

    //FAWE start
    public BrushSettings process(Player player, Arguments arguments, BrushSettings settings)
            throws WorldEditException {
        LocalSession session = worldEdit.getSessionManager().get(player);
        session.setTool(player, null);
        BrushTool tool = session.getBrushTool(player);
        if (tool != null) {
            tool.setPrimary(settings);
            tool.setSecondary(settings);

            player.print(
                    Caption.of("fawe.worldedit.brush.brush.equipped", arguments.get().split(" ")[1]));
            ToolCommands.sendUnbindInstruction(player, UNBIND_COMMAND_COMPONENT);
        }
        return settings;
    }

    public BrushSettings set(InjectedValueAccess context, Brush brush, String permission)
            throws InvalidToolBindException {
        Player player = context.injectedValue(Key.of(Player.class))
                .orElseThrow(() -> new IllegalStateException("No player"));
        LocalSession session = player.getSession();
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
        Arguments arguments = context.injectedValue(Key.of(Arguments.class)).orElse(null);
        if (arguments != null) {
            String args = arguments.get();
            bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
        }
        bs.addPermission(permission);
        bs.setBrush(brush);
        return process(player, arguments, bs);
    }
    //FAWE end

    static void setOperationBasedBrush(
            Player player, LocalSession session, double radius,
            Contextual<? extends Operation> factory,
            RegionFactory shape,
            String permission
    ) throws WorldEditException {
        WorldEdit.getInstance().checkMaxBrushRadius(radius, player);
        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        tool.setSize(radius);
        tool.setFill(null);
        tool.setBrush(new OperationFactoryBrush(factory, shape, session), permission);

        player.print(Caption.of("worldedit.brush.operation.equip", TextComponent.of(factory.toString())));
        ToolCommands.sendUnbindInstruction(player, UNBIND_COMMAND_COMPONENT);
    }

}
