package com.sk89q.worldedit.world.block;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.registry.ItemRegistry;
import com.sk89q.worldedit.world.registry.Registries;

public final class ItemTypesCache {
    public static void init() {}

    static {
        Platform platform = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS);
        Registries registries = platform.getRegistries();
        ItemRegistry itemReg = registries.getItemRegistry();
        for (String key : itemReg.values()) {
            ItemType item = new ItemType(key);
            ItemType.REGISTRY.register(key, item);
        }
    }
}
