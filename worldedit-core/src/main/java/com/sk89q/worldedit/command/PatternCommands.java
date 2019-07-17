package com.sk89q.worldedit.command;

import com.boydti.fawe.object.DataAnglePattern;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.clipboard.MultiClipboardHolder;
import com.boydti.fawe.object.collection.RandomCollection;
import com.boydti.fawe.object.pattern.*;
import com.boydti.fawe.object.random.SimplexRandom;
import com.boydti.fawe.util.ColorUtil;
import com.boydti.fawe.util.TextureUtil;
import org.enginehub.piston.annotation.Command;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.ClipboardPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.shape.WorldEditExpressionEnvironment;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.command.binding.Range;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.world.biome.BiomeType;
import org.enginehub.piston.annotation.param.Arg;

import java.awt.Color;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Command(aliases = {"patterns"},
        desc = "Help for the various patterns. [More Info](https://git.io/vSPmA)",
        descFooter = "Patterns determine what blocks are placed\n" +
        " - Use [brackets] for arguments\n" +
        " - Use , to OR multiple\n" +
        "e.g. #surfacespread[10][#existing],andesite\n" +
        "More Info: https://git.io/vSPmA"
)
public class PatternCommands extends MethodCommands {
    public PatternCommands(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Command(
            name = "#existing",
            aliases = {"#*", "*", ".*"},
            desc = "Use the block that is already there",
            usage = "[properties]"
    )
    public Pattern existing(Extent extent, @Optional String properties) { // TODO FIXME , @Optional String properties
        if (properties == null) return new ExistingPattern(extent);
        return new PropertyPattern(extent).addRegex(".*[" + properties + "]");
    }

    @Command(
            name = "#clipboard",
            aliases = {"#copy"},
            desc = "Use the blocks in your clipboard as the pattern")
    public Pattern clipboard(LocalSession session) throws EmptyClipboardException {
        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();
        return new ClipboardPattern(clipboard);
    }

    @Command(
            name = "#simplex",
            desc = "Use simplex noise to randomize blocks. Tutorial: https://imgur.com/a/rwVAE"
)
    public Pattern simplex(@Arg() double scale, Pattern other) {
        if (other instanceof RandomPattern) {
            scale = (1d / Math.max(1, scale));
            RandomCollection<Pattern> collection = ((RandomPattern) other).getCollection();
            collection.setRandom(new SimplexRandom(scale));
        }
        return other;
    }

    @Command(
            name = "#color",
            desc = "Use the block closest to a specific color"
)
    public Pattern color(TextureUtil textureUtil, String color) {
        Color colorObj = ColorUtil.parseColor(color);
        return textureUtil.getNearestBlock(colorObj.getRGB()).getDefaultState();
    }

    @Command(
            name = "#anglecolor",
            desc = "A darker block based on the existing terrain angle"
)
    public Pattern anglecolor(Extent extent, LocalSession session, @Optional("true") boolean randomize, @Optional("100") double maxComplexity, @Optional("1") int distance) {
        return new AngleColorPattern(extent, session, distance);
    }

    @Command(
            name = "#angledata",
            desc = "Block data based on the existing terrain angle"
    )
    public Pattern angledata(Extent extent, @Optional("1") int distance) {
        return new DataAnglePattern(extent, distance);
    }

    @Command(
            name = "#saturate",
            desc = "Saturate the existing block with a color"
)
    public Pattern saturate(Extent extent, LocalSession session, String arg) {
        Color color = ColorUtil.parseColor(arg);
        return new SaturatePattern(extent, color.getRGB(), session);
    }

    @Command(
            name = "#averagecolor",
            desc = "Average between the existing block and a color"
)
    public Pattern averagecolor(Extent extent, LocalSession session, String arg) {
        Color color = ColorUtil.parseColor(arg);
        return new AverageColorPattern(extent, color.getRGB(), session);
    }

    @Command(
            name = "#desaturate",
            desc = "Desaturated color of the existing block"
)
    public Pattern desaturate(Extent extent, LocalSession session, @Optional("100") double percent) {
        return new DesaturatePattern(extent, percent / 100d, session);
    }

    @Command(
            name = "#lighten",
            desc = "Lighten the existing block"
)
    public Pattern lighten(Extent extent, TextureUtil util) {
        return new ShadePattern(extent, false, util);
    }

    @Command(
            name = "#darken",
            desc = "Darken the existing block"
)
    public Pattern darken(Extent extent, TextureUtil util) {
        return new ShadePattern(extent, true, util);
    }

    @Command(
            name = "#fullcopy",
            desc = "Places your full clipboard at each block"
)
    public Pattern fullcopy(Player player, Extent extent, LocalSession session, @Optional("#copy") String location, @Optional("false") boolean rotate, @Optional("false") boolean flip) throws EmptyClipboardException, InputParseException, IOException {
        List<ClipboardHolder> clipboards;
        switch (location.toLowerCase()) {
            case "#copy":
            case "#clipboard":
                ClipboardHolder clipboard = session.getExistingClipboard();
                if (clipboard == null) {
                    throw new InputParseException("To use #fullcopy, please first copy something to your clipboard");
                }
                if (!rotate && !flip) {
                    return new FullClipboardPattern(extent, clipboard.getClipboard());
                }
                clipboards = Collections.singletonList(clipboard);
                break;
            default:
                MultiClipboardHolder multi = ClipboardFormats.loadAllFromInput(player, location, null, true);
                clipboards = multi != null ? multi.getHolders() : null;
                break;
        }
        if (clipboards == null) {
            throw new InputParseException("#fullcopy:<source>");
        }
        return new RandomFullClipboardPattern(extent, clipboards, rotate, flip);
    }

    @Command(
            name = "#buffer",
            desc = "Only place a block once while a pattern is in use",
            descFooter = "Only place a block once while a pattern is in use\n" +
                    "Use with a brush when you don't want to apply to the same spot twice"
)
    public Pattern buffer(Actor actor, Pattern pattern) {
        return new BufferedPattern(FawePlayer.wrap(actor), pattern);
    }

    @Command(
            name = "#buffer2d",
            desc = "Only place a block once in a column while a pattern is in use"
)
    public Pattern buffer2d(Actor actor, Pattern pattern) {
        return new BufferedPattern2D(FawePlayer.wrap(actor), pattern);
    }

    @Command(
            name = "#iddatamask",
            desc = "Use the pattern's id and the existing blocks data with the provided mask",
            descFooter = "Use the pattern's id and the existing blocks data with the provided mask\n" +
                    " - Use to replace slabs or where the data values needs to be shifted instead of set"
)
    public Pattern iddatamask(Actor actor, LocalSession session, Extent extent, @Range(min = 0, max = 15) int bitmask, Pattern pattern) {
        return new IdDataMaskPattern(extent, pattern, bitmask);
    }

    @Command(
            name = "#id",
            desc = "Only change the block id"
)
    public Pattern id(Actor actor, LocalSession session, Extent extent, Pattern pattern) {
        return new IdPattern(extent, pattern);
    }

    @Command(
            name = "#data",
            desc = "Only change the block data"
)
    public Pattern data(Actor actor, LocalSession session, Extent extent, Pattern pattern) {
        return new DataPattern(extent, pattern);
    }

    @Command(
            name = "#biome",
            aliases = {"$"},
            desc = "Set the biome"
)
    public Pattern biome(Actor actor, LocalSession session, Extent extent, BiomeType biome) {
        return new BiomePattern(extent, biome);
    }

    @Command(
            name = "#relative",
            aliases = {"#~", "#r", "#rel"},
            desc = "Offset the pattern to where you click"
)
    public Pattern relative(Actor actor, LocalSession session, Extent extent, Pattern pattern) {
        return new RelativePattern(pattern);
    }

    @Command(
            name = "#!x",
            aliases = {"#nx", "#nox"},
            desc = "The pattern will not be provided the x axis info",
            descFooter = "The pattern will not be provided the z axis info.\n" +
                    "Example: #!x[#!z[#~[#l3d[pattern]]]]"
)
    public Pattern nox(Actor actor, LocalSession session, Extent extent, Pattern pattern) {
        return new NoXPattern(pattern);
    }

    @Command(
            name = "#!y",
            aliases = {"#ny", "#noy"},
            desc = "The pattern will not be provided the y axis info"
)
    public Pattern noy(Actor actor, LocalSession session, Extent extent, Pattern pattern) {
        return new NoYPattern(pattern);
    }

    @Command(
            name = "#!z",
            aliases = {"#nz", "#noz"},
            desc = "The pattern will not be provided the z axis info"
)
    public Pattern noz(Actor actor, LocalSession session, Extent extent, Pattern pattern) {
        return new NoZPattern(pattern);
    }

    @Command(
            name = "#mask",
            desc = "Apply a pattern depending on a mask"
)
    public Pattern mask(Actor actor, LocalSession session, Mask mask, Pattern pass, Pattern fail) {
        PatternExtent extent = new PatternExtent(pass);
        return new MaskedPattern(mask, extent, fail);
    }

    @Command(
            name = "#offset",
            desc = "Offset a pattern"
)
    public Pattern offset(Actor actor, LocalSession session, double x, double y, double z, Pattern pattern) {
        return new OffsetPattern(pattern, (int) x, (int) y, (int) z);
    }

    @Command(
            name = "#surfacespread",
            desc = "Applies to only blocks on a surface. Selects a block from provided pattern with a given ranomized offset `[0, <distance>)`. e.g. Use `#existing` to randomly offset blocks in the world, or `#copy` to offset blocks in your clipboard"
)
    public Pattern surfacespread(Actor actor, LocalSession session, double distance, Pattern pattern) {
        return new SurfaceRandomOffsetPattern(pattern, (int) distance);
    }

    @Command(
            name = "#solidspread",
            desc = "Randomly spread solid blocks"
)
    public Pattern solidspread(Actor actor, LocalSession session, double x, double y, double z, Pattern pattern) {
        return new SolidRandomOffsetPattern(pattern, (int) x, (int) y, (int) z);
    }

    @Command(
            name = "#spread",
            aliases = {"#randomoffset"},
            desc = "Randomly spread blocks"
)
    public Pattern spread(Actor actor, LocalSession session, double x, double y, double z, Pattern pattern) {
        return new RandomOffsetPattern(pattern, (int) x, (int) y, (int) z);
    }

    @Command(
            name = "#linear",
            aliases = {"#l"},
            desc = "Sequentially set blocks from a list of patterns"
)
    public Pattern linear(Actor actor, LocalSession session, Pattern other) {
        if (other instanceof RandomPattern) {
            Set<Pattern> patterns = ((RandomPattern) other).getPatterns();
            return new LinearBlockPattern(patterns.toArray(new Pattern[patterns.size()]));
        }
        return other;
    }

    @Command(
            name = "#linear3d",
            aliases = {"#l3d"},
            desc = "Use the x,y,z coordinate to pick a block from the list"
)
    public Pattern linear3d(Actor actor, LocalSession session, Pattern other) {
        if (other instanceof RandomPattern) {
            Set<Pattern> patterns = ((RandomPattern) other).getPatterns();
            return new Linear3DBlockPattern(patterns.toArray(new Pattern[patterns.size()]));
        }
        return other;
    }

    @Command(
            name = "#linear2d",
            aliases = {"#l2d"},
            desc = "Use the x,z coordinate to pick a block from the list"
)
    public Pattern linear2d(Actor actor, LocalSession session, Pattern other) {
        if (other instanceof RandomPattern) {
            Set<Pattern> patterns = ((RandomPattern) other).getPatterns();
            return new Linear2DBlockPattern(patterns.toArray(new Pattern[patterns.size()]));
        }
        return other;
    }

    @Command(
            name = "=",
            aliases = {"#=", "#expression"},
            desc = "Expression pattern: http://wiki.sk89q.com/wiki/WorldEdit/Expression_syntax"
)
    public Pattern expression(Actor actor, LocalSession session, Extent extent, String input) throws ExpressionException {
        Expression exp = Expression.compile(input, "x", "y", "z");
        WorldEditExpressionEnvironment env = new WorldEditExpressionEnvironment(extent, Vector3.ONE, Vector3.ZERO);
        exp.setEnvironment(env);
        return new ExpressionPattern(exp);
    }
}
