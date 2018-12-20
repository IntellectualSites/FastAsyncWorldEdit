package com.sk89q.worldedit.command.tool;

import com.boydti.fawe.object.mask.IdMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockStateHolder;
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
    public boolean actPrimary(Platform server, LocalConfiguration config, Player player, LocalSession session, com.sk89q.worldedit.util.Location clicked) {
        World world = (World) clicked.getExtent();
        final Vector pos = clicked.toVector();

        EditSession editSession = session.createEditSession(player);

        BlockStateHolder block = editSession.getBlock(pos);
        if (block.getBlockType().getMaterial().isAir()) {
            return true;
        }

        if (block.getBlockType() == BlockTypes.BEDROCK && !player.canDestroyBedrock()) {
            return true;
        }

        editSession.getSurvivalExtent().setToolUse(config.superPickaxeManyDrop);

        final int radius = (int) range;
        final BlockReplace replace = new BlockReplace(editSession, (editSession.nullBlock));
        editSession.setMask((Mask) null);
        RecursiveVisitor visitor = new RecursiveVisitor(new IdMask(editSession), replace, radius, editSession);
        visitor.visit(pos);
        Operations.completeBlindly(visitor);

        editSession.flushQueue();
        session.remember(editSession);

        return true;
    }


}