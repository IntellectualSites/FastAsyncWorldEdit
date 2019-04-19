package com.thevoxelbox.voxelsniper.command;

import com.thevoxelbox.voxelsniper.VoxelSniper;
import com.thevoxelbox.voxelsniper.api.command.VoxelCommand;
import org.bukkit.entity.Player;

public class VoxelChunkCommand extends VoxelCommand {
    public VoxelChunkCommand(final VoxelSniper plugin) {
        super("VoxelChunk", plugin);
        setIdentifier("vchunk");
    }

    @Override
    public boolean onCommand(Player player, String[] args) {
        player.getWorld().refreshChunk(player.getLocation().getBlockX(), player.getLocation().getBlockZ());
        return true;
    }
}
