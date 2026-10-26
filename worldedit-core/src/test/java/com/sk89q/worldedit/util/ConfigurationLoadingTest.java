package com.sk89q.worldedit.util;

import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.session.SessionManager;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

// The loaders write static limits, so the tests must not run alongside each other
@Execution(ExecutionMode.SAME_THREAD)
class ConfigurationLoadingTest {

    @TempDir
    Path directory;

    // The statics the loaders write, put back after each test so later test classes see what they would have
    private int maxRadius;
    private int maxSuperRadius;
    private int maxBrushRadius;
    private int maxButcherRadius;
    private int maxHistorySize;
    private int expirationGrace;

    @BeforeEach
    void saveStatics() {
        maxRadius = LocalConfiguration.MAX_RADIUS;
        maxSuperRadius = LocalConfiguration.MAX_SUPER_RADIUS;
        maxBrushRadius = LocalConfiguration.MAX_BRUSH_RADIUS;
        maxButcherRadius = LocalConfiguration.MAX_BUTCHER_RADIUS;
        maxHistorySize = LocalSession.MAX_HISTORY_SIZE;
        expirationGrace = SessionManager.EXPIRATION_GRACE;
    }

    @AfterEach
    void restoreStatics() {
        LocalConfiguration.MAX_RADIUS = maxRadius;
        LocalConfiguration.MAX_SUPER_RADIUS = maxSuperRadius;
        LocalConfiguration.MAX_BRUSH_RADIUS = maxBrushRadius;
        LocalConfiguration.MAX_BUTCHER_RADIUS = maxButcherRadius;
        LocalSession.MAX_HISTORY_SIZE = maxHistorySize;
        SessionManager.EXPIRATION_GRACE = expirationGrace;
    }

    private LocalConfiguration yaml(String content) throws IOException {
        Path file = directory.resolve("worldedit-config.yml");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        LocalConfiguration config = new YAMLConfiguration(new YAMLProcessor(file.toFile(), true), LogManager.getLogger());
        config.load();
        return config;
    }

    private LocalConfiguration properties(String content) throws IOException {
        Path file = directory.resolve("worldedit.properties");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        LocalConfiguration config = new PropertiesConfiguration(file);
        config.load();
        return config;
    }

    @Test
    void yamlDisallowedBlocksAreNamespacedAndLowerCase() throws IOException {
        LocalConfiguration config = yaml("""
                limits:
                  disallowed-blocks: [Bedrock, "minecraft:TNT"]
                """);

        assertEquals(Set.of("minecraft:bedrock", "minecraft:tnt"), config.disallowedBlocks);
    }

    @Test
    void propertiesDisallowedBlocksAreNamespacedAndLowerCase() throws IOException {
        LocalConfiguration config = properties("""
                disallowed-blocks=Bedrock, minecraft:TNT
                """);

        assertEquals(Set.of("minecraft:bedrock", "minecraft:tnt"), config.disallowedBlocks);
    }

    @Test
    void anEmptyPropertyIsAnEmptySet() throws IOException {
        // The first load writes the empty default, the second one reads it back
        properties("");
        LocalConfiguration config = new PropertiesConfiguration(directory.resolve("worldedit.properties"));
        config.load();

        assertEquals(Set.of(), config.disallowedBlocks);
    }

    @Test
    void blankYamlEntriesAreSkipped() throws IOException {
        LocalConfiguration config = yaml("""
                limits:
                  disallowed-blocks: ["", "  ", " TNT "]
                """);

        assertEquals(Set.of("minecraft:tnt"), config.disallowedBlocks);
    }

}
