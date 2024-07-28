package com.sk89q.worldedit.command;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweAPI;
import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.database.DBHandler;
import com.fastasyncworldedit.core.database.RollbackDatabase;
import com.fastasyncworldedit.core.history.RollbackOptimizedHistory;
import com.fastasyncworldedit.core.history.changeset.SimpleChangeSetSummary;
import com.fastasyncworldedit.core.regions.RegionWrapper;
import com.fastasyncworldedit.core.util.MainUtil;
import com.fastasyncworldedit.core.util.StringMan;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.argument.Arguments;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.command.util.annotation.Confirm;
import com.sk89q.worldedit.command.util.annotation.Time;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Identifiable;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.component.PaginationBox;
import com.sk89q.worldedit.util.formatting.component.TextComponentProducer;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import com.sk89q.worldedit.util.formatting.text.event.HoverEvent;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;
import org.jetbrains.annotations.Range;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static com.sk89q.worldedit.internal.command.CommandUtil.checkCommandArgument;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class HistorySubCommands {

    private final HistoryCommands parent;

    public HistorySubCommands(HistoryCommands parent) {
        this.parent = parent;
    }

    @Command(
            name = "restore",
            aliases = {"rerun"},
            desc = "Rerun edits"
                    + " - The time uses s, m, h, d, y.\n"
                    + " - Import from disk: /history import"
    )
    @CommandPermissions("worldedit.history.restore")
    @Confirm
    public synchronized void rerun(
            Player player, World world, RollbackDatabase database,
            @ArgFlag(name = 'u', desc = "String user", def = "me")
                    UUID other,
            @ArgFlag(name = 'r', def = "0", desc = "radius")
            @Range(from = 0, to = Integer.MAX_VALUE)
                    int radius,
            @ArgFlag(name = 't', desc = "Time e.g. 20s", def = "0")
            @Time
                    long timeDiff
    ) throws WorldEditException {
        rollback(player, world, database, other, radius, timeDiff, true);
    }

    @Command(
            name = "rollback",
            desc = "Undo a specific edit. "
                    + " - The time uses s, m, h, d, y."
    )
    @CommandPermissions("worldedit.history.rollback")
    @Confirm
    public synchronized void rollback(
            Player player, World world, RollbackDatabase database,
            @ArgFlag(name = 'u', desc = "String user", def = "")
                    UUID other,
            @ArgFlag(name = 'r', def = "0", desc = "radius")
            @Range(from = 0, to = Integer.MAX_VALUE)
                    int radius,
            @ArgFlag(name = 't', desc = "Time e.g. 20s", def = "0")
            @Time
                    long timeDiff,
            @Switch(name = 'f', desc = "Restore instead of rollback")
                    boolean restore
    ) throws WorldEditException {
        if (!Settings.settings().HISTORY.USE_DATABASE) {
            player.print(Caption.of("fawe.error.setting.disable", "history.use-database (Import with /history import )"));
            return;
        }
        checkCommandArgument(radius > 0, "Radius must be >= 0");
        checkCommandArgument(timeDiff > 0, "Time must be >= 0");

        if (other == null) {
            other = player.getUniqueId();
        }
        if (!other.equals(player.getUniqueId())) {
            player.checkPermission("worldedit.history.undo.other");
        }
        if (other == Identifiable.EVERYONE) {
            other = null;
        }
        Location origin = player.getLocation();
        BlockVector3 bot = origin.toBlockPoint().subtract(radius, radius, radius);
        BlockVector3 top = origin.toBlockPoint().add(radius, radius, radius);
        bot = bot.clampY(world.getMinY(), world.getMaxY());
        top = top.clampY(world.getMinY(), world.getMaxY());
        // TODO mask the regions bot / top to the bottom and top coord in the allowedRegions
        // TODO: then mask the edit to the bot / top
        //        if (allowedRegions.length != 1 || !allowedRegions[0].isGlobal()) {
        //            finalQueue = new MaskedIQueueExtent(SetQueue.IMP.getNewQueue(fp.getWorld(), true, false), allowedRegions);
        //        } else {
        //            finalQueue = SetQueue.IMP.getNewQueue(fp.getWorld(), true, false);
        //        }
        int count = 0;
        UUID finalOther = other;
        long minTime = System.currentTimeMillis() - timeDiff;
        for (Supplier<RollbackOptimizedHistory> supplier : database.getEdits(other, minTime, bot, top, !restore, restore)) {
            count++;
            RollbackOptimizedHistory edit = supplier.get();
            if (restore) {
                edit.redo(player);
            } else {
                edit.undo(player);
            }
            String path = edit.getWorld().getName() + "/" + finalOther + "-" + edit.getIndex();
            player.print(Caption.of("fawe.worldedit.rollback.rollback.element", path));
        }
        player.print(Caption.of("fawe.worldedit.tool.tool.inspect.info.footer", count));
    }

    @Command(
            name = "import",
            desc = "Import history into the database"
                    + " - The time uses s, m, h, d, y.\n"
                    + " - Import from disk: /history import"
    )
    @CommandPermissions("fawe.rollback.import")
    @Confirm
    public synchronized void importdb(Actor actor) throws WorldEditException {
        File folder = MainUtil.getFile(Fawe.platform().getDirectory(), Settings.settings().PATHS.HISTORY);
        if (folder.exists()) {
            for (File worldFolder : Objects.requireNonNull(folder.listFiles())) {
                if (worldFolder != null && worldFolder.isDirectory()) {
                    String worldName = worldFolder.getName();
                    World world = FaweAPI.getWorld(worldName);
                    if (world != null) {
                        for (File userFolder : worldFolder.listFiles()) {
                            if (!userFolder.isDirectory()) {
                                continue;
                            }
                            String userUUID = userFolder.getName();
                            try {
                                UUID uuid = UUID.fromString(userUUID);
                                for (File historyFile : userFolder.listFiles()) {
                                    String name = historyFile.getName();
                                    if (!name.endsWith(".bd")) {
                                        continue;
                                    }
                                    RollbackOptimizedHistory rollback = new RollbackOptimizedHistory(
                                            world, uuid,
                                            Integer.parseInt(
                                                    name.substring(0, name.length() - 3))
                                    );
                                    SimpleChangeSetSummary summary = rollback
                                            .summarize(RegionWrapper.GLOBAL(), false);
                                    if (summary != null) {
                                        rollback.setDimensions(
                                                BlockVector3.at(summary.minX, world.getMinY(), summary.minZ),
                                                BlockVector3
                                                        .at(summary.maxX, world.getMaxY(), summary.maxZ)
                                        );
                                        rollback.setTime(historyFile.lastModified());
                                        RollbackDatabase db = DBHandler.dbHandler().getDatabase(world);
                                        db.logEdit(rollback);
                                        actor.print(TextComponent.of("Logging: " + historyFile));
                                    }
                                }
                            } catch (IllegalArgumentException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            actor.print(TextComponent.of("Done import!"));
        }
    }

    @Command(
            name = "info",
            aliases = {"summary", "summarize"},
            desc = "Summarize an edit"
    )
    @CommandPermissions(
            value = "worldedit.history.info",
            queued = false
    )
    public synchronized void summary(
            Player player, RollbackDatabase database, Arguments arguments,
            @Arg(desc = "Player uuid/name")
                    UUID other,
            @Arg(desc = "edit index")
                    Integer index
    ) throws WorldEditException, ExecutionException, InterruptedException {
        RollbackOptimizedHistory edit = database.getEdit(other, index).get();
        if (edit == null) {
            player.print(Caption.of("fawe.worldedit.schematic.schematic.none"));
            return;
        }
        Location origin = player.getLocation();

        String name = Fawe.platform().getName(edit.getUUID());
        String cmd = edit.getCommand();
        BlockVector3 pos1 = edit.getMinimumPoint();
        BlockVector3 pos2 = edit.getMaximumPoint();

        double distanceX = Math.min(Math.abs(pos1.x() - origin.x()), Math.abs(pos2.x() - origin.x()));
        double distanceZ = Math.min(Math.abs(pos1.z() - origin.z()), Math.abs(pos2.z() - origin.z()));
        int distance = (int) Math.sqrt(distanceX * distanceX + distanceZ * distanceZ);

        BlockVector2 dirVec = BlockVector2.at(edit.getOriginX() - origin.x(), edit.getOriginZ() - origin.z());
        Direction direction = Direction.findClosest(dirVec.toVector3(), Direction.Flag.ALL);

        long seconds = (System.currentTimeMillis() - edit.getBDFile().lastModified()) / 1000;
        String timeStr = MainUtil.secToTime(seconds);

        long size = edit.longSize();
        boolean biomes = edit.getBioFile().exists();
        boolean createdEnts = edit.getEnttFile().exists();
        boolean removedEnts = edit.getEntfFile().exists();
        boolean createdTiles = edit.getNbttFile().exists();
        boolean removedTiles = edit.getNbtfFile().exists();

        TranslatableComponent header = Caption.of(
                "fawe.worldedit.history.find.element",
                name,
                timeStr,
                distance,
                direction.name(),
                cmd
        );

        String sizeStr = StringMan.humanReadableByteCountBin(edit.getSizeOnDisk());
        String extra = "";
        if (biomes) {
            extra += "biomes, ";
        }
        if (createdEnts) {
            extra += "+entity, ";
        }
        if (removedEnts) {
            extra += "-entity, ";
        }
        if (createdTiles) {
            extra += "+tile, ";
        }
        if (removedTiles) {
            extra += "-tile, ";
        }

        TranslatableComponent body = Caption.of(
                "fawe.worldedit.history.find.element.more",
                size,
                edit.getMinimumPoint(),
                edit.getMaximumPoint(),
                extra.trim(),
                sizeStr
        );
        Component distr = TextComponent
                .of("/history distr")
                .clickEvent(ClickEvent.suggestCommand("//history distr " + other + " " + index));
        TextComponentProducer content = new TextComponentProducer().append(header).newline().append(body).newline().append(distr);
        player.print(content.create());
    }

    private PaginationBox list(
            RollbackDatabase database,
            String pageCommand,
            List<Supplier<? extends ChangeSet>> histories,
            BlockVector3 origin
    ) {
        return PaginationBox.fromStrings("Edits:", pageCommand, histories, new Function<>() {
                    @Nonnull
                    @Override
                    public Component apply(@Nullable Supplier<? extends ChangeSet> input) {
                        ChangeSet edit = input.get();

                        if (edit instanceof RollbackOptimizedHistory rollback) {

                            UUID uuid = rollback.getUUID();
                            int index = rollback.getIndex();
                            String name = Fawe.platform().getName(rollback.getUUID());

                            String cmd = rollback.getCommand();
                            BlockVector3 pos1 = rollback.getMinimumPoint();
                            BlockVector3 pos2 = rollback.getMaximumPoint();

                            double distanceX = Math.min(Math.abs(pos1.x() - origin.x()), Math.abs(pos2.x() - origin.x()));
                            double distanceZ = Math.min(Math.abs(pos1.z() - origin.z()), Math.abs(pos2.z() - origin.z()));
                            int distance = (int) Math.sqrt(distanceX * distanceX + distanceZ * distanceZ);

                            BlockVector2 dirVec = BlockVector2.at(
                                    rollback.getOriginX() - origin.x(),
                                    rollback.getOriginZ() - origin.z()
                            );
                            Direction direction = Direction.findClosest(dirVec.toVector3(), Direction.Flag.ALL);

                            long seconds = (System.currentTimeMillis() - rollback.getBDFile().lastModified()) / 1000;
                            String timeStr = MainUtil.secToTime(seconds);

                            long size = edit.longSize();

                            TranslatableComponent elem = Caption.of(
                                    "fawe.worldedit.history.find.element",
                                    name,
                                    timeStr,
                                    distance,
                                    direction.name(),
                                    cmd
                            );

                            String infoCmd = "//history summary " + uuid + " " + index;
                            TranslatableComponent hover = Caption.of("fawe.worldedit.history.find.hover", size);
                            elem = elem.hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, hover));
                            elem = elem.clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND, infoCmd));
                            return elem;
                        } else {
                            // TODO
                            return TextComponent.empty();
                        }
                    }
                }
        );
    }

    @Command(
            name = "find",
            aliases = {"inspect", "search", "near"},
            desc = "Find nearby edits"
    )
    @CommandPermissions(
            value = "worldedit.history.find",
            queued = false
    )
    public synchronized void find(
            Player player, World world, RollbackDatabase database, Arguments arguments,
            @ArgFlag(name = 'u', def = "", desc = "String user")
                    UUID other,
            @ArgFlag(name = 'r', def = "0", desc = "radius")
            @Range(from = 0, to = Integer.MAX_VALUE)
                    Integer radius,
            @ArgFlag(name = 't', desc = "Time e.g. 20s", def = "0")
            @Time
                    Long timeDiff,
            @ArgFlag(name = 'p', desc = "Page to view.", def = "")
                    Integer page
    ) throws WorldEditException {
        if (!Settings.settings().HISTORY.USE_DATABASE) {
            player.print(Caption.of("fawe.error.setting.disable", "history.use-database (Import with //history import )"));
            return;
        }
        if (other == null && radius == 0 && timeDiff == 0) {
            throw new InsufficientArgumentsException(Caption.of("fawe.error.invalid-user"));
        }
        checkCommandArgument(radius > 0, Caption.of("fawe.error.radius-too-small"));
        checkCommandArgument(timeDiff > 0, Caption.of("fawe.error.time-too-less"));

        Location origin = player.getLocation();
        String pageCommand = "/" + arguments.get().replaceAll("-p [0-9]+", "").trim();
        Reference<List<Supplier<? extends ChangeSet>>> cached = player.getMeta(pageCommand);
        List<Supplier<? extends ChangeSet>> history = cached == null ? null : cached.get();

        if (page == null || history == null) {
            if (other == null) {
                other = player.getUniqueId();
            }
            if (!other.equals(player.getUniqueId())) {
                player.checkPermission("worldedit.history.undo.other");
            }
            if (other == Identifiable.EVERYONE) {
                other = null;
            }

            BlockVector3 bot = origin.toBlockPoint().subtract(radius, radius, radius);
            BlockVector3 top = origin.toBlockPoint().add(radius, radius, radius);
            bot = bot.clampY(world.getMinY(), world.getMaxY());
            top = top.clampY(world.getMinY(), world.getMaxY());

            long minTime = System.currentTimeMillis() - timeDiff;
            Iterable<Supplier<RollbackOptimizedHistory>> edits = database.getEdits(other, minTime, bot, top, false, false);
            history = Lists.newArrayList(edits);
            player.setMeta(pageCommand, new SoftReference<>(history));
            page = 1;
        }

        PaginationBox box = list(database, pageCommand, history, origin.toBlockPoint());
        player.print(box.create(page));
    }

    @Command(
            name = "distr",
            aliases = {"distribution"},
            desc = "View block distribution for an edit"
    )
    @CommandPermissions(
            value = "worldedit.history.distr",
            queued = false
    )
    public void distr(
            Player player, LocalSession session, RollbackDatabase database, Arguments arguments,
            @Arg(desc = "Player uuid/name")
                    UUID other,
            @Arg(desc = "edit index")
                    Integer index,
            @ArgFlag(name = 'p', desc = "Page to view.", def = "")
                    Integer page
    ) throws ExecutionException, InterruptedException {
        String pageCommand = "/" + arguments.get().replaceAll("-p [0-9]+", "").trim();
        Reference<PaginationBox> cached = player.getMeta(pageCommand);
        PaginationBox pages = cached == null ? null : cached.get();
        if (page == null || pages == null) {
            RollbackOptimizedHistory edit = database.getEdit(other, index).get();
            SimpleChangeSetSummary summary = edit.summarize(RegionWrapper.GLOBAL(), false);
            if (summary != null) {
                List<Countable<BlockState>> distr = summary.getBlockDistributionWithData();
                SelectionCommands.BlockDistributionResult distrPages = new SelectionCommands.BlockDistributionResult(
                        distr,
                        true,
                        pageCommand
                );
                pages = new PaginationBox.MergedPaginationBox("Block Distribution", pageCommand, pages, distrPages);
                player.setMeta(pageCommand, new SoftReference<>(pages));
            }
            page = 1;
        }
        if (pages == null) {
            player.print(Caption.of("fawe.worldedit.history.distr.summary_null"));
        } else {
            player.print(pages.create(page));
        }
    }

    @Command(
            name = "list",
            desc = "List your history"
    )
    @CommandPermissions(
            value = "worldedit.history.list",
            queued = false
    )
    public void list(
            Player player, LocalSession session, RollbackDatabase database, Arguments arguments,
            @Arg(desc = "Player uuid/name")
                    UUID other,
            @ArgFlag(name = 'p', desc = "Page to view.", def = "")
                    Integer page
    ) {
        List<Supplier<? extends ChangeSet>> history = Lists.transform(
                session.getHistory(),
                (Function<ChangeSet, Supplier<ChangeSet>>) input -> () -> input
        );
        Location origin = player.getLocation();
        String pageCommand = "/" + arguments.get().replaceAll("-p [0-9]+", "").trim();
        Reference<PaginationBox> cached = player.getMeta(pageCommand);
        PaginationBox pages = cached == null ? null : cached.get();
        if (page == null || pages == null) {
            pages = list(database, pageCommand, history, origin.toBlockPoint());
            page = 1;
        }
        player.print(pages.create(page));
    }

    @Command(
            name = "clear",
            desc = "Clear your history"
    )
    @CommandPermissions("worldedit.history.clear")
    public void clearHistory(Actor actor, LocalSession session) {
        parent.clearHistory(actor, session);
    }

}
