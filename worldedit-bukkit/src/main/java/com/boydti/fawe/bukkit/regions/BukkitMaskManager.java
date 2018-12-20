package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.regions.FaweMaskManager;
import org.bukkit.entity.Player;

public abstract class BukkitMaskManager extends FaweMaskManager<Player> {

    public BukkitMaskManager(final String plugin) {
        super(plugin);
    }

    public boolean hasMemberPermission(Player player) {
        return player.hasPermission("fawe." + getKey() + ".member");
    }
}
