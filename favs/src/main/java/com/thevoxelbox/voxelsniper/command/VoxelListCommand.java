package com.thevoxelbox.voxelsniper.command;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.BlockMaskBuilder;
import com.thevoxelbox.voxelsniper.RangeBlockHelper;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Sniper;
import com.thevoxelbox.voxelsniper.VoxelSniper;
import com.thevoxelbox.voxelsniper.api.command.VoxelCommand;
import org.bukkit.entity.Player;

public class VoxelListCommand extends VoxelCommand {
    public VoxelListCommand(final VoxelSniper plugin) {
        super("VoxelList", plugin);
        setIdentifier("vl");
        setPermission("voxelsniper.sniper");
    }

    @Override
    public boolean onCommand(Player player, String[] args) {
        Sniper sniper = plugin.getSniperManager().getSniperForPlayer(player);

        SnipeData snipeData = sniper.getSnipeData(sniper.getCurrentToolId());
        if (args.length == 0) {
            final RangeBlockHelper rangeBlockHelper = new RangeBlockHelper(player, sniper.getWorld());
            final AsyncBlock targetBlock = rangeBlockHelper.getTargetBlock();
            snipeData.getVoxelList().add(BukkitAdapter.adapt(targetBlock.getBlockData()));
            snipeData.getVoxelMessage().voxelList();
            return true;
        } else {
            if (args[0].equalsIgnoreCase("clear")) {
                snipeData.getVoxelList().clear();
                snipeData.getVoxelMessage().voxelList();
                return true;
            }
        }

        for (String string : args) {
            boolean remove = false;
            if (string.charAt(0) == '-') {
                string = string.substring(1);
                remove = true;
            }
            BlockMaskBuilder builder = new BlockMaskBuilder().addRegex(string);
            BlockMask mask = builder.build(new NullExtent());
            if (remove) {
                snipeData.getVoxelList().removeValue(mask);
            } else {
                snipeData.getVoxelList().add(mask);
            }
        }
        return true;
    }
}
