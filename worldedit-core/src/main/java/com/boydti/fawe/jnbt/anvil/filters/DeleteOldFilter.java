package com.boydti.fawe.jnbt.anvil.filters;

import com.boydti.fawe.jnbt.anvil.MCAFilterCounter;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class DeleteOldFilter extends MCAFilterCounter {
    private final long time;

    public DeleteOldFilter(long time) {
        this.time = time;
        if (time < 1) {
            throw new IllegalArgumentException("Time must be positive");
        }
    }

    @Override
    public boolean appliesFile(Path path, BasicFileAttributes attr) {
        long modified = attr.lastModifiedTime().toMillis();
//        long access = attr.lastAccessTime().toMillis();
        long last = modified;//Math.max(modified, access);
        if (last != 0 && System.currentTimeMillis() - last > this.time) {
            path.toFile().delete();
            get().add(512 * 512 * 256);
        }
        return false;
    }
}