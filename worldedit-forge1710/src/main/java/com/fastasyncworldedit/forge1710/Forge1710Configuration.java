package com.fastasyncworldedit.forge1710;

import com.sk89q.worldedit.util.PropertiesConfiguration;

import java.nio.file.Path;

public class Forge1710Configuration extends PropertiesConfiguration {

    private final Path workingDirectory;

    public Forge1710Configuration(Path workingDirectory) {
        super(workingDirectory.resolve("worldedit.properties"));
        this.workingDirectory = workingDirectory;
    }

    @Override
    public Path getWorkingDirectoryPath() {
        return workingDirectory;
    }

}
