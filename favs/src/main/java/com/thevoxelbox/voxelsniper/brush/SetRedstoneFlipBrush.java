package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;

/**
 * @author Voxel
 */
public class SetRedstoneFlipBrush extends Brush {
    private Block block = null;
    private Undo undo;
    private boolean northSouth = true;

    /**
     *
     */
    public SetRedstoneFlipBrush() {
        this.setName("Set Redstone Flip");
    }

    private boolean set(final Block bl) {
        if (this.block == null) {
            this.block = bl;
            return true;
        } else {
            this.undo = new Undo();
            final int lowX = Math.min(this.block.getX(), bl.getX());
            final int lowY = Math.min(this.block.getY(), bl.getY());
            final int lowZ = Math.min(this.block.getZ(), bl.getZ());
            final int highX = Math.max(this.block.getX(), bl.getX());
            final int highY = Math.max(this.block.getY(), bl.getY());
            final int highZ = Math.max(this.block.getZ(), bl.getZ());

            for (int y = lowY; y <= highY; y++) {
                for (int x = lowX; x <= highX; x++) {
                    for (int z = lowZ; z <= highZ; z++) {
                        this.perform(this.clampY(x, y, z));
                    }
                }
            }
            this.block = null;
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private void perform(final AsyncBlock bl) {
        if (bl.getType() == Material.REPEATER) {
            if (this.northSouth) {
                if ((bl.getPropertyId() % 4) == 1) {
                    this.undo.put(bl);
                    bl.setPropertyId((bl.getPropertyId() + 2));
                } else if ((bl.getPropertyId() % 4) == 3) {
                    this.undo.put(bl);
                    bl.setPropertyId((bl.getPropertyId() - 2));
                }
            } else {
                if ((bl.getPropertyId() % 4) == 2) {
                    this.undo.put(bl);
                    bl.setPropertyId((bl.getPropertyId() - 2));
                } else if ((bl.getPropertyId() % 4) == 0) {
                    this.undo.put(bl);
                    bl.setPropertyId((bl.getPropertyId() + 2));
                }
            }
        }
    }

    @Override
    protected final void arrow(final SnipeData v) {
        if (this.set(this.getTargetBlock())) {
            v.sendMessage(ChatColor.GRAY + "Point one");
        } else {
            v.owner().storeUndo(this.undo);
        }
    }

    @Override
    protected final void powder(final SnipeData v) {
        if (this.set(this.getLastBlock())) {
            v.sendMessage(ChatColor.GRAY + "Point one");
        } else {
            v.owner().storeUndo(this.undo);
        }
    }

    @Override
    public final void info(final Message vm) {
        this.block = null;
        vm.brushName(this.getName());
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        for (int i = 1; i < par.length; i++) {
            if (par[i].equalsIgnoreCase("info")) {
                v.sendMessage(ChatColor.GOLD + "Set Repeater Flip Parameters:");
                v.sendMessage(ChatColor.AQUA + "/b setrf <direction> -- valid direction inputs are(n,s,e,world), Set the direction that you wish to flip your repeaters, defaults to north/south.");
                return;
            }
            if (par[i].startsWith("n") || par[i].startsWith("s") || par[i].startsWith("ns")) {
                this.northSouth = true;
                v.sendMessage(ChatColor.AQUA + "Flip direction set to north/south");
            } else if (par[i].startsWith("e") || par[i].startsWith("world") || par[i].startsWith("ew")) {
                this.northSouth = false;
                v.sendMessage(ChatColor.AQUA + "Flip direction set to east/west.");
            } else {
                v.sendMessage(ChatColor.RED + "Invalid brush parameters! use the info parameter to display parameter info.");
            }
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.setredstoneflip";
    }
}
