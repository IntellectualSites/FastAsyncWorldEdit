package com.fastasyncworldedit.bukkit.regions;

import com.fastasyncworldedit.core.regions.FaweMaskManager;
import org.bukkit.permissions.Permissible;

public abstract class BukkitMaskManager extends FaweMaskManager {

    public BukkitMaskManager(final String plugin) {
        super(plugin);
    }

    public boolean hasMemberPermission(Permissible player) {
        return player.hasPermission("fawe." + getKey() + ".member");
    }

}
