package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.boydti.fawe.bukkit.wrapper.AsyncWorld;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;
import org.bukkit.ChatColor;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

/**
 * http://www.voxelwiki.com/minecraft/Voxelsniper#Line_Brush
 *
 * @author Gavjenks
 * @author giltwist
 * @author MikeMatrix
 */
public class LineBrush extends PerformBrush {
    private static final Vector HALF_BLOCK_OFFSET = new Vector(0.5, 0.5, 0.5);
    private Vector originCoords = null;
    private Vector targetCoords = new Vector();
    private AsyncWorld targetWorld;

    /**
     *
     */
    public LineBrush() {
        this.setName("Line");
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v) {
        if (par[1].equalsIgnoreCase("info")) {
            v.sendMessage(ChatColor.GOLD + "Line Brush instructions: Right click first point with the arrow. Right click with powder to draw a line to set the second point.");
        }
    }

    private void linePowder(final SnipeData v) {
        final Vector originClone = this.originCoords.clone().add(LineBrush.HALF_BLOCK_OFFSET);
        final Vector targetClone = this.targetCoords.clone().add(LineBrush.HALF_BLOCK_OFFSET);

        final Vector direction = targetClone.clone().subtract(originClone);
        final double length = this.targetCoords.distance(this.originCoords);

        if (length == 0) {
            this.current.perform((AsyncBlock) this.targetCoords.toLocation(this.targetWorld).getBlock());
        } else {
            for (final BlockIterator blockIterator = new BlockIterator(this.targetWorld, originClone, direction, 0, NumberConversions.round(length)); blockIterator.hasNext(); ) {
                final AsyncBlock currentBlock = (AsyncBlock) blockIterator.next();
                this.current.perform(currentBlock);
            }
        }

        v.owner().storeUndo(this.current.getUndo());
    }

    @Override
    protected final void arrow(final SnipeData v) {
        this.originCoords = this.getTargetBlock().getLocation().toVector();
        this.targetWorld = this.getTargetBlock().getWorld();
        v.owner().getPlayer().sendMessage(ChatColor.DARK_PURPLE + "First point selected.");
    }

    @Override
    protected final void powder(final SnipeData v) {
        if (this.originCoords == null || !this.getTargetBlock().getWorld().equals(this.targetWorld)) {
            v.owner().getPlayer().sendMessage(ChatColor.RED + "Warning: You did not select a first coordinate with the arrow");
        } else {
            this.targetCoords = this.getTargetBlock().getLocation().toVector();
            this.linePowder(v);
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.line";
    }
}
