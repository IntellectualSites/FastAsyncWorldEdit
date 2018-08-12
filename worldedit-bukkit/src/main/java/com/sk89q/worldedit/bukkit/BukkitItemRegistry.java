package com.sk89q.worldedit.bukkit;

import com.sk89q.worldedit.world.registry.BundledItemRegistry;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collection;

public class BukkitItemRegistry extends BundledItemRegistry {
    @Override
    public Collection<String> registerItems() {
        ArrayList<String> items = new ArrayList<>();
        for (Material m : Material.values()) {
            if (!m.isLegacy() && m.isItem()) items.add(m.getKey().getNamespace() + ":" + m.getKey().getKey());
        }
        return items;
    }
}
