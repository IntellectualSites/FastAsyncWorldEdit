package com.fastasyncworldedit.bukkit.regions;

import com.fastasyncworldedit.core.regions.FaweMask;
import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.apache.logging.log4j.Logger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class GriefPreventionFeature extends BukkitMaskManager implements Listener {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    public GriefPreventionFeature(final Plugin griefpreventionPlugin) {
        super(griefpreventionPlugin.getName());
        LOGGER.info("Plugin 'GriefPrevention' found. Using it now.");
    }

    public boolean isAllowed(Player player, Claim claim, MaskType type) {
        return claim != null && (claim.getOwnerName().equalsIgnoreCase(player.getName()) || claim
                .getOwnerName()
                .equals(player.getUniqueId()) || TaskManager.taskManager().syncAt(
                        () -> type == MaskType.MEMBER && claim.allowBuild(player, Material.AIR) == null,
                BukkitAdapter.adapt(claim.getLesserBoundaryCorner())
        ));
    }

    @Override
    public FaweMask getMask(final com.sk89q.worldedit.entity.Player wePlayer, MaskType type, boolean isWhitelist) {
        final Player player = BukkitAdapter.adapt(wePlayer);
        final Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);
        if (claim != null) {
            if (isAllowed(player, claim, type)) {
                claim.getGreaterBoundaryCorner().getBlockX();
                final BlockVector3 pos1 = BlockVector3.at(
                        claim.getLesserBoundaryCorner().getBlockX(),
                        wePlayer.getWorld().getMinY(),
                        claim.getLesserBoundaryCorner().getBlockZ()
                );
                final BlockVector3 pos2 = BlockVector3.at(
                        claim.getGreaterBoundaryCorner().getBlockX(),
                        wePlayer.getWorld().getMaxY(),
                        claim.getGreaterBoundaryCorner().getBlockZ()
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
