package com.thevoxelbox.voxelsniper;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.thevoxelbox.voxelsniper.brush.IBrush;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Brush registration manager.
 */
public class Brushes {
    private Multimap<Class<? extends IBrush>, String> brushes = HashMultimap.create();

    /**
     * Register a brush for VoxelSniper to be able to use.
     *
     * @param clazz   Brush implementing IBrush interface.
     * @param handles Handles under which the brush can be accessed ingame.
     */
    public void registerSniperBrush(Class<? extends IBrush> clazz, String... handles) {
        Preconditions.checkNotNull(clazz, "Cannot register null as a class.");
        for (String handle : handles) {
            brushes.put(clazz, handle.toLowerCase());
        }
    }

    /**
     * Retrieve Brush class via handle Lookup.
     *
     * @param handle Case insensitive brush handle
     * @return Brush class
     */
    public Class<? extends IBrush> getBrushForHandle(String handle) {
        Preconditions.checkNotNull(handle, "Brushhandle can not be null.");
        if (!brushes.containsValue(handle.toLowerCase())) {
            return null;
        }

        for (Map.Entry<Class<? extends IBrush>, String> entry : brushes.entries()) {
            if (entry.getValue().equalsIgnoreCase(handle)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * @return Amount of IBrush classes registered with the system under Sniper visibility.
     */
    public int registeredSniperBrushes() {
        return brushes.keySet().size();
    }

    /**
     * @return Amount of handles registered with the system under Sniper visibility.
     */
    public int registeredSniperBrushHandles() {
        return brushes.size();
    }

    /**
     * @param clazz Brush class
     * @return All Sniper registered handles for the brush.
     */
    public Set<String> getSniperBrushHandles(Class<? extends IBrush> clazz) {
        return new HashSet<>(brushes.get(clazz));
    }

    /**
     * @return Immutable Multimap copy of all the registered brushes
     */
    public Multimap<Class<? extends IBrush>, String> getRegisteredBrushesMultimap() {
        return ImmutableMultimap.copyOf(brushes);
    }
}
