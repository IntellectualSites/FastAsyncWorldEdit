package com.thevoxelbox.voxelsniper.command;

import com.thevoxelbox.voxelsniper.SnipeAction;
import com.thevoxelbox.voxelsniper.Sniper;
import com.thevoxelbox.voxelsniper.VoxelSniper;
import com.thevoxelbox.voxelsniper.api.command.VoxelCommand;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class VoxelBrushToolCommand extends VoxelCommand {
    public VoxelBrushToolCommand(final VoxelSniper plugin) {
        super("VoxelBrushTool", plugin);
        setIdentifier("btool");
        setPermission("voxelsniper.sniper");
    }

    @Override
    public boolean onCommand(Player player, String[] args) {
        Sniper sniper = plugin.getSniperManager().getSniperForPlayer(player);

        if (args != null && args.length > 0) {
            if (args[0].equalsIgnoreCase("assign")) {
                SnipeAction action;
                if (args[1].equalsIgnoreCase("arrow")) {
                    action = SnipeAction.ARROW;
                } else if (args[1].equalsIgnoreCase("powder")) {
                    action = SnipeAction.GUNPOWDER;
                } else {
                    player.sendMessage("/btool assign <arrow|powder> <toolid>");
                    return true;
                }

                if (args.length == 3 && args[2] != null && !args[2].isEmpty()) {
                    Material itemInHand = player.getItemInHand().getType();
                    if (sniper.setTool(args[2], action, itemInHand)) {
                        player.sendMessage(itemInHand.name() + " has been assigned to '" + args[2] + "' as action " + action.name() + ".");
                    } else {
                        player.sendMessage("Couldn't assign tool.");
                    }
                    return true;
                }
            } else if (args[0].equalsIgnoreCase("remove")) {
                if (args.length == 2 && args[1] != null && !args[1].isEmpty()) {
                    sniper.removeTool(args[1]);
                    return true;
                } else {
                    Material itemInHand = player.getItemInHand().getType();
                    if (sniper.getCurrentToolId() == null) {
                        player.sendMessage("Can't unassign default tool.");
                        return true;
                    }
                    sniper.removeTool(sniper.getCurrentToolId(), itemInHand);
                    return true;
                }
            }
        }
        player.sendMessage("/btool assign <arrow|powder> <toolid>");
        player.sendMessage("/btool remove [toolid]");
        return true;
    }
}
