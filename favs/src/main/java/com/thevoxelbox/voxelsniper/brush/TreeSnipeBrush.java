package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.boydti.fawe.bukkit.wrapper.AsyncBlockState;
import com.google.common.base.Objects;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;
import com.thevoxelbox.voxelsniper.util.UndoDelegate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.block.BlockFace;


/**
 * @author Mick
 */
public class TreeSnipeBrush extends Brush {
    private TreeType treeType = TreeType.TREE;

    /**
     *
     */
    public TreeSnipeBrush() {
        this.setName("Tree Snipe");
    }

    @SuppressWarnings("deprecation")
    private void single(final SnipeData v, AsyncBlock targetBlock) {
        UndoDelegate undoDelegate = new UndoDelegate(targetBlock.getWorld());
        AsyncBlock blockBelow = targetBlock.getRelative(BlockFace.DOWN);
        AsyncBlockState currentState = blockBelow.getState();
        undoDelegate.setBlock(blockBelow);
        blockBelow.setType(Material.GRASS);
        this.getWorld().generateTree(targetBlock.getLocation(), this.treeType, undoDelegate);
        Undo undo = undoDelegate.getUndo();
        blockBelow.setTypeIdAndPropertyId(currentState.getTypeId(), currentState.getPropertyId(), true);
        undo.put(blockBelow);
        v.owner().storeUndo(undo);
    }

    private int getYOffset() {
        for (int i = 1; i < (getTargetBlock().getWorld().getMaxHeight() - 1 - getTargetBlock().getY()); i++) {
            if (Objects.equal(getTargetBlock().getRelative(0, i + 1, 0).getType(), Material.AIR)) {
                return i;
            }
        }
        return 0;
    }

    private void printTreeType(final Message vm) {
        String printout = "";

        boolean delimiterHelper = true;
        for (final TreeType treeType : TreeType.values()) {
            if (delimiterHelper) {
                delimiterHelper = false;
            } else {
                printout += ", ";
            }
            printout += ((treeType.equals(this.treeType)) ? ChatColor.GRAY + treeType.name().toLowerCase() : ChatColor.DARK_GRAY + treeType.name().toLowerCase()) + ChatColor.WHITE;
        }

        vm.custom(printout);
    }

    @Override
    protected final void arrow(final SnipeData v) {
        AsyncBlock targetBlock = getTargetBlock().getRelative(0, getYOffset(), 0);
        this.single(v, targetBlock);
    }

    @Override
    protected final void powder(final SnipeData v) {
        this.single(v, getTargetBlock());
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
        this.printTreeType(vm);
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        for (int i = 1; i < par.length; i++) {
            if (par[i].equalsIgnoreCase("info")) {
                v.sendMessage(ChatColor.GOLD + "Tree snipe brush:");
                v.sendMessage(ChatColor.AQUA + "/b t treetype");
                this.printTreeType(v.getVoxelMessage());
                return;
            }
            try {
                this.treeType = TreeType.valueOf(par[i].toUpperCase());
                this.printTreeType(v.getVoxelMessage());
            } catch (final IllegalArgumentException exception) {
                v.getVoxelMessage().brushMessage("No such tree type.");
            }
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.treesnipe";
    }
}
