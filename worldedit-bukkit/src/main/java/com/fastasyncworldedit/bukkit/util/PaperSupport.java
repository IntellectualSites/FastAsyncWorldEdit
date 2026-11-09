package com.fastasyncworldedit.bukkit.util;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.CompletableFuture;

/**
 * Not for external use. May change at any point.
 */
@ApiStatus.Internal
public enum PaperSupport {
    ;
    public static final boolean PAPER;

    static {
        PAPER = hasClass("com.destroystokyo.paper.PaperConfig")
            || hasClass("io.papermc.paper.configuration.Configuration");
    }

    public static boolean isPaper() {
        return PAPER;
    }

    public static CompletableFuture<Boolean> teleportAsync(Entity entity, Location location) {
        if (isPaper()) {
            return entity.teleportAsync(location);
        }
        return CompletableFuture.completedFuture(entity.teleport(location));
    }

    public static CompletableFuture<Boolean> teleportAsync(Entity entity, Location location, PlayerTeleportEvent.TeleportCause cause) {
        if (isPaper()) {
            return entity.teleportAsync(location, cause);
        }
        return CompletableFuture.completedFuture(entity.teleport(location, cause));
    }

    public static CompletableFuture<Chunk> getChunkAtAsync(Location location) {
        return getChunkAtAsync(location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, true);
    }

    public static CompletableFuture<Chunk> getChunkAtAsync(World world, int cx, int cz, boolean gen) {
        return getChunkAtAsync(world, cx, cz, gen, false);
    }

    public static CompletableFuture<Chunk> getChunkAtAsync(World world, int cx, int cz, boolean gen, boolean urgent) {
        if (isPaper()) {
            return world.getChunkAtAsync(cx, cz, gen, urgent);
        } else {
            return CompletableFuture.completedFuture(world.getChunkAt(cx, cz));
        }
    }

    private static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
