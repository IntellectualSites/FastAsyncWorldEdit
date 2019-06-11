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

// $Id$

package com.sk89q.worldedit.command;

import com.boydti.fawe.config.BBC;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.world.snapshot.InvalidSnapshotException;
import com.sk89q.worldedit.world.snapshot.Snapshot;
import com.sk89q.worldedit.world.storage.MissingWorldException;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

/**
 * Snapshot commands.
 */
@Command(aliases = {"snapshot", "snap"}, desc = "List, load and view information related to snapshots")
public class SnapshotCommands {

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    private final WorldEdit we;

    public SnapshotCommands(WorldEdit we) {
        this.we = we;
    }

    @Command(
            aliases = { "list" },
            usage = "[num]",
            desc = "List snapshots",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.snapshots.list")
    public void list(Player player, CommandContext args) throws WorldEditException {

        LocalConfiguration config = we.getConfiguration();

        if (config.snapshotRepo == null) {
            BBC.SNAPSHOT_NOT_CONFIGURED.send(player);
            return;
        }

        try {
            List<Snapshot> snapshots = config.snapshotRepo.getSnapshots(true, player.getWorld().getName());

            if (!snapshots.isEmpty()) {

                int num = args.argsLength() > 0 ? Math.min(40, Math.max(5, args.getInteger(0))) : 5;

                BBC.SNAPSHOT_LIST_HEADER.send(player, player.getWorld().getName());
                for (byte i = 0; i < Math.min(num, snapshots.size()); i++) {
                    player.print((i + 1) + ". " + snapshots.get(i).getName());
                }

                BBC.SNAPSHOT_LIST_FOOTER.send(player);
            } else {
                BBC.SNAPSHOT_NOT_AVAILABLE.send(player);

                // Okay, let's toss some debugging information!
                File dir = config.snapshotRepo.getDirectory();

                try {
                    WorldEdit.logger.info("WorldEdit found no snapshots: looked in: "
                            + dir.getCanonicalPath());
                } catch (IOException e) {
                    WorldEdit.logger.info("WorldEdit found no snapshots: looked in "
                            + "(NON-RESOLVABLE PATH - does it exist?): "
                            + dir.getPath());
                }
            }
        } catch (MissingWorldException ex) {
            BBC.SNAPSHOT_NOT_FOUND_WORLD.send(player);
        }
    }

    @Command(
            aliases = { "use" },
            usage = "<snapshot>",
            desc = "Choose a snapshot to use",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.snapshots.restore")
    public void use(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        LocalConfiguration config = we.getConfiguration();

        if (config.snapshotRepo == null) {
            BBC.SNAPSHOT_NOT_CONFIGURED.send(player);
            return;
        }

        String name = args.getString(0);

        // Want the latest snapshot?
        if (name.equalsIgnoreCase("latest")) {
            try {
                Snapshot snapshot = config.snapshotRepo.getDefaultSnapshot(player.getWorld().getName());

                if (snapshot != null) {
                    session.setSnapshot(null);
                    BBC.SNAPSHOT_NEWEST.send(player);
                } else {
                    BBC.SNAPSHOT_NOT_FOUND.send(player);
                }
            } catch (MissingWorldException ex) {
                BBC.SNAPSHOT_NOT_FOUND_WORLD.send(player);
            }
        } else {
            try {
                session.setSnapshot(config.snapshotRepo.getSnapshot(name));
                BBC.SNAPSHOT_SET.send(player, name);
            } catch (InvalidSnapshotException e) {
                BBC.SNAPSHOT_NOT_AVAILABLE.send(player);
            }
        }
    }

    @Command(
            aliases = { "sel" },
            usage = "<index>",
            desc = "Choose the snapshot based on the list id",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.snapshots.restore")
    public void sel(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        LocalConfiguration config = we.getConfiguration();

        if (config.snapshotRepo == null) {
            BBC.SNAPSHOT_NOT_CONFIGURED.send(player);
            return;
        }

        int index = -1;
        try {
            index = Integer.parseInt(args.getString(0));
        } catch (NumberFormatException e) {
            player.printError(BBC.getPrefix() + "Invalid index, " + args.getString(0) + " is not a valid integer.");
            return;
        }

        if (index < 1) {
            BBC.SNAPSHOT_INVALID_INDEX.send(player);
            return;
        }

        try {
            List<Snapshot> snapshots = config.snapshotRepo.getSnapshots(true, player.getWorld().getName());
            if (snapshots.size() < index) {
                player.printError(BBC.getPrefix() + "Invalid index, must be between 1 and " + snapshots.size() + ".");
                return;
            }
            Snapshot snapshot = snapshots.get(index - 1);
            if (snapshot == null) {
                BBC.SNAPSHOT_NOT_AVAILABLE.send(player);
                return;
            }
            session.setSnapshot(snapshot);
            BBC.SNAPSHOT_SET.send(player, snapshot.getName());
        } catch (MissingWorldException e) {
            BBC.SNAPSHOT_NOT_FOUND_WORLD.send(player);
        }
    }

    @Command(
            aliases = { "before" },
            usage = "<date>",
            desc = "Choose the nearest snapshot before a date",
            min = 1,
            max = -1
    )
    @CommandPermissions("worldedit.snapshots.restore")
    public void before(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        LocalConfiguration config = we.getConfiguration();

        if (config.snapshotRepo == null) {
            BBC.SNAPSHOT_NOT_CONFIGURED.send(player);
            return;
        }

        Calendar date = session.detectDate(args.getJoinedStrings(0));

        if (date == null) {
            BBC.SNAPSHOT_ERROR_DATE.send(player);
        } else {
            try {
                Snapshot snapshot = config.snapshotRepo.getSnapshotBefore(date, player.getWorld().getName());

                if (snapshot == null) {
                    dateFormat.setTimeZone(session.getTimeZone());
                    player.printError("Couldn't find a snapshot before "
                            + dateFormat.format(date.getTime()) + ".");
                } else {
                    session.setSnapshot(snapshot);
                    BBC.SNAPSHOT_SET.send(player, snapshot.getName());
                }
            } catch (MissingWorldException ex) {
                BBC.SNAPSHOT_NOT_FOUND_WORLD.send(player);
            }
        }
    }

    @Command(
            aliases = { "after" },
            usage = "<date>",
            desc = "Choose the nearest snapshot after a date",
            min = 1,
            max = -1
    )
    @CommandPermissions("worldedit.snapshots.restore")
    public void after(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        LocalConfiguration config = we.getConfiguration();

        if (config.snapshotRepo == null) {
            BBC.SNAPSHOT_NOT_CONFIGURED.send(player);
            return;
        }

        Calendar date = session.detectDate(args.getJoinedStrings(0));

        if (date == null) {
            BBC.SNAPSHOT_ERROR_DATE.send(player);
        } else {
            try {
                Snapshot snapshot = config.snapshotRepo.getSnapshotAfter(date, player.getWorld().getName());
                if (snapshot == null) {
                    dateFormat.setTimeZone(session.getTimeZone());
                    player.printError("Couldn't find a snapshot after "
                            + dateFormat.format(date.getTime()) + ".");
                } else {
                    session.setSnapshot(snapshot);
                    BBC.SNAPSHOT_SET.send(player, snapshot.getName());
                }
            } catch (MissingWorldException ex) {
                BBC.SNAPSHOT_NOT_FOUND_WORLD.send(player);
            }
        }
    }

}
