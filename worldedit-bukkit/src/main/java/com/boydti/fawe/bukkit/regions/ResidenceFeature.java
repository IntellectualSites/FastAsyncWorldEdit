package com.boydti.fawe.bukkit.regions;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.CuboidArea;
import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.regions.FaweMask;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class ResidenceFeature extends BukkitMaskManager implements Listener {
    private FaweBukkit plugin;
    private Plugin residence;

    public ResidenceFeature(final Plugin residencePlugin, final FaweBukkit p3) {
        super(residencePlugin.getName());
        this.residence = residencePlugin;
        this.plugin = p3;

    }

    public boolean isAllowed(Player player, ClaimedResidence residence, MaskType type) {
        return residence != null && (residence.getOwner().equals(player.getName()) || residence.getOwner().equals(player.getUniqueId().toString()) || type == MaskType.MEMBER && residence.getPermissions().playerHas(player, "build", false));
    }

    @Override
    public FaweMask getMask(final com.sk89q.worldedit.entity.Player wePlayer, final MaskType type) {
        final Player player = BukkitAdapter.adapt(wePlayer);
        final Location location = player.getLocation();
        ClaimedResidence residence = Residence.getInstance().getResidenceManager().getByLoc(location);
        if (residence != null) {
            boolean isAllowed;
            while (!(isAllowed = isAllowed(player, residence, type)) && residence != null) {
                residence = residence.getSubzoneByLoc(location);
            }
            if (isAllowed) {
                final CuboidArea area = residence.getAreaArray()[0];
                final Location pos1 = area.getLowLoc();
                final Location pos2 = area.getHighLoc();
                final ClaimedResidence finalResidence = residence;
                return new FaweMask(BukkitAdapter.asBlockVector(pos1), BukkitAdapter.asBlockVector(pos2)) {
                @Override
                    public boolean isValid(com.sk89q.worldedit.entity.Player player, MaskType type) {
                        return isAllowed(BukkitAdapter.adapt(player), finalResidence, type);
                    }
                };
            }
        }
        return null;
    }
}
