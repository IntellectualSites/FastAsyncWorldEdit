package com.sk89q.worldedit.bukkit.adapter;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;

public abstract class CachedBukkitAdapter implements IBukkitAdapter {
    private int[] itemTypes;
    private int[] blockTypes;

    private boolean init() {
        if (itemTypes == null) {
            Material[] materials = Material.values();
            itemTypes = new int[materials.length];
            blockTypes = new int[materials.length];
            for (int i = 0; i < materials.length; i++) {
                Material material = materials[i];
                if (material.isLegacy()) continue;
                NamespacedKey key = material.getKey();
                String id = key.getNamespace() + ":" + key.getKey();
                if (material.isBlock()) {
                    blockTypes[i] = BlockTypes.get(id).getInternalId();
                }
                if (material.isItem()) {
                    itemTypes[i] = ItemTypes.get(id).getInternalId();
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Converts a Material to a ItemType
     *
     * @param material The material
     * @return The itemtype
     */
    @Override
    public ItemType asItemType(Material material) {
        try {
            return ItemTypes.get(itemTypes[material.ordinal()]);
        } catch (NullPointerException e) {
            if (init()) return asItemType(material);
            return ItemTypes.get(itemTypes[material.ordinal()]);
        }
    }

    @Override
    public BlockType asBlockType(Material material) {
        try {
            return BlockTypesCache.values[blockTypes[material.ordinal()]];
        } catch (NullPointerException e) {
            if (init()) return asBlockType(material);
            throw e;
        }
    }

    /**
     * Create a WorldEdit BlockStateHolder from a Bukkit BlockData
     *
     * @param blockData The Bukkit BlockData
     * @return The WorldEdit BlockState
     */
    @Override
    public BlockState adapt(BlockData blockData) {
        try {
            checkNotNull(blockData);
            Material material = blockData.getMaterial();
            BlockType type = BlockTypes.getFromStateId(blockTypes[material.ordinal()]);
            List<? extends Property> propList = type.getProperties();
            if (propList.size() == 0) return type.getDefaultState();
            String properties = blockData.getAsString();
            return BlockState.get(type, properties, type.getDefaultState());
        } catch (NullPointerException e) {
            if (init()) return adapt(blockData);
            throw e;
        }
    }
}
