package com.thevoxelbox.voxelsniper.command;

import com.thevoxelbox.voxelsniper.PaintingWrapper;
import com.thevoxelbox.voxelsniper.VoxelSniper;
import com.thevoxelbox.voxelsniper.api.command.VoxelCommand;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class VoxelPaintCommand extends VoxelCommand {
    public VoxelPaintCommand(final VoxelSniper plugin) {
        super("VoxelPaint", plugin);
        setIdentifier("paint");
        setPermission("voxelsniper.paint");
    }

    @Override
    public boolean onCommand(Player player, String[] args) {
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("back")) {
                PaintingWrapper.paint(player, true, true, 0);
                return true;
            } else {
                try {
                    PaintingWrapper.paint(player, false, false, Integer.parseInt(args[0]));
                    return true;
                } catch (final Exception exception) {
                    player.sendMessage(ChatColor.RED + "Invalid input.");
                    return true;
                }
            }
        } else {
            PaintingWrapper.paint(player, true, false, 0);
            return true;
        }
    }
}
