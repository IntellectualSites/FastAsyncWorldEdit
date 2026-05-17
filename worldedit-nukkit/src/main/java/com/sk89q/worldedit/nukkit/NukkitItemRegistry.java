package com.sk89q.worldedit.nukkit;

import com.fastasyncworldedit.nukkit.mapping.ItemMapping;
import com.sk89q.worldedit.world.registry.BundledItemRegistry;

import java.util.Collection;

/**
 * Nukkit item registry that provides all known JE item IDs from the mapping data.
 */
class NukkitItemRegistry extends BundledItemRegistry {

    @Override
    public Collection<String> values() {
        return ItemMapping.getAllJeItemIds();
    }

}
