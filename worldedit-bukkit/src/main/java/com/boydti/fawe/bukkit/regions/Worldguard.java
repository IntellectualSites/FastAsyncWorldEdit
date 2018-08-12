package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.bukkit.filter.WorldGuardFilter;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.regions.FaweMask;
import com.boydti.fawe.regions.general.RegionFilter;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.AbstractRegion;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class Worldguard extends BukkitMaskManager implements Listener {
    WorldGuardPlugin worldguard;
    FaweBukkit plugin;

    private WorldGuardPlugin getWorldGuard() {
        final Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if ((plugin == null) || !(plugin instanceof WorldGuardPlugin)) {
            return null; // Maybe you want throw an exception instead
        }

        return (WorldGuardPlugin) plugin;
    }

    public Worldguard(final Plugin p2, final FaweBukkit p3) {
        super(p2.getName());
        this.worldguard = this.getWorldGuard();
        this.plugin = p3;
    }

    public ProtectedRegion getRegion(final com.sk89q.worldguard.LocalPlayer player, final Location loc) {
        RegionManager manager = this.worldguard.getRegionManager(loc.getWorld());
        if (manager == null) {
            if (this.worldguard.getGlobalStateManager().get(loc.getWorld()).useRegions) {
                System.out.println("Region capability is not enabled for WorldGuard.");
            } else {
                System.out.println("WorldGuard is not enabled for that world.");
            }
            return null;
        }
        final ProtectedRegion global = manager.getRegion("__global__");
        if (global != null && isAllowed(player, global)) {
            return global;
        }
        final ApplicableRegionSet regions = manager.getApplicableRegions(loc);
        for (final ProtectedRegion region : regions) {
            if (isAllowed(player, region)) {
                return region;
            }
        }
        return null;
    }

    public boolean isAllowed(LocalPlayer localplayer, ProtectedRegion region) {
        if (region.isOwner(localplayer) || region.isOwner(localplayer.getName())) {
            return true;
        } else if (region.getId().toLowerCase().equals(localplayer.getName().toLowerCase())) {
            return true;
        } else if (region.getId().toLowerCase().contains(localplayer.getName().toLowerCase() + "//")) {
            return true;
        } else if (region.isOwner("*")) {
            return true;
        }
        if (localplayer.hasPermission("fawe.worldguard.member")) {
            if (region.isMember(localplayer) || region.isMember(localplayer.getName())) {
                return true;
            } else if (region.isMember("*")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public FaweMask getMask(FawePlayer<Player> fp, MaskType type) {
        final Player player = fp.parent;
        final com.sk89q.worldguard.LocalPlayer localplayer = this.worldguard.wrapPlayer(player);
        final Location location = player.getLocation();
        final ProtectedRegion myregion = this.getRegion(localplayer, location);
        if (myregion != null) {
            final Location pos1;
            final Location pos2;
            if (myregion.getId().equals("__global__")) {
                pos1 = new Location(location.getWorld(), Integer.MIN_VALUE, 0, Integer.MIN_VALUE);
                pos2 = new Location(location.getWorld(), Integer.MAX_VALUE, 255, Integer.MAX_VALUE);
            } else {
                if (myregion instanceof ProtectedCuboidRegion) {
                    pos1 = new Location(location.getWorld(), myregion.getMinimumPoint().getBlockX(), myregion.getMinimumPoint().getBlockY(), myregion.getMinimumPoint().getBlockZ());
                    pos2 = new Location(location.getWorld(), myregion.getMaximumPoint().getBlockX(), myregion.getMaximumPoint().getBlockY(), myregion.getMaximumPoint().getBlockZ());
                } else {
                    return new FaweMask(adapt(myregion), myregion.getId()) {
                        @Override
                        public boolean isValid(FawePlayer player, MaskType type) {
                            return isAllowed(worldguard.wrapPlayer((Player) player.parent), myregion);
                        }
                    };
                }
            }
            return new BukkitMask(pos1, pos2) {
                @Override
                public String getName() {
                    return myregion.getId();
                }

                @Override
                public boolean isValid(FawePlayer player, MaskType type) {
                    return isAllowed(worldguard.wrapPlayer((Player) player.parent), myregion);
                }
            };
        } else {
            return null;
        }
    }

    @Override
    public RegionFilter getFilter(String world) {
        return new WorldGuardFilter(Bukkit.getWorld(world));
    }

    private static class AdaptedRegion extends AbstractRegion {
        private final ProtectedRegion region;

        public AdaptedRegion(ProtectedRegion region) {
            super(null);
            this.region = region;
        }

        @Override
        public Vector getMinimumPoint() {
            return region.getMinimumPoint();
        }

        @Override
        public Vector getMaximumPoint() {
            return region.getMaximumPoint();
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
            return region.contains(position);
        }
    }

    private static Region adapt(ProtectedRegion region) {
        if (region instanceof ProtectedCuboidRegion) {
            return new CuboidRegion(region.getMinimumPoint(), region.getMaximumPoint());
        }
        if (region instanceof GlobalProtectedRegion) {
            return RegionWrapper.GLOBAL();
        }
        if (region instanceof ProtectedPolygonalRegion) {
            ProtectedPolygonalRegion casted = (ProtectedPolygonalRegion) region;
            BlockVector max = region.getMaximumPoint();
            BlockVector min = region.getMinimumPoint();
            return new Polygonal2DRegion(null, casted.getPoints(), min.getBlockY(), max.getBlockY());
        }
        return new AdaptedRegion(region);
    }
}
