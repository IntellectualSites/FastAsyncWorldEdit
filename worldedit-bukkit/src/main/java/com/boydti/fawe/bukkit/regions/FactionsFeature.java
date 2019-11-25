package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.regions.FaweMask;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.ps.PS;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
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
    public FaweMask getMask(final com.sk89q.worldedit.entity.Player p, MaskType type) {
        final Player player = BukkitAdapter.adapt(p);
        final Location loc = player.getLocation();
        final PS ps = PS.valueOf(loc);
        final Faction fac = BoardColl.get().getFactionAt(ps);
        if (fac != null) {
            if (type == MaskType.OWNER) {
                MPlayer leader = fac.getLeader();
                if (leader != null && p.getUniqueId().equals(leader.getUuid())) {
                    final Chunk chunk = loc.getChunk();
                    final BlockVector3 pos1 = BlockVector3.at(chunk.getX() * 16, 0, chunk.getZ() * 16);
                    final BlockVector3 pos2 = BlockVector3
                        .at((chunk.getX() * 16) + 15, 156, (chunk.getZ() * 16) + 15);
                    return new FaweMask(pos1, pos2);
                }
            } else if (fac.getOnlinePlayers().contains(player)) {
                if (!fac.getComparisonName().equals("wilderness")) {
                    final Chunk chunk = loc.getChunk();
                    final BlockVector3 pos1 = BlockVector3.at(chunk.getX() * 16, 0, chunk.getZ() * 16);
                    final BlockVector3 pos2 = BlockVector3.at((chunk.getX() * 16) + 15, 156, (chunk.getZ() * 16) + 15);
                    return new FaweMask(pos1, pos2);
                }
            }
        }
        return null;
    }
}
