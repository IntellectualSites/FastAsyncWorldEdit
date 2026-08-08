package com.fastasyncworldedit.nukkit;

import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.generic.BaseFullChunk;
import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplAdapter;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplLoader;
import com.fastasyncworldedit.nukkit.adapter.NukkitPlatformCapabilities;
import com.fastasyncworldedit.nukkit.adapter.TestNukkitImplAdapter;
import com.fastasyncworldedit.nukkit.blocks.NukkitGetBlocks;
import com.fastasyncworldedit.nukkit.mapping.BiomeMapping;
import com.fastasyncworldedit.nukkit.mapping.BlockMapping;
import com.fastasyncworldedit.nukkit.mapping.ItemMapping;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.platform.PlatformsRegisteredEvent;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extension.platform.Preference;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import com.sk89q.worldedit.world.registry.BlockCategoryRegistry;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.Registries;
import it.unimi.dsi.fastutil.ints.Int2CharOpenHashMap;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ResourceLock("NukkitImplLoader")
class NukkitGetBlocksBehaviorTest {

    @BeforeAll
    static void setUpWorldEditRegistries() throws Exception {
        BlockRegistry blockRegistry = mock(BlockRegistry.class);
        BlockMaterial airMaterial = mock(BlockMaterial.class, CALLS_REAL_METHODS);
        when(airMaterial.isAir()).thenReturn(true);
        BlockMaterial solidMaterial = mock(BlockMaterial.class, CALLS_REAL_METHODS);
        when(blockRegistry.values()).thenReturn(Arrays.asList(
                "minecraft:__reserved__", "minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:stone"
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
        BiomeType.REGISTRY.register("minecraft:plains", new BiomeType("minecraft:plains"));
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
        setInstance(new TestAdapter());
    }

    @AfterEach
    void tearDown() throws Exception {
        setInstance(null);
    }

    @Test
    void canBeInstantiatedWithMockedLevel() {
        NukkitGetBlocks getBlocks = new NukkitGetBlocks(level, 3, 5);

        assertEquals(3, getBlocks.getX());
        assertEquals(5, getBlocks.getZ());
        assertEquals(0, getBlocks.getMinY());
        assertEquals(319, getBlocks.getMaxY());
    }

    @Test
    void getBlockCachesChunkAfterFirstAccess() {
        when(chunk.getFullBlock(1, 64, 2)).thenReturn(0);
        NukkitGetBlocks getBlocks = new NukkitGetBlocks(level, 3, 5);

        getBlocks.getBlock(1, 64, 2);
        getBlocks.getBlock(1, 64, 2);

        verify(level).getChunk(3, 5, true);
        verify(chunk, times(2)).getFullBlock(1, 64, 2);
    }

    @Test
    void biomeAndTileLookupsReuseCachedChunk() {
        NukkitGetBlocks getBlocks = new NukkitGetBlocks(level, 3, 5);

        getBlocks.getBiomeType(2, 70, 4);
        getBlocks.tile(2, 70, 4);

        verify(level).getChunk(3, 5, true);
        verify(chunk).getBiomeId(2, 4);
        verify(chunk).getTile(2, 70, 4);
    }

    @Test
    void lightingLookupsFetchChunkFromLevel() {
        when(chunk.getBlockSkyLight(3, 80, 4)).thenReturn(15);
        when(chunk.getBlockLight(3, 80, 4)).thenReturn(7);
        NukkitGetBlocks getBlocks = new NukkitGetBlocks(level, 3, 5);

        assertEquals(15, getBlocks.getSkyLight(3, 80, 4));
        assertEquals(7, getBlocks.getEmittedLight(3, 80, 4));

        verify(level).getChunk(3, 5, true);
    }

    @Test
    void entityLookupUsesLevelChunkEntitiesWithoutCachingChunk() {
        when(level.getChunkEntities(3, 5)).thenReturn(Collections.emptyMap());
        NukkitGetBlocks getBlocks = new NukkitGetBlocks(level, 3, 5);

        assertTrue(getBlocks.entities().isEmpty());
        assertTrue(getBlocks.getFullEntities().isEmpty());

        verify(level, times(2)).getChunkEntities(3, 5);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void callAppliesSetBlocksToCurrentChunkAndRefreshesPlayers() throws Exception {
        NukkitImplAdapter adapter = mock(NukkitImplAdapter.class);
        when(adapter.getBlockDataBits()).thenReturn(6);
        when(adapter.getBlockDataMask()).thenReturn(63);
        stubAdapterChunkAccess(adapter);
        setInstance(adapter);

        char[] section = new char[4096];
        java.util.Arrays.fill(section, (char) BlockTypesCache.ReservedIDs.__RESERVED__);
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
        when(level.getChunkPlayers(3, 5)).thenReturn(Collections.emptyMap());

        NukkitGetBlocks getBlocks = new NukkitGetBlocks(level, 3, 5);
        Future<?> future = callLocked(getBlocks, set);

        assertTrue(future.isDone());
        verify(level).getChunk(3, 5, true);
        verify(adapter).setFullBlockId(eq(chunk), eq(1), eq(2), eq(3), eq(0), anyInt());
        verify(chunk).setChanged(true);
        verify(level).getChunkPlayers(3, 5);
    }

    @Test
    void getBlockFallsBackToAirWhenChunkIsMissing() {
        when(level.getChunk(3, 5, true)).thenReturn(null);
        NukkitGetBlocks getBlocks = new NukkitGetBlocks(level, 3, 5);

        assertNotNull(getBlocks.getBlock(0, 64, 0));
        verify(level).getChunk(3, 5, true);
        verifyNoMoreInteractions(chunk);
    }

    @Test
    void unmappedNukkitFullIdFallsBackToAir() {
        // 12345 has no JE mapping (beFullIdToJeOrdinal default return is MAX_VALUE). Reads must not
        // throw — otherwise //copy / //paste abort on any unmapped Bedrock block. Instead the block
        // degrades to AIR (__RESERVED__), matching Bukkit's PaperweightGetBlocks behaviour.
        when(chunk.getFullBlock(1, 64, 2)).thenReturn(12345);
        NukkitGetBlocks getBlocks = new NukkitGetBlocks(level, 3, 5);

        BlockState block = getBlocks.getBlock(1, 64, 2);

        assertEquals(
                BlockTypesCache.states[BlockTypesCache.ReservedIDs.__RESERVED__],
                block,
                "Unmapped Nukkit block should degrade to AIR instead of throwing"
        );
    }

    @Test
    void loadUsesAbsoluteWorldYForNegativeMinSectionPosition() {
        // MOT overworld (minY=-64) exposes a bug where NukkitGetBlocks.update treated the
        // normalized array index handed to it by CharBlocks.Section.update as an absolute section
        // position. That shifted every read up by |minSectionPosition|*16 blocks and read
        // out-of-range sections, throwing cn.nukkit.utils.ChunkException "Invalid section N"
        // (seen via HeightmapProcessor -> CharBlocks.load -> update).
        //
        // load(absoluteSection=0) flows to update with normalized index 0 - (-4) = 4.
        // The fix recomputes baseY = (4 + minSectionPosition(-4)) << 4 = 0, so reads target
        // world y=0..15. The buggy baseY = 4 << 4 = 64 would have requested world y=64..79.
        when(level.getMinBlockY()).thenReturn(-64);
        when(level.getMaxBlockY()).thenReturn(319);
        when(chunk.getFullBlock(anyInt(), anyInt(), anyInt())).thenReturn(0);

        NukkitGetBlocks getBlocks = new NukkitGetBlocks(level, 3, 5);
        char[] section = getBlocks.load(0);

        assertNotNull(section);
        // baseY=0 corners must be read, and the buggy baseY=64 must never be requested.
        verify(chunk, atLeastOnce()).getFullBlock(eq(0), eq(0), eq(0));
        verify(chunk, atLeastOnce()).getFullBlock(eq(15), eq(15), eq(15));
        verify(chunk, never()).getFullBlock(anyInt(), eq(64), anyInt());
    }

    @Test
    void heightMapIsComputedFromNukkitBlocks() throws Exception {
        char stoneOrdinal = BlockTypes.STONE.getDefaultState().getOrdinalChar();
        Int2CharOpenHashMap beToJe = new Int2CharOpenHashMap();
        beToJe.defaultReturnValue(Character.MAX_VALUE);
        beToJe.put(0, (char) BlockTypesCache.ReservedIDs.AIR);
        beToJe.put(123, stoneOrdinal);
        setStaticField("beFullIdToJeOrdinal", beToJe);
        when(chunk.getFullBlock(1, 70, 2)).thenReturn(123);

        try {
            NukkitGetBlocks getBlocks = new NukkitGetBlocks(level, 3, 5);
            int[] heightMap = getBlocks.getHeightMap(HeightMapType.WORLD_SURFACE);

            assertEquals(71, heightMap[(2 << 4) | 1]);
        } finally {
            Int2CharOpenHashMap reset = new Int2CharOpenHashMap();
            reset.defaultReturnValue(Character.MAX_VALUE);
            reset.put(0, (char) BlockTypesCache.ReservedIDs.AIR);
            setStaticField("beFullIdToJeOrdinal", reset);
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void callWithoutLockThrowsExplicitError() {
        NukkitGetBlocks getBlocks = new NukkitGetBlocks(level, 3, 5);
        IChunkSet set = mock(IChunkSet.class);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> getBlocks.call(mock(IQueueExtent.class), set, () -> { })
        );

        assertEquals("Attempted to call chunk GET but chunk was not call-locked.", exception.getMessage());
    }

    @Test
    void tileWithoutIdFailsBeforeApplyingBlocks() throws Exception {
        NukkitImplAdapter adapter = mock(NukkitImplAdapter.class);
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
        when(set.tiles()).thenReturn(Map.of(
                BlockVector3.at(1, 66, 3),
                eagerFaweTag(LinCompoundTag.builder().build())
        ));
        when(set.getEntityRemoves()).thenReturn(new HashSet<>());
        when(set.entities()).thenReturn(Collections.emptyList());

        NukkitGetBlocks getBlocks = new NukkitGetBlocks(level, 3, 5);
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> callLocked(getBlocks, set)
        );

        assertTrue(exception.getMessage().contains("does not contain an id"), exception::getMessage);
        verify(adapter, never()).setFullBlockId(eq(chunk), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(chunk, never()).setChanged(true);
    }

    @Test
    void conflicting3DBiomesFailBeforeApplyingBlocks() throws Exception {
        NukkitImplAdapter adapter = mock(NukkitImplAdapter.class);
        when(adapter.getBlockDataBits()).thenReturn(6);
        when(adapter.getBlockDataMask()).thenReturn(63);
        stubAdapterChunkAccess(adapter);
        setInstance(adapter);

        char[] section = new char[4096];
        Arrays.fill(section, (char) BlockTypesCache.ReservedIDs.__RESERVED__);
        section[(2 << 8) | (3 << 4) | 1] = BlockTypesCache.ReservedIDs.AIR;

        BiomeType plains = new BiomeType("minecraft:plains");
        BiomeType forest = new BiomeType("minecraft:forest");
        IChunkSet set = mock(IChunkSet.class);
        when(set.getMinSectionPosition()).thenReturn(0);
        when(set.getMaxSectionPosition()).thenReturn(0);
        when(set.hasSection(0)).thenReturn(true);
        when(set.loadIfPresent(0)).thenReturn(section);
        when(set.hasBiomes()).thenReturn(true);
        when(set.hasBiomes(0)).thenReturn(true);
        when(set.getBiomeType(1, 0, 2)).thenReturn(plains);
        when(set.getBiomeType(1, 4, 2)).thenReturn(forest);
        when(set.tiles()).thenReturn(Collections.emptyMap());
        when(set.getEntityRemoves()).thenReturn(new HashSet<>());
        when(set.entities()).thenReturn(Collections.emptyList());

        NukkitGetBlocks getBlocks = new NukkitGetBlocks(level, 3, 5);
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> callLocked(getBlocks, set)
        );

        assertTrue(exception.getMessage().contains("2D biome columns"), exception::getMessage);
        verify(adapter, never()).setFullBlockId(eq(chunk), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(chunk, never()).setBiomeId(anyInt(), anyInt(), anyByte());
        verify(chunk, never()).setChanged(true);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void threeDimensionalBiomeCapabilityAppliesVerticalBiomeUpdates() throws Exception {
        NukkitImplAdapter adapter = mock(NukkitImplAdapter.class);
        when(adapter.getBlockDataBits()).thenReturn(6);
        when(adapter.getBlockDataMask()).thenReturn(63);
        when(adapter.supports(NukkitPlatformCapabilities.THREE_DIMENSIONAL_BIOMES)).thenReturn(true);
        stubAdapterChunkAccess(adapter);
        setInstance(adapter);

        BiomeType plains = new BiomeType("minecraft:plains");
        BiomeType forest = new BiomeType("minecraft:forest");
        IChunkSet set = mock(IChunkSet.class);
        when(set.getMinSectionPosition()).thenReturn(0);
        when(set.getMaxSectionPosition()).thenReturn(0);
        when(set.hasBiomes()).thenReturn(true);
        when(set.hasBiomes(0)).thenReturn(true);
        when(set.getBiomeType(1, 0, 2)).thenReturn(plains);
        when(set.getBiomeType(1, 4, 2)).thenReturn(forest);
        when(set.tiles()).thenReturn(Collections.emptyMap());
        when(set.getEntityRemoves()).thenReturn(new HashSet<>());
        when(set.entities()).thenReturn(Collections.emptyList());
        when(level.getChunkPlayers(3, 5)).thenReturn(Collections.emptyMap());

        NukkitGetBlocks getBlocks = new NukkitGetBlocks(level, 3, 5);
        Future<?> future = callLocked(getBlocks, set);

        assertTrue(future.isDone());
        verify(adapter).setChunkBiomeId(eq(chunk), eq(1), eq(0), eq(2), eq(1));
        verify(adapter).setChunkBiomeId(eq(chunk), eq(1), eq(4), eq(2), eq(4));
        verify(adapter, never()).setFullBlockId(eq(chunk), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(chunk).setChanged(true);
    }

    @Test
    void unmappedJavaOrdinalThrowsInsteadOfFallingBackToAir() throws Exception {
        int[] jeToBe = new int[Math.max(BlockTypesCache.states.length, BlockTypesCache.ReservedIDs.AIR + 1)];
        Arrays.fill(jeToBe, 0);
        jeToBe[BlockTypesCache.ReservedIDs.AIR] = -1;
        setStaticField("jeOrdinalToBeFullId", jeToBe);

        try {
            UnsupportedOperationException exception = assertThrows(
                    UnsupportedOperationException.class,
                    () -> BlockMapping.jeOrdinalToFullId((char) BlockTypesCache.ReservedIDs.AIR)
            );

            assertTrue(exception.getMessage().contains("No Nukkit block mapping"), exception::getMessage);
        } finally {
            Arrays.fill(jeToBe, 0);
            setStaticField("jeOrdinalToBeFullId", jeToBe);
        }
    }

    @Test
    void unmappedItemMappingsThrowInsteadOfFallingBackToAir() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> ItemMapping.jeToBe("minecraft:not_a_real_item")
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> ItemMapping.beToJe(-12345, 0)
        );
    }

    @Test
    void airItemShortCircuitsLookup() {
        // Holding nothing must resolve to minecraft:air without entering the lookup table,
        // which never contains air because createItemData rejects null items.
        assertEquals("minecraft:air", ItemMapping.beToJe(Item.get(Item.AIR)));
    }

    @SuppressWarnings("unchecked")
    @Test
    void beToJeSurvivesUnstableItemKeyLookup() throws Exception {
        // A live item whose platform key lookup blows up (e.g. MOT getNamespaceId over
        // runtime-unregistered items) must surface as the intended UnsupportedOperationException,
        // never a raw NPE that masks the unmapped-item diagnostic.
        NukkitImplAdapter previous = NukkitImplLoader.get();
        Field beToJeField = ItemMapping.class.getDeclaredField("BE_TO_JE");
        beToJeField.setAccessible(true);
        Map<String, String> beToJe = (Map<String, String>) beToJeField.get(null);
        Map<String, String> saved = new java.util.HashMap<>(beToJe);
        try {
            beToJe.clear();
            NukkitImplAdapter unstableKeyAdapter = new TestAdapter() {
                @Override
                public String getItemMappingKey(Item item) {
                    throw new RuntimeException("simulated unstable lookup");
                }
            };
            setInstance(unstableKeyAdapter);
            // A non-air item with a non-empty count so isAirItem is false, forcing the key lookup.
            Item stone = Item.get(1, 0, 1);
            var ex = assertThrows(
                    UnsupportedOperationException.class,
                    () -> ItemMapping.beToJe(stone)
            );
            assertTrue(ex.getMessage().contains("No Java item mapping"), ex::getMessage);
        } finally {
            beToJe.clear();
            beToJe.putAll(saved);
            setInstance(previous);
        }
    }

    @Test
    void unmappedBiomeMappingsThrowInsteadOfFallingBackToPlains() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> BiomeMapping.jeToBe("minecraft:not_a_real_biome")
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> BiomeMapping.beToJe(-12345)
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Future<?> callLocked(NukkitGetBlocks getBlocks, IChunkSet set) {
        getBlocks.lockCall();
        try {
            return getBlocks.call(mock(IQueueExtent.class), set, () -> { });
        } finally {
            getBlocks.unlockCall();
        }
    }

    private static FaweCompoundTag eagerFaweTag(LinCompoundTag tag) throws ReflectiveOperationException {
        Class<?> type = Class.forName("com.fastasyncworldedit.core.nbt.EagerFaweCompoundTag");
        java.lang.reflect.Constructor<?> constructor = type.getDeclaredConstructor(LinCompoundTag.class);
        constructor.setAccessible(true);
        return (FaweCompoundTag) constructor.newInstance(tag);
    }

    private static void setInstance(NukkitImplAdapter adapter) throws Exception {
        Field field = NukkitImplLoader.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, adapter);
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

    private static class TestAdapter extends TestNukkitImplAdapter {

        @Override
        public String getPlatformName() {
            return "Test";
        }

        @Override
        public int getBlockDataBits() {
            return 6;
        }
    }
}
