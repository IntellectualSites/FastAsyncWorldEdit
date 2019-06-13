package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;

/**
 * @author Kavutop
 */
public class CylinderBrush extends PerformBrush {
    private double trueCircle = 0;

    public CylinderBrush() {
        this.setName("Cylinder");
    }

    private void cylinder(final SnipeData v, Block targetBlock) {
        final int brushSize = v.getBrushSize();
        int yStartingPoint = targetBlock.getY() + v.getcCen();
        int yEndPoint = targetBlock.getY() + v.getVoxelHeight() + v.getcCen();

        if (yEndPoint < yStartingPoint) {
            yEndPoint = yStartingPoint;
        }
        if (yStartingPoint < 0) {
            yStartingPoint = 0;
            v.sendMessage(ChatColor.DARK_PURPLE + "Warning: off-world start position.");
        } else if (yStartingPoint > this.getWorld().getMaxHeight() - 1) {
            yStartingPoint = this.getWorld().getMaxHeight() - 1;
            v.sendMessage(ChatColor.DARK_PURPLE + "Warning: off-world start position.");
        }
        if (yEndPoint < 0) {
            yEndPoint = 0;
            v.sendMessage(ChatColor.DARK_PURPLE + "Warning: off-world end position.");
        } else if (yEndPoint > this.getWorld().getMaxHeight() - 1) {
            yEndPoint = this.getWorld().getMaxHeight() - 1;
            v.sendMessage(ChatColor.DARK_PURPLE + "Warning: off-world end position.");
        }

        final double bSquared = Math.pow(brushSize + this.trueCircle, 2);

        for (int y = yEndPoint; y >= yStartingPoint; y--) {
            for (int x = brushSize; x >= 0; x--) {
                final double xSquared = Math.pow(x, 2);

                for (int z = brushSize; z >= 0; z--) {
                    if ((xSquared + Math.pow(z, 2)) <= bSquared) {
                        this.current.perform(this.clampY(targetBlock.getX() + x, y, targetBlock.getZ() + z));
                        this.current.perform(this.clampY(targetBlock.getX() + x, y, targetBlock.getZ() - z));
                        this.current.perform(this.clampY(targetBlock.getX() - x, y, targetBlock.getZ() + z));
                        this.current.perform(this.clampY(targetBlock.getX() - x, y, targetBlock.getZ() - z));
                    }
                }
            }
        }
        v.owner().storeUndo(this.current.getUndo());
    }

    @Override
    protected final void arrow(final SnipeData v) {
        this.cylinder(v, this.getTargetBlock());
    }

    @Override
    protected final void powder(final SnipeData v) {
        this.cylinder(v, this.getLastBlock());
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
        vm.size();
        vm.height();
        vm.center();
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        for (int i = 1; i < par.length; i++) {
            final String parameter = par[i];

            if (parameter.equalsIgnoreCase("info")) {
                v.sendMessage(ChatColor.GOLD + "Cylinder Brush Parameters:");
                v.sendMessage(ChatColor.AQUA + "/b c h[number] -- set the cylinder v.voxelHeight.  Default is 1.");
                v.sendMessage(ChatColor.DARK_AQUA + "/b c true -- will use a true circle algorithm instead of the skinnier version with classic sniper nubs. /b b false will switch back. (false is default)");
                v.sendMessage(ChatColor.DARK_BLUE + "/b c c[number] -- set the origin of the cylinder compared to the target block. Positive numbers will move the cylinder upward, negative will move it downward.");
                return;
            }
            if (parameter.startsWith("true")) {
                this.trueCircle = 0.5;
                v.sendMessage(ChatColor.AQUA + "True circle mode ON.");
            } else if (parameter.startsWith("false")) {
                this.trueCircle = 0;
                v.sendMessage(ChatColor.AQUA + "True circle mode OFF.");
            } else if (parameter.startsWith("h")) {
                v.setVoxelHeight((int) Double.parseDouble(parameter.replace("h", "")));
                v.sendMessage(ChatColor.AQUA + "Cylinder v.voxelHeight set to: " + v.getVoxelHeight());
            } else if (parameter.startsWith("c")) {
                v.setcCen((int) Double.parseDouble(parameter.replace("c", "")));
                v.sendMessage(ChatColor.AQUA + "Cylinder origin set to: " + v.getcCen());
            } else {
                v.sendMessage(ChatColor.RED + "Invalid brush parameters! Use the info parameter to display parameter info.");
            }
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.cylinder";
    }
}
