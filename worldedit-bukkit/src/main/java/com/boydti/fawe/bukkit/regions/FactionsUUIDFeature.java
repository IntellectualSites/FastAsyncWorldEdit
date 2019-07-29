package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.regions.FaweMask;
import com.boydti.fawe.util.Permission;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class FactionsUUIDFeature extends BukkitMaskManager implements Listener {
    private final Board instance;

    public FactionsUUIDFeature(final Plugin factionsPlugin, final FaweBukkit p3) {
        super(factionsPlugin.getName());
        this.instance = Board.getInstance();
    }

    @Override
    public FaweMask getMask(final FawePlayer<Player> fp, MaskType type) {
        final Player player = BukkitAdapter.adapt(fp.toWorldEditPlayer());
        final Chunk chunk = player.getLocation().getChunk();
        final boolean perm = Permission
            .hasPermission(fp.toWorldEditPlayer(), "fawe.factions.wilderness");
        final World world = player.getWorld();

        RegionWrapper locs = new RegionWrapper(chunk.getX(), chunk.getX(), chunk.getZ(), chunk.getZ());

        int count = 32;

        if (this.isAdded(locs, world, player, perm, type)) {
            boolean hasPerm = true;

            while (hasPerm && count > 0) {
                count--;

                hasPerm = false;

                RegionWrapper chunkSelection = new RegionWrapper(locs.maxX + 1, locs.maxX + 1, locs.minZ, locs.maxZ);

                if (this.isAdded(chunkSelection, world, player, perm, type)) {
                    locs = new RegionWrapper(locs.minX, locs.maxX + 1, locs.minZ, locs.maxZ);
                    hasPerm = true;
                }

                chunkSelection = new RegionWrapper(locs.minX - 1, locs.minX - 1, locs.minZ, locs.maxZ);

                if (this.isAdded(chunkSelection, world, player, perm, type)) {
                    locs = new RegionWrapper(locs.minX - 1, locs.maxX, locs.minZ, locs.maxZ);
                    hasPerm = true;
                }

                chunkSelection = new RegionWrapper(locs.minX, locs.maxX, locs.maxZ + 1, locs.maxZ + 1);

                if (this.isAdded(chunkSelection, world, player, perm, type)) {
                    locs = new RegionWrapper(locs.minX, locs.maxX, locs.minZ, locs.maxZ + 1);
                    hasPerm = true;
                }

                chunkSelection = new RegionWrapper(locs.minX, locs.maxX, locs.minZ - 1, locs.minZ - 1);

                if (this.isAdded(chunkSelection, world, player, perm, type)) {
                    locs = new RegionWrapper(locs.minX, locs.maxX, locs.minZ - 1, locs.maxZ);
                    hasPerm = true;
                }
            }

            final BlockVector3 pos1 = BlockVector3.at(locs.minX << 4, 1, locs.minZ << 4);
            final BlockVector3 pos2 = BlockVector3.at(15 + (locs.maxX << 4), 256, 15 + (locs.maxZ << 4));
            return new FaweMask(pos1,pos2);
        }
        return null;
    }

    public boolean isAdded(final RegionWrapper locs, final World world, final Player player, final boolean perm, MaskType type) {
        for (int x = locs.minX; x <= locs.maxX; x++) {
            for (int z = locs.minZ; z <= locs.maxZ; z++) {
                final Faction fac = this.instance.getFactionAt(new FLocation(world.getName(), x, z));
                if (fac == null) {
                    return false;
                }
                // TODO types
                if (!fac.getOnlinePlayers().contains(player)) {
                    return false;
                }
                if (fac.isWilderness() && !perm) {
                    return false;
                }
            }
        }
        return true;
    }
}
