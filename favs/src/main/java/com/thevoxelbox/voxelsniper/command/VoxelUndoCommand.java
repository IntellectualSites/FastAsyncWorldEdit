package com.thevoxelbox.voxelsniper.command;

import com.thevoxelbox.voxelsniper.Sniper;
import com.thevoxelbox.voxelsniper.VoxelSniper;
import com.thevoxelbox.voxelsniper.api.command.VoxelCommand;
import org.bukkit.entity.Player;

public class VoxelUndoCommand extends VoxelCommand
{
    public VoxelUndoCommand(final VoxelSniper plugin)
    {
        super("VoxelUndo", plugin);
        setIdentifier("u");
        setPermission("voxelsniper.sniper");
    }

    @Override
    public boolean onCommand(Player player, String[] args)
    {
        Sniper sniper = plugin.getSniperManager().getSniperForPlayer(player);

        if (args.length == 1)
        {
            try
            {
                int amount = Integer.parseInt(args[0]);
                sniper.undo(amount);
            }
            catch (NumberFormatException exception)
            {
                player.sendMessage("Error while parsing amount of undo. Number format exception.");
            }
        }
        else
        {
            sniper.undo();
        }
        plugin.getLogger().info("Player \"" + player.getName() + "\" used /u");
        return true;
    }
}
