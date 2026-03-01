package com.sk89q.worldedit.nukkitmot;

import com.fastasyncworldedit.nukkitmot.mapping.BlockMapping;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.BundledBlockRegistry;

import java.util.Collection;
import javax.annotation.Nullable;

/**
 * Nukkit block registry that extends the bundled registry with Nukkit-specific material data.
 */
class NukkitBlockRegistry extends BundledBlockRegistry {

    @Nullable
    @Override
    public BlockMaterial getMaterial(BlockType blockType) {
        // Use bundled data; Nukkit-specific material is provided by NukkitBlockMaterial when needed
        return super.getMaterial(blockType);
    }

    @Override
    public Collection<String> values() {
        return BlockMapping.getAllJeBlockDefaultStates();
    }

}
