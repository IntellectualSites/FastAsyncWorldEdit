package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;


public class SetBrush extends PerformBrush {
    private static final int SELECTION_SIZE_MAX = 5000000;
    private Block block = null;

    public SetBrush() {
        this.setName("Set");
    }

    private boolean set(final Block bl, final SnipeData v) {
        if (this.block == null) {
            this.block = bl;
            return true;
        } else {
            if (!this.block.getWorld().getName().equals(bl.getWorld().getName())) {
                v.sendMessage(ChatColor.RED + "You selected points in different worlds!");
                this.block = null;
                return true;
            }
            final int lowX = Math.min(this.block.getX(), bl.getX());
            final int lowY = Math.min(this.block.getY(), bl.getY());
            final int lowZ = Math.min(this.block.getZ(), bl.getZ());
            final int highX = Math.max(this.block.getX(), bl.getX());
            final int highY = Math.max(this.block.getY(), bl.getY());
            final int highZ = Math.max(this.block.getZ(), bl.getZ());

            if (Math.abs(highX - lowX) * Math.abs(highZ - lowZ) * Math.abs(highY - lowY) > SELECTION_SIZE_MAX) {
                v.sendMessage(ChatColor.RED + "Selection size above hardcoded limit of 5000000, please use a smaller selection.");
            } else {
                for (int y = lowY; y <= highY; y++) {
                    for (int x = lowX; x <= highX; x++) {
                        for (int z = lowZ; z <= highZ; z++) {
                            this.current.perform(this.clampY(x, y, z));
                        }
                    }
                }
            }

            this.block = null;
            return false;
        }
    }

    @Override
    protected final void arrow(final SnipeData v) {
        if (this.set(this.getTargetBlock(), v)) {
            v.sendMessage(ChatColor.GRAY + "Point one");
        } else {
            v.owner().storeUndo(this.current.getUndo());
        }
    }

    @Override
    protected final void powder(final SnipeData v) {
        if (this.set(this.getLastBlock(), v)) {
            v.sendMessage(ChatColor.GRAY + "Point one");
        } else {
            v.owner().storeUndo(this.current.getUndo());
        }
    }

    @Override
    public final void info(final Message vm) {
        this.block = null;
        vm.brushName(this.getName());
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        super.parameters(par, v);
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.set";
    }
}
