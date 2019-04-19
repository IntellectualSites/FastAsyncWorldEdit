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
public class pComboInk extends vPerformer {

    private int d;
    private int dr;
    private int i;

    public pComboInk() {
        name = "Combo-Ink";
    }

    @Override
    public void init(com.thevoxelbox.voxelsniper.SnipeData v) {
        w = v.getWorld();
        d = v.getPropertyId();
        dr = v.getReplaceData();
        i = v.getVoxelId();
    }

    @Override
    public void info(Message vm) {
        vm.performerName(name);
        vm.voxel();
        vm.data();
        vm.replaceData();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void perform(AsyncBlock b) {
        if (b.getPropertyId() == dr) {
            h.put(b);
            b.setTypeIdAndPropertyId(i, d, true);
        }
    }

    @Override
    public boolean isUsingReplaceMaterial() {
        return true;
    }
}
