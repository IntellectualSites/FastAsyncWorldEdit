package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import org.bukkit.ChatColor;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

public class BiomeBrush extends Brush {

    private Biome selectedBiome = Biome.PLAINS;
    public BiomeBrush() {
        this.setName("Biome (/b biome [Biome Name])");
    }

    private void biome(final SnipeData v) {
        final int brushSize = v.getBrushSize();
        final double brushSizeSquared = Math.pow(brushSize, 2);

        for (int x = -brushSize; x <= brushSize; x++) {
            final double xSquared = Math.pow(x, 2);

            for (int z = -brushSize; z <= brushSize; z++) {
                if ((xSquared + Math.pow(z, 2)) <= brushSizeSquared) {
                    this.getWorld().setBiome(this.getTargetBlock().getX() + x, this.getTargetBlock().getZ() + z, this.selectedBiome);
                }
            }
        }

        final Block block1 = this.getWorld().getBlockAt(this.getTargetBlock().getX() - brushSize, 0, this.getTargetBlock().getZ() - brushSize);
        final Block block2 = this.getWorld().getBlockAt(this.getTargetBlock().getX() + brushSize, 0, this.getTargetBlock().getZ() + brushSize);

        final int lowChunkX = (block1.getX() <= block2.getX()) ? block1.getChunk().getX() : block2.getChunk().getX();
        final int lowChunkZ = (block1.getZ() <= block2.getZ()) ? block1.getChunk().getZ() : block2.getChunk().getZ();
        final int highChunkX = (block1.getX() >= block2.getX()) ? block1.getChunk().getX() : block2.getChunk().getX();
        final int highChunkZ = (block1.getZ() >= block2.getZ()) ? block1.getChunk().getZ() : block2.getChunk().getZ();

        for (int x = lowChunkX; x <= highChunkX; x++) {
            for (int z = lowChunkZ; z <= highChunkZ; z++) {
                this.getWorld().refreshChunk(x, z);
            }
        }
    }

    @Override
    protected final void arrow(final SnipeData v) {
        this.biome(v);
    }

    @Override
    protected final void powder(final SnipeData v) {
        this.biome(v);
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
        vm.size();
        vm.custom(ChatColor.GOLD + "Currently selected biome type: " + ChatColor.DARK_GREEN + this.selectedBiome.name());
    }

    @Override
    public final void parameters(final String[] args, final SnipeData v) {
        if (args[1].equalsIgnoreCase("info")) {
            v.sendMessage(ChatColor.GOLD + "Biome Brush Parameters:");
            StringBuilder availableBiomes = new StringBuilder();

            for (final Biome biome : Biome.values()) {
                if (availableBiomes.length() == 0) {
                    availableBiomes = new StringBuilder(ChatColor.DARK_GREEN + biome.name());
                    continue;
                }

                availableBiomes.append(ChatColor.RED + ", " + ChatColor.DARK_GREEN)
                        .append(biome.name());

            }
            v.sendMessage(ChatColor.DARK_BLUE + "Available biomes: " + availableBiomes);
        } else {
            // allows biome names with spaces in their name
            StringBuilder biomeName = new StringBuilder(args[1]);
            for (int i = 2; i < args.length; i++) {
                biomeName.append(" ").append(args[i]);
            }

            for (final Biome biome : Biome.values()) {
                if (biome.name().equalsIgnoreCase(biomeName.toString())) {
                    this.selectedBiome = biome;
                    break;
                }
            }
            v.sendMessage(ChatColor.GOLD + "Currently selected biome type: " + ChatColor.DARK_GREEN + this.selectedBiome.name());
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.biome";
    }
}
