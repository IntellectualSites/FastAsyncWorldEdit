package com.fastasyncworldedit.bukkit.regions;

import com.fastasyncworldedit.core.regions.FaweMask;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.apache.logging.log4j.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class TownyFeature extends BukkitMaskManager implements Listener {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    public TownyFeature(Plugin townyPlugin) {
        super(townyPlugin.getName());
        LOGGER.info("Plugin 'Towny' found. Using it now.");
    }

    public boolean isAllowed(Player player, TownBlock block) {
        if (block == null) {
            return false;
        }
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) {
            return false;
        }
        if (block.hasResident(resident) || block.hasTrustedResident(resident)) {
            return true;
        }
        Town town = block.getTownOrNull(); // Will not be null, because block is not null.
        if (town.isMayor(resident) || town.hasTrustedResident(resident)) {
            return true;
        }
        if (!town.hasResident(resident)) {
            return false;
        }
        if (player.hasPermission("fawe.towny.*")) {
            return true;
        }
        for (String rank : resident.getTownRanks()) {
            if (player.hasPermission("fawe.towny." + rank)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public FaweMask getMask(final com.sk89q.worldedit.entity.Player wePlayer, MaskType type, boolean isWhitelist) {
        final Player player = BukkitAdapter.adapt(wePlayer);
        final Location location = player.getLocation();
        final WorldCoord mycoord = WorldCoord.parseWorldCoord(location);
        if (mycoord.isWilderness()) {
            return null;
        }
        final TownBlock myplot = mycoord.getTownBlockOrNull(); // Will not be null, because of the isWilderness() test above.
        boolean isMember = isAllowed(player, myplot);
        if (isMember) {
            final Location loc1 = mycoord.getLowerMostCornerLocation();
            final Location loc2 = mycoord.getUpperMostCornerLocation();
            final BlockVector3 pos1 = BlockVector3.at(loc1.getX(), loc1.getY(), loc1.getZ());
            final BlockVector3 pos2 = BlockVector3.at(loc2.getX() - 1, loc2.getY(), loc2.getZ() - 1);
            return new FaweMask(new CuboidRegion(pos1, pos2)) {
                @Override
                public boolean isValid(com.sk89q.worldedit.entity.Player player, MaskType type) {
                    return isAllowed(BukkitAdapter.adapt(player), myplot);
                }
            };
        }
        return null;
    }

}
