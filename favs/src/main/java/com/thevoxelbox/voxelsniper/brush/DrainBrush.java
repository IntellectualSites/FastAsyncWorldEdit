package com.thevoxelbox.voxelsniper.brush;

import com.sk89q.worldedit.world.block.BlockTypes;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;
import org.bukkit.ChatColor;

/**
 * @author Gavjenks
 * @author psanker
 */
public class DrainBrush extends Brush {
    private double trueCircle = 0;
    private boolean disc = false;

    /**
     *
     */
    public DrainBrush() {
        this.setName("Drain");
    }

    @SuppressWarnings("deprecation")
    private void drain(final SnipeData v) {
        final int brushSize = v.getBrushSize();
        final double brushSizeSquared = Math.pow(brushSize + this.trueCircle, 2);
        final Undo undo = new Undo();

        if (this.disc) {
            for (int x = brushSize; x >= 0; x--) {
                final double xSquared = Math.pow(x, 2);

                for (int y = brushSize; y >= 0; y--) {
                    if ((xSquared + Math.pow(y, 2)) <= brushSizeSquared) {
                        if (this.getBlockIdAt(this.getTargetBlock().getX() + x, this.getTargetBlock().getY(), this.getTargetBlock().getZ() + y) == BlockTypes.WATER.getInternalId() || this.getBlockIdAt(this.getTargetBlock().getX() + x, this.getTargetBlock().getY(), this.getTargetBlock().getZ() + y) == BlockTypes.LAVA.getInternalId()) {
                            undo.put(this.clampY(this.getTargetBlock().getX() + x, this.getTargetBlock().getY(), this.getTargetBlock().getZ() + y));
                            this.setBlockIdAt(this.getTargetBlock().getZ() + y, this.getTargetBlock().getX() + x, this.getTargetBlock().getY(), BlockTypes.AIR.getInternalId());
                        }

                        if (this.getBlockIdAt(this.getTargetBlock().getX() + x, this.getTargetBlock().getY(), this.getTargetBlock().getZ() - y) == BlockTypes.WATER.getInternalId() || this.getBlockIdAt(this.getTargetBlock().getX() + x, this.getTargetBlock().getY(), this.getTargetBlock().getZ() - y) == BlockTypes.LAVA.getInternalId()) {
                            undo.put(this.clampY(this.getTargetBlock().getX() + x, this.getTargetBlock().getY(), this.getTargetBlock().getZ() - y));
                            this.setBlockIdAt(this.getTargetBlock().getZ() - y, this.getTargetBlock().getX() + x, this.getTargetBlock().getY(), BlockTypes.AIR.getInternalId());
                        }

                        if (this.getBlockIdAt(this.getTargetBlock().getX() - x, this.getTargetBlock().getY(), this.getTargetBlock().getZ() + y) == BlockTypes.WATER.getInternalId() || this.getBlockIdAt(this.getTargetBlock().getX() - x, this.getTargetBlock().getY(), this.getTargetBlock().getZ() + y) == BlockTypes.LAVA.getInternalId()) {
                            undo.put(this.clampY(this.getTargetBlock().getX() - x, this.getTargetBlock().getY(), this.getTargetBlock().getZ() + y));
                            this.setBlockIdAt(this.getTargetBlock().getZ() + y, this.getTargetBlock().getX() - x, this.getTargetBlock().getY(), BlockTypes.AIR.getInternalId());
                        }

                        if (this.getBlockIdAt(this.getTargetBlock().getX() - x, this.getTargetBlock().getY(), this.getTargetBlock().getZ() - y) == BlockTypes.WATER.getInternalId() || this.getBlockIdAt(this.getTargetBlock().getX() - x, this.getTargetBlock().getY(), this.getTargetBlock().getZ() - y) == BlockTypes.LAVA.getInternalId()) {
                            undo.put(this.clampY(this.getTargetBlock().getX() - x, this.getTargetBlock().getY(), this.getTargetBlock().getZ() - y));
                            this.setBlockIdAt(this.getTargetBlock().getZ() - y, this.getTargetBlock().getX() - x, this.getTargetBlock().getY(), BlockTypes.AIR.getInternalId());
                        }
                    }
                }
            }
        } else {
            for (int y = (brushSize + 1) * 2; y >= 0; y--) {
                final double ySquared = Math.pow(y - brushSize, 2);

                for (int x = (brushSize + 1) * 2; x >= 0; x--) {
                    final double xSquared = Math.pow(x - brushSize, 2);

                    for (int z = (brushSize + 1) * 2; z >= 0; z--) {
                        if ((xSquared + Math.pow(z - brushSize, 2) + ySquared) <= brushSizeSquared) {
                            if (this.getBlockIdAt(this.getTargetBlock().getX() + x - brushSize, this.getTargetBlock().getY() + z - brushSize, this.getTargetBlock().getZ() + y - brushSize) == BlockTypes.WATER.getInternalId() || this.getBlockIdAt(this.getTargetBlock().getX() + x - brushSize, this.getTargetBlock().getY() + z - brushSize, this.getTargetBlock().getZ() + y - brushSize) == BlockTypes.LAVA.getInternalId()) {
                                undo.put(this.clampY(this.getTargetBlock().getX() + x, this.getTargetBlock().getY() + z, this.getTargetBlock().getZ() + y));
                                this.setBlockIdAt(this.getTargetBlock().getZ() + y - brushSize, this.getTargetBlock().getX() + x - brushSize, this.getTargetBlock().getY() + z - brushSize, BlockTypes.AIR.getInternalId());
                            }
                        }
                    }
                }
            }
        }

        v.owner().storeUndo(undo);
    }

    @Override
    protected final void arrow(final SnipeData v) {
        this.drain(v);
    }

    @Override
    protected final void powder(final SnipeData v) {
        this.drain(v);
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
        vm.size();

        vm.custom(ChatColor.AQUA + ((this.trueCircle == 0.5) ? "True circle mode ON" : "True circle mode OFF"));
        vm.custom(ChatColor.AQUA + ((this.disc) ? "Disc drain mode ON" : "Disc drain mode OFF"));
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        for (int i = 1; i < par.length; i++) {
            final String parameter = par[i];

            if (parameter.equalsIgnoreCase("info")) {
                v.sendMessage(ChatColor.GOLD + "Drain Brush Parameters:");
                v.sendMessage(ChatColor.AQUA + "/b drain true -- will use a true sphere algorithm instead of the skinnier version with classic sniper nubs. /b drain false will switch back. (false is default)");
                v.sendMessage(ChatColor.AQUA + "/b drain d -- toggles disc drain mode, as opposed to a ball drain mode");
                return;
            } else if (parameter.startsWith("true")) {
                this.trueCircle = 0.5;
                v.sendMessage(ChatColor.AQUA + "True circle mode ON.");
            } else if (parameter.startsWith("false")) {
                this.trueCircle = 0;
                v.sendMessage(ChatColor.AQUA + "True circle mode OFF.");
            } else if (parameter.equalsIgnoreCase("d")) {
                if (this.disc) {
                    this.disc = false;
                    v.sendMessage(ChatColor.AQUA + "Disc drain mode OFF");
                } else {
                    this.disc = true;
                    v.sendMessage(ChatColor.AQUA + "Disc drain mode ON");
                }
            } else {
                v.sendMessage(ChatColor.RED + "Invalid brush parameters! Use the info parameter to display parameter info.");
            }
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.drain";
    }
}
