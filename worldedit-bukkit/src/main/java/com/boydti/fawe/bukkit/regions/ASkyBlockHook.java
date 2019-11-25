package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.regions.FaweMask;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.wasteofplastic.askyblock.ASkyBlockAPI;
import com.wasteofplastic.askyblock.Island;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class ASkyBlockHook extends BukkitMaskManager implements Listener {

    public ASkyBlockHook(final Plugin aSkyBlock) {
        super(aSkyBlock.getName());

    }

    public boolean isAllowed(Player player, Island island, MaskType type) {
        return island != null && (player.getUniqueId().equals(island.getOwner()) || (type == MaskType.MEMBER && island.getMembers().contains(player.getUniqueId()) && hasMemberPermission(player)));
    }

    @Override
    public FaweMask getMask(final com.sk89q.worldedit.entity.Player player, MaskType type) {
        final Location location = BukkitAdapter.adapt(player).getLocation();

        Island island = ASkyBlockAPI.getInstance().getIslandAt(location);
        if (island != null && isAllowed(BukkitAdapter.adapt(player), island, type)) {

            Location center1 = island.getCenter();
            MutableBlockVector3 center = MutableBlockVector3.at(center1.getX(), center1.getY(), center1.getZ());
            BlockVector3 pos1 = BlockVector3.at(island.getMinProtectedX(), 0, island.getMinProtectedZ());
            MutableBlockVector3 pos2 = center.add(center.subtract(pos1)).mutY(255);

            return new FaweMask(pos1, pos2) {
                @Override
                public boolean isValid(com.sk89q.worldedit.entity.Player player, MaskType type) {
                    return isAllowed(BukkitAdapter.adapt(player), island, type);
                }
            };
        }

        return null;
    }
}
