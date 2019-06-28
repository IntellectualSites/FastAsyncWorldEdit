package com.sk89q.worldedit.extent.inventory;

import com.sk89q.worldedit.blocks.BaseItem;

public interface SlottableBlockBag {
    BaseItem getItem(int slot);

    void setItem(int slot, BaseItem block);

    default int size() {
        return 36;
    }

    default int getSelectedSlot() {
        return -1;
    }
}