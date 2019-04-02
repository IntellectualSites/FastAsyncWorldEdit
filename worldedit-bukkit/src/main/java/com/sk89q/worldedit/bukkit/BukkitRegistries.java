/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.bukkit;

import com.sk89q.worldedit.world.registry.*;

/**
 * World data for the Bukkit platform.
 */
class BukkitRegistries extends BundledRegistries {

    private static final BukkitRegistries INSTANCE = new BukkitRegistries();
    private final BlockRegistry blockRegistry = new BukkitBlockRegistry();
    private final ItemRegistry itemRegistry = new BukkitItemRegistry();
    private final BiomeRegistry biomeRegistry = new BukkitBiomeRegistry();
    private final EntityRegistry entityRegistry = new BukkitEntityRegistry();
    private final BlockCategoryRegistry blockCategoryRegistry = new BukkitBlockCategoryRegistry();
    private final ItemCategoryRegistry itemCategoryRegistry = new BukkitItemCategoryRegistry();

    /**
     * Create a new instance.
     */
    BukkitRegistries() {
    }

    @Override
    public BlockRegistry getBlockRegistry() {
        return blockRegistry;
    }
    
    @Override
    public BlockCategoryRegistry getBlockCategoryRegistry() {
    	return blockCategoryRegistry;
    }

    @Override
    public BiomeRegistry getBiomeRegistry() {
        return biomeRegistry;
    }

    @Override
    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }
    
    @Override
    public ItemCategoryRegistry getItemCategoryRegistry() {
    	return itemCategoryRegistry;
    }

    @Override
    public EntityRegistry getEntityRegistry() {
        return entityRegistry;
    }

    /**
     * Get a static instance.
     *
     * @return an instance
     */
    public static BukkitRegistries getInstance() {
        return INSTANCE;
    }

}
