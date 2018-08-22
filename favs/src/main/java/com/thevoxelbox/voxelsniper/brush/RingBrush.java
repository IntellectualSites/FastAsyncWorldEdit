package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;

/**
 * http://www.voxelwiki.com/minecraft/Voxelsniper#Ring_Brush
 *
 * @author Voxel
 */
public class RingBrush extends PerformBrush
{
    private double trueCircle = 0;
    private double innerSize = 0;

    /**
     *
     */
    public RingBrush()
    {
        this.setName("Ring");
    }

    private void ring(final SnipeData v, AsyncBlock targetBlock)
    {
        final int brushSize = v.getBrushSize();
        final double outerSquared = Math.pow(brushSize + this.trueCircle, 2);
        final double innerSquared = Math.pow(this.innerSize, 2);

        for (int x = brushSize; x >= 0; x--)
        {
            final double xSquared = Math.pow(x, 2);
            for (int z = brushSize; z >= 0; z--)
            {
                final double ySquared = Math.pow(z, 2);
                if ((xSquared + ySquared) <= outerSquared && (xSquared + ySquared) >= innerSquared)
                {
                    current.perform(targetBlock.getRelative(x, 0, z));
                    current.perform(targetBlock.getRelative(x, 0, -z));
                    current.perform(targetBlock.getRelative(-x, 0, z));
                    current.perform(targetBlock.getRelative(-x, 0, -z));
                }
            }
        }

        v.owner().storeUndo(this.current.getUndo());
    }

    @Override
    protected final void arrow(final SnipeData v)
    {
        this.ring(v, this.getTargetBlock());
    }

    @Override
    protected final void powder(final SnipeData v)
    {
        this.ring(v, this.getLastBlock());
    }

    @Override
    public final void info(final Message vm)
    {
        vm.brushName(this.getName());
        vm.size();
        vm.custom(ChatColor.AQUA + "The inner radius is " + ChatColor.RED + this.innerSize);
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v)
    {
        for (int i = 1; i < par.length; i++)
        {
            if (par[i].equalsIgnoreCase("info"))
            {
                v.sendMessage(ChatColor.GOLD + "Ring Brush Parameters:");
                v.sendMessage(ChatColor.AQUA + "/b ri true -- will use a true circle algorithm instead of the skinnier version with classic sniper nubs. /b ri false will switch back. (false is default)");
                v.sendMessage(ChatColor.AQUA + "/b ri ir2.5 -- will set the inner radius to 2.5 units");
                return;
            }
            else if (par[i].startsWith("true"))
            {
                this.trueCircle = 0.5;
                v.sendMessage(ChatColor.AQUA + "True circle mode ON.");
            }
            else if (par[i].startsWith("false"))
            {
                this.trueCircle = 0;
                v.sendMessage(ChatColor.AQUA + "True circle mode OFF.");
            }
            else if (par[i].startsWith("ir"))
            {
                try
                {
                    final double d = Double.parseDouble(par[i].replace("ir", ""));
                    this.innerSize = d;
                    v.sendMessage(ChatColor.AQUA + "The inner radius has been set to " + ChatColor.RED + this.innerSize);
                }
                catch (final Exception exception)
                {
                    v.sendMessage(ChatColor.RED + "The parameters included are invalid.");
                }
            }
            else
            {
                v.sendMessage(ChatColor.RED + "Invalid brush parameters! use the info parameter to display parameter info.");
            }
        }
    }

    @Override
    public String getPermissionNode()
    {
        return "voxelsniper.brush.ring";
    }
}
