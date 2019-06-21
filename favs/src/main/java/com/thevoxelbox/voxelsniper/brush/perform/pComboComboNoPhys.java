/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thevoxelbox.voxelsniper.brush.perform;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;


public class pComboComboNoPhys extends vPerformer {

    private int d;
    private int dr;
    private int i;
    private int ir;

    public pComboComboNoPhys() {
        name = "Combo-Combo No-Physics";
    }

    @Override
    public void init(SnipeData v) {
        w = v.getWorld();
        d = v.getPropertyId();
        dr = v.getReplaceData();
        i = v.getVoxelId();
        ir = v.getReplaceId();
    }

    @Override
    public void info(Message vm) {
        vm.performerName(name);
        vm.voxel();
        vm.replace();
        vm.data();
        vm.replaceData();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void perform(AsyncBlock b) {
        if (b.getTypeId() == ir && b.getPropertyId() == dr) {
            h.put(b);
            b.setTypeId(i);
            b.setPropertyId(d);
        }
    }

    @Override
    public boolean isUsingReplaceMaterial() {
        return true;
    }
}
