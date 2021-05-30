package com.boydti.fawe.util;

import com.boydti.fawe.config.Caption;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.extent.NullExtent;
import com.boydti.fawe.regions.FaweMask;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.regions.FaweMaskMultipleRegions;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class WEManager {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    public static final WEManager IMP = new WEManager();

    public final ArrayDeque<FaweMaskManager> managers = new ArrayDeque<>();

    public void cancelEditSafe(AbstractDelegateExtent parent, FaweException reason) throws FaweException {
        LOGGER.warn("CancelEditSafe was hit. Please ignore this message.");
        Extent currentExtent = parent.getExtent();
        if (!(currentExtent instanceof NullExtent)) {
            parent.extent = new NullExtent(parent.extent, reason);
        }
        throw reason;
    }

    public void cancelEdit(AbstractDelegateExtent parent, FaweException reason) throws WorldEditException {
        cancelEditSafe(parent, reason);
    }

    @Deprecated
    public Region[] getMask(Player player) {
        return getMask(player, FaweMaskManager.MaskType.getDefaultMaskType());
    }

    public boolean isIn(int x, int y, int z, Region region) {
        return region.contains(x, y, z);
    }

    /**
     * Get a player's mask.
     */
    public Region[] getMask(Player player, FaweMaskManager.MaskType type) {
        if (!Settings.IMP.REGION_RESTRICTIONS || player.hasPermission("fawe.bypass") || player.hasPermission("fawe.bypass.regions")) {
            return new Region[]{RegionWrapper.GLOBAL()};
        }
        Location loc = player.getLocation();
        String world = player.getWorld().getName();
        if (!world.equals(player.getMeta("lastMaskWorld"))) {
            player.deleteMeta("lastMaskWorld");
            player.deleteMeta("lastMask");
        }
        player.setMeta("lastMaskWorld", world);
        Set<FaweMask> masks = player.getMeta("lastMask");
        Set<Region> backupRegions = new HashSet<>();
        Set<Region> regions = new HashSet<>();


        if (masks == null) {
            masks = new HashSet<>();
        } else {
            synchronized (masks) {
                boolean removed = false;
                if (!masks.isEmpty()) {
                    Iterator<FaweMask> iterator = masks.iterator();
                    while (iterator.hasNext()) {
                        FaweMask mask = iterator.next();
                        if (mask.isValid(player, type)) {
                            if (mask instanceof FaweMaskMultipleRegions) {
                                removed = true;
                                for (Region region : ((FaweMaskMultipleRegions) mask).getRegions()) {
                                    if (region.contains(loc.toBlockPoint())) {
                                        regions.add(region);
                                        removed = false;
                                    } else {
                                        backupRegions.add(region);
                                    }
                                }
                            } else { // Normal FaweMask
                                Region region = mask.getRegion();
                                if (region.contains(loc.toBlockPoint())) {
                                    regions.add(region);
                                } else {
                                    removed = true;
                                    backupRegions.add(region);
                                }
                            }
                        } else {
                            player.print(Caption.of("Invalid Mask"));
                            removed = true;
                            iterator.remove();
                        }
                    }
                }
                if (!removed) {
                    return regions.toArray(new Region[0]);
                }
                masks.clear();
            }
        }
        Set<FaweMask> tmpMasks = new HashSet<>();
        for (FaweMaskManager manager : managers) {
            if (player.hasPermission("fawe." + manager.getKey())) {
                try {
                    if (manager.isExclusive() && !masks.isEmpty()) {
                        continue;
                    }
                    final FaweMask mask = manager.getMask(player, FaweMaskManager.MaskType.getDefaultMaskType());
                    if (mask != null) {
                        regions.add(mask.getRegion());
                        masks.add(mask);
                        if (manager.isExclusive()) {
                            break;
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            } else {
                player.printError(TextComponent.of("Missing permission " +  "fawe." + manager.getKey()));
            }
        }
        LOGGER.debug("Region info for " + player.getName());
        LOGGER.debug("There are " + backupRegions.size() + " backupRegions being added to Regions. Regions has " + regions.size() + " before backupRegions are added");
        regions.addAll(backupRegions);
        LOGGER.debug("Finished adding regions for " + player.getName());
        if (!masks.isEmpty()) {
            player.setMeta("lastMask", masks);
        } else {
            player.deleteMeta("lastMask");
        }
        return regions.toArray(new Region[0]);
    }


    public boolean intersects(Region region1, Region region2) {
        BlockVector3 rg1P1 = region1.getMinimumPoint();
        BlockVector3 rg1P2 = region1.getMaximumPoint();
        BlockVector3 rg2P1 = region2.getMinimumPoint();
        BlockVector3 rg2P2 = region2.getMaximumPoint();

        return rg1P1.getBlockX() <= rg2P2.getBlockX() && rg1P2.getBlockX() >= rg2P1.getBlockX()
            && rg1P1.getBlockZ() <= rg2P2.getBlockZ() && rg1P2.getBlockZ() >= rg2P1.getBlockZ();
    }

    public boolean regionContains(Region selection, HashSet<Region> mask) {
        for (Region region : mask) {
            if (this.intersects(region, selection)) {
                return true;
            }
        }
        return false;
    }
}
