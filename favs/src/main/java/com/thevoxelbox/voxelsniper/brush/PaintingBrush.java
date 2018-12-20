package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.PaintingWrapper;
import com.thevoxelbox.voxelsniper.SnipeData;

/**
 * Painting scrolling Brush.
 * http://www.voxelwiki.com/minecraft/Voxelsniper#The_Painting_Picker_Brush
 *
 * @author Voxel
 */
public class PaintingBrush extends Brush
{
    /**
     *
     */
    public PaintingBrush()
    {
        this.setName("Painting");
    }

    /**
     * Scroll painting forward.
     *
     * @param v Sniper caller
     */
    @Override
    protected final void arrow(final SnipeData v)
    {
        PaintingWrapper.paint(v.owner().getPlayer(), true, false, 0);
    }

    /**
     * Scroll painting backwards.
     *
     * @param v Sniper caller
     */
    @Override
    protected final void powder(final SnipeData v)
    {
        PaintingWrapper.paint(v.owner().getPlayer(), true, true, 0);
    }

    @Override
    public final void info(final Message vm)
    {
        vm.brushName(this.getName());
    }

    @Override
    public String getPermissionNode()
    {
        return "voxelsniper.brush.painting";
    }
}
