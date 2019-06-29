package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public class DiscBrush extends PerformBrush {
    private double trueCircle = 0;

    /**
     * Default Constructor.
     */
    public DiscBrush() {
        this.setName("Disc");
    }

    /**
     * Disc executor.
     *
     * @param v Snipe Data
     */
    private void disc(final SnipeData v, final Block targetBlock) {
        final double radiusSquared = (v.getBrushSize() + this.trueCircle) * (v.getBrushSize() + this.trueCircle);
        final Vector centerPoint = targetBlock.getLocation().toVector();
        final Vector currentPoint = centerPoint.clone();

        for (int x = -v.getBrushSize(); x <= v.getBrushSize(); x++) {
            currentPoint.setX(centerPoint.getX() + x);
            for (int z = -v.getBrushSize(); z <= v.getBrushSize(); z++) {
                currentPoint.setZ(centerPoint.getZ() + z);
                if (centerPoint.distanceSquared(currentPoint) <= radiusSquared) {
                    this.current.perform(this.clampY(currentPoint.getBlockX(), currentPoint.getBlockY(), currentPoint.getBlockZ()));
                }
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
    public final void parameters(final String[] par, final SnipeData v) {
        for (int i = 1; i < par.length; i++) {
            final String parameter = par[i].toLowerCase();

            if (parameter.equalsIgnoreCase("info")) {
                v.sendMessage(ChatColor.GOLD + "Disc Brush Parameters:");
                v.sendMessage(ChatColor.AQUA + "/b d true|false" + " -- toggles useing the true circle algorithm instead of the skinnier version with classic sniper nubs. (false is default)");
                return;
            } else if (parameter.startsWith("true")) {
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
        return "voxelsniper.brush.disc";
    }
}
