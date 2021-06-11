package com.fastasyncworldedit;

import com.fastasyncworldedit.beta.implementation.preloader.Preloader;
import com.fastasyncworldedit.beta.implementation.queue.QueueHandler;
import com.fastasyncworldedit.regions.FaweMaskManager;
import com.fastasyncworldedit.util.TaskManager;
import com.fastasyncworldedit.util.image.ImageViewer;
import com.sk89q.worldedit.entity.Player;

import java.io.File;
import java.util.Collection;
import java.util.UUID;

public interface IFawe {

    File getDirectory();

    TaskManager getTaskManager();

    Collection<FaweMaskManager> getMaskManagers();

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

    Preloader getPreloader();

    default boolean isChunksStretched() {
        return true;
    }

    FAWEPlatformAdapterImpl getPlatformAdapter();

}
