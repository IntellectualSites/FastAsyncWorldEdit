package com.fastasyncworldedit.core;

import com.fastasyncworldedit.core.queue.implementation.QueueHandler;
import com.fastasyncworldedit.core.queue.implementation.preloader.Preloader;
import com.fastasyncworldedit.core.regions.FaweMaskManager;
import com.fastasyncworldedit.core.util.TaskManager;
import com.fastasyncworldedit.core.util.image.ImageViewer;
import com.sk89q.worldedit.entity.Player;

import java.io.File;
import java.util.Collection;
import java.util.UUID;

public interface IFawe {

    File getDirectory();

    TaskManager getTaskManager();

    Collection<FaweMaskManager> getMaskManagers();

    /**
     * @deprecated for removal with no replacement. Out of scope for FAWE.
     */
    @Deprecated(forRemoval = true, since = "TODO")
    default ImageViewer getImageViewer(Player player) {
        return null;
    }

    String getPlatform();

    UUID getUUID(String name);

    String getName(UUID uuid);

    default String getDebugInfo() {
        return "";
    }

    QueueHandler getQueueHandler();

    /**
     * Get the preloader instance and initialise if needed
     *
     * @param initialise if the preloader should be initialised if null
     * @return preloader instance
     */
    Preloader getPreloader(boolean initialise);

    @Deprecated(forRemoval = true, since = "TODO")
    default boolean isChunksStretched() {
        return true;
    }

    FAWEPlatformAdapterImpl getPlatformAdapter();

}
