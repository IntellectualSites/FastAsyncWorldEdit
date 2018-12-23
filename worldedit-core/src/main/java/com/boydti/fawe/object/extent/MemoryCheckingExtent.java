package com.boydti.fawe.object.extent;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.Perm;
import com.boydti.fawe.util.WEManager;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
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
    public boolean setBlock(final BlockVector3 location, final BlockStateHolder block) throws WorldEditException {
        if (super.setBlock(location, block)) {
            if (MemUtil.isMemoryLimited()) {
                if (this.player != null) {
                    player.sendMessage(BBC.WORLDEDIT_CANCEL_REASON.format(BBC.WORLDEDIT_CANCEL_REASON_LOW_MEMORY.s()));
                    if (Perm.hasPermission(this.player, "worldedit.fast")) {
                        BBC.WORLDEDIT_OOM_ADMIN.send(this.player);
                    }
                }
                WEManager.IMP.cancelEdit(this, BBC.WORLDEDIT_CANCEL_REASON_LOW_MEMORY);
                return false;
            }
            return true;
        }
        return false;
    }
}
