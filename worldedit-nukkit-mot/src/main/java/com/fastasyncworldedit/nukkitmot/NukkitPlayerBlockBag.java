package com.fastasyncworldedit.nukkitmot;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import com.fastasyncworldedit.nukkitmot.mapping.ItemMapping;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.extent.inventory.BlockBagException;
import com.sk89q.worldedit.extent.inventory.OutOfBlocksException;
import com.sk89q.worldedit.extent.inventory.OutOfSpaceException;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.HashMap;
import java.util.Map;

public class NukkitPlayerBlockBag extends BlockBag {

    private final Player player;
    private Map<Integer, Item> items;

    public NukkitPlayerBlockBag(Player player) {
        this.player = player;
    }

    private void loadInventory() {
        if (items == null) {
            items = new HashMap<>(player.getInventory().getContents());
        }
    }

    @Override
    public void fetchBlock(BlockState blockState) throws BlockBagException {
        if (blockState.getBlockType().getMaterial().isAir()) {
            throw new IllegalArgumentException("Can't fetch air block");
        }

        loadInventory();

        BlockType type = blockState.getBlockType();
        if (!type.hasItemType()) {
            throw new OutOfBlocksException();
        }
        ItemMapping.NukkitItemData beData = ItemMapping.jeToBe(type.getItemType().id());
        if (beData.itemId() == 0) {
            throw new OutOfBlocksException();
        }

        for (Map.Entry<Integer, Item> entry : items.entrySet()) {
            Item item = entry.getValue();
            if (item.getId() == beData.itemId()
                    && item.getDamage() == beData.metadata()
                    && item.getCount() > 0) {
                if (item.getCount() == 1) {
                    items.put(entry.getKey(), Item.get(Item.AIR));
                } else {
                    item.setCount(item.getCount() - 1);
                }
                return;
            }
        }
        throw new OutOfBlocksException();
    }

    @Override
    public void storeBlock(BlockState blockState, int amount) throws BlockBagException {
        if (blockState.getBlockType().getMaterial().isAir()) {
            throw new IllegalArgumentException("Can't store air block");
        }
        if (!blockState.getBlockType().hasItemType()) {
            throw new IllegalArgumentException("This block cannot be stored");
        }

        loadInventory();

        BlockType type = blockState.getBlockType();
        ItemMapping.NukkitItemData beData = ItemMapping.jeToBe(type.getItemType().id());

        // Merge into existing stacks first
        for (Map.Entry<Integer, Item> entry : items.entrySet()) {
            if (amount <= 0) {
                return;
            }
            Item item = entry.getValue();
            if (item.getId() == beData.itemId()
                    && item.getDamage() == beData.metadata()
                    && item.getCount() < item.getMaxStackSize()) {
                int space = item.getMaxStackSize() - item.getCount();
                int add = Math.min(space, amount);
                item.setCount(item.getCount() + add);
                amount -= add;
            }
        }

        // Place into empty slots
        for (int slot = 0; slot < 36 && amount > 0; slot++) {
            Item item = items.get(slot);
            if (item == null || item.isNull()) {
                int stackSize = Math.min(amount, 64);
                items.put(slot, Item.get(beData.itemId(), beData.metadata(), stackSize));
                amount -= stackSize;
            }
        }

        if (amount > 0) {
            throw new OutOfSpaceException(blockState.getBlockType());
        }
    }

    @Override
    public void flushChanges() {
        if (items != null) {
            player.getInventory().setContents(items);
            items = null;
        }
    }

    @Override
    public void addSourcePosition(Location pos) {
    }

    @Override
    public void addSingleSourcePosition(Location pos) {
    }

}
