package com.boydti.fawe.jnbt.anvil.filters;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.jnbt.anvil.MCAFile;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.regions.general.RegionFilter;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

public class DeleteUnclaimedFilter extends DeleteUninhabitedFilter {
    private ArrayList<RegionFilter> filters = new ArrayList<>();
    public DeleteUnclaimedFilter(World world, long fileDuration, long inhabitedTicks, long chunkInactivity) {
        super(fileDuration, inhabitedTicks, chunkInactivity);
        for (FaweMaskManager m : FaweAPI.getMaskManagers()) {
            RegionFilter filter = m.getFilter(world.getName());
            if (filter != null) {
                filters.add(filter);
            }
        }
    }

    @Override
    public boolean shouldDelete(File file, BasicFileAttributes attr, int mcaX, int mcaZ) throws IOException {
        boolean contains = false;
        for (RegionFilter filter : filters) {
            if (contains = filter.containsRegion(mcaX, mcaZ)) {
                break;
            }
        }
        return !contains && super.shouldDelete(file, attr, mcaX, mcaZ);
    }

    @Override
    public boolean shouldDeleteChunk(MCAFile mca, int cx, int cz) {
        boolean contains = false;
        for (RegionFilter filter : filters) {
            if (contains = filter.containsChunk(cx, cz)) {
                break;
            }
        }
        return !contains && super.shouldDeleteChunk(mca, cx, cz);
    }
}
