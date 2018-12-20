package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.Perm;
import com.massivecraft.factions.FLocation;
import java.lang.reflect.Method;
import java.util.List;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class FactionsOneFeature extends BukkitMaskManager implements Listener {

    private final Class clazzBoard;
    private final Method methodGetFactionAt;

    public FactionsOneFeature(final Plugin factionsPlugin, final FaweBukkit p3) throws Throwable {
        super(factionsPlugin.getName());
        this.clazzBoard = Class.forName("com.massivecraft.factions.Board");
        this.methodGetFactionAt = clazzBoard.getDeclaredMethod("getFactionAt", FLocation.class);
    }

    @Override
    public BukkitMask getMask(final FawePlayer<Player> fp, MaskType type) {
        final Player player = fp.parent;
        final Chunk chunk = player.getLocation().getChunk();
        final boolean perm = Perm.hasPermission(FawePlayer.wrap(player), "fawe.factions.wilderness");
        final World world = player.getWorld();

        RegionWrapper locs = new RegionWrapper(chunk.getX(), chunk.getX(), chunk.getZ(), chunk.getZ());

        int count = 32;

        if (this.isAdded(locs, world, player, perm, type)) {
            boolean hasPerm = true;

            RegionWrapper chunkSelection;
            while (hasPerm && (count > 0)) {
                count--;

                hasPerm = false;

                chunkSelection = new RegionWrapper(locs.maxX + 1, locs.maxX + 1, locs.minZ, locs.maxZ);

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

            final Location pos1 = new Location(world, locs.minX << 4, 1, locs.minZ << 4);
            final Location pos2 = new Location(world, 15 + (locs.maxX << 4), 256, 15 + (locs.maxZ << 4));
            return new BukkitMask(pos1, pos2) {
                @Override
                public String getName() {
                    return "CHUNK:" + pos1.getChunk().getX() + "," + pos1.getChunk().getZ();
                }
            };
        }
        return null;
    }

    public boolean isAdded(final RegionWrapper locs, final World world, final Player player, final boolean perm, MaskType type) {
        try {
            for (int x = locs.minX; x <= locs.maxX; x++) {
                for (int z = locs.minZ; z <= locs.maxZ; z++) {
                    final Object fac = methodGetFactionAt.invoke(null, new FLocation(world.getName(), x, z));
                    if (fac == null) {
                        return false;
                    }
                    if (type == MaskType.OWNER) {
                        Object leader = fac.getClass().getDeclaredMethod("getFPlayerLeader").invoke(fac);
                        return player.getName().equals(leader.getClass().getDeclaredMethod("getName").invoke(leader));
                    }
                    Method methodGetOnlinePlayers = fac.getClass().getDeclaredMethod("getOnlinePlayers");
                    List<Player> players = (List<Player>) methodGetOnlinePlayers.invoke(fac);
                    if (!players.contains(player)) {
                        return false;
                    }
                    Method isNone = fac.getClass().getDeclaredMethod("isNone");
                    if ((boolean) isNone.invoke(fac)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Throwable e) {
            MainUtil.handleError(e);
            return false;
        }
    }
}
