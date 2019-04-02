package com.thevoxelbox.voxelsniper.brush;

import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;
import org.bukkit.ChatColor;

import java.util.Random;

/**
 * http://www.voxelwiki.com/minecraft/Voxelsniper#Splatter_Overlay_Brush
 *
 * @author Gavjenks Splatterized blockPositionY Giltwist
 */
public class SplatterOverlayBrush extends PerformBrush
{
    private static final int GROW_PERCENT_MIN = 1;
    private static final int GROW_PERCENT_DEFAULT = 1000;
    private static final int GROW_PERCENT_MAX = 9999;
    private static final int SEED_PERCENT_MIN = 1;
    private static final int SEED_PERCENT_DEFAULT = 1000;
    private static final int SEED_PERCENT_MAX = 9999;
    private static final int SPLATREC_PERCENT_MIN = 1;
    private static final int SPLATREC_PERCENT_DEFAULT = 3;
    private static final int SPLATREC_PERCENT_MAX = 10;
    private int seedPercent; // Chance block on first pass is made active
    private int growPercent; // chance block on recursion pass is made active
    private int splatterRecursions; // How many times you grow the seeds
    private int yOffset = 0;
    private boolean randomizeHeight = false;
    private Random generator = new Random();
    private int depth = 3;
    private boolean allBlocks = false;

    /**
     *
     */
    public SplatterOverlayBrush()
    {
        this.setName("Splatter Overlay");
    }

    @SuppressWarnings("deprecation")
	private void sOverlay(final SnipeData v)
    {

        // Splatter Time
        final int[][] splat = new int[2 * v.getBrushSize() + 1][2 * v.getBrushSize() + 1];
        // Seed the array
        for (int x = 2 * v.getBrushSize(); x >= 0; x--)
        {
            for (int y = 2 * v.getBrushSize(); y >= 0; y--)
            {
                if (this.generator.nextInt(SEED_PERCENT_MAX + 1) <= this.seedPercent)
                {
                    splat[x][y] = 1;
                }
            }
        }
        // Grow the seeds
        final int gref = this.growPercent;
        final int[][] tempSplat = new int[2 * v.getBrushSize() + 1][2 * v.getBrushSize() + 1];
        int growcheck;

        for (int r = 0; r < this.splatterRecursions; r++)
        {
            this.growPercent = gref - ((gref / this.splatterRecursions) * (r));
            for (int x = 2 * v.getBrushSize(); x >= 0; x--)
            {
                for (int y = 2 * v.getBrushSize(); y >= 0; y--)
                {
                    tempSplat[x][y] = splat[x][y]; // prime tempsplat

                    growcheck = 0;
                    if (splat[x][y] == 0)
                    {
                        if (x != 0 && splat[x - 1][y] == 1)
                        {
                            growcheck++;
                        }
                        if (y != 0 && splat[x][y - 1] == 1)
                        {
                            growcheck++;
                        }
                        if (x != 2 * v.getBrushSize() && splat[x + 1][y] == 1)
                        {
                            growcheck++;
                        }
                        if (y != 2 * v.getBrushSize() && splat[x][y + 1] == 1)
                        {
                            growcheck++;
                        }
                    }

                    if (growcheck >= 1 && this.generator.nextInt(GROW_PERCENT_MAX + 1) <= this.growPercent)
                    {
                        tempSplat[x][y] = 1; // prevent bleed into splat
                    }
                }
            }
            // integrate tempsplat back into splat at end of iteration
            for (int x = 2 * v.getBrushSize(); x >= 0; x--)
            {
                if (2 * v.getBrushSize() + 1 >= 0)
                    System.arraycopy(tempSplat[x], 0, splat[x], 0, 2 * v.getBrushSize() + 1);
            }
        }
        this.growPercent = gref;

        final int[][] memory = new int[2 * v.getBrushSize() + 1][2 * v.getBrushSize() + 1];
        final double brushSizeSquared = Math.pow(v.getBrushSize() + 0.5, 2);

        for (int z = v.getBrushSize(); z >= -v.getBrushSize(); z--)
        {
            for (int x = v.getBrushSize(); x >= -v.getBrushSize(); x--)
            {
                for (int y = this.getTargetBlock().getY(); y > 0; y--)
                {
                    // start scanning from the height you clicked at
                    if (memory[x + v.getBrushSize()][z + v.getBrushSize()] != 1)
                    {
                        // if haven't already found the surface in this column
                        if ((Math.pow(x, 2) + Math.pow(z, 2)) <= brushSizeSquared && splat[x + v.getBrushSize()][z + v.getBrushSize()] == 1)
                        {
                            // if inside of the column && if to be splattered
                            final int check = this.getBlockIdAt(this.getTargetBlock().getX() + x, y + 1, this.getTargetBlock().getZ() + z);
                            if (check == 0 || check == 8 || check == 9)
                            {
                                // must start at surface... this prevents it filling stuff in if you click in a wall
                                // and it starts out below surface.
                                if (!this.allBlocks)
                                {
                                    // if the override parameter has not been activated, go to the switch that filters out manmade stuff.
                                    BlockType type = BlockTypes.get(this.getBlockIdAt(this.getTargetBlock().getX() + x, y, this.getTargetBlock().getZ() + z));
                                    BlockMaterial mat = type.getMaterial();
                                    if (mat.isSolid() && mat.isFullCube() && !mat.hasContainer()) {
                                        final int depth = randomizeHeight ? generator.nextInt(this.depth) : this.depth;

                                        for (int d = this.depth - 1; ((this.depth - d) <= depth); d--) {
                                            if (!this.clampY(this.getTargetBlock().getX() + x, y - d, this.getTargetBlock().getZ() + z).isEmpty()) {
                                                // fills down as many layers as you specify in parameters
                                                this.current.perform(this.clampY(this.getTargetBlock().getX() + x, y - d + yOffset, this.getTargetBlock().getZ() + z));
                                                // stop it from checking any other blocks in this vertical 1x1 column.
                                                memory[x + v.getBrushSize()][z + v.getBrushSize()] = 1;
                                            }
                                        }
                                        continue;
                                    } else {
                                        continue;
                                    }
                                }
                                else
                                {
                                    final int depth = randomizeHeight ? generator.nextInt(this.depth) : this.depth;
                                    for (int d = this.depth - 1; ((this.depth - d) <= depth); d--)
                                    {
                                        if (!this.clampY(this.getTargetBlock().getX() + x, y - d, this.getTargetBlock().getZ() + z).isEmpty())
                                        {
                                            // fills down as many layers as you specify in parameters
                                            this.current.perform(this.clampY(this.getTargetBlock().getX() + x, y - d + yOffset, this.getTargetBlock().getZ() + z));
                                            // stop it from checking any other blocks in this vertical 1x1 column.
                                            memory[x + v.getBrushSize()][z + v.getBrushSize()] = 1;
                                        }
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

    private void soverlayTwo(final SnipeData v)
    {
        // Splatter Time
        final int[][] splat = new int[2 * v.getBrushSize() + 1][2 * v.getBrushSize() + 1];
        // Seed the array
        for (int x = 2 * v.getBrushSize(); x >= 0; x--)
        {
            for (int y = 2 * v.getBrushSize(); y >= 0; y--)
            {
                if (this.generator.nextInt(SEED_PERCENT_MAX + 1) <= this.seedPercent)
                {
                    splat[x][y] = 1;
                }
            }
        }
        // Grow the seeds
        final int gref = this.growPercent;
        final int[][] tempsplat = new int[2 * v.getBrushSize() + 1][2 * v.getBrushSize() + 1];
        int growcheck;

        for (int r = 0; r < this.splatterRecursions; r++)
        {
            this.growPercent = gref - ((gref / this.splatterRecursions) * (r));

            for (int x = 2 * v.getBrushSize(); x >= 0; x--)
            {
                for (int y = 2 * v.getBrushSize(); y >= 0; y--)
                {
                    tempsplat[x][y] = splat[x][y]; // prime tempsplat

                    growcheck = 0;
                    if (splat[x][y] == 0)
                    {
                        if (x != 0 && splat[x - 1][y] == 1)
                        {
                            growcheck++;
                        }
                        if (y != 0 && splat[x][y - 1] == 1)
                        {
                            growcheck++;
                        }
                        if (x != 2 * v.getBrushSize() && splat[x + 1][y] == 1)
                        {
                            growcheck++;
                        }
                        if (y != 2 * v.getBrushSize() && splat[x][y + 1] == 1)
                        {
                            growcheck++;
                        }
                    }

                    if (growcheck >= 1 && this.generator.nextInt(GROW_PERCENT_MAX + 1) <= this.growPercent)
                    {
                        tempsplat[x][y] = 1; // prevent bleed into splat
                    }

                }

            }
            // integrate tempsplat back into splat at end of iteration
            for (int x = 2 * v.getBrushSize(); x >= 0; x--)
            {
                if (2 * v.getBrushSize() + 1 >= 0)
                    System.arraycopy(tempsplat[x], 0, splat[x], 0, 2 * v.getBrushSize() + 1);
            }
        }
        this.growPercent = gref;

        final int[][] memory = new int[v.getBrushSize() * 2 + 1][v.getBrushSize() * 2 + 1];
        final double brushSizeSquared = Math.pow(v.getBrushSize() + 0.5, 2);

        for (int z = v.getBrushSize(); z >= -v.getBrushSize(); z--)
        {
            for (int x = v.getBrushSize(); x >= -v.getBrushSize(); x--)
            {
                for (int y = this.getTargetBlock().getY(); y > 0; y--)
                { // start scanning from the height you clicked at
                    if (memory[x + v.getBrushSize()][z + v.getBrushSize()] != 1)
                    { // if haven't already found the surface in this column
                        if ((Math.pow(x, 2) + Math.pow(z, 2)) <= brushSizeSquared && splat[x + v.getBrushSize()][z + v.getBrushSize()] == 1)
                        { // if inside of the column...&& if to be splattered
                            if (!this.getBlockAt(this.getTargetBlock().getX() + x, y - 1, this.getTargetBlock().getZ() + z).isEmpty())
                            { // if not a floating block (like one of Notch'world pools)
                                if (this.getBlockAt(this.getTargetBlock().getX() + x, y + 1, this.getTargetBlock().getZ() + z).isEmpty())
                                { // must start at surface... this prevents it filling stuff in if
                                    // you click in a wall and it starts out below surface.
                                    if (!this.allBlocks)
                                    { // if the override parameter has not been activated, go to the switch that filters out manmade stuff.

                                        BlockType type = BlockTypes.get(this.getBlockIdAt(this.getTargetBlock().getX() + x, y, this.getTargetBlock().getZ() + z));
                                        BlockMaterial mat = type.getMaterial();
                                        if (mat.isSolid() && mat.isFullCube() && !mat.hasContainer())
                                        {
                                                final int depth = randomizeHeight ? generator.nextInt(this.depth) : this.depth;
                                                for (int d = 1; (d < depth + 1); d++) {
                                                    this.current.perform(this.clampY(this.getTargetBlock().getX() + x, y + d + yOffset, this.getTargetBlock().getZ() + z)); // fills down as many layers as you specify
                                                    // in parameters
                                                    memory[x + v.getBrushSize()][z + v.getBrushSize()] = 1; // stop it from checking any other blocks in this vertical 1x1 column.
                                                }
                                                continue;
                                        } else {
                                            continue;
                                        }
                                    }
                                    else
                                    {
                                        final int depth = randomizeHeight ? generator.nextInt(this.depth) : this.depth;
                                        for (int d = 1; (d < depth + 1); d++)
                                        {
                                            this.current.perform(this.clampY(this.getTargetBlock().getX() + x, y + d + yOffset, this.getTargetBlock().getZ() + z)); // fills down as many layers as you specify in
                                            // parameters
                                            memory[x + v.getBrushSize()][z + v.getBrushSize()] = 1; // stop it from checking any other blocks in this vertical 1x1 column.
                                        }
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
        this.sOverlay(v);
    }

    @Override
    protected final void powder(final SnipeData v)
    {
        this.soverlayTwo(v);
    }

    @Override
    public final void info(final Message vm)
    {
        if (this.seedPercent < SEED_PERCENT_MIN || this.seedPercent > SEED_PERCENT_MAX)
        {
            this.seedPercent = SEED_PERCENT_DEFAULT;
        }
        if (this.growPercent < GROW_PERCENT_MIN || this.growPercent > GROW_PERCENT_MAX)
        {
            this.growPercent = GROW_PERCENT_DEFAULT;
        }
        if (this.splatterRecursions < SPLATREC_PERCENT_MIN || this.splatterRecursions > SPLATREC_PERCENT_MAX)
        {
            this.splatterRecursions = SPLATREC_PERCENT_DEFAULT;
        }
        vm.brushName(this.getName());
        vm.size();
        vm.custom(ChatColor.BLUE + "Seed percent set to: " + this.seedPercent / 100 + "%");
        vm.custom(ChatColor.BLUE + "Growth percent set to: " + this.growPercent / 100 + "%");
        vm.custom(ChatColor.BLUE + "Recursions set to: " + this.splatterRecursions);
        vm.custom(ChatColor.BLUE + "Y-Offset set to: " + this.yOffset);
    }

    @Override
    public final void parameters(final String[] par, final SnipeData v)
    {
        for (int i = 1; i < par.length; i++)
        {
            final String parameter = par[i];
            try
            {
                if (parameter.equalsIgnoreCase("info"))
                {
                    v.sendMessage(ChatColor.GOLD + "Splatter Overlay brush parameters:");
                    v.sendMessage(ChatColor.AQUA + "d[number] (ex:  d3) How many blocks deep you want to replace from the surface.");
                    v.sendMessage(ChatColor.BLUE + "all (ex:  /b over all) Sets the brush to overlay over ALL materials, not just natural surface ones (will no longer ignore trees and buildings).  The parameter /some will set it back to default.");
                    v.sendMessage(ChatColor.AQUA + "/b sover s[int] -- set a seed percentage (1-9999). 100 = 1% Default is 1000");
                    v.sendMessage(ChatColor.AQUA + "/b sover g[int] -- set a growth percentage (1-9999).  Default is 1000");
                    v.sendMessage(ChatColor.AQUA + "/b sover r[int] -- set a recursion (1-10).  Default is 3");
                    return;
                }
                else if (parameter.startsWith("d"))
                {
                    this.depth = Integer.parseInt(parameter.replace("d", ""));
                    v.sendMessage(ChatColor.AQUA + "Depth set to " + this.depth);
                    if (this.depth < 1)
                    {
                        this.depth = 1;
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
                else if (par[i].startsWith("s"))
                {
                    final double temp = Integer.parseInt(parameter.replace("s", ""));
                    if (temp >= SEED_PERCENT_MIN && temp <= SEED_PERCENT_MAX)
                    {
                        v.sendMessage(ChatColor.AQUA + "Seed percent set to: " + temp / 100 + "%");
                        this.seedPercent = (int) temp;
                    }
                    else
                    {
                        v.sendMessage(ChatColor.RED + "Seed percent must be an integer 1-9999!");
                    }
                }
                else if (parameter.startsWith("g"))
                {
                    final double temp = Integer.parseInt(parameter.replace("g", ""));
                    if (temp >= GROW_PERCENT_MIN && temp <= GROW_PERCENT_MAX)
                    {
                        v.sendMessage(ChatColor.AQUA + "Growth percent set to: " + temp / 100 + "%");
                        this.growPercent = (int) temp;
                    }
                    else
                    {
                        v.sendMessage(ChatColor.RED + "Growth percent must be an integer 1-9999!");
                    }
                }
                else if (parameter.startsWith("randh"))
                {
                    randomizeHeight = !randomizeHeight;
                    v.sendMessage(ChatColor.RED + "RandomizeHeight set to: " + randomizeHeight);
                }
                else if (parameter.startsWith("r"))
                {
                    final int temp = Integer.parseInt(parameter.replace("r", ""));
                    if (temp >= SPLATREC_PERCENT_MIN && temp <= SPLATREC_PERCENT_MAX)
                    {
                        v.sendMessage(ChatColor.AQUA + "Recursions set to: " + temp);
                        this.splatterRecursions = temp;
                    }
                    else
                    {
                        v.sendMessage(ChatColor.RED + "Recursions must be an integer 1-10!");
                    }
                }
                else if (parameter.startsWith("yoff"))
                {
                    final int temp = Integer.parseInt(parameter.replace("yoff", ""));
                    if (temp >= SPLATREC_PERCENT_MIN && temp <= SPLATREC_PERCENT_MAX)
                    {
                        v.sendMessage(ChatColor.AQUA + "Y-Offset set to: " + temp);
                        this.yOffset = temp;
                    }
                    else
                    {
                        v.sendMessage(ChatColor.RED + "Recursions must be an integer 1-10!");
                    }
                }
                else
                {
                    v.sendMessage(ChatColor.RED + "Invalid brush parameters! use the info parameter to display parameter info.");
                }
            }
            catch (Exception exception)
            {
                v.sendMessage(String.format("An error occured while processing parameter %s.", parameter));
            }
        }
    }

    @Override
    public String getPermissionNode()
    {
        return "voxelsniper.brush.splatteroverlay";
    }
}
