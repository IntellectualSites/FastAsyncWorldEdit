package com.thevoxelbox.voxelsniper.brush;

import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;
import org.bukkit.ChatColor;

public class BlendBallBrush extends BlendBrushBase {

    public BlendBallBrush() {
        this.setName("Blend Ball");
    }

    @SuppressWarnings("deprecation")
    @Override
    protected final void blend(final SnipeData v) {
        final int brushSize = v.getBrushSize();
        final int brushSizeDoubled = 2 * brushSize;
        // Array that holds the original materials plus a buffer
        final int[][][] oldMaterials = new int[2 * (brushSize + 1) + 1][2 * (brushSize + 1) + 1][2 * (brushSize + 1) + 1];
        // Array that holds the blended materials
        final int[][][] newMaterials = new int[brushSizeDoubled + 1][brushSizeDoubled + 1][brushSizeDoubled + 1];

        // Log current materials into oldmats
        for (int x = 0; x <= 2 * (brushSize + 1); x++) {
            for (int y = 0; y <= 2 * (brushSize + 1); y++) {
                for (int z = 0; z <= 2 * (brushSize + 1); z++) {
                    oldMaterials[x][y][z] = this.getBlockIdAt(this.getTargetBlock().getX() - brushSize - 1 + x, this.getTargetBlock().getY() - brushSize - 1 + y, this.getTargetBlock().getZ() - brushSize - 1 + z);
                }
            }
        }

        // Log current materials into newmats
        for (int x = 0; x <= brushSizeDoubled; x++) {
            for (int y = 0; y <= brushSizeDoubled; y++) {
                System.arraycopy(oldMaterials[x + 1][y + 1], 1, newMaterials[x][y], 0,
                                 brushSizeDoubled + 1);
            }
        }

        // Blend materials
        for (int x = 0; x <= brushSizeDoubled; x++) {
            for (int y = 0; y <= brushSizeDoubled; y++) {
                for (int z = 0; z <= brushSizeDoubled; z++) {
                    final int[] materialFrequency = new int[BlockTypes.size()]; // Array that tracks frequency of materials neighboring given block
                    int modeMatCount = 0;
                    int modeMatId = 0;
                    boolean tiecheck = true;

                    for (int m = -1; m <= 1; m++) {
                        for (int n = -1; n <= 1; n++) {
                            for (int o = -1; o <= 1; o++) {
                                if (!(m == 0 && n == 0 && o == 0)) {
                                    materialFrequency[oldMaterials[x + 1 + m][y + 1 + n][z + 1 + o]]++;
                                }
                            }
                        }
                    }

                    // Find most common neighboring material.
                    for (BlockType type : BlockTypes.values) {
                        int i = type.getInternalId();
                        if (materialFrequency[i] > modeMatCount && !(this.excludeAir && type.getMaterial().isAir()) && !(this.excludeWater && (type == BlockTypes.WATER))) {
                            modeMatCount = materialFrequency[i];
                            modeMatId = i;
                        }
                    }
                    // Make sure there'world not a tie for most common
                    for (int i = 0; i < modeMatId; i++) {
                        BlockType type = BlockTypes.get(i);
                        if (materialFrequency[i] == modeMatCount && !(this.excludeAir && type.getMaterial().isAir()) && !(this.excludeWater && (type == BlockTypes.WATER))) {
                            tiecheck = false;
                        }
                    }

                    // Record most common neighbor material for this block
                    if (tiecheck) {
                        newMaterials[x][y][z] = modeMatId;
                    }
                }
            }
        }

        final Undo undo = new Undo();
        final double rSquared = Math.pow(brushSize + 1, 2);

        // Make the changes
        for (int x = brushSizeDoubled; x >= 0; x--) {
            final double xSquared = Math.pow(x - brushSize - 1, 2);

            for (int y = 0; y <= brushSizeDoubled; y++) {
                final double ySquared = Math.pow(y - brushSize - 1, 2);

                for (int z = brushSizeDoubled; z >= 0; z--) {
                    if (xSquared + ySquared + Math.pow(z - brushSize - 1, 2) <= rSquared) {
                        if (!(this.excludeAir && BlockTypes.get(newMaterials[x][y][z]).getMaterial().isAir()) && !(this.excludeWater && (newMaterials[x][y][z] == BlockTypes.WATER.getInternalId()))) {
                            if (this.getBlockIdAt(this.getTargetBlock().getX() - brushSize + x, this.getTargetBlock().getY() - brushSize + y, this.getTargetBlock().getZ() - brushSize + z) != newMaterials[x][y][z]) {
                                undo.put(this.clampY(this.getTargetBlock().getX() - brushSize + x, this.getTargetBlock().getY() - brushSize + y, this.getTargetBlock().getZ() - brushSize + z));
                            }
                            this.setBlockIdAt(this.getTargetBlock().getZ() - brushSize + z, this.getTargetBlock().getX() - brushSize + x, this.getTargetBlock().getY() - brushSize + y, newMaterials[x][y][z]);
                        }
                    }
                }
            }
        }
        v.owner().storeUndo(undo);
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        if (par[1].equalsIgnoreCase("info")) {
            v.sendMessage(ChatColor.GOLD + "Blend Ball Parameters:");
            v.sendMessage(ChatColor.AQUA + "/b bb water -- toggle include or exclude (default: exclude) water");
            return;
        }

        super.parameters(par, v);
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.blendball";
    }
}
