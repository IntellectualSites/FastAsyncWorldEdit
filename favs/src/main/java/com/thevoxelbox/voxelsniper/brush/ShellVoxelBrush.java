package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;

/**
 * THIS BRUSH SHOULD NOT USE PERFORMERS.
 * @author Voxel
 */
public class ShellVoxelBrush extends Brush {
    /**
     *
     */
    public ShellVoxelBrush() {
        this.setName("Shell Voxel");
    }

    private void vShell(final SnipeData v, Block targetBlock) {
        final int brushSize = v.getBrushSize();
        final int brushSizeSquared = 2 * brushSize;
        final int[][][] oldMaterials = new int[2 * (brushSize + 1) + 1][2 * (brushSize + 1) + 1][2 * (brushSize + 1) + 1]; // Array that holds the original materials plus a  buffer
        final int[][][] newMaterials = new int[2 * brushSize + 1][2 * brushSize + 1][2 * brushSize + 1]; // Array that holds the hollowed materials

        int blockPositionX = targetBlock.getX();
        int blockPositionY = targetBlock.getY();
        int blockPositionZ = targetBlock.getZ();
        // Log current materials into oldmats
        for (int x = 0; x <= 2 * (brushSize + 1); x++) {
            for (int y = 0; y <= 2 * (brushSize + 1); y++) {
                for (int z = 0; z <= 2 * (brushSize + 1); z++) {
                    oldMaterials[x][y][z] = this.getBlockIdAt(blockPositionX - brushSize - 1 + x, blockPositionY - brushSize - 1 + y, blockPositionZ - brushSize - 1 + z);
                }
            }
        }

        // Log current materials into newmats
        for (int x = 0; x <= brushSizeSquared; x++) {
            for (int y = 0; y <= brushSizeSquared; y++) {
                System.arraycopy(oldMaterials[x + 1][y + 1], 1, newMaterials[x][y], 0,
                        brushSizeSquared + 1);
            }
        }
        int temp;

        // Hollow Brush Area
        for (int x = 0; x <= brushSizeSquared; x++) {
            for (int z = 0; z <= brushSizeSquared; z++) {
                for (int y = 0; y <= brushSizeSquared; y++) {
                    temp = 0;

                    if (oldMaterials[x + 1 + 1][z + 1][y + 1] == v.getReplaceId()) {
                        temp++;
                    }
                    if (oldMaterials[x + 1 - 1][z + 1][y + 1] == v.getReplaceId()) {
                        temp++;
                    }
                    if (oldMaterials[x + 1][z + 1 + 1][y + 1] == v.getReplaceId()) {
                        temp++;
                    }
                    if (oldMaterials[x + 1][z + 1 - 1][y + 1] == v.getReplaceId()) {
                        temp++;
                    }
                    if (oldMaterials[x + 1][z + 1][y + 1 + 1] == v.getReplaceId()) {
                        temp++;
                    }
                    if (oldMaterials[x + 1][z + 1][y + 1 - 1] == v.getReplaceId()) {
                        temp++;
                    }

                    if (temp == 0) {
                        newMaterials[x][z][y] = v.getVoxelId();
                    }
                }
            }
        }

        // Make the changes
        final Undo undo = new Undo();

        for (int x = brushSizeSquared; x >= 0; x--) {
            for (int y = 0; y <= brushSizeSquared; y++) {
                for (int z = brushSizeSquared; z >= 0; z--) {
                    if (this.getBlockIdAt(blockPositionX - brushSize + x, blockPositionY - brushSize + y, blockPositionZ - brushSize + z) != newMaterials[x][y][z]) {
                        undo.put(this.clampY(blockPositionX - brushSize + x, blockPositionY - brushSize + y, blockPositionZ - brushSize + z));
                    }
                    this.setBlockIdAt(blockPositionZ - brushSize + z, blockPositionX - brushSize + x, blockPositionY - brushSize + y, newMaterials[x][y][z]);
                }
            }
        }
        v.owner().storeUndo(undo);

        v.owner().getPlayer().sendMessage(ChatColor.AQUA + "Shell complete.");
    }

    @Override
    protected final void arrow(final SnipeData v) {
        this.vShell(v, this.getTargetBlock());
    }

    @Override
    protected final void powder(final SnipeData v) {
        this.vShell(v, this.getLastBlock());
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
        vm.size();
        vm.voxel();
        vm.replace();
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        if (par[1].equalsIgnoreCase("info")) {
            v.sendMessage(ChatColor.GOLD + "Shell Voxel Parameters:");
        } else {
            v.sendMessage(ChatColor.RED + "Invalid parameter - see the info message for help.");
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.shellvoxel";
    }
}
