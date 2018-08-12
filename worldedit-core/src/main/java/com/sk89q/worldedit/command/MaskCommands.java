package com.sk89q.worldedit.command;

import com.boydti.fawe.object.mask.*;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.*;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.regions.shape.WorldEditExpressionEnvironment;
import com.sk89q.worldedit.session.request.RequestSelection;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.function.Predicate;

@Command(aliases = {"masks"},
        desc = "Help for the various masks. [More Info](https://git.io/v9r4K)",
        help = "Masks determine if a block can be placed\n" +
                " - Use [brackets] for arguments\n" +
                " - Use , to OR multiple\n" +
                " - Use & to AND multiple\n" +
                "e.g. >[stone,dirt],#light[0][5],$jungle\n" +
                "More Info: https://git.io/v9r4K"
)
public class MaskCommands extends MethodCommands {
    public MaskCommands(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Command(
            aliases = {"#simplex"},
            desc = "Use simplex noise as the mask",
            usage = "<scale=10> <min=0> <max=100>",
            min = 3,
            max = 3
    )
    public Mask simplex(double scale, double min, double max) {
        scale = (1d / Math.max(1, scale));
        min = (min - 50) / 50;
        max = (max - 50) / 50;
        return new SimplexMask(scale, min, max);
    }

    @Command(
            aliases = {"#light"},
            desc = "Restrict to specific light levels",
            usage = "<min> <max>",
            min = 2,
            max = 2
    )
    public Mask light(Extent extent, double min, double max) {
        return new LightMask(extent, (int) min, (int) max);
    }

    @Command(
            aliases = {"false"},
            desc = "Always false"
    )
    public Mask falseMask(Extent extent) {
        return Masks.alwaysFalse();
    }

    @Command(
            aliases = {"true"},
            desc = "Always true"
    )
    public Mask trueMask(Extent extent) {
        return Masks.alwaysTrue();
    }

    @Command(
            aliases = {"#skylight"},
            desc = "Restrict to specific sky light levels",
            usage = "<min> <max>",
            min = 2,
            max = 2
    )
    public Mask skylight(Extent extent, double min, double max) {
        return new SkyLightMask(extent, (int) min, (int) max);
    }

    @Command(
            aliases = {"#blocklight", "#emittedlight"},
            desc = "Restrict to specific block light levels",
            usage = "<min> <max>",
            min = 2,
            max = 2
    )
    public Mask blocklight(Extent extent, double min, double max) {
        return new BlockLightMask(extent, (int) min, (int) max);
    }

    @Command(
            aliases = {"#opacity"},
            desc = "Restrict to specific opacity levels",
            usage = "<min> <max>",
            min = 2,
            max = 2
    )
    public Mask opacity(Extent extent, double min, double max) {
        return new OpacityMask(extent, (int) min, (int) max);
    }

    @Command(
            aliases = {"#brightness"},
            desc = "Restrict to specific block brightness",
            usage = "<min> <max>",
            min = 2,
            max = 2
    )
    public Mask brightness(Extent extent, double min, double max) {
        return new BrightnessMask(extent, (int) min, (int) max);
    }

    @Command(
            aliases = {"#offset"},
            desc = "Offset a mask",
            usage = "<dx> <dy> <dz> <mask>",
            min = 4,
            max = 4
    )
    public Mask offset(double x, double y, double z, Mask mask) {
        return new OffsetMask(mask, new Vector(x, y, z));
    }

    @Command(
            aliases = {"#haslight"},
            desc = "Restricts to blocks with light (sky or emitted)"
    )
    public Mask haslight(Extent extent) {
        return new LightMask(extent, 1, Integer.MAX_VALUE);
    }

    @Command(
            aliases = {"#nolight"},
            desc = "Restrict to blocks without light (sky or emitted)"
    )
    public Mask nolight(Extent extent) {
        return new LightMask(extent, 0, 0);
    }

    @Command(
            aliases = {"#existing"},
            desc = "If there is a non air block"
    )
    public Mask existing(Extent extent) {
        return new ExistingBlockMask(extent);
    }

    @Command(
            aliases = {"#solid"},
            desc = "If there is a solid block"
    )
    public Mask solid(Extent extent) {
        return new SolidBlockMask(extent);
    }

    @Command(
            aliases = {"#liquid"},
            desc = "If there is a solid block"
    )
    public Mask liquid(Extent extent) {
        return new BlockMaskBuilder().addAll(b -> b.getMaterial().isLiquid()).build(extent);
    }

    @Command(
            aliases = {"#dregion", "#dselection", "#dsel"},
            desc = "inside the player's selection"
    )
    public Mask dregion() {
        return new RegionMask(new RequestSelection());
    }

    @Command(
            aliases = {"#region", "#selection", "#sel"},
            desc = "inside the provided selection"
    )
    public Mask selection(Player player, LocalSession session) throws IncompleteRegionException {
        return new RegionMask(session.getSelection(player.getWorld()).clone());
    }

    @Command(
            aliases = {"#xaxis"},
            desc = "Restrict to initial x axis"
    )
    public Mask xaxis() {
        return new XAxisMask();
    }

    @Command(
            aliases = {"#yaxis"},
            desc = "Restrict to initial y axis"
    )
    public Mask yaxis() {
        return new YAxisMask();
    }

    @Command(
            aliases = {"#zaxis"},
            desc = "Restrict to initial z axis"
    )
    public Mask zaxis() {
        return new ZAxisMask();
    }

    @Command(
            aliases = {"#id"},
            desc = "Restrict to initial id"
    )
    public Mask id(Extent extent) {
        return new IdMask(extent);
    }

    @Command(
            aliases = {"#data"},
            desc = "Restrict to initial data"
    )
    public Mask data(Extent extent) {
        return new DataMask(extent);
    }

    @Command(
            aliases = {"#iddata"},
            desc = "Restrict to initial block id and data"
    )
    public Mask iddata(Extent extent) {
        return new IdDataMask(extent);
    }

    @Command(
            aliases = {"#air"},
            desc = "Restrict to types of air"
    )
    public Mask air(Extent extent) {
        return new BlockMaskBuilder().addAll(b -> b.getMaterial().isAir()).build(extent);
    }

    @Command(
            aliases = {"#wall"},
            desc = "Restrict to walls (any block n,e,s,w of air)"
    )
    public Mask wall(Extent extent) {
        Mask blockMask = air(extent);
        return new MaskUnion(new ExistingBlockMask(extent), new WallMask(blockMask, 1, 8));
    }

    @Command(
            aliases = {"#surface"},
            desc = "Restrict to surfaces (any solid block touching air)"
    )
    public Mask surface(Extent extent) {
        return new SurfaceMask(extent);
    }

    @Command(
            aliases = {"\\", "/", "#angle"},
            desc = "Restrict to specific terrain angle",
            help = "Restrict to specific terrain angle\n" +
                    "The -o flag will only overlay\n" +
                    "Example: /[0d][45d]\n" +
                    "Explanation: Allows any block where the adjacent block is between 0 and 45 degrees.\n" +
                    "Example: /[3][20]\n" +
                    "Explanation: Allows any block where the adjacent block is between 3 and 20 blocks below",
            usage = "<min> <max> [distance=1]",
            min = 2
    )
    public Mask angle(Extent extent, String min, String max, @Switch('o') boolean overlay, @Optional("1") int distance) throws ExpressionException {
        double y1, y2;
        boolean override;
        if (max.endsWith("d")) {
            double y1d = Expression.compile(min.substring(0, min.length() - 1)).evaluate();
            double y2d = Expression.compile(max.substring(0, max.length() - 1)).evaluate();
            y1 = (Math.tan(y1d * (Math.PI / 180)));
            y2 = (Math.tan(y2d * (Math.PI / 180)));
        } else {
            y1 = (Expression.compile(min).evaluate());
            y2 = (Expression.compile(max).evaluate());
        }
        return new AngleMask(extent, y1, y2, overlay, distance);
    }

    @Command(
            aliases = {"(", ")", "#roc"},
            desc = "Restrict to near specific terrain slope rate of change",
            help = "Restrict to near specific terrain slope rate of change\n" +
                    "The -o flag will only overlay\n" +
                    "Example: ([0d][45d][5]\n" +
                    "Explanation: Restrict near where the angle changes between 0-45 degrees within 5 blocks\n" +
                    "Note: Use negatives for decreasing slope",
            usage = "<min> <max> [distance=4]",
            min = 2
    )
    public Mask roc(Extent extent, String min, String max, @Switch('o') boolean overlay, @Optional("4") int distance) throws ExpressionException {
        double y1, y2;
        boolean override;
        if (max.endsWith("d")) {
            double y1d = Expression.compile(min.substring(0, min.length() - 1)).evaluate();
            double y2d = Expression.compile(max.substring(0, max.length() - 1)).evaluate();
            y1 = (Math.tan(y1d * (Math.PI / 180)));
            y2 = (Math.tan(y2d * (Math.PI / 180)));
        } else {
            y1 = (Expression.compile(min).evaluate());
            y2 = (Expression.compile(max).evaluate());
        }
        return new ROCAngleMask(extent, y1, y2, overlay, distance);
    }

    @Command(
            aliases = {"^", "#extrema"},
            desc = "Restrict to near specific terrain extrema",
            help = "Restrict to near specific terrain extrema\n" +
                    "The -o flag will only overlay\n" +
                    "Example: ([0d][45d][5]\n" +
                    "Explanation: Restrict to near 45 degrees of local maxima\n" +
                    "Note: Use negatives for local minima",
            usage = "<min> <max> [distance=1]",
            min = 2,
            max = 4
    )
    public Mask extrema(Extent extent, String min, String max, @Switch('o') boolean overlay, @Optional("4") int distance) throws ExpressionException {
        double y1, y2;
        boolean override;
        if (max.endsWith("d")) {
            double y1d = Expression.compile(min.substring(0, min.length() - 1)).evaluate();
            double y2d = Expression.compile(max.substring(0, max.length() - 1)).evaluate();
            y1 = (Math.tan(y1d * (Math.PI / 180)));
            y2 = (Math.tan(y2d * (Math.PI / 180)));
        } else {
            y1 = (Expression.compile(min).evaluate());
            y2 = (Expression.compile(max).evaluate());
        }
        return new ExtremaMask(extent, y1, y2, overlay, distance);
    }

    @Command(
            aliases = {"{"},
            desc = "Restricts blocks to within a specific radius range of the initial block",
            usage = "<min> <max>",
            min = 2,
            max = 2
    )
    public Mask radius(double min, double max) throws ExpressionException {
        return new RadiusMask((int) min, (int) max);
    }

    @Command(
            aliases = {"|"},
            desc = "sides with a specific number of other blocks",
            usage = "<mask> <min> <max>",
            min = 3,
            max = 3
    )
    public Mask wall(Mask mask, double min, double max) throws ExpressionException {
        return new WallMask(mask, (int) min, (int) max);
    }

    @Command(
            aliases = {"~"},
            desc = "Adjacent to a specific number of other blocks",
            usage = "<mask> [min=1] [max=8]",
            min = 1,
            max = 3
    )
    public Mask adjacent(Mask mask, @Optional("-1") double min, @Optional("-1") double max) throws ExpressionException {
        if (min == -1 && max == -1) {
            min = 1;
            max = 8;
        } else if (max == -1) max = min;
        if (max >= 8 && min == 1) {
            return new AdjacentAnyMask(mask);
        }
        return new AdjacentMask(mask, (int) min, (int) max);
    }

    @Command(
            aliases = {"<"},
            desc = "below a specific block",
            usage = "<mask>",
            min = 1,
            max = 1
    )
    public Mask below(Mask mask) throws ExpressionException {
        OffsetMask offsetMask = new OffsetMask(mask, new Vector(0, 1, 0));
        return new MaskIntersection(offsetMask, Masks.negate(mask));
    }

    @Command(
            aliases = {">"},
            desc = "above a specific block",
            usage = "<mask>",
            min = 1,
            max = 1
    )
    public Mask above(Mask mask) throws ExpressionException {
        OffsetMask offsetMask = new OffsetMask(mask, new Vector(0, -1, 0));
        return new MaskIntersection(offsetMask, Masks.negate(mask));
    }

    @Command(
            aliases = {"$", "#biome"},
            desc = "in a specific biome",
            help = "in a specific biome. For a list of biomes use //biomelist",
            usage = "<biome>",
            min = 1,
            max = 1
    )
    public Mask biome(Extent extent, BaseBiome biome) throws ExpressionException {
        return new BiomeMask(extent, biome);
    }

    @Command(
            aliases = {"%"},
            desc = "percentage chance",
            usage = "<chance>",
            min = 1,
            max = 1
    )
    public Mask random(double chance) throws ExpressionException {
        chance = chance / 100;
        return new RandomMask(chance);
    }

    @Command(
            aliases = {"="},
            desc = "expression mask",
            usage = "<expression>",
            min = 1,
            max = 1
    )
    public Mask expression(Extent extent, String input) throws ExpressionException {
        Expression exp = Expression.compile(input, "x", "y", "z");
        WorldEditExpressionEnvironment env = new WorldEditExpressionEnvironment(extent, Vector.ONE, Vector.ZERO);
        exp.setEnvironment(env);
        return new ExpressionMask(exp);
    }

    @Command(
            aliases = {"!", "#not", "#negate"},
            desc = "Negate another mask",
            usage = "<mask>",
            min = 1,
            max = 1
    )
    public Mask expression(Mask mask) throws ExpressionException {
        return Masks.negate(mask);
    }
}
