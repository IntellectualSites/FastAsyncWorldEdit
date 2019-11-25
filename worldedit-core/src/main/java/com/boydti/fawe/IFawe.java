package com.boydti.fawe;

import com.boydti.fawe.beta.implementation.queue.QueueHandler;
import com.boydti.fawe.beta.implementation.cache.preloader.Preloader;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FaweQueue;
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

    Player wrap(final Object obj);

    void setupVault();

    TaskManager getTaskManager();

    FaweQueue getNewQueue(World world, boolean fast);

    FaweQueue getNewQueue(String world, boolean fast);

    String getWorldName(World world);

    Collection<FaweMaskManager> getMaskManagers();

    void startMetrics();

    default ImageViewer getImageViewer(Player player) {
        return null;
    }

    public default void registerPacketListener() {}

    String getPlatform();

    UUID getUUID(String name);

    String getName(UUID uuid);

    Object getBlocksHubApi();

    default String getDebugInfo() {
        return "";
    }

    QueueHandler getQueueHandler();

    Preloader getPreloader();

}
