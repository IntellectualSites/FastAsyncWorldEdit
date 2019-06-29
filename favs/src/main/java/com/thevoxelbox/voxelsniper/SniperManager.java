package com.thevoxelbox.voxelsniper;

import com.google.common.collect.Maps;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class SniperManager {
    private Map<UUID, Sniper> sniperInstances = Maps.newHashMap();
    private VoxelSniper plugin;

    public SniperManager(VoxelSniper plugin) {
        this.plugin = plugin;
    }

    public Sniper getSniperForPlayer(Player player) {
        if (sniperInstances.get(player.getUniqueId()) == null) {
            sniperInstances.put(player.getUniqueId(), new Sniper(plugin, player));
        }
        return sniperInstances.get(player.getUniqueId());
    }
}
