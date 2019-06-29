/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thevoxelbox.voxelsniper.brush.perform;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;


public class pMatInk extends vPerformer {

    private int i;
    private int dr;

    public pMatInk() {
        name = "Mat-Ink";
    }

    @Override
    public void init(SnipeData v) {
        w = v.getWorld();
        i = v.getVoxelId();
        dr = v.getReplaceData();
    }

    @Override
    public void info(Message vm) {
        vm.performerName(name);
        vm.voxel();
        vm.replaceData();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void perform(AsyncBlock b) {
        if (b.getPropertyId() == dr) {
            h.put(b);
            b.setTypeId(i);
        }
    }

    @Override
    public boolean isUsingReplaceMaterial() {
        return true;
    }
}
