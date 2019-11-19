package com.sk89q.worldedit.bukkit;

import com.sk89q.worldedit.world.registry.BundledItemRegistry;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.Collection;

public class BukkitItemRegistry extends BundledItemRegistry {
    @Override
    public Collection<String> values() {
        ArrayList<String> values = new ArrayList<>();
        for (Material m : Material.values()) {
            if (!m.isLegacy() && (m.isBlock() || m.isItem())) {
                String id = m.getKey().toString();
                values.add(id);
            }
        }
        return values;
    }
}
