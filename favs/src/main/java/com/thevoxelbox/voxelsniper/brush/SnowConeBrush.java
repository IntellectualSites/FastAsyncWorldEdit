package com.thevoxelbox.voxelsniper.brush;


import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/**
 * http://www.voxelwiki.com/minecraft/Voxelsniper#Snow_cone_brush
 *
 * @author Voxel
 */
public class SnowConeBrush extends Brush
{
    @SuppressWarnings("deprecation")
	private void addSnow(final SnipeData v, Block targetBlock)
    {
        int brushSize;
        int blockPositionX = targetBlock.getX();
        int blockPositionY = targetBlock.getY();
        int blockPositionZ = targetBlock.getZ();
        if (targetBlock.isEmpty())
        {
            brushSize = 0;
        }
        else
        {
            brushSize = this.clampY(blockPositionX, blockPositionY, blockPositionZ).getPropertyId() + 1;
        }

        final int brushSizeDoubled = 2 * brushSize;
        final int[][] snowcone = new int[brushSizeDoubled + 1][brushSizeDoubled + 1]; // Will hold block IDs
        final int[][] snowconeData = new int[brushSizeDoubled + 1][brushSizeDoubled + 1]; // Will hold data values for snowcone
        final int[][] yOffset = new int[brushSizeDoubled + 1][brushSizeDoubled + 1];
        // prime the arrays

        for (int x = 0; x <= brushSizeDoubled; x++)
        {
            for (int z = 0; z <= brushSizeDoubled; z++)
            {
                boolean flag = true;

                for (int i = 0; i < 10; i++)
                { // overlay
                    if (flag)
                    {
                        if ((this.getBlockIdAt(blockPositionX - brushSize + x, blockPositionY - i, blockPositionZ - brushSize + z) == 0 || this.getBlockIdAt(blockPositionX - brushSize + x, blockPositionY - i, blockPositionZ - brushSize + z) == BlockTypes.SNOW.getInternalId()) && !this.getBlockAt(blockPositionX - brushSize + x, blockPositionY - i - 1, blockPositionZ - brushSize + z).isEmpty() && this.getBlockIdAt(blockPositionX - brushSize + x, blockPositionY - i - 1, blockPositionZ - brushSize + z) != Material.SNOW.getId())
                        {
                            flag = false;
                            yOffset[x][z] = i;
                        }
                    }
                }
                snowcone[x][z] = this.getBlockIdAt(blockPositionX - brushSize + x, blockPositionY - yOffset[x][z], blockPositionZ - brushSize + z);
                snowconeData[x][z] = this.clampY(blockPositionX - brushSize + x, blockPositionY - yOffset[x][z], blockPositionZ - brushSize + z).getPropertyId();
            }
        }

        // figure out new snowheights
        for (int x = 0; x <= brushSizeDoubled; x++)
        {
            final double xSquared = Math.pow(x - brushSize, 2);

            for (int z = 0; z <= 2 * brushSize; z++)
            {
                final double zSquared = Math.pow(z - brushSize, 2);
                final double dist = Math.pow(xSquared + zSquared, .5); // distance from center of array
                final int snowData = brushSize - (int) Math.ceil(dist);

                if (snowData >= 0)
                { // no funny business
                    switch (snowData)
                    {
                        case 0:
                            if (BlockTypes.get(snowcone[x][z]).getMaterial().isAir())
                            {
                                snowcone[x][z] = BlockTypes.SNOW.getInternalId();
                                snowconeData[x][z] = 0;
                            }
                            break;
                        case 7: // Turn largest snowtile into snowblock
                            if (snowcone[x][z] == BlockTypes.SNOW.getInternalId())
                            {
                                snowcone[x][z] = BlockTypes.SNOW_BLOCK.getInternalId();
                                snowconeData[x][z] = 0;
                            }
                            break;
                        default: // Increase snowtile size, if smaller than target

                            if (snowData > snowconeData[x][z])
                            {
                                switch (BlockTypes.get(snowcone[x][z]))
                                {
                                    case AIR:
                                    case CAVE_AIR:
                                    case VOID_AIR:
                                        snowconeData[x][z] = snowData;
                                        snowcone[x][z] = BlockTypes.SNOW.getInternalId();
                                    case SNOW_BLOCK:
                                        snowconeData[x][z] = snowData;
                                        break;
                                    default:
                                        break;

                                }
                            }
                            else if (yOffset[x][z] > 0 && snowcone[x][z] == BlockTypes.SNOW.getInternalId())
                            {
                                snowconeData[x][z]++;
                                if (snowconeData[x][z] == 7)
                                {
                                    snowconeData[x][z] = 0;
                                    snowcone[x][z] = BlockTypes.SNOW_BLOCK.getInternalId();
                                }
                            }
                            break;
                    }
                }
            }
        }

        final Undo undo = new Undo();

        for (int x = 0; x <= brushSizeDoubled; x++)
        {
            for (int z = 0; z <= brushSizeDoubled; z++)
            {

                if (this.getBlockIdAt(blockPositionX - brushSize + x, blockPositionY - yOffset[x][z], blockPositionZ - brushSize + z) != snowcone[x][z] || this.clampY(blockPositionX - brushSize + x, blockPositionY - yOffset[x][z], blockPositionZ - brushSize + z).getPropertyId() != snowconeData[x][z])
                {
                    undo.put(this.clampY(blockPositionX - brushSize + x, blockPositionY - yOffset[x][z], blockPositionZ - brushSize + z));
                }
                this.setBlockIdAt(blockPositionZ - brushSize + z, blockPositionX - brushSize + x, blockPositionY - yOffset[x][z], snowcone[x][z]);
                this.clampY(blockPositionX - brushSize + x, blockPositionY - yOffset[x][z], blockPositionZ - brushSize + z).setPropertyId(snowconeData[x][z]);

            }
        }
        v.owner().storeUndo(undo);
    }

    @Override
    protected final void arrow(final SnipeData v)
    {
    }

    @Override
    protected final void powder(final SnipeData v)
    {
        switch (getTargetBlock().getType())
        {
            case SNOW:
                this.addSnow(v, this.getTargetBlock());
                break;
            default:
                Block blockAbove = getTargetBlock().getRelative(BlockFace.UP);
                if (blockAbove != null && BukkitAdapter.adapt(blockAbove.getType()).getMaterial().isAir())
                {
                    addSnow(v, blockAbove);
                }
                else
                {
                    v.owner().getPlayer().sendMessage(ChatColor.RED + "Error: Center block neither snow nor air.");
                }
                break;
        }
    }

    @Override
    public final void info(final Message vm)
    {
        vm.brushName("Snow Cone");
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v)
    {
        if (par[1].equalsIgnoreCase("info"))
        {
            v.sendMessage(ChatColor.GOLD + "Snow Cone Parameters:");
        }
    }

    @Override
    public String getPermissionNode()
    {
        return "voxelsniper.brush.snowcone";
    }
}
