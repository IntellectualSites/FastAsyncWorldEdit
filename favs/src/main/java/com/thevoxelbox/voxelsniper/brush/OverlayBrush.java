package com.thevoxelbox.voxelsniper.brush;

import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;
import org.bukkit.ChatColor;
import org.bukkit.Material;

/**
 * http://www.voxelwiki.com/minecraft/Voxelsniper#The_Overlay_.2F_Topsoil_Brush
 *
 * @author Gavjenks
 */
public class OverlayBrush extends PerformBrush
{
    private static final int DEFAULT_DEPTH = 3;
    private int depth = DEFAULT_DEPTH;
    private boolean allBlocks = false;

    /**
     *
     */
    public OverlayBrush()
    {
        this.setName("Overlay (Topsoil Filling)");
    }

    private void overlay(final SnipeData v)
    {
        final int brushSize = v.getBrushSize();
        final double brushSizeSquared = Math.pow(brushSize + 0.5, 2);


        for (int z = brushSize; z >= -brushSize; z--)
        {
            for (int x = brushSize; x >= -brushSize; x--)
            {
                // check if column is valid
                // column is valid if it has no solid block right above the clicked layer
                final int materialId = this.getBlockIdAt(this.getTargetBlock().getX() + x, this.getTargetBlock().getY() + 1, this.getTargetBlock().getZ() + z);
                if (isIgnoredBlock(materialId))
                {
                    if ((Math.pow(x, 2) + Math.pow(z, 2)) <= brushSizeSquared)
                    {
                        for (int y = this.getTargetBlock().getY(); y > 0; y--)
                        {
                            // check for surface
                            final int layerBlockId = this.getBlockIdAt(this.getTargetBlock().getX() + x, y, this.getTargetBlock().getZ() + z);
                            if (!isIgnoredBlock(layerBlockId))
                            {
                                for (int currentDepth = y; y - currentDepth < depth; currentDepth--)
                                {
                                    final int currentBlockId = this.getBlockIdAt(this.getTargetBlock().getX() + x, currentDepth, this.getTargetBlock().getZ() + z);
                                    if (isOverrideableMaterial(currentBlockId))
                                    {
                                        this.current.perform(this.clampY(this.getTargetBlock().getX() + x, currentDepth, this.getTargetBlock().getZ() + z));
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        v.owner().storeUndo(this.current.getUndo());
    }

    @SuppressWarnings("deprecation")
	private boolean isIgnoredBlock(int materialId)
    {
        BlockType type = BlockTypes.get(materialId);
        switch (type.getResource().toUpperCase()) {
            case "WATER":
            case "LAVA":
            case "CACTUS":
                return true;
        }
        BlockMaterial mat = type.getMaterial();
        return mat.isTranslucent();
    }

    @SuppressWarnings("deprecation")
	private boolean isOverrideableMaterial(int materialId)
    {
        BlockMaterial mat = BlockTypes.get(materialId).getMaterial();
        if (allBlocks && !(mat.isAir()))
        {
            return true;
        }

        if (!mat.isFragileWhenPushed() && mat.isFullCube()) {
            return true;
        }
        return false;
    }

    private void overlayTwo(final SnipeData v)
    {
        final int brushSize = v.getBrushSize();
        final double brushSizeSquared = Math.pow(brushSize + 0.5, 2);
        final int[][] memory = new int[brushSize * 2 + 1][brushSize * 2 + 1];

        for (int z = brushSize; z >= -brushSize; z--)
        {
            for (int x = brushSize; x >= -brushSize; x--)
            {
                boolean surfaceFound = false;
                for (int y = this.getTargetBlock().getY(); y > 0 && !surfaceFound; y--)
                { // start scanning from the height you clicked at
                    if (memory[x + brushSize][z + brushSize] != 1)
                    { // if haven't already found the surface in this column
                        if ((Math.pow(x, 2) + Math.pow(z, 2)) <= brushSizeSquared)
                        { // if inside of the column...
                            if (!this.getBlockAt(this.getTargetBlock().getX() + x, y - 1, this.getTargetBlock().getZ() + z).isEmpty())
                            { // if not a floating block (like one of Notch'world pools)
                                if (this.getBlockAt(this.getTargetBlock().getX() + x, y + 1, this.getTargetBlock().getZ() + z).isEmpty())
                                { // must start at surface... this prevents it filling stuff in if
                                    // you click in a wall and it starts out below surface.
                                    if (!this.allBlocks)
                                    { // if the override parameter has not been activated, go to the switch that filters out manmade stuff.

                                        BlockType type = BlockTypes.get(this.getBlockIdAt(this.getTargetBlock().getX() + x, y, this.getTargetBlock().getZ() + z));
                                        BlockMaterial mat = type.getMaterial();
                                        if (mat.isSolid() && mat.isFullCube() && !mat.hasContainer()) {
                                            for (int d = 1; (d < this.depth + 1); d++) {
                                                this.current.perform(this.clampY(this.getTargetBlock().getX() + x, y + d, this.getTargetBlock().getZ() + z)); // fills down as many layers as you specify
                                                // in parameters
                                                memory[x + brushSize][z + brushSize] = 1; // stop it from checking any other blocks in this vertical 1x1 column.
                                            }
                                            surfaceFound = true;
                                            continue;

                                        } else {
                                            continue;
                                        }
                                    }
                                    else
                                    {
                                        for (int d = 1; (d < this.depth + 1); d++)
                                        {
                                            this.current.perform(this.clampY(this.getTargetBlock().getX() + x, y + d, this.getTargetBlock().getZ() + z)); // fills down as many layers as you specify in
                                            // parameters
                                            memory[x + brushSize][z + brushSize] = 1; // stop it from checking any other blocks in this vertical 1x1 column.
                                        }
                                        surfaceFound = true;
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }

        v.owner().storeUndo(this.current.getUndo());
    }

    @Override
    protected final void arrow(final SnipeData v)
    {
        this.overlay(v);
    }

    @Override
    protected final void powder(final SnipeData v)
    {
        this.overlayTwo(v);
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
                v.sendMessage(ChatColor.GOLD + "Overlay brush parameters:");
                v.sendMessage(ChatColor.AQUA + "d[number] (ex:  d3) How many blocks deep you want to replace from the surface.");
                v.sendMessage(ChatColor.BLUE + "all (ex:  /b over all) Sets the brush to overlay over ALL materials, not just natural surface ones (will no longer ignore trees and buildings).  The parameter /some will set it back to default.");
                return;
            }
            if (parameter.startsWith("d"))
            {
                try {
                    this.depth = Integer.parseInt(parameter.replace("d", ""));

                    if (this.depth < 1)
                    {
                        this.depth = 1;
                    }

                    v.sendMessage(ChatColor.AQUA + "Depth set to " + this.depth);
                } catch (NumberFormatException e)  {
                    v.sendMessage(ChatColor.RED + "Depth isn't a number.");
                }
            }
            else if (parameter.startsWith("all"))
            {
                this.allBlocks = true;
                v.sendMessage(ChatColor.BLUE + "Will overlay over any block." + this.depth);
            }
            else if (parameter.startsWith("some"))
            {
                this.allBlocks = false;
                v.sendMessage(ChatColor.BLUE + "Will overlay only natural block types." + this.depth);
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
        return "voxelsniper.brush.overlay";
    }
}
