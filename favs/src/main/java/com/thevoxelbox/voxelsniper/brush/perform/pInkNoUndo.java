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
public class pInkNoUndo extends vPerformer {

    private int d;

    public pInkNoUndo() {
        name = "Ink, No-Undo"; // made name more descriptive - Giltwist
    }

    @Override
    public void init(com.thevoxelbox.voxelsniper.SnipeData v) {
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
        if (b.getPropertyId() != d) {
            b.setPropertyId(d);
        }
    }
}