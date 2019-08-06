package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.bukkit.filter.WorldGuardFilter;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMask;
import com.boydti.fawe.regions.general.RegionFilter;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.AbstractRegion;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class WorldguardFlag extends BukkitMaskManager implements Listener {

    private WorldGuardPlugin worldguard;

    public WorldguardFlag(Plugin p2) {
        super("worldguardflag");
        this.worldguard = (WorldGuardPlugin) p2; // this.getWorldGuard();
    }

    @Override
    public FaweMask getMask(FawePlayer<Player> fp, MaskType type) {
        final Player player = BukkitAdapter.adapt(fp.toWorldEditPlayer());
        final LocalPlayer localplayer = this.worldguard.wrapPlayer(player);
        final RegionContainer container = WorldGuard.getInstance().getPlatform()
            .getRegionContainer();
        final RegionManager manager = container.get(fp.getWorld());

        return new FaweMask(new ManagerRegion(manager, localplayer)) {
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
        public BlockVector3 getMinimumPoint() {
            BlockVector3 point = null;
            for (Map.Entry<String, ProtectedRegion> entry : manager.getRegions().entrySet()) {
                BlockVector3 p = entry.getValue().getMinimumPoint();
                if (point == null) {
                    point = p;
                    continue;
                }
                point = point.getMinimum(p);
            }
            return point;
        }

        @Override
        public BlockVector3 getMaximumPoint() {
            BlockVector3 point = null;
            for (Map.Entry<String, ProtectedRegion> entry : manager.getRegions().entrySet()) {
                BlockVector3 p = entry.getValue().getMaximumPoint();
                if (point == null) {
                    point = p;
                    continue;
                }
                point = point.getMaximum(p);
            }
            return point;
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
            // Make sure that all these flags are not denied. Denies override allows. WorldGuardExtraFlags can add Flags.WORLDEDIT
            return manager.getApplicableRegions(position)
                .testState(localplayer, Flags.BUILD, Flags.BLOCK_PLACE, Flags.BLOCK_BREAK);
        }

    }
}
