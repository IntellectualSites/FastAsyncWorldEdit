package com.thevoxelbox.voxelsniper.command;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.thevoxelbox.voxelsniper.RangeBlockHelper;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Sniper;
import com.thevoxelbox.voxelsniper.VoxelSniper;
import com.thevoxelbox.voxelsniper.api.command.VoxelCommand;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class VoxelInkCommand extends VoxelCommand
{
    public VoxelInkCommand(final VoxelSniper plugin)
    {
        super("VoxelInk", plugin);
        setIdentifier("vi");
        setPermission("voxelsniper.sniper");
    }

    @Override
    public boolean onCommand(Player player, String[] args)
    {
        Sniper sniper = plugin.getSniperManager().getSniperForPlayer(player);

        int dataValue;

        if (args.length == 0)
        {
            AsyncBlock targetBlock = new RangeBlockHelper(player, sniper.getWorld()).getTargetBlock();
            if (targetBlock != null)
            {
                dataValue = targetBlock.getPropertyId();
            }
            else
            {
                return true;
            }
        }
        else
        {
            try
            {
                dataValue = Integer.parseInt(args[0]);
            }
            catch (NumberFormatException exception)
            {
                BlockState state = BlockState.get(args[0]);
                if (state == null) {
                    player.sendMessage("Couldn't parse input.");
                    return true;
                } else {
                    dataValue = state.getInternalPropertiesId();
                }
            }
        }

        SnipeData snipeData = sniper.getSnipeData(sniper.getCurrentToolId());
        snipeData.setData(dataValue);
        snipeData.getVoxelMessage().data();
        return true;
    }
}
