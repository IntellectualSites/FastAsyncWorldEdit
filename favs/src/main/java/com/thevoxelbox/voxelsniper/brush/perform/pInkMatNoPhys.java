package com.thevoxelbox.voxelsniper.brush.perform;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;

/**
 * @author Voxel
 */
public class pInkMatNoPhys extends vPerformer
{

    private int d;
    private int ir;

    public pInkMatNoPhys()
    {
        name = "Ink-Mat, No Physics";
    }

    @Override
    public void init(com.thevoxelbox.voxelsniper.SnipeData v)
    {
        w = v.getWorld();
        d = v.getPropertyId();
        ir = v.getReplaceId();
    }

    @Override
    public void info(Message vm)
    {
        vm.performerName(name);
        vm.data();
        vm.replace();
    }

    @SuppressWarnings("deprecation")
	@Override
    public void perform(AsyncBlock b)
    {
        if (b.getTypeId() == ir)
        {
            h.put(b);
            b.setPropertyId(d);
        }
    }

    @Override
    public boolean isUsingReplaceMaterial()
    {
        return true;
    }
}