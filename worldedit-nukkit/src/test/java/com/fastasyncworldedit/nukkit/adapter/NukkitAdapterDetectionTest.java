package com.fastasyncworldedit.nukkit.adapter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("NukkitImplLoader")
class NukkitAdapterDetectionTest {

    @BeforeEach
    @AfterEach
    void resetLoader() throws Exception {
        setInstance(null);
    }

    @Test
    void getThrowsBeforeDetectIsCalled() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, NukkitImplLoader::get);

        assertEquals("NukkitImplLoader.detect() has not been called yet", thrown.getMessage());
    }

    @Test
    void detectUsesMotProbeToChooseAdapterClass() {
        String expectedAdapter = isMotApiVisible()
                ? "com.fastasyncworldedit.nukkit.adapter.mot.NukkitMOTAdapter"
                : "com.fastasyncworldedit.nukkit.adapter.nkx.NukkitAdapter";

        try {
            NukkitImplAdapter detected = NukkitImplLoader.detect();

            assertEquals(expectedPlatformName(), detected.getPlatformName());
            assertSame(detected, NukkitImplLoader.get());
            assertSame(detected, NukkitImplLoader.detect(), "detect() caches the runtime adapter instance");
        } catch (RuntimeException exception) {
            // The main Nukkit module loads adapter subprojects reflectively from the final shadow JAR.
            // If those adapter classes are not on the unit-test runtime classpath, the current behavior is a
            // RuntimeException naming the class selected by the MOT-vs-NKX probe.
            assertTrue(exception.getMessage().contains(expectedAdapter), exception::getMessage);
        }
    }

    @Test
    void cachedAdapterMetadataIsReturnedUnchanged() throws Exception {
        NukkitImplAdapter adapter = new MetadataAdapter("MOT-SNAPSHOT", 13);
        setInstance(adapter);

        NukkitImplAdapter loaded = NukkitImplLoader.get();

        assertSame(adapter, loaded);
        assertEquals("MOT-SNAPSHOT", loaded.getPlatformName());
        assertEquals(13, loaded.getBlockDataBits());
        assertEquals((1 << 13) - 1, loaded.getBlockDataMask());
    }

    private static boolean isMotApiVisible() {
        try {
            Class.forName("cn.nukkit.GameVersion");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static String expectedPlatformName() {
        return isMotApiVisible() ? "Nukkit-MOT" : "NKX";
    }

    private static void setInstance(NukkitImplAdapter adapter) throws Exception {
        Field field = NukkitImplLoader.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, adapter);
    }

    private static final class MetadataAdapter extends TestNukkitImplAdapter {

        private final String getPlatformName;
        private final int getBlockDataBits;

        private MetadataAdapter(String getPlatformName, int getBlockDataBits) {
            this.getPlatformName = getPlatformName;
            this.getBlockDataBits = getBlockDataBits;
        }

        @Override
        public String getPlatformName() {
            return getPlatformName;
        }

        @Override
        public int getBlockDataBits() {
            return getBlockDataBits;
        }
    }
}
