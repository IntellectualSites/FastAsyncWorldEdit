package com.fastasyncworldedit.bukkit.regions;

import com.fastasyncworldedit.core.regions.FaweMask;
import com.flowpowered.math.vector.Vector3i;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.TrustTypes;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.apache.logging.log4j.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class GriefDefenderFeature extends BukkitMaskManager implements Listener {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    public GriefDefenderFeature(final Plugin GriefDefenderPlugin) {
        super(GriefDefenderPlugin.getName());
        LOGGER.info("Plugin 'GriefDefender' found. Using it now.");
    }

    public boolean isAllowed(Player player, Claim claim, MaskType type) {
        return GriefDefender.getCore().isEnabled(player.getWorld().getUID()) && !claim.isWilderness() && (claim
                .getOwnerName()
                .equalsIgnoreCase(player.getName()) || claim.getOwnerUniqueId().equals(player.getUniqueId()) ||
                type == MaskType.MEMBER && claim.getUserTrusts(TrustTypes.BUILDER).contains(player.getUniqueId()));
    }

    @Override
    public FaweMask getMask(final com.sk89q.worldedit.entity.Player wePlayer, MaskType type) {
        final Player player = BukkitAdapter.adapt(wePlayer);
        final Location loc = player.getLocation();
        final Vector3i vector = Vector3i.from(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        final Claim claim = GriefDefender.getCore().getClaimManager(loc.getWorld().getUID()).getClaimAt(vector);
        if (!claim.isWilderness()) {
            if (isAllowed(player, claim, type)) {
                claim.getGreaterBoundaryCorner().getX();
                final BlockVector3 pos1 = BlockVector3.at(
                        claim.getLesserBoundaryCorner().getX(),
                        claim.getLesserBoundaryCorner().getY(),
                        claim.getLesserBoundaryCorner().getZ()
                );
                final BlockVector3 pos2 = BlockVector3.at(
                        claim.getGreaterBoundaryCorner().getX(),
                        claim.getGreaterBoundaryCorner().getY(),
                        claim.getGreaterBoundaryCorner().getZ()
                );
                return new FaweMask(new CuboidRegion(pos1, pos2)) {

                    @Override
                    public boolean isValid(com.sk89q.worldedit.entity.Player wePlayer, MaskType type) {
                        return isAllowed(player, claim, type);
                    }
                };
            }
        }
        return null;
    }

}
