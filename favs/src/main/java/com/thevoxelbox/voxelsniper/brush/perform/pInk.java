/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thevoxelbox.voxelsniper.brush.perform;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;


public class pInk extends vPerformer {

    private int d;

    public pInk() {
        name = "Ink";
    }

    @Override
    public void init(SnipeData v) {
        w = v.getWorld();
        d = v.getPropertyId();
    }

    @Override
    public void info(Message vm) {
        vm.performerName(name);
        vm.data();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void perform(AsyncBlock b) {
        h.put(b);
        b.setPropertyId(d);
    }
}
