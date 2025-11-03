package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_21_9;

import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkHolderManager;
import com.fastasyncworldedit.bukkit.adapter.CachedBukkitAdapter;
import com.fastasyncworldedit.bukkit.adapter.DelegateSemaphore;
import com.fastasyncworldedit.bukkit.adapter.NMSAdapter;
import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.math.BitArrayUnstretched;
import com.fastasyncworldedit.core.math.IntPair;
import com.fastasyncworldedit.core.util.FoliaUtil;
import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.TaskManager;
import com.mojang.serialization.DataResult;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.bukkit.adapter.Refraction;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import io.papermc.lib.PaperLib;
import io.papermc.paper.util.MCUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.ThreadingDetector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.Strategy;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.apache.logging.log4j.Logger;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.CraftChunk;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.function.IntFunction;
import java.util.stream.LongStream;

import static net.minecraft.core.registries.Registries.BIOME;

public final class PaperweightPlatformAdapter extends NMSAdapter {

    public static final Field fieldData;

    public static final Constructor<?> dataConstructor;

    public static final Field fieldStorage;
    public static final Field fieldPalette;

    private static final MethodHandle palettedContainerUnpackSpigot;

    private static final Field fieldTickingFluidCount;
    private static final Field fieldTickingBlockCount;
    private static final Field fieldBiomes;

    private static final MethodHandle methodGetVisibleChunk;

    private static final Field fieldThreadingDetector;
    private static final Field fieldLock;

    private static final MethodHandle methodRemoveGameEventListener;
    private static final MethodHandle methodremoveTickingBlockEntity;

    private static final Field fieldRemove;

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private static Field SERVER_LEVEL_ENTITY_MANAGER;

    static final MethodHandle PALETTED_CONTAINER_GET;

    static {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            fieldData = PalettedContainer.class.getDeclaredField(Refraction.pickName("data", "b"));
            fieldData.setAccessible(true);

            Class<?> dataClazz = fieldData.getType();
            dataConstructor = dataClazz.getDeclaredConstructors()[0];
            dataConstructor.setAccessible(true);

            fieldStorage = dataClazz.getDeclaredField(Refraction.pickName("storage", "b"));
            fieldStorage.setAccessible(true);
            fieldPalette = dataClazz.getDeclaredField(Refraction.pickName("palette", "c"));
            fieldPalette.setAccessible(true);

            //noinspection JavaLangInvokeHandleSignature - method is obfuscated
            palettedContainerUnpackSpigot = PaperLib.isPaper() ? null : lookup.findStatic(
                    PalettedContainer.class,
                    "a", // unpack
                    MethodType.methodType(DataResult.class, Strategy.class, PalettedContainerRO.PackedData.class)
            );

            fieldTickingFluidCount = LevelChunkSection.class.getDeclaredField(Refraction.pickName(
                    "tickingFluidCount",
                    "g"
            ));
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

            Method getVisibleChunkIfPresent = ChunkMap.class.getDeclaredMethod(
                    Refraction.pickName(
                            "getVisibleChunkIfPresent",
                            "b"
                    ), long.class
            );
            getVisibleChunkIfPresent.setAccessible(true);
            methodGetVisibleChunk = lookup.unreflect(getVisibleChunkIfPresent);

            if (!PaperLib.isPaper()) {
                fieldThreadingDetector = PalettedContainer.class.getDeclaredField(Refraction.pickName("threadingDetector", "d"));
                fieldThreadingDetector.setAccessible(true);
                fieldLock = ThreadingDetector.class.getDeclaredField(Refraction.pickName("lock", "c"));
                fieldLock.setAccessible(true);
                SERVER_LEVEL_ENTITY_MANAGER = ServerLevel.class.getDeclaredField(Refraction.pickName("entityManager", "M"));
                SERVER_LEVEL_ENTITY_MANAGER.setAccessible(true);
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

            Method palettedContainerGet = PalettedContainer.class.getDeclaredMethod(
                    Refraction.pickName("get", "a"),
                    int.class
            );
            palettedContainerGet.setAccessible(true);
            PALETTED_CONTAINER_GET = lookup.unreflect(palettedContainerGet);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static TagValueOutput createOutput() {
        return TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                DedicatedServer.getServer().registryAccess()
        );
    }

    public static TagValueOutput createOutput(CompoundTag compoundTag) {
        return TagValueOutput.createWrappingWithContext(
                ProblemReporter.DISCARDING,
                DedicatedServer.getServer().registryAccess(),
                compoundTag
        );
    }

    public static ValueInput createInput(CompoundTag nativeTag) {
        return TagValueInput.create(
                ProblemReporter.DISCARDING,
                DedicatedServer.getServer().registryAccess(),
                nativeTag
        );
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
            LOGGER.error("Error apply DelegateSemaphore", e);
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
                            return toLevelChunk(chunk);
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

    private static LevelChunk toLevelChunk(Chunk chunk) {
        return (LevelChunk) ((CraftChunk) chunk).getHandle(ChunkStatus.FULL);
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
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        if (FoliaUtil.isFoliaServer()) {
            try {
                serverLevel.getChunkSource().addTicketWithRadius(ChunkHolderManager.UNLOAD_COOLDOWN, pos, 0);
            } catch (Exception ignored) {
            }
            return;
        }
        try {
            MCUtil.MAIN_EXECUTOR.execute(() -> serverLevel.getChunkSource().addTicketWithRadius(ChunkHolderManager.UNLOAD_COOLDOWN, pos, 0));
        } catch (Exception e) {
            try {
                serverLevel.getChunkSource().addTicketWithRadius(ChunkHolderManager.UNLOAD_COOLDOWN, pos, 0);
            } catch (Exception ignored) {
            }
        }
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
        LevelChunk levelChunk = PaperLib.isPaper()
                ? nmsWorld.getChunkSource().getChunkAtIfLoadedImmediately(chunkX, chunkZ)
                : chunkHolder.getTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).orElse(null);
        if (levelChunk == null) {
            return;
        }
        StampLockHolder lockHolder = new StampLockHolder();
        NMSAdapter.beginChunkPacketSend(nmsWorld.getWorld().getName(), pair, lockHolder);
        if (lockHolder.chunkLock == null) {
            return;
        }

        ChunkPos pos = levelChunk.getPos();
        ClientboundLevelChunkWithLightPacket packet = PaperLib.isPaper()
                ? new ClientboundLevelChunkWithLightPacket(levelChunk, nmsWorld.getLightEngine(), null, null, false)
                : new ClientboundLevelChunkWithLightPacket(levelChunk, nmsWorld.getLightEngine(), null, null);

        Runnable sendPacket = () -> nearbyPlayers(nmsWorld, pos).forEach(p -> p.connection.send(packet));

        if (FoliaUtil.isFoliaServer()) {
            try {
                sendPacket.run();
            } finally {
                NMSAdapter.endChunkPacketSend(nmsWorld.getWorld().getName(), pair, lockHolder);
            }
        } else {
            try {
                MinecraftServer.getServer().execute(() -> {
                    try {
                        sendPacket.run();
                    } finally {
                        NMSAdapter.endChunkPacketSend(nmsWorld.getWorld().getName(), pair, lockHolder);
                    }
                });
            } catch (Exception e) {
                try {
                    sendPacket.run();
                } finally {
                    NMSAdapter.endChunkPacketSend(nmsWorld.getWorld().getName(), pair, lockHolder);
                }
            }
        }
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
            RegistryAccess registryAccess,
            @Nullable PalettedContainer<Holder<Biome>> biomes
    ) {
        return newChunkSection(layer, null, blocks, adapter, registryAccess, biomes);
    }

    public static LevelChunkSection newChunkSection(
            final int layer,
            final IntFunction<char[]> get,
            char[] set,
            CachedBukkitAdapter adapter,
            RegistryAccess registryAccess,
            @Nullable PalettedContainer<Holder<Biome>> biomes
    ) {
        if (set == null) {
            return newChunkSection(registryAccess, biomes);
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
            final int blockBitArrayEnd = MathMan.longArrayLength(bitsPerEntryNonZero, 4096);

            if (num_palette == 1) {
                for (int i = 0; i < blockBitArrayEnd; i++) {
                    blockStates[i] = 0;
                }
            } else {
                final BitArrayUnstretched bitArray = new BitArrayUnstretched(bitsPerEntryNonZero, 4096, blockStates);
                bitArray.fromRaw(blocksCopy);
            }

            final long[] bits = Arrays.copyOfRange(blockStates, 0, blockBitArrayEnd);
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
            var strategy = Strategy.createForBlockStates(Block.BLOCK_STATE_REGISTRY);
            var packedData = new PalettedContainerRO.PackedData<>(palette, Optional.of(LongStream.of(bits)), bitsPerEntry);
            DataResult<PalettedContainer<net.minecraft.world.level.block.state.BlockState>> result;
            if (PaperLib.isPaper()) {
                result = PalettedContainer.unpack(strategy, packedData, Blocks.AIR.defaultBlockState(), null);
            } else {
                //noinspection unchecked
                result = (DataResult<PalettedContainer<net.minecraft.world.level.block.state.BlockState>>)
                        palettedContainerUnpackSpigot.invokeExact(strategy, packedData);
            }
            if (biomes == null) {
                biomes = PalettedContainerFactory.create(registryAccess).createForBiomes();
            }
            return new LevelChunkSection(result.getOrThrow(), biomes);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create block palette", e);
        } finally {
            Arrays.fill(blockToPalette, Integer.MAX_VALUE);
            Arrays.fill(paletteToBlock, Integer.MAX_VALUE);
            Arrays.fill(blockStates, 0);
            Arrays.fill(blocksCopy, 0);
        }
    }

    @SuppressWarnings("deprecation") // Only deprecated in paper
    private static LevelChunkSection newChunkSection(
            RegistryAccess registryAccess,
            @Nullable PalettedContainer<Holder<Biome>> biomes
    ) {
        PalettedContainerFactory factory = PalettedContainerFactory.create(registryAccess);
        if (biomes == null) {
            return new LevelChunkSection(factory);
        }
        return new LevelChunkSection(factory.createForBlockStates(), biomes);
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
        final List<Holder<Biome>> palette = new ArrayList<>();
        for (BiomeType biomeType : new LinkedList<>(Set.of(biomes))) {
            if (biomeType == null) {
                palette.add(biomeRegistry.byId(adapter.getInternalBiomeId(BiomeTypes.PLAINS)));
                continue;
            }
            palette.add(biomeRegistry.byId(adapter.getInternalBiomeId(biomeType)));
        }
        int biomeCount = palette.size();
        int bitsPerEntry = MathMan.log2nlz(biomeCount - 1);

        if (bitsPerEntry > 3) {
            bitsPerEntry = MathMan.log2nlz(biomeRegistry.size() - 1);
        }

        int bitsPerEntryNonZero = Math.max(bitsPerEntry, 1); // We do want to use zero sometimes
        final int arrayLength = MathMan.longArrayLength(bitsPerEntryNonZero, 64);

        var strategy = Strategy.createForBiomes(biomeRegistry);
        var packedData = new PalettedContainerRO.PackedData<>(
                palette, Optional.of(LongStream.of(new long[arrayLength])), bitsPerEntry
        );
        DataResult<PalettedContainer<Holder<Biome>>> result;
        if (PaperLib.isPaper()) {
            result = PalettedContainer.unpack(
                    strategy,
                    packedData,
                    biomeRegistry.byIdOrThrow(adapter.getInternalBiomeId(BiomeTypes.PLAINS)),
                    null
            );
        } else {
            try {
                //noinspection unchecked
                result = (DataResult<PalettedContainer<Holder<Biome>>>)
                        palettedContainerUnpackSpigot.invokeExact(strategy, packedData);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to create biome palette for Spigot", e);
            }
        }
        var biomePalettedContainer = result.getOrThrow();

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

        return biomePalettedContainer;
    }

    public static void clearCounts(final LevelChunkSection section) throws IllegalAccessException {
        fieldTickingFluidCount.setShort(section, (short) 0);
        fieldTickingBlockCount.setShort(section, (short) 0);
    }

    public static BiomeType adapt(Holder<Biome> biome, LevelAccessor levelAccessor) {
        final Registry<Biome> biomeRegistry = levelAccessor.registryAccess().lookupOrThrow(BIOME);
        final int id = biomeRegistry.getId(biome.value());
        if (id < 0) {
            // this shouldn't be the case, but other plugins can be weird
            return BiomeTypes.OCEAN;
        }
        return BiomeTypes.getLegacy(id);
    }

    static void removeBeacon(BlockEntity beacon, LevelChunk levelChunk) {
        try {
            Map<BlockPos, BlockEntity> blockEntities = levelChunk.getBlockEntities();
            if (levelChunk.loaded || levelChunk.level.isClientSide()) {
                BlockEntity blockEntity = blockEntities.remove(beacon.getBlockPos());
                if (blockEntity != null) {
                    if (!levelChunk.level.isClientSide()) {
                        methodRemoveGameEventListener.invoke(levelChunk, beacon, levelChunk.level);
                    }
                    fieldRemove.set(beacon, true);
                }
            }
            methodremoveTickingBlockEntity.invoke(levelChunk, beacon.getBlockPos());
        } catch (Throwable throwable) {
            LOGGER.error("Error removing beacon", throwable);
        }
    }

    static List<Entity> getEntities(LevelChunk chunk) {
        if (PaperLib.isPaper()) {
            return Optional.ofNullable(chunk.level
                    .moonrise$getEntityLookup()
                    .getChunk(chunk.locX, chunk.locZ)).map(ChunkEntitySlices::getAllEntities).orElse(Collections.emptyList());
        }
        try {
            //noinspection unchecked
            return ((PersistentEntitySectionManager<Entity>) (SERVER_LEVEL_ENTITY_MANAGER.get(chunk.level))).getEntities(chunk.getPos());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to lookup entities [PAPER=false]", e);
        }
    }

}
