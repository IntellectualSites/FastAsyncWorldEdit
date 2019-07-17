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
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.world.snapshot.InvalidSnapshotException;
import com.sk89q.worldedit.world.snapshot.Snapshot;
import com.sk89q.worldedit.world.storage.MissingWorldException;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Snapshot commands.
 */
@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
@Command(aliases = {"snapshot", "snap"}, desc = "List, load and view information related to snapshots")
public class SnapshotCommands {

    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private final WorldEdit we;

    public SnapshotCommands(WorldEdit we) {
        this.we = we;
    }

    @Command(
        name = "list",
        desc = "List snapshots"
    )
    @CommandPermissions("worldedit.snapshots.list")
    public void list(Player player,
                     @Arg(desc = "# of snapshots to list", def = "5")
                         int num) throws WorldEditException {

        LocalConfiguration config = we.getConfiguration();

        if (config.snapshotRepo == null) {
            BBC.SNAPSHOT_NOT_CONFIGURED.send(player);
            return;
        }

        try {
            List<Snapshot> snapshots = config.snapshotRepo.getSnapshots(true, player.getWorld().getName());

            if (!snapshots.isEmpty()) {

                num = Math.min(40, Math.max(5, num));

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
        name = "use",
        desc = "Choose a snapshot to use"
    )
    @CommandPermissions("worldedit.snapshots.restore")
    public void use(Player player, LocalSession session,
                    @Arg(desc = "Snapeshot to use")
                        String name) throws WorldEditException {

        LocalConfiguration config = we.getConfiguration();

        if (config.snapshotRepo == null) {
            BBC.SNAPSHOT_NOT_CONFIGURED.send(player);
            return;
        }

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
        name = "sel",
        desc = "Choose the snapshot based on the list id"
    )
    @CommandPermissions("worldedit.snapshots.restore")
    public void sel(Player player, LocalSession session,
                    @Arg(desc = "The list ID to select")
                        int index) throws WorldEditException {
        LocalConfiguration config = we.getConfiguration();

        if (config.snapshotRepo == null) {
            BBC.SNAPSHOT_NOT_CONFIGURED.send(player);
            return;
        }

        if (index < 1) {
            BBC.SNAPSHOT_INVALID_INDEX.send(player);
            return;
        }

        try {
            List<Snapshot> snapshots = config.snapshotRepo.getSnapshots(true, player.getWorld().getName());
            if (snapshots.size() < index) {
                player.printError("Invalid index, must be between 1 and " + snapshots.size() + ".");
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
        name = "before",
        desc = "Choose the nearest snapshot before a date"
    )
    @CommandPermissions("worldedit.snapshots.restore")
    public void before(Player player, LocalSession session,
                       @Arg(desc = "The soonest date that may be used")
                           ZonedDateTime date) throws WorldEditException {

        LocalConfiguration config = we.getConfiguration();

        if (config.snapshotRepo == null) {
            BBC.SNAPSHOT_NOT_CONFIGURED.send(player);
            return;
        }

            try {
                Snapshot snapshot = config.snapshotRepo.getSnapshotBefore(date, player.getWorld().getName());

                if (snapshot == null) {
                    player.printError("Couldn't find a snapshot before "
                    + dateFormat.withZone(session.getTimeZone().toZoneId()).format(date) + ".");
                } else {
                    session.setSnapshot(snapshot);
                    BBC.SNAPSHOT_SET.send(player, snapshot.getName());
                }
            } catch (MissingWorldException ex) {
                BBC.SNAPSHOT_NOT_FOUND_WORLD.send(player);
            }
    }

    @Command(
        name = "after",
        desc = "Choose the nearest snapshot after a date"
    )
    @CommandPermissions("worldedit.snapshots.restore")
    public void after(Player player, LocalSession session,
                      @Arg(desc = "The soonest date that may be used")
                          ZonedDateTime date) throws WorldEditException {

        LocalConfiguration config = we.getConfiguration();

        if (config.snapshotRepo == null) {
            BBC.SNAPSHOT_NOT_CONFIGURED.send(player);
            return;
        }

            try {
                Snapshot snapshot = config.snapshotRepo.getSnapshotAfter(date, player.getWorld().getName());
                if (snapshot == null) {
                    player.printError("Couldn't find a snapshot after "
                    + dateFormat.withZone(session.getTimeZone().toZoneId()).format(date) + ".");
                } else {
                    session.setSnapshot(snapshot);
                    BBC.SNAPSHOT_SET.send(player, snapshot.getName());
                }
            } catch (MissingWorldException ex) {
                BBC.SNAPSHOT_NOT_FOUND_WORLD.send(player);
        }
    }

}
