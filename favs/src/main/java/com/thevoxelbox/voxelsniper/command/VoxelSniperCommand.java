package com.thevoxelbox.voxelsniper.command;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Sniper;
import com.thevoxelbox.voxelsniper.VoxelSniper;
import com.thevoxelbox.voxelsniper.brush.IBrush;
import com.thevoxelbox.voxelsniper.api.command.VoxelCommand;
import com.thevoxelbox.voxelsniper.brush.perform.PerformerE;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class VoxelSniperCommand extends VoxelCommand
{

    public VoxelSniperCommand(final VoxelSniper plugin)
    {

        super("VoxelSniper", plugin);
        setIdentifier("vs");
        setPermission("voxelsniper.sniper");
    }

    @Override
    public boolean onCommand(Player player, String[] args)
    {
        Sniper sniper = VoxelSniper.getInstance().getSniperManager().getSniperForPlayer(player);

        if (args.length >= 1)
        {
            if (args[0].equalsIgnoreCase("brushes"))
            {
                Multimap<Class<? extends IBrush>, String> registeredBrushesMultimap = VoxelSniper.getInstance().getBrushManager().getRegisteredBrushesMultimap();
                List<String> allHandles = Lists.newLinkedList();
                for (Class<? extends IBrush> brushClass : registeredBrushesMultimap.keySet())
                {
                    allHandles.addAll(registeredBrushesMultimap.get(brushClass));
                }
                player.sendMessage(Joiner.on(", ").skipNulls().join(allHandles));
                return true;
            }
            else if (args[0].equalsIgnoreCase("range"))
            {
                SnipeData snipeData = sniper.getSnipeData(sniper.getCurrentToolId());
                if (args.length == 2)
                {
                    try
                    {
                        int range = Integer.parseInt(args[1]);
                        if (range < 0)
                        {
                            player.sendMessage("Negative values are not allowed.");
                        }
                        snipeData.setRange(range);
                        snipeData.setRanged(true);
                        snipeData.getVoxelMessage().toggleRange();

                    }
                    catch (NumberFormatException exception)
                    {
                        player.sendMessage("Can't parse number.");
                    }
                    return true;
                }
                else
                {
                    snipeData.setRanged(!snipeData.isRanged());
                    snipeData.getVoxelMessage().toggleRange();
                    return true;
                }
            }
            else if (args[0].equalsIgnoreCase("perf"))
            {
                player.sendMessage(ChatColor.AQUA + "Available performers (abbreviated):");
                player.sendMessage(PerformerE.performer_list_short);
                return true;
            }
            else if (args[0].equalsIgnoreCase("perflong"))
            {
                player.sendMessage(ChatColor.AQUA + "Available performers:");
                player.sendMessage(PerformerE.performer_list_long);
                return true;
            }
            else if (args[0].equalsIgnoreCase("enable"))
            {
                sniper.setEnabled(true);
                player.sendMessage("VoxelSniper is " + (sniper.isEnabled() ? "enabled" : "disabled"));
                return true;
            }
            else if (args[0].equalsIgnoreCase("disable"))
            {
                sniper.setEnabled(false);
                player.sendMessage("VoxelSniper is " + (sniper.isEnabled() ? "enabled" : "disabled"));
                return true;
            }
            else if (args[0].equalsIgnoreCase("toggle"))
            {
                sniper.setEnabled(!sniper.isEnabled());
                player.sendMessage("VoxelSniper is " + (sniper.isEnabled() ? "enabled" : "disabled"));
                return true;
            }
        }
        player.sendMessage(ChatColor.DARK_RED + "VoxelSniper - Current Brush Settings:");
        sniper.displayInfo();
        return true;
    }
}
