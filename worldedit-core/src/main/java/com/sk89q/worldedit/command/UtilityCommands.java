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
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.command.FaweParser;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Commands;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.DelegateConsumer;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal3;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.chat.Message;
import com.boydti.fawe.util.chat.UsageMessage;
import com.boydti.fawe.util.image.ImageUtil;
import com.sk89q.minecraft.util.commands.*;

import static com.sk89q.minecraft.util.commands.Logging.LogMode.PLACEMENT;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Logging;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.util.CreatureButcher;
import com.sk89q.worldedit.command.util.EntityRemover;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.factory.DefaultTransformParser;
import com.sk89q.worldedit.extension.factory.parser.mask.DefaultMaskParser;
import com.sk89q.worldedit.extension.factory.parser.pattern.DefaultPatternParser;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.EntityVisitor;
import com.sk89q.worldedit.internal.annotation.Direction;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.expression.runtime.EvaluationException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.command.binding.Range;
import com.sk89q.worldedit.util.command.CommandCallable;
import com.sk89q.worldedit.util.command.CommandMapping;
import com.sk89q.worldedit.util.command.Dispatcher;
import com.sk89q.worldedit.util.command.binding.Text;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Utility commands.
 */
@Command(aliases = {}, desc = "Various utility commands: [More Info](http://wiki.sk89q.com/wiki/WorldEdit/Utilities)")
public class UtilityCommands extends MethodCommands {

    private final WorldEdit we;

    public UtilityCommands(WorldEdit we) {
        super(we);
        this.we = we;
    }

    @Command(
            aliases = {"patterns"},
            usage = "[page=1|search|pattern]",
            desc = "View help about patterns",
            help = "Patterns determine what blocks are placed\n" +
                    " - Use [brackets] for arguments\n" +
                    " - Use , to OR multiple\n" +
                    "e.g. #surfacespread[10][#existing],andesite\n" +
                    "More Info: https://git.io/vSPmA",
            queued = false
    )
    public void patterns(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        displayModifierHelp(player, DefaultPatternParser.class, args);
    }

    @Command(
            aliases = {"masks"},
            usage = "[page=1|search|mask]",
            desc = "View help about masks",
            help = "Masks determine if a block can be placed\n" +
                    " - Use [brackets] for arguments\n" +
                    " - Use , to OR multiple\n" +
                    " - Use & to AND multiple\n" +
                    "e.g. >[stone,dirt],#light[0][5],$jungle\n" +
                    "More Info: https://git.io/v9r4K",
            queued = false
    )
    public void masks(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        displayModifierHelp(player, DefaultMaskParser.class, args);
    }

    @Command(
            aliases = {"transforms"},
            usage = "[page=1|search|transform]",
            desc = "View help about transforms",
            help = "Transforms modify how a block is placed\n" +
                    " - Use [brackets] for arguments\n" +
                    " - Use , to OR multiple\n" +
                    " - Use & to AND multiple\n" +
                    "More Info: https://git.io/v9KHO",
            queued = false
    )
    public void transforms(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        displayModifierHelp(player, DefaultTransformParser.class, args);
    }

    private void displayModifierHelp(Player player, Class<? extends FaweParser> clazz, CommandContext args) {
        FaweParser parser = FaweAPI.getParser(clazz);
        if (args.argsLength() == 0) {
            String base = getCommand().aliases()[0];
            UsageMessage msg = new UsageMessage(getCallable(), (WorldEdit.getInstance().getConfiguration().noDoubleSlash ? "" : "/") + base, args.getLocals());
            msg.newline().paginate(base, 0, 1).send(player);
            return;
        }
        if (parser != null) {
            CommandMapping mapping = parser.getDispatcher().get(args.getString(0));
            if (mapping != null) {
                new UsageMessage(mapping.getCallable(), args.getString(0), args.getLocals()) {
                    @Override
                    public String separateArg(String arg) {
                        return "&7[" + arg + "&7]";
                    }
                }.send(player);
            } else {
                UtilityCommands.help(args, player, getCommand().aliases()[0] + " ", parser.getDispatcher());
            }
        }
    }

    @Command(
            aliases = {"/heightmapinterface"},
            desc = "Generate the heightmap interface: https://github.com/boy0001/HeightMap"
    )
    @CommandPermissions("fawe.admin")
    public void heightmapInterface(Player player, @Optional("100") int min, @Optional("200") int max) throws IOException {
        player.print("Please wait while we generate the minified heightmaps.");
        File srcFolder = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HEIGHTMAP);

        File webSrc = new File(Fawe.imp().getDirectory(), "web" + File.separator + "heightmap");
        File minImages = new File(webSrc, "images" + File.separator + "min");
        File maxImages = new File(webSrc, "images" + File.separator + "max");
        final int sub = srcFolder.getAbsolutePath().length();
        List<String> images = new ArrayList<>();
        MainUtil.iterateFiles(srcFolder, file -> {
            switch (file.getName().substring(file.getName().lastIndexOf('.')).toLowerCase()) {
                case ".png":
                case ".jpeg":
                    break;
                default:
                    return;
            }
            try {
                String name = file.getAbsolutePath().substring(sub);
                if (name.startsWith(File.separator)) name = name.replaceFirst(java.util.regex.Pattern.quote(File.separator), "");
                BufferedImage img = MainUtil.readImage(file);
                BufferedImage minImg = ImageUtil.getScaledInstance(img, min, min, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
                BufferedImage maxImg = max == -1 ? img : ImageUtil.getScaledInstance(img, max, max, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
                player.print("Writing " + name);
                File minFile = new File(minImages, name);
                File maxFile = new File(maxImages, name);
                minFile.getParentFile().mkdirs();
                maxFile.getParentFile().mkdirs();
                ImageIO.write(minImg, "png", minFile);
                ImageIO.write(maxImg, "png", maxFile);
                images.add(name);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        StringBuilder config = new StringBuilder();
        config.append("var images = [\n");
        for (String image : images) {
            config.append('"' + image.replace(File.separator, "/") + "\",\n");
        }
        config.append("];\n");
        config.append("// The low res images (they should all be the same size)\n");
        config.append("var src_min = \"images/min/\";\n");
        config.append("// The max resolution images (Use the same if there are no higher resolution ones available)\n");
        config.append("var src_max = \"images/max/\";\n");
        config.append("// The local source for the image (used in commands)\n");
        config.append("var src_local = \"file://\";\n");
        File configFile = new File(webSrc, "config.js");
        player.print("Writing " + configFile);
        Files.write(configFile.toPath(), config.toString().getBytes());
        player.print("Done! See: `FastAsyncWorldEdit/web/heightmap`");
    }

    @Command(
            aliases = {"/cancel", "fcancel"},
            desc = "Cancel your current command",
            max = 0,
            queued = false
    )
    public void cancel(FawePlayer player) {
        int cancelled = player.cancel(false);
        BBC.WORLDEDIT_CANCEL_COUNT.send(player, cancelled);
    }

    @Command(
        aliases = { "/fill" },
        usage = "<pattern> <radius> [depth] [direction]",
        desc = "Fill a hole",
        min = 2,
        max = 4
    )
    @CommandPermissions("worldedit.fill")
    @Logging(PLACEMENT)
    public void fill(Player player, LocalSession session, EditSession editSession, Pattern pattern, double radius, @Optional("1") double depth, @Optional("down") @Direction BlockVector3 direction) throws WorldEditException {
        we.checkMaxRadius(radius);
        BlockVector3 pos = session.getPlacementPosition(player);
        int affected = editSession.fillDirection(pos, pattern, radius, (int) depth, direction);
        player.print(affected + " block(s) have been created.");
    }

    @Command(
        aliases = { "/fillr" },
        usage = "<pattern> <radius> [depth]",
        desc = "Fill a hole recursively",
        min = 2,
        max = 3
    )
    @CommandPermissions("worldedit.fill.recursive")
    @Logging(PLACEMENT)
    public void fillr(Player player, LocalSession session, EditSession editSession, Pattern pattern, double radius, @Optional("-1") double depth) throws WorldEditException {
        we.checkMaxRadius(radius);
        BlockVector3 pos = session.getPlacementPosition(player);
        if (depth == -1) depth = Integer.MAX_VALUE;
        int affected = editSession.fillXZ(pos, pattern, radius, (int) depth, true);
        player.print(affected + " block(s) have been created.");
    }

    @Command(
        aliases = { "/drain" },
        usage = "<radius>",
        flags = "w",
        desc = "Drain a pool",
        help = "Removes all connected water sources.\n" +
                "  If -w is specified, also makes waterlogged blocks non-waterlogged.",
        min = 1,
        max = 1
    )
    @CommandPermissions("worldedit.drain")
    @Logging(PLACEMENT)
    public void drain(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        double radius = Math.max(0, args.getDouble(0));
        boolean waterlogged = args.hasFlag('w');
        we.checkMaxRadius(radius);
        int affected = editSession.drainArea(
                session.getPlacementPosition(player), radius, waterlogged);
        player.print(affected + " block(s) have been changed.");
    }

    @Command(
        aliases = { "/fixlava", "fixlava" },
        usage = "<radius>",
        desc = "Fix lava to be stationary",
        min = 1,
        max = 1
    )
    @CommandPermissions("worldedit.fixlava")
    @Logging(PLACEMENT)
    public void fixLava(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        double radius = Math.max(0, args.getDouble(0));
        we.checkMaxRadius(radius);
        int affected = editSession.fixLiquid(session.getPlacementPosition(player), radius, BlockTypes.LAVA);
        player.print(affected + " block(s) have been changed.");
    }

    @Command(
        aliases = { "/fixwater", "fixwater" },
        usage = "<radius>",
        desc = "Fix water to be stationary",
        min = 1,
        max = 1
    )
    @CommandPermissions("worldedit.fixwater")
    @Logging(PLACEMENT)
    public void fixWater(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        double radius = Math.max(0, args.getDouble(0));
        we.checkMaxRadius(radius);
        int affected = editSession.fixLiquid(session.getPlacementPosition(player), radius, BlockTypes.WATER);
        BBC.VISITOR_BLOCK.send(player, affected);
    }

    @Command(
        aliases = { "/removeabove", "removeabove" },
        usage = "[size] [height]",
        desc = "Remove blocks above your head.",
        min = 0,
        max = 2
    )
    @CommandPermissions("worldedit.removeabove")
    @Logging(PLACEMENT)
    public void removeAbove(Player player, LocalSession session, EditSession editSession, @Optional("1") double size, @Optional("256") double height) throws WorldEditException {

        we.checkMaxRadius(size);
        int affected = editSession.removeAbove(session.getPlacementPosition(player), (int) size, (int) height);
        BBC.VISITOR_BLOCK.send(player, affected);
    }

    @Command(
        aliases = { "/removebelow", "removebelow" },
        usage = "[size] [height]",
        desc = "Remove blocks below you.",
        min = 0,
        max = 2
    )
    @CommandPermissions("worldedit.removebelow")
    @Logging(PLACEMENT)
    public void removeBelow(Player player, LocalSession session, EditSession editSession, @Optional("1") double size, @Optional("256") double height) throws WorldEditException {

        we.checkMaxRadius(size);
        int affected = editSession.removeBelow(session.getPlacementPosition(player), (int) size, (int) height);
        BBC.VISITOR_BLOCK.send(player, affected);
    }

    @Command(
        aliases = { "/removenear", "removenear" },
        usage = "<mask> [size]",
        desc = "Remove blocks near you.",
        min = 1,
        max = 2
    )
    @CommandPermissions("worldedit.removenear")
    @Logging(PLACEMENT)
    public void removeNear(Player player, LocalSession session, EditSession editSession, Mask mask, @Optional("50") double size) throws WorldEditException {

        we.checkMaxRadius(size);
        size = Math.max(1, size);
        int affected = editSession.removeNear(session.getPlacementPosition(player), mask, (int) size);
        BBC.VISITOR_BLOCK.send(player, affected);
    }

    @Command(
        aliases = { "/replacenear", "replacenear" },
        usage = "<size> <from-id> <to-id>",
        desc = "Replace nearby blocks",
        flags = "f",
        min = 3,
        max = 3
    )
    @CommandPermissions("worldedit.replacenear")
    @Logging(PLACEMENT)
    public void replaceNear(Player player, LocalSession session, EditSession editSession, double size, @Optional Mask from, Pattern to) throws WorldEditException {

        if (from == null) {
            from = new ExistingBlockMask(editSession);
        }
        int affected;

        BlockVector3 base = session.getPlacementPosition(player);
        BlockVector3 min = base.subtract((int)size, (int)size, (int)size);
        BlockVector3 max = base.add((int)size, (int)size, (int)size);
        Region region = new CuboidRegion(player.getWorld(), min, max);

        affected = editSession.replaceBlocks(region, from, to);
        BBC.VISITOR_BLOCK.send(player, affected);
    }

    @Command(
        aliases = { "/snow", "snow" },
        usage = "[radius]",
        desc = "Simulates snow",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.snow")
    @Logging(PLACEMENT)
    public void snow(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {
        double size = args.argsLength() > 0 ? Math.max(1, args.getDouble(0)) : 10;
        we.checkMaxRadius(size);

        int affected = editSession.simulateSnow(session.getPlacementPosition(player), size);
        player.print(affected + " surfaces covered. Let it snow~");
    }

    @Command(
        aliases = {"/thaw", "thaw"},
        usage = "[radius]",
        desc = "Thaws the area",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.thaw")
    @Logging(PLACEMENT)
    public void thaw(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {
        double size = args.argsLength() > 0 ? Math.max(1, args.getDouble(0)) : 10;
        we.checkMaxRadius(size);

        int affected = editSession.thaw(session.getPlacementPosition(player), size);
        player.print(affected + " surfaces thawed.");
    }

    @Command(
        aliases = { "/green", "green" },
        usage = "[radius]",
        desc = "Greens the area",
        help = "Converts dirt to grass blocks. -f also converts coarse dirt.",
        flags = "f",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.green")
    @Logging(PLACEMENT)
    public void green(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {
        final double size = args.argsLength() > 0 ? Math.max(1, args.getDouble(0)) : 10;
        we.checkMaxRadius(size);
        final boolean onlyNormalDirt = !args.hasFlag('f');

        final int affected = editSession.green(session.getPlacementPosition(player), size, onlyNormalDirt);
        BBC.VISITOR_BLOCK.send(player, affected);
    }

    @Command(
            aliases = { "/ex", "/ext", "/extinguish", "ex", "ext", "extinguish" },
            usage = "[radius]",
            desc = "Extinguish nearby fire",
            min = 0,
            max = 1
        )
    @CommandPermissions("worldedit.extinguish")
    @Logging(PLACEMENT)
    public void extinguish(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        LocalConfiguration config = we.getConfiguration();

        int defaultRadius = config.maxRadius != -1 ? Math.min(40, config.maxRadius) : 40;
        int size = args.argsLength() > 0 ? Math.max(1, args.getInteger(0))
                : defaultRadius;
        we.checkMaxRadius(size);

        int affected = editSession.removeNear(session.getPlacementPosition(player), BlockTypes.FIRE.toMask(editSession), size);
        BBC.VISITOR_BLOCK.send(player, affected);
    }

    @Command(
        aliases = { "butcher" },
        usage = "[radius]",
        flags = "plangbtfr",
        desc = "Kill all or nearby mobs",
        help =
            "Kills nearby mobs, based on radius, if none is given uses default in configuration.\n" +
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
    @CommandPermissions("worldedit.butcher")
    @Logging(PLACEMENT)
    public void butcher(Actor actor, CommandContext args) throws WorldEditException {
        LocalConfiguration config = we.getConfiguration();
        Player player = actor instanceof Player ? (Player) actor : null;

        // technically the default can be larger than the max, but that's not my problem
        int radius = config.butcherDefaultRadius;

        // there might be a better way to do this but my brain is fried right now
        if (args.argsLength() > 0) { // user inputted radius, override the default
            radius = args.getInteger(0);
            if (radius < -1) {
                actor.printError("Use -1 to remove all mobs in loaded chunks");
                return;
            }
            if (config.butcherMaxRadius != -1) { // clamp if there is a max
                if (radius == -1) {
                    radius = config.butcherMaxRadius;
                } else { // Math.min does not work if radius is -1 (actually highest possible value)
                    radius = Math.min(radius, config.butcherMaxRadius);
                }
            }
        }

        CreatureButcher flags = new CreatureButcher(actor);
        flags.fromCommand(args);

        List<EntityVisitor> visitors = new ArrayList<>();
        LocalSession session = null;
        EditSession editSession = null;

        if (player != null) {
            session = we.getSessionManager().get(player);
            BlockVector3 center = session.getPlacementPosition(player);
            editSession = session.createEditSession(player);
            List<? extends Entity> entities;
            if (radius >= 0) {
                CylinderRegion region = CylinderRegion.createRadius(editSession, center, radius);
                entities = editSession.getEntities(region);
            } else {
                entities = editSession.getEntities();
            }
            visitors.add(new EntityVisitor(entities.iterator(), flags.createFunction()));
        } else {
            Platform platform = we.getPlatformManager().queryCapability(Capability.WORLD_EDITING);
            for (World world : platform.getWorlds()) {
                List<? extends Entity> entities = world.getEntities();
                visitors.add(new EntityVisitor(entities.iterator(), flags.createFunction()));
            }
        }

        int killed = 0;
        for (EntityVisitor visitor : visitors) {
            Operations.completeLegacy(visitor);
            killed += visitor.getAffected();
        }

        BBC.KILL_SUCCESS.send(actor, killed, radius);

        if (editSession != null) {
            session.remember(editSession);
            editSession.flushSession();
        }
    }

    @Command(
        aliases = { "remove", "rem", "rement" },
        usage = "<type> <radius>",
        desc = "Remove all entities of a type",
        min = 2,
        max = 2
    )
    @CommandPermissions("worldedit.remove")
    @Logging(PLACEMENT)
    public void remove(Actor actor, CommandContext args) throws WorldEditException, CommandException {
        String typeStr = args.getString(0);
        int radius = args.getInteger(1);
        Player player = actor instanceof Player ? (Player) actor : null;

        if (radius < -1) {
            actor.printError("Use -1 to remove all entities in loaded chunks");
            return;
        }

        EntityRemover remover = new EntityRemover();
        remover.fromString(typeStr);

        List<EntityVisitor> visitors = new ArrayList<>();
        LocalSession session = null;
        EditSession editSession = null;

        if (player != null) {
            session = we.getSessionManager().get(player);
            BlockVector3 center = session.getPlacementPosition(player);
            editSession = session.createEditSession(player);
            List<? extends Entity> entities;
            if (radius >= 0) {
                CylinderRegion region = CylinderRegion.createRadius(editSession, center, radius);
                entities = editSession.getEntities(region);
            } else {
                entities = editSession.getEntities();
            }
            visitors.add(new EntityVisitor(entities.iterator(), remover.createFunction()));
        } else {
            Platform platform = we.getPlatformManager().queryCapability(Capability.WORLD_EDITING);
            for (World world : platform.getWorlds()) {
                List<? extends Entity> entities = world.getEntities();
                visitors.add(new EntityVisitor(entities.iterator(), remover.createFunction()));
            }
        }

        int removed = 0;
        for (EntityVisitor visitor : visitors) {
            Operations.completeLegacy(visitor);
            removed += visitor.getAffected();
        }

        BBC.KILL_SUCCESS.send(actor, removed, radius);

        if (editSession != null) {
            session.remember(editSession);
            editSession.flushSession();
        }
    }

    @Command(
        aliases = { "/calc", "/calculate", "/eval", "/evaluate", "/solve" },
        usage = "<expression>",
        desc = "Evaluate a mathematical expression"
    )
    @CommandPermissions("worldedit.calc")
    public void calc(final Actor actor, @Text String input) throws CommandException {
        try {
            FaweLimit limit = FawePlayer.wrap(actor).getLimit();
            final Expression expression = Expression.compile(input);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Double> futureResult = executor.submit((Callable<Double>) expression::evaluate);

            Double result = Double.NaN;
            try {
                result = futureResult.get(limit.MAX_EXPRESSION_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                futureResult.cancel(true);
                e.printStackTrace();
            }

            executor.shutdownNow();
            actor.print("= " + result);
        } catch (EvaluationException e) {
            actor.printError(String.format(
                    "'%s' could not be evaluated (error: %s)", input, e.getMessage()));
        } catch (ExpressionException e) {
            actor.printError(String.format(
                    "'%s' could not be parsed as a valid expression", input));
        }
    }

    @Command(
            aliases = {"/confirm"},
            desc = "Confirm a command"
    )
    public void confirm(FawePlayer fp) throws WorldEditException {
        if (!fp.confirm()) {
            BBC.NOTHING_CONFIRMED.send(fp);
        }
    }

    @Command(
        aliases = { "/help" },
        usage = "[<command>]",
        desc = "Displays help for WorldEdit commands",
        min = 0,
        max = -1,
        queued = false
    )
    @CommandPermissions("worldedit.help")
    public void help(Actor actor, CommandContext args) throws WorldEditException {
        help(args, we, actor);
    }

    protected static CommandMapping detectCommand(Dispatcher dispatcher, String command, boolean isRootLevel) {
        CommandMapping mapping;

        // First try the command as entered
        mapping = dispatcher.get(command);
        if (mapping != null) {
            return mapping;
        }

        // Then if we're looking at root commands and the user didn't use
        // any slashes, let's try double slashes and then single slashes.
        // However, be aware that there exists different single slash
        // and double slash commands in WorldEdit
        if (isRootLevel && !command.contains("/")) {
            mapping = dispatcher.get("//" + command);
            if (mapping != null) {
                return mapping;
            }

            mapping = dispatcher.get("/" + command);
            if (mapping != null) {
                return mapping;
            }
        }

        return null;
    }

    public static void list(File dir, Actor actor, CommandContext args, @Range(min = 0) int page, String formatName, boolean playerFolder, String onClickCmd) {
        list(dir, actor, args, page, -1, formatName, playerFolder, new RunnableVal3<Message, URI, String>() {
            @Override
            public void run(Message m, URI uri, String fileName) {
                m.text(BBC.SCHEMATIC_LIST_ELEM, fileName, "");
                if (onClickCmd != null) m.cmdTip(onClickCmd + " " + fileName);
            }
        });
    }

    public static void list(File dir, Actor actor, CommandContext args, @Range(min = 0) int page, int perPage, String formatName, boolean playerFolder, RunnableVal3<Message, URI, String> eachMsg) {
        AtomicInteger pageInt = new AtomicInteger(page);
        List<File> fileList = new ArrayList<>();
        if (perPage == -1) perPage = actor instanceof Player ? 12 : 20; // More pages for console
        page = getFiles(dir, actor, args, page, perPage, formatName, playerFolder, fileList::add);

        if (fileList.isEmpty()) {
            BBC.SCHEMATIC_NONE.send(actor);
            return;
        }

        int pageCount = (fileList.size() + perPage - 1) / perPage;
        if (page < 1) {
            BBC.SCHEMATIC_PAGE.send(actor, ">0");
            return;
        }
        if (page > pageCount) {
            BBC.SCHEMATIC_PAGE.send(actor, "<" + (pageCount + 1));
            return;
        }

        final int sortType = args.hasFlag('d') ? -1 : args.hasFlag('n') ? 1 : 0;
        // cleanup file list
        fileList.sort((f1, f2) -> {
            boolean dir1 = f1.isDirectory();
            boolean dir2 = f2.isDirectory();
            if (dir1 != dir2)
                return dir1 ? -1 : 1;
            int res;
            if (sortType == 0) { // use name by default
                int p = f1.getParent().compareTo(f2.getParent());
                if (p == 0) { // same parent, compare names
                    res = f1.getName().compareTo(f2.getName());
                } else { // different parent, sort by that
                    res = p;
                }
            } else {
                res = Long.compare(f1.lastModified(), f2.lastModified()); // use date if there is a flag
                if (sortType == 1)
                    res = -res; // flip date for newest first instead of oldest first
            }
            return res;
        });

        int offset = (page - 1) * perPage;

        int limit = Math.min(offset + perPage, fileList.size());

        String fullArgs = (String) args.getLocals().get("arguments");
        String baseCmd = null;
        if (fullArgs != null) {
            baseCmd = fullArgs.endsWith(" " + page) ? fullArgs.substring(0, fullArgs.length() - (" " + page).length()) : fullArgs;
        }
        Message m = new Message(BBC.SCHEMATIC_LIST, page, pageCount);

        UUID uuid = playerFolder ? actor.getUniqueId() : null;
        for (int i = offset; i < limit; i++) {
            m.newline();
            File file = fileList.get(i);
            eachMsg.run(m, file.toURI(), getPath(dir, file, uuid));
        }
        if (baseCmd != null) {
            m.newline().paginate(baseCmd, page, pageCount);
        }
        m.send(actor);
    }

    public static int getFiles(File dir, Actor actor, CommandContext args, @Range(min = 0) int page, int perPage, String formatName, boolean playerFolder, Consumer<File> forEachFile) {
        Consumer<File> rootFunction = forEachFile;

        int len = args.argsLength();
        List<String> filters = new ArrayList<>();

        String dirFilter = File.separator;

        boolean listMine = false;
        boolean listGlobal = !Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS;
        if (len > 0) {
            int max = len;
            if (MathMan.isInteger(args.getString(len - 1))) {
                page = args.getInteger(--len);
            }
            for (int i = 0; i < len; i++) {
                String arg = args.getString(i);
                switch (arg.toLowerCase()) {
                    case "me":
                    case "mine":
                    case "local":
                    case "private":
                        listMine = true;
                        break;
                    case "public":
                    case "global":
                        listGlobal = true;
                        break;
                    case "all":
                        listMine = true;
                        listGlobal = true;
                        break;
                    default:
                        if (arg.endsWith("/") || arg.endsWith(File.separator)) {
                            arg = arg.replace("/", File.separator);
                            String newDirFilter = dirFilter + arg;
                            boolean exists = new File(dir, newDirFilter).exists() || playerFolder && MainUtil.resolveRelative(new File(dir, actor.getUniqueId() + newDirFilter)).exists();
                            if (!exists) {
                                arg = arg.substring(0, arg.length() - File.separator.length());
                                if (arg.length() > 3 && arg.length() <= 16) {
                                    UUID fromName = Fawe.imp().getUUID(arg);
                                    if (fromName != null) {
                                        newDirFilter = dirFilter + fromName + File.separator;
                                        listGlobal = true;
                                    }
                                }
                            }
                            dirFilter = newDirFilter;
                        }
                        else {
                            filters.add(arg);
                        }
                        break;
                }
            }
        }
        if (!listMine && !listGlobal) {
            listMine = true;
        }

        List<File> toFilter = new ArrayList<>();
        if (!filters.isEmpty()) {
            forEachFile = new DelegateConsumer<File>(forEachFile) {
                @Override
                public void accept(File file) {
                    toFilter.add(file);
                }
            };
        }

        if (formatName != null) {
            final ClipboardFormat cf = ClipboardFormats.findByAlias(formatName);
            forEachFile = new DelegateConsumer<File>(forEachFile) {
                @Override
                public void accept(File file) {
                    if (cf.isFormat(file)) super.accept(file);
                }
            };
        } else {
            forEachFile = new DelegateConsumer<File>(forEachFile) {
                @Override
                public void accept(File file) {
                    if (!file.toString().endsWith(".cached")) super.accept(file);
                }
            };
        }
        if (playerFolder) {
            if (listMine) {
                File playerDir = MainUtil.resolveRelative(new File(dir, actor.getUniqueId() + dirFilter));
                if (playerDir.exists()) allFiles(playerDir.listFiles(), false, forEachFile);
            }
            if (listGlobal) {
                File rel = MainUtil.resolveRelative(new File(dir, dirFilter));
                forEachFile = new DelegateConsumer<File>(forEachFile) {
                    @Override
                    public void accept(File f) {
                        try {
                            if (f.isDirectory()) {
                                UUID uuid = UUID.fromString(f.getName());
                                return;
                            }
                        } catch (IllegalArgumentException exception) {}
                        super.accept(f);
                    }
                };
                if (rel.exists()) allFiles(rel.listFiles(), false, forEachFile);
            }
        } else {
            File rel = MainUtil.resolveRelative(new File(dir, dirFilter));
            if (rel.exists()) allFiles(rel.listFiles(), false, forEachFile);
        }
        if (!filters.isEmpty() && !toFilter.isEmpty()) {
            List<File> result = filter(toFilter, filters);
            for (File file : result) rootFunction.accept(file);
        }
        return page;
    }

    private static List<File> filter(List<File> fileList, List<String> filters) {

        String[] normalizedNames = new String[fileList.size()];
        for (int i = 0; i < fileList.size(); i++) {
            String normalized = fileList.get(i).getName().toLowerCase();
            if (normalized.startsWith("../")) normalized = normalized.substring(3);
            normalizedNames[i] = normalized.replace("/", File.separator);
        }

        for (String filter : filters) {
            if (fileList.isEmpty()) return fileList;
            String lowerFilter = filter.toLowerCase().replace("/", File.separator);
            List<File> newList = new ArrayList<>();

            for (int i = 0; i < normalizedNames.length; i++) {
                if (normalizedNames[i].startsWith(lowerFilter)) newList.add(fileList.get(i));
            }
            if (newList.isEmpty()) {
                for (int i = 0; i < normalizedNames.length; i++) {
                    if (normalizedNames[i].contains(lowerFilter)) newList.add(fileList.get(i));
                }

                if (newList.isEmpty()) {
                    String checkName = filter.replace("\\", "/").split("/")[0];
                    if (checkName.length() > 3 && checkName.length() <= 16) {
                        UUID fromName = Fawe.imp().getUUID(checkName);
                        if (fromName != null) {
                            lowerFilter = filter.replaceFirst(checkName, fromName.toString()).toLowerCase();
                            for (int i = 0; i < normalizedNames.length; i++) {
                                if (normalizedNames[i].startsWith(lowerFilter)) newList.add(fileList.get(i));
                            }
                        }
                    }
                }
            }
            fileList = newList;
        }
        return fileList;
    }

    public static void allFiles(File[] files, boolean recursive, Consumer<File> task) {
        if (files == null || files.length == 0) return;
        for (File f : files) {
            if (f.isDirectory()) {
                if (recursive) {
                    allFiles(f.listFiles(), recursive, task);
                } else {
                    task.accept(f);
                }
            } else {
                task.accept(f);
            }
        }
    }

    private static String getPath(File root, File file, UUID uuid) {
        File dir;
        if (uuid != null) {
            dir = new File(root, uuid.toString());
        } else {
            dir = root;
        }

        ClipboardFormat format = ClipboardFormats.findByFile(file);
        URI relative = dir.toURI().relativize(file.toURI());
        StringBuilder name = new StringBuilder();
        if (relative.isAbsolute()) {
            relative = root.toURI().relativize(file.toURI());
            name.append(".." + File.separator);
        }
        name.append(relative.getPath());
        return name.toString();
    }

    public static void help(CommandContext args, WorldEdit we, Actor actor) {
        help(args, actor, "/", we.getPlatformManager().getCommandManager().getDispatcher());
    }

    public static void help(CommandContext args, Actor actor, String prefix, CommandCallable callable) {
        final int perPage = actor instanceof Player ? 12 : 20; // More pages for console

        HelpBuilder builder = new HelpBuilder(callable, args, perPage) {
            @Override
            public void displayFailure(String message) {
                actor.printError(message);
            }

            @Override
            public void displayUsage(CommandCallable callable, String command) {
                new UsageMessage(callable, command).send(actor);
            }

            @Override
            public void displayCategories(Map<String, Map<CommandMapping, String>> categories) {
                Message msg = new Message();
                msg.prefix().text(BBC.HELP_HEADER_CATEGORIES).newline();
                for (Map.Entry<String, Map<CommandMapping, String>> entry : categories.entrySet()) {
                    String s1 = Commands.getAlias(UtilityCommands.class, "/help") + " " + entry.getKey();
                    String s2 = entry.getValue().size() + "";
                    msg.text(BBC.HELP_ITEM_ALLOWED, "&a" + s1, s2);
                    msg.tooltip(StringMan.join(entry.getValue().keySet(), ", ", CommandMapping::getPrimaryAlias));
                    msg.command(s1);
                    msg.newline();
                }
                msg.text(BBC.HELP_FOOTER).link("https://git.io/vSKE5").newline();
                msg.paginate((prefix.equals("/") ? Commands.getAlias(UtilityCommands.class, "/help") : prefix), 0, 1);
                msg.send(actor);
            }

            @Override
            public void displayCommands(Map<CommandMapping, String> commandMap, String visited, int page, int pageTotal, int effectiveLength) {
                Message msg = new Message();
                msg.prefix().text(BBC.HELP_HEADER, page + 1, pageTotal).newline();

                CommandLocals locals = args.getLocals();

                if (!visited.isEmpty()) {
                    visited = visited + " ";
                }

                // Add each command
                for (Map.Entry<CommandMapping, String> cmdEntry : commandMap.entrySet()) {
                    CommandMapping mapping = cmdEntry.getKey();
                    String subPrefix = cmdEntry.getValue();

                    StringBuilder s1 = new StringBuilder();
                    s1.append(prefix);
                    s1.append(subPrefix);
                    CommandCallable c = mapping.getCallable();
                    s1.append(visited);
                    s1.append(mapping.getPrimaryAlias());
                    String s2 = mapping.getDescription().getDescription();
                    if (c.testPermission(locals)) {
                        msg.text(BBC.HELP_ITEM_ALLOWED, s1, s2);
                        String helpCmd = (prefix.equals("/") ? Commands.getAlias(UtilityCommands.class, "/help") + " " : "") + s1;
                        msg.cmdTip(helpCmd);
                        msg.newline();
                    } else {
                        msg.text(BBC.HELP_ITEM_DENIED, s1, s2).newline();
                    }
                }

                if (args.argsLength() == 0) {
                    msg.text(BBC.HELP_FOOTER).newline();
                }
                String baseCommand = (prefix.equals("/") ? Commands.getAlias(UtilityCommands.class, "/help") : prefix);
                if (effectiveLength > 0) baseCommand += " " + args.getString(0, effectiveLength - 1);
                msg.paginate(baseCommand, page + 1, pageTotal);

                msg.send(actor);
            }
        };

        builder.run();
    }
}
