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
public class pIncludeInk extends vPerformer {

    private VoxelList includeList;
    private int data;

    public pIncludeInk() {
        name = "Include Ink";
    }

    @Override
    public void info(Message vm) {
        vm.performerName(name);
        vm.voxelList();
        vm.data();
    }

    @Override
    public void init(com.thevoxelbox.voxelsniper.SnipeData v) {
        w = v.getWorld();
        data = v.getPropertyId();
        includeList = v.getVoxelList();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void perform(AsyncBlock b) {
        if (includeList.contains(b.getBlockData())) {
            h.put(b);
            b.setPropertyId(data);
        }
    }
}
