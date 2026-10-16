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

package com.sk89q.util.yaml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class YAMLProcessorTest {

    @TempDir
    Path directory;

    private YAMLProcessor load(String content) throws IOException {
        Path file = directory.resolve("config.yml");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        YAMLProcessor config = new YAMLProcessor(file.toFile(), false, YAMLFormat.EXTENDED);
        config.load();
        return config;
    }

    @Test
    void typedListsKeepOnlyEntriesOfTheirType() throws IOException {
        YAMLProcessor config = load("""
                mixed: [1, two, 3.5, true, null]
                """);

        assertEquals(List.of("1", "two", "3.5", "true"), config.getStringList("mixed", null));
        assertEquals(List.of(1, 3), config.getIntList("mixed", null));
        assertEquals(List.of(1.0, 3.5), config.getDoubleList("mixed", null));
        assertEquals(List.of(true), config.getBooleanList("mixed", null));
    }

    @Test
    void aMissingListGivesTheDefault() throws IOException {
        YAMLProcessor config = load("present: [a]\n");
        List<String> def = new ArrayList<>(List.of("x"));

        assertSame(def, config.getStringList("absent", def));
        assertEquals(List.of(), config.getStringList("absent", null));
        // Defaults are not written unless asked for
        assertNull(config.getProperty("absent"));

        config.setWriteDefaults(true);
        config.getStringList("absent", def);
        assertEquals(def, config.getProperty("absent"));
    }

    @Test
    void removingAPropertyThatIsNotThereDoesNothing() throws IOException {
        YAMLProcessor config = load("""
                a:
                  b: 1
                  c: 2
                scalar: 5
                """);

        config.removeProperty("missing.b");
        config.removeProperty("scalar.b");
        config.removeProperty("a.b");

        assertNull(config.getProperty("a.b"));
        assertEquals(2, config.getInt("a.c"));
        assertEquals(5, config.getInt("scalar"));
    }

    @Test
    void commentLinesArePrefixedOnce() throws IOException {
        YAMLProcessor config = load("a: 1\n");

        config.setComment("a", "plain", "# already a comment", "");

        assertEquals(
                String.join(YAMLProcessor.LINE_BREAK, "# plain", "# already a comment", "# "),
                config.getComment("a")
        );
    }

}
