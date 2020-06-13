package com.boydti.fawe.util;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RegionCacheUtil {
    public RegionCacheUtil() {

    }

    public void cache(Region region) {
        Iterator<BlockVector3> iter = region.iterator();
    }

    public void run() {
        TaskManager.IMP.async(new Runnable() {
            @Override
            public void run() {

            }
        });
    }
}
