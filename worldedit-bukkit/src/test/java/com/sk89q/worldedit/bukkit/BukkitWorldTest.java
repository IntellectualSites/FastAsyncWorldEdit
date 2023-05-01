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

package com.sk89q.worldedit.bukkit;

import com.sk89q.worldedit.util.TreeGenerator;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BukkitWorldTest {

    @Test
    public void testTreeTypeMapping() {
        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getVersion).thenReturn("Dummy Version");
            for (TreeGenerator.TreeType type : TreeGenerator.TreeType.values()) {
                if (type != TreeGenerator.TreeType.MANGROVE) assertNotNull(BukkitWorld.toBukkitTreeType(type), "No mapping for: " + type);
                if (type == TreeGenerator.TreeType.MANGROVE) assertNull(BukkitWorld.toBukkitTreeType(type), "No mapping for:" +
                        " " + type);
            }
        }

    }

}
