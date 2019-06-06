package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockFace;

/**
 * @author Voxel
 */
public class DiscFaceBrush extends PerformBrush {
    private double trueCircle = 0;

    /**
     *
     */
    public DiscFaceBrush() {
        this.setName("Disc Face");
    }

    private void discUD(final SnipeData v, AsyncBlock targetBlock) {
        final int brushSize = v.getBrushSize();
        final double brushSizeSquared = Math.pow(brushSize + this.trueCircle, 2);

        for (int x = brushSize; x >= 0; x--) {
            final double xSquared = Math.pow(x, 2);

            for (int z = brushSize; z >= 0; z--) {
                if ((xSquared + Math.pow(z, 2)) <= brushSizeSquared) {
                    current.perform(targetBlock.getRelative(x, 0, z));
                    current.perform(targetBlock.getRelative(x, 0, -z));
                    current.perform(targetBlock.getRelative(-x, 0, z));
                    current.perform(targetBlock.getRelative(-x, 0, -z));
                }
            }
        }

        v.owner().storeUndo(this.current.getUndo());
    }

    private void discNS(final SnipeData v, AsyncBlock targetBlock) {
        final int brushSize = v.getBrushSize();
        final double brushSizeSquared = Math.pow(brushSize + this.trueCircle, 2);

        for (int x = brushSize; x >= 0; x--) {
            final double xSquared = Math.pow(x, 2);
            for (int y = brushSize; y >= 0; y--) {
                if ((xSquared + Math.pow(y, 2)) <= brushSizeSquared) {
                    current.perform(targetBlock.getRelative(x, y, 0));
                    current.perform(targetBlock.getRelative(x, -y, 0));
                    current.perform(targetBlock.getRelative(-x, y, 0));
                    current.perform(targetBlock.getRelative(-x, -y, 0));
                }
            }
        }

        v.owner().storeUndo(this.current.getUndo());
    }

    private void discEW(final SnipeData v, AsyncBlock targetBlock) {
        final int brushSize = v.getBrushSize();
        final double brushSizeSquared = Math.pow(brushSize + this.trueCircle, 2);

        for (int x = brushSize; x >= 0; x--) {
            final double xSquared = Math.pow(x, 2);
            for (int y = brushSize; y >= 0; y--) {
                if ((xSquared + Math.pow(y, 2)) <= brushSizeSquared) {
                    current.perform(targetBlock.getRelative(0, x, y));
                    current.perform(targetBlock.getRelative(0, x, -y));
                    current.perform(targetBlock.getRelative(0, -x, y));
                    current.perform(targetBlock.getRelative(0, -x, -y));
                }
            }
        }

        v.owner().storeUndo(this.current.getUndo());
    }

    private void pre(final SnipeData v, AsyncBlock targetBlock) {
        BlockFace blockFace = getTargetBlock().getFace(this.getLastBlock());
        if (blockFace == null) {
            return;
        }
        switch (blockFace) {
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
                this.discUD(v, targetBlock);
                break;

            default:
                break;
        }
    }

    @Override
    protected final void arrow(final SnipeData v) {
        this.pre(v, this.getTargetBlock());
    }

    @Override
    protected final void powder(final SnipeData v) {
        this.pre(v, this.getLastBlock());
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
        vm.size();
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        for (int i = 1; i < par.length; i++) {
            final String parameter = par[i];

            if (parameter.equalsIgnoreCase("info")) {
                v.sendMessage(ChatColor.GOLD + "Disc Face brush Parameters:");
                v.sendMessage(ChatColor.AQUA + "/b df true -- will use a true circle algorithm instead of the skinnier version with classic sniper nubs. /b b false will switch back. (false is default)");
                return;
            }
            if (parameter.startsWith("true")) {
                this.trueCircle = 0.5;
                v.sendMessage(ChatColor.AQUA + "True circle mode ON.");
            } else if (parameter.startsWith("false")) {
                this.trueCircle = 0;
                v.sendMessage(ChatColor.AQUA + "True circle mode OFF.");
            } else {
                v.sendMessage(ChatColor.RED + "Invalid brush parameters! Use the info parameter to display parameter info.");
            }
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.discface";
    }
}
