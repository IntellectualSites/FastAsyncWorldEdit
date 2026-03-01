package com.sk89q.worldedit.nukkitmot;

import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.BundledRegistries;
import com.sk89q.worldedit.world.registry.ItemRegistry;

/**
 * Nukkit platform registries, extending BundledRegistries for base functionality.
 */
class NukkitRegistries extends BundledRegistries {

    private static final NukkitRegistries INSTANCE = new NukkitRegistries();
    private final BlockRegistry blockRegistry = new NukkitBlockRegistry();
    private final ItemRegistry itemRegistry = new NukkitItemRegistry();

    NukkitRegistries() {
    }

    @Override
    public BlockRegistry getBlockRegistry() {
        return blockRegistry;
    }

    @Override
    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    public static NukkitRegistries getInstance() {
        return INSTANCE;
    }

}
