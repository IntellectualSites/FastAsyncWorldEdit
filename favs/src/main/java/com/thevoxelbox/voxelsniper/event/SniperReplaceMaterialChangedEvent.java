package com.thevoxelbox.voxelsniper.event;

import com.thevoxelbox.voxelsniper.Sniper;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.HandlerList;

public class SniperReplaceMaterialChangedEvent extends SniperMaterialChangedEvent {
    private static final HandlerList handlers = new HandlerList();

    public SniperReplaceMaterialChangedEvent(Sniper sniper, String toolId, BlockData originalMaterial, BlockData newMaterial) {
        super(sniper, toolId, originalMaterial, newMaterial);
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
