/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thevoxelbox.voxelsniper.brush.perform;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;

/**
 * @author Voxel
 */
public class pComboMat extends vPerformer
{

    private int d;
    private int i;
    private int ir;

    public pComboMat()
    {
        name = "Combo-Mat";
    }

    @Override
    public void init(com.thevoxelbox.voxelsniper.SnipeData v)
    {
        w = v.getWorld();
        d = v.getPropertyId();
        i = v.getVoxelId();
        ir = v.getReplaceId();
    }

    @Override
    public void info(Message vm)
    {
        vm.performerName(name);
        vm.voxel();
        vm.replace();
        vm.data();
    }

    @SuppressWarnings("deprecation")
	@Override
    public void perform(AsyncBlock b)
    {
        if (b.getTypeId() == ir)
        {
            h.put(b);
            b.setTypeIdAndPropertyId(i, d, true);
        }
    }

    @Override
    public boolean isUsingReplaceMaterial()
    {
        return true;
    }
}
