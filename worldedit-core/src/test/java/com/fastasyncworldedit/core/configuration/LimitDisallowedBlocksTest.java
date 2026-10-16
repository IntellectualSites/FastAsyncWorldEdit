package com.fastasyncworldedit.core.configuration;

import com.sk89q.worldedit.extension.platform.Actor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// The limit groups live on the shared Settings instance, so the tests must not run alongside each other
@Execution(ExecutionMode.SAME_THREAD)
class LimitDisallowedBlocksTest {

    private final Settings settings = Settings.settings();
    private List<String> defaultBlocks;

    @BeforeEach
    void rememberDefaultGroup() {
        defaultBlocks = settings.LIMITS.get("default").DISALLOWED_BLOCKS;
    }

    @AfterEach
    void restoreGroups() {
        settings.LIMITS.get("default").DISALLOWED_BLOCKS = defaultBlocks;
        settings.LIMITS.remove("builder");
        settings.LIMITS.remove("guest");
    }

    private static Actor actorWith(String... permissions) {
        Set<String> granted = Set.of(permissions);
        Actor actor = mock(Actor.class);
        when(actor.hasPermission(anyString())).thenAnswer(call -> granted.contains(call.<String>getArgument(0)));
        return actor;
    }

    private void addGroup(String name, String... blocks) {
        Settings.LIMITS group = new Settings.LIMITS();
        group.DISALLOWED_BLOCKS = List.of(blocks);
        settings.LIMITS.put(name, group);
    }

    @Test
    void entriesOfASingleGroupAreNamespacedAndLowerCase() {
        settings.LIMITS.get("default").DISALLOWED_BLOCKS = List.of("tnt", "minecraft:FIRE");

        assertEquals(Set.of("minecraft:tnt", "minecraft:fire"), settings.getLimit(actorWith()).DISALLOWED_BLOCKS);
    }

    @Test
    void blocksDisallowedByEveryGroupStayDisallowed() {
        addGroup("builder", "tnt", "fire", "minecraft:sand");
        addGroup("guest", "tnt", "fire");

        assertEquals(
                Set.of("minecraft:tnt", "minecraft:fire"),
                settings.getLimit(actorWith("fawe.limit.builder", "fawe.limit.guest")).DISALLOWED_BLOCKS
        );
    }

}
