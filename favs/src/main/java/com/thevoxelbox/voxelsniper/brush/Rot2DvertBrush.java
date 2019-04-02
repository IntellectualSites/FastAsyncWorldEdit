package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.util.BlockWrapper;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

/**
 * @author Gavjenks, hack job from the other 2d rotation brush blockPositionY piotr
 */
// The X Y and Z variable names in this file do NOT MAKE ANY SENSE. Do not attempt to actually figure out what on earth is going on here. Just go to the
// original 2d horizontal brush if you wish to make anything similar to this, and start there. I didn't bother renaming everything.
public class Rot2DvertBrush extends Brush
{
    private int mode = 0;
    private int bSize;
    private int brushSize;
    private BlockWrapper[][][] snap;
    private double se;

    /**
     *
     */
    public Rot2DvertBrush()
    {
        this.setName("2D Rotation");
    }

    @SuppressWarnings("deprecation")
	private void getMatrix()
    {
        this.brushSize = (this.bSize * 2) + 1;

        this.snap = new BlockWrapper[this.brushSize][this.brushSize][this.brushSize];

        int sx = this.getTargetBlock().getX() - this.bSize;
        int sy = this.getTargetBlock().getY() - this.bSize;
        int sz = this.getTargetBlock().getZ() - this.bSize;

        for (int x = 0; x < this.snap.length; x++)
        {
            sz = this.getTargetBlock().getZ() - this.bSize;

            for (int z = 0; z < this.snap.length; z++)
            {
                sy = this.getTargetBlock().getY() - this.bSize;

                for (int y = 0; y < this.snap.length; y++)
                {
                    final AsyncBlock block = this.clampY(sx, sy, sz); // why is this not sx + x, sy + y sz + z?
                    this.snap[x][y][z] = new BlockWrapper(block);
                    block.setTypeId(BlockTypes.AIR.getInternalId());
                    sy++;
                }

                sz++;
            }
            sx++;
        }
    }

    private void rotate(final SnipeData v)
    {
        final double brushSizeSquared = Math.pow(this.bSize + 0.5, 2);
        final double cos = Math.cos(this.se);
        final double sin = Math.sin(this.se);
        final boolean[][] doNotFill = new boolean[this.snap.length][this.snap.length];
        // I put y in the inside loop, since it doesn't have any power functions, should be much faster.
        // Also, new array keeps track of which x and z coords are being assigned in the rotated space so that we can
        // do a targeted filling of only those columns later that were left out.

        for (int x = 0; x < this.snap.length; x++)
        {
            final int xx = x - this.bSize;
            final double xSquared = Math.pow(xx, 2);

            for (int z = 0; z < this.snap.length; z++)
            {
                final int zz = z - this.bSize;

                if (xSquared + Math.pow(zz, 2) <= brushSizeSquared)
                {
                    final double newX = (xx * cos) - (zz * sin);
                    final double newZ = (xx * sin) + (zz * cos);

                    doNotFill[(int) newX + this.bSize][(int) newZ + this.bSize] = true;

                    for (int y = 0; y < this.snap.length; y++)
                    {
                        final int yy = y - this.bSize;

                        final BlockWrapper block = this.snap[y][x][z];
                        if (BlockTypes.get(block.getId()).getMaterial().isAir())
                        {
                            continue;
                        }
                        this.setBlockIdAndDataAt(this.getTargetBlock().getX() + yy, this.getTargetBlock().getY() + (int) newX, this.getTargetBlock().getZ() + (int) newZ, block.getId(), block.getPropertyId());
                    }
                }
            }
        }

        for (int x = 0; x < this.snap.length; x++)
        {
            final double xSquared = Math.pow(x - this.bSize, 2);
            final int fx = x + this.getTargetBlock().getX() - this.bSize;

            for (int z = 0; z < this.snap.length; z++)
            {
                if (xSquared + Math.pow(z - this.bSize, 2) <= brushSizeSquared)
                {
                    final int fz = z + this.getTargetBlock().getZ() - this.bSize;

                    if (!doNotFill[x][z])
                    {
                        // smart fill stuff
                        for (int y = 0; y < this.snap.length; y++)
                        {
                            final int fy = y + this.getTargetBlock().getY() - this.bSize;

                            final int a = this.getBlockIdAt(fy, fx + 1, fz);
                            final int aData = this.getBlockDataAt(fy, fx + 1, fz);
                            final int d = this.getBlockIdAt(fy, fx - 1, fz);
                            final int dData = this.getBlockDataAt(fy, fx - 1, fz);
                            final int c = this.getBlockIdAt(fy, fx, fz + 1);
                            final int b = this.getBlockIdAt(fy, fx, fz - 1);
                            final int bData = this.getBlockDataAt(fy, fx, fz - 1);

                            int winner;
                            int winnerData;

                            if (a == b || a == c || a == d)
                            { // I figure that since we are already narrowing it down to ONLY the holes left behind, it
                                // should
                                // be fine to do all 5 checks needed to be legit about it.
                                winner = a;
                                winnerData = aData;
                            }
                            else if (b == d || c == d)
                            {
                                winner = d;
                                winnerData = dData;
                            }
                            else
                            {
                                winner = b; // blockPositionY making this default, it will also automatically cover situations where B = C;
                                winnerData = bData;
                            }

                            this.setBlockIdAndDataAt(fy, fx, fz, winner, winnerData);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected final void arrow(final SnipeData v)
    {
        this.bSize = v.getBrushSize();

        if (this.mode == 0) {
            this.getMatrix();
            this.rotate(v);
        } else {
            v.owner().getPlayer().sendMessage(ChatColor.RED + "Something went wrong.");
        }
    }

    @Override
    protected final void powder(final SnipeData v)
    {
        this.bSize = v.getBrushSize();

        if (this.mode == 0) {
            this.getMatrix();
            this.rotate(v);
        } else {
            v.owner().getPlayer().sendMessage(ChatColor.RED + "Something went wrong.");
        }
    }

    @Override
    public final void info(final Message vm)
    {
        vm.brushName(this.getName());
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v)
    {
        try
        {
            this.se = Math.toRadians(Double.parseDouble(par[1]));
            v.sendMessage(ChatColor.GREEN + "Angle set to " + this.se);
        }
        catch (Exception _ex)
        {
            v.sendMessage("Exception while parsing parameter: " + par[1]);
            Bukkit.getLogger().severe(_ex.getMessage());
        }
    }

    @Override
    public String getPermissionNode()
    {
        return "voxelsniper.brush.rot2dvert";
    }
}
