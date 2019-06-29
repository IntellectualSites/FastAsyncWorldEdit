/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thevoxelbox.voxelsniper.brush.perform;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;

public class pCombo extends vPerformer {

    private int i;
    private int d;

    public pCombo() {
        name = "Combo";
    }

    @Override
    public void info(Message vm) {
        vm.performerName(name);
        vm.voxel();
        vm.data();
    }

    @Override
    public void init(SnipeData v) {
        w = v.getWorld();
        i = v.getVoxelId();
        d = v.getPropertyId();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void perform(AsyncBlock b) {
        h.put(b);
        b.setTypeIdAndPropertyId(i, d, true);
    }
}
