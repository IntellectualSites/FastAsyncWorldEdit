package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;

/**
 * @author MikeMatrix
 */
public class CheckerVoxelDiscBrush extends PerformBrush
{
    private boolean useWorldCoordinates = true;

    /**
     * Default constructor.
     */
    public CheckerVoxelDiscBrush()
    {
        this.setName("Checker Voxel Disc");
    }

    /**
     * @param v
     * @param target
     */
    private void applyBrush(final SnipeData v, final Block target)
    {
        for (int x = v.getBrushSize(); x >= -v.getBrushSize(); x--)
        {
            for (int y = v.getBrushSize(); y >= -v.getBrushSize(); y--)
            {
                final int sum = this.useWorldCoordinates ? target.getX() + x + target.getZ() + y : x + y;
                if (sum % 2 != 0)
                {
                    this.current.perform(this.clampY(target.getX() + x, target.getY(), target.getZ() + y));
                }
            }
        }
        v.owner().storeUndo(this.current.getUndo());
    }

    @Override
    protected final void arrow(final SnipeData v)
    {
        this.applyBrush(v, this.getTargetBlock());
    }

    @Override
    protected final void powder(final SnipeData v)
    {
        this.applyBrush(v, this.getLastBlock());
    }

    @Override
    public final void info(final Message vm)
    {
        vm.brushName(this.getName());
        vm.size();
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v)
    {
        for (int x = 1; x < par.length; x++)
        {
            final String parameter = par[x].toLowerCase();

            if (parameter.equals("info"))
            {
                v.sendMessage(ChatColor.GOLD + this.getName() + " Parameters:");
                v.sendMessage(ChatColor.AQUA + "true  -- Enables using World Coordinates.");
                v.sendMessage(ChatColor.AQUA + "false -- Disables using World Coordinates.");
                return;
            }
            if (parameter.startsWith("true"))
            {
                this.useWorldCoordinates = true;
                v.sendMessage(ChatColor.AQUA + "Enabled using World Coordinates.");
            }
            else if (parameter.startsWith("false"))
            {
                this.useWorldCoordinates = false;
                v.sendMessage(ChatColor.AQUA + "Disabled using World Coordinates.");
            }
            else
            {
                v.sendMessage(ChatColor.RED + "Invalid brush parameters! use the info parameter to display parameter info.");
                break;
            }
        }
    }

    @Override
    public String getPermissionNode()
    {
        return "voxelsniper.brush.checkervoxeldisc";
    }
}
