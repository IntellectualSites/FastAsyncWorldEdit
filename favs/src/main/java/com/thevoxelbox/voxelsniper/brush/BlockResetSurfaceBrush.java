package com.thevoxelbox.voxelsniper.brush;

import java.util.ArrayList;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.boydti.fawe.bukkit.wrapper.AsyncWorld;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * This brush only looks for solid blocks, and then changes those plus any air blocks touching them. If it works, this brush should be faster than the original
 * blockPositionY an amount proportional to the volume of a snipe selection area / the number of blocks touching air in the selection. This is because every solid block
 * surrounded blockPositionY others should take equally long to check and not change as it would take MC to change them and then check and find no lighting to update. For
 * air blocks surrounded blockPositionY other air blocks, this brush saves about 80-100 checks blockPositionY not updating them or their lighting. And for air blocks touching solids,
 * this brush is slower, because it replaces the air once per solid block it is touching. I assume on average this is about 2 blocks. So every air block
 * touching a solid negates one air block floating in air. Thus, for selections that have more air blocks surrounded blockPositionY air than air blocks touching solids,
 * this brush will be faster, which is almost always the case, especially for undeveloped terrain and for larger brush sizes (unlike the original brush, this
 * should only slow down blockPositionY the square of the brush size, not the cube of the brush size). For typical terrain, blockPositionY my calculations, overall speed increase is
 * about a factor of 5-6 for a size 20 brush. For a complicated city or ship, etc., this may be only a factor of about 2. In a hypothetical worst case scenario
 * of a 3d checkerboard of stone and air every other block, this brush should only be about 1.5x slower than the original brush. Savings increase for larger
 * brushes.
 *
 * @author GavJenks
 */
public class BlockResetSurfaceBrush extends Brush
{
    /**
     *
     */
    public BlockResetSurfaceBrush()
    {
        this.setName("Block Reset Brush Surface Only");
    }

    @SuppressWarnings("deprecation")
	private void applyBrush(final SnipeData v)
    {
        final AsyncWorld world = this.getWorld();

        for (int z = -v.getBrushSize(); z <= v.getBrushSize(); z++)
        {
            for (int x = -v.getBrushSize(); x <= v.getBrushSize(); x++)
            {
                for (int y = -v.getBrushSize(); y <= v.getBrushSize(); y++)
                {

                    AsyncBlock block = world.getBlockAt(this.getTargetBlock().getX() + x, this.getTargetBlock().getY() + y, this.getTargetBlock().getZ() + z);


                    Material type = block.getType();
                    BlockMaterial mat = BukkitAdapter.adapt(type).getMaterial();
                    if (!mat.isSolid() || !mat.isFullCube() || mat.hasContainer())
                    {
                        continue;
                    }

                    boolean airFound = false;

                    if (world.getBlockAt(this.getTargetBlock().getX() + x + 1, this.getTargetBlock().getY() + y, this.getTargetBlock().getZ() + z).isEmpty())
                    {
                        block = world.getBlockAt(this.getTargetBlock().getX() + x + 1, this.getTargetBlock().getY() + y, this.getTargetBlock().getZ() + z);
                        final int oldData = block.getPropertyId();
                        resetBlock(block, oldData);
                        airFound = true;
                    }

                    if (world.getBlockAt(this.getTargetBlock().getX() + x - 1, this.getTargetBlock().getY() + y, this.getTargetBlock().getZ() + z).isEmpty())
                    {
                        block = world.getBlockAt(this.getTargetBlock().getX() + x - 1, this.getTargetBlock().getY() + y, this.getTargetBlock().getZ() + z);
                        final int oldData = block.getPropertyId();
                        resetBlock(block, oldData);
                        airFound = true;
                    }

                    if (world.getBlockAt(this.getTargetBlock().getX() + x, this.getTargetBlock().getY() + y + 1, this.getTargetBlock().getZ() + z).isEmpty())
                    {
                        block = world.getBlockAt(this.getTargetBlock().getX() + x, this.getTargetBlock().getY() + y + 1, this.getTargetBlock().getZ() + z);
                        final int oldData = block.getPropertyId();
                        resetBlock(block, oldData);
                        airFound = true;
                    }

                    if (world.getBlockAt(this.getTargetBlock().getX() + x, this.getTargetBlock().getY() + y - 1, this.getTargetBlock().getZ() + z).isEmpty())
                    {
                        block = world.getBlockAt(this.getTargetBlock().getX() + x, this.getTargetBlock().getY() + y - 1, this.getTargetBlock().getZ() + z);
                        final int oldData = block.getPropertyId();
                        resetBlock(block, oldData);
                        airFound = true;
                    }

                    if (world.getBlockAt(this.getTargetBlock().getX() + x, this.getTargetBlock().getY() + y, this.getTargetBlock().getZ() + z + 1).isEmpty())
                    {
                        block = world.getBlockAt(this.getTargetBlock().getX() + x, this.getTargetBlock().getY() + y, this.getTargetBlock().getZ() + z + 1);
                        final int oldData = block.getPropertyId();
                        resetBlock(block, oldData);
                        airFound = true;
                    }

                    if (world.getBlockAt(this.getTargetBlock().getX() + x, this.getTargetBlock().getY() + y, this.getTargetBlock().getZ() + z - 1).isEmpty())
                    {
                        block = world.getBlockAt(this.getTargetBlock().getX() + x, this.getTargetBlock().getY() + y, this.getTargetBlock().getZ() + z - 1);
                        final int oldData = block.getPropertyId();
                        resetBlock(block, oldData);
                        airFound = true;
                    }

                    if (airFound)
                    {
                        block = world.getBlockAt(this.getTargetBlock().getX() + x, this.getTargetBlock().getY() + y, this.getTargetBlock().getZ() + z);
                        final int oldData = block.getPropertyId();
                        resetBlock(block, oldData);
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
	private void resetBlock(AsyncBlock block, final int oldData)
    {
        block.setTypeIdAndPropertyId(block.getTypeId(),  ((block.getPropertyId() + 1) & 0xf), true);
        block.setTypeIdAndPropertyId(block.getTypeId(), oldData, true);
    }

    @Override
    protected final void arrow(final SnipeData v)
    {
        applyBrush(v);
    }

    @Override
    protected final void powder(final SnipeData v)
    {
        applyBrush(v);
    }

    @Override
    public final void info(final Message vm)
    {
        vm.brushName(this.getName());
    }

    @Override
    public String getPermissionNode()
    {
        return "voxelsniper.brush.blockresetsurface";
    }
}
