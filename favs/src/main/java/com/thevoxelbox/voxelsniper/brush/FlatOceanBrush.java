package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncChunk;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import org.bukkit.ChatColor;
import org.bukkit.Material;

/**
 * @author GavJenks
 */
public class FlatOceanBrush extends Brush {
    private static final int DEFAULT_WATER_LEVEL = 29;
    private static final int DEFAULT_FLOOR_LEVEL = 8;
    private int waterLevel = DEFAULT_WATER_LEVEL;
    private int floorLevel = DEFAULT_FLOOR_LEVEL;

    public FlatOceanBrush() {
        this.setName("FlatOcean");
    }

    @SuppressWarnings("deprecation")
    private void flatOcean(final AsyncChunk chunk) {
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                for (int y = 0; y < chunk.getWorld().getMaxHeight(); y++) {
                    if (y <= this.floorLevel) {
                        chunk.getBlock(x, y, z).setType(Material.DIRT);
                    } else if (y <= this.waterLevel) {
                        chunk.getBlock(x, y, z).setType(Material.WATER);
                    } else {
                        chunk.getBlock(x, y, z).setType(Material.AIR);
                    }
                }
            }
        }
    }

    @Override
    protected final void arrow(final SnipeData v) {
        this.flatOcean(this.getWorld().getChunkAt(this.getTargetBlock()));
    }

    @Override
    protected final void powder(final SnipeData v) {
        this.flatOcean(this.getWorld().getChunkAt(this.getTargetBlock()));
        this.flatOcean(this.getWorld().getChunkAt(this.clampY(this.getTargetBlock().getX() + CHUNK_SIZE, 1, this.getTargetBlock().getZ())));
        this.flatOcean(this.getWorld().getChunkAt(this.clampY(this.getTargetBlock().getX() + CHUNK_SIZE, 1, this.getTargetBlock().getZ() + CHUNK_SIZE)));
        this.flatOcean(this.getWorld().getChunkAt(this.clampY(this.getTargetBlock().getX(), 1, this.getTargetBlock().getZ() + CHUNK_SIZE)));
        this.flatOcean(this.getWorld().getChunkAt(this.clampY(this.getTargetBlock().getX() - CHUNK_SIZE, 1, this.getTargetBlock().getZ() + CHUNK_SIZE)));
        this.flatOcean(this.getWorld().getChunkAt(this.clampY(this.getTargetBlock().getX() - CHUNK_SIZE, 1, this.getTargetBlock().getZ())));
        this.flatOcean(this.getWorld().getChunkAt(this.clampY(this.getTargetBlock().getX() - CHUNK_SIZE, 1, this.getTargetBlock().getZ() - CHUNK_SIZE)));
        this.flatOcean(this.getWorld().getChunkAt(this.clampY(this.getTargetBlock().getX(), 1, this.getTargetBlock().getZ() - CHUNK_SIZE)));
        this.flatOcean(this.getWorld().getChunkAt(this.clampY(this.getTargetBlock().getX() + CHUNK_SIZE, 1, this.getTargetBlock().getZ() - CHUNK_SIZE)));
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
        vm.custom(ChatColor.RED + "THIS BRUSH DOES NOT UNDO");
        vm.custom(ChatColor.GREEN + "Water level set to " + this.waterLevel);
        vm.custom(ChatColor.GREEN + "Ocean floor level set to " + this.floorLevel);
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        for (int i = 1; i < par.length; i++) {
            final String parameter = par[i];

            if (parameter.equalsIgnoreCase("info")) {
                v.sendMessage(ChatColor.GREEN + "yo[number] to set the Level to which the water will rise.");
                v.sendMessage(ChatColor.GREEN + "yl[number] to set the Level to which the ocean floor will rise.");
            }
            if (parameter.startsWith("yo")) {
                int newWaterLevel = Integer.parseInt(parameter.replace("yo", ""));
                if (newWaterLevel < this.floorLevel) {
                    newWaterLevel = this.floorLevel + 1;
                }
                this.waterLevel = newWaterLevel;
                v.sendMessage(ChatColor.GREEN + "Water Level set to " + this.waterLevel);
            } else if (parameter.startsWith("yl")) {
                int newFloorLevel = Integer.parseInt(parameter.replace("yl", ""));
                if (newFloorLevel > this.waterLevel) {
                    newFloorLevel = this.waterLevel - 1;
                    if (newFloorLevel == 0) {
                        newFloorLevel = 1;
                        this.waterLevel = 2;
                    }
                }
                this.floorLevel = newFloorLevel;
                v.sendMessage(ChatColor.GREEN + "Ocean floor Level set to " + this.floorLevel);
            }
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.flatocean";
    }
}
