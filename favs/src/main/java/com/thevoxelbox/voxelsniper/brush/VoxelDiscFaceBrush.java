package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;


public class VoxelDiscFaceBrush extends PerformBrush {

    public VoxelDiscFaceBrush() {
        this.setName("Voxel Disc Face");
    }

    private void disc(final SnipeData v, Block targetBlock) {
        for (int x = v.getBrushSize(); x >= -v.getBrushSize(); x--) {
            for (int y = v.getBrushSize(); y >= -v.getBrushSize(); y--) {
                this.current.perform(this.clampY(targetBlock.getX() + x, targetBlock.getY(), targetBlock.getZ() + y));
            }
        }

        v.owner().storeUndo(this.current.getUndo());
    }

    private void discNS(final SnipeData v, Block targetBlock) {
        for (int x = v.getBrushSize(); x >= -v.getBrushSize(); x--) {
            for (int y = v.getBrushSize(); y >= -v.getBrushSize(); y--) {
                this.current.perform(this.clampY(targetBlock.getX() + x, targetBlock.getY() + y, targetBlock.getZ()));
            }
        }

        v.owner().storeUndo(this.current.getUndo());
    }

    private void discEW(final SnipeData v, Block targetBlock) {
        for (int x = v.getBrushSize(); x >= -v.getBrushSize(); x--) {
            for (int y = v.getBrushSize(); y >= -v.getBrushSize(); y--) {
                this.current.perform(this.clampY(targetBlock.getX(), targetBlock.getY() + x, targetBlock.getZ() + y));
            }
        }

        v.owner().storeUndo(this.current.getUndo());
    }

    private void pre(final SnipeData v, final BlockFace bf, Block targetBlock) {
        if (bf == null) {
            return;
        }
        switch (bf) {
            case NORTH:
            case SOUTH:
                this.discNS(v, targetBlock);
                break;

            case EAST:
            case WEST:
                this.discEW(v, targetBlock);
                break;

            case UP:
            case DOWN:
                this.disc(v, targetBlock);
                break;

            default:
                break;
        }
    }

    @Override
    protected final void arrow(final SnipeData v) {
        this.pre(v, this.getTargetBlock().getFace(this.getLastBlock()), this.getTargetBlock());
    }

    @Override
    protected final void powder(final SnipeData v) {
        this.pre(v, this.getTargetBlock().getFace(this.getLastBlock()), this.getLastBlock());
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
        vm.size();
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.voxeldiscface";
    }
}
