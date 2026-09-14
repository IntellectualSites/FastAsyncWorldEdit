package com.fastasyncworldedit.forge1710.registry;

import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;

public class Forge1710BlockRegistry implements BlockRegistry {

    private final Map<String, BlockMaterial> materials = new ConcurrentHashMap<>();

    @Override
    public Component getRichName(BlockType blockType) {
        Block block = NativeBlockMapper.get().getBlock(blockType);
        if (block == null) {
            return TextComponent.of(blockType.id());
        }
        try {
            return TextComponent.of(block.getLocalizedName());
        } catch (Throwable t) {
            return TextComponent.of(blockType.id());
        }
    }

    @Nullable
    @Override
    public BlockMaterial getMaterial(BlockType blockType) {
        Block block = NativeBlockMapper.get().getBlock(blockType);
        // FAWE registers ids 1.7.10 does not have (minecraft:__reserved__, cave_air, void_air) and requires a material for
        // every type, so anything without a native block behaves like air.
        Block backing = block != null ? block : Blocks.air;
        return materials.computeIfAbsent(blockType.id(), id -> new Forge1710BlockMaterial(backing));
    }

    @Override
    public Map<String, ? extends Property<?>> getProperties(BlockType blockType) {
        return NativeBlockMapper.get().getProperties(blockType.id());
    }

    @Override
    public OptionalInt getInternalBlockStateId(BlockState state) {
        int nativeId = NativeBlockMapper.get().toNative(state.getOrdinal());
        return nativeId < 0 ? OptionalInt.empty() : OptionalInt.of(nativeId);
    }

    @Override
    public Collection<String> values() {
        return NativeBlockMapper.get().values();
    }

    @Override
    public Map<String, ? extends List<Property<?>>> getAllProperties() {
        return Collections.singletonMap(NativeBlockMapper.META_PROPERTY_NAME,
                Collections.<Property<?>>singletonList(NativeBlockMapper.metaProperty()));
    }

}
