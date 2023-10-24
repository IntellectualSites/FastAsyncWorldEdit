package com.fastasyncworldedit.core.command.tool;

import com.fastasyncworldedit.core.configuration.Caption;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.BlockTool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.generation.StructureType;

import javax.annotation.Nullable;

/**
 * Places a feature
 *
 * @since TODO
 */
public class StructurePlacer implements BlockTool {

    private final StructureType structure;

    /**
     * New instance
     *
     * @since TODO
     */
    public StructurePlacer(StructureType structure) {
        this.structure = structure;
    }

    @Override
    public boolean canUse(Actor player) {
        return player.hasPermission("worldedit.tool.feature");
    }

    @Override
    public boolean actPrimary(
            Platform server,
            LocalConfiguration config,
            Player player,
            LocalSession session,
            Location clicked,
            @Nullable Direction face
    ) {

        try (EditSession editSession = session.createEditSession(player)) {
            try {
                boolean successful = false;

                final BlockVector3 pos = clicked.toVector().add().toBlockPoint();
                for (int i = 0; i < 10; i++) {
                    if (structure.place(editSession, pos)) {
                        successful = true;
                        break;
                    }
                }

                if (!successful) {
                    player.print(Caption.of("worldedit.tool.feature.failed"));
                }
            } catch (MaxChangedBlocksException e) {
                player.print(Caption.of("worldedit.tool.max-block-changes"));
            } finally {
                session.remember(editSession);
            }
        }

        return true;
    }

}
