/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thevoxelbox.voxelsniper.brush.perform;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.util.VoxelList;

/**
 * @author Voxel
 */
public class pExcludeCombo extends vPerformer
{

    private VoxelList excludeList;
    private int id;
    private int data;

    public pExcludeCombo()
    {
        name = "Exclude Combo";
    }

    @Override
    public void info(Message vm)
    {
        vm.performerName(name);
        vm.voxelList();
        vm.voxel();
        vm.data();
    }

    @Override
    public void init(com.thevoxelbox.voxelsniper.SnipeData v)
    {
        w = v.getWorld();
        id = v.getVoxelId();
        data = v.getPropertyId();
        excludeList = v.getVoxelList();
    }

    @SuppressWarnings("deprecation")
	@Override
    public void perform(AsyncBlock b)
    {
        if (!excludeList.contains(b.getBlockData()))
        {
            h.put(b);
            b.setTypeIdAndPropertyId(id, data, true);
        }
    }
}
