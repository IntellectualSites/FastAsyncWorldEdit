package com.sk89q.worldedit.command.tool;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

/**
 * A super pickaxe mode that will remove blocks in an area.
 */
public class AreaPickaxe implements BlockTool {

    private int range;

    public AreaPickaxe(int range) {
        this.range = range;
    }

    @Override
    public boolean canUse(Actor player) {
        return player.hasPermission("worldedit.superpickaxe.area");
    }

    @Override
    public boolean actPrimary(Platform server, LocalConfiguration config, Player player, LocalSession session, com.sk89q.worldedit.util.Location clicked) {
        int ox = clicked.getBlockX();
        int oy = clicked.getBlockY();
        int oz = clicked.getBlockZ();
        BlockType initialType = clicked.getExtent().getBlock(clicked.toVector()).getBlockType();

        if (initialType.getMaterial().isAir()) {
            return true;
        }

        if (initialType == BlockTypes.BEDROCK && !player.canDestroyBedrock()) {
            return true;
        }

        EditSession editSession = session.createEditSession(player);
        editSession.getSurvivalExtent().setToolUse(config.superPickaxeManyDrop);

        for (int x = ox - range; x <= ox + range; ++x) {
            for (int z = oz - range; z <= oz + range; ++z) {
                for (int y = oy + range; y >= oy - range; --y) {
                    if (initialType.equals(editSession.getLazyBlock(x, y, z))) {
                        continue;
                    }
                    editSession.setBlock(x, y, z, BlockTypes.AIR.getDefaultState());
                }
            }
        }
        editSession.flushQueue();
        session.remember(editSession);

        return true;
    }


}