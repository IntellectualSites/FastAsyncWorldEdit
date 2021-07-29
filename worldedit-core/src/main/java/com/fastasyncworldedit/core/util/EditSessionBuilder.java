package com.fastasyncworldedit.core.util;

import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.world.World;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;


//TODO Migrate all logic to com.sk89q.worldedit.EditSessionBuilder
@Deprecated(forRemoval = true)
public class EditSessionBuilder {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private World world;

    /**
     * An EditSession builder<br>
     * - Unset values will revert to their default<br>
     * <br>
     * player: The player doing the edit (defaults to to null)<br>
     * limit: Block/Entity/Action limit (defaults to unlimited)<br>
     * changeSet: Stores changes (defaults to config.yml value)<br>
     * allowedRegions: Allowed editable regions (defaults to player's allowed regions, or everywhere)<br>
     * autoQueue: Changes can occur before flushQueue() (defaults true)<br>
     * fastmode: bypasses history (defaults to player fastmode or config.yml console history)<br>
     * checkMemory: If low memory checks are enabled (defaults to player's fastmode or true)<br>
     * combineStages: If history is combined with dispatching
     *
     * @param world A world must be provided for all EditSession(s)
     */
    public EditSessionBuilder(World world) {
        this.world = world;
    }

    @Nullable
    public World getWorld() {
        return world;
    }

}
