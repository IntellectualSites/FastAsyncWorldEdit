package com.thevoxelbox.voxelsniper.brush;

import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import org.bukkit.ChatColor;

/**
 * @author Monofraps
 */
@SuppressWarnings("deprecation")
public abstract class BlendBrushBase extends Brush {
    protected boolean excludeAir = true;
    protected boolean excludeWater = true;

    /**
     * @param v
     */
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

    /**
     * @return
     */
    protected final boolean isExcludeAir() {
        return excludeAir;
    }

    /**
     * @param excludeAir
     */
    protected final void setExcludeAir(boolean excludeAir) {
        this.excludeAir = excludeAir;
    }

    /**
     * @return
     */
    protected final boolean isExcludeWater() {
        return excludeWater;
    }

    /**
     * @param excludeWater
     */
    protected final void setExcludeWater(boolean excludeWater) {
        this.excludeWater = excludeWater;
    }
}
