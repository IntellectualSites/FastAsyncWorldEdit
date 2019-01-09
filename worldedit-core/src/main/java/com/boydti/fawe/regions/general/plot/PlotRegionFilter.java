package com.boydti.fawe.regions.general.plot;

import com.boydti.fawe.regions.general.CuboidRegionFilter;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.sk89q.worldedit.math.BlockVector2;

import java.util.ArrayList;


import static com.google.common.base.Preconditions.checkNotNull;

public class PlotRegionFilter extends CuboidRegionFilter {
    private final PlotArea area;

    public PlotRegionFilter(PlotArea area) {
        checkNotNull(area);
        this.area = area;
    }
    @Override
    public void calculateRegions() {
        ArrayList<Plot> plots = new ArrayList<>(area.getPlots());
        for (Plot plot : plots) {
            Location pos1 = plot.getBottom();
            Location pos2 = plot.getTop();
            add(new BlockVector2(pos1.getX(), pos1.getZ()), new BlockVector2(pos2.getX(), pos2.getZ()));
        }
    }
}
