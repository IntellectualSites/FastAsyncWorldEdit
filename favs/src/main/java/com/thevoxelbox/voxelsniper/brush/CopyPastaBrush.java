package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;

/**
 * http://www.voxelwiki.com/minecraft/Voxelsniper#CopyPasta_Brush
 *
 * @author giltwist
 */
public class CopyPastaBrush extends Brush
{
    private static final int BLOCK_LIMIT = 10000;

    private boolean pasteAir = true; // False = no air, true = air
    private int points = 0; //
    private int numBlocks = 0;
    private int[] firstPoint = new int[3];
    private int[] secondPoint = new int[3];
    private int[] pastePoint = new int[3];
    private int[] minPoint = new int[3];
    private int[] offsetPoint = new int[3];
    private int[] blockArray;
    private int[] arraySize = new int[3];
    private int pivot = 0; // ccw degrees    

    /**
     *
     */
    public CopyPastaBrush()
    {
        this.setName("CopyPasta");
    }

    @SuppressWarnings("deprecation")
	private void doCopy(final SnipeData v)
    {
        for (int i = 0; i < 3; i++)
        {
            this.arraySize[i] = Math.abs(this.firstPoint[i] - this.secondPoint[i]) + 1;
            this.minPoint[i] = Math.min(this.firstPoint[i], this.secondPoint[i]);
            this.offsetPoint[i] = this.minPoint[i] - this.firstPoint[i]; // will always be negative or zero
        }

        this.numBlocks = (this.arraySize[0]) * (this.arraySize[1]) * (this.arraySize[2]);

        if (this.numBlocks > 0 && this.numBlocks < CopyPastaBrush.BLOCK_LIMIT)
        {
            this.blockArray = new int[this.numBlocks];

            for (int i = 0; i < this.arraySize[0]; i++)
            {
                for (int j = 0; j < this.arraySize[1]; j++)
                {
                    for (int k = 0; k < this.arraySize[2]; k++)
                    {
                        final int currentPosition = i + this.arraySize[0] * j + this.arraySize[0] * this.arraySize[1] * k;
                        this.blockArray[currentPosition] = this.getWorld().getBlockAt(this.minPoint[0] + i, this.minPoint[1] + j, this.minPoint[2] + k).getCombinedId();
                    }
                }
            }

            v.sendMessage(ChatColor.AQUA + "" + this.numBlocks + " blocks copied.");
        }
        else
        {
            v.sendMessage(ChatColor.RED + "Copy area too big: " + this.numBlocks + "(Limit: " + CopyPastaBrush.BLOCK_LIMIT + ")");
        }
    }

    @SuppressWarnings("deprecation")
	private void doPasta(final SnipeData v)
    {
        final Undo undo = new Undo();

        for (int i = 0; i < this.arraySize[0]; i++)
        {
            for (int j = 0; j < this.arraySize[1]; j++)
            {
                for (int k = 0; k < this.arraySize[2]; k++)
                {
                    final int currentPosition = i + this.arraySize[0] * j + this.arraySize[0] * this.arraySize[1] * k;
                    AsyncBlock block;

                    switch (this.pivot)
                    {
                        case 180:
                            block = this.clampY(this.pastePoint[0] - this.offsetPoint[0] - i, this.pastePoint[1] + this.offsetPoint[1] + j, this.pastePoint[2] - this.offsetPoint[2] - k);
                            break;
                        case 270:
                            block = this.clampY(this.pastePoint[0] + this.offsetPoint[2] + k, this.pastePoint[1] + this.offsetPoint[1] + j, this.pastePoint[2] - this.offsetPoint[0] - i);
                            break;
                        case 90:
                            block = this.clampY(this.pastePoint[0] - this.offsetPoint[2] - k, this.pastePoint[1] + this.offsetPoint[1] + j, this.pastePoint[2] + this.offsetPoint[0] + i);
                            break;
                        default: // assume no rotation
                            block = this.clampY(this.pastePoint[0] + this.offsetPoint[0] + i, this.pastePoint[1] + this.offsetPoint[1] + j, this.pastePoint[2] + this.offsetPoint[2] + k);
                            break;
                    }

                    if (!(BlockTypes.getFromStateId(this.blockArray[currentPosition]).getMaterial().isAir() && !this.pasteAir))
                    {

                        if (block.getCombinedId() != this.blockArray[currentPosition])
                        {
                            undo.put(block);
                        }
                        block.setCombinedId(this.blockArray[currentPosition]);
                    }
                }
            }
        }
        v.sendMessage(ChatColor.AQUA + "" + this.numBlocks + " blocks pasted.");

        v.owner().storeUndo(undo);
    }

    @Override
    protected final void arrow(final com.thevoxelbox.voxelsniper.SnipeData v)
    {
        switch (this.points)
        {
            case 0:
                this.firstPoint[0] = this.getTargetBlock().getX();
                this.firstPoint[1] = this.getTargetBlock().getY();
                this.firstPoint[2] = this.getTargetBlock().getZ();
                v.sendMessage(ChatColor.GRAY + "First point");
                this.points = 1;
                break;
            case 1:
                this.secondPoint[0] = this.getTargetBlock().getX();
                this.secondPoint[1] = this.getTargetBlock().getY();
                this.secondPoint[2] = this.getTargetBlock().getZ();
                v.sendMessage(ChatColor.GRAY + "Second point");
                this.points = 2;
                break;
            default:
                this.firstPoint = new int[3];
                this.secondPoint = new int[3];
                this.numBlocks = 0;
                this.blockArray = new int[1];
                this.points = 0;
                v.sendMessage(ChatColor.GRAY + "Points cleared.");
                break;
        }
    }

    @Override
    protected final void powder(final com.thevoxelbox.voxelsniper.SnipeData v)
    {
        if (this.points == 2)
        {
            if (this.numBlocks == 0)
            {
                this.doCopy(v);
            }
            else if (this.numBlocks > 0 && this.numBlocks < CopyPastaBrush.BLOCK_LIMIT)
            {
                this.pastePoint[0] = this.getTargetBlock().getX();
                this.pastePoint[1] = this.getTargetBlock().getY();
                this.pastePoint[2] = this.getTargetBlock().getZ();
                this.doPasta(v);
            }
            else
            {
                v.sendMessage(ChatColor.RED + "Error");
            }
        }
        else
        {
            v.sendMessage(ChatColor.RED + "You must select exactly two points.");
        }
    }

    @Override
    public final void info(final Message vm)
    {
        vm.brushName(this.getName());
        vm.custom(ChatColor.GOLD + "Paste air: " + this.pasteAir);
        vm.custom(ChatColor.GOLD + "Pivot angle: " + this.pivot);
    }

    @Override
    public final void parameters(final String[] par, final com.thevoxelbox.voxelsniper.SnipeData v)
    {
        final String parameter = par[1];

        if (parameter.equalsIgnoreCase("info"))
        {
            v.sendMessage(ChatColor.GOLD + "CopyPasta Parameters:");
            v.sendMessage(ChatColor.AQUA + "/b cp air -- toggle include (default) or exclude  air during paste");
            v.sendMessage(ChatColor.AQUA + "/b cp 0|90|180|270 -- toggle rotation (0 default)");
            return;
        }

        if (parameter.equalsIgnoreCase("air"))
        {
            this.pasteAir = !this.pasteAir;

            v.sendMessage(ChatColor.GOLD + "Paste air: " + this.pasteAir);
            return;
        }

        if (parameter.equalsIgnoreCase("90") || parameter.equalsIgnoreCase("180") || parameter.equalsIgnoreCase("270") || parameter.equalsIgnoreCase("0"))
        {
            this.pivot = Integer.parseInt(parameter);
            v.sendMessage(ChatColor.GOLD + "Pivot angle: " + this.pivot);
        }
    }

    @Override
    public String getPermissionNode()
    {
        return "voxelsniper.brush.copypasta";
    }
}
