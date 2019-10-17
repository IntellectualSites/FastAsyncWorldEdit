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

package com.sk89q.worldedit.command.tool;

import com.boydti.fawe.object.mask.BlockTypeMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

/**
 * A pickaxe mode that recursively finds adjacent blocks within range of
 * an initial block and of the same type.
 */
public class RecursivePickaxe implements BlockTool {

    private double range;

    public RecursivePickaxe(double range) {
        this.range = range;
    }

    @Override
    public boolean canUse(Actor player) {
        return player.hasPermission("worldedit.superpickaxe.recursive");
    }

    @Override
    public boolean actPrimary(Platform server, LocalConfiguration config, Player player, LocalSession session, Location clicked) {
        World world = (World) clicked.getExtent();
        final BlockVector3 pos = clicked.toBlockPoint();

        BlockVector3 origin = clicked.toVector().toBlockPoint();
        BlockType initialType = world.getBlock(origin).getBlockType();

        if (initialType.getMaterial().isAir()) {
            return false;
        }

        if (initialType == BlockTypes.BEDROCK && !player.canDestroyBedrock()) {
            return false;
        }

        try (EditSession editSession = session.createEditSession(player)) {
            editSession.getSurvivalExtent().setToolUse(config.superPickaxeManyDrop);

            final int radius = (int) range;
            final BlockReplace replace = new BlockReplace(editSession, BlockTypes.AIR.getDefaultState());
            editSession.setMask(null);
            RecursiveVisitor visitor = new RecursiveVisitor(new BlockTypeMask(editSession), replace, radius);
            visitor.visit(pos);
            Operations.completeBlindly(visitor);

            editSession.flushQueue();
            session.remember(editSession);
        }

        return true;
    }
}
