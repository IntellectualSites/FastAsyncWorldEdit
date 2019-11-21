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
import com.boydti.fawe.config.BBC;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.database.DBHandler;
import com.boydti.fawe.database.RollbackDatabase;
import com.boydti.fawe.logging.rollback.RollbackOptimizedHistory;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.command.util.annotation.Confirm;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.Switch;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.jetbrains.annotations.Range;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Commands to undo, redo, and clear history.
 */
@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class HistoryCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public HistoryCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
        name = "fawerollback",
            aliases = {"frb", "/fawerollback", "/rollback"},
            desc = "Undo a specific edit. " +
                   " - The time uses s, m, h, d, y.\n" +
                   " - Import from disk: /frb #import"
    )
    @CommandPermissions("worldedit.history.rollback")
    public void faweRollback(Player player, LocalSession session, @Arg(desc = "String user") String user, @Arg(def = "0", desc = "radius") @Range(from = 0, to=Integer.MAX_VALUE) int radius, @Arg(name = "time", desc = "String", def = "0") String time, @Switch(name = 'r', desc = "TODO") boolean restore) throws WorldEditException {
        if (!Settings.IMP.HISTORY.USE_DATABASE) {
            player.print(TranslatableComponent.of("fawe.error.setting.disable" , "history.use-database (Import with /frb #import )"));
            return;
        }
        if (user.charAt(0) == '#') {
            if (user.equals("#import")) {
                if (!player.hasPermission("fawe.rollback.import")) {
                    player.print(TranslatableComponent.of("fawe.error.no.perm", "fawe.rollback.import"));
                    return;
                }
                File folder = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HISTORY);
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
                                                    name.substring(0, name.length() - 3)));
                                            DiskStorageHistory.DiskStorageSummary summary = rollback
                                                .summarize(RegionWrapper.GLOBAL(), false);
                                            if (summary != null) {
                                                rollback.setDimensions(
                                                    BlockVector3.at(summary.minX, 0, summary.minZ),
                                                    BlockVector3
                                                        .at(summary.maxX, 255, summary.maxZ));
                                                rollback.setTime(historyFile.lastModified());
                                                RollbackDatabase db = DBHandler.IMP
                                                    .getDatabase(world);
                                                db.logEdit(rollback);
                                                player.print("Logging: " + historyFile);
                                            }
                                        }
                                    } catch (IllegalArgumentException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                    player.print("Done import!");
                }
                return;
            }
            String toParse = user.substring(1);
            if (!MathMan.isInteger(toParse)) {
                player.print(TranslatableComponent.of("fawe.error.command.syntax" , "/frb #<index>"));
                return;
            }
            int index = Integer.parseInt(toParse);
            final World world = player.getWorld();
            UUID uuid = player.getUniqueId();
            DiskStorageHistory file = new DiskStorageHistory(world, uuid, index);
            if (file.getBDFile().exists()) {
                if (restore) file.redo(player);
                else file.undo(player);
                player.print(TranslatableComponent.of("fawe.worldedit.rollback.rollback.element" , world.getName() + "/" + user + "-" + index));
            } else {
                player.print(TranslatableComponent.of("fawe.worldedit.tool.tool.inspect.info.footer" , 0));
            }
            return;
        }
        UUID other = Fawe.imp().getUUID(user);
        if (other == null) {
            player.print(TranslatableComponent.of("fawe.error.player.not.found" , user));
            return;
        }
        if (radius == 0) {
            player.print(TranslatableComponent.of("fawe.error.command.syntax" , "/frb " + user + " <radius> <time>"));
            return;
        }
        long timeDiff = MainUtil.timeToSec(time) * 1000;
        if (timeDiff == 0) {
            player.print(TranslatableComponent.of("fawe.error.command.syntax" , "/frb " + user + " " + radius + " <time>"));
            return;
        }
        radius = Math.max(Math.min(500, radius), 0);
        final World world = player.getWorld();
        Location origin = player.getLocation();
        BlockVector3 bot = origin.toBlockPoint().subtract(radius, radius, radius);
        bot = bot.withY(Math.max(0, bot.getY()));
        BlockVector3 top = origin.toBlockPoint().add(radius, radius, radius);
        bot = bot.withY(Math.min(255, top.getY()));
        RollbackDatabase database = DBHandler.IMP.getDatabase(world);
        final AtomicInteger count = new AtomicInteger();

        Region[] allowedRegions = player.getCurrentRegions(FaweMaskManager.MaskType.OWNER);
        if (allowedRegions == null) {
            player.printError(TranslatableComponent.of("fawe.error.no.region"));
            return;
        }
        // TODO mask the regions bot / top to the bottom and top coord in the allowedRegions
        // TODO: then mask the edit to the bot / top
//        if (allowedRegions.length != 1 || !allowedRegions[0].isGlobal()) {
//            finalQueue = new MaskedIQueueExtent(SetQueue.IMP.getNewQueue(fp.getWorld(), true, false), allowedRegions);
//        } else {
//            finalQueue = SetQueue.IMP.getNewQueue(fp.getWorld(), true, false);
//        }
        database.getPotentialEdits(other, System.currentTimeMillis() - timeDiff, bot, top, new RunnableVal<DiskStorageHistory>() {
            @Override
            public void run(DiskStorageHistory edit) {
                edit.undo(player, allowedRegions);
                player.print(TranslatableComponent.of("fawe.worldedit.rollback.rollback.element" , edit.getWorld().getName() + "/" + user + "-" + edit.getIndex()));
                count.incrementAndGet();
            }
        }, () -> player.print(TranslatableComponent.of("fawe.worldedit.tool.tool.inspect.info.footer" , count)), true, restore);
    }

    @Command(
            name = "fawerestore",
            aliases = {"/fawerestore", "/frestore"},
            desc = "Redo a specific edit. " +
                   " - The time uses s, m, h, d, y.\n" +
                   " - Import from disk: /frb #import"
    )
    @CommandPermissions("worldedit.history.rollback")
    public void restore(Player player, LocalSession session, @Arg(desc = "String user") String user, @Arg(def = "0", desc = "radius") int radius, @Arg(name = "time", desc = "String", def = "0") String time) throws WorldEditException {
        faweRollback(player, session, user, radius, time, true);
    }

    @Command(
            name = "/undo",
            aliases = { "/un", "/ud", "undo" },
            desc = "Undoes the last action (from history)"
    )
    @CommandPermissions({"worldedit.history.undo", "worldedit.history.undo.self"})
    public void undo(Player player, LocalSession session,
                     @Confirm(Confirm.Processor.LIMIT) @Arg(desc = "Number of undoes to perform", def = "1")
                             int times,
                     @Arg(name = "player", desc = "Undo this player's operations", def = "")
                             String playerName) throws WorldEditException {
        times = Math.max(1, times);
        LocalSession undoSession = session;
        if (session.hasFastMode()) {
            player.print(TranslatableComponent.of("fawe.worldedit.history.command.undo.disabled"));
            return;
        }
        if (playerName != null) {
            player.checkPermission("worldedit.history.undo.other");
            undoSession = worldEdit.getSessionManager().findByName(playerName);
            if (undoSession == null) {
                player.printError(TranslatableComponent.of("worldedit.session.cant-find-session", TextComponent.of(playerName)));
                return;
            }
        }
        int timesUndone = 0;
        for (int i = 0; i < times; ++i) {
            EditSession undone = undoSession.undo(undoSession.getBlockBag(player), player);
            if (undone != null) {
                timesUndone++;
                worldEdit.flushBlockBag(player, undone);
            } else {
                break;
            }
        }
        if (timesUndone > 0) {
            player.printInfo(TranslatableComponent.of("worldedit.undo.undone", TextComponent.of(timesUndone)));
        } else {
            player.printError(TranslatableComponent.of("worldedit.undo.none"));
        }
    }

    @Command(
        name = "/redo",
        aliases = { "/do", "/rd", "redo" },
        desc = "Redoes the last action (from history)"
    )
    @CommandPermissions({"worldedit.history.redo", "worldedit.history.redo.self"})
    public void redo(Player player, LocalSession session,
                     @Confirm(Confirm.Processor.LIMIT) @Arg(desc = "Number of redoes to perform", def = "1")
                         int times,
                     @Arg(name = "player", desc = "Redo this player's operations", def = "")
                         String playerName) throws WorldEditException {
        times = Math.max(1, times);
        LocalSession redoSession = session;
        if (playerName != null) {
            player.checkPermission("worldedit.history.redo.other");
            redoSession = worldEdit.getSessionManager().findByName(playerName);
            if (redoSession == null) {
                player.printError(TranslatableComponent.of("worldedit.session.cant-find-session", TextComponent.of(playerName)));
                return;
            }
        }
        int timesRedone = 0;
        for (int i = 0; i < times; ++i) {
            EditSession redone = redoSession.redo(redoSession.getBlockBag(player), player);
            if (redone != null) {
                timesRedone++;
                worldEdit.flushBlockBag(player, redone);
            } else {
                break;
            }
        }
        if (timesRedone > 0) {
            player.printInfo(TranslatableComponent.of("worldedit.redo.redid", TextComponent.of(timesRedone)));
        } else {
            player.printError(TranslatableComponent.of("worldedit.redo.none"));
        }
    }

    @Command(
        name = "clearhistory",
        aliases = { "/clearhistory" },
        desc = "Clear your history"
    )
    @CommandPermissions("worldedit.history.clear")
    public void clearHistory(Actor actor, LocalSession session) {
        session.clearHistory();
        actor.printInfo(TranslatableComponent.of("worldedit.clearhistory.cleared"));
    }

}
