package com.fastasyncworldedit.bukkit.filter;

import com.fastasyncworldedit.core.regions.general.CuboidRegionFilter;
import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.math.BlockVector2;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.GriefDefender;
import com.flowpowered.math.vector.Vector3i;
import org.bukkit.World;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

public class GriefDefenderFilter extends CuboidRegionFilter {
    private final Collection<Claim> claims;
    private final World world;

    public GriefDefenderFilter(World world) {
        checkNotNull(world);
        this.claims = TaskManager.IMP.sync(
                (Supplier<Collection<Claim>>) () -> new ArrayDeque<>(GriefDefender.getCore().getAllClaims()));
        this.world = world;
    }

    @Override
    public void calculateRegions() {
        for (Claim claim : claims) {
            Vector3i bot = claim.getGreaterBoundaryCorner();
            if (world.getUID().equals(claim.getWorldUniqueId())) {
                Vector3i top = claim.getGreaterBoundaryCorner();
                BlockVector2 pos1 = BlockVector2.at(bot.getX(), bot.getZ());
                BlockVector2 pos2 = BlockVector2.at(top.getX(), top.getZ());
                add(pos1, pos2);
            }
        }
    }
}
