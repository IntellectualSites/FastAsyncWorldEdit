/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.util;

import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldedit.LocalConfiguration;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The loaders write static limits, so the tests must not run alongside each other
@Execution(ExecutionMode.SAME_THREAD)
class ConfigurationLoadingTest {

    @TempDir
    Path directory;

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
    void yamlOptionsAreReadFromTheirPaths() throws IOException {
        LocalConfiguration config = yaml("""
                limits:
                  max-polygonal-points:
                    default: -5
                    maximum: 30
                  vertical-height:
                    default: 0
                  max-super-pickaxe-size: 9
                  disallowed-blocks: [Bedrock, "minecraft:TNT"]
                  allowed-data-cycle-blocks: ["minecraft:oak_stairs"]
                navigation-wand:
                  item: minecraft:stick
                  max-distance: 70
                saving:
                  dir: saved
                use-inventory:
                  enable: true
                shell-save-type: "  "
                debug: true
                """);

        assertEquals(-1, config.defaultMaxPolygonalPoints);
        assertEquals(30, config.maxPolygonalPoints);
        assertEquals(1, config.defaultVerticalHeight);
        assertEquals(9, config.maxSuperPickaxeSize);
        assertEquals(9, LocalConfiguration.MAX_SUPER_RADIUS);
        assertEquals(Set.of("minecraft:bedrock", "minecraft:tnt"), config.disallowedBlocks);
        assertEquals(Set.of("minecraft:oak_stairs"), config.allowedDataCycleBlocks);
        assertEquals("minecraft:stick", config.navigationWand);
        assertEquals(70, config.navigationWandMaxDistance);
        assertEquals("saved", config.saveDir);
        assertTrue(config.useInventory);
        assertNull(config.shellSaveType);
        assertTrue(config.profile);
        // Not in the file
        assertEquals("minecraft:wooden_axe", config.wandItem);
        assertEquals("craftscripts", config.scriptsDir);
        assertFalse(config.snapshotsConfigured);
    }

    @Test
    void propertiesAreReadFromTheirKeysAndWrittenBack() throws IOException {
        LocalConfiguration config = properties("""
                max-polygon-points=30
                default-max-polygon-points=-5
                nav-wand-item=minecraft:stick
                schematic-save-dir=saved
                use-inventory=true
                disallowed-blocks=Bedrock, minecraft:TNT
                """);

        assertEquals(30, config.maxPolygonalPoints);
        assertEquals(-1, config.defaultMaxPolygonalPoints);
        assertEquals("minecraft:stick", config.navigationWand);
        assertEquals("saved", config.saveDir);
        assertTrue(config.useInventory);
        assertEquals(Set.of("minecraft:bedrock", "minecraft:tnt"), config.disallowedBlocks);
        assertEquals("minecraft:wooden_axe", config.wandItem);

        Properties written = new Properties();
        try (InputStream in = Files.newInputStream(directory.resolve("worldedit.properties"))) {
            written.load(in);
        }
        assertEquals("30", written.getProperty("max-polygon-points"));
        assertEquals("minecraft:wooden_axe", written.getProperty("wand-item"));
        assertEquals("craftscripts", written.getProperty("craftscript-dir"));
    }

    @Test
    void anEmptyPropertyIsAnEmptySet() throws IOException {
        // The first load writes the empty default, the second one reads it back
        properties("");
        LocalConfiguration config = new PropertiesConfiguration(directory.resolve("worldedit.properties"));
        config.load();

        assertEquals(Set.of(), config.disallowedBlocks);
    }

}
