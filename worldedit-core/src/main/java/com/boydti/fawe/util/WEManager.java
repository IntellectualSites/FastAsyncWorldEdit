package com.boydti.fawe.util;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.extent.NullExtent;
import com.boydti.fawe.regions.FaweMask;
import com.boydti.fawe.regions.FaweMaskManager;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.Region;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class WEManager {

    public final static WEManager IMP = new WEManager();

    public final ArrayDeque<FaweMaskManager> managers = new ArrayDeque<>();

    public void cancelEditSafe(Extent parent, BBC reason) throws FaweException {
        try {
            final Field field = AbstractDelegateExtent.class.getDeclaredField("extent");
            field.setAccessible(true);
            Object currentExtent = field.get(parent);
            if (!(currentExtent instanceof NullExtent)) {
                field.set(parent, new NullExtent((Extent) field.get(parent), reason));
            }
        } catch (final Exception e) {
            MainUtil.handleError(e);
        }
        throw new FaweException(reason);
    }

    public void cancelEdit(Extent parent, BBC reason) throws WorldEditException {
        cancelEditSafe(parent, reason);
    }

    public boolean maskContains(final HashSet<RegionWrapper> mask, final int x, final int z) {
        for (final RegionWrapper region : mask) {
            if ((x >= region.minX) && (x <= region.maxX) && (z >= region.minZ) && (z <= region.maxZ)) {
                return true;
            }
        }
        return false;
    }

    public boolean maskContains(RegionWrapper[] mask, final int x, final int z) {
        switch (mask.length) {
            case 0:
                return false;
            case 1:
                return mask[0].isIn(x, z);
            default:
                for (final RegionWrapper region : mask) {
                    if (region.isIn(x, z)) {
                        return true;
                    }
                }
                return false;
        }
    }

    @Deprecated
    public Region[] getMask(final FawePlayer<?> player) {
        return getMask(player, FaweMaskManager.MaskType.getDefaultMaskType());
    }

    public boolean isIn(int x, int y, int z, Region region) {
        if (region.contains(x, y, z)) {
            return true;
        }
        return false;
    }

    /**
     * Get a player's mask
     *
     * @param player
     * @return
     */
    public Region[] getMask(final FawePlayer<?> player, FaweMaskManager.MaskType type) {
        if (!Settings.IMP.REGION_RESTRICTIONS || player.hasPermission("fawe.bypass") || player.hasPermission("fawe.bypass.regions")) {
            return new Region[]{RegionWrapper.GLOBAL()};
        }
        FaweLocation loc = player.getLocation();
        String world = loc.world;
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
                    Iterator<FaweMask> iter = masks.iterator();
                    while (iter.hasNext()) {
                        FaweMask mask = iter.next();
                        if (mask.isValid(player, type)) {
                            Region region = mask.getRegion();
                            if (region.contains(loc.x, loc.y, loc.z)) {
                                regions.add(region);
                            } else {
                                removed = true;
                                backupRegions.add(region);
                            }
                        } else {
                            removed = true;
                            iter.remove();
                        }
                    }
                }
                if (!removed) return regions.toArray(new Region[regions.size()]);
                masks.clear();
            }
        }
        Set<FaweMask> tmpMasks = new HashSet<>();
        for (final FaweMaskManager manager : managers) {
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
        if (!tmpMasks.isEmpty()) {
            masks = tmpMasks;
            regions = masks.stream().map(mask -> mask.getRegion()).collect(Collectors.toSet());
        } else {
            regions.addAll(backupRegions);
        }
        if (!masks.isEmpty()) {
            player.setMeta("lastMask", masks);
        } else {
            player.deleteMeta("lastMask");
        }
        return regions.toArray(new Region[regions.size()]);
    }


    public boolean intersects(final Region region1, final Region region2) {
        Vector rg1P1 = region1.getMinimumPoint();
        Vector rg1P2 = region1.getMaximumPoint();
        Vector rg2P1 = region2.getMinimumPoint();
        Vector rg2P2 = region2.getMaximumPoint();

        return (rg1P1.getBlockX() <= rg2P2.getBlockX()) && (rg1P2.getBlockX() >= rg2P1.getBlockX()) && (rg1P1.getBlockZ() <= rg2P2.getBlockZ()) && (rg1P2.getBlockZ() >= rg2P1.getBlockZ());
    }

    public boolean regionContains(final Region selection, final HashSet<Region> mask) {
        for (final Region region : mask) {
            if (this.intersects(region, selection)) {
                return true;
            }
        }
        return false;
    }

    public boolean delay(final FawePlayer<?> player, final String command) {
        final long start = System.currentTimeMillis();
        return this.delay(player, new Runnable() {
            @Override
            public void run() {
                try {
                    if ((System.currentTimeMillis() - start) > 1000) {
                        BBC.WORLDEDIT_RUN.send(FawePlayer.wrap(player));
                    }
                    TaskManager.IMP.task(new Runnable() {
                        @Override
                        public void run() {
                            final long start = System.currentTimeMillis();
                            player.executeCommand(command.substring(1));
                            TaskManager.IMP.later(new Runnable() {
                                @Override
                                public void run() {
                                    SetQueue.IMP.addEmptyTask(new Runnable() {
                                        @Override
                                        public void run() {
                                            if ((System.currentTimeMillis() - start) > 1000) {
                                                BBC.WORLDEDIT_COMPLETE.send(FawePlayer.wrap(player));
                                            }
                                        }
                                    });
                                }
                            }, 2);
                        }
                    });
                } catch (final Exception e) {
                    MainUtil.handleError(e);
                }
            }
        }, false, false);
    }

    public boolean delay(final FawePlayer<?> player, final Runnable whenDone, final boolean delayed, final boolean onlyDelayedExecution) {
        final boolean free = SetQueue.IMP.addEmptyTask(null);
        if (free) {
            if (delayed) {
                if (whenDone != null) {
                    whenDone.run();
                }
            } else {
                if ((whenDone != null) && !onlyDelayedExecution) {
                    whenDone.run();
                } else {
                    return false;
                }
            }
        } else {
            if (!delayed && (player != null)) {
                BBC.WORLDEDIT_DELAYED.send(player);
            }
            SetQueue.IMP.addEmptyTask(whenDone);
        }
        return true;
    }
}
