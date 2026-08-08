package com.fastasyncworldedit.nukkit;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.generic.BaseFullChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.fastasyncworldedit.core.queue.implementation.packet.ChunkPacket;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplAdapter;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplLoader;
import com.fastasyncworldedit.nukkit.adapter.NukkitPlatformCapabilities;
import com.fastasyncworldedit.nukkit.adapter.TestNukkitImplAdapter;
import com.fastasyncworldedit.nukkit.blocks.NukkitGetBlocks;
import com.fastasyncworldedit.nukkit.mapping.BiomeMapping;
import com.fastasyncworldedit.nukkit.mapping.BlockMapping;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.platform.PlatformsRegisteredEvent;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extension.platform.Preference;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import com.sk89q.worldedit.world.registry.BlockCategoryRegistry;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.Registries;
import it.unimi.dsi.fastutil.ints.Int2CharOpenHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ResourceLock("NukkitImplLoader")
class NukkitPlatformCompatibilityTest {

    @BeforeAll
    static void setUpWorldEditRegistries() throws Exception {
        BlockRegistry blockRegistry = mock(BlockRegistry.class);
        BlockMaterial airMaterial = mock(BlockMaterial.class, CALLS_REAL_METHODS);
        when(airMaterial.isAir()).thenReturn(true);
        BlockMaterial solidMaterial = mock(BlockMaterial.class, CALLS_REAL_METHODS);
        when(blockRegistry.values()).thenReturn(Arrays.asList(
                "minecraft:__reserved__", "minecraft:air", "minecraft:cave_air", "minecraft:void_air"
        ));
        when(blockRegistry.getProperties(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.emptyMap());
        when(blockRegistry.getMaterial(org.mockito.ArgumentMatchers.any(BlockType.class))).thenAnswer(invocation -> {
            BlockType type = invocation.getArgument(0);
            return type.id().contains("air") || type.id().contains("__reserved__") ? airMaterial : solidMaterial;
        });

        Registries registries = mock(Registries.class);
        BlockCategoryRegistry blockCategoryRegistry = mock(BlockCategoryRegistry.class);
        when(blockCategoryRegistry.getCategorisedByName(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Collections.emptySet());
        when(registries.getBlockRegistry()).thenReturn(blockRegistry);
        when(registries.getBlockCategoryRegistry()).thenReturn(blockCategoryRegistry);

        Platform platform = mock(Platform.class);
        when(platform.getRegistries()).thenReturn(registries);
        Map<Capability, Preference> capabilities = new EnumMap<>(Capability.class);
        for (Capability capability : Capability.values()) {
            capabilities.put(capability, Preference.NORMAL);
        }
        when(platform.getCapabilities()).thenReturn(capabilities);
        when(platform.getConfiguration()).thenReturn(new LocalConfiguration() {
            @Override
            public void load() {
            }
        });

        WorldEdit.getInstance().getPlatformManager().register(platform);
        WorldEdit.getInstance().getPlatformManager().handlePlatformsRegistered(new PlatformsRegisteredEvent());
        try {
            BiomeType.REGISTRY.register("minecraft:plains", new BiomeType("minecraft:plains"));
        } catch (IllegalArgumentException ignored) {
            // Shared static registry may already be initialized by another Nukkit test class.
        }
        setBiomeMappings();

        Int2CharOpenHashMap beToJe = new Int2CharOpenHashMap();
        beToJe.defaultReturnValue(Character.MAX_VALUE);
        beToJe.put(0, (char) BlockTypesCache.ReservedIDs.AIR);
        setStaticField("beFullIdToJeOrdinal", beToJe);
        int[] jeToBe = new int[Math.max(BlockTypesCache.states.length, BlockTypesCache.ReservedIDs.AIR + 1)];
        Arrays.fill(jeToBe, 0);
        setStaticField("jeOrdinalToBeFullId", jeToBe);
    }

    private Level level;
    private BaseFullChunk chunk;

    @BeforeEach
    void setUp() throws Exception {
        level = mock(Level.class);
        chunk = mock(BaseFullChunk.class);
        when(level.getName()).thenReturn("world");
        when(level.getMinBlockY()).thenReturn(0);
        when(level.getMaxBlockY()).thenReturn(319);
        when(level.getChunk(3, 5, true)).thenReturn(chunk);
        setInstance(new TestAdapter("Nukkit-MOT", "Nukkit-MOT 1.0", 6, Set.of()));
    }

    @AfterEach
    void tearDown() throws Exception {
        setInstance(null);
    }

    @Test
    void adapterDetectionSelectsMotWhenGameVersionIsPresentAndNkxOtherwise() throws Exception {
        setInstance(null);
        String expectedAdapter = isMotApiVisible()
                ? "com.fastasyncworldedit.nukkit.adapter.mot.NukkitMOTAdapter"
                : "com.fastasyncworldedit.nukkit.adapter.nkx.NukkitAdapter";

        try {
            NukkitImplAdapter adapter = NukkitImplLoader.detect();

            assertEquals(isMotApiVisible() ? "Nukkit-MOT" : "NKX", adapter.getPlatformName());
        } catch (RuntimeException exception) {
            assertTrue(exception.getMessage().contains(expectedAdapter), exception::getMessage);
        }
    }

    @Test
    void chunkReadsUseCachedChunkAndFallbackToAirWhenMissing() {
        when(chunk.getFullBlock(1, 64, 2)).thenReturn(0);
        NukkitGetBlocks getBlocks = new NukkitGetBlocks(level, 3, 5);

        getBlocks.getBlock(1, 64, 2);
        getBlocks.getBlock(1, 64, 2);

        verify(level).getChunk(3, 5, true);
        verify(chunk, times(2)).getFullBlock(1, 64, 2);

        Level missingLevel = mock(Level.class);
        when(missingLevel.getMinBlockY()).thenReturn(0);
        when(missingLevel.getMaxBlockY()).thenReturn(319);
        when(missingLevel.getChunk(3, 5, true)).thenReturn(null);

        assertNotNull(new NukkitGetBlocks(missingLevel, 3, 5).getBlock(0, 64, 0));
        verify(missingLevel).getChunk(3, 5, true);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void chunkCallAppliesBlocksAndRefreshesPlayers() throws Exception {
        NukkitImplAdapter adapter = mock(NukkitImplAdapter.class);
        when(adapter.getVersion()).thenReturn("Nukkit-MOT 1.0");
        when(adapter.getCapabilities()).thenReturn(Set.of());
        when(adapter.getBlockDataBits()).thenReturn(6);
        when(adapter.getBlockDataMask()).thenReturn(63);
        stubAdapterChunkAccess(adapter);
        setInstance(adapter);

        char[] section = new char[4096];
        Arrays.fill(section, (char) BlockTypesCache.ReservedIDs.__RESERVED__);
        section[(2 << 8) | (3 << 4) | 1] = BlockTypesCache.ReservedIDs.AIR;

        IChunkSet set = mock(IChunkSet.class);
        when(set.getMinSectionPosition()).thenReturn(0);
        when(set.getMaxSectionPosition()).thenReturn(0);
        when(set.hasSection(0)).thenReturn(true);
        when(set.loadIfPresent(0)).thenReturn(section);
        when(set.hasBiomes()).thenReturn(false);
        when(set.tiles()).thenReturn(Collections.emptyMap());
        when(set.getEntityRemoves()).thenReturn(new HashSet<>());
        when(set.entities()).thenReturn(Collections.emptyList());
        Player player = mock(Player.class);
        when(level.getChunkPlayers(3, 5)).thenReturn(Map.of(1L, player));

        NukkitGetBlocks getBlocks = new NukkitGetBlocks(level, 3, 5);
        getBlocks.lockCall();
        Future<?> future;
        try {
            future = getBlocks.call(mock(IQueueExtent.class), set, () -> { });
        } finally {
            getBlocks.unlockCall();
        }

        assertTrue(future.isDone());
        verify(adapter).setFullBlockId(eq(chunk), eq(1), eq(2), eq(3), eq(0), anyInt());
        verify(chunk).setChanged(true);
        verify(level).requestChunk(3, 5, player);
    }

    @ParameterizedTest
    @EnumSource(TreeGenerator.TreeType.class)
    void allTreeTypesReturnFalseWhenAdapterDoesNotPlace(TreeGenerator.TreeType type) throws Exception {
        // TestAdapter.growTree returns false, so generateTree returns false without throwing and
        // without recording history (the EditSession is not touched).
        setInstance(new TestAdapter("Nukkit-MOT", "Nukkit-MOT 1.0", 6, Set.of()));
        NukkitWorld world = new NukkitWorld(level);
        EditSession editSession = mock(EditSession.class);

        boolean result;
        try {
            result = world.generateTree(type, editSession, BlockVector3.at(1, 64, 2));
        } catch (Exception e) {
            throw new AssertionError("generateTree should not throw when placement fails", e);
        }

        assertFalse(result);
        verifyNoInteractions(editSession);
    }

    @Test
    void cuiOperationThrowsWhileChunkSendingIsGracefulNoOp() {
        NukkitWorld world = new NukkitWorld(level);
        ChunkPacket packet = mock(ChunkPacket.class);
        com.sk89q.worldedit.entity.Player worldEditPlayer = mock(com.sk89q.worldedit.entity.Player.class);
        // Fake chunk packets are unsupported but fail silently on Nukkit.
        assertDoesNotThrow(() -> world.sendFakeChunk(worldEditPlayer, packet));

        // CUI dispatch is genuinely unsupported and throws.
        NukkitPlayer player = new NukkitPlayer(mock(Player.class));
        assertThrows(UnsupportedOperationException.class, () -> player.dispatchCUIEvent(null));

        // Platform-level sendChunk is a no-op (chunk resends are driven by the edit flush path).
        NukkitPlatformAdapter adapter = new NukkitPlatformAdapter();
        IChunkGet chunkGet = mock(IChunkGet.class);
        assertDoesNotThrow(() -> adapter.sendChunk(chunkGet, 0xFFFF, true));
    }

    @Test
    void nukkitWorldReports2DBiomeStorage() {
        NukkitWorld world = new NukkitWorld(level);

        assertFalse(world.fullySupports3DBiomes());
    }

    @Test
    void nukkitWorldReports3DBiomeStorageWhenAdapterSupportsIt() throws Exception {
        setInstance(new TestAdapter(
                "Nukkit-MOT",
                "Nukkit-MOT 1.0",
                13,
                Set.of(NukkitPlatformCapabilities.THREE_DIMENSIONAL_BIOMES)
        ));
        NukkitWorld world = new NukkitWorld(level);

        assertTrue(world.fullySupports3DBiomes());
    }

    @Test
    void relighterRemainsNoOpWithoutLevel() {
        // Without a live Level the relighter cannot resolve chunks and stays a safe no-op.
        NukkitRelighter relighter = new NukkitRelighter(null);

        assertFalse(relighter.addChunk(1, 2, new byte[0], 0xFFFF));
        assertTrue(relighter.isEmpty());
    }

    @Test
    void capabilityModelsMatchMotAndNkxSupportMatrix() {
        NukkitImplAdapter mot = new TestAdapter(
                "Nukkit-MOT",
                "Nukkit-MOT 1.0",
                13,
                Set.of(NukkitPlatformCapabilities.THREE_DIMENSIONAL_BIOMES)
        );
        NukkitImplAdapter nkx = new TestAdapter(
                "NKX",
                "NKX 1.0",
                6,
                Set.of(NukkitPlatformCapabilities.THREE_DIMENSIONAL_BIOMES)
        );

        for (NukkitPlatformCapabilities capability : NukkitPlatformCapabilities.values()) {
            boolean expected3DBiomeSupport = capability == NukkitPlatformCapabilities.THREE_DIMENSIONAL_BIOMES;
            assertEquals(expected3DBiomeSupport, mot.supports(capability), capability::name);
            assertEquals(expected3DBiomeSupport, nkx.supports(capability), capability::name);
        }
    }

    @Test
    void platformVersionReportsNonEmptyAdapterVersion() throws Exception {
        setInstance(new TestAdapter("Nukkit-MOT", "Nukkit-MOT 1.0", 13, Set.of()));

        String version = NukkitImplLoader.getPlatformVersion();

        assertNotNull(version);
        assertFalse(version.isEmpty());
    }

    private static boolean isMotApiVisible() {
        try {
            Class.forName("cn.nukkit.GameVersion");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static void setInstance(NukkitImplAdapter adapter) throws Exception {
        setLoaderField("instance", adapter);
        setLoaderField("platformVersion", null);
        setLoaderField("capabilities", null);
    }

    private static void setLoaderField(String name, Object value) throws Exception {
        Field field = NukkitImplLoader.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private void stubAdapterChunkAccess(NukkitImplAdapter adapter) {
        when(adapter.getChunk(level, 3, 5)).thenAnswer(invocation -> level.getChunk(3, 5, true));
        doAnswer(invocation -> {
            boolean changed = invocation.getArgument(1);
            chunk.setChanged(changed);
            return null;
        }).when(adapter).setChunkChanged(eq(chunk), anyBoolean());
    }

    private static void setStaticField(String name, Object value) throws Exception {
        Field field = BlockMapping.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    @SuppressWarnings("unchecked")
    private static void setBiomeMappings() throws Exception {
        Field jeToBeField = BiomeMapping.class.getDeclaredField("JE_TO_BE");
        jeToBeField.setAccessible(true);
        Map<String, Integer> jeToBe = (Map<String, Integer>) jeToBeField.get(null);
        jeToBe.clear();
        jeToBe.put("minecraft:plains", 1);
        jeToBe.put("minecraft:forest", 4);

        Field beToJeField = BiomeMapping.class.getDeclaredField("BE_TO_JE");
        beToJeField.setAccessible(true);
        Map<Integer, String> beToJe = (Map<Integer, String>) beToJeField.get(null);
        beToJe.clear();
        beToJe.put(0, "minecraft:plains");
        beToJe.put(1, "minecraft:plains");
        beToJe.put(4, "minecraft:forest");
    }

    private static final class TestAdapter extends TestNukkitImplAdapter {

        private final String getPlatformName;
        private final String getVersion;
        private final int getBlockDataBits;
        private final Set<NukkitPlatformCapabilities> getCapabilities;

        private TestAdapter(
                String getPlatformName,
                String getVersion,
                int getBlockDataBits,
                Set<NukkitPlatformCapabilities> getCapabilities
        ) {
            this.getPlatformName = getPlatformName;
            this.getVersion = getVersion;
            this.getBlockDataBits = getBlockDataBits;
            this.getCapabilities = getCapabilities;
        }

        @Override
        public String getPlatformName() {
            return getPlatformName;
        }

        @Override
        public String getVersion() {
            return getVersion;
        }

        @Override
        public int getBlockDataBits() {
            return getBlockDataBits;
        }

        @Override
        public Set<NukkitPlatformCapabilities> getCapabilities() {
            return getCapabilities;
        }
    }

}
