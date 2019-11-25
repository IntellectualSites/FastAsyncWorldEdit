package com.boydti.fawe.bukkit.preloader;

import com.boydti.fawe.Fawe;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.PluginBase;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.PhantomReference;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class PluginPreloader extends PluginBase {
    private World world;
    private Set<BlockVector2> loaded;
    private int index;
    private AtomicBoolean invalidator;
    private final Object invalidatorLock;

    public PluginPreloader() {
        invalidator = new AtomicBoolean();
        invalidatorLock = new Object();
    }

    public AtomicBoolean invalidate() {
        synchronized (invalidatorLock) {
            invalidator.set(false);
            return invalidator = new AtomicBoolean(true);
        }
    }

    private synchronized void unload() {
        World oldWorld = world;
        if (oldWorld != null) {
            Set<BlockVector2> toUnload = loaded;
            if (loaded != null && index > 0) {
                Iterator<BlockVector2> iter = toUnload.iterator();
                Fawe.get().getQueueHandler().sync(() -> {
                    for (int i = 0; i < index && iter.hasNext(); i++) {
                        BlockVector2 chunk = iter.next();
                        world.removePluginChunkTicket(chunk.getX(), chunk.getZ(), this);
                    }
                });
            }
        }
        this.world = null;
        this.loaded = null;
        this.index = 0;
    }

    public void update(Region region) {
        AtomicBoolean invalidator = invalidate();
        synchronized (this) {
            com.sk89q.worldedit.world.World weWorld = region.getWorld();
            if (weWorld == null) {
                return;
            }
            unload();
            index = 0;
            world = BukkitAdapter.adapt(weWorld);
            loaded = region.getChunks();
            Iterator<BlockVector2> iter = loaded.iterator();

            if (!invalidator.get()) return;
            Fawe.get().getQueueHandler().syncWhenFree(() -> {
                for (; iter.hasNext() && invalidator.get();index++) {
                    BlockVector2 chunk = iter.next();
                    if (!world.isChunkLoaded(chunk.getX(), chunk.getZ())) {
                        world.addPluginChunkTicket(chunk.getX(), chunk.getZ(), this);
                    }
                }
            });
        }
    }

    public void clear() {
        invalidate();
        unload();
    }

    @Override
    public @NotNull File getDataFolder() {
        return null;
    }

    @Override
    public @NotNull PluginDescriptionFile getDescription() {
        return null;
    }

    @Override
    public @NotNull FileConfiguration getConfig() {
        return null;
    }

    @Override
    public @Nullable InputStream getResource(@NotNull String filename) {
        return null;
    }

    @Override
    public void saveConfig() {

    }

    @Override
    public void saveDefaultConfig() {

    }

    @Override
    public void saveResource(@NotNull String resourcePath, boolean replace) {

    }

    @Override
    public void reloadConfig() {

    }

    @Override
    public @NotNull PluginLoader getPluginLoader() {
        return null;
    }

    @Override
    public @NotNull Server getServer() {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onLoad() {

    }

    @Override
    public void onEnable() {

    }

    @Override
    public boolean isNaggable() {
        return false;
    }

    @Override
    public void setNaggable(boolean canNag) {

    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return null;
    }

    @Override
    public @NotNull Logger getLogger() {
        return null;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        return null;
    }
}
