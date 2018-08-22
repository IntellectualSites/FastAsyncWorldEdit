package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.bukkit.filter.WorldGuardFilter;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMask;
import com.boydti.fawe.regions.general.RegionFilter;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.AbstractRegion;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public class WorldguardFlag extends BukkitMaskManager implements Listener {
    private WorldGuardPlugin worldguard;
    FaweBukkit plugin;

    public WorldguardFlag(final Plugin p2, final FaweBukkit p3) {
        super("worldguardflag");
        this.worldguard = (WorldGuardPlugin) p2; // this.getWorldGuard();
        this.plugin = p3;
    }

    @Override
    public FaweMask getMask(FawePlayer<Player> fp, MaskType type) {
        final Player player = fp.parent;
        final LocalPlayer localplayer = this.worldguard.wrapPlayer(player);
        final RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        final RegionManager manager = container.get(fp.getWorld());

        return new FaweMask(new ManagerRegion(manager, localplayer), null) {
            @Override
            public boolean isValid(FawePlayer player, MaskType type) {
                // We rely on the region mask instead of this
                return true;
            }
        };
    }

    @Override
    public RegionFilter getFilter(String world) {
        return new WorldGuardFilter(Bukkit.getWorld(world));
    }

    /***
     * ManagerRegion wraps a RegionManager and will provide results based upon the regions enclosed
     */
    private static class ManagerRegion extends AbstractRegion {
        private final RegionManager manager;
        private final LocalPlayer localplayer;

        ManagerRegion(RegionManager manager, LocalPlayer localplayer) {
            super(null);
            this.manager = manager;
            this.localplayer = localplayer;
        }

        @Override
        public Vector getMinimumPoint() {
            Vector point = null;
            for (Map.Entry<String, ProtectedRegion> entry : manager.getRegions().entrySet()) {
                Vector p = entry.getValue().getMinimumPoint();
                if (point == null) {
                    point = p;
                    continue;
                }
                point = Vector.getMinimum(point, p);
            }
            return point;
        }

        @Override
        public Vector getMaximumPoint() {
            Vector point = null;
            for (Map.Entry<String, ProtectedRegion> entry : manager.getRegions().entrySet()) {
                Vector p = entry.getValue().getMaximumPoint();
                if (point == null) {
                    point = p;
                    continue;
                }
                point = Vector.getMaximum(point, p);
            }
            return point;
        }

        @Override
        public void expand(Vector... changes) {
            throw new UnsupportedOperationException("Region is immutable");
        }

        @Override
        public void contract(Vector... changes) {
            throw new UnsupportedOperationException("Region is immutable");
        }

        @Override
        public boolean contains(Vector position) {
            // Make sure that all these flags are not denied. Denies override allows. WorldGuardExtraFlags can add Flags.WORLDEDIT
            return  manager.getApplicableRegions(position).testState(localplayer, Flags.BUILD, Flags.BLOCK_PLACE, Flags.BLOCK_BREAK);
        }

    }
}
