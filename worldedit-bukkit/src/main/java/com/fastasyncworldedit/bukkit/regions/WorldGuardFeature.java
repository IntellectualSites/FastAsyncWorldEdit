package com.fastasyncworldedit.bukkit.regions;

import com.fastasyncworldedit.core.regions.FaweMask;
import com.fastasyncworldedit.core.regions.RegionWrapper;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.AbstractRegion;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionIntersection;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class WorldGuardFeature extends BukkitMaskManager implements Listener {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private final WorldGuardPlugin worldguard;

    public WorldGuardFeature(Plugin plugin) {
        super(plugin.getName());
        this.worldguard = this.getWorldGuard();
        LOGGER.info("Plugin 'WorldGuard' found. Using it now.");

    }

    private static Region adapt(ProtectedRegion region) {
        if (region instanceof ProtectedCuboidRegion) {
            return new CuboidRegion(region.getMinimumPoint(), region.getMaximumPoint());
        }
        if (region instanceof GlobalProtectedRegion) {
            return RegionWrapper.GLOBAL();
        }
        if (region instanceof ProtectedPolygonalRegion casted) {
            BlockVector3 max = region.getMaximumPoint();
            BlockVector3 min = region.getMinimumPoint();
            return new Polygonal2DRegion(null, casted.getPoints(), min.y(), max.y());
        }
        return new AdaptedRegion(region);
    }

    private WorldGuardPlugin getWorldGuard() {
        final Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (!(plugin instanceof WorldGuardPlugin)) {
            return null; // Maybe you want throw an exception instead
        }

        return (WorldGuardPlugin) plugin;
    }

    /**
     * Get the WorldGuard regions a player is allowed in based on the current location, or if isWhitelist set to false, get the
     * blacklisted regions for the world.
     */
    public Set<ProtectedRegion> getRegions(LocalPlayer player, Location location, boolean isWhitelist) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container == null) {
            LOGGER.info("Region capability is not enabled for WorldGuard.");
            return Collections.emptySet();
        }
        RegionManager manager = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (manager == null) {
            LOGGER.info("Region capability is not enabled for that world.");
            return Collections.emptySet();
        }
        //Merge WorldGuardFlag
        if (isWhitelist) {
            // Only consider global when whitelisting
            final ProtectedRegion global = manager.getRegion("__global__");
            if (global != null && isAllowed(player, global)) {
                return Collections.singleton(global);
            }
            final ApplicableRegionSet regions = manager.getApplicableRegions(BlockVector3.at(
                    location.getX(),
                    location.getY(),
                    location.getZ()
            ));
            if (player.hasPermission("fawe.worldguardflag") && !regions.testState(
                    player,
                    Flags.BUILD,
                    Flags.BLOCK_PLACE,
                    Flags.BLOCK_BREAK
            )) {
                return Collections.emptySet();
            }
            Set<ProtectedRegion> protectedRegions = new HashSet<>();
            for (ProtectedRegion region : regions) {
                if (isAllowed(player, region)) {
                    protectedRegions.add(region);
                }
            }
            return Collections.unmodifiableSet(protectedRegions);
        } else {
            final Collection<ProtectedRegion> regions = manager.getRegions().values();
            Set<ProtectedRegion> protectedRegions = new HashSet<>();
            for (ProtectedRegion region : regions) {
                if (region.getId().equals("__global__")) {
                    continue;
                }
                if (!isAllowed(player, region)) {
                    protectedRegions.add(region);
                }
            }
            return Collections.unmodifiableSet(protectedRegions);
        }
    }

    public boolean isAllowed(LocalPlayer localplayer, ProtectedRegion region) {
        //Check if player is the owner of the region, the region's ID contains the player's name (why?), or if the region's owners contains "*".
        if (region.isOwner(localplayer) || region.isOwner(localplayer.getName())) {
            return true;
        } else if (region.getId().toLowerCase(Locale.ROOT).equals(localplayer.getName().toLowerCase(Locale.ROOT))) {
            return true;
        } else if (region.getId().toLowerCase(Locale.ROOT).contains(localplayer.getName().toLowerCase(Locale.ROOT) + "//")) {
            return true;
        } else if (region.isOwner("*")) {
            return true;
        } else if (localplayer.hasPermission("worldguard.region.bypass")
                || localplayer.hasPermission("worldguard.region.bypass."
                + localplayer.getWorld().getName().toLowerCase(Locale.ROOT))) {
            return true;
        }

        //Check if the player has the FAWE permission for editing in WG regions as member, then checking member status.
        if (localplayer.hasPermission("fawe.worldguard.member")) {
            if (region.isMember(localplayer) || region.isMember(localplayer.getName())) {
                return true;
            } else {
                return region.isMember("*");
            }
        }
        return false;
    }

    @Override
    public FaweMask getMask(com.sk89q.worldedit.entity.Player wePlayer, MaskType type, boolean isWhitelist) {
        final Player player = BukkitAdapter.adapt(wePlayer);
        final LocalPlayer localplayer = this.worldguard.wrapPlayer(player);
        final Location location = player.getLocation();
        final Set<ProtectedRegion> regions = this.getRegions(localplayer, location, isWhitelist);
        if (!regions.isEmpty()) {
            RegionManager manager = WorldGuard
                    .getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(location.getWorld()));
            if (manager == null) {
                return null;
            }
            Set<Region> result = new HashSet<>();
            for (ProtectedRegion myRegion : regions) {
                if (myRegion.getId().equals("__global__")) {
                    return new FaweMask(RegionWrapper.GLOBAL()) {
                        @Override
                        public boolean isValid(com.sk89q.worldedit.entity.Player player, MaskType type) {
                            return manager.hasRegion(myRegion.getId())
                                    && isAllowed(worldguard.wrapPlayer(BukkitAdapter.adapt(player)), myRegion);
                        }
                    };
                } else {
                    if (myRegion instanceof ProtectedCuboidRegion) {
                        result.add(new CuboidRegion(myRegion.getMinimumPoint(), myRegion.getMaximumPoint()));
                    } else {
                        result.add(adapt(myRegion));
                    }
                }
            }
            return new FaweMask(new RegionIntersection(wePlayer.getWorld(), result)) {
                @Override
                public boolean isValid(com.sk89q.worldedit.entity.Player player, MaskType type) {
                    final LocalPlayer localplayer = worldguard.wrapPlayer(BukkitAdapter.adapt(player));
                    for (ProtectedRegion myRegion : regions) {
                        if (!manager.hasRegion(myRegion.getId()) || !isAllowed(localplayer, myRegion)) {
                            return false;
                        }
                    }
                    return true;
                }
            };
        }
        return null;
    }

    private static class AdaptedRegion extends AbstractRegion {

        private final ProtectedRegion region;

        public AdaptedRegion(ProtectedRegion region) {
            super(null);
            this.region = region;
        }

        @Override
        public BlockVector3 getMinimumPoint() {
            return region.getMinimumPoint();
        }

        @Override
        public BlockVector3 getMaximumPoint() {
            return region.getMaximumPoint();
        }

        @Override
        public void expand(BlockVector3... changes) {
            throw new UnsupportedOperationException("Region is immutable");
        }

        @Override
        public void contract(BlockVector3... changes) {
            throw new UnsupportedOperationException("Region is immutable");
        }

        @Override
        public boolean contains(BlockVector3 position) {
            return region.contains(position);
        }

    }

}
