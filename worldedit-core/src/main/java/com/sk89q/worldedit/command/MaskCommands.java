package com.sk89q.worldedit.command;

import com.boydti.fawe.object.mask.AdjacentAnyMask;
import com.boydti.fawe.object.mask.AdjacentMask;
import com.boydti.fawe.object.mask.AngleMask;
import com.boydti.fawe.object.mask.BiomeMask;
import com.boydti.fawe.object.mask.BlockLightMask;
import com.boydti.fawe.object.mask.BrightnessMask;
import com.boydti.fawe.object.mask.DataMask;
import com.boydti.fawe.object.mask.ExtremaMask;
import com.boydti.fawe.object.mask.IdDataMask;
import com.boydti.fawe.object.mask.IdMask;
import com.boydti.fawe.object.mask.LightMask;
import com.boydti.fawe.object.mask.OpacityMask;
import com.boydti.fawe.object.mask.ROCAngleMask;
import com.boydti.fawe.object.mask.RadiusMask;
import com.boydti.fawe.object.mask.RandomMask;
import com.boydti.fawe.object.mask.SimplexMask;
import com.boydti.fawe.object.mask.SkyLightMask;
import com.boydti.fawe.object.mask.SurfaceMask;
import com.boydti.fawe.object.mask.WallMask;
import com.boydti.fawe.object.mask.XAxisMask;
import com.boydti.fawe.object.mask.YAxisMask;
import com.boydti.fawe.object.mask.ZAxisMask;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockMaskBuilder;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.ExpressionMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.mask.MaskUnion;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.mask.OffsetMask;
import com.sk89q.worldedit.function.mask.RegionMask;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.shape.WorldEditExpressionEnvironment;
import com.sk89q.worldedit.session.request.RequestSelection;
import com.sk89q.worldedit.world.biome.BiomeType;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.Switch;

//@Command(aliases = {"masks"},
//        desc = "Help for the various masks. [More Info](https://git.io/v9r4K)",
//        descFooter = "Masks determine if a block can be placed\n" +
//                " - Use [brackets] for arguments\n" +
//                " - Use , to OR multiple\n" +
//                " - Use & to AND multiple\n" +
//                "e.g. >[stone,dirt],#light[0][5],$jungle\n" +
//                "More Info: https://git.io/v9r4K"
//)
public class MaskCommands extends MethodCommands {
    private final WorldEdit worldEdit;

    public MaskCommands(WorldEdit worldEdit) {
        this.worldEdit = worldEdit;
    }

    @Command(
            name = "#simplex",
            desc = "Use simplex noise as the mask"
)
    public Mask simplex(double scale, double min, double max) {
        scale = (1d / Math.max(1, scale));
        min = (min - 50) / 50;
        max = (max - 50) / 50;
        return new SimplexMask(scale, min, max);
    }

    @Command(
            name = "#light",
            desc = "Restrict to specific light levels"
)
    public Mask light(Extent extent, double min, double max) {
        return new LightMask(extent, (int) min, (int) max);
    }

    @Command(
            name = "false",
            aliases = {"#false"},
            desc = "Always false"
    )
    public Mask falseMask(Extent extent) {
        return Masks.alwaysFalse();
    }

    @Command(
            name = "true",
            aliases = {"#true"},
            desc = "Always true"
    )
    public Mask trueMask(Extent extent) {
        return Masks.alwaysTrue();
    }

    @Command(
            name = "#skylight",
            desc = "Restrict to specific sky light levels"
    )
    public Mask skylight(Extent extent, double min, double max) {
        return new SkyLightMask(extent, (int) min, (int) max);
    }

    @Command(
            name = "#blocklight",
            aliases = {"#emittedlight"},
            desc = "Restrict to specific block light levels"
)
    public Mask blocklight(Extent extent, double min, double max) {
        return new BlockLightMask(extent, (int) min, (int) max);
    }

    @Command(
            name = "#opacity",
            desc = "Restrict to specific opacity levels"
)
    public Mask opacity(Extent extent, double min, double max) {
        return new OpacityMask(extent, (int) min, (int) max);
    }

    @Command(
            name = "#brightness",
            desc = "Restrict to specific block brightness"
)
    public Mask brightness(Extent extent, double min, double max) {
        return new BrightnessMask(extent, (int) min, (int) max);
    }

    @Command(
            name = "#offset",
            desc = "Offset a mask"
)
    public Mask offset(double x, double y, double z, Mask mask) {
        return new OffsetMask(mask, BlockVector3.at(x, y, z));
    }

    @Command(
            name = "#haslight",
            desc = "Restricts to blocks with light (sky or emitted)"
    )
    public Mask haslight(Extent extent) {
        return new LightMask(extent, 1, Integer.MAX_VALUE);
    }

    @Command(
            name = "#nolight",
            desc = "Restrict to blocks without light (sky or emitted)"
    )
    public Mask nolight(Extent extent) {
        return new LightMask(extent, 0, 0);
    }

    @Command(
            name = "#existing",
            desc = "If there is a non air block"
    )
    public Mask existing(Extent extent) {
        return new ExistingBlockMask(extent);
    }

    @Command(
            name = "#solid",
            desc = "If there is a solid block"
    )
    public Mask solid(Extent extent) {
        return new SolidBlockMask(extent);
    }

    @Command(
            name = "#liquid",
            desc = "If there is a solid block"
    )
    public Mask liquid(Extent extent) {
        return new BlockMaskBuilder().addAll(b -> b.getMaterial().isLiquid()).build(extent);
    }

    @Command(
            name = "#dregion",
            aliases = {"#dselection", "#dsel"},
            desc = "inside the player's selection"
    )
    public Mask dregion() {
        return new RegionMask(new RequestSelection());
    }

    @Command(
            name = "#region",
            aliases = {"#selection", "#sel"},
            desc = "inside the provided selection"
    )
    public Mask selection(Player player, LocalSession session) throws IncompleteRegionException {
        return new RegionMask(session.getSelection(player.getWorld()).clone());
    }

    @Command(
            name = "#xaxis",
            desc = "Restrict to initial x axis"
    )
    public Mask xaxis() {
        return new XAxisMask();
    }

    @Command(
            name = "#yaxis",
            desc = "Restrict to initial y axis"
    )
    public Mask yaxis() {
        return new YAxisMask();
    }

    @Command(
            name = "#zaxis",
            desc = "Restrict to initial z axis"
    )
    public Mask zaxis() {
        return new ZAxisMask();
    }

    @Command(
            name = "#id",
            desc = "Restrict to initial id"
    )
    public Mask id(Extent extent) {
        return new IdMask(extent);
    }

    @Command(
            name = "#data",
            desc = "Restrict to initial data"
    )
    public Mask data(Extent extent) {
        return new DataMask(extent);
    }

    @Command(
            name = "#iddata",
            desc = "Restrict to initial block id and data"
    )
    public Mask iddata(Extent extent) {
        return new IdDataMask(extent);
    }

    @Command(
            name = "#air",
            desc = "Restrict to types of air"
    )
    public Mask air(Extent extent) {
        return new BlockMaskBuilder().addAll(b -> b.getMaterial().isAir()).build(extent);
    }

    @Command(
            name = "#wall",
            desc = "Restrict to walls (any block n,e,s,w of air)"
    )
    public Mask wall(Extent extent) {
        Mask blockMask = air(extent);
        return new MaskUnion(new ExistingBlockMask(extent), new WallMask(blockMask, 1, 8));
    }

    @Command(
            name = "#surface",
            desc = "Restrict to surfaces (any solid block touching air)"
    )
    public Mask surface(Extent extent) {
        return new SurfaceMask(extent);
    }

    @Command(
            name = "\\",
            aliases = {"/", "#angle", "#\\", "#/"},
            desc = "Restrict to specific terrain angle",
            descFooter = "Restrict to specific terrain angle\n" +
                    "The -o flag will only overlay\n" +
                    "Example: /[0d][45d]\n" +
                    "Explanation: Allows any block where the adjacent block is between 0 and 45 degrees.\n" +
                    "Example: /[3][20]\n" +
                    "Explanation: Allows any block where the adjacent block is between 3 and 20 blocks below"
)
    public Mask angle(Extent extent, String min, String max, @Switch(name = 'o', desc = "TODO") boolean overlay, @Arg(name = "distance", desc = "int", def = "1") int distance) throws ExpressionException {
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
            name = "(",
            aliases = {")", "#roc", "#(", "#)"},
            desc = "Restrict to near specific terrain slope rate of change",
            descFooter = "Restrict to near specific terrain slope rate of change\n" +
                    "The -o flag will only overlay\n" +
                    "Example: ([0d][45d][5]\n" +
                    "Explanation: Restrict near where the angle changes between 0-45 degrees within 5 blocks\n" +
                    "Note: Use negatives for decreasing slope"
)
    public Mask roc(Extent extent, String min, String max, @Switch(name = 'o', desc = "TODO") boolean overlay, @Arg(name = "distance", desc = "int", def = "4") int distance) throws ExpressionException {
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
            name = "^",
            aliases = {"#extrema", "#^"},
            desc = "Restrict to near specific terrain extrema",
            descFooter = "Restrict to near specific terrain extrema\n" +
                    "The -o flag will only overlay\n" +
                    "Example: ([0d][45d][5]\n" +
                    "Explanation: Restrict to near 45 degrees of local maxima\n" +
                    "Note: Use negatives for local minima"
)
    public Mask extrema(Extent extent, String min, String max, @Switch(name = 'o', desc = "TODO") boolean overlay, @Arg(name = "distance", desc = "int", def = "4") int distance) throws ExpressionException {
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
            name = "{",
            aliases = {"#{"},
            desc = "Restricts blocks to within a specific radius range of the initial block"
)
    public Mask radius(double min, double max) throws ExpressionException {
        return new RadiusMask((int) min, (int) max);
    }

    @Command(
            name = "|",
            aliases = {"#|", "#side"},
            desc = "sides with a specific number of other blocks"
)
    public Mask wall(Mask mask, double min, double max) throws ExpressionException {
        return new WallMask(mask, (int) min, (int) max);
    }

    @Command(
            name = "~",
            aliases = {"#~", "#adjacent"},
            desc = "Adjacent to a specific number of other blocks"
)
    public Mask adjacent(Mask mask, @Arg(name = "min", desc = "double", def = "-1") double min, @Arg(name = "max", desc = "double", def = "-1") double max) throws ExpressionException {
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
            name = "<",
            aliases = {"#<", "#below"},
            desc = "below a specific block"
)
    public Mask below(Mask mask) throws ExpressionException {
        OffsetMask offsetMask = new OffsetMask(mask, BlockVector3.at(0, 1, 0));
        return new MaskIntersection(offsetMask, Masks.negate(mask));
    }

    @Command(
            name = ">",
            aliases = {"#>", "#above"},
            desc = "above a specific block"
)
    public Mask above(Mask mask) throws ExpressionException {
        OffsetMask offsetMask = new OffsetMask(mask, BlockVector3.at(0, -1, 0));
        return new MaskIntersection(offsetMask, Masks.negate(mask));
    }

    @Command(
            name = "$",
            aliases = {"#biome", "#$"},
            desc = "in a specific biome",
            descFooter = "in a specific biome. For a list of biomes use //biomelist"
)
    public Mask biome(Extent extent, BiomeType biome) throws ExpressionException {
        return new BiomeMask(extent, biome);
    }

    @Command(
            name = "%",
            aliases = {"#%", "#percent"},
            desc = "percentage chance"
)
    public Mask random(double chance) throws ExpressionException {
        chance = chance / 100;
        return new RandomMask(chance);
    }

    @Command(
            name = "=",
            aliases = {"#=", "#expression"},
            desc = "expression mask"
)
    public Mask expression(Extent extent, String input) throws ExpressionException {
        Expression exp = Expression.compile(input, "x", "y", "z");
        WorldEditExpressionEnvironment env = new WorldEditExpressionEnvironment(extent, Vector3.ONE, Vector3.ZERO);
        exp.setEnvironment(env);
        return new ExpressionMask(exp);
    }

    @Command(
            name = "!",
            aliases = {"#not", "#negate", "#!"},
            desc = "Negate another mask"
)
    public Mask expression(Mask mask) throws ExpressionException {
        return Masks.negate(mask);
    }
}
