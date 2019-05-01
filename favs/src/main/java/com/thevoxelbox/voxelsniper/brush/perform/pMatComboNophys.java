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
public class pMatComboNophys extends vPerformer {

    private int dr;
    private int i;
    private int ir;

    public pMatComboNophys() {
        name = "Mat-Combo, No Physics";
    }

    @Override
    public void init(com.thevoxelbox.voxelsniper.SnipeData v) {
        w = v.getWorld();
        dr = v.getReplaceData();
        i = v.getVoxelId();
        ir = v.getReplaceId();
    }

    @Override
    public void info(Message vm) {
        vm.performerName(name);
        vm.voxel();
        vm.replace();
        vm.replaceData();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void perform(AsyncBlock b) {
        if (b.getTypeId() == ir && b.getPropertyId() == dr) {
            h.put(b);
            b.setTypeId(i);
        }
    }

    @Override
    public boolean isUsingReplaceMaterial() {
        return true;
    }
}
