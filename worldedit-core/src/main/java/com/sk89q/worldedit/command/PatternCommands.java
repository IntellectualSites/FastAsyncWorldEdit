package com.sk89q.worldedit.command;

import com.boydti.fawe.object.DataAnglePattern;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.clipboard.MultiClipboardHolder;
import com.boydti.fawe.object.collection.RandomCollection;
import com.boydti.fawe.object.pattern.*;
import com.boydti.fawe.object.random.SimplexRandom;
import com.boydti.fawe.util.ColorUtil;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.ClipboardPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.regions.shape.WorldEditExpressionEnvironment;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.command.binding.Range;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.awt.Color;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Command(aliases = {"patterns"},
        desc = "Help for the various patterns. [More Info](https://git.io/vSPmA)",
        help = "Patterns determine what blocks are placed\n" +
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
            aliases = {"#existing", "#*", "*", ".*"},
            desc = "Use the block that is already there",
            usage = "[properties]"
    )
    public Pattern existing(Extent extent, @Optional String properties) { // TODO FIXME , @Optional String properties
        if (properties == null) return new ExistingPattern(extent);
        return new PropertyPattern(extent).addRegex(".*[" + properties + "]");
    }

    @Command(
            aliases = {"#clipboard", "#copy"},
            desc = "Use the blocks in your clipboard as the pattern")
    public Pattern clipboard(LocalSession session) throws EmptyClipboardException {
        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();
        return new ClipboardPattern(clipboard);
    }

    @Command(
            aliases = {"#simplex"},
            desc = "Use simplex noise to randomize blocks. Tutorial: https://imgur.com/a/rwVAE",
            usage = "<scale=10> <pattern>",
            min = 2,
            max = 2
    )
    public Pattern simplex(double scale, Pattern other) {
        if (other instanceof RandomPattern) {
            scale = (1d / Math.max(1, scale));
            RandomCollection<Pattern> collection = ((RandomPattern) other).getCollection();
            collection.setRandom(new SimplexRandom(scale));
        }
        return other;
    }

    @Command(
            aliases = {"#color"},
            desc = "Use the block closest to a specific color",
            usage = "<color>",
            min = 1,
            max = 1
    )
    public Pattern color(TextureUtil textureUtil, String arg) {
        Color color = ColorUtil.parseColor(arg);
        return textureUtil.getNearestBlock(color.getRGB()).getDefaultState();
    }

    @Command(
            aliases = {"#anglecolor"},
            desc = "A darker block based on the existing terrain angle",
            usage = "[randomize=true] [max-complexity=100] [distance=1]",
            min = 0,
            max = 3
    )
    public Pattern anglecolor(Extent extent, LocalSession session, @Optional("true") boolean randomize, @Optional("100") double maxComplexity, @Optional("1") int distance) {
        return new AngleColorPattern(extent, session, distance);
    }

    @Command(
            aliases = {"#angledata"},
            usage = "[distance=1]",
            desc = "Block data based on the existing terrain angle"
    )
    public Pattern angledata(Extent extent, @Optional("1") int distance) {
        return new DataAnglePattern(extent, distance);
    }

    @Command(
            aliases = {"#saturate"},
            desc = "Saturate the existing block with a color",
            usage = "<color>",
            min = 1,
            max = 3
    )
    public Pattern saturate(Extent extent, LocalSession session, String arg) {
        Color color = ColorUtil.parseColor(arg);
        return new SaturatePattern(extent, color.getRGB(), session);
    }

    @Command(
            aliases = {"#averagecolor"},
            desc = "Average between the existing block and a color",
            usage = "<color>",
            min = 1,
            max = 3
    )
    public Pattern averagecolor(Extent extent, LocalSession session, String arg) {
        Color color = ColorUtil.parseColor(arg);
        return new AverageColorPattern(extent, color.getRGB(), session);
    }

    @Command(
            aliases = {"#desaturate"},
            desc = "Desaturated color of the existing block",
            usage = "[percent=100] [randomize=true] [max-complexity=100]",
            min = 0,
            max = 3
    )
    public Pattern desaturate(Extent extent, LocalSession session, @Optional("100") double percent) {
        return new DesaturatePattern(extent, percent / 100d, session);
    }

    @Command(
            aliases = {"#lighten"},
            desc = "Lighten the existing block",
            min = 0,
            max = 2
    )
    public Pattern lighten(Extent extent, TextureUtil util) {
        return new ShadePattern(extent, false, util);
    }

    @Command(
            aliases = {"#darken"},
            desc = "Darken the existing block",
            min = 0,
            max = 2
    )
    public Pattern darken(Extent extent, TextureUtil util) {
        return new ShadePattern(extent, true, util);
    }

    @Command(
            aliases = {"#fullcopy"},
            desc = "Places your full clipboard at each block",
            usage = "[schem|folder|url=#copy] [rotate=false] [flip=false]",
            min = 0,
            max = 2
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
                MultiClipboardHolder multi = ClipboardFormat.SCHEMATIC.loadAllFromInput(player, location, true);
                clipboards = multi != null ? multi.getHolders() : null;
                break;
        }
        if (clipboards == null) {
            throw new InputParseException("#fullcopy:<source>");
        }
        return new RandomFullClipboardPattern(extent, clipboards, rotate, flip);
    }

    @Command(
            aliases = {"#buffer"},
            desc = "Only place a block once while a pattern is in use",
            help = "Only place a block once while a pattern is in use\n" +
                    "Use with a brush when you don't want to apply to the same spot twice",
            usage = "<pattern>",
            min = 1,
            max = 1
    )
    public Pattern buffer(Actor actor, Pattern pattern) {
        return new BufferedPattern(FawePlayer.wrap(actor), pattern);
    }

    @Command(
            aliases = {"#buffer2d"},
            desc = "Only place a block once in a column while a pattern is in use",
            usage = "<pattern>",
            min = 1,
            max = 1
    )
    public Pattern buffer2d(Actor actor, Pattern pattern) {
        return new BufferedPattern2D(FawePlayer.wrap(actor), pattern);
    }

    @Command(
            aliases = {"#iddatamask"},
            desc = "Use the pattern's id and the existing blocks data with the provided mask",
            help = "Use the pattern's id and the existing blocks data with the provided mask\n" +
                    " - Use to replace slabs or where the data values needs to be shifted instead of set",
            usage = "<bitmask=15> <pattern>",
            min = 2,
            max = 2
    )
    public Pattern iddatamask(Actor actor, LocalSession session, Extent extent, @Range(min = 0, max = 15) int bitmask, Pattern pattern) {
        return new IdDataMaskPattern(extent, pattern, bitmask);
    }

    @Command(
            aliases = {"#id"},
            desc = "Only change the block id",
            usage = "<pattern>",
            min = 1,
            max = 1
    )
    public Pattern id(Actor actor, LocalSession session, Extent extent, Pattern pattern) {
        return new IdPattern(extent, pattern);
    }

    @Command(
            aliases = {"#data"},
            desc = "Only change the block data",
            usage = "<pattern>",
            min = 1,
            max = 1
    )
    public Pattern data(Actor actor, LocalSession session, Extent extent, Pattern pattern) {
        return new DataPattern(extent, pattern);
    }

    @Command(
            aliases = {"#biome", "$"},
            desc = "Set the biome",
            usage = "<biome>",
            min = 1,
            max = 1
    )
    public Pattern data(Actor actor, LocalSession session, Extent extent, BaseBiome biome) {
        return new BiomePattern(extent, biome);
    }

    @Command(
            aliases = {"#relative", "#~", "#r", "#rel"},
            desc = "Offset the pattern to where you click",
            usage = "<pattern>",
            min = 1,
            max = 1
    )
    public Pattern relative(Actor actor, LocalSession session, Extent extent, Pattern pattern) {
        return new RelativePattern(pattern);
    }

    @Command(
            aliases = {"#!x", "#nx", "#nox"},
            desc = "The pattern will not be provided the x axis info",
            help = "The pattern will not be provided the z axis info.\n" +
                    "Example: #!x[#!z[#~[#l3d[pattern]]]]",
            usage = "<pattern>",
            min = 1,
            max = 1
    )
    public Pattern nox(Actor actor, LocalSession session, Extent extent, Pattern pattern) {
        return new NoXPattern(pattern);
    }

    @Command(
            aliases = {"#!y", "#ny", "#noy"},
            desc = "The pattern will not be provided the y axis info",
            usage = "<pattern>",
            min = 1,
            max = 1
    )
    public Pattern noy(Actor actor, LocalSession session, Extent extent, Pattern pattern) {
        return new NoYPattern(pattern);
    }

    @Command(
            aliases = {"#!z", "#nz", "#noz"},
            desc = "The pattern will not be provided the z axis info",
            usage = "<pattern>",
            min = 1,
            max = 1
    )
    public Pattern noz(Actor actor, LocalSession session, Extent extent, Pattern pattern) {
        return new NoZPattern(pattern);
    }

    @Command(
            aliases = {"#mask"},
            desc = "Apply a pattern depending on a mask",
            usage = "<mask> <pattern-true> <pattern-false>",
            min = 3,
            max = 3
    )
    public Pattern mask(Actor actor, LocalSession session, Mask mask, Pattern pass, Pattern fail) {
        PatternExtent extent = new PatternExtent(pass);
        return new MaskedPattern(mask, extent, fail);
    }

    @Command(
            aliases = {"#offset"},
            desc = "Offset a pattern",
            usage = "<dx> <dy> <dz> <pattern>",
            min = 4,
            max = 4
    )
    public Pattern offset(Actor actor, LocalSession session, double x, double y, double z, Pattern pattern) {
        return new OffsetPattern(pattern, (int) x, (int) y, (int) z);
    }

    @Command(
            aliases = {"#surfacespread"},
            desc = "Applies to only blocks on a surface. Selects a block from provided pattern with a given ranomized offset `[0, <distance>)`. e.g. Use `#existing` to randomly offset blocks in the world, or `#copy` to offset blocks in your clipboard",
            usage = "<distance> <pattern>",
            min = 2,
            max = 2
    )
    public Pattern surfacespread(Actor actor, LocalSession session, double distance, Pattern pattern) {
        return new SurfaceRandomOffsetPattern(pattern, (int) distance);
    }

    @Command(
            aliases = {"#solidspread"},
            desc = "Randomly spread solid blocks",
            usage = "<dx> <dy> <dz> <pattern>",
            min = 4,
            max = 4
    )
    public Pattern solidspread(Actor actor, LocalSession session, double x, double y, double z, Pattern pattern) {
        return new SolidRandomOffsetPattern(pattern, (int) x, (int) y, (int) z);
    }

    @Command(
            aliases = {"#spread", "#randomoffset"},
            desc = "Randomly spread blocks",
            usage = "<dx> <dy> <dz> <pattern>",
            min = 4,
            max = 4
    )
    public Pattern spread(Actor actor, LocalSession session, double x, double y, double z, Pattern pattern) {
        return new RandomOffsetPattern(pattern, (int) x, (int) y, (int) z);
    }

    @Command(
            aliases = {"#linear", "#l"},
            desc = "Sequentially set blocks from a list of patterns",
            usage = "<pattern>",
            min = 1,
            max = 1
    )
    public Pattern linear(Actor actor, LocalSession session, Pattern other) {
        if (other instanceof RandomPattern) {
            Set<Pattern> patterns = ((RandomPattern) other).getPatterns();
            return new LinearBlockPattern(patterns.toArray(new Pattern[patterns.size()]));
        }
        return other;
    }

    @Command(
            aliases = {"#linear3d", "#l3d"},
            desc = "Use the x,y,z coordinate to pick a block from the list",
            usage = "<pattern>",
            min = 1,
            max = 1
    )
    public Pattern linear3d(Actor actor, LocalSession session, Pattern other) {
        if (other instanceof RandomPattern) {
            Set<Pattern> patterns = ((RandomPattern) other).getPatterns();
            return new Linear3DBlockPattern(patterns.toArray(new Pattern[patterns.size()]));
        }
        return other;
    }

    @Command(
            aliases = {"#linear2d", "#l2d"},
            desc = "Use the x,z coordinate to pick a block from the list",
            usage = "<pattern>",
            min = 1,
            max = 1
    )
    public Pattern linear2d(Actor actor, LocalSession session, Pattern other) {
        if (other instanceof RandomPattern) {
            Set<Pattern> patterns = ((RandomPattern) other).getPatterns();
            return new Linear2DBlockPattern(patterns.toArray(new Pattern[patterns.size()]));
        }
        return other;
    }

    @Command(
            aliases = {"="},
            desc = "Expression pattern: http://wiki.sk89q.com/wiki/WorldEdit/Expression_syntax",
            usage = "<expression>",
            min = 1,
            max = 1
    )
    public Pattern expression(Actor actor, LocalSession session, Extent extent, String input) throws ExpressionException {
        Expression exp = Expression.compile(input, "x", "y", "z");
        WorldEditExpressionEnvironment env = new WorldEditExpressionEnvironment(extent, Vector.ONE, Vector.ZERO);
        exp.setEnvironment(env);
        return new ExpressionPattern(exp);
    }
}
