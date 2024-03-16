package com.fastasyncworldedit.core.extent;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.util.MemUtil;
import com.fastasyncworldedit.core.util.Permission;
import com.fastasyncworldedit.core.util.WEManager;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;

public final class MemoryCheckingExtent extends PassthroughExtent {

    private final Actor actor;

    public MemoryCheckingExtent(Actor actor, Extent extent) {
        super(extent);
        this.actor = actor;
    }

    @Override
    public Extent getExtent() {
        if (MemUtil.isMemoryLimited()) {
            if (this.actor != null) {
                actor.print(Caption.of(
                        "fawe.cancel.reason",
                        Caption.of("fawe.cancel.reason.low.memory")
                ));
                if (Permission.hasPermission(this.actor, "worldedit.fast")) {
                    this.actor.print(Caption.of("fawe.info.worldedit.oom.admin"));
                }
            }
            WEManager.weManager().cancelEdit(this, FaweCache.LOW_MEMORY);
        }
        return super.getExtent();
    }

}
