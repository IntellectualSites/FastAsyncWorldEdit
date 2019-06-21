/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thevoxelbox.voxelsniper.brush.perform;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;


public class pComboMatNoPhys extends vPerformer {

    private int d;
    private int i;
    private int ir;

    public pComboMatNoPhys() {
        name = "Combo-Mat, No Physics";
    }

    @Override
    public void init(SnipeData v) {
        w = v.getWorld();
        d = v.getPropertyId();
        i = v.getVoxelId();
        ir = v.getReplaceId();
    }

    @Override
    public void info(Message vm) {
        vm.performerName(name);
        vm.voxel();
        vm.replace();
        vm.data();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void perform(AsyncBlock b) {
        if (b.getTypeId() == ir) {
            h.put(b);
            b.setTypeIdAndPropertyId(i, d, false);
        }
    }

    @Override
    public boolean isUsingReplaceMaterial() {
        return true;
    }
}
