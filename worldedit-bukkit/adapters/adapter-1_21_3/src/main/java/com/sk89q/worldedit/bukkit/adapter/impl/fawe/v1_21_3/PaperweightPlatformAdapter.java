package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_21_3;

import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkHolderManager;
import com.fastasyncworldedit.bukkit.adapter.CachedBukkitAdapter;
import com.fastasyncworldedit.bukkit.adapter.DelegateSemaphore;
import com.fastasyncworldedit.bukkit.adapter.NMSAdapter;
import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.math.BitArrayUnstretched;
import com.fastasyncworldedit.core.math.IntPair;
import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.bukkit.adapter.Refraction;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import io.papermc.lib.PaperLib;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.BitStorage;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.ThreadingDetector;
import net.minecraft.util.Unit;
import net.minecraft.util.ZeroBitStorage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.GlobalPalette;
import net.minecraft.world.level.chunk.HashMapPalette;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LinearPalette;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.SingleValuePalette;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftChunk;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntFunction;

import static java.lang.invoke.MethodType.methodType;
import static net.minecraft.core.registries.Registries.BIOME;

public final class PaperweightPlatformAdapter extends NMSAdapter {

    public static final Field fieldData;

    public static final Constructor<?> dataConstructor;

    public static final Field fieldStorage;
    public static final Field fieldPalette;

    private static final Field fieldTickingFluidCount;
    private static final Field fieldTickingBlockCount;
    private static final Field fieldBiomes;

    private static final MethodHandle methodGetVisibleChunk;

    private static final Field fieldThreadingDetector;
    private static final Field fieldLock;

    private static final MethodHandle methodRemoveGameEventListener;
    private static final MethodHandle methodremoveTickingBlockEntity;

    /*
     * This is a workaround for the changes from https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/commits/1fddefce1cdce44010927b888432bf70c0e88cde#src/main/java/org/bukkit/craftbukkit/CraftChunk.java
     * and is only needed to support 1.19.4 versions before *and* after this change.
     */
    private static final MethodHandle CRAFT_CHUNK_GET_HANDLE;

    private static final Field fieldRemove;

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private static Method PAPER_CHUNK_GEN_ALL_ENTITIES;
    private static Field SERVER_LEVEL_ENTITY_MANAGER;

    static final MethodHandle PALETTED_CONTAINER_GET;

    static {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            fieldData = PalettedContainer.class.getDeclaredField(Refraction.pickName("data", "d"));
            fieldData.setAccessible(true);

            Class<?> dataClazz = fieldData.getType();
            dataConstructor = dataClazz.getDeclaredConstructors()[0];
            dataConstructor.setAccessible(true);

            fieldStorage = dataClazz.getDeclaredField(Refraction.pickName("storage", "b"));
            fieldStorage.setAccessible(true);
            fieldPalette = dataClazz.getDeclaredField(Refraction.pickName("palette", "c"));
            fieldPalette.setAccessible(true);

            fieldTickingFluidCount = LevelChunkSection.class.getDeclaredField(Refraction.pickName("tickingFluidCount", "g"));
            fieldTickingFluidCount.setAccessible(true);
            fieldTickingBlockCount = LevelChunkSection.class.getDeclaredField(Refraction.pickName("tickingBlockCount", "f"));
            fieldTickingBlockCount.setAccessible(true);
            Field tmpFieldBiomes;
            try {
                // Seems it's sometimes biomes and sometimes "i". Idk this is just easier than having to try to deal with it
                tmpFieldBiomes = LevelChunkSection.class.getDeclaredField("biomes"); // apparently unobf
            } catch (NoSuchFieldException ignored) {
                tmpFieldBiomes = LevelChunkSection.class.getDeclaredField("i"); // apparently obf
            }
            fieldBiomes = tmpFieldBiomes;
            fieldBiomes.setAccessible(true);

            Method getVisibleChunkIfPresent = ChunkMap.class.getDeclaredMethod(Refraction.pickName(
                    "getVisibleChunkIfPresent",
                    "b"
            ), long.class);
            getVisibleChunkIfPresent.setAccessible(true);
            methodGetVisibleChunk = lookup.unreflect(getVisibleChunkIfPresent);

            if (!PaperLib.isPaper()) {
                fieldThreadingDetector = PalettedContainer.class.getDeclaredField(Refraction.pickName("threadingDetector", "f"));
                fieldThreadingDetector.setAccessible(true);
                fieldLock = ThreadingDetector.class.getDeclaredField(Refraction.pickName("lock", "c"));
                fieldLock.setAccessible(true);
            } else {
                // in paper, the used methods are synchronized properly
                fieldThreadingDetector = null;
                fieldLock = null;
            }

            Method removeGameEventListener = LevelChunk.class.getDeclaredMethod(
                    Refraction.pickName("removeGameEventListener", "a"),
                    BlockEntity.class,
                    ServerLevel.class
            );
            removeGameEventListener.setAccessible(true);
            methodRemoveGameEventListener = lookup.unreflect(removeGameEventListener);

            Method removeBlockEntityTicker = LevelChunk.class.getDeclaredMethod(
                    Refraction.pickName(
                            "removeBlockEntityTicker",
                            "k"
                    ), BlockPos.class
            );
            removeBlockEntityTicker.setAccessible(true);
            methodremoveTickingBlockEntity = lookup.unreflect(removeBlockEntityTicker);

            fieldRemove = BlockEntity.class.getDeclaredField(Refraction.pickName("remove", "p"));
            fieldRemove.setAccessible(true);

            try {
                Level.class.getDeclaredMethod("moonrise$getEntityLookup");
                PAPER_CHUNK_GEN_ALL_ENTITIES = ChunkEntitySlices.class.getDeclaredMethod("getAllEntities");
                PAPER_CHUNK_GEN_ALL_ENTITIES.setAccessible(true);
            } catch (NoSuchMethodException ignored) {
                // Non-Paper
                SERVER_LEVEL_ENTITY_MANAGER = ServerLevel.class.getDeclaredField(Refraction.pickName("entityManager", "N"));
                SERVER_LEVEL_ENTITY_MANAGER.setAccessible(true);
            }

            Method palettedContaienrGet = PalettedContainer.class.getDeclaredMethod(
                    Refraction.pickName("get", "a"),
                    int.class
            );
            palettedContaienrGet.setAccessible(true);
            PALETTED_CONTAINER_GET = lookup.unreflect(palettedContaienrGet);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        MethodHandle craftChunkGetHandle;
        final MethodType type = methodType(LevelChunk.class);
        try {
            craftChunkGetHandle = lookup.findVirtual(CraftChunk.class, "getHandle", type);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            try {
                final MethodType newType = methodType(ChunkAccess.class, ChunkStatus.class);
                craftChunkGetHandle = lookup.findVirtual(CraftChunk.class, "getHandle", newType);
                craftChunkGetHandle = MethodHandles.insertArguments(craftChunkGetHandle, 1, ChunkStatus.FULL);
            } catch (NoSuchMethodException | IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
        CRAFT_CHUNK_GET_HANDLE = craftChunkGetHandle;
    }

    static boolean setSectionAtomic(
            String worldName,
            IntPair pair,
            LevelChunkSection[] sections,
            LevelChunkSection expected,
            LevelChunkSection value,
            int layer
    ) {
        return NMSAdapter.setSectionAtomic(worldName, pair, sections, expected, value, layer);
    }

    // There is no point in having a functional semaphore for paper servers.
    private static final ThreadLocal<DelegateSemaphore> SEMAPHORE_THREAD_LOCAL =
            ThreadLocal.withInitial(() -> new DelegateSemaphore(1, null));

    static DelegateSemaphore applyLock(LevelChunkSection section) {
        if (PaperLib.isPaper()) {
            return SEMAPHORE_THREAD_LOCAL.get();
        }
        try {
            synchronized (section) {
                PalettedContainer<net.minecraft.world.level.block.state.BlockState> blocks = section.getStates();
                ThreadingDetector currentThreadingDetector = (ThreadingDetector) fieldThreadingDetector.get(blocks);
                synchronized (currentThreadingDetector) {
                    Semaphore currentLock = (Semaphore) fieldLock.get(currentThreadingDetector);
                    if (currentLock instanceof DelegateSemaphore delegateSemaphore) {
                        return delegateSemaphore;
                    }
                    DelegateSemaphore newLock = new DelegateSemaphore(1, currentLock);
                    fieldLock.set(currentThreadingDetector, newLock);
                    return newLock;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static CompletableFuture<LevelChunk> ensureLoaded(ServerLevel serverLevel, int chunkX, int chunkZ) {
        LevelChunk levelChunk = getChunkImmediatelyAsync(serverLevel, chunkX, chunkZ);
        if (levelChunk != null) {
            return CompletableFuture.completedFuture(levelChunk);
        }
        if (PaperLib.isPaper()) {
            CompletableFuture<LevelChunk> future = serverLevel
                    .getWorld()
                    .getChunkAtAsync(chunkX, chunkZ, true, true)
                    .thenApply(chunk -> {
                        addTicket(serverLevel, chunkX, chunkZ);
                        try {
                            return (LevelChunk) CRAFT_CHUNK_GET_HANDLE.invoke(chunk);
                        } catch (Throwable e) {
                            LOGGER.error("Could not asynchronously load chunk at {},{}", chunkX, chunkZ, e);
                            return null;
                        }
                    });
            try {
                if (!future.isCompletedExceptionally() || (future.isDone() && future.get() != null)) {
                    return future;
                }
                Throwable t = future.exceptionNow();
                LOGGER.error("Asynchronous chunk load at {},{} exceptionally completed immediately", chunkX, chunkZ, t);
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error(
                        "Unexpected error when getting completed future at chunk {},{}. Returning to default.",
                        chunkX,
                        chunkZ,
                        e
                );
            }
        }
        return CompletableFuture.supplyAsync(() -> TaskManager.taskManager().sync(() -> serverLevel.getChunk(chunkX, chunkZ)));
    }

    public static @Nullable LevelChunk getChunkImmediatelyAsync(ServerLevel serverLevel, int chunkX, int chunkZ) {
        if (!PaperLib.isPaper()) {
            LevelChunk nmsChunk = serverLevel.getChunkSource().getChunk(chunkX, chunkZ, false);
            if (nmsChunk != null) {
                return nmsChunk;
            }
            if (Fawe.isMainThread()) {
                return serverLevel.getChunk(chunkX, chunkZ);
            }
            return null;
        } else {
            LevelChunk nmsChunk = serverLevel.getChunkSource().getChunkAtIfCachedImmediately(chunkX, chunkZ);
            if (nmsChunk != null) {
                addTicket(serverLevel, chunkX, chunkZ);
                return nmsChunk;
            }
            nmsChunk = serverLevel.getChunkSource().getChunkAtIfLoadedImmediately(chunkX, chunkZ);
            if (nmsChunk != null) {
                addTicket(serverLevel, chunkX, chunkZ);
                return nmsChunk;
            }
            // Avoid "async" methods from the main thread.
            if (Fawe.isMainThread()) {
                return serverLevel.getChunk(chunkX, chunkZ);
            }
            return null;
        }
    }

    private static void addTicket(ServerLevel serverLevel, int chunkX, int chunkZ) {
        // Ensure chunk is definitely loaded before applying a ticket
        io.papermc.paper.util.MCUtil.MAIN_EXECUTOR.execute(() -> serverLevel
                .getChunkSource()
                .addRegionTicket(ChunkHolderManager.UNLOAD_COOLDOWN, new ChunkPos(chunkX, chunkZ), 0, Unit.INSTANCE));
    }

    public static ChunkHolder getPlayerChunk(ServerLevel nmsWorld, final int chunkX, final int chunkZ) {
        ChunkMap chunkMap = nmsWorld.getChunkSource().chunkMap;
        try {
            return (ChunkHolder) methodGetVisibleChunk.invoke(chunkMap, ChunkPos.asLong(chunkX, chunkZ));
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    @SuppressWarnings("deprecation")
    public static void sendChunk(IntPair pair, ServerLevel nmsWorld, int chunkX, int chunkZ) {
        ChunkHolder chunkHolder = getPlayerChunk(nmsWorld, chunkX, chunkZ);
        if (chunkHolder == null) {
            return;
        }
        LevelChunk levelChunk;
        if (PaperLib.isPaper()) {
            // getChunkAtIfLoadedImmediately is paper only
            levelChunk = nmsWorld.getChunkSource().getChunkAtIfLoadedImmediately(chunkX, chunkZ);
        } else {
            levelChunk = chunkHolder.getTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).orElse(null);
        }
        if (levelChunk == null) {
            return;
        }
        StampLockHolder lockHolder = new StampLockHolder();
        NMSAdapter.beginChunkPacketSend(nmsWorld.getWorld().getName(), pair, lockHolder);
        if (lockHolder.chunkLock == null) {
            return;
        }
        MinecraftServer.getServer().execute(() -> {
            try {
                ChunkPos pos = levelChunk.getPos();
                // NOTE: the ClientboundForgetLevelChunkPacket packet is required on 1.21.3
                // as the client won't update empty -> non-empty sections properly otherwise
                ClientboundForgetLevelChunkPacket forget = new ClientboundForgetLevelChunkPacket(pos);
                ClientboundLevelChunkWithLightPacket packet;
                if (PaperLib.isPaper()) {
                    packet = new ClientboundLevelChunkWithLightPacket(
                            levelChunk,
                            nmsWorld.getChunkSource().getLightEngine(),
                            null,
                            null,
                            false // last false is to not bother with x-ray
                    );
                } else {
                    // deprecated on paper - deprecation suppressed
                    packet = new ClientboundLevelChunkWithLightPacket(
                            levelChunk,
                            nmsWorld.getChunkSource().getLightEngine(),
                            null,
                            null
                    );
                }
                nearbyPlayers(nmsWorld, pos).forEach(p -> {
                    p.connection.send(forget);
                    p.connection.send(packet);
                });
            } finally {
                NMSAdapter.endChunkPacketSend(nmsWorld.getWorld().getName(), pair, lockHolder);
            }
        });
    }

    private static List<ServerPlayer> nearbyPlayers(ServerLevel serverLevel, ChunkPos coordIntPair) {
        return serverLevel.getChunkSource().chunkMap.getPlayers(coordIntPair, false);
    }

    /*
    NMS conversion
     */
    public static LevelChunkSection newChunkSection(
            final int layer,
            final char[] blocks,
            CachedBukkitAdapter adapter,
            Registry<Biome> biomeRegistry,
            @Nullable PalettedContainer<Holder<Biome>> biomes
    ) {
        return newChunkSection(layer, null, blocks, adapter, biomeRegistry, biomes);
    }

    public static LevelChunkSection newChunkSection(
            final int layer,
            final IntFunction<char[]> get,
            char[] set,
            CachedBukkitAdapter adapter,
            Registry<Biome> biomeRegistry,
            @Nullable PalettedContainer<Holder<Biome>> biomes
    ) {
        if (set == null) {
            return newChunkSection(biomeRegistry, biomes);
        }
        final int[] blockToPalette = FaweCache.INSTANCE.BLOCK_TO_PALETTE.get();
        final int[] paletteToBlock = FaweCache.INSTANCE.PALETTE_TO_BLOCK.get();
        final long[] blockStates = FaweCache.INSTANCE.BLOCK_STATES.get();
        final int[] blocksCopy = FaweCache.INSTANCE.SECTION_BLOCKS.get();
        try {
            int num_palette;
            if (get == null) {
                num_palette = createPalette(blockToPalette, paletteToBlock, blocksCopy, set, adapter);
            } else {
                num_palette = createPalette(layer, blockToPalette, paletteToBlock, blocksCopy, get, set, adapter);
            }

            int bitsPerEntry = MathMan.log2nlz(num_palette - 1);
            if (bitsPerEntry > 0 && bitsPerEntry < 5) {
                bitsPerEntry = 4;
            } else if (bitsPerEntry > 8) {
                bitsPerEntry = MathMan.log2nlz(Block.BLOCK_STATE_REGISTRY.size() - 1);
            }

            int bitsPerEntryNonZero = Math.max(bitsPerEntry, 1); // We do want to use zero sometimes
            final int blocksPerLong = MathMan.floorZero((double) 64 / bitsPerEntryNonZero);
            final int blockBitArrayEnd = MathMan.ceilZero((float) 4096 / blocksPerLong);

            if (num_palette == 1) {
                for (int i = 0; i < blockBitArrayEnd; i++) {
                    blockStates[i] = 0;
                }
            } else {
                final BitArrayUnstretched bitArray = new BitArrayUnstretched(bitsPerEntryNonZero, 4096, blockStates);
                bitArray.fromRaw(blocksCopy);
            }

            final long[] bits = Arrays.copyOfRange(blockStates, 0, blockBitArrayEnd);
            final BitStorage nmsBits;
            if (bitsPerEntry == 0) {
                nmsBits = new ZeroBitStorage(4096);
            } else {
                nmsBits = new SimpleBitStorage(bitsPerEntry, 4096, bits);
            }
            List<net.minecraft.world.level.block.state.BlockState> palette;
            if (bitsPerEntry < 9) {
                palette = new ArrayList<>();
                for (int i = 0; i < num_palette; i++) {
                    int ordinal = paletteToBlock[i];
                    blockToPalette[ordinal] = Integer.MAX_VALUE;
                    final BlockState state = BlockTypesCache.states[ordinal];
                    palette.add(((PaperweightBlockMaterial) state.getMaterial()).getState());
                }
            } else {
                palette = List.of();
            }

            // Create palette with data
            @SuppressWarnings("deprecation") // constructor is deprecated on paper, but needed to keep compatibility with spigot
            final PalettedContainer<net.minecraft.world.level.block.state.BlockState> blockStatePalettedContainer =
                    new PalettedContainer<>(
                            Block.BLOCK_STATE_REGISTRY,
                            PalettedContainer.Strategy.SECTION_STATES,
                            PalettedContainer.Strategy.SECTION_STATES.getConfiguration(Block.BLOCK_STATE_REGISTRY, bitsPerEntry),
                            nmsBits,
                            palette
                    );
            if (biomes == null) {
                IdMap<Holder<Biome>> biomeHolderIdMap = biomeRegistry.asHolderIdMap();
                biomes = new PalettedContainer<>(
                        biomeHolderIdMap,
                        biomeHolderIdMap.byIdOrThrow(WorldEditPlugin
                                .getInstance()
                                .getBukkitImplAdapter()
                                .getInternalBiomeId(
                                        BiomeTypes.PLAINS)),
                        PalettedContainer.Strategy.SECTION_BIOMES
                );
            }

            return new LevelChunkSection(blockStatePalettedContainer, biomes);
        } catch (final Throwable e) {
            throw e;
        } finally {
            Arrays.fill(blockToPalette, Integer.MAX_VALUE);
            Arrays.fill(paletteToBlock, Integer.MAX_VALUE);
            Arrays.fill(blockStates, 0);
            Arrays.fill(blocksCopy, 0);
        }
    }

    @SuppressWarnings("deprecation") // Only deprecated in paper
    private static LevelChunkSection newChunkSection(
            Registry<Biome> biomeRegistry,
            @Nullable PalettedContainer<Holder<Biome>> biomes
    ) {
        if (biomes == null) {
            return new LevelChunkSection(biomeRegistry);
        }
        PalettedContainer<net.minecraft.world.level.block.state.BlockState> dataPaletteBlocks = new PalettedContainer<>(
                Block.BLOCK_STATE_REGISTRY,
                Blocks.AIR.defaultBlockState(),
                PalettedContainer.Strategy.SECTION_STATES
        );
        return new LevelChunkSection(dataPaletteBlocks, biomes);
    }

    public static void setBiomesToChunkSection(LevelChunkSection section, PalettedContainer<Holder<Biome>> biomes) {
        try {
            fieldBiomes.set(section, biomes);
        } catch (IllegalAccessException e) {
            LOGGER.error("Could not set biomes to chunk section", e);
        }
    }

    /**
     * Create a new {@link PalettedContainer<Biome>}. Should only be used if no biome container existed beforehand.
     */
    public static PalettedContainer<Holder<Biome>> getBiomePalettedContainer(
            BiomeType[] biomes,
            IdMap<Holder<Biome>> biomeRegistry
    ) {
        if (biomes == null) {
            return null;
        }
        BukkitImplAdapter<?> adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        // Don't stream this as typically will see 1-4 biomes; stream overhead is large for the small length
        Map<BiomeType, Holder<Biome>> palette = new HashMap<>();
        for (BiomeType biomeType : new LinkedList<>(Arrays.asList(biomes))) {
            Holder<Biome> biome;
            if (biomeType == null) {
                biome = biomeRegistry.byId(adapter.getInternalBiomeId(BiomeTypes.PLAINS));
            } else {
                biome = biomeRegistry.byId(adapter.getInternalBiomeId(biomeType));
            }
            palette.put(biomeType, biome);
        }
        int biomeCount = palette.size();
        int bitsPerEntry = MathMan.log2nlz(biomeCount - 1);
        Object configuration = PalettedContainer.Strategy.SECTION_STATES.getConfiguration(
                new FakeIdMapBiome(biomeCount),
                bitsPerEntry
        );
        if (bitsPerEntry > 3) {
            bitsPerEntry = MathMan.log2nlz(biomeRegistry.size() - 1);
        }
        PalettedContainer<Holder<Biome>> biomePalettedContainer = new PalettedContainer<>(
                biomeRegistry,
                biomeRegistry.byIdOrThrow(adapter.getInternalBiomeId(BiomeTypes.PLAINS)),
                PalettedContainer.Strategy.SECTION_BIOMES
        );

        final Palette<Holder<Biome>> biomePalette;
        if (bitsPerEntry == 0) {
            biomePalette = new SingleValuePalette<>(
                    biomePalettedContainer.registry,
                    biomePalettedContainer,
                    new ArrayList<>(palette.values()) // Must be modifiable
            );
        } else if (bitsPerEntry == 4) {
            biomePalette = LinearPalette.create(
                    4,
                    biomePalettedContainer.registry,
                    biomePalettedContainer,
                    new ArrayList<>(palette.values()) // Must be modifiable
            );
        } else if (bitsPerEntry < 9) {
            biomePalette = HashMapPalette.create(
                    bitsPerEntry,
                    biomePalettedContainer.registry,
                    biomePalettedContainer,
                    new ArrayList<>(palette.values()) // Must be modifiable
            );
        } else {
            biomePalette = GlobalPalette.create(
                    bitsPerEntry,
                    biomePalettedContainer.registry,
                    biomePalettedContainer,
                    null // unused
            );
        }

        int bitsPerEntryNonZero = Math.max(bitsPerEntry, 1); // We do want to use zero sometimes
        final int blocksPerLong = MathMan.floorZero((double) 64 / bitsPerEntryNonZero);
        final int arrayLength = MathMan.ceilZero(64f / blocksPerLong);


        BitStorage bitStorage = bitsPerEntry == 0 ? new ZeroBitStorage(64) : new SimpleBitStorage(
                bitsPerEntry,
                64,
                new long[arrayLength]
        );

        try {
            Object data = dataConstructor.newInstance(configuration, bitStorage, biomePalette);
            fieldData.set(biomePalettedContainer, data);
            int index = 0;
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 4; z++) {
                    for (int x = 0; x < 4; x++, index++) {
                        BiomeType biomeType = biomes[index];
                        if (biomeType == null) {
                            continue;
                        }
                        Holder<Biome> biome = biomeRegistry.byId(WorldEditPlugin
                                .getInstance()
                                .getBukkitImplAdapter()
                                .getInternalBiomeId(biomeType));
                        if (biome == null) {
                            continue;
                        }
                        biomePalettedContainer.set(x, y, z, biome);
                    }
                }
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return biomePalettedContainer;
    }

    public static void clearCounts(final LevelChunkSection section) throws IllegalAccessException {
        fieldTickingFluidCount.setShort(section, (short) 0);
        fieldTickingBlockCount.setShort(section, (short) 0);
    }

    public static BiomeType adapt(Holder<Biome> biome, LevelAccessor levelAccessor) {
        final Registry<Biome> biomeRegistry = levelAccessor.registryAccess().lookupOrThrow(BIOME);
        if (biomeRegistry.getKey(biome.value()) == null) {
            return biomeRegistry.asHolderIdMap().getId(biome) == -1 ? BiomeTypes.OCEAN
                    : null;
        }
        return BiomeTypes.get(biome.unwrapKey().orElseThrow().location().toString());
    }

    static void removeBeacon(BlockEntity beacon, LevelChunk levelChunk) {
        try {
            if (levelChunk.loaded || levelChunk.level.isClientSide()) {
                BlockEntity blockEntity = levelChunk.blockEntities.remove(beacon.getBlockPos());
                if (blockEntity != null) {
                    if (!levelChunk.level.isClientSide) {
                        methodRemoveGameEventListener.invoke(levelChunk, beacon, levelChunk.level);
                    }
                    fieldRemove.set(beacon, true);
                }
            }
            methodremoveTickingBlockEntity.invoke(levelChunk, beacon.getBlockPos());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    static List<Entity> getEntities(LevelChunk chunk) {
        if (PaperLib.isPaper()) {
            try {
                //noinspection unchecked
                return (List<Entity>) PAPER_CHUNK_GEN_ALL_ENTITIES.invoke(chunk.level
                        .moonrise$getEntityLookup()
                        .getChunk(chunk.locX, chunk.locZ));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to lookup entities [PAPER=true]", e);
            }
        }
        try {
            //noinspection unchecked
            return ((PersistentEntitySectionManager<Entity>) (SERVER_LEVEL_ENTITY_MANAGER.get(chunk.level))).getEntities(chunk.getPos());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to lookup entities [PAPER=false]", e);
        }
    }

    record FakeIdMapBlock(int size) implements IdMap<net.minecraft.world.level.block.state.BlockState> {

        @Override
        public int getId(final net.minecraft.world.level.block.state.BlockState entry) {
            return 0;
        }

        @Nullable
        @Override
        public net.minecraft.world.level.block.state.BlockState byId(final int index) {
            return null;
        }

        @Nonnull
        @Override
        public Iterator<net.minecraft.world.level.block.state.BlockState> iterator() {
            return Collections.emptyIterator();
        }

    }

    record FakeIdMapBiome(int size) implements IdMap<Biome> {

        @Override
        public int getId(final Biome entry) {
            return 0;
        }

        @Nullable
        @Override
        public Biome byId(final int index) {
            return null;
        }

        @Nonnull
        @Override
        public Iterator<Biome> iterator() {
            return Collections.emptyIterator();
        }

    }

}
