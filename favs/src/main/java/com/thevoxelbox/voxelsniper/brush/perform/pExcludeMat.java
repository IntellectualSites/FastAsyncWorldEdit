/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thevoxelbox.voxelsniper.brush.perform;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.util.VoxelList;


public class pExcludeMat extends vPerformer {

    private VoxelList excludeList;
    private int id;

    public pExcludeMat() {
        name = "Exclude Material";
    }

    @Override
    public void info(Message vm) {
        vm.performerName(name);
        vm.voxelList();
        vm.voxel();
    }

    @Override
    public void init(SnipeData v) {
        w = v.getWorld();
        id = v.getVoxelId();
        excludeList = v.getVoxelList();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void perform(AsyncBlock b) {
        if (!excludeList.contains(b.getBlockData())) {
            h.put(b);
            b.setTypeId(id);
        }
    }
}
