package com.fastasyncworldedit.nukkit;

import cn.nukkit.level.Level;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplAdapter;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplLoader;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.TreeGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
        // growTree returns false on the mock, so placement does not occur and history capture
        // is skipped.
        when(adapter.growTree(
                org.mockito.ArgumentMatchers.any(Level.class),
                org.mockito.ArgumentMatchers.any(TreeGenerator.TreeType.class),
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

    @ParameterizedTest
    @EnumSource(TreeGenerator.TreeType.class)
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

    private static void setInstance(NukkitImplAdapter adapter) throws Exception {
        Field field = NukkitImplLoader.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, adapter);
    }
}
