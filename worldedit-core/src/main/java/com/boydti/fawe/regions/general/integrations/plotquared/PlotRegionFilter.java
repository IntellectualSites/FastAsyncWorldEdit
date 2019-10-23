package com.boydti.fawe.regions.general.integrations.plotquared;

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.regions.general.CuboidRegionFilter;
import com.github.intellectualsites.plotsquared.plot.object.Location;
import com.github.intellectualsites.plotsquared.plot.object.Plot;
import com.github.intellectualsites.plotsquared.plot.object.PlotArea;
import com.sk89q.worldedit.math.BlockVector2;
import java.util.ArrayList;

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
