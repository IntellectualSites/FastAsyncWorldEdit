package com.fastasyncworldedit.nukkit;

import cn.nukkit.Player;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplAdapter;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplLoader;
import com.sk89q.worldedit.entity.metadata.EntityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ResourceLock("NukkitImplLoader")
class NukkitEntityPropertiesTest {

    @AfterEach
    void tearDown() throws Exception {
        setInstance(null);
    }

    @Test
    void playerFacetReportsPlayerDerivedAndNotPasteable() {
        NukkitPlayer player = new NukkitPlayer(new Player());

        EntityProperties properties = player.getFacet(EntityProperties.class);

        assertNotNull(properties);
        assertTrue(properties.isPlayerDerived());
        assertTrue(properties.isLiving());
        assertFalse(properties.isPasteable());
    }

    @Test
    void entityFacetClassifiesKnownAnimalIdentifier() throws Exception {
        cn.nukkit.entity.Entity entity = new cn.nukkit.entity.Entity();
        NukkitImplAdapter adapter = mock(NukkitImplAdapter.class);
        when(adapter.getEntityIdentifier(entity)).thenReturn("minecraft:cow");
        setInstance(adapter);

        EntityProperties properties = new NukkitEntity(entity).getFacet(EntityProperties.class);

        assertNotNull(properties);
        assertTrue(properties.isAnimal());
        assertTrue(properties.isLiving());
        assertFalse(properties.isProjectile());
        assertTrue(properties.isPasteable());
    }

    @Test
    void entityFacetClassifiesKnownProjectileIdentifier() throws Exception {
        cn.nukkit.entity.Entity entity = new cn.nukkit.entity.Entity();
        NukkitImplAdapter adapter = mock(NukkitImplAdapter.class);
        when(adapter.getEntityIdentifier(entity)).thenReturn("minecraft:arrow");
        setInstance(adapter);

        EntityProperties properties = new NukkitEntity(entity).getFacet(EntityProperties.class);

        assertNotNull(properties);
        assertTrue(properties.isProjectile());
        assertFalse(properties.isLiving());
        assertTrue(properties.isPasteable());
    }

    private static void setInstance(NukkitImplAdapter adapter) throws Exception {
        Field field = NukkitImplLoader.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, adapter);
    }

}
