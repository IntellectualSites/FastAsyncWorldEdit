package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.bukkit.filter.GriefPreventionFilter;
import com.boydti.fawe.regions.FaweMask;
import com.boydti.fawe.regions.general.RegionFilter;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class GriefPreventionFeature extends BukkitMaskManager implements Listener {

    public GriefPreventionFeature(final Plugin griefpreventionPlugin) {
        super(griefpreventionPlugin.getName());
    }

    public boolean isAllowed(Player player, Claim claim, MaskType type) {
        return claim != null && (claim.getOwnerName().equalsIgnoreCase(player.getName()) || claim.getOwnerName().equals(player.getUniqueId()) ||
            type == MaskType.MEMBER && claim.allowBuild(player, Material.AIR) == null);
    }

    @Override
    public FaweMask getMask(final com.sk89q.worldedit.entity.Player fp, MaskType type) {
        final Player player = BukkitAdapter.adapt(fp);
        final Claim claim = GriefPrevention.instance.dataStore.getClaimAt(BukkitAdapter.adapt(fp.getLocation()), true, null);
        if (claim != null) {
            if (isAllowed(player, claim, type)) {
                claim.getGreaterBoundaryCorner().getBlockX();
                final BlockVector3 pos1 = BlockVector3.at(claim.getLesserBoundaryCorner().getBlockX(), 0, claim.getLesserBoundaryCorner().getBlockZ());
                final BlockVector3 pos2 = BlockVector3.at(claim.getGreaterBoundaryCorner().getBlockX(), 256, claim.getGreaterBoundaryCorner().getBlockZ());
                return new FaweMask(pos1, pos2) {

                    @Override
                    public boolean isValid(com.sk89q.worldedit.entity.Player fp, MaskType type) {
                        return isAllowed(player, claim, type);
                    }
                };
            }
        }
        return null;
    }

    @Override
    public RegionFilter getFilter(String world) {
        return new GriefPreventionFilter(Bukkit.getWorld(world));
    }
}
