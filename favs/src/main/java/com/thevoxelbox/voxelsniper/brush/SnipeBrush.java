package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;

/**
 * @author Voxel
 */
public class SnipeBrush extends PerformBrush {
    /**
     *
     */
    public SnipeBrush() {
        this.setName("Snipe");
    }

    @Override
    protected final void arrow(final SnipeData v) {
        this.current.perform(this.getTargetBlock());
        v.owner().storeUndo(this.current.getUndo());
    }

    @Override
    protected final void powder(final SnipeData v) {
        this.current.perform(this.getLastBlock());
        v.owner().storeUndo(this.current.getUndo());
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.snipe";
    }
}
