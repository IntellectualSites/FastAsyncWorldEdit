package com.thevoxelbox.voxelsniper.command;

import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Sniper;
import com.thevoxelbox.voxelsniper.VoxelSniper;
import com.thevoxelbox.voxelsniper.api.command.VoxelCommand;
import com.thevoxelbox.voxelsniper.brush.IBrush;
import com.thevoxelbox.voxelsniper.brush.perform.Performer;
import com.thevoxelbox.voxelsniper.event.SniperBrushChangedEvent;
import com.thevoxelbox.voxelsniper.event.SniperBrushSizeChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class VoxelBrushCommand extends VoxelCommand {
    public VoxelBrushCommand(final VoxelSniper plugin) {
        super("VoxelBrush", plugin);
        setIdentifier("b");
        setPermission("voxelsniper.sniper");
    }

    @Override
    public boolean onCommand(Player player, String[] args) {
        Sniper sniper = plugin.getSniperManager().getSniperForPlayer(player);
        String currentToolId = sniper.getCurrentToolId();
        SnipeData snipeData = sniper.getSnipeData(currentToolId);

        if (args == null || args.length == 0) {
            sniper.previousBrush(currentToolId);
            sniper.displayInfo();
            return true;
        } else if (args.length > 0) {
            try {
                int newBrushSize = Integer.parseInt(args[0]);
                if (!player.hasPermission("voxelsniper.ignorelimitations") && newBrushSize > plugin.getVoxelSniperConfiguration().getLiteSniperMaxBrushSize()) {
                    player.sendMessage("Size is restricted to " + plugin.getVoxelSniperConfiguration().getLiteSniperMaxBrushSize() + " for you.");
                    newBrushSize = plugin.getVoxelSniperConfiguration().getLiteSniperMaxBrushSize();
                }
                int originalSize = snipeData.getBrushSize();
                snipeData.setBrushSize(newBrushSize);
                SniperBrushSizeChangedEvent event = new SniperBrushSizeChangedEvent(sniper, currentToolId, originalSize, snipeData.getBrushSize());
                Bukkit.getPluginManager().callEvent(event);
                snipeData.getVoxelMessage().size();
                return true;
            } catch (NumberFormatException ingored) {
            }

            Class<? extends IBrush> brush = plugin.getBrushManager().getBrushForHandle(args[0]);
            if (brush != null) {
                IBrush orignalBrush = sniper.getBrush(currentToolId);
                sniper.setBrush(currentToolId, brush);

                if (args.length > 1) {
                    IBrush currentBrush = sniper.getBrush(currentToolId);
                    if (currentBrush instanceof Performer) {
                        String[] parameters = Arrays.copyOfRange(args, 1, args.length);
                        ((Performer) currentBrush).parse(parameters, snipeData);
                        return true;
                    } else {
                        String[] parameters = hackTheArray(Arrays.copyOfRange(args, 1, args.length));
                        currentBrush.parameters(parameters, snipeData);
                        return true;
                    }
                }
                SniperBrushChangedEvent event = new SniperBrushChangedEvent(sniper, currentToolId, orignalBrush, sniper.getBrush(currentToolId));
                sniper.displayInfo();
                return true;
            } else {
                player.sendMessage("Couldn't find Brush for brush handle \"" + args[0] + "\"");
                return true;
            }
        }
        return false;
    }
}
