package com.thevoxelbox.voxelsniper.brush;


import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;


public class SnowConeBrush extends Brush {
    @SuppressWarnings("deprecation")
    private void addSnow(final SnipeData v, Block targetBlock) {
        int brushSize;
        int blockPositionX = targetBlock.getX();
        int blockPositionY = targetBlock.getY();
        int blockPositionZ = targetBlock.getZ();
        if (targetBlock.isEmpty()) {
            brushSize = 0;
        } else {
            brushSize = this.clampY(blockPositionX, blockPositionY, blockPositionZ).getPropertyId() + 1;
        }

        final int brushSizeDoubled = 2 * brushSize;
        final int[][] snowCone = new int[brushSizeDoubled + 1][brushSizeDoubled + 1]; // Will hold block IDs
        final int[][] snowConeData = new int[brushSizeDoubled + 1][brushSizeDoubled + 1]; // Will hold data values for snowCone
        final int[][] yOffset = new int[brushSizeDoubled + 1][brushSizeDoubled + 1];
        // prime the arrays

        for (int x = 0; x <= brushSizeDoubled; x++) {
            for (int z = 0; z <= brushSizeDoubled; z++) {
                boolean flag = true;

                for (int i = 0; i < 10; i++) { // overlay
                    if (flag) {
                        if ((this.getBlockAt(blockPositionX - brushSize + x, blockPositionY - i, blockPositionZ - brushSize + z).isEmpty() || this.getBlockIdAt(blockPositionX - brushSize + x, blockPositionY - i, blockPositionZ - brushSize + z) == BlockTypes.SNOW.getInternalId()) && !this.getBlockAt(blockPositionX - brushSize + x, blockPositionY - i - 1, blockPositionZ - brushSize + z).isEmpty() && this.getBlockIdAt(blockPositionX - brushSize + x, blockPositionY - i - 1, blockPositionZ - brushSize + z) != BlockTypes.SNOW.getInternalId()) {
                            flag = false;
                            yOffset[x][z] = i;
                        }
                    }
                }
                snowCone[x][z] = this.getBlockIdAt(blockPositionX - brushSize + x, blockPositionY - yOffset[x][z], blockPositionZ - brushSize + z);
                snowConeData[x][z] = this.clampY(blockPositionX - brushSize + x, blockPositionY - yOffset[x][z], blockPositionZ - brushSize + z).getPropertyId();
            }
        }

        // figure out new snowHeights
        for (int x = 0; x <= brushSizeDoubled; x++) {
            final double xSquared = Math.pow(x - brushSize, 2);

            for (int z = 0; z <= 2 * brushSize; z++) {
                final double zSquared = Math.pow(z - brushSize, 2);
                final double dist = Math.pow(xSquared + zSquared, .5); // distance from center of array
                final int snowData = brushSize - (int) Math.ceil(dist);

                if (snowData >= 0) { // no funny business
                    switch (snowData) {
                        case 0:
                            if (BlockTypes.get(snowCone[x][z]).getMaterial().isAir()) {
                                snowCone[x][z] = BlockTypes.SNOW.getInternalId();
                                snowConeData[x][z] = 0;
                            }
                            break;
                        case 7: // Turn largest snowTile into snow block
                            if (snowCone[x][z] == BlockTypes.SNOW.getInternalId()) {
                                snowCone[x][z] = BlockTypes.SNOW_BLOCK.getInternalId();
                                snowConeData[x][z] = 0;
                            }
                            break;
                        default: // Increase snowTile size, if smaller than target

                            if (snowData > snowConeData[x][z]) {
                                BlockType blockType =
                                        BlockTypes.get(snowCone[x][z]);
                                if (blockType.getMaterial().isAir()) {
                                    snowConeData[x][z] = snowData;
                                    snowCone[x][z] = BlockTypes.SNOW.getInternalId();

                                    snowConeData[x][z] = snowData;
                                } else if (blockType == BlockTypes.SNOW_BLOCK) {
                                    snowConeData[x][z] = snowData;
                                }
                            } else if (yOffset[x][z] > 0 && snowCone[x][z] == BlockTypes.SNOW.getInternalId()) {
                                snowConeData[x][z]++;
                                if (snowConeData[x][z] == 7) {
                                    snowConeData[x][z] = 0;
                                    snowCone[x][z] = BlockTypes.SNOW_BLOCK.getInternalId();
                                }
                            }
                            break;
                    }
                }
            }
        }

        final Undo undo = new Undo();

        for (int x = 0; x <= brushSizeDoubled; x++) {
            for (int z = 0; z <= brushSizeDoubled; z++) {

                if (this.getBlockIdAt(blockPositionX - brushSize + x, blockPositionY - yOffset[x][z], blockPositionZ - brushSize + z) != snowCone[x][z] || this.clampY(blockPositionX - brushSize + x, blockPositionY - yOffset[x][z], blockPositionZ - brushSize + z).getPropertyId() != snowConeData[x][z]) {
                    undo.put(this.clampY(blockPositionX - brushSize + x, blockPositionY - yOffset[x][z], blockPositionZ - brushSize + z));
                }
                this.setBlockIdAt(blockPositionZ - brushSize + z, blockPositionX - brushSize + x, blockPositionY - yOffset[x][z], snowCone[x][z]);
                this.clampY(blockPositionX - brushSize + x, blockPositionY - yOffset[x][z], blockPositionZ - brushSize + z).setPropertyId(snowConeData[x][z]);

            }
        }
        v.owner().storeUndo(undo);
    }

    @Override
    protected final void arrow(final SnipeData v) {
    }

    @Override
    protected final void powder(final SnipeData v) {
        if (getTargetBlock().getType() == Material.SNOW) {
            this.addSnow(v, this.getTargetBlock());
        } else {
            Block blockAbove = getTargetBlock().getRelative(BlockFace.UP);
            if (blockAbove != null && BukkitAdapter.adapt(blockAbove.getType()).getMaterial()
                                                   .isAir()) {
                addSnow(v, blockAbove);
            } else {
                v.owner().getPlayer()
                 .sendMessage(ChatColor.RED + "Error: Center block neither snow nor air.");
            }
        }
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName("Snow Cone");
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        if (par[1].equalsIgnoreCase("info")) {
            v.sendMessage(ChatColor.GOLD + "Snow Cone Parameters:");
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.snowcone";
    }
}
