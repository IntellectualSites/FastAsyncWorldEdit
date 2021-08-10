package com.fastasyncworldedit.bukkit.regions;

import com.fastasyncworldedit.core.regions.FaweMask;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.apache.logging.log4j.Logger;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class TownyFeature extends BukkitMaskManager implements Listener {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private final Plugin towny;

    public TownyFeature(Plugin townyPlugin) {
        super(townyPlugin.getName());
        this.towny = townyPlugin;
        LOGGER.info("Plugin 'Towny' found. Using it now.");
    }

    public boolean isAllowed(Player player, TownBlock block) {
        if (block == null) {
            return false;
        }
        Resident resident;
        try {
            resident = TownyUniverse.getInstance().getResident(player.getName());
            try {
                if (block.getResident().equals(resident)) {
                    return true;
                }
            } catch (NotRegisteredException ignored) {
            }
            Town town = block.getTown();
            if (town.isMayor(resident)) {
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
        } catch (NotRegisteredException ignored) {
        }
        return false;
    }

    @Override
    public FaweMask getMask(final com.sk89q.worldedit.entity.Player wePlayer, MaskType type) {
        final Player player = BukkitAdapter.adapt(wePlayer);
        final Location location = player.getLocation();
        try {
            final PlayerCache cache = ((Towny) this.towny).getCache(player);
            final WorldCoord mycoord = cache.getLastTownBlock();
            if (mycoord == null) {
                return null;
            }
            final TownBlock myplot = mycoord.getTownBlock();
            if (myplot == null) {
                return null;
            }
            boolean isMember = isAllowed(player, myplot);
            if (isMember) {
                final Chunk chunk = location.getChunk();
                final BlockVector3 pos1 = BlockVector3
                        .at(chunk.getX() * 16, 0, chunk.getZ() * 16);
                final BlockVector3 pos2 = BlockVector3.at(
                        chunk.getX() * 16 + 15, 156, chunk.getZ() * 16
                                + 15);
                return new FaweMask(new CuboidRegion(pos1, pos2)) {
                    @Override
                    public boolean isValid(com.sk89q.worldedit.entity.Player player, MaskType type) {
                        return isAllowed(BukkitAdapter.adapt(player), myplot);
                    }
                };
            }
        } catch (Exception ignored) {
        }
        return null;
    }

}
