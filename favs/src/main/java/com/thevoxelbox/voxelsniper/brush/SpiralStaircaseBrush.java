package com.thevoxelbox.voxelsniper.brush;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.LegacyMapper;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Undo;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;

/**
 * http://www.voxelwiki.com/minecraft/Voxelsniper#Spiral_Staircase_Brush
 *
 * @author giltwist
 */
public class SpiralStaircaseBrush extends Brush
{
    private String stairtype = "block"; // "block" 1x1 blocks (default), "step" alternating step double step, "stair" staircase with blocks on corners
    private String sdirect = "c"; // "c" clockwise (default), "cc" counter-clockwise
    private String sopen = "n"; // "n" north (default), "e" east, "world" south, "world" west

    /**
     *
     */
    public SpiralStaircaseBrush()
    {
        this.setName("Spiral Staircase");
    }

    @SuppressWarnings("deprecation")
	private void buildStairWell(final SnipeData v, Block targetBlock)
    {
        if (v.getVoxelHeight() < 1)
        {
            v.setVoxelHeight(1);
            v.sendMessage(ChatColor.RED + "VoxelHeight must be a natural number! Set to 1.");
        }

        final int[][][] spiral = new int[2 * v.getBrushSize() + 1][v.getVoxelHeight()][2 * v.getBrushSize() + 1];

        // locate first block in staircase
        // Note to self, fix these
        int startX = 0;
        int startZ = 0;
        int y = 0;
        int xOffset = 0;
        int zOffset = 0;
        int toggle = 0;

        if (this.sdirect.equalsIgnoreCase("cc"))
        {
            if (this.sopen.equalsIgnoreCase("n"))
            {
                startX = 0;
                startZ = 2 * v.getBrushSize();
            }
            else if (this.sopen.equalsIgnoreCase("e"))
            {
                startX = 0;
                startZ = 0;
            }
            else if (this.sopen.equalsIgnoreCase("s"))
            {
                startX = 2 * v.getBrushSize();
                startZ = 0;
            }
            else
            {
                startX = 2 * v.getBrushSize();
                startZ = 2 * v.getBrushSize();
            }
        }
        else
        {
            if (this.sopen.equalsIgnoreCase("n"))
            {
                startX = 0;
                startZ = 0;
            }
            else if (this.sopen.equalsIgnoreCase("e"))
            {
                startX = 2 * v.getBrushSize();
                startZ = 0;
            }
            else if (this.sopen.equalsIgnoreCase("s"))
            {
                startX = 2 * v.getBrushSize();
                startZ = 2 * v.getBrushSize();
            }
            else
            {
                startX = 0;
                startZ = 2 * v.getBrushSize();
            }
        }

        while (y < v.getVoxelHeight())
        {
            if (this.stairtype.equalsIgnoreCase("block"))
            {
                // 1x1x1 voxel material steps
                spiral[startX + xOffset][y][startZ + zOffset] = 1;
                y++;
            }
            else if (this.stairtype.equalsIgnoreCase("step"))
            {
                // alternating step-doublestep, uses data value to determine type
                switch (toggle)
                {
                    case 0:
                        toggle = 2;
                        spiral[startX + xOffset][y][startZ + zOffset] = 1;
                        break;
                    case 1:
                        toggle = 2;
                        spiral[startX + xOffset][y][startZ + zOffset] = 1;
                        break;
                    case 2:
                        toggle = 1;
                        spiral[startX + xOffset][y][startZ + zOffset] = 2;
                        y++;
                        break;
                    default:
                        break;
                }

            }

            // Adjust horizontal position and do stair-option array stuff
            if (startX + xOffset == 0)
            { // All North
                if (startZ + zOffset == 0)
                { // NORTHEAST
                    if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                    {
                        spiral[startX + xOffset][y][startZ + zOffset] = 1;
                    }
                    if (this.sdirect.equalsIgnoreCase("c"))
                    {
                        xOffset++;
                    }
                    else
                    {
                        zOffset++;
                    }
                }
                else if (startZ + zOffset == 2 * v.getBrushSize())
                { // NORTHWEST
                    if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                    {
                        spiral[startX + xOffset][y][startZ + zOffset] = 1;
                    }
                    if (this.sdirect.equalsIgnoreCase("c"))
                    {
                        zOffset--;
                    }
                    else
                    {
                        xOffset++;
                    }
                }
                else
                { // JUST PLAIN NORTH
                    if (this.sdirect.equalsIgnoreCase("c"))
                    {
                        if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                        {
                            spiral[startX + xOffset][y][startZ + zOffset] = 5;
                            y++;
                        }
                        zOffset--;
                    }
                    else
                    {
                        if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                        {
                            spiral[startX + xOffset][y][startZ + zOffset] = 4;
                            y++;
                        }
                        zOffset++;
                    }
                }
            }
            else if (startX + xOffset == 2 * v.getBrushSize())
            { // ALL SOUTH
                if (startZ + zOffset == 0)
                { // SOUTHEAST
                    if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                    {
                        spiral[startX + xOffset][y][startZ + zOffset] = 1;
                    }
                    if (this.sdirect.equalsIgnoreCase("c"))
                    {
                        zOffset++;
                    }
                    else
                    {
                        xOffset--;
                    }
                }
                else if (startZ + zOffset == 2 * v.getBrushSize())
                { // SOUTHWEST
                    if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                    {
                        spiral[startX + xOffset][y][startZ + zOffset] = 1;
                    }
                    if (this.sdirect.equalsIgnoreCase("c"))
                    {
                        xOffset--;
                    }
                    else
                    {
                        zOffset--;
                    }
                }
                else
                { // JUST PLAIN SOUTH
                    if (this.sdirect.equalsIgnoreCase("c"))
                    {
                        if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                        {
                            spiral[startX + xOffset][y][startZ + zOffset] = 4;
                            y++;
                        }
                        zOffset++;
                    }
                    else
                    {
                        if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                        {
                            spiral[startX + xOffset][y][startZ + zOffset] = 5;
                            y++;
                        }
                        zOffset--;
                    }
                }
            }
            else if (startZ + zOffset == 0)
            { // JUST PLAIN EAST
                if (this.sdirect.equalsIgnoreCase("c"))
                {
                    if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                    {
                        spiral[startX + xOffset][y][startZ + zOffset] = 2;
                        y++;
                    }
                    xOffset++;
                }
                else
                {
                    if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                    {
                        spiral[startX + xOffset][y][startZ + zOffset] = 3;
                        y++;
                    }
                    xOffset--;
                }
            }
            else
            { // JUST PLAIN WEST
                if (this.sdirect.equalsIgnoreCase("c"))
                {
                    if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                    {
                        spiral[startX + xOffset][y][startZ + zOffset] = 3;
                        y++;
                    }
                    xOffset--;
                }
                else
                {
                    if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                    {
                        spiral[startX + xOffset][y][startZ + zOffset] = 2;
                        y++;
                    }
                    xOffset++;
                }
            }
        }

        final Undo undo = new Undo();
        // Make the changes

        for (int x = 2 * v.getBrushSize(); x >= 0; x--)
        {
            for (int i = v.getVoxelHeight() - 1; i >= 0; i--)
            {
                for (int z = 2 * v.getBrushSize(); z >= 0; z--)
                {
                    int blockPositionX = targetBlock.getX();
                    int blockPositionY = targetBlock.getY();
                    int blockPositionZ = targetBlock.getZ();
                    switch (spiral[x][i][z])
                    {
                        case 0:
                            if (i != v.getVoxelHeight() - 1)
                            {
                                if (!((this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair")) && spiral[x][i + 1][z] == 1))
                                {
                                    this.setBlockIdAt(blockPositionZ - v.getBrushSize() + z, blockPositionX - v.getBrushSize() + x, blockPositionY + i, BlockTypes.AIR.getInternalId());
                                }

                            }
                            else
                            {
                                this.setBlockIdAt(blockPositionZ - v.getBrushSize() + z, blockPositionX - v.getBrushSize() + x, blockPositionY + i, BlockTypes.AIR.getInternalId());
                            }

                            break;
                        case 1:
                            switch (stairtype) {

                            }
                            if (this.stairtype.equalsIgnoreCase("block"))
                            {
                                this.setBlockIdAt(blockPositionZ - v.getBrushSize() + z, blockPositionX - v.getBrushSize() + x, blockPositionY + i, v.getVoxelId());
                            }
                            else if (this.stairtype.equalsIgnoreCase("step"))
                            {
                                this.setBlockLegacy(blockPositionX - v.getBrushSize() + x, blockPositionY + i, blockPositionZ - v.getBrushSize() + z, 44, v.getPropertyId());
                            }
                            else if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                            {
                                this.setBlockIdAt(blockPositionZ - v.getBrushSize() + z, blockPositionX - v.getBrushSize() + x, blockPositionY + i - 1, v.getVoxelId());

                            }
                            break;
                        case 2:
                            if (this.stairtype.equalsIgnoreCase("step"))
                            {
                                this.setBlockLegacy(blockPositionX - v.getBrushSize() + x, blockPositionY + i, blockPositionZ - v.getBrushSize() + z, 43, v.getPropertyId());
                            }
                            else if (this.stairtype.equalsIgnoreCase("woodstair"))
                            {
                                this.setBlockLegacy(blockPositionX - v.getBrushSize() + x, blockPositionY + i, blockPositionZ - v.getBrushSize() + z,  53, 0);
                            }
                            else if (this.stairtype.equalsIgnoreCase("cobblestair"))
                            {
                                this.setBlockLegacy(blockPositionX - v.getBrushSize() + x, blockPositionY + i, blockPositionZ - v.getBrushSize() + z,  67, 0);
                            }
                            break;
                        default:
                            if (this.stairtype.equalsIgnoreCase("woodstair"))
                            {
                                this.setBlockLegacy(blockPositionX - v.getBrushSize() + x, blockPositionY + i, blockPositionZ - v.getBrushSize() + z, 53, (spiral[x][i][z] - 2));
                            }
                            else if (this.stairtype.equalsIgnoreCase("cobblestair"))
                            {
                                this.setBlockLegacy(blockPositionX - v.getBrushSize() + x, blockPositionY + i, blockPositionZ - v.getBrushSize() + z, 67, (spiral[x][i][z] - 2));
                            }
                            break;
                    }
                }
            }
        }
        v.owner().storeUndo(undo);
    }

    @SuppressWarnings("deprecation")
	private void digStairWell(final SnipeData v, Block targetBlock)
    {
        if (v.getVoxelHeight() < 1)
        {
            v.setVoxelHeight(1);
            v.sendMessage(ChatColor.RED + "VoxelHeight must be a natural number! Set to 1.");
        }

        // initialize array
        final int[][][] spiral = new int[2 * v.getBrushSize() + 1][v.getVoxelHeight()][2 * v.getBrushSize() + 1];

        // locate first block in staircase
        // Note to self, fix these
        int startX = 0;
        int startZ = 0;
        int y = 0;
        int xOffset = 0;
        int zOffset = 0;
        int toggle = 0;

        if (this.sdirect.equalsIgnoreCase("cc"))
        {
            if (this.sopen.equalsIgnoreCase("n"))
            {
                startX = 0;
                startZ = 2 * v.getBrushSize();
            }
            else if (this.sopen.equalsIgnoreCase("e"))
            {
                startX = 0;
                startZ = 0;
            }
            else if (this.sopen.equalsIgnoreCase("s"))
            {
                startX = 2 * v.getBrushSize();
                startZ = 0;
            }
            else
            {
                startX = 2 * v.getBrushSize();
                startZ = 2 * v.getBrushSize();
            }
        }
        else
        {
            if (this.sopen.equalsIgnoreCase("n"))
            {
                startX = 0;
                startZ = 0;
            }
            else if (this.sopen.equalsIgnoreCase("e"))
            {
                startX = 2 * v.getBrushSize();
                startZ = 0;
            }
            else if (this.sopen.equalsIgnoreCase("s"))
            {
                startX = 2 * v.getBrushSize();
                startZ = 2 * v.getBrushSize();
            }
            else
            {
                startX = 0;
                startZ = 2 * v.getBrushSize();
            }
        }

        while (y < v.getVoxelHeight())
        {
            if (this.stairtype.equalsIgnoreCase("block"))
            {
                // 1x1x1 voxel material steps
                spiral[startX + xOffset][y][startZ + zOffset] = 1;
                y++;
            }
            else if (this.stairtype.equalsIgnoreCase("step"))
            {
                // alternating step-doublestep, uses data value to determine type
                switch (toggle)
                {
                    case 0:
                        toggle = 2;
                        spiral[startX + xOffset][y][startZ + zOffset] = 2;
                        break;
                    case 1:
                        toggle = 2;
                        spiral[startX + xOffset][y][startZ + zOffset] = 2;
                        break;
                    case 2:
                        toggle = 1;
                        spiral[startX + xOffset][y][startZ + zOffset] = 1;
                        y++;
                        break;
                    default:
                        break;
                }

            }

            // Adjust horizontal position and do stair-option array stuff
            if (startX + xOffset == 0)
            { // All North
                if (startZ + zOffset == 0)
                { // NORTHEAST
                    if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                    {
                        spiral[startX + xOffset][y][startZ + zOffset] = 1;
                    }
                    if (this.sdirect.equalsIgnoreCase("c"))
                    {
                        xOffset++;
                    }
                    else
                    {
                        zOffset++;
                    }
                }
                else if (startZ + zOffset == 2 * v.getBrushSize())
                { // NORTHWEST
                    if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                    {
                        spiral[startX + xOffset][y][startZ + zOffset] = 1;
                    }
                    if (this.sdirect.equalsIgnoreCase("c"))
                    {
                        zOffset--;
                    }
                    else
                    {
                        xOffset++;
                    }
                }
                else
                { // JUST PLAIN NORTH
                    if (this.sdirect.equalsIgnoreCase("c"))
                    {
                        if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                        {
                            spiral[startX + xOffset][y][startZ + zOffset] = 4;
                            y++;
                        }
                        zOffset--;
                    }
                    else
                    {
                        if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                        {
                            spiral[startX + xOffset][y][startZ + zOffset] = 5;
                            y++;
                        }
                        zOffset++;
                    }
                }

            }
            else if (startX + xOffset == 2 * v.getBrushSize())
            { // ALL SOUTH
                if (startZ + zOffset == 0)
                { // SOUTHEAST
                    if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                    {
                        spiral[startX + xOffset][y][startZ + zOffset] = 1;
                    }
                    if (this.sdirect.equalsIgnoreCase("c"))
                    {
                        zOffset++;
                    }
                    else
                    {
                        xOffset--;
                    }
                }
                else if (startZ + zOffset == 2 * v.getBrushSize())
                { // SOUTHWEST
                    if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                    {
                        spiral[startX + xOffset][y][startZ + zOffset] = 1;
                    }
                    if (this.sdirect.equalsIgnoreCase("c"))
                    {
                        xOffset--;
                    }
                    else
                    {
                        zOffset--;
                    }
                }
                else
                { // JUST PLAIN SOUTH
                    if (this.sdirect.equalsIgnoreCase("c"))
                    {
                        if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                        {
                            spiral[startX + xOffset][y][startZ + zOffset] = 5;
                            y++;
                        }
                        zOffset++;
                    }
                    else
                    {
                        if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                        {
                            spiral[startX + xOffset][y][startZ + zOffset] = 4;
                            y++;
                        }
                        zOffset--;
                    }
                }

            }
            else if (startZ + zOffset == 0)
            { // JUST PLAIN EAST
                if (this.sdirect.equalsIgnoreCase("c"))
                {
                    if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                    {
                        spiral[startX + xOffset][y][startZ + zOffset] = 3;
                        y++;
                    }
                    xOffset++;
                }
                else
                {
                    if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                    {
                        spiral[startX + xOffset][y][startZ + zOffset] = 2;
                        y++;
                    }
                    xOffset--;
                }
            }
            else
            { // JUST PLAIN WEST
                if (this.sdirect.equalsIgnoreCase("c"))
                {
                    if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                    {
                        spiral[startX + xOffset][y][startZ + zOffset] = 2;
                        y++;
                    }
                    xOffset--;
                }
                else
                {
                    if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                    {
                        spiral[startX + xOffset][y][startZ + zOffset] = 3;
                        y++;
                    }
                    xOffset++;
                }
            }

        }

        final Undo undo = new Undo();
        // Make the changes

        for (int x = 2 * v.getBrushSize(); x >= 0; x--)
        {

            for (int i = v.getVoxelHeight() - 1; i >= 0; i--)
            {

                for (int z = 2 * v.getBrushSize(); z >= 0; z--)
                {

                    int blockPositionX = targetBlock.getX();
                    int blockPositionY = targetBlock.getY();
                    int blockPositionZ = targetBlock.getZ();
                    switch (spiral[x][i][z])
                    {
                        case 0:
                            this.setBlockIdAt(blockPositionZ - v.getBrushSize() + z, blockPositionX - v.getBrushSize() + x, blockPositionY - i, BlockTypes.AIR.getInternalId());
                            break;
                        case 1:
                            if (this.stairtype.equalsIgnoreCase("block"))
                            {
                                this.setBlockIdAt(blockPositionZ - v.getBrushSize() + z, blockPositionX - v.getBrushSize() + x, blockPositionY - i, v.getVoxelId());
                            }
                            else if (this.stairtype.equalsIgnoreCase("step"))
                            {
                                this.setBlockLegacy(blockPositionX - v.getBrushSize() + x, blockPositionY - i, blockPositionZ - v.getBrushSize() + z, 44, v.getPropertyId());
                            }
                            else if (this.stairtype.equalsIgnoreCase("woodstair") || this.stairtype.equalsIgnoreCase("cobblestair"))
                            {
                                this.setBlockIdAt(blockPositionZ - v.getBrushSize() + z, blockPositionX - v.getBrushSize() + x, blockPositionY - i, v.getVoxelId());
                            }
                            break;
                        case 2:
                            if (this.stairtype.equalsIgnoreCase("step"))
                            {
                                this.setBlockLegacy(blockPositionX - v.getBrushSize() + x, blockPositionY - i, blockPositionZ - v.getBrushSize() + z, 43, v.getPropertyId());
                            }
                            else if (this.stairtype.equalsIgnoreCase("woodstair"))
                            {
                                this.setBlockLegacy(blockPositionX - v.getBrushSize() + x, blockPositionY - i, blockPositionZ - v.getBrushSize() + z,  53, 0);
                            }
                            else if (this.stairtype.equalsIgnoreCase("cobblestair"))
                            {
                                this.setBlockLegacy(blockPositionX - v.getBrushSize() + x, blockPositionY - i, blockPositionZ - v.getBrushSize() + z, 67, 0);
                            }
                            break;
                        default:
                            if (this.stairtype.equalsIgnoreCase("woodstair"))
                            {
                                this.setBlockLegacy(blockPositionX - v.getBrushSize() + x, blockPositionY - i, blockPositionZ - v.getBrushSize() + z, 53, (spiral[x][i][z] - 2));
                            }
                            else if (this.stairtype.equalsIgnoreCase("cobblestair"))
                            {
                                this.setBlockLegacy(blockPositionZ - v.getBrushSize() + z, blockPositionX - v.getBrushSize() + x, blockPositionY - i, 67, (spiral[x][i][z] - 2));
                            }
                            break;
                    }
                }
            }
        }
        v.owner().storeUndo(undo);
    }

    @Override
    protected final void arrow(final SnipeData v)
    {
        this.digStairWell(v, this.getTargetBlock()); // make stairwell below target
    }

    @Override
    protected final void powder(final SnipeData v)
    {
        this.buildStairWell(v, this.getLastBlock()); // make stairwell above target
    }

    @Override
    public final void info(final Message vm)
    {
        vm.brushName("Spiral Staircase");
        vm.size();
        vm.voxel();
        vm.height();
        vm.data();
        vm.custom(ChatColor.BLUE + "Staircase type: " + this.stairtype);
        vm.custom(ChatColor.BLUE + "Staircase turns: " + this.sdirect);
        vm.custom(ChatColor.BLUE + "Staircase opens: " + this.sopen);
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v)
    {
        if (par[1].equalsIgnoreCase("info"))
        {
            v.sendMessage(ChatColor.GOLD + "Spiral Staircase Parameters:");
            v.sendMessage(ChatColor.AQUA + "/b sstair 'block' (default) | step' | 'woodstair' | 'cobblestair' -- set the type of staircase");
            v.sendMessage(ChatColor.AQUA + "/b sstair 'c' (default) | 'cc' -- set the turning direction of staircase");
            v.sendMessage(ChatColor.AQUA + "/b sstair 'n' (default) | 'e' | 's' | 'world' -- set the opening direction of staircase");
            return;
        }

        for (int i = 1; i < par.length; i++)
        {
            // stairs
            // step/slab

            try {
                BlockType type = BlockTypes.parse(par[i]);
                this.stairtype = par[i].toLowerCase().intern();
                v.sendMessage(ChatColor.BLUE + "Staircase type: " + this.stairtype);
                return;
            } catch (InputParseException ignore) {}
            if ("block".equals(par[i].toLowerCase())) {
            }
            if (par[i].equalsIgnoreCase("block") || par[i].equalsIgnoreCase("step") || par[i].equalsIgnoreCase("woodstair") || par[i].equalsIgnoreCase("cobblestair"))
            {
                this.stairtype = par[i].toLowerCase().intern();
                v.sendMessage(ChatColor.BLUE + "Staircase type: " + this.stairtype);
            }
            else if (par[i].equalsIgnoreCase("c") || par[i].equalsIgnoreCase("cc"))
            {
                this.sdirect = par[i];
                v.sendMessage(ChatColor.BLUE + "Staircase turns: " + this.sdirect);
            }
            else if (par[i].equalsIgnoreCase("n") || par[i].equalsIgnoreCase("e") || par[i].equalsIgnoreCase("s") || par[i].equalsIgnoreCase("world"))
            {
                this.sopen = par[i];
                v.sendMessage(ChatColor.BLUE + "Staircase opens: " + this.sopen);
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
        return "voxelsniper.brush.spiralstaircase";
    }
}
