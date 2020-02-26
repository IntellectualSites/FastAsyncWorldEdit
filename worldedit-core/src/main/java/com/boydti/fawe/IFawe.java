package com.boydti.fawe;

import com.boydti.fawe.beta.implementation.queue.QueueHandler;
import com.boydti.fawe.beta.implementation.cache.preloader.Preloader;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.image.ImageViewer;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.util.Collection;
import java.util.UUID;

public interface IFawe {

    void debug(final String s);

    File getDirectory();

    TaskManager getTaskManager();

    Collection<FaweMaskManager> getMaskManagers();

    default ImageViewer getImageViewer(Player player) {
        return null;
    }

    public default void registerPacketListener() {}

    String getPlatform();

    UUID getUUID(String name);

    String getName(UUID uuid);

    default String getDebugInfo() {
        return "";
    }

    QueueHandler getQueueHandler();

    Preloader getPreloader();

}
