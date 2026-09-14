package com.fastasyncworldedit.forge1710.registry;

import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.biome.BiomeData;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.registry.BiomeRegistry;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.BundledRegistries;
import com.sk89q.worldedit.world.registry.ItemRegistry;

public final class Forge1710Registries extends BundledRegistries {

    private static final Forge1710Registries INSTANCE = new Forge1710Registries();

    private final Forge1710BlockRegistry blockRegistry = new Forge1710BlockRegistry();
    private final Forge1710ItemRegistry itemRegistry = new Forge1710ItemRegistry();
    private final BiomeRegistry biomeRegistry = new BiomeRegistry() {
        @Override
        public Component getRichName(BiomeType biomeType) {
            return TextComponent.of(biomeType.id());
        }

        @Override
        @SuppressWarnings("deprecation")
        public BiomeData getData(BiomeType biome) {
            return biome::id;
        }
    };

    private Forge1710Registries() {
    }

    public static Forge1710Registries getInstance() {
        return INSTANCE;
    }

    @Override
    public BlockRegistry getBlockRegistry() {
        return blockRegistry;
    }

    @Override
    public Forge1710ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    @Override
    public BiomeRegistry getBiomeRegistry() {
        return biomeRegistry;
    }

}
