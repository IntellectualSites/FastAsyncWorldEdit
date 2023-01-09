package com.fastasyncworldedit.core.extent.clipboard;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.IFawe;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class Clipboards {

    public static Clipboard load(Actor actor) {
        Path path = createActorPath(actor);
        if (Files.isDirectory(path)) {
            try {
                return new DiskBasedClipboard(path);
            } catch (IOException e) {
                return null; // failed to load
            }
        }
        return null; // not does exist
    }

    public static Clipboard create(Region region, BlockVector3 origin, Actor actor) {
        if (!(region instanceof CuboidRegion)) {
            return new BlockArrayClipboard(region, actor.getUniqueId());
        }
        return new DiskBasedClipboard(region.getDimensions(), region.getMinimumPoint(), origin, createActorPath(actor));
    }

    private static Path createActorPath(Actor actor) {
        Path folder = Objects.requireNonNull(Fawe.<IFawe>platform(), "Platform not present")
                .getDirectory().toPath()
                .resolve("clipboards")
                .resolve(actor.getUniqueId().toString());
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return folder;
    }
}
