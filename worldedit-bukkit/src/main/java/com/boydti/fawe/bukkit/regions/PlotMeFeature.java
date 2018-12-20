package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.object.FawePlayer;
import com.worldcretornica.plotme_core.Plot;
import com.worldcretornica.plotme_core.PlotMe_Core;
import com.worldcretornica.plotme_core.bukkit.PlotMe_CorePlugin;
import com.worldcretornica.plotme_core.bukkit.api.BukkitPlayer;
import com.worldcretornica.plotme_core.bukkit.api.BukkitWorld;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class PlotMeFeature extends BukkitMaskManager implements Listener {
    FaweBukkit plugin;
    PlotMe_Core plotme;

    public PlotMeFeature(final Plugin plotmePlugin, final FaweBukkit p3) {
        super(plotmePlugin.getName());
        this.plotme = ((PlotMe_CorePlugin) plotmePlugin).getAPI();
        this.plugin = p3;

    }
    public boolean isAllowed(Player player, Plot plot, MaskType type) {
        return plot != null && type == MaskType.MEMBER ? plot.isAllowed(player.getUniqueId()) : player.getUniqueId().equals(plot.getOwnerId());
    }

    @Override
    public BukkitMask getMask(final FawePlayer<Player> fp, MaskType type) {
        final Player player = fp.parent;
        final Location location = player.getLocation();
        final Plot plot = this.plotme.getPlotMeCoreManager().getPlotById(new BukkitPlayer(player));
        if (plot == null) {
            return null;
        }
        if (isAllowed(player, plot, type)) {
            final Location pos1 = new Location(location.getWorld(), this.plotme.getGenManager(player.getWorld().getName()).bottomX(plot.getId(), new BukkitWorld(player.getWorld())), 0, this.plotme
            .getGenManager(player.getWorld().getName()).bottomZ(plot.getId(), new BukkitWorld(player.getWorld())));
            final Location pos2 = new Location(location.getWorld(), this.plotme.getGenManager(player.getWorld().getName()).topX(plot.getId(), new BukkitWorld(player.getWorld())), 256, this.plotme
            .getGenManager(player.getWorld().getName()).topZ(plot.getId(), new BukkitWorld(player.getWorld())));
            return new BukkitMask(pos1, pos2) {
                @Override
                public String getName() {
                    return plot.getId();
                }

                @Override
                public boolean isValid(FawePlayer player, MaskType type) {
                    return isAllowed((Player) player.parent, plot, type);
                }
            };
        }
        return null;
    }
}
