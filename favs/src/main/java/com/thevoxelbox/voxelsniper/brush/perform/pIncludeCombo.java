/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thevoxelbox.voxelsniper.brush.perform;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.util.VoxelList;


public class pIncludeCombo extends vPerformer {

    private VoxelList includeList;
    private int id;
    private int data;

    public pIncludeCombo() {
        name = "Include Combo";
    }

    @Override
    public void info(Message vm) {
        vm.performerName(name);
        vm.voxelList();
        vm.voxel();
        vm.data();
    }

    @Override
    public void init(SnipeData v) {
        w = v.getWorld();
        id = v.getVoxelId();
        data = v.getPropertyId();
        includeList = v.getVoxelList();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void perform(AsyncBlock b) {
        if (includeList.contains(b.getBlockData())) {
            h.put(b);
            b.setTypeIdAndPropertyId(id, data, true);
        }
    }
}
