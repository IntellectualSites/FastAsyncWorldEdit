package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_17_R1_2;

import com.fastasyncworldedit.bukkit.adapter.CachedBukkitAdapter;
import com.fastasyncworldedit.bukkit.adapter.DelegateSemaphore;
import com.fastasyncworldedit.bukkit.adapter.NMSAdapter;
import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.math.BitArrayUnstretched;
import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.ReflectionUtils;
import com.fastasyncworldedit.core.util.TaskManager;
import com.mojang.datafixers.util.Either;
import com.sk89q.worldedit.bukkit.adapter.Refraction;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import io.papermc.lib.PaperLib;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.BitStorage;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.HashMapPalette;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LinearPalette;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.level.gameevent.GameEventListener;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.CraftChunk;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Stream;

public final class PaperweightPlatformAdapter extends NMSAdapter {

    public static final Field FIELD_STORAGE;
    public static final Field FIELD_PALETTE;
    public static final Field FIELD_BITS;

    public static final Field FIELD_BITS_PER_ENTRY;

    private static final Field FIELD_TICKING_FLUID_CONTENT;
    private static final Field FIELD_TICKING_BLOCK_COUNT;
    private static final Field FIELD_NON_EMPTY_BLOCK_COUNT;

    private static final Field FIELD_BIOMES;

    private static final MethodHandle METHOD_GET_VISIBLE_CHUNK;

    private static final Field FIELD_LOCK;

    private static final Field FIELD_GAME_EVENT_DISPATCHER_SECTIONS;
    private static final MethodHandle METHOD_REMOVE_BLOCK_ENTITY_TICKER;

    private static final Field FIELD_REMOVE;

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    static {
        try {
            FIELD_BITS = PalettedContainer.class.getDeclaredField(Refraction.pickName("bits", "l"));
            FIELD_BITS.setAccessible(true);
            FIELD_STORAGE = PalettedContainer.class.getDeclaredField(Refraction.pickName("storage", "c"));
            FIELD_STORAGE.setAccessible(true);
            FIELD_PALETTE = PalettedContainer.class.getDeclaredField(Refraction.pickName("palette", "k"));
            FIELD_PALETTE.setAccessible(true);

            FIELD_BITS_PER_ENTRY = BitStorage.class.getDeclaredField(Refraction.pickName("bits", "c"));
            FIELD_BITS_PER_ENTRY.setAccessible(true);

            FIELD_TICKING_FLUID_CONTENT = LevelChunkSection.class.getDeclaredField(Refraction.pickName("tickingFluidCount", "h"));
            FIELD_TICKING_FLUID_CONTENT.setAccessible(true);
            FIELD_TICKING_BLOCK_COUNT = LevelChunkSection.class.getDeclaredField(Refraction.pickName("tickingBlockCount", "g"));
            FIELD_TICKING_BLOCK_COUNT.setAccessible(true);
            FIELD_NON_EMPTY_BLOCK_COUNT = LevelChunkSection.class.getDeclaredField(Refraction.pickName("nonEmptyBlockCount", "f"));
            FIELD_NON_EMPTY_BLOCK_COUNT.setAccessible(true);

            FIELD_BIOMES = ChunkBiomeContainer.class.getDeclaredField(Refraction.pickName("biomes", "f"));
            FIELD_BIOMES.setAccessible(true);

            Method getVisibleChunkIfPresent = ChunkMap.class.getDeclaredMethod(Refraction.pickName(
                    "getVisibleChunkIfPresent",
                    "getVisibleChunk"
            ), long.class);
            getVisibleChunkIfPresent.setAccessible(true);
            METHOD_GET_VISIBLE_CHUNK = MethodHandles.lookup().unreflect(getVisibleChunkIfPresent);

            if (!PaperLib.isPaper()) {
                FIELD_LOCK = PalettedContainer.class.getDeclaredField(Refraction.pickName("lock", "m"));
                FIELD_LOCK.setAccessible(true);
            } else {
                // in paper, the used methods are synchronized properly
                FIELD_LOCK = null;
            }

            FIELD_GAME_EVENT_DISPATCHER_SECTIONS = LevelChunk.class.getDeclaredField(Refraction.pickName(
                    "gameEventDispatcherSections", "x"));
            FIELD_GAME_EVENT_DISPATCHER_SECTIONS.setAccessible(true);
            Method removeBlockEntityTicker = LevelChunk.class.getDeclaredMethod(
                    Refraction.pickName(
                            "removeBlockEntityTicker",
                            "l"
                    ), BlockPos.class
            );
            removeBlockEntityTicker.setAccessible(true);
            METHOD_REMOVE_BLOCK_ENTITY_TICKER = MethodHandles.lookup().unreflect(removeBlockEntityTicker);

            FIELD_REMOVE = BlockEntity.class.getDeclaredField(Refraction.pickName("remove", "p"));
            FIELD_REMOVE.setAccessible(true);
        } catch (RuntimeException e) {
            LOGGER.debug("Something went wrong", e);
            throw e;
        } catch (Throwable rethrow) {
            LOGGER.debug("Something went wrong", rethrow);
            throw new RuntimeException(rethrow);
        }
    }

    static boolean setSectionAtomic(
            LevelChunkSection[] sections,
            LevelChunkSection expected,
            LevelChunkSection value,
            int layer
    ) {
        if (layer >= 0 && layer < sections.length) {
            return ReflectionUtils.compareAndSet(sections, expected, value, layer);
        }
        return false;
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
                Semaphore currentLock = (Semaphore) FIELD_LOCK.get(blocks);
                if (currentLock instanceof DelegateSemaphore delegateSemaphore) {
                    return delegateSemaphore;
                }
                DelegateSemaphore newLock = new DelegateSemaphore(1, currentLock);
                FIELD_LOCK.set(blocks, newLock);
                return newLock;
            }
        } catch (Throwable e) {
            LOGGER.error("Something went wrong on apply lock", e);
            throw new RuntimeException(e);
        }
    }

    public static LevelChunk ensureLoaded(ServerLevel serverLevel, int chunkX, int chunkZ) {
        if (!PaperLib.isPaper()) {
            LevelChunk nmsChunk = serverLevel.getChunkSource().getChunk(chunkX, chunkZ, false);
            if (nmsChunk != null) {
                return nmsChunk;
            }
            if (Fawe.isMainThread()) {
                return serverLevel.getChunk(chunkX, chunkZ);
            }
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
            CompletableFuture<org.bukkit.Chunk> future = serverLevel.getWorld().getChunkAtAsync(chunkX, chunkZ, true, true);
            try {
                CraftChunk chunk;
                try {
                    chunk = (CraftChunk) future.get(10, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    String world = serverLevel.getWorld().getName();
                    // We've already taken 10 seconds we can afford to wait a little here.
                    boolean loaded = TaskManager.taskManager().sync(() -> Bukkit.getWorld(world) != null);
                    if (loaded) {
                        LOGGER.warn("Chunk {},{} failed to load in 10 seconds in world {}. Retrying...", chunkX, chunkZ, world);
                        // Retry chunk load
                        chunk = (CraftChunk) serverLevel.getWorld().getChunkAtAsync(chunkX, chunkZ, true, true).get();
                    } else {
                        throw new UnsupportedOperationException("Cannot load chunk from unloaded world " + world + "!");
                    }
                }
                return chunk.getHandle();
            } catch (Throwable e) {
                LOGGER.error("Something went wrong on ensure chunk loading", e);
            }
        }
        return TaskManager.taskManager().sync(() -> serverLevel.getChunk(chunkX, chunkZ));
    }

    private static void addTicket(ServerLevel serverLevel, int chunkX, int chunkZ) {
        // Ensure chunk is definitely loaded before applying a ticket
        net.minecraft.server.MCUtil.MAIN_EXECUTOR.execute(() -> serverLevel
                .getChunkSource()
                .addRegionTicket(TicketType.PLUGIN, new ChunkPos(chunkX, chunkZ), 0, Unit.INSTANCE));
    }

    public static ChunkHolder getPlayerChunk(ServerLevel nmsWorld, final int chunkX, final int chunkZ) {
        ChunkMap chunkMap = nmsWorld.getChunkSource().chunkMap;
        try {
            return (ChunkHolder) METHOD_GET_VISIBLE_CHUNK.invoke(chunkMap, ChunkPos.asLong(chunkX, chunkZ));
        } catch (Throwable thr) {
            LOGGER.debug("Something went wrong to get player chunk", thr);
            throw new RuntimeException(thr);
        }
    }

    @SuppressWarnings("unchecked")
    public static void sendChunk(ServerLevel nmsWorld, int chunkX, int chunkZ, boolean lighting) {
        ChunkHolder chunkHolder = getPlayerChunk(nmsWorld, chunkX, chunkZ);
        if (chunkHolder == null) {
            return;
        }
        ChunkPos coordIntPair = new ChunkPos(chunkX, chunkZ);
        // UNLOADED_CHUNK
        Optional<LevelChunk> optional = ((Either) chunkHolder
                .getTickingChunkFuture()
                .getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK)).left();
        if (PaperLib.isPaper()) {
            // getChunkAtIfLoadedImmediately is paper only
            optional = optional.or(() -> Optional.ofNullable(nmsWorld
                    .getChunkSource()
                    .getChunkAtIfLoadedImmediately(chunkX, chunkZ)));
        }
        if (optional.isEmpty()) {
            return;
        }
        LevelChunk levelChunk = optional.get();
        TaskManager.taskManager().task(() -> {
            ClientboundLevelChunkPacket chunkPacket = new ClientboundLevelChunkPacket(levelChunk);
            nearbyPlayers(nmsWorld, coordIntPair).forEach(p -> p.connection.send(chunkPacket));
            if (lighting) {
                //This needs to be true otherwise Minecraft will update lighting from/at the chunk edges (bad)
                boolean trustEdges = true;
                ClientboundLightUpdatePacket packet =
                        new ClientboundLightUpdatePacket(coordIntPair, nmsWorld.getChunkSource().getLightEngine(), null, null,
                                trustEdges
                        );
                nearbyPlayers(nmsWorld, coordIntPair).forEach(p -> p.connection.send(packet));
            }
        });
    }

    private static Stream<ServerPlayer> nearbyPlayers(ServerLevel serverLevel, ChunkPos coordIntPair) {
        return serverLevel.getChunkSource().chunkMap.getPlayers(coordIntPair, false);
    }

    /*
    NMS conversion
     */
    public static LevelChunkSection newChunkSection(
            final int layer,
            final char[] blocks,
            boolean fastMode,
            CachedBukkitAdapter adapter
    ) {
        return newChunkSection(layer, null, blocks, fastMode, adapter);
    }

    public static LevelChunkSection newChunkSection(
            final int layer,
            final Function<Integer, char[]> get,
            char[] set,
            boolean fastMode,
            CachedBukkitAdapter adapter
    ) {
        if (set == null) {
            return newChunkSection(layer);
        }
        final int[] blockToPalette = FaweCache.INSTANCE.BLOCK_TO_PALETTE.get();
        final int[] paletteToBlock = FaweCache.INSTANCE.PALETTE_TO_BLOCK.get();
        final long[] blockStates = FaweCache.INSTANCE.BLOCK_STATES.get();
        final int[] blocksCopy = FaweCache.INSTANCE.SECTION_BLOCKS.get();
        try {
            int num_palette;
            final short[] nonEmptyBlockCount = fastMode ? new short[1] : null;
            if (get == null) {
                num_palette = createPalette(blockToPalette, paletteToBlock, blocksCopy, set, adapter, nonEmptyBlockCount);
            } else {
                num_palette = createPalette(
                        layer,
                        blockToPalette,
                        paletteToBlock,
                        blocksCopy,
                        get,
                        set,
                        adapter,
                        nonEmptyBlockCount
                );
            }
            // BlockStates
            int bitsPerEntry = MathMan.log2nlz(num_palette - 1);
            if (Settings.settings().PROTOCOL_SUPPORT_FIX || num_palette != 1) {
                bitsPerEntry = Math.max(bitsPerEntry, 4); // Protocol support breaks <4 bits per entry
            } else {
                bitsPerEntry = Math.max(bitsPerEntry, 1); // For some reason minecraft needs 4096 bits to store 0 entries
            }
            if (bitsPerEntry > 8) {
                bitsPerEntry = MathMan.log2nlz(Block.BLOCK_STATE_REGISTRY.size() - 1);
            }

            final int blocksPerLong = MathMan.floorZero((double) 64 / bitsPerEntry);
            final int blockBitArrayEnd = MathMan.ceilZero((float) 4096 / blocksPerLong);

            if (num_palette == 1) {
                for (int i = 0; i < blockBitArrayEnd; i++) {
                    blockStates[i] = 0;
                }
            } else {
                final BitArrayUnstretched bitArray = new BitArrayUnstretched(bitsPerEntry, 4096, blockStates);
                bitArray.fromRaw(blocksCopy);
            }

            LevelChunkSection levelChunkSection = newChunkSection(layer);
            // set palette & data bits
            final PalettedContainer<net.minecraft.world.level.block.state.BlockState> dataPaletteBlocks =
                    levelChunkSection.getStates();
            // private DataPalette<T> h;
            // protected DataBits a;
            final long[] bits = Arrays.copyOfRange(blockStates, 0, blockBitArrayEnd);
            final BitStorage nmsBits = new BitStorage(bitsPerEntry, 4096, bits);
            final Palette<net.minecraft.world.level.block.state.BlockState> blockStatePalettedContainer;
            if (bitsPerEntry <= 4) {
                blockStatePalettedContainer = new LinearPalette<>(Block.BLOCK_STATE_REGISTRY, bitsPerEntry, dataPaletteBlocks,
                        NbtUtils::readBlockState
                );
            } else if (bitsPerEntry < 9) {
                blockStatePalettedContainer = new HashMapPalette<>(
                        Block.BLOCK_STATE_REGISTRY,
                        bitsPerEntry,
                        dataPaletteBlocks,
                        NbtUtils::readBlockState,
                        NbtUtils::writeBlockState
                );
            } else {
                blockStatePalettedContainer = LevelChunkSection.GLOBAL_BLOCKSTATE_PALETTE;
            }

            // set palette if required
            if (bitsPerEntry < 9) {
                for (int i = 0; i < num_palette; i++) {
                    final int ordinal = paletteToBlock[i];
                    blockToPalette[ordinal] = Integer.MAX_VALUE;
                    final BlockState state = BlockTypesCache.states[ordinal];
                    final net.minecraft.world.level.block.state.BlockState blockState = ((PaperweightBlockMaterial) state.getMaterial()).getState();
                    blockStatePalettedContainer.idFor(blockState);
                }
            }
            try {
                FIELD_STORAGE.set(dataPaletteBlocks, nmsBits);
                FIELD_PALETTE.set(dataPaletteBlocks, blockStatePalettedContainer);
                FIELD_BITS.set(dataPaletteBlocks, bitsPerEntry);
            } catch (final IllegalAccessException e) {
                LOGGER.debug("Something went wrong", e);
                throw new RuntimeException(e);
            }

            if (!fastMode) {
                levelChunkSection.recalcBlockCounts();
            } else {
                try {
                    FIELD_NON_EMPTY_BLOCK_COUNT.set(levelChunkSection, nonEmptyBlockCount[0]);
                } catch (IllegalAccessException e) {
                    LOGGER.debug("Something went wrong", e);
                    throw new RuntimeException(e);
                }
            }
            return levelChunkSection;
        } catch (final Throwable e) {
            LOGGER.error("Something went wrong at create new chunk section", e);
            throw e;
        } finally {
            Arrays.fill(blockToPalette, Integer.MAX_VALUE);
            Arrays.fill(paletteToBlock, Integer.MAX_VALUE);
            Arrays.fill(blockStates, 0);
            Arrays.fill(blocksCopy, 0);
        }
    }

    private static LevelChunkSection newChunkSection(int layer) {
        return new LevelChunkSection(layer);
    }

    public static void clearCounts(final LevelChunkSection section) throws IllegalAccessException {
        FIELD_TICKING_FLUID_CONTENT.setShort(section, (short) 0);
        FIELD_TICKING_BLOCK_COUNT.setShort(section, (short) 0);
    }
    public static BiomeType adapt(Biome biome, LevelAccessor levelAccessor) {
        ResourceLocation resourceLocation = levelAccessor.registryAccess().ownedRegistryOrThrow(Registry.BIOME_REGISTRY).getKey(
                biome);
        if (resourceLocation == null) {
            return levelAccessor.registryAccess().ownedRegistryOrThrow(Registry.BIOME_REGISTRY).getId(biome) == -1
                    ? BiomeTypes.OCEAN
                    : null;
        }
        return BiomeTypes.get(resourceLocation.toString().toLowerCase(Locale.ROOT));
    }

    @SuppressWarnings("unchecked")
    static void removeBeacon(BlockEntity beacon, LevelChunk levelChunk) {
        try {
            // Do the method ourselves to avoid trying to reflect generic method parameters
            if (levelChunk.loaded || levelChunk.level.isClientSide()) {
                BlockEntity blockEntity = levelChunk.blockEntities.remove(beacon.getBlockPos());
                if (blockEntity != null) {
                    if (!levelChunk.level.isClientSide) {
                        Block block = beacon.getBlockState().getBlock();
                        if (block instanceof EntityBlock) {
                            GameEventListener gameEventListener = ((EntityBlock) block).getListener(levelChunk.level, beacon);
                            if (gameEventListener != null) {
                                int i = SectionPos.blockToSectionCoord(beacon.getBlockPos().getY());
                                GameEventDispatcher gameEventDispatcher = levelChunk.getEventDispatcher(i);
                                gameEventDispatcher.unregister(gameEventListener);
                                if (gameEventDispatcher.isEmpty()) {
                                    try {
                                        ((Int2ObjectMap<GameEventDispatcher>) FIELD_GAME_EVENT_DISPATCHER_SECTIONS.get(levelChunk))
                                                .remove(i);
                                    } catch (IllegalAccessException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                    FIELD_REMOVE.set(beacon, true);
                }
            }
            METHOD_REMOVE_BLOCK_ENTITY_TICKER.invoke(levelChunk, beacon.getBlockPos());
        } catch (Throwable throwable) {
            LOGGER.error("Something went wrong to remove beacon", throwable);
        }
    }

    static List<Entity> getEntities(LevelChunk chunk) {
        return chunk.level.entityManager.getEntities(chunk.getPos());
    }

}
