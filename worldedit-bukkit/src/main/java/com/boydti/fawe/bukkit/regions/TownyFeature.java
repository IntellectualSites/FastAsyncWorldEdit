package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.regions.FaweMask;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class TownyFeature extends BukkitMaskManager implements Listener {

    private final Plugin towny;

    public TownyFeature(Plugin townyPlugin) {
        super(townyPlugin.getName());
        this.towny = townyPlugin;
    }

    public boolean isAllowed(Player player, TownBlock block) {
        if (block == null) {
            return false;
        }
        Resident resident;
        try {
            resident = TownyUniverse.getDataSource().getResident(player.getName());
            try {
                if (block.getResident().equals(resident)) {
                    return true;
                }
            } catch (NotRegisteredException ignore) {
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
        } catch (NotRegisteredException e) {
            return false;
        }
        return false;
    }

    @Override
    public FaweMask getMask(com.sk89q.worldedit.entity.Player wePlayer) {
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
                return new FaweMask(pos1, pos2) {
                    @Override
                    public boolean isValid(com.sk89q.worldedit.entity.Player player, MaskType type) {
                        return isAllowed(BukkitAdapter.adapt(player),myplot);
                    }
                };
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
