package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import org.bukkit.ChatColor;

public abstract class BlendBrushBase extends Brush {
    protected boolean excludeAir = true;
    protected boolean excludeWater = true;

    protected abstract void blend(final SnipeData v);

    @Override
    protected final void arrow(final SnipeData v) {
        this.excludeAir = false;
        this.blend(v);
    }

    @Override
    protected final void powder(final SnipeData v) {
        this.excludeAir = true;
        this.blend(v);
    }

    @Override
    public final void info(final Message vm) {
        vm.brushName(this.getName());
        vm.size();
        vm.voxel();
        vm.custom(ChatColor.BLUE + "Water Mode: " + (this.excludeWater ? "exclude" : "include"));
    }

    @Override
    public void parameters(final String[] par, final SnipeData v) {
        for (int i = 1; i < par.length; ++i) {
            if (par[i].equalsIgnoreCase("water")) {
                this.excludeWater = !this.excludeWater;
                v.sendMessage(ChatColor.AQUA + "Water Mode: " + (this.excludeWater ? "exclude" : "include"));
            }
        }
    }

    protected final boolean isExcludeAir() {
        return excludeAir;
    }

    protected final void setExcludeAir(boolean excludeAir) {
        this.excludeAir = excludeAir;
    }

    protected final boolean isExcludeWater() {
        return excludeWater;
    }

    protected final void setExcludeWater(boolean excludeWater) {
        this.excludeWater = excludeWater;
    }
}
