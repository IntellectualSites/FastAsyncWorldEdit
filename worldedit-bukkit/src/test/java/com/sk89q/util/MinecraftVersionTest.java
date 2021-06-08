package com.sk89q.util;

import com.boydti.fawe.bukkit.util.MinecraftVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class MinecraftVersionTest {

    private final MinecraftVersion latestVersion = new MinecraftVersion(1, 16, 3);

    @Test
    public void testComparable() {
        assertEquals(0, latestVersion.compareTo(new MinecraftVersion(1, 16, 3)));
        assertEquals(1, latestVersion.compareTo(new MinecraftVersion(1, 15, 2)));
        assertEquals(-1, latestVersion.compareTo(new MinecraftVersion(1, 17, 0)));
    }

    @Test
    public void testEqualOrHigher() {
        assertTrue(latestVersion.isEqualOrHigher(new MinecraftVersion(1, 16, 3)));
        assertTrue(latestVersion.isEqualOrHigher(new MinecraftVersion(1, 16, 2)));
        assertFalse(latestVersion.isEqualOrHigher(new MinecraftVersion(1, 16, 4)));
    }

    @Test
    public void testEqualOrHigherWithoutRelease() {
        assertTrue(latestVersion.isEqualOrHigher(new MinecraftVersion(1, 16)));
    }

    @Test
    public void testEqual() {
        assertTrue(latestVersion.isEqual(new MinecraftVersion(1, 16, 3)));
        assertFalse(latestVersion.isEqual(new MinecraftVersion(1, 16, 2)));
        assertFalse(latestVersion.isEqual(new MinecraftVersion(1, 16)));
    }

    @Test
    public void testEqualOrLower() {
        assertTrue(latestVersion.isEqualOrLower(new MinecraftVersion(1, 16, 3)));
        assertTrue(latestVersion.isEqualOrLower(new MinecraftVersion(1, 16, 4)));
        assertFalse(latestVersion.isEqualOrLower(new MinecraftVersion(1, 16, 2)));
    }

    @Test
    public void testForChunkStretched() {
        assertTrue(latestVersion.isEqualOrHigher(MinecraftVersion.NETHER));
        assertFalse(latestVersion.isLower(new MinecraftVersion(1, 14, 2)));
    }

}
