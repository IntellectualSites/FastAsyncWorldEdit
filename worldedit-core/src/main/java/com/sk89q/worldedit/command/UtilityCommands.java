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
import com.boydti.fawe.util.gui.FormBuilder;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.command.util.CreatureButcher;
import com.sk89q.worldedit.command.util.EntityRemover;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.factory.DefaultMaskParser;
import com.sk89q.worldedit.extension.factory.DefaultTransformParser;
import com.sk89q.worldedit.extension.factory.HashTagPatternParser;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.EntityVisitor;
import com.sk89q.worldedit.internal.annotation.Direction;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.expression.runtime.EvaluationException;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.command.*;
import com.sk89q.worldedit.util.command.binding.Range;
import com.sk89q.worldedit.util.command.binding.Text;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.util.command.parametric.ParameterData;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


import static com.sk89q.minecraft.util.commands.Logging.LogMode.PLACEMENT;

/**
 * Utility commands.
 */
@Command(aliases = {}, desc = "Various utility commands: [More Info](http://wiki.sk89q.com/wiki/WorldEdit/Utilities)")
public class UtilityCommands extends MethodCommands {
    public UtilityCommands(WorldEdit we) {
        super(we);
    }

    @Command(
            aliases = {"patterns"},
            usage = "[page=1|search|pattern]",
            desc = "View help about patterns",
            help = "Patterns determine what blocks are placed\n" +
                    " - Use [brackets] for arguments\n" +
                    " - Use , to OR multiple\n" +
                    "e.g. #surfacespread[10][#existing],andesite\n" +
                    "More Info: https://git.io/vSPmA"
    )
    public void patterns(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        displayModifierHelp(player, HashTagPatternParser.class, args);
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
                    "More Info: https://git.io/v9r4K"
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
                    "More Info: https://git.io/v9KHO"
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
                UtilityCommands.help(args, worldEdit, player, getCommand().aliases()[0] + " ", parser.getDispatcher());
            }
        }
    }

    @Command(
            aliases = {"/fill"},
            usage = "<pattern> <radius> [depth] [direction]",
            desc = "Fill a hole",
            min = 2,
            max = 4
    )
    @CommandPermissions("worldedit.fill")
    @Logging(PLACEMENT)
    public void fill(Player player, LocalSession session, EditSession editSession, Pattern pattern, double radius, @Optional("1") double depth, @Optional("down") @Direction Vector direction) throws WorldEditException {
        worldEdit.checkMaxRadius(radius);
        Vector pos = session.getPlacementPosition(player);
        int affected;
        affected = editSession.fillDirection(pos, pattern, radius, (int) depth, direction);
        player.print(BBC.getPrefix() + affected + " block(s) have been created.");
    }

    @Command(
            aliases = {"/fillr"},
            usage = "<pattern> <radius> [depth]",
            desc = "Fill a hole recursively",
            min = 2,
            max = 3
    )
    @CommandPermissions("worldedit.fill.recursive")
    @Logging(PLACEMENT)
    public void fillr(Player player, LocalSession session, EditSession editSession, Pattern pattern, double radius, @Optional("-1") double depth) throws WorldEditException {
        worldEdit.checkMaxRadius(radius);
        Vector pos = session.getPlacementPosition(player);
        if (depth == -1) depth = Integer.MAX_VALUE;
        int affected = editSession.fillXZ(pos, pattern, radius, (int) depth, true);
        player.print(BBC.getPrefix() + affected + " block(s) have been created.");
    }

    @Command(
            aliases = {"/drain"},
            usage = "<radius>",
            desc = "Drain a pool",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.drain")
    @Logging(PLACEMENT)
    public void drain(Player player, LocalSession session, EditSession editSession, double radius) throws WorldEditException {
        worldEdit.checkMaxRadius(radius);
        int affected = editSession.drainArea(
                session.getPlacementPosition(player), radius);
        player.print(BBC.getPrefix() + affected + " block(s) have been changed.");
    }

    @Command(
            aliases = {"/fixlava", "fixlava"},
            usage = "<radius>",
            desc = "Fix lava to be stationary",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.fixlava")
    @Logging(PLACEMENT)
    public void fixLava(Player player, LocalSession session, EditSession editSession, double radius) throws WorldEditException {
        worldEdit.checkMaxRadius(radius);
        int affected = editSession.fixLiquid(
                session.getPlacementPosition(player), radius, BlockTypes.LAVA.toMask(editSession));
        player.print(BBC.getPrefix() + affected + " block(s) have been changed.");
    }

    @Command(
            aliases = {"/fixwater", "fixwater"},
            usage = "<radius>",
            desc = "Fix water to be stationary",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.fixwater")
    @Logging(PLACEMENT)
    public void fixWater(Player player, LocalSession session, EditSession editSession, double radius) throws WorldEditException {
        worldEdit.checkMaxRadius(radius);
        int affected = editSession.fixLiquid(
                session.getPlacementPosition(player), radius, BlockTypes.WATER.toMask(editSession));
        player.print(BBC.getPrefix() + affected + " block(s) have been changed.");
    }

    @Command(
            aliases = {"/removeabove", "removeabove"},
            usage = "[size] [height]",
            desc = "Remove blocks above your head.",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.removeabove")
    @Logging(PLACEMENT)
    public void removeAbove(Player player, LocalSession session, EditSession editSession, @Optional("1") double size, @Optional("256") double height) throws WorldEditException {
        worldEdit.checkMaxRadius(size);
        int affected = editSession.removeAbove(session.getPlacementPosition(player), (int) size, (int) height);
        player.print(BBC.getPrefix() + affected + " block(s) have been removed.");
    }

    @Command(
            aliases = {"/removebelow", "removebelow"},
            usage = "[size] [height]",
            desc = "Remove blocks below you.",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.removebelow")
    @Logging(PLACEMENT)
    public void removeBelow(Player player, LocalSession session, EditSession editSession, @Optional("1") double size, @Optional("256") double height) throws WorldEditException {
        worldEdit.checkMaxRadius(size);
        int affected = editSession.removeBelow(session.getPlacementPosition(player), (int) size, (int) height);
        player.print(BBC.getPrefix() + affected + " block(s) have been removed.");
    }

    @Command(
            aliases = {"/removenear", "removenear"},
            usage = "<block> [size]",
            desc = "Remove blocks near you.",
            min = 1,
            max = 2
    )
    @CommandPermissions("worldedit.removenear")
    @Logging(PLACEMENT)
    public void removeNear(Player player, LocalSession session, EditSession editSession, Mask mask, @Optional("50") double size) throws WorldEditException {
        worldEdit.checkMaxRadius(size);
        int affected = editSession.removeNear(session.getPlacementPosition(player), mask, (int) size);
        player.print(BBC.getPrefix() + affected + " block(s) have been removed.");
    }

    @Command(
            aliases = {"/replacenear", "replacenear"},
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
        Vector base = session.getPlacementPosition(player);
        Vector min = base.subtract(size, size, size);
        Vector max = base.add(size, size, size);
        Region region = new CuboidRegion(player.getWorld(), min, max);

        int affected = editSession.replaceBlocks(region, from, to);
        BBC.VISITOR_BLOCK.send(player, affected);
    }

    @Command(
            aliases = {"/snow", "snow"},
            usage = "[radius]",
            desc = "Simulates snow",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.snow")
    @Logging(PLACEMENT)
    public void snow(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        double size = args.argsLength() > 0 ? Math.max(1, args.getDouble(0)) : 10;

        int affected = editSession.simulateSnow(session.getPlacementPosition(player), size);
        player.print(BBC.getPrefix() + affected + " surfaces covered. Let it snow~");
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

        int affected = editSession.thaw(session.getPlacementPosition(player), size);
        player.print(BBC.getPrefix() + affected + " surfaces thawed.");
    }

    @Command(
            aliases = {"/green", "green"},
            usage = "[radius]",
            desc = "Greens the area",
            flags = "f",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.green")
    @Logging(PLACEMENT)
    public void green(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        final double size = args.argsLength() > 0 ? Math.max(1, args.getDouble(0)) : 10;
        final boolean onlyNormalDirt = !args.hasFlag('f');

        final int affected = editSession.green(session.getPlacementPosition(player), size);
        player.print(BBC.getPrefix() + affected + " surfaces greened.");
    }

    @Command(
            aliases = {"/ex", "/ext", "/extinguish", "ex", "ext", "extinguish"},
            usage = "[radius]",
            desc = "Extinguish nearby fire",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.extinguish")
    @Logging(PLACEMENT)
    public void extinguish(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        LocalConfiguration config = worldEdit.getConfiguration();

        int defaultRadius = config.maxRadius != -1 ? Math.min(40, config.maxRadius) : 40;
        int size = args.argsLength() > 0 ? Math.max(1, args.getInteger(0))
                : defaultRadius;
        worldEdit.checkMaxRadius(size);

        int affected = editSession.removeNear(session.getPlacementPosition(player), BlockTypes.FIRE.toMask(editSession), size);
        player.print(BBC.getPrefix() + affected + " block(s) have been removed.");
    }

    @Command(
            aliases = {"butcher"},
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
        LocalConfiguration config = worldEdit.getConfiguration();
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

        List<EntityVisitor> visitors = new ArrayList<EntityVisitor>();
        LocalSession session = null;
        EditSession editSession = null;

        if (player != null) {
            session = worldEdit.getSessionManager().get(player);
            Vector center = session.getPlacementPosition(player);
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
            Platform platform = worldEdit.getPlatformManager().queryCapability(Capability.WORLD_EDITING);
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
            editSession.flushQueue();
        }
    }

    @Command(
            aliases = {"remove", "rem", "rement"},
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

        List<EntityVisitor> visitors = new ArrayList<EntityVisitor>();
        LocalSession session = null;
        EditSession editSession = null;

        if (player != null) {
            session = worldEdit.getSessionManager().get(player);
            Vector center = session.getPlacementPosition(player);
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
            Platform platform = worldEdit.getPlatformManager().queryCapability(Capability.WORLD_EDITING);
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
            editSession.flushQueue();
        }
    }

    @Command(
            aliases = {"/calc", "/calculate", "/eval", "/evaluate", "/solve"},
            usage = "<expression>",
            desc = "Evaluate a mathematical expression"
    )
    @CommandPermissions("worldedit.calc")
    public void calc(final Actor actor, @Text String input) throws CommandException {
        try {
            FaweLimit limit = FawePlayer.wrap(actor).getLimit();
            final Expression expression = Expression.compile(input);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Double> futureResult = executor.submit(new Callable<Double>() {
                @Override
                public Double call() throws Exception {

                    return expression.evaluate();
                }
            });

            Double result = Double.NaN;
            try {
                result = futureResult.get(limit.MAX_EXPRESSION_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                futureResult.cancel(true);
                e.printStackTrace();
            }

            executor.shutdownNow();
            actor.print(BBC.getPrefix() + "= " + result);
        } catch (EvaluationException e) {
            actor.printError(String.format(
                    "'%s' could not be parsed as a valid expression", input));
        } catch (ExpressionException e) {
            actor.printError(String.format(
                    "'%s' could not be evaluated (error: %s)", input, e.getMessage()));
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
            aliases = {"/help"},
            usage = "[<command>]",
            desc = "Displays help for WorldEdit commands",
            min = 0,
            max = -1
    )
    public void help(Actor actor, CommandContext args) throws WorldEditException {
        help(args, worldEdit, actor);
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
        page = getFiles(dir, actor, args, page, perPage, formatName, playerFolder, file -> fileList.add(file));

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
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                boolean dir1 = f1.isDirectory();
                boolean dir2 = f2.isDirectory();
                if (dir1 != dir2) return dir1 ? -1 : 1;
                int res;
                if (sortType == 0) { // use name by default
                    int p = f1.getParent().compareTo(f2.getParent());
                    if (p == 0) { // same parent, compare names
                        res = f1.getName().compareTo(f2.getName());
                    } else { // different parent, sort by that
                        res = p;
                    }
                } else {
                    res = Long.valueOf(f1.lastModified()).compareTo(f2.lastModified()); // use date if there is a flag
                    if (sortType == 1) res = -res; // flip date for newest first instead of oldest first
                }
                return res;
            }
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

        FileFilter ignoreUUIDs = f -> {
            try {
                if (f.isDirectory()) {
                    UUID uuid = UUID.fromString(f.getName());
                    return false;
                }
            } catch (IllegalArgumentException exception) {}
            return true;
        };

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
            final ClipboardFormat cf = ClipboardFormat.findByAlias(formatName);
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

        ClipboardFormat format = ClipboardFormat.findByFile(file);
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
        help(args, we, actor, "/", null);
    }

    @Command(
            aliases = {"/gui"},
            desc = "Open the GUI"
    )
    @Logging(PLACEMENT)
    public void gui(Actor actor, FawePlayer fp, LocalSession session, CommandContext args) throws WorldEditException, CommandException {
        FormBuilder gui = Fawe.imp().getFormBuilder();
        if (gui == null) throw new CommandException("Only supported on Pocket Edition");

        Dispatcher callable = worldEdit.getPlatformManager().getCommandManager().getDispatcher();
        CommandLocals locals = args.getLocals();

        String prefix = Commands.getAlias(UtilityCommands.class, "/gui");

        // TODO sort commands by most used

        new HelpBuilder(callable, args, prefix, Integer.MAX_VALUE) {
            @Override
            public void displayFailure(String message) {
                gui.setTitle("Error");
                gui.addLabel(message);
            }

            @Override
            public void displayUsage(CommandCallable callable, String commandString) {
                gui.setTitle(commandString);

                if (callable instanceof Dispatcher) {
                    Dispatcher dispathcer = (Dispatcher) callable;
                    dispathcer.getCommands();
                    gui.addLabel("Dispatcher not implemented for " + commandString);
                } else {
                    Description cmdDesc = callable.getDescription();



                    List<Parameter> params = cmdDesc.getParameters();
                    String[] suggested = new String[params.size()];
                    if (cmdDesc.getUsage() != null) {
                        String[] usageArgs = cmdDesc.getUsage().split(" ", params.size());
                        for (int i = 0; i < usageArgs.length; i++) {
                            String arg = usageArgs[i];
                            String[] splitSug = arg.split("=");
                            if (splitSug.length == 2) {
                                suggested[i] = splitSug[1];
                            }
                        }
                    }
                    for (int i = 0 ; i < params.size(); i++) {
                        String[] def = params.get(i).getDefaultValue();
                        if (def != null && def.length != 0) {
                            suggested[i] = def[0];
                        }
                    }

                    String help = cmdDesc.getHelp();
                    if (help == null || help.isEmpty()) help = cmdDesc.getDescription();

                    gui.addLabel(BBC.color("&2" + help + "\n"));

                    List<String> flags = new ArrayList<>();

                    for (int i = 0; i < params.size(); i++) {
                        Parameter param = params.get(i);
                        String name = param.getName();
                        boolean optional = param.isValueFlag() || param.isOptional();
                        String[] def = param.getDefaultValue();

                        if (param.getFlag() != null) {
                            flags.add("-" + param.getFlag() + " ");
                        } else {
                            flags.add("");
                        }

                        if (param instanceof ParameterData) {
                            ParameterData pd = (ParameterData) param;
                            Type type = pd.getType();
                            String suggestion = suggested[i];

                            String color = optional ? "3" : "c";
                            StringBuilder label = new StringBuilder(BBC.color("&" + color + name + ": "));
//                            if (suggested[i] != null) label.append(" e.g. " + suggestion);
                            Range range = MainUtil.getOf(pd.getModifiers(), Range.class);
                            double min = 0;
                            double max = 100;
                            if (range != null) {
                                min = range.min();
                                max = range.max();
                            } else {
                                SuggestedRange suggestedRange = MainUtil.getOf(pd.getModifiers(), SuggestedRange.class);
                                if (suggestedRange != null) {
                                    min = suggestedRange.min();
                                    max = suggestedRange.max();
                                } else  if (name.equalsIgnoreCase("radius") || name.equalsIgnoreCase("size")) {
                                    max = WorldEdit.getInstance().getConfiguration().maxBrushRadius;
                                }
                            }
                            int step = 1;
                            Step stepSizeAnn = MainUtil.getOf(pd.getModifiers(), Step.class);
                            if (stepSizeAnn != null) {
                                double stepVal = stepSizeAnn.value();
                                step = Math.max(1, (int) stepVal);
                            }
                            /*
                            BaseBiome
                            Vector
                            Vector2D
                             */

                            switch (type.getTypeName()) {
                                case "double":
                                case "java.lang.Double": {
                                    double value = suggestion != null ? Double.parseDouble(suggestion) : min;
                                    gui.addSlider("\n" + label.toString(), min, max, 1, value);
                                    break;
                                }
                                case "int":
                                case "java.lang.Integer": {
                                    int value = suggestion != null ? Integer.parseInt(suggestion) : (int) min;
                                    gui.addSlider("\n" + label.toString(), min, max, 1, value);
                                    break;
                                }
                                case "boolean":
                                case "java.lang.Boolean": {
                                    boolean value = suggestion != null ? Boolean.parseBoolean(suggestion) : false;
                                    gui.addToggle(label.toString(), value);
                                    break;
                                }
                                case "com.sk89q.worldedit.patterns.Pattern": {
                                    gui.addInput("\n" + label.toString(), "stone", "wood");
                                    break;
                                }
                                case "com.sk89q.worldedit.blocks.BaseBlock": {
                                    gui.addInput("\n" + label.toString(), "stone", "wood");
                                    break;
                                }
                                case "com.sk89q.worldedit.function.mask.Mask": {
                                    gui.addInput("\n" + label.toString(), "stone", "wood");
                                    break;
                                }
                                default:
                                case "java.lang.String": {
                                    // TODO
                                    // clipboard
                                    // schematic
                                    // image
                                    if (suggestion == null) suggestion = "";
                                    gui.addInput("\n" + label.toString(), suggestion, suggestion);
                                    break;
                                }
                            }
                        } else {
                            throw new UnsupportedOperationException("Unsupported callable: " + callable.getClass() + " | " + param.getClass());
                        }
                    }

                    gui.setResponder(new Consumer<Map<Integer, Object>>() {
                        @Override
                        public void accept(Map<Integer, Object> response) {
                            int index = 0;
                            StringBuilder command = new StringBuilder(commandString);
                            for (Map.Entry<Integer, Object> arg : response.entrySet()) {
                                String argValue = arg.getValue().toString();
                                String flag = flags.get(index);
                                if (!flag.isEmpty()) {
                                    if (argValue.equalsIgnoreCase("false")) continue;
                                    if (argValue.equalsIgnoreCase("true")) argValue = "";
                                }
                                command.append(" " + flag + argValue);
                                index++;
                            }
                            CommandEvent event = new CommandEvent(actor, command.toString());
                            CommandManager.getInstance().handleCommand(event);
                        }
                    });
                }
            }

            @Override
            public void displayCategories(Map<String, Map<CommandMapping, String>> categories) {
                gui.setTitle(BBC.HELP_HEADER_CATEGORIES.s());
                List<String> categoryList = new ArrayList<>();
                for (Map.Entry<String, Map<CommandMapping, String>> categoryEntry : categories.entrySet()) {
                    String category = categoryEntry.getKey();
                    categoryList.add(category);
                    Map<CommandMapping, String> commandMap = categoryEntry.getValue();
                    int size = commandMap.size();

                    String plural = size == 1 ? "command" : "commands";
                    gui.addButton(BBC.HELP_ITEM_ALLOWED.f(category, "(" + size + " " + plural + ")"), null);
                }

                gui.setResponder(new Consumer<Map<Integer, Object>>() {
                    @Override
                    public void accept(Map<Integer, Object> response) {
                        if (response.isEmpty()) {
                            // ??
                            throw new IllegalArgumentException("No response for categories");
                        } else {
                            Map.Entry<Integer, Object> clicked = response.entrySet().iterator().next();
                            String category = categoryList.get(clicked.getKey());
                            String arguments = prefix + " " + category;
                            CommandEvent event = new CommandEvent(actor, arguments);
                            CommandManager.getInstance().handleCommand(event);
                        }
                    }
                });
            }

            @Override
            public void displayCommands(Map<CommandMapping, String> commandMap, String visited, int page, int pageTotal, int effectiveLength) {
                gui.setTitle(BBC.HELP_HEADER_SUBCOMMANDS.s());

                String baseCommand = prefix;
                if (effectiveLength > 0) baseCommand += " " + args.getString(0, effectiveLength - 1);

                CommandLocals locals = args.getLocals();
                if (!visited.isEmpty()) {
                    visited = visited + " ";
                }

                List<String> commands = new ArrayList<>();

                for (Map.Entry<CommandMapping, String> cmdEntry : commandMap.entrySet()) {
                    CommandMapping mapping = cmdEntry.getKey();
                    String subPrefix = cmdEntry.getValue();

                    StringBuilder helpCmd = new StringBuilder();
                    helpCmd.append(prefix);
                    helpCmd.append(" ");
                    helpCmd.append(subPrefix);
                    CommandCallable c = mapping.getCallable();
                    helpCmd.append(visited);
                    helpCmd.append(mapping.getPrimaryAlias());
                    String s2 = mapping.getDescription().getDescription();
                    if (c.testPermission(locals)) {
//                        gui.addLabel(s2);
                        gui.addButton(helpCmd.toString(), null);
                        commands.add(helpCmd.toString());
                    }
                }

                gui.setResponder(new Consumer<Map<Integer, Object>>() {
                    @Override
                    public void accept(Map<Integer, Object> response) {
                        if (response.isEmpty()) {
                            // ??
                            throw new IllegalArgumentException("No response for command list: " + prefix);
                        } else {
                            Map.Entry<Integer, Object> clicked = response.entrySet().iterator().next();
                            int index = clicked.getKey();
                            String cmd = commands.get(index);
                            CommandEvent event = new CommandEvent(actor, cmd);
                            CommandManager.getInstance().handleCommand(event);
                        }
                    }
                });
            }
        }.run();

        gui.display(fp);
    }

    public static void help(CommandContext args, WorldEdit we, Actor actor, String prefix, CommandCallable callable) {
        final int perPage = actor instanceof Player ? 12 : 20; // More pages for console

        HelpBuilder builder = new HelpBuilder(callable, args, prefix, perPage) {
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
                boolean first = true;
                for (Map.Entry<String, Map<CommandMapping, String>> entry : categories.entrySet()) {
                    String s1 = Commands.getAlias(UtilityCommands.class, "/help") + " " + entry.getKey();
                    String s2 = entry.getValue().size() + "";
                    msg.text(BBC.HELP_ITEM_ALLOWED, "&a" + s1, s2);
                    msg.tooltip(StringMan.join(entry.getValue().keySet(), ", ", cm -> cm.getPrimaryAlias()));
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

    public static Class<UtilityCommands> inject() {
        return UtilityCommands.class;
    }
}
