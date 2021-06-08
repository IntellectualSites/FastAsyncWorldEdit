package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.regions.FaweMaskManager;
import org.bukkit.permissions.Permissible;

public abstract class BukkitMaskManager extends FaweMaskManager {

    public BukkitMaskManager(final String plugin) {
        super(plugin);
    }

    public boolean hasMemberPermission(Permissible player) {
        return player.hasPermission("fawe." + getKey() + ".member");
    }
}
