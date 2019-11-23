package com.boydti.fawe.bukkit.filter;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.regions.general.CuboidRegionFilter;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.World;

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
        Fawe.get().getQueueHandler().sync(() -> {
            WorldGuardFilter.this.manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(FaweAPI.getWorld(world.getName()));
            for (ProtectedRegion region : manager.getRegions().values()) {
                BlockVector3 min = region.getMinimumPoint();
                BlockVector3 max = region.getMaximumPoint();
                if (max.getBlockX() - min.getBlockX() > 1024 || max.getBlockZ() - min.getBlockZ() > 1024) {
                    getLogger(WorldGuardFilter.class).debug("Large or complex region shapes cannot be optimized. Filtering will be slower");
                    large = true;
                    break;
                }
                add(min.toBlockVector2(), max.toBlockVector2());
            }
        });
    }

    @Override
    public boolean containsChunk(int chunkX, int chunkZ) {
        if (!large) return super.containsChunk(chunkX, chunkZ);
        BlockVector3 pos1 = BlockVector3.at(chunkX << 4, 0, chunkZ << 4);
        BlockVector3 pos2 = BlockVector3.at(pos1.getBlockX() + 15, 255, pos1.getBlockZ() + 15);
        ProtectedCuboidRegion chunkRegion = new ProtectedCuboidRegion("unimportant", pos1, pos2);
        ApplicableRegionSet set = manager.getApplicableRegions(chunkRegion);
        return set.size() > 0 && !set.getRegions().iterator().next().getId().equals("__global__");
    }

    @Override
    public boolean containsRegion(int mcaX, int mcaZ) {
        if (!large) return super.containsRegion(mcaX, mcaZ);
        BlockVector3 pos1 = BlockVector3.at(mcaX << 9, 0, mcaZ << 9);
        BlockVector3 pos2 = BlockVector3.at(pos1.getBlockX() + 511, 255, pos1.getBlockZ() + 511);
        ProtectedCuboidRegion regionRegion = new ProtectedCuboidRegion("unimportant", pos1, pos2);
        ApplicableRegionSet set = manager.getApplicableRegions(regionRegion);
        return set.size() > 0 && !set.getRegions().iterator().next().getId().equals("__global__");
    }
}
