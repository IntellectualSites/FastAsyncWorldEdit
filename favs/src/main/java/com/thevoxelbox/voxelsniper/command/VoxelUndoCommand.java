package com.thevoxelbox.voxelsniper.command;

import com.boydti.fawe.config.BBC;
import com.thevoxelbox.voxelsniper.Sniper;
import com.thevoxelbox.voxelsniper.VoxelSniper;
import com.thevoxelbox.voxelsniper.api.command.VoxelCommand;
import org.bukkit.entity.Player;

public class VoxelUndoCommand extends VoxelCommand {
    public VoxelUndoCommand(final VoxelSniper plugin) {
        super("VoxelUndo", plugin);
        setIdentifier("u");
        setPermission("voxelsniper.sniper");
    }

    @Override
    public boolean onCommand(Player player, String[] args) {
        Sniper sniper = plugin.getSniperManager().getSniperForPlayer(player);

        if (args.length == 1) {
            try {
                int amount = Integer.parseInt(args[0]);
                sniper.undo(amount);
            } catch (NumberFormatException exception) {
                player.sendMessage(BBC.getPrefix() + "Number expected; string given.");
            }
        } else {
            sniper.undo();
        }
        return true;
    }
}
