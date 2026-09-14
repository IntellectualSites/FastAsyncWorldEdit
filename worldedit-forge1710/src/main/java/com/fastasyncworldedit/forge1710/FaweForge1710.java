package com.fastasyncworldedit.forge1710;

import com.fastasyncworldedit.core.FAWEPlatformAdapterImpl;
import com.fastasyncworldedit.core.IFawe;
import com.fastasyncworldedit.core.queue.implementation.QueueHandler;
import com.fastasyncworldedit.core.queue.implementation.preloader.Preloader;
import com.fastasyncworldedit.core.regions.FaweMaskManager;
import com.fastasyncworldedit.core.util.TaskManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class FaweForge1710 implements IFawe {

    private final File directory;
    private final Forge1710TaskManager taskManager;
    private final Forge1710PlatformAdapter platformAdapter = new Forge1710PlatformAdapter();

    public FaweForge1710(File directory, Forge1710TaskManager taskManager) {
        this.directory = directory;
        this.taskManager = taskManager;
    }

    @Override
    public File getDirectory() {
        return directory;
    }

    @Override
    public TaskManager getTaskManager() {
        return taskManager;
    }

    @Override
    public Collection<FaweMaskManager> getMaskManagers() {
        return Collections.emptyList();
    }

    @Override
    public String getPlatform() {
        return "Forge-1.7.10";
    }

    @Override
    public UUID getUUID(String name) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return null;
        }
        for (Object o : server.getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) o;
            if (player.getCommandSenderName().equalsIgnoreCase(name)) {
                return player.getUniqueID();
            }
        }
        GameProfile profile = server.func_152358_ax().func_152655_a(name);
        return profile != null ? profile.getId() : null;
    }

    @Override
    public String getName(UUID uuid) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return null;
        }
        for (Object o : server.getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) o;
            if (player.getUniqueID().equals(uuid)) {
                return player.getCommandSenderName();
            }
        }
        GameProfile profile = server.func_152358_ax().func_152652_a(uuid);
        return profile != null ? profile.getName() : null;
    }

    @Override
    public QueueHandler getQueueHandler() {
        return new Forge1710QueueHandler();
    }

    @Override
    public Preloader getPreloader(boolean initialise) {
        return null;
    }

    @Override
    public FAWEPlatformAdapterImpl getPlatformAdapter() {
        return platformAdapter;
    }

}
