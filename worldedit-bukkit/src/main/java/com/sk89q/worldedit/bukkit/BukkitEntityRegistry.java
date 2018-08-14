package com.sk89q.worldedit.bukkit;

import com.sk89q.worldedit.world.registry.EntityRegistry;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BukkitEntityRegistry implements EntityRegistry {
    @Override
    public Collection<String> registerEntities() {
        List<String> types = new ArrayList<>();
        for (EntityType type : EntityType.values()) {
            String name = type.getName();
            if (name != null) {
                if (name.indexOf(':') == -1) name = "minecraft:" + name;
                types.add(name);
            }
        }
        return types;
    }
}