package com.boydti.fawe.bukkit.filter;

import com.boydti.fawe.regions.general.CuboidRegionFilter;
import com.intellectualcrafters.plot.object.RunnableVal;
import com.intellectualcrafters.plot.util.TaskManager;
import com.sk89q.worldedit.math.BlockVector2;

import java.util.ArrayDeque;
import java.util.Collection;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.World;


import static com.google.common.base.Preconditions.checkNotNull;

public class GriefPreventionFilter extends CuboidRegionFilter {
    private final Collection<Claim> claims;
    private final World world;

    public GriefPreventionFilter(World world) {
        checkNotNull(world);
        this.claims = TaskManager.IMP.sync(new RunnableVal<Collection<Claim>>() {
            @Override
            public void run(Collection<Claim> claims) {
                this.value = new ArrayDeque(GriefPrevention.instance.dataStore.getClaims());
            }
        });
        this.world = world;
    }

    @Override
    public void calculateRegions() {
        for (Claim claim : claims) {
            org.bukkit.Location bot = claim.getGreaterBoundaryCorner();
            if (world.equals(bot.getWorld())) {
                org.bukkit.Location top = claim.getGreaterBoundaryCorner();
                BlockVector2 pos1 = new BlockVector2(bot.getBlockX(), bot.getBlockZ());
                BlockVector2 pos2 = new BlockVector2(top.getBlockX(), top.getBlockZ());
                add(pos1, pos2);
            }
        }
    }
}
