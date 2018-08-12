package com.sk89q.worldedit.bukkit;

import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class CachedBukkitAdapter {
    private static final BlockData[][] blockDataCache = new BlockData[BlockTypes.size()][];
    private static final ItemTypes[] itemTypes;
    private static final BlockTypes[] blockTypes;
    static {
        Material[] materials = Material.values();
        itemTypes = new ItemTypes[materials.length];
        blockTypes = new BlockTypes[materials.length];
        for (int i = 0; i < materials.length; i++) {
            Material material = materials[i];
            if (material.isBlock() && !material.isLegacy()) {
                NamespacedKey key = material.getKey();
                blockTypes[i] = BlockTypes.get(key.getNamespace() + ":" + key.getKey());
            } else if (material.isItem() && !material.isLegacy()) {
                itemTypes[i] = ItemTypes.get(material.getKey().toString());
            }
        }
    }

    /**
     * Converts a Material to a ItemType
     *
     * @param material The material
     * @return The itemtype
     */
    public static ItemType asItemType(Material material) {
        return itemTypes[material.ordinal()];
    }

    /**
     * Create a WorldEdit BlockStateHolder from a Bukkit BlockData
     *
     * @param blockData The Bukkit BlockData
     * @return The WorldEdit BlockState
     */
    public static BlockState adapt(BlockData blockData) {
        checkNotNull(blockData);
        Material material = blockData.getMaterial();
        BlockTypes type = blockTypes[material.ordinal()];

        List<? extends Property> propList = type.getProperties();
        if (propList.size() == 0) return type.getDefaultState();
        String properties = blockData.getAsString();

        return BlockState.get(type, properties, type.getDefaultState().getInternalPropertiesId());
    }

    public static BlockData getBlockData(int combinedId) {
        int typeId = combinedId & BlockTypes.BIT_MASK;
        BlockData[] dataCache = blockDataCache[typeId];
        if (dataCache == null) {
            BlockTypes type = BlockTypes.get(typeId);
            blockDataCache[typeId] = dataCache = new BlockData[type.getMaxStateId() + 1];
        }
        int propId = combinedId >> BlockTypes.BIT_OFFSET;
        BlockData blockData = dataCache[propId];
        if (blockData == null) {
            dataCache[propId] = blockData = Bukkit.createBlockData(BlockState.get(combinedId).getAsString());
        }
        return blockData;
    }

    public static BlockTypes adapt(Material material) {
        return blockTypes[material.ordinal()];
    }

    /**
     * Create a Bukkit BlockData from a WorldEdit BlockStateHolder
     *
     * @param block The WorldEdit BlockStateHolder
     * @return The Bukkit BlockData
     */
    public static BlockData adapt(BlockStateHolder block) {
        checkNotNull(block);
        int typeId = block.getInternalBlockTypeId();
        BlockData[] dataCache = blockDataCache[typeId];
        if (dataCache == null) {
            BlockTypes type = BlockTypes.get(typeId);
            blockDataCache[typeId] = dataCache = new BlockData[type.getMaxStateId() + 1];
        }
        int propId = block.getInternalPropertiesId();
        BlockData blockData = dataCache[propId];
        if (blockData == null) {
            dataCache[propId] = blockData = Bukkit.createBlockData(block.getAsString());
        }
        return blockData;
    }
}
