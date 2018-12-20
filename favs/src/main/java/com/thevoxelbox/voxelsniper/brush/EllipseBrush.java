package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;

/**
 * http://www.voxelwiki.com/minecraft/Voxelsniper#Ellipse_Brush
 *
 * @author psanker
 */
public class EllipseBrush extends PerformBrush
{
    private static final double TWO_PI = (2 * Math.PI);
    private static final int SCL_MIN = 1;
    private static final int SCL_MAX = 9999;
    private static final int SCL_DEFAULT = 10;
    private static final int STEPS_MIN = 1;
    private static final int STEPS_MAX = 2000;
    private static final int STEPS_DEFAULT = 200;
    private int xscl;
    private int yscl;
    private int steps;
    private double stepSize;
    private boolean fill;

    /**
     *
     */
    public EllipseBrush()
    {
        this.setName("Ellipse");
    }

    private void ellipse(final SnipeData v, AsyncBlock targetBlock)
    {
        try
        {
            for (double steps = 0; (steps <= TWO_PI); steps += stepSize)
            {
                final int x = (int) Math.round(this.xscl * Math.cos(steps));
                final int y = (int) Math.round(this.yscl * Math.sin(steps));

                switch (getTargetBlock().getFace(this.getLastBlock()))
                {
                    case NORTH:
                    case SOUTH:
                        current.perform(targetBlock.getRelative(0, x, y));
                        break;
                    case EAST:
                    case WEST:
                        current.perform(targetBlock.getRelative(x, y, 0));
                        break;
                    case UP:
                    case DOWN:
                        current.perform(targetBlock.getRelative(x, 0, y));
                    default:
                        break;
                }

                if (steps >= TWO_PI)
                {
                    break;
                }
            }
        }
        catch (final Exception exception)
        {
            v.sendMessage(ChatColor.RED + "Invalid target.");
        }

        v.owner().storeUndo(this.current.getUndo());
    }

    private void ellipsefill(final SnipeData v, AsyncBlock targetBlock)
    {
        int ix = this.xscl;
        int iy = this.yscl;

        current.perform(targetBlock);

        try
        {
            if (ix >= iy)
            { // Need this unless you want weird holes
                for (iy = this.yscl; iy > 0; iy--)
                {
                    for (double steps = 0; (steps <= TWO_PI); steps += stepSize)
                    {
                        final int x = (int) Math.round(ix * Math.cos(steps));
                        final int y = (int) Math.round(iy * Math.sin(steps));

                        switch (getTargetBlock().getFace(this.getLastBlock()))
                        {
                            case NORTH:
                            case SOUTH:
                                current.perform(targetBlock.getRelative(0, x, y));
                                break;
                            case EAST:
                            case WEST:
                                current.perform(targetBlock.getRelative(x, y, 0));
                                break;
                            case UP:
                            case DOWN:
                                current.perform(targetBlock.getRelative(x, 0, y));
                            default:
                                break;
                        }

                        if (steps >= TWO_PI)
                        {
                            break;
                        }
                    }
                    ix--;
                }
            }
            else
            {
                for (ix = this.xscl; ix > 0; ix--)
                {
                    for (double steps = 0; (steps <= TWO_PI); steps += stepSize)
                    {
                        final int x = (int) Math.round(ix * Math.cos(steps));
                        final int y = (int) Math.round(iy * Math.sin(steps));

                        switch (getTargetBlock().getFace(this.getLastBlock()))
                        {
                            case NORTH:
                            case SOUTH:
                                current.perform(targetBlock.getRelative(0, x, y));
                                break;
                            case EAST:
                            case WEST:
                                current.perform(targetBlock.getRelative(x, y, 0));
                                break;
                            case UP:
                            case DOWN:
                                current.perform(targetBlock.getRelative(x, 0, y));
                            default:
                                break;
                        }

                        if (steps >= TWO_PI)
                        {
                            break;
                        }
                    }
                    iy--;
                }
            }
        }
        catch (final Exception exception)
        {
            v.sendMessage(ChatColor.RED + "Invalid target.");
        }

        v.owner().storeUndo(this.current.getUndo());
    }

    private void execute(final SnipeData v, AsyncBlock targetBlock)
    {
        this.stepSize = (TWO_PI / this.steps);

        if (this.fill)
        {
            this.ellipsefill(v, targetBlock);
        }
        else
        {
            this.ellipse(v, targetBlock);
        }
    }

    @Override
    protected final void arrow(final SnipeData v)
    {
        this.execute(v, this.getTargetBlock());
    }

    @Override
    protected final void powder(final SnipeData v)
    {
        this.execute(v, this.getLastBlock());
    }

    @Override
    public final void info(final Message vm)
    {
        if (this.xscl < SCL_MIN || this.xscl > SCL_MAX)
        {
            this.xscl = SCL_DEFAULT;
        }

        if (this.yscl < SCL_MIN || this.yscl > SCL_MAX)
        {
            this.yscl = SCL_DEFAULT;
        }

        if (this.steps < STEPS_MIN || this.steps > STEPS_MAX)
        {
            this.steps = STEPS_DEFAULT;
        }

        vm.brushName(this.getName());
        vm.custom(ChatColor.AQUA + "X-size set to: " + ChatColor.DARK_AQUA + this.xscl);
        vm.custom(ChatColor.AQUA + "Y-size set to: " + ChatColor.DARK_AQUA + this.yscl);
        vm.custom(ChatColor.AQUA + "Render step number set to: " + ChatColor.DARK_AQUA + this.steps);
        if (this.fill)
        {
            vm.custom(ChatColor.AQUA + "Fill mode is enabled");
        }
        else
        {
            vm.custom(ChatColor.AQUA + "Fill mode is disabled");
        }
    }

    @Override
    public final void parameters(final String[] par, final com.thevoxelbox.voxelsniper.SnipeData v)
    {
        for (int i = 1; i < par.length; i++)
        {
            final String parameter = par[i];

            try
            {
                if (parameter.equalsIgnoreCase("info"))
                {
                    v.sendMessage(ChatColor.GOLD + "Ellipse brush parameters");
                    v.sendMessage(ChatColor.AQUA + "x[n]: Set X size modifier to n");
                    v.sendMessage(ChatColor.AQUA + "y[n]: Set Y size modifier to n");
                    v.sendMessage(ChatColor.AQUA + "t[n]: Set the amount of time steps");
                    v.sendMessage(ChatColor.AQUA + "fill: Toggles fill mode");
                    return;
                }
                else if (parameter.startsWith("x"))
                {
                    int tempXScale = Integer.parseInt(par[i].replace("x", ""));
                    if (tempXScale < SCL_MIN || tempXScale > SCL_MAX)
                    {
                        v.sendMessage(ChatColor.AQUA + "Invalid X scale (" + SCL_MIN + "-" + SCL_MAX + ")");
                        continue;
                    }
                    this.xscl = tempXScale;
                    v.sendMessage(ChatColor.AQUA + "X-scale modifier set to: " + this.xscl);
                }
                else if (parameter.startsWith("y"))
                {
                    int tempYScale = Integer.parseInt(par[i].replace("y", ""));
                    if (tempYScale < SCL_MIN || tempYScale > SCL_MAX)
                    {
                        v.sendMessage(ChatColor.AQUA + "Invalid Y scale (" + SCL_MIN + "-" + SCL_MAX + ")");
                        continue;
                    }
                    this.yscl = tempYScale;
                    v.sendMessage(ChatColor.AQUA + "Y-scale modifier set to: " + this.yscl);
                }
                else if (parameter.startsWith("t"))
                {
                    int tempSteps = Integer.parseInt(par[i].replace("t", ""));
                    if (tempSteps < STEPS_MIN || tempSteps > STEPS_MAX)
                    {
                        v.sendMessage(ChatColor.AQUA + "Invalid step number (" + STEPS_MIN + "-" + STEPS_MAX + ")");
                        continue;
                    }
                    this.steps = tempSteps;
                    v.sendMessage(ChatColor.AQUA + "Render step number set to: " + this.steps);
                }
                else if (parameter.equalsIgnoreCase("fill"))
                {
                    if (this.fill)
                    {
                        this.fill = false;
                        v.sendMessage(ChatColor.AQUA + "Fill mode is disabled");
                    }
                    else
                    {
                        this.fill = true;
                        v.sendMessage(ChatColor.AQUA + "Fill mode is enabled");
                    }
                }
                else
                {
                    v.sendMessage(ChatColor.RED + "Invalid brush parameters! Use the \"info\" parameter to display parameter info.");
                }

            }
            catch (final Exception exception)
            {
                v.sendMessage(ChatColor.RED + "Incorrect parameter \"" + parameter + "\"; use the \"info\" parameter.");
            }
        }
    }

    @Override
    public String getPermissionNode()
    {
        return "voxelsniper.brush.ellipse";
    }
}
