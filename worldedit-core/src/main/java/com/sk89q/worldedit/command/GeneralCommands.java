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

import com.boydti.fawe.config.BBC;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.extension.input.DisallowedUsageException;
import com.sk89q.worldedit.entity.Player;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * General WorldEdit commands.
 */
public class GeneralCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public GeneralCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
        aliases = { "/limit" },
        usage = "[limit]",
        desc = "Modify block change limit",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.limit")
    public void limit(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        LocalConfiguration config = worldEdit.getConfiguration();
        boolean mayDisable = player.hasPermission("worldedit.limit.unrestricted");

        int limit = args.argsLength() == 0 ? config.defaultChangeLimit : Math.max(-1, args.getInteger(0));
        if (!mayDisable && config.maxChangeLimit > -1) {
            if (limit > config.maxChangeLimit) {
                player.printError(BBC.getPrefix() + "Your maximum allowable limit is " + config.maxChangeLimit + ".");
                return;
            }
        }

        session.setBlockChangeLimit(limit);

        if (limit != config.defaultChangeLimit) {
            player.print(BBC.getPrefix() + "Block change limit set to " + limit + ". (Use //limit to go back to the default.)");
        } else {
            player.print(BBC.getPrefix() + "Block change limit set to " + limit + ".");
        }
    }

    @Command(
            aliases = { "/timeout" },
            usage = "[time]",
            desc = "Modify evaluation timeout time.",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.timeout")
    public void timeout(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        LocalConfiguration config = worldEdit.getConfiguration();
        boolean mayDisable = player.hasPermission("worldedit.timeout.unrestricted");

        int limit = args.argsLength() == 0 ? config.calculationTimeout : Math.max(-1, args.getInteger(0));
        if (!mayDisable && config.maxCalculationTimeout > -1) {
            if (limit > config.maxCalculationTimeout) {
                player.printError(BBC.getPrefix() + "Your maximum allowable timeout is " + config.maxCalculationTimeout + " ms.");
                return;
            }
        }

        session.setTimeout(limit);

        if (limit != config.calculationTimeout) {
            player.print(BBC.getPrefix() + "Timeout time set to " + limit + " ms. (Use //timeout to go back to the default.)");
        } else {
            player.print(BBC.getPrefix() + "Timeout time set to " + limit + " ms.");
        }
    }

    @Command(
            aliases = { "/drawsel" },
            usage = "[on|off]",
            desc = "Toggle drawing the current selection",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.drawsel")
    public void drawSelection(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        if (!WorldEdit.getInstance().getConfiguration().serverSideCUI) {
            throw new DisallowedUsageException("This functionality is disabled in the configuration!");
        }
        String newState = args.getString(0, null);
        if (session.shouldUseServerCUI()) {
            if ("on".equals(newState)) {
                player.printError(BBC.getPrefix() + "Server CUI already enabled.");
                return;
            }

            session.setUseServerCUI(false);
            session.updateServerCUI(player);
            player.print("Server CUI disabled.");
        } else {
            if ("off".equals(newState)) {
                player.printError(BBC.getPrefix() + "Server CUI already disabled.");
                return;
            }

            session.setUseServerCUI(true);
            session.updateServerCUI(player);
            player.print("Server CUI enabled. This only supports cuboid regions, with a maximum size of 32x32x32.");
        }
    }

}
