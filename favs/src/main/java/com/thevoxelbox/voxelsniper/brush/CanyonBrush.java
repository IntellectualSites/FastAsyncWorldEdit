package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.boydti.fawe.bukkit.wrapper.AsyncChunk;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;

/**
 * @author Voxel
 */
public class CanyonBrush extends Brush {
    private static final int SHIFT_LEVEL_MIN = 10;
    private static final int SHIFT_LEVEL_MAX = 60;
    private int yLevel = 10;

    /**
     *
     */
    public CanyonBrush() {
        this.setName("Canyon");
    }

    /**
     * @param chunk
     * @param undo
     */
    @SuppressWarnings("deprecation")
    protected final void canyon(final AsyncChunk chunk, final Undo undo) {
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int currentYLevel = this.yLevel;

                for (int y = 63; y < this.getWorld().getMaxHeight(); y++) {
                    final AsyncBlock block = chunk.getBlock(x, y, z);
                    final AsyncBlock currentYLevelBlock = chunk.getBlock(x, currentYLevel, z);

                    undo.put(block);
                    undo.put(currentYLevelBlock);

                    currentYLevelBlock.setTypeId(block.getTypeId());
                    block.setType(Material.AIR);

                    currentYLevel++;
                }

                final AsyncBlock block = chunk.getBlock(x, 0, z);
                undo.put(block);
                block.setTypeId(BlockTypes.BEDROCK.getInternalId());

                for (int y = 1; y < SHIFT_LEVEL_MIN; y++) {
                    final Block currentBlock = chunk.getBlock(x, y, z);
                    undo.put(currentBlock);
                    currentBlock.setType(Material.STONE);
                }
            }
        }
    }

    @Override
    protected void arrow(final SnipeData v) {
        final Undo undo = new Undo();

        canyon(getTargetBlock().getChunk(), undo);

        v.owner().storeUndo(undo);
    }

    @Override
    protected void powder(final SnipeData v) {
        final Undo undo = new Undo();

        Chunk targetChunk = getTargetBlock().getChunk();
        for (int x = targetChunk.getX() - 1; x <= targetChunk.getX() + 1; x++) {
            for (int z = targetChunk.getX() - 1; z <= targetChunk.getX() + 1; z++) {
                canyon(getWorld().getChunkAt(x, z), undo);
            }
        }

        v.owner().storeUndo(undo);
    }

    @Override
    public void info(final Message vm) {
        vm.brushName(this.getName());
        vm.custom(ChatColor.GREEN + "Shift Level set to " + this.yLevel);
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        if (par[1].equalsIgnoreCase("info")) {
            v.sendMessage(ChatColor.GREEN + "y[number] to set the Level to which the land will be shifted down");
        }
        if (par[1].startsWith("y")) {
            int _i = Integer.parseInt(par[1].replace("y", ""));
            if (_i < SHIFT_LEVEL_MIN) {
                _i = SHIFT_LEVEL_MIN;
            } else if (_i > SHIFT_LEVEL_MAX) {
                _i = SHIFT_LEVEL_MAX;
            }
            this.yLevel = _i;
            v.sendMessage(ChatColor.GREEN + "Shift Level set to " + this.yLevel);
        }
    }

    protected final int getYLevel() {
        return yLevel;
    }

    protected final void setYLevel(int yLevel) {
        this.yLevel = yLevel;
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.canyon";
    }
}
