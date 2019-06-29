package com.boydti.fawe.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.*;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Rollback extends FaweCommand {

    public Rollback() {
        super("fawe.rollback");
    }

    @Override
    public boolean execute(final FawePlayer player, final String... args) {
        if (!Settings.IMP.HISTORY.USE_DATABASE) {
            BBC.SETTING_DISABLE.send(player, "history.use-database (Import with /frb #import )");
            return false;
        }
        if (args.length != 3) {
            BBC.COMMAND_SYNTAX.send(player, "/frb u:<uuid> r:<radius> t:<time>");
            return false;
        }
        switch (args[0]) {
            case "i":
            case "info":
            case "undo":
            case "revert":
                BBC.COMMAND_SYNTAX.send(player, "/frb u:<uuid> r:<radius> t:<time>");
                return false;
        }


        if (args.length < 1) {
            BBC.COMMAND_SYNTAX.send(player, "/frb <info|undo> u:<uuid> r:<radius> t:<time>");
            return false;
        }
        World world = player.getWorld();
        switch (args[0]) {
            default: {
                BBC.COMMAND_SYNTAX.send(player, "/frb info u:<uuid> r:<radius> t:<time>");
                return false;
            }
            case "i":
            case "info": {
                if (args.length < 2) {
                    BBC.COMMAND_SYNTAX.send(player, "/frb <info|undo> u:<uuid> r:<radius> t:<time>");
                    return false;
                }
                player.deleteMeta(FawePlayer.METADATA_KEYS.ROLLBACK);
                Location origin = player.getPlayer().getLocation();
                rollback(player, !player.hasPermission("fawe.rollback.deep"), Arrays.copyOfRange(args, 1, args.length), new RunnableVal<List<DiskStorageHistory>>() {
                    @Override
                    public void run(List<DiskStorageHistory> edits) {
                        long total = 0;
                        player.sendMessage("&d=| Username | Bounds | Distance | Changes | Age |=");
                        for (DiskStorageHistory edit : edits) {
                            DiskStorageHistory.DiskStorageSummary summary = edit.summarize(new RegionWrapper(origin.getBlockX(), origin.getBlockX(), origin.getBlockZ(), origin.getBlockZ()), !player.hasPermission("fawe.rollback.deep"));
                            RegionWrapper region = new RegionWrapper(summary.minX, summary.maxX, summary.minZ, summary.maxZ);
                            int distance = region.distance(origin.getBlockX(), origin.getBlockZ());
                            String name = Fawe.imp().getName(edit.getUUID());
                            long seconds = (System.currentTimeMillis() - edit.getBDFile().lastModified()) / 1000;
                            total += edit.getBDFile().length();
                            int size = summary.getSize();
                            Map<BlockState, Double> percents = summary.getPercents();
                            StringBuilder percentString = new StringBuilder();
                            String prefix = "";
                            for (Map.Entry<BlockState, Double> entry : percents.entrySet()) {
                                BlockState state = entry.getKey();
                                String itemName = "#" + state;
                                percentString.append(prefix).append(entry.getValue()).append("% ").append(itemName);
                                prefix = ", ";
                            }
                            player.sendMessage("&c" + name + " | " + region + " | " + distance + "m | " + size + " | " + MainUtil.secToTime(seconds));
                            player.sendMessage("&8 - &7(" + percentString + ")");
                        }
                        player.sendMessage("&d==================================================");
                        player.sendMessage("&dSize: " + (((double) (total / 1024)) / 1000) + "MB");
                        player.sendMessage("&dTo rollback: /frb undo");
                        player.sendMessage("&d==================================================");
                        player.setMeta(FawePlayer.METADATA_KEYS.ROLLBACK, edits);
                    }
                });
                break;
            }
            case "undo":
            case "revert": {
                if (!player.hasPermission("fawe.rollback.perform")) {
                    BBC.NO_PERM.send(player, "fawe.rollback.perform");
                    return false;
                }
                final List<DiskStorageHistory> edits = player.getMeta(FawePlayer.METADATA_KEYS.ROLLBACK);
                player.deleteMeta(FawePlayer.METADATA_KEYS.ROLLBACK);
                if (edits == null) {
                    BBC.COMMAND_SYNTAX.send(player, "/frb info u:<uuid> r:<radius> t:<time>");
                    return false;
                }
                final Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        if (edits.size() == 0) {
                            player.sendMessage("Rollback complete!");
                            return;
                        }
                        DiskStorageHistory edit = edits.remove(0);
                        player.sendMessage("&d" + edit.getBDFile());
                        EditSession session = edit.toEditSession(null);
                        session.undo(session);
                        edit.deleteFiles();
                        session.getQueue().addNotifyTask(this);
                    }
                };
                task.run();
            }
        }
        return true;
    }

    public void rollback(final FawePlayer player, final boolean shallow, final String[] args, final RunnableVal<List<DiskStorageHistory>> result) {
        UUID user = null;
        int radius = Integer.MAX_VALUE;
        long time = Long.MAX_VALUE;
        for (String arg : args) {
            String[] split = arg.split(":");
            if (split.length != 2) {
                BBC.COMMAND_SYNTAX.send(player, "/frb <info|undo> u:<uuid> r:<radius> t:<time>");
                return;
            }
            switch (split[0].toLowerCase()) {
                case "username":
                case "user":
                case "u": {
                    try {
                        if (split[1].length() > 16) {
                            user = UUID.fromString(split[1]);
                        } else {
                            user = Fawe.imp().getUUID(split[1]);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                    if (user == null) {
                        player.sendMessage("&dInvalid user: " + split[1]);
                        return;
                    }
                    break;
                }
                case "r":
                case "radius": {
                    if (!MathMan.isInteger(split[1])) {
                        player.sendMessage("&dInvalid radius: " + split[1]);
                        return;
                    }
                    radius = Integer.parseInt(split[1]);
                    break;
                }
                case "t":
                case "time": {
                    time = MainUtil.timeToSec(split[1]) * 1000;
                    break;
                }
                default: {
                    BBC.COMMAND_SYNTAX.send(player, "/frb <info|undo> u:<uuid> r:<radius> t:<time>");
                    return;
                }
            }
        }
        Location origin = player.getLocation();
        List<DiskStorageHistory> edits = FaweAPI.getBDFiles(origin, user, radius, time, shallow);
        if (edits == null) {
            player.sendMessage("&cToo broad, try refining your search!");
            return;
        }
        if (edits.size() == 0) {
            player.sendMessage("&cNo edits found!");
            return;
        }
        result.run(edits);
    }
}
