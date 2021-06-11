package com.fastasyncworldedit.object.extent;

import com.fastasyncworldedit.FaweCache;
import com.fastasyncworldedit.configuration.Caption;
import com.fastasyncworldedit.util.MemUtil;
import com.fastasyncworldedit.util.Permission;
import com.fastasyncworldedit.util.WEManager;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.PassthroughExtent;

public class MemoryCheckingExtent extends PassthroughExtent {

    private final Player player;

    public MemoryCheckingExtent(Player player, Extent extent) {
        super(extent);
        this.player = player;
    }

    @Override
    public Extent getExtent() {
        if (MemUtil.isMemoryLimited()) {
            if (this.player != null) {
                player.print(Caption.of("fawe.cancel.worldedit.cancel.reason",
                        Caption.of("fawe.cancel.worldedit.cancel.reason.low.memory")));
                if (Permission.hasPermission(this.player, "worldedit.fast")) {
                    this.player.print(Caption.of("fawe.info.worldedit.oom.admin"));
                }
            }
            WEManager.IMP.cancelEdit(this, FaweCache.LOW_MEMORY);
        }
        return super.getExtent();
    }
}
