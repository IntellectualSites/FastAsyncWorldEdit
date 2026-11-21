package com.fastasyncworldedit.nukkit;

import cn.nukkit.level.Level;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplAdapter;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplLoader;
import com.fastasyncworldedit.nukkit.util.NukkitTreeTypes;
import com.fastasyncworldedit.nukkit.util.NukkitTreeTypes.NukkitTreeKind;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.TreeGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ResourceLock("NukkitImplLoader")
class NukkitTreeGenerationTest {

    private Level level;
    private EditSession editSession;
    private NukkitWorld world;
    private NukkitImplAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        level = mock(Level.class);
        editSession = mock(EditSession.class);
        when(level.getName()).thenReturn("world");
        when(level.getMinBlockY()).thenReturn(-64);
        when(level.getMaxBlockY()).thenReturn(319);
        world = new NukkitWorld(level);

        adapter = mock(NukkitImplAdapter.class);
        when(adapter.getCapabilities()).thenReturn(Set.of());
        when(adapter.getPlatformName()).thenReturn("TestFork");
        when(adapter.supportsTree(org.mockito.ArgumentMatchers.any(NukkitTreeKind.class))).thenReturn(true);
        // growTree returns false on the mock, so placement does not occur and history capture
        // is skipped.
        when(adapter.growTree(
                org.mockito.ArgumentMatchers.any(Level.class),
                org.mockito.ArgumentMatchers.any(NukkitTreeKind.class),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()
        )).thenReturn(false);
        setInstance(adapter);
    }

    @AfterEach
    void tearDown() throws Exception {
        setInstance(null);
    }

    // WorldEdit tree types that no Nukkit fork provides a generator for (RED_MUSHROOM,
    // BROWN_MUSHROOM, RANDOM_MUSHROOM, JUNGLE_BUSH, CHORUS_PLANT, PALE_OAK_CREAKING, RANDOM):
    // these must throw instead of silently substituting a different tree (e.g. oak). EnumSource
    // requires inline constant names, so the list is spelled out per test.
    @ParameterizedTest
    @EnumSource(
            value = TreeGenerator.TreeType.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"RED_MUSHROOM", "BROWN_MUSHROOM", "RANDOM_MUSHROOM", "JUNGLE_BUSH", "CHORUS_PLANT",
                    "PALE_OAK_CREAKING", "RANDOM"}
    )
    void treeGenerationReturnsFalseWhenPlacementFails(TreeGenerator.TreeType type) {
        // When the platform adapter reports no placement (growTree == false), generation returns
        // false without throwing and without recording history.
        boolean result;
        try {
            result = world.generateTree(type, editSession, BlockVector3.at(1, 64, 2));
        } catch (Exception e) {
            throw new AssertionError("generateTree should not throw when placement fails", e);
        }
        assertFalse(result);
        verifyNoInteractions(editSession);
    }

    @ParameterizedTest
    @EnumSource(
            value = TreeGenerator.TreeType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"RED_MUSHROOM", "BROWN_MUSHROOM", "RANDOM_MUSHROOM", "JUNGLE_BUSH", "CHORUS_PLANT",
                    "PALE_OAK_CREAKING", "RANDOM"}
    )
    void unsupportedTreeTypesThrowInsteadOfSubstituting(TreeGenerator.TreeType type) {
        // Types with no Nukkit generator must fail loudly before any snapshot or placement,
        // never silently place a different tree (the old behaviour was oak substitution).
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> world.generateTree(type, editSession, BlockVector3.at(1, 64, 2))
        );
        assertTrue(
                exception.getMessage().contains(type.name()),
                "message should name the requested type: " + exception.getMessage()
        );
        assertTrue(
                exception.getMessage().contains("Supported types"),
                "message should list supported types: " + exception.getMessage()
        );
        verifyNoInteractions(editSession);
    }

    @Test
    void randomTreeTypeMessageHintsAtSupportedFamilyRandoms() {
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> world.generateTree(
                        TreeGenerator.TreeType.RANDOM, editSession, BlockVector3.at(1, 64, 2)
                )
        );
        assertTrue(exception.getMessage().contains("randredwood"));
    }

    @Test
    void forkUnsupportedTreeKindThrowsWithPlatformName() {
        when(adapter.supportsTree(NukkitTreeKind.AZALEA)).thenReturn(false);
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> world.generateTree(
                        TreeGenerator.TreeType.AZALEA, editSession, BlockVector3.at(1, 64, 2)
                )
        );
        assertTrue(exception.getMessage().contains("AZALEA"));
        assertTrue(exception.getMessage().contains("TestFork"));
        verifyNoInteractions(editSession);
    }

    @Test
    void unknownTreeTypeIdThrowsInsteadOfFallingBackToOak() {
        com.sk89q.worldedit.world.generation.TreeType unknown =
                new com.sk89q.worldedit.world.generation.TreeType("definitely_not_a_tree");
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> world.generateTree(unknown, editSession, BlockVector3.at(1, 64, 2))
        );
        assertTrue(exception.getMessage().contains("definitely_not_a_tree"));
        verifyNoInteractions(editSession);
    }

    @Test
    void resolutionMatrixMapsExpectedKinds() {
        assertEquals(NukkitTreeKind.OAK, NukkitTreeTypes.resolve(TreeGenerator.TreeType.TREE));
        assertEquals(NukkitTreeKind.DARK_OAK, NukkitTreeTypes.resolve(TreeGenerator.TreeType.DARK_OAK));
        assertEquals(NukkitTreeKind.ACACIA, NukkitTreeTypes.resolve(TreeGenerator.TreeType.ACACIA));
        assertEquals(NukkitTreeKind.CHERRY, NukkitTreeTypes.resolve(TreeGenerator.TreeType.CHERRY));
        assertEquals(NukkitTreeKind.MANGROVE, NukkitTreeTypes.resolve(TreeGenerator.TreeType.TALL_MANGROVE));
        assertEquals(NukkitTreeKind.TALL_SPRUCE, NukkitTreeTypes.resolve(TreeGenerator.TreeType.MEGA_REDWOOD));
        assertEquals(NukkitTreeKind.SPRUCE, NukkitTreeTypes.resolve(TreeGenerator.TreeType.PINE));
        assertEquals(NukkitTreeKind.PALE_OAK, NukkitTreeTypes.resolve(TreeGenerator.TreeType.PALE_OAK));
        assertEquals(NukkitTreeKind.AZALEA, NukkitTreeTypes.resolve(TreeGenerator.TreeType.AZALEA));
        assertTrue(NukkitTreeTypes.isSupported(TreeGenerator.TreeType.WARPED_FUNGUS));
        assertFalse(NukkitTreeTypes.isSupported(TreeGenerator.TreeType.CHORUS_PLANT));
    }

    private static void setInstance(NukkitImplAdapter adapter) throws Exception {
        Field field = NukkitImplLoader.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, adapter);
    }
}
