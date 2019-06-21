/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thevoxelbox.voxelsniper.brush.perform;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;


public class pMaterialNoPhys extends vPerformer {
    private int i;

    public pMaterialNoPhys() {
        name = "Set, No-Physics";
    }

    @Override
    public void init(SnipeData v) {
        w = v.getWorld();
        i = v.getVoxelId();
    }

    @Override
    public void info(Message vm) {
        vm.performerName(name);
        vm.voxel();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void perform(AsyncBlock b) {
        if (b.getTypeId() != i) {
            h.put(b);
            b.setTypeId(i);
        }
    }
}
