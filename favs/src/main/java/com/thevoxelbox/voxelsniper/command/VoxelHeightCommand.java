package com.thevoxelbox.voxelsniper.command;

import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Sniper;
import com.thevoxelbox.voxelsniper.VoxelSniper;
import com.thevoxelbox.voxelsniper.api.command.VoxelCommand;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class VoxelHeightCommand extends VoxelCommand {
    public VoxelHeightCommand(final VoxelSniper plugin) {
        super("VoxelHeight", plugin);
        setIdentifier("vh");
        setPermission("voxelsniper.sniper");
    }

    @Override
    public boolean onCommand(Player player, String[] args) {
        Sniper sniper = plugin.getSniperManager().getSniperForPlayer(player);
        SnipeData snipeData = sniper.getSnipeData(sniper.getCurrentToolId());

        try {
            int height = Integer.parseInt(args[0]);
            snipeData.setVoxelHeight(height);
            snipeData.getVoxelMessage().height();
            return true;
        } catch (final Exception exception) {
            player.sendMessage(ChatColor.RED + "Invalid input.");
            return true;
        }
    }
}
