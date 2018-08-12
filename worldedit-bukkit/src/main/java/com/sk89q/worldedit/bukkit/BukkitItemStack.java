package com.sk89q.worldedit.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.bukkit.util.ItemUtil;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.world.item.ItemType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;

public class BukkitItemStack extends BaseItemStack {
    private ItemStack stack;
    private boolean loadedNBT;

    public BukkitItemStack(ItemStack stack) {
        super(BukkitAdapter.asItemType(stack.getType()));
        this.stack = stack;
    }

    public BukkitItemStack(ItemType type, ItemStack stack) {
        super(type);
        this.stack = stack;
    }

    @Override
    public int getAmount() {
        return stack.getAmount();
    }

    @Nullable
    @Override
    public Object getNativeItem() {
        return super.getNativeItem();
    }

    @Nullable
    @Override
    public CompoundTag getNbtData() {
        if (!loadedNBT) {
            loadedNBT = true;
            ItemUtil util = Fawe.<FaweBukkit>imp().getItemUtil();
            if (util != null) {
                super.setNbtData(util.getNBT(stack));
            }
        }
        return super.getNbtData();
    }

    @Override
    public void setNbtData(@Nullable CompoundTag nbtData) {
        ItemUtil util = Fawe.<FaweBukkit>imp().getItemUtil();
        if (util != null) {
            stack = util.setNBT(stack, nbtData);
        }
        super.setNbtData(nbtData);
    }
}
