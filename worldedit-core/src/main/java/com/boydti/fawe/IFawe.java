package com.boydti.fawe;

import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.cui.CUI;
import com.boydti.fawe.util.gui.FormBuilder;
import com.boydti.fawe.util.image.ImageViewer;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.util.Collection;
import java.util.UUID;

public interface IFawe {
    public void debug(final String s);

    public File getDirectory();

    public void setupCommand(final String label, final FaweCommand cmd);

    public FawePlayer wrap(final Object obj);

    public void setupVault();

    public TaskManager getTaskManager();

    public FaweQueue getNewQueue(World world, boolean fast);

    public FaweQueue getNewQueue(String world, boolean fast);

    public String getWorldName(World world);

    public Collection<FaweMaskManager> getMaskManagers();

    public void startMetrics();

    default CUI getCUI(FawePlayer player) { return null; }

    default ImageViewer getImageViewer(FawePlayer player) { return null; }

    public default void registerPacketListener() {}

    default int getPlayerCount() {
        return Fawe.get().getCachedPlayers().size();
    }

    public String getPlatformVersion();

    public boolean isOnlineMode();

    public String getPlatform();

    public UUID getUUID(String name);

    public String getName(UUID uuid);

    public Object getBlocksHubApi();

    public default String getDebugInfo() {
        return "";
    }

    public default FormBuilder getFormBuilder() {
        return null;
    }
}
