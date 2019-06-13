package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;

/**
 * @author Voxel
 */
public class VoxelDiscBrush extends PerformBrush {

    public VoxelDiscBrush() {
        this.setName("Voxel Disc");
    }

    private void disc(final SnipeData v, AsyncBlock targetBlock) {
        for (int x = v.getBrushSize(); x >= -v.getBrushSize(); x--) {
            for (int z = v.getBrushSize(); z >= -v.getBrushSize(); z--) {
                current.perform(targetBlock.getRelative(x, 0, z));
            }
        }
        v.owner().storeUndo(this.current.getUndo());
    }

    @Override
    protected final void arrow(final SnipeData v) {
        this.disc(v, this.getTargetBlock());
    }

    @Override
    protected final void powder(final SnipeData v) {
        this.disc(v, this.getLastBlock());
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
        vm.size();
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.voxeldisc";
    }
}
