package com.fastasyncworldedit.bukkit.util;

import com.fastasyncworldedit.core.configuration.Caption;
import com.sk89q.worldedit.WorldEditException;

/**
 * Thrown if the world has been unloaded.
 */
public class WorldUnloadedException extends WorldEditException {

    /**
     * Create a new instance.
     */
    public WorldUnloadedException() {
        super(Caption.of("worldedit.error.world-unloaded"));
    }

}
