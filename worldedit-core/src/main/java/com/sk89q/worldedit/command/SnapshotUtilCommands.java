/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.command;

import com.fastasyncworldedit.core.configuration.Caption;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.command.util.Logging;
import com.sk89q.worldedit.command.util.annotation.SynchronousSettingExpected;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.snapshot.experimental.Snapshot;
import com.sk89q.worldedit.world.snapshot.experimental.SnapshotRestore;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.Switch;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;

import static com.sk89q.worldedit.command.SnapshotCommands.checkSnapshotsConfigured;
import static com.sk89q.worldedit.command.SnapshotCommands.resolveSnapshotName;
import static com.sk89q.worldedit.command.util.Logging.LogMode.REGION;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class SnapshotUtilCommands {

    private final WorldEdit we;
    private final LegacySnapshotUtilCommands legacy;

    public SnapshotUtilCommands(WorldEdit we) {
        this.we = we;
        this.legacy = new LegacySnapshotUtilCommands(we);
    }

    @Command(
            name = "restore",
            aliases = {"/restore"},
            desc = "Restore the selection from a snapshot"
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.snapshots.restore")
    @SynchronousSettingExpected
    public void restore(
            Actor actor, World world, LocalSession session, EditSession editSession,
            @Arg(name = "snapshot", desc = "The snapshot to restore", def = "")
                    String snapshotName,
            //FAWE start - biome and entity restore
            @Switch(name = 'b', desc = "If biomes should be restored. If restoring from pre-1.15 to 1.15+, biomes may not be " +
                    "exactly the same due to 3D biomes.")
                    boolean restoreBiomes,
            @Switch(name = 'e', desc = "If entities should be restored. Will cause issues with duplicate entities if all " +
                    "original entities were not removed.")
                    boolean restoreEntities
            //FAWE end
    ) throws WorldEditException, IOException {
        LocalConfiguration config = we.getConfiguration();
        checkSnapshotsConfigured(config);

        if (config.snapshotRepo != null) {
            //FAWE start - biome and entity restore
            legacy.restore(actor, world, session, editSession, snapshotName, restoreBiomes, restoreEntities);
            //FAWE end
            return;
        }

        Region region = session.getSelection(world);
        Snapshot snapshot;

        if (snapshotName != null) {
            URI uri = resolveSnapshotName(config, snapshotName);
            Optional<Snapshot> snapOpt = config.snapshotDatabase.getSnapshot(uri);
            if (snapOpt.isEmpty()) {
                actor.print(Caption.of("worldedit.restore.not-available"));
                return;
            }
            snapshot = snapOpt.get();
        } else {
            snapshot = session.getSnapshotExperimental();
        }

        // No snapshot set?
        if (snapshot == null) {
            try (Stream<Snapshot> snapshotStream =
                         config.snapshotDatabase.getSnapshotsNewestFirst(world.getName())) {
                snapshot = snapshotStream
                        .findFirst().orElse(null);
            }

            if (snapshot == null) {
                actor.print(Caption.of(
                        "worldedit.restore.none-for-specific-world",
                        TextComponent.of(world.getName())
                ));
                return;
            }
        }
        actor.print(Caption.of(
                "worldedit.restore.loaded",
                TextComponent.of(snapshot.getInfo().getDisplayName())
        ));

        try {
            // Restore snapshot
            //FAWE start - biome and entity restore
            SnapshotRestore restore = new SnapshotRestore(snapshot, editSession, region, restoreBiomes, restoreEntities);
            //FAWE end

            restore.restore();

            if (restore.hadTotalFailure()) {
                String error = restore.getLastErrorMessage();
                if (!restore.getMissingChunks().isEmpty()) {
                    actor.print(Caption.of("worldedit.restore.chunk-not-present"));
                } else if (error != null) {
                    actor.print(Caption.of("worldedit.restore.block-place-failed"));
                    actor.print(Caption.of("worldedit.restore.block-place-error", TextComponent.of(error)));
                } else {
                    actor.print(Caption.of("worldedit.restore.chunk-load-failed"));
                }
            } else {
                actor.print(Caption.of(
                        "worldedit.restore.restored",
                        TextComponent.of(restore.getMissingChunks().size()),
                        TextComponent.of(restore.getErrorChunks().size())
                ));
            }
        } finally {
            try {
                snapshot.close();
            } catch (IOException ignored) {
            }
        }
    }

}
