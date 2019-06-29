package com.thevoxelbox.voxelsniper.brush.perform;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;


public class pInkComboNoPhys extends vPerformer {

    private int d;
    private int dr;
    private int ir;

    public pInkComboNoPhys() {
        name = "Ink-Combo, No Physics";
    }

    @Override
    public void init(SnipeData v) {
        w = v.getWorld();
        d = v.getPropertyId();
        dr = v.getReplaceData();
        ir = v.getReplaceId();
    }

    @Override
    public void info(Message vm) {
        vm.performerName(name);
        vm.replace();
        vm.data();
        vm.replaceData();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void perform(AsyncBlock b) {
        if (b.getTypeId() == ir && b.getPropertyId() == dr) {
            h.put(b);
            b.setPropertyId(d);
        }
    }

    @Override
    public boolean isUsingReplaceMaterial() {
        return true;
    }
}
