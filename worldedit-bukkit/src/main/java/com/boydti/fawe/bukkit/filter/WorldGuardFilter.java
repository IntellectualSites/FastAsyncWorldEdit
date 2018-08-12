package com.boydti.fawe.bukkit.filter;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.regions.general.CuboidRegionFilter;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.World;


import static com.google.common.base.Preconditions.checkNotNull;

public class WorldGuardFilter extends CuboidRegionFilter {
    private final World world;
    private boolean large;
    private RegionManager manager;

    public WorldGuardFilter(World world) {
        checkNotNull(world);
        this.world = world;
    }
    @Override
    public void calculateRegions() {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                WorldGuardFilter.this.manager = WorldGuardPlugin.inst().getRegionManager(world);
                for (ProtectedRegion region : manager.getRegions().values()) {
                    BlockVector min = region.getMinimumPoint();
                    BlockVector max = region.getMaximumPoint();
                    if (max.getBlockX() - min.getBlockX() > 1024 || max.getBlockZ() - min.getBlockZ() > 1024) {
                        Fawe.debug("Large or complex region shapes cannot be optimized. Filtering will be slower");
                        large = true;
                        break;
                    }
                    add(min.toVector2D(), max.toVector2D());
                }
            }
        });
    }

    @Override
    public boolean containsChunk(int chunkX, int chunkZ) {
        if (!large) return super.containsChunk(chunkX, chunkZ);
        BlockVector pos1 = new BlockVector(chunkX << 4, 0, chunkZ << 4);
        BlockVector pos2 = new BlockVector(pos1.getBlockX() + 15, 255, pos1.getBlockZ() + 15);
        ProtectedCuboidRegion chunkRegion = new ProtectedCuboidRegion("unimportant", pos1, pos2);
        ApplicableRegionSet set = manager.getApplicableRegions(chunkRegion);
        return set.size() > 0 && !set.getRegions().iterator().next().getId().equals("__global__");
    }

    @Override
    public boolean containsRegion(int mcaX, int mcaZ) {
        if (!large) return super.containsRegion(mcaX, mcaZ);
        BlockVector pos1 = new BlockVector(mcaX << 9, 0, mcaZ << 9);
        BlockVector pos2 = new BlockVector(pos1.getBlockX() + 511, 255, pos1.getBlockZ() + 511);
        ProtectedCuboidRegion regionRegion = new ProtectedCuboidRegion("unimportant", pos1, pos2);
        ApplicableRegionSet set = manager.getApplicableRegions(regionRegion);
        return set.size() > 0 && !set.getRegions().iterator().next().getId().equals("__global__");
    }
}