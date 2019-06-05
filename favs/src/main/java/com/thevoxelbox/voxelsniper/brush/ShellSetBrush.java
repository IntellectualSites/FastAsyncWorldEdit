package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;

import java.util.ArrayList;

/**
 * http://www.voxelwiki.com/minecraft/Voxelsniper#Shell_Brushes
 *
 * @author Piotr
 */
public class ShellSetBrush extends Brush {
    private static final int MAX_SIZE = 5000000;
    private Block block = null;

    /**
     *
     */
    public ShellSetBrush() {
        this.setName("Shell Set");
    }

    @SuppressWarnings("deprecation")
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

            int selectionSize = Math.abs(highX - lowX) * Math.abs(highZ - lowZ) * Math.abs(highY - lowY);
            if (selectionSize > MAX_SIZE) {
                v.sendMessage(ChatColor.RED + "Selection size above hardcoded limit, please use a smaller selection.");
            } else {
                final ArrayList<AsyncBlock> blocks = new ArrayList<>(selectionSize / 2);
                for (int y = lowY; y <= highY; y++) {
                    for (int x = lowX; x <= highX; x++) {
                        for (int z = lowZ; z <= highZ; z++) {
                            if (this.getWorld().getBlockAt(x, y, z).getTypeId() == v.getReplaceId()) {
                                continue;
                            } else if (this.getWorld().getBlockAt(x + 1, y, z).getTypeId() == v.getReplaceId()) {
                                continue;
                            } else if (this.getWorld().getBlockAt(x - 1, y, z).getTypeId() == v.getReplaceId()) {
                                continue;
                            } else if (this.getWorld().getBlockAt(x, y, z + 1).getTypeId() == v.getReplaceId()) {
                                continue;
                            } else if (this.getWorld().getBlockAt(x, y, z - 1).getTypeId() == v.getReplaceId()) {
                                continue;
                            } else if (this.getWorld().getBlockAt(x, y + 1, z).getTypeId() == v.getReplaceId()) {
                                continue;
                            } else if (this.getWorld().getBlockAt(x, y - 1, z).getTypeId() == v.getReplaceId()) {
                                continue;
                            } else {
                                blocks.add(this.getWorld().getBlockAt(x, y, z));
                            }
                        }
                    }
                }

                final Undo undo = new Undo();
                for (final AsyncBlock currentBlock : blocks) {
                    if (currentBlock.getTypeId() != v.getVoxelId()) {
                        undo.put(currentBlock);
                        currentBlock.setTypeId(v.getVoxelId());
                    }
                }
                v.owner().storeUndo(undo);
                v.sendMessage(ChatColor.AQUA + "Shell complete.");
            }

            this.block = null;
            return false;
        }
    }

    @Override
    protected final void arrow(final SnipeData v) {
        if (this.set(this.getTargetBlock(), v)) {
            v.owner().getPlayer().sendMessage(ChatColor.GRAY + "Point one");
        }
    }

    @Override
    protected final void powder(final SnipeData v) {
        if (this.set(this.getLastBlock(), v)) {
            v.owner().getPlayer().sendMessage(ChatColor.GRAY + "Point one");
        }
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
        vm.size();
        vm.voxel();
        vm.replace();
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.shellset";
    }
}
