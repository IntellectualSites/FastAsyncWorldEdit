package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;

/**
 * A brush that creates a solid ball.
 * http://www.voxelwiki.com/minecraft/Voxelsniper#The_Ball_Brush
 *
 * @author Piotr
 */
public class BallBrush extends PerformBrush
{
    public static final double TRUE_CIRCLE_ON_VALUE = 0.5;
    public static final int TRUE_CIRCLE_OFF_VALUE = 0;
    private double trueCircle = 0;

    /**
     *
     */
    public BallBrush()
    {
        this.setName("Ball");
    }

    private void ball(final SnipeData v, AsyncBlock targetBlock)
    {
        final int brushSize = v.getBrushSize();
        final double brushSizeSquared = Math.pow(brushSize + this.trueCircle, 2);

        int blockPositionX = targetBlock.getX();
        int blockPositionY = targetBlock.getY();
        int blockPositionZ = targetBlock.getZ();
        this.current.perform(targetBlock);

        for (int z = 1; z <= brushSize; z++)
        {
            final double zSquared = Math.pow(z, 2);

            this.current.perform(this.clampY(blockPositionX + z, blockPositionY, blockPositionZ));
            this.current.perform(this.clampY(blockPositionX - z, blockPositionY, blockPositionZ));
            this.current.perform(this.clampY(blockPositionX, blockPositionY + z, blockPositionZ));
            this.current.perform(this.clampY(blockPositionX, blockPositionY - z, blockPositionZ));
            this.current.perform(this.clampY(blockPositionX, blockPositionY, blockPositionZ + z));
            this.current.perform(this.clampY(blockPositionX, blockPositionY, blockPositionZ - z));

            for (int x = 1; x <= brushSize; x++)
            {
                final double xSquared = Math.pow(x, 2);

                if (zSquared + xSquared <= brushSizeSquared)
                {
                    this.current.perform(this.clampY(blockPositionX + z, blockPositionY, blockPositionZ + x));
                    this.current.perform(this.clampY(blockPositionX + z, blockPositionY, blockPositionZ - x));
                    this.current.perform(this.clampY(blockPositionX - z, blockPositionY, blockPositionZ + x));
                    this.current.perform(this.clampY(blockPositionX - z, blockPositionY, blockPositionZ - x));
                    this.current.perform(this.clampY(blockPositionX + z, blockPositionY + x, blockPositionZ));
                    this.current.perform(this.clampY(blockPositionX + z, blockPositionY - x, blockPositionZ));
                    this.current.perform(this.clampY(blockPositionX - z, blockPositionY + x, blockPositionZ));
                    this.current.perform(this.clampY(blockPositionX - z, blockPositionY - x, blockPositionZ));
                    this.current.perform(this.clampY(blockPositionX, blockPositionY + z, blockPositionZ + x));
                    this.current.perform(this.clampY(blockPositionX, blockPositionY + z, blockPositionZ - x));
                    this.current.perform(this.clampY(blockPositionX, blockPositionY - z, blockPositionZ + x));
                    this.current.perform(this.clampY(blockPositionX, blockPositionY - z, blockPositionZ - x));
                }

                for (int y = 1; y <= brushSize; y++)
                {
                    if ((xSquared + Math.pow(y, 2) + zSquared) <= brushSizeSquared)
                    {
                        this.current.perform(this.clampY(blockPositionX + x, blockPositionY + y, blockPositionZ + z));
                        this.current.perform(this.clampY(blockPositionX + x, blockPositionY + y, blockPositionZ - z));
                        this.current.perform(this.clampY(blockPositionX - x, blockPositionY + y, blockPositionZ + z));
                        this.current.perform(this.clampY(blockPositionX - x, blockPositionY + y, blockPositionZ - z));
                        this.current.perform(this.clampY(blockPositionX + x, blockPositionY - y, blockPositionZ + z));
                        this.current.perform(this.clampY(blockPositionX + x, blockPositionY - y, blockPositionZ - z));
                        this.current.perform(this.clampY(blockPositionX - x, blockPositionY - y, blockPositionZ + z));
                        this.current.perform(this.clampY(blockPositionX - x, blockPositionY - y, blockPositionZ - z));
                    }
                }
            }
        }

        v.owner().storeUndo(this.current.getUndo());
    }

    @Override
    protected final void arrow(final SnipeData v)
    {
        this.ball(v, this.getTargetBlock());
    }

    @Override
    protected final void powder(final SnipeData v)
    {
        this.ball(v, this.getLastBlock());
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
        for (int i = 1; i < par.length; i++)
        {
            final String parameter = par[i];

            if (parameter.equalsIgnoreCase("info"))
            {
                v.sendMessage(ChatColor.GOLD + "Ball Brush Parameters:");
                v.sendMessage(ChatColor.AQUA + "/b b true -- will use a true sphere algorithm instead of the skinnier version with classic sniper nubs. /b b false will switch back. (false is default)");
                return;
            }
            else if (parameter.startsWith("true"))
            {
                this.trueCircle = TRUE_CIRCLE_ON_VALUE;
                v.sendMessage(ChatColor.AQUA + "True circle mode ON.");
            }
            else if (parameter.startsWith("false"))
            {
                this.trueCircle = TRUE_CIRCLE_OFF_VALUE;
                v.sendMessage(ChatColor.AQUA + "True circle mode OFF.");
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
        return "voxelsniper.brush.ball";
    }
}
