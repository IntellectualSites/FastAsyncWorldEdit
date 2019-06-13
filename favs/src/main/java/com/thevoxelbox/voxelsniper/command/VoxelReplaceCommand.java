package com.thevoxelbox.voxelsniper.command;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.thevoxelbox.voxelsniper.RangeBlockHelper;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Sniper;
import com.thevoxelbox.voxelsniper.VoxelSniper;
import com.thevoxelbox.voxelsniper.api.command.VoxelCommand;
import org.bukkit.entity.Player;

public class VoxelReplaceCommand extends VoxelCommand
{
    public VoxelReplaceCommand(final VoxelSniper plugin)
    {
        super("VoxelReplace", plugin);
        setIdentifier("vr");
        setPermission("voxelsniper.sniper");
    }

    @Override
    public boolean onCommand(Player player, String[] args)
    {
        Sniper sniper = plugin.getSniperManager().getSniperForPlayer(player);
        SnipeData snipeData = sniper.getSnipeData(sniper.getCurrentToolId());

        if (args.length == 0)
        {
            AsyncBlock targetBlock = new RangeBlockHelper(player, sniper.getWorld()).getTargetBlock();
            if (targetBlock != null)
            {
                snipeData.setReplaceId(targetBlock.getTypeId());
                snipeData.getVoxelMessage().replace();
            }
            return true;
        }

        BlockType weType = BlockTypes.parse(args[0]);
        if (weType != null)
        {
            snipeData.setReplaceId(weType.getInternalId());
            snipeData.getVoxelMessage().replace();
            return true;
        }
        return false;
    }
}
