package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;

/**
 * @author MikeMatrix
 */
public class BlockResetBrush extends Brush {
    private static final ArrayList<Material> DENIED_UPDATES = new ArrayList<>();

    static {
        BlockResetBrush.DENIED_UPDATES.add(Material.SIGN);
        BlockResetBrush.DENIED_UPDATES.add(Material.LEGACY_SIGN_POST);
        BlockResetBrush.DENIED_UPDATES.add(Material.WALL_SIGN);
        BlockResetBrush.DENIED_UPDATES.add(Material.CHEST);
        BlockResetBrush.DENIED_UPDATES.add(Material.FURNACE);
        BlockResetBrush.DENIED_UPDATES.add(Material.LEGACY_BURNING_FURNACE);
        BlockResetBrush.DENIED_UPDATES.add(Material.LEGACY_REDSTONE_TORCH_OFF);
        BlockResetBrush.DENIED_UPDATES.add(Material.LEGACY_REDSTONE_TORCH_ON);
        BlockResetBrush.DENIED_UPDATES.add(Material.REDSTONE_WIRE);
        BlockResetBrush.DENIED_UPDATES.add(Material.LEGACY_DIODE_BLOCK_OFF);
        BlockResetBrush.DENIED_UPDATES.add(Material.LEGACY_DIODE_BLOCK_ON);
        BlockResetBrush.DENIED_UPDATES.add(Material.LEGACY_WOODEN_DOOR);
        BlockResetBrush.DENIED_UPDATES.add(Material.LEGACY_WOOD_DOOR);
        BlockResetBrush.DENIED_UPDATES.add(Material.IRON_DOOR);
        BlockResetBrush.DENIED_UPDATES.add(Material.LEGACY_IRON_DOOR_BLOCK);
        BlockResetBrush.DENIED_UPDATES.add(Material.LEGACY_FENCE_GATE);
    }

    /**
     *
     */
    public BlockResetBrush() {
        this.setName("Block Reset Brush");
    }

    @SuppressWarnings("deprecation")
    private void applyBrush(final SnipeData v) {
        for (int z = -v.getBrushSize(); z <= v.getBrushSize(); z++) {
            for (int x = -v.getBrushSize(); x <= v.getBrushSize(); x++) {
                for (int y = -v.getBrushSize(); y <= v.getBrushSize(); y++) {
                    final Block block = this.getWorld().getBlockAt(this.getTargetBlock().getX() + x, this.getTargetBlock().getY() + y, this.getTargetBlock().getZ() + z);
                    if (BlockResetBrush.DENIED_UPDATES.contains(block.getType())) {
                        continue;
                    }

                    block.setBlockData(block.getType().createBlockData(), true);
                }
            }
        }
    }

    @Override
    protected final void arrow(final SnipeData v) {
        applyBrush(v);
    }

    @Override
    protected final void powder(final SnipeData v) {
        applyBrush(v);
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.blockreset";
    }
}
