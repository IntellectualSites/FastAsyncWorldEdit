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

package com.sk89q.worldedit.command.tool;

import com.fastasyncworldedit.core.configuration.Settings;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extension.platform.permission.ActorSelectorLimits;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

//FAWE start - enum-ized
public enum SelectionWand implements DoubleActionBlockTool {
    INSTANCE;
//FAWE end

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    @Override
    public boolean actSecondary(
            Platform server,
            LocalConfiguration config,
            Player player,
            LocalSession session,
            Location clicked,
            @Nullable Direction face
    ) {
        RegionSelector selector = session.getRegionSelector(player.getWorld());
        BlockVector3 blockPoint = clicked.toVector().toBlockPoint();

        if (selector.selectPrimary(blockPoint, ActorSelectorLimits.forActor(player))) {
            //FAWE start
            if (Settings.IMP.EXPERIMENTAL.OTHER) {
                LOGGER.info("actSecondary Hit and about to explain with explainPrimarySelection");
            }
            //FAWE end
            selector.explainPrimarySelection(player, session, blockPoint);
        }
        return true;
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
        RegionSelector selector = session.getRegionSelector(player.getWorld());
        BlockVector3 blockPoint = clicked.toVector().toBlockPoint();

        if (selector.selectSecondary(blockPoint, ActorSelectorLimits.forActor(player))) {
            //FAWE start
            if (Settings.IMP.EXPERIMENTAL.OTHER) {
                LOGGER.info("actPrimary Hit and about to explain with explainSecondarySelection");
            }
            //FAWE end
            selector.explainSecondarySelection(player, session, blockPoint);
        }
        return true;
    }

    @Override
    public boolean canUse(Actor actor) {
        return actor.hasPermission("worldedit.selection.pos");
    }
}
