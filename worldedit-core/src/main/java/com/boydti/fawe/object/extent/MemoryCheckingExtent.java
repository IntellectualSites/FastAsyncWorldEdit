package com.boydti.fawe.object.extent;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.Permission;
import com.boydti.fawe.util.WEManager;
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
                player.print(BBC.WORLDEDIT_CANCEL_REASON.format(BBC.WORLDEDIT_CANCEL_REASON_LOW_MEMORY.s()));
                if (Permission.hasPermission(this.player, "worldedit.fast")) {
                    BBC.WORLDEDIT_OOM_ADMIN.send(this.player);
                }
            }
            WEManager.IMP.cancelEdit(this, FaweCache.LOW_MEMORY);
        }
        return super.getExtent();
    }
}
