package com.fastasyncworldedit.bukkit.regions.plotsquared;

import com.fastasyncworldedit.regions.general.CuboidRegionFilter;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
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
            Location bottom = plot.getCorners()[0];
            Location top = plot.getCorners()[1];
            add(BlockVector2.at(bottom.getX(), bottom.getZ()), BlockVector2.at(top.getX(), top.getZ()));
        }
    }
}
