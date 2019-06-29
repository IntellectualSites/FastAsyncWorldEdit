package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.boydti.fawe.bukkit.wrapper.AsyncWorld;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;


public class OceanBrush extends Brush {
    private static final int WATER_LEVEL_DEFAULT = 62; // y=63 -- we are using array indices here
    private static final int WATER_LEVEL_MIN = 12;
    private static final int LOW_CUT_LEVEL = 12;

    private int waterLevel = WATER_LEVEL_DEFAULT;
    private boolean coverFloor = false;

    public OceanBrush() {
        this.setName("OCEANATOR 5000(tm)");
    }

    private int getHeight(final int bx, final int bz) {
        for (int y = this.getWorld().getHighestBlockYAt(bx, bz); y > 0; y--) {
            final Material material = this.clampY(bx, y, bz).getType();
            if (material.isSolid()) {
                return y;
            }
        }
        return 0;
    }

    protected final void oceanator(final SnipeData v, final Undo undo) {
        final AsyncWorld world = this.getWorld();

        final int minX = (int) Math.floor((this.getTargetBlock().getX() - v.getBrushSize()));
        final int minZ = (int) Math.floor((this.getTargetBlock().getZ() - v.getBrushSize()));
        final int maxX = (int) Math.floor((this.getTargetBlock().getX() + v.getBrushSize()));
        final int maxZ = (int) Math.floor((this.getTargetBlock().getZ() + v.getBrushSize()));

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                final int currentHeight = getHeight(x, z);
                final int wLevelDiff = currentHeight - (this.waterLevel - 1);
                final int newSeaFloorLevel = Math.max((this.waterLevel - wLevelDiff), LOW_CUT_LEVEL);

                final int highestY = this.getWorld().getHighestBlockYAt(x, z);

                // go down from highest Y block down to new sea floor
                for (int y = highestY; y > newSeaFloorLevel; y--) {
                    final Block block = world.getBlockAt(x, y, z);
                    if (!block.getType().equals(Material.AIR)) {
                        undo.put(block);
                        block.setType(Material.AIR);
                    }
                }

                // go down from water level to new sea level
                for (int y = this.waterLevel; y > newSeaFloorLevel; y--) {
                    final Block block = world.getBlockAt(x, y, z);
                    if (!block.getType().equals(Material.WATER)) {
                        // do not put blocks into the undo we already put into
                        if (!block.getType().equals(Material.AIR)) {
                            undo.put(block);
                        }
                        block.setType(Material.WATER);
                    }
                }

                // cover the sea floor of required
                if (this.coverFloor && (newSeaFloorLevel < this.waterLevel)) {
                    AsyncBlock block = world.getBlockAt(x, newSeaFloorLevel, z);
                    if (block.getTypeId() != v.getVoxelId()) {
                        undo.put(block);
                        block.setTypeId(v.getVoxelId());
                    }
                }
            }
        }
    }

    @Override
    protected final void arrow(final SnipeData v) {
        Undo undo = new Undo();
        this.oceanator(v, undo);
        v.owner().storeUndo(undo);
    }

    @Override
    protected final void powder(final SnipeData v) {
        arrow(v);
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        for (int i = 0; i < par.length; i++) {
            final String parameter = par[i];

            try {
                if (parameter.equalsIgnoreCase("info")) {
                    v.sendMessage(ChatColor.BLUE + "Parameters:");
                    v.sendMessage(ChatColor.GREEN + "-wlevel #  " + ChatColor.BLUE + "--  Sets the water level (e.g. -wlevel 64)");
                    v.sendMessage(ChatColor.GREEN + "-cfloor [y|n]  " + ChatColor.BLUE + "--  Enables or disables sea floor cover (e.g. -cfloor y) (Cover material will be your voxel material)");
                } else if (parameter.equalsIgnoreCase("-wlevel")) {
                    if ((i + 1) >= par.length) {
                        v.sendMessage(ChatColor.RED + "Missing parameter. Correct syntax: -wlevel [#] (e.g. -wlevel 64)");
                        continue;
                    }

                    int temp = Integer.parseInt(par[++i]);

                    if (temp <= WATER_LEVEL_MIN) {
                        v.sendMessage(ChatColor.RED + "Error: Your specified water level was below 12.");
                        continue;
                    }

                    this.waterLevel = temp - 1;
                    v.sendMessage(ChatColor.BLUE + "Water level set to " + ChatColor.GREEN + (waterLevel + 1)); // +1 since we are working with 0-based array indices
                } else if (parameter.equalsIgnoreCase("-cfloor") || parameter.equalsIgnoreCase("-coverfloor")) {
                    if ((i + 1) >= par.length) {
                        v.sendMessage(ChatColor.RED + "Missing parameter. Correct syntax: -cfloor [y|n] (e.g. -cfloor y)");
                        continue;
                    }

                    this.coverFloor = par[++i].equalsIgnoreCase("y");
                    v.sendMessage(ChatColor.BLUE + String.format("Floor cover %s.", ChatColor.GREEN + (this.coverFloor ? "enabled" : "disabled")));
                }
            } catch (Exception exception) {
                v.sendMessage(ChatColor.RED + String.format("Error while parsing parameter: %s", parameter));
                exception.printStackTrace();
            }
        }
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
        vm.custom(ChatColor.BLUE + "Water level set to " + ChatColor.GREEN + (waterLevel + 1)); // +1 since we are working with 0-based array indices
        vm.custom(ChatColor.BLUE + String.format("Floor cover %s.", ChatColor.GREEN + (this.coverFloor ? "enabled" : "disabled")));
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.ocean";
    }
}
