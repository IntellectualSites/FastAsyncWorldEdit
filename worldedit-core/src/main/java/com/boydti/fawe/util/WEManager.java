package com.boydti.fawe.util;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.extent.NullExtent;
import com.boydti.fawe.regions.FaweMask;
import com.boydti.fawe.regions.FaweMaskManager;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class WEManager {

    public final static WEManager IMP = new WEManager();

    public final ArrayDeque<FaweMaskManager> managers = new ArrayDeque<>();

    public void cancelEditSafe(Extent parent, FaweException reason) throws FaweException {
        try {
            final Field field = AbstractDelegateExtent.class.getDeclaredField("extent");
            field.setAccessible(true);
            Object currentExtent = field.get(parent);
            if (!(currentExtent instanceof NullExtent)) {
                field.set(parent, new NullExtent((Extent) field.get(parent), reason));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw reason;
    }

    public void cancelEdit(Extent parent, FaweException reason) throws WorldEditException {
        cancelEditSafe(parent, reason);
    }

    public boolean maskContains(HashSet<RegionWrapper> mask, int x, int z) {
        for (RegionWrapper region : mask) {
            if (x >= region.minX && x <= region.maxX && z >= region.minZ && z <= region.maxZ) {
                return true;
            }
        }
        return false;
    }

    public boolean maskContains(RegionWrapper[] mask, int x, int z) {
        switch (mask.length) {
            case 0:
                return false;
            case 1:
                return mask[0].isIn(x, z);
            default:
                return Arrays.stream(mask).anyMatch(region -> region.isIn(x, z));
        }
    }

    @Deprecated
    public Region[] getMask(Player player) {
        return getMask(player, FaweMaskManager.MaskType.getDefaultMaskType());
    }

    public boolean isIn(int x, int y, int z, Region region) {
        return region.contains(x, y, z);
    }

    /**
     * Get a player's mask
     *
     * @param player
     * @return
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
                            Region region = mask.getRegion();
                            if (region.contains(loc.toBlockPoint())) {
                                regions.add(region);
                            } else {
                                removed = true;
                                backupRegions.add(region);
                            }
                        } else {
                            removed = true;
                            iterator.remove();
                        }
                    }
                }
                if (!removed) return regions.toArray(new Region[0]);
                masks.clear();
            }
        }
        Set<FaweMask> tmpMasks = new HashSet<>();
        for (FaweMaskManager manager : managers) {
            if (player.hasPermission("fawe." + manager.getKey())) {
                try {
                    if (manager.isExclusive() && !masks.isEmpty()) continue;
                    final FaweMask mask = manager.getMask(player, FaweMaskManager.MaskType.getDefaultMaskType());
                    if (mask != null) {
                        regions.add(mask.getRegion());
                        masks.add(mask);
                        if (manager.isExclusive()) break;
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        regions.addAll(backupRegions);
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

        return rg1P1.getBlockX() <= rg2P2.getBlockX() && rg1P2.getBlockX() >= rg2P1.getBlockX() &&
            rg1P1.getBlockZ() <= rg2P2.getBlockZ() &&
            rg1P2.getBlockZ() >= rg2P1.getBlockZ();
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
