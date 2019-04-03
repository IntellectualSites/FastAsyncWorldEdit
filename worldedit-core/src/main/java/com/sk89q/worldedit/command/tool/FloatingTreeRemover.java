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

import com.boydti.fawe.object.collection.BlockVectorSet;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockCategories;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * A pickaxe mode that removes floating treetops (logs and leaves not connected
 * to anything else)
 */
public class FloatingTreeRemover implements BlockTool {
    private int rangeSq;

    public FloatingTreeRemover() {
        rangeSq = 100*100;
    }

    @Override
    public boolean canUse(Actor player) {
        return player.hasPermission("worldedit.tool.deltree");
    }

    private boolean isTreeBlock(BlockType type) {
        return BlockCategories.LEAVES.contains(type)
                || BlockCategories.LOGS.contains(type)
                || type == BlockTypes.RED_MUSHROOM_BLOCK
                || type == BlockTypes.BROWN_MUSHROOM_BLOCK
                || type == BlockTypes.MUSHROOM_STEM
                || type == BlockTypes.VINE;
    }

    @Override
    public boolean actPrimary(Platform server, LocalConfiguration config,
            Player player, LocalSession session, Location clicked) {

        final World world = (World) clicked.getExtent();
        BlockVector3 pos = clicked.toBlockPoint();
        final BlockState state = world.getBlock(pos);

        if (!isTreeBlock(state.getBlockType())) {
            player.printError("That's not a tree.");
            return true;
        }

        try (EditSession editSession = session.createEditSession(player)) {
            try {
                Pattern replace = BlockTypes.AIR;
                RecursiveVisitor visitor = new RecursiveVisitor(new BlockMask(editSession, logs, leaves), replace, 64, editSession);
                visitor.visit(pos);
                Operations.completeBlindly(visitor);
            } finally {
                session.remember(editSession);
            }
        }

        return true;
    }

    private BlockVector3[] recurseDirections = {
            Direction.EAST.toBlockVector(),
            Direction.SOUTH.toBlockVector(),
            Direction.WEST.toBlockVector(),
            Direction.UP.toBlockVector(),
            Direction.DOWN.toBlockVector(),
    };
}
