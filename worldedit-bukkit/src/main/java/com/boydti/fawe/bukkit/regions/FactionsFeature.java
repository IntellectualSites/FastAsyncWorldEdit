package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMask;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.ps.PS;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class FactionsFeature extends BukkitMaskManager implements Listener {

    public FactionsFeature(final Plugin factionsPlugin) {
        super(factionsPlugin.getName());
    }

    @Override
    public FaweMask getMask(final FawePlayer<Player> fp, MaskType type) {
        final Player player = fp.parent;
        final Location loc = player.getLocation();
        final PS ps = PS.valueOf(loc);
        final Faction fac = BoardColl.get().getFactionAt(ps);
        if (fac != null) {
            if (type == MaskType.OWNER) {
                MPlayer leader = fac.getLeader();
                if (leader != null && fp.getUUID().equals(leader.getUuid())) {
                    final Chunk chunk = loc.getChunk();
                    final Location pos1 = new Location(loc.getWorld(), chunk.getX() * 16, 0, chunk.getZ() * 16);
                    final Location pos2 = new Location(loc.getWorld(), (chunk.getX() * 16) + 15, 156, (chunk.getZ() * 16) + 15);
                    return new FaweMask(BukkitAdapter.adapt(pos1).toBlockPoint(), BukkitAdapter.adapt(pos2).toBlockPoint());
                }
            } else if (fac.getOnlinePlayers().contains(player)) {
                if (!fac.getComparisonName().equals("wilderness")) {
                    final Chunk chunk = loc.getChunk();
                    final Location pos1 = new Location(loc.getWorld(), chunk.getX() * 16, 0, chunk.getZ() * 16);
                    final Location pos2 = new Location(loc.getWorld(), (chunk.getX() * 16) + 15, 156, (chunk.getZ() * 16) + 15);
                    return new FaweMask(BukkitAdapter.adapt(pos1).toBlockPoint(), BukkitAdapter.adapt(pos2).toBlockPoint());
                }
            }
        }
        return null;
    }
}
