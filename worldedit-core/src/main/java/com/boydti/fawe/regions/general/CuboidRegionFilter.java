package com.boydti.fawe.regions.general;

import com.boydti.fawe.object.collection.LongHashSet;
import com.sk89q.worldedit.math.BlockVector2;

public abstract class CuboidRegionFilter implements RegionFilter {

    private final LongHashSet occupiedRegions;
    private final LongHashSet unoccupiedChunks;

    public CuboidRegionFilter() {
        this.occupiedRegions = new LongHashSet();
        this.unoccupiedChunks = new LongHashSet();
    }

    /**
     * Loop over all regions and call add(...) with the corners
     */
    public abstract void calculateRegions();

    public void add(BlockVector2 pos1, BlockVector2 pos2) {
        int ccx1 = pos1.getBlockX() >> 9;
        int ccz1 = pos1.getBlockZ() >> 9;
        int ccx2 = pos2.getBlockX() >> 9;
        int ccz2 = pos2.getBlockZ() >> 9;
        for (int x = ccx1; x <= ccx2; x++) {
            for (int z = ccz1; z <= ccz2; z++) {
                if (!occupiedRegions.containsKey(x, z)) {
                    occupiedRegions.add(x, z);
                    int bcx = x << 5;
                    int bcz = z << 5;
                    int tcx = bcx + 32;
                    int tcz = bcz + 32;
                    for (int cz = bcz; cz < tcz; cz++) {
                        for (int cx = bcx; cx < tcx; cx++) {
                            unoccupiedChunks.add(cx, cz);
                        }
                    }
                }
            }
        }
        int cx1 = pos1.getBlockX() >> 4;
        int cz1 = pos1.getBlockZ() >> 4;
        int cx2 = pos2.getBlockX() >> 4;
        int cz2 = pos2.getBlockZ() >> 4;
        for (int cz = cz1; cz <= cz2; cz++) {
            for (int cx = cx1; cx <= cx2; cx++) {
                unoccupiedChunks.remove(cx, cz);
            }
        }
    }

    public void clear() {
        occupiedRegions.popAll();
        unoccupiedChunks.popAll();
    }

    @Override
    public boolean containsRegion(int mcaX, int mcaZ) {
        return occupiedRegions.containsKey(mcaX, mcaZ);
    }

    @Override
    public boolean containsChunk(int chunkX, int chunkZ) {
        int mcaX = chunkX >> 5;
        int mcaZ = chunkZ >> 5;
        return occupiedRegions.containsKey(mcaX, mcaZ) && !unoccupiedChunks.containsKey(chunkX, chunkZ);
    }
}
