package com.thevoxelbox.voxelsniper.brush;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

public class ScannerBrush extends Brush {

    private static final int DEPTH_MIN = 1;
    private static final int DEPTH_DEFAULT = 24;
    private static final int DEPTH_MAX = 64;

    private int depth = DEPTH_DEFAULT;
    private Material checkFor = Material.AIR;

    public ScannerBrush() {
        this.setName("Scanner");
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        } else {
            return Math.min(value, max);
        }
    }

    private void scan(SnipeData v, BlockFace bf) {
        if (bf == null) {
            return;
        }

        switch (bf) {
            case NORTH:
                // Scan south
                for (int i = 1; i < this.depth + 1; i++) {
                    if (this.clampY(this.getTargetBlock().getX() + i, this.getTargetBlock().getY(),
                        this.getTargetBlock().getZ()).getType() == this.checkFor) {
                        v.sendMessage(ChatColor.GREEN + "" + this.checkFor + " found after " + i
                            + " blocks.");
                        return;
                    }
                }
                v.sendMessage(ChatColor.GRAY + "Nope.");
                break;

            case SOUTH:
                // Scan north
                for (int i = 1; i < this.depth + 1; i++) {
                    if (this.clampY(this.getTargetBlock().getX() - i, this.getTargetBlock().getY(),
                        this.getTargetBlock().getZ()).getType() == this.checkFor) {
                        v.sendMessage(ChatColor.GREEN + "" + this.checkFor + " found after " + i
                            + " blocks.");
                        return;
                    }
                }
                v.sendMessage(ChatColor.GRAY + "Nope.");
                break;

            case EAST:
                // Scan west
                for (int i = 1; i < this.depth + 1; i++) {
                    if (this.clampY(this.getTargetBlock().getX(), this.getTargetBlock().getY(),
                        this.getTargetBlock().getZ() + i).getType() == this.checkFor) {
                        v.sendMessage(ChatColor.GREEN + "" + this.checkFor + " found after " + i
                            + " blocks.");
                        return;
                    }
                }
                v.sendMessage(ChatColor.GRAY + "Nope.");
                break;

            case WEST:
                // Scan east
                for (int i = 1; i < this.depth + 1; i++) {
                    if (this.clampY(this.getTargetBlock().getX(), this.getTargetBlock().getY(),
                        this.getTargetBlock().getZ() - i).getType() == this.checkFor) {
                        v.sendMessage(ChatColor.GREEN + "" + this.checkFor + " found after " + i
                            + " blocks.");
                        return;
                    }
                }
                v.sendMessage(ChatColor.GRAY + "Nope.");
                break;

            case UP:
                // Scan down
                for (int i = 1; i < this.depth + 1; i++) {
                    if (this.getTargetBlock().getY() - i <= 0) {
                        break;
                    }
                    if (this.clampY(this.getTargetBlock().getX(), this.getTargetBlock().getY() - i,
                        this.getTargetBlock().getZ()).getType() == this.checkFor) {
                        v.sendMessage(ChatColor.GREEN + "" + this.checkFor + " found after " + i
                            + " blocks.");
                        return;
                    }
                }
                v.sendMessage(ChatColor.GRAY + "Nope.");
                break;

            case DOWN:
                // Scan up
                for (int i = 1; i < this.depth + 1; i++) {
                    if (this.getTargetBlock().getY() + i >= v.getWorld().getMaxHeight()) {
                        break;
                    }
                    if (this.clampY(this.getTargetBlock().getX(), this.getTargetBlock().getY() + i,
                        this.getTargetBlock().getZ()).getType() == this.checkFor) {
                        v.sendMessage(ChatColor.GREEN + "" + this.checkFor + " found after " + i
                            + " blocks.");
                        return;
                    }
                }
                v.sendMessage(ChatColor.GRAY + "Nope.");
                break;

            default:
                break;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected final void arrow(SnipeData v) {
        this.checkFor = BukkitAdapter.adapt(BlockTypes.get(v.getVoxelId()));
        this.scan(v, this.getTargetBlock().getFace(this.getLastBlock()));
    }

    @SuppressWarnings("deprecation")
    @Override
    protected final void powder(SnipeData v) {
        this.checkFor = BukkitAdapter.adapt(BlockTypes.get(v.getVoxelId()));
        this.scan(v, this.getTargetBlock().getFace(this.getLastBlock()));
    }

    @Override
    public final void info(Message vm) {
        vm.brushName(this.getName());
        vm.custom(ChatColor.GREEN + "Scanner depth set to " + this.depth);
        vm.custom(ChatColor.GREEN + "Scanner scans for " + this.checkFor + " (change with /v #)");
    }

    @Override
    public final void parameters(String[] par, SnipeData v) {
        for (int i = 1; i < par.length; i++) {
            if (par[i].equalsIgnoreCase("info")) {
                v.sendMessage(ChatColor.GOLD + "Scanner brush Parameters:");
                v.sendMessage(ChatColor.AQUA
                    + "/b sc d# -- will set the search depth to #. Clamps to 1 - 64.");
                return;
            }
            if (par[i].startsWith("d")) {
                this.depth = this
                    .clamp(Integer.parseInt(par[i].substring(1)), DEPTH_MIN, DEPTH_MAX);
                v.sendMessage(ChatColor.AQUA + "Scanner depth set to " + this.depth);
            } else {
                v.sendMessage(ChatColor.RED
                    + "Invalid brush parameters! Use the info parameter to display parameter info.");
            }
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.scanner";
    }
}
