package com.boydti.fawe.object.extent;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.Permission;
import com.boydti.fawe.util.WEManager;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class MemoryCheckingExtent extends AbstractDelegateExtent {
    private final FawePlayer<?> player;

    public MemoryCheckingExtent(final FawePlayer<?> player, final Extent extent) {
        super(extent);
        this.player = player;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(final BlockVector3 location, final B block) throws WorldEditException {
        if (super.setBlock(location, block)) {
            if (MemUtil.isMemoryLimited()) {
                if (this.player != null) {
                    player.sendMessage(BBC.WORLDEDIT_CANCEL_REASON.format(BBC.WORLDEDIT_CANCEL_REASON_LOW_MEMORY.s()));
                    if (Permission.hasPermission(this.player.toWorldEditPlayer(), "worldedit.fast")) {
                        BBC.WORLDEDIT_OOM_ADMIN.send(this.player);
                    }
                }
                WEManager.IMP.cancelEdit(this, FaweException.LOW_MEMORY);
                return false;
            }
            return true;
        }
        return false;
    }
}
