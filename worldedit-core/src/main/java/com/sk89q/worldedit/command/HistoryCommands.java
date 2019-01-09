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
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.database.DBHandler;
import com.boydti.fawe.database.RollbackDatabase;
import com.boydti.fawe.logging.rollback.RollbackOptimizedHistory;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.command.binding.Range;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Commands to undo, redo, and clear history.
 */
@Command(aliases = {}, desc = "Commands to undo, redo, and clear history: [More Info](http://wiki.sk89q.com/wiki/WorldEdit/Features#History)")
public class HistoryCommands extends MethodCommands {

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public HistoryCommands(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Command(
            aliases = {"/frb", "frb", "fawerollback", "/fawerollback", "/rollback"},
            usage = "<user=Empire92> <radius=5> <time=3d4h>",
            desc = "Undo a specific edit. " +
                    " - The time uses s, m, h, d, y.\n" +
                    " - Import from disk: /frb #import",
            min = 1,
            max = 3
    )
    @CommandPermissions("worldedit.history.rollback")
    public void faweRollback(final Player player, LocalSession session, final String user, @Optional("0") @Range(min = 0) int radius, @Optional("0") String time, @Switch('r') boolean restore) throws WorldEditException {
        if (!Settings.IMP.HISTORY.USE_DATABASE) {
            BBC.SETTING_DISABLE.send(player, "history.use-database (Import with /frb #import )");
            return;
        }
        switch (user.charAt(0)) {
            case '#': {
                if (user.equals("#import")) {
                    if (!player.hasPermission("fawe.rollback.import")) {
                        BBC.NO_PERM.send(player, "fawe.rollback.import");
                        return;
                    }
                    File folder = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HISTORY);
                    if (!folder.exists()) {
                        return;
                    }
                    for (File worldFolder : folder.listFiles()) {
                        if (!worldFolder.isDirectory()) {
                            continue;
                        }
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
                                        RollbackOptimizedHistory rollback = new RollbackOptimizedHistory(world, uuid, Integer.parseInt(name.substring(0, name.length() - 3)));
                                        DiskStorageHistory.DiskStorageSummary summary = rollback.summarize(RegionWrapper.GLOBAL(), false);
                                        if (summary != null) {
                                            rollback.setDimensions(new BlockVector3(summary.minX, 0, summary.minZ), new BlockVector3(summary.maxX, 255, summary.maxZ));
                                            rollback.setTime(historyFile.lastModified());
                                            RollbackDatabase db = DBHandler.IMP.getDatabase(world);
                                            db.logEdit(rollback);
                                            player.print(BBC.getPrefix() + "Logging: " + historyFile);
                                        }
                                    }
                                } catch (IllegalArgumentException e) {
                                    e.printStackTrace();
                                    continue;
                                }
                            }
                        }
                    }
                    player.print(BBC.getPrefix() + "Done import!");
                    return;
                }
                String toParse = user.substring(1);
                if (!MathMan.isInteger(toParse)) {
                    BBC.COMMAND_SYNTAX.send(player, "/frb #<index>");
                    return;
                }
                int index = Integer.parseInt(toParse);
                final World world = player.getWorld();
                UUID uuid = player.getUniqueId();
                DiskStorageHistory file = new DiskStorageHistory(world, uuid, index);
                if (file.getBDFile().exists()) {
                    if (restore) file.redo(FawePlayer.wrap(player));
                    else file.undo(FawePlayer.wrap(player));
                    BBC.ROLLBACK_ELEMENT.send(player, Fawe.imp().getWorldName(world) + "/" + user + "-" + index);
                } else {
                    BBC.TOOL_INSPECT_INFO_FOOTER.send(player, 0);
                }
                return;
            }
        }
        UUID other = Fawe.imp().getUUID(user);
        if (other == null) {
            BBC.PLAYER_NOT_FOUND.send(player, user);
            return;
        }
        if (radius == 0) {
            BBC.COMMAND_SYNTAX.send(player, "/frb " + user + " <radius> <time>");
            return;
        }
        long timeDiff = MainUtil.timeToSec(time) * 1000;
        if (timeDiff == 0) {
            BBC.COMMAND_SYNTAX.send(player, "/frb " + user + " " + radius + " <time>");
            return;
        }
        radius = Math.max(Math.min(500, radius), 0);
        final World world = player.getWorld();
        Location origin = player.getLocation();
        BlockVector3 bot = origin.toVector().toBlockPoint().subtract(radius, radius, radius);
        bot = bot.withY(Math.max(0, bot.getY()));
//        bot.mutY(Math.max(0, bot.getY()));
        BlockVector3 top = origin.toVector().toBlockPoint().add(radius, radius, radius);
        bot = bot.withY(Math.min(255, top.getY()));
//        top.mutY(Math.min(255, top.getY()));
        RollbackDatabase database = DBHandler.IMP.getDatabase(world);
        final AtomicInteger count = new AtomicInteger();
        final FawePlayer fp = FawePlayer.wrap(player);

        final FaweQueue finalQueue;
        Region[] allowedRegions = fp.getCurrentRegions(FaweMaskManager.MaskType.OWNER);
        if (allowedRegions == null) {
            BBC.NO_REGION.send(fp);
            return;
        }
        // TODO mask the regions bot / top to the bottom and top coord in the allowedRegions
        // TODO: then mask the edit to the bot / top
//        if (allowedRegions.length != 1 || !allowedRegions[0].isGlobal()) {
//            finalQueue = new MaskedFaweQueue(SetQueue.IMP.getNewQueue(fp.getWorld(), true, false), allowedRegions);
//        } else {
//            finalQueue = SetQueue.IMP.getNewQueue(fp.getWorld(), true, false);
//        }
        database.getPotentialEdits(other, System.currentTimeMillis() - timeDiff, bot, top, new RunnableVal<DiskStorageHistory>() {
            @Override
            public void run(DiskStorageHistory edit) {
                edit.undo(fp, allowedRegions);
                BBC.ROLLBACK_ELEMENT.send(player, Fawe.imp().getWorldName(edit.getWorld()) + "/" + user + "-" + edit.getIndex());
                count.incrementAndGet();
            }
        }, new Runnable() {
            @Override
            public void run() {
                BBC.TOOL_INSPECT_INFO_FOOTER.send(player, count);
            }
        }, true, restore);
    }

    @Command(
            aliases = {"/fawerestore", "/frestore"},
            usage = "<user=Empire92|*> <radius=5> <time=3d4h>",
            desc = "Redo a specific edit. " +
                    " - The time uses s, m, h, d, y.\n" +
                    " - Import from disk: /frb #import",
            min = 1,
            max = 3
    )
    @CommandPermissions("worldedit.history.rollback")
    public void restore(final Player player, LocalSession session, final String user, @Optional("0") @Range(min = 0) int radius, @Optional("0") String time) throws WorldEditException {
        faweRollback(player, session, user, radius, time, true);
    }

    @Command(
            aliases = {"/undo", "undo"},
            usage = "[times] [player]",
            desc = "Undoes the last action",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.history.undo")
    public void undo(Player player, LocalSession session, CommandContext context) throws WorldEditException {
        int times = Math.max(1, context.getInteger(0, 1));
        FawePlayer.wrap(player).checkConfirmation(() -> {
            EditSession undone = null;
            int i = 0;
            for (; i < times; ++i) {
                if (context.argsLength() < 2) {
                    undone = session.undo(session.getBlockBag(player), player);
                } else {
                    player.checkPermission("worldedit.history.undo.other");
                    LocalSession sess = worldEdit.getSessionManager().findByName(context.getString(1));
                    if (sess == null) {
                        BBC.COMMAND_HISTORY_OTHER_ERROR.send(player, context.getString(1));
                        break;
                    }
                    undone = sess.undo(session.getBlockBag(player), player);
                    if (undone == null) break;
                }
            }
            if (undone == null) i--;
            if (i > 0) {
                BBC.COMMAND_UNDO_SUCCESS.send(player, i == 1 ? "" : " x" + i);
                worldEdit.flushBlockBag(player, undone);
            }
            if (undone == null) {
                BBC.COMMAND_UNDO_ERROR.send(player);
            }
        }, getArguments(context), times, 50, context);
    }

    @Command(
            aliases = {"/redo", "redo"},
            usage = "[times] [player]",
            desc = "Redoes the last action (from history)",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.history.redo")
    public void redo(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        int times = Math.max(1, args.getInteger(0, 1));

        EditSession redone = null;
        int i = 0;
        for (; i < times; ++i) {
            if (args.argsLength() < 2) {
                redone = session.redo(session.getBlockBag(player), player);
            } else {
                player.checkPermission("worldedit.history.redo.other");
                LocalSession sess = worldEdit.getSessionManager().findByName(args.getString(1));
                if (sess == null) {
                    BBC.COMMAND_HISTORY_OTHER_ERROR.send(player, args.getString(1));
                    break;
                }
                redone = sess.redo(session.getBlockBag(player), player);
                if (redone == null) break;
            }
        }
        if (redone == null) i--;
        if (i > 0) {
            BBC.COMMAND_REDO_SUCCESS.send(player, i == 1 ? "" : " x" + i);
            worldEdit.flushBlockBag(player, redone);
        }
        if (redone == null) {
            BBC.COMMAND_REDO_ERROR.send(player);
        }
    }

    @Command(
            aliases = {"/clearhistory", "clearhistory"},
            usage = "",
            desc = "Clear your history",
            min = 0,
            max = 0
    )
    @CommandPermissions("worldedit.history.clear")
    public void clearHistory(Player player, LocalSession session) throws WorldEditException {
        session.clearHistory();
        BBC.COMMAND_HISTORY_CLEAR.send(player);
    }


}