package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.object.FawePlayer;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.*;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class TownyFeature extends BukkitMaskManager implements Listener {
    FaweBukkit plugin;
    Plugin towny;

    public TownyFeature(final Plugin townyPlugin, final FaweBukkit p3) {
        super(townyPlugin.getName());
        this.towny = townyPlugin;
        this.plugin = p3;
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
        } catch (NotRegisteredException ignore) {}
            Town town = block.getTown();
            if (town.isMayor(resident)) {
                return true;
            }
            if (!town.hasResident(resident)) return false;
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
    public BukkitMask getMask(final FawePlayer<Player> fp) {
        final Player player = fp.parent;
        final Location location = player.getLocation();
        try {
            final PlayerCache cache = ((Towny) this.towny).getCache(player);
            final WorldCoord mycoord = cache.getLastTownBlock();
            if (mycoord == null) {
                return null;
            } else {
                final TownBlock myplot = mycoord.getTownBlock();
                if (myplot == null) {
                    return null;
                } else {
                    boolean isMember = isAllowed(player, myplot);
                    if (isMember) {
                        final Chunk chunk = location.getChunk();
                        final Location pos1 = new Location(location.getWorld(), chunk.getX() * 16, 0, chunk.getZ() * 16);
                        final Location pos2 = new Location(location.getWorld(), (chunk.getX() * 16) + 15, 156, (chunk.getZ() * 16) + 15);
                        return new BukkitMask(pos1, pos2) {
                            @Override
                            public String getName() {
                                return "PLOT:" + location.getChunk().getX() + "," + location.getChunk().getZ();
                            }

                            @Override
                            public boolean isValid(FawePlayer player, MaskType type) {
                                return isAllowed((Player) player.parent, myplot);
                            }
                        };
                    }
                }
            }
        } catch (final Exception e) {}
        return null;
    }
}
