package com.thevoxelbox.voxelsniper.brush;

import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;

import org.bukkit.ChatColor;
import org.bukkit.Material;

/**
 * http://www.voxelwiki.com/minecraft/Voxelsniper#Blend_Brushes
 */
public class BlendDiscBrush extends BlendBrushBase
{
    /**
     *
     */
    public BlendDiscBrush()
    {
        this.setName("Blend Disc");
    }

    @SuppressWarnings("deprecation")
	@Override
    protected final void blend(final SnipeData v)
    {
        final int brushSize = v.getBrushSize();
        final int brushSizeDoubled = 2 * brushSize;
        final int[][] oldMaterials = new int[2 * (brushSize + 1) + 1][2 * (brushSize + 1) + 1]; // Array that holds the original materials plus a buffer
        final int[][] newMaterials = new int[brushSizeDoubled + 1][brushSizeDoubled + 1]; // Array that holds the blended materials

        // Log current materials into oldmats
        for (int x = 0; x <= 2 * (brushSize + 1); x++)
        {
            for (int z = 0; z <= 2 * (brushSize + 1); z++)
            {
                oldMaterials[x][z] = this.getBlockIdAt(this.getTargetBlock().getX() - brushSize - 1 + x, this.getTargetBlock().getY(), this.getTargetBlock().getZ() - brushSize - 1 + z);
            }
        }

        // Log current materials into newmats
        for (int x = 0; x <= brushSizeDoubled; x++)
        {
            for (int z = 0; z <= brushSizeDoubled; z++)
            {
                newMaterials[x][z] = oldMaterials[x + 1][z + 1];
            }
        }

        // Blend materials
        for (int x = 0; x <= brushSizeDoubled; x++)
        {
            for (int z = 0; z <= brushSizeDoubled; z++)
            {
                final int[] materialFrequency = new int[BlockTypes.size()]; // Array that tracks frequency of materials neighboring given block
                int modeMatCount = 0;
                int modeMatId = 0;
                boolean tiecheck = true;

                for (int m = -1; m <= 1; m++)
                {
                    for (int n = -1; n <= 1; n++)
                    {
                        if (!(m == 0 && n == 0))
                        {
                            materialFrequency[oldMaterials[x + 1 + m][z + 1 + n]]++;
                        }
                    }
                }

                // Find most common neighboring material.
                for (BlockType type : BlockTypes.values)
                {
                    int i = type.getInternalId();
                    if (materialFrequency[i] > modeMatCount && !(this.excludeAir && type.getMaterial().isAir()) && !(this.excludeWater && (type == BlockTypes.WATER)))
                    {
                        modeMatCount = materialFrequency[i];
                        modeMatId = i;
                    }
                }
                // Make sure there'world not a tie for most common
                for (int i = 0; i < modeMatId; i++)
                {
                    BlockType type = BlockTypes.get(i);
                    if (materialFrequency[i] == modeMatCount && !(this.excludeAir && type.getMaterial().isAir()) && !(this.excludeWater && (type == BlockTypes.WATER)))
                    {
                        tiecheck = false;
                    }
                }

                // Record most common neighbor material for this block
                if (tiecheck)
                {
                    newMaterials[x][z] = modeMatId;
                }
            }
        }

        final Undo undo = new Undo();
        final double rSquared = Math.pow(brushSize + 1, 2);

        // Make the changes
        for (int x = brushSizeDoubled; x >= 0; x--)
        {
            final double xSquared = Math.pow(x - brushSize - 1, 2);

            for (int z = brushSizeDoubled; z >= 0; z--)
            {
                if (xSquared + Math.pow(z - brushSize - 1, 2) <= rSquared)
                {
                    if (!(this.excludeAir && BlockTypes.get(newMaterials[x][z]).getMaterial().isAir()) && !(this.excludeWater && (newMaterials[x][z] == BlockTypes.WATER.getInternalId())))
                    {
                        if (this.getBlockIdAt(this.getTargetBlock().getX() - brushSize + x, this.getTargetBlock().getY(), this.getTargetBlock().getZ() - brushSize + z) != newMaterials[x][z])
                        {
                            undo.put(this.clampY(this.getTargetBlock().getX() - brushSize + x, this.getTargetBlock().getY(), this.getTargetBlock().getZ() - brushSize + z));
                        }
                        this.setBlockIdAt(this.getTargetBlock().getZ() - brushSize + z, this.getTargetBlock().getX() - brushSize + x, this.getTargetBlock().getY(), newMaterials[x][z]);
                    }
                }
            }
        }
        v.owner().storeUndo(undo);
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v)
    {
        if (par[1].equalsIgnoreCase("info"))
        {
            v.sendMessage(ChatColor.GOLD + "Blend Disc Parameters:");
            v.sendMessage(ChatColor.AQUA + "/b bd water -- toggle include or exclude (default) water");
            return;
        }

        super.parameters(par, v);
    }

    @Override
    public String getPermissionNode()
    {
        return "voxelsniper.brush.blenddisc";
    }
}
