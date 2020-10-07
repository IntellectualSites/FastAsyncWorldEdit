package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.bukkit.filter.WorldGuardFilter;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.regions.FaweMask;
import com.boydti.fawe.regions.general.RegionFilter;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.AbstractRegion;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

public class Worldguard extends BukkitMaskManager implements Listener {
    private final WorldGuardPlugin worldguard;

    private WorldGuardPlugin getWorldGuard() {
        final Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (!(plugin instanceof WorldGuardPlugin)) {
            return null; // Maybe you want throw an exception instead
        }

        return (WorldGuardPlugin) plugin;
    }

    public Worldguard(Plugin p2) {
        super(p2.getName());
        this.worldguard = this.getWorldGuard();
    }

    public ProtectedRegion getRegion(LocalPlayer player, Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container == null) {
            System.out.println("Region capability is not enabled for WorldGuard.");
            return null;
        }
        RegionManager manager = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (manager == null) {
            System.out.println("Region capability is not enabled for that world.");
            return null;
        }
        final ProtectedRegion global = manager.getRegion("__global__");
        if (global != null && isAllowed(player, global)) {
            return global;
        }
        final ApplicableRegionSet regions = manager.getApplicableRegions(BlockVector3.at(location.getX(), location.getY(), location.getZ()));
        //Merge WorldGuardFlag
        if (player.hasPermission("fawe.worldguardflag") && !regions.testState(player, Flags.BUILD, Flags.BLOCK_PLACE, Flags.BLOCK_BREAK)) {
            return null;
        }
        for (ProtectedRegion region : regions) {
            if (isAllowed(player, region)) {
                return region;
            }
        }
        return null;
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
    public FaweMask getMask(com.sk89q.worldedit.entity.Player wePlayer, MaskType type) {
        final Player player = BukkitAdapter.adapt(wePlayer);
        final LocalPlayer localplayer = this.worldguard.wrapPlayer(player);
        final Location location = player.getLocation();
        final ProtectedRegion myregion = this.getRegion(localplayer, location);
        if (myregion != null) {
            final BlockVector3 pos1;
            final BlockVector3 pos2;
            if (myregion.getId().equals("__global__")) {
                pos1 = BlockVector3.at(Integer.MIN_VALUE, 0, Integer.MIN_VALUE);
                pos2 = BlockVector3.at(Integer.MAX_VALUE, 255, Integer.MAX_VALUE);
            } else {
                if (myregion instanceof ProtectedCuboidRegion) {
                    pos1 = BlockVector3.at(myregion.getMinimumPoint().getBlockX(), myregion.getMinimumPoint().getBlockY(), myregion.getMinimumPoint().getBlockZ());
                    pos2 = BlockVector3.at(myregion.getMaximumPoint().getBlockX(), myregion.getMaximumPoint().getBlockY(), myregion.getMaximumPoint().getBlockZ());
                } else {
                    return new FaweMask(adapt(myregion)) {
                        @Override
                        public boolean isValid(com.sk89q.worldedit.entity.Player player, MaskType type) {
                            return isAllowed(worldguard.wrapPlayer(BukkitAdapter.adapt(player)), myregion);
                        }
                    };
                }
            }
            return new FaweMask(new CuboidRegion(pos1, pos2)) {
                @Override
                public boolean isValid(com.sk89q.worldedit.entity.Player player, MaskType type) {
                    return isAllowed(worldguard.wrapPlayer(BukkitAdapter.adapt(player)), myregion);
                }
            };
        }
        return null;
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

    private static Region adapt(ProtectedRegion region) {
        if (region instanceof ProtectedCuboidRegion) {
            return new CuboidRegion(region.getMinimumPoint(), region.getMaximumPoint());
        }
        if (region instanceof GlobalProtectedRegion) {
            return RegionWrapper.GLOBAL();
        }
        if (region instanceof ProtectedPolygonalRegion) {
            ProtectedPolygonalRegion casted = (ProtectedPolygonalRegion) region;
            BlockVector3 max = region.getMaximumPoint();
            BlockVector3 min = region.getMinimumPoint();
            return new Polygonal2DRegion(null, casted.getPoints(), min.getBlockY(), max.getBlockY());
        }
        return new AdaptedRegion(region);
    }
}
