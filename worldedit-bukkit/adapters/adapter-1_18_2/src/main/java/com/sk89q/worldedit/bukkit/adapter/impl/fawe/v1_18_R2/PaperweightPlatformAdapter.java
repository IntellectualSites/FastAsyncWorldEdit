package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_18_R2;

import com.fastasyncworldedit.bukkit.adapter.CachedBukkitAdapter;
import com.fastasyncworldedit.bukkit.adapter.DelegateSemaphore;
import com.fastasyncworldedit.bukkit.adapter.NMSAdapter;
import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.math.BitArrayUnstretched;
import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.ReflectionUtils;
import com.fastasyncworldedit.core.util.TaskManager;
import com.mojang.datafixers.util.Either;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.bukkit.adapter.Refraction;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import io.papermc.lib.PaperLib;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.BitStorage;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.ThreadingDetector;
import net.minecraft.util.ZeroBitStorage;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.GlobalPalette;
import net.minecraft.world.level.chunk.HashMapPalette;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LinearPalette;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.SingleValuePalette;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.level.gameevent.GameEventListener;
import org.bukkit.craftbukkit.v1_18_R2.CraftChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

public final class PaperweightPlatformAdapter extends NMSAdapter {

    public static final Field fieldData;

    public static final Constructor<?> dataConstructor;

    public static final Field fieldStorage;
    public static final Field fieldPalette;


    public static final Field fieldTickingFluidCount;
    public static final Field fieldTickingBlockCount;
    public static final Field fieldNonEmptyBlockCount;

    private static final MethodHandle methodGetVisibleChunk;

    private static final int CHUNKSECTION_BASE;
    private static final int CHUNKSECTION_SHIFT;

    private static final Field fieldThreadingDetector;
    private static final long fieldThreadingDetectorOffset;

    private static final Field fieldLock;
    private static final long fieldLockOffset;

    private static final Field fieldGameEventDispatcherSections;
    private static final MethodHandle methodremoveTickingBlockEntity;

    private static final Field fieldRemove;

    static {
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

            fieldTickingFluidCount = LevelChunkSection.class.getDeclaredField(Refraction.pickName("tickingFluidCount", "h"));
            fieldTickingFluidCount.setAccessible(true);
            fieldTickingBlockCount = LevelChunkSection.class.getDeclaredField(Refraction.pickName("tickingBlockCount", "g"));
            fieldTickingBlockCount.setAccessible(true);
            fieldNonEmptyBlockCount = LevelChunkSection.class.getDeclaredField(Refraction.pickName("nonEmptyBlockCount", "f"));
            fieldNonEmptyBlockCount.setAccessible(true);

            Method getVisibleChunkIfPresent = ChunkMap.class.getDeclaredMethod(Refraction.pickName(
                    "getVisibleChunkIfPresent",
                    "b"
            ), long.class);
            getVisibleChunkIfPresent.setAccessible(true);
            methodGetVisibleChunk = MethodHandles.lookup().unreflect(getVisibleChunkIfPresent);

            Unsafe unsafe = ReflectionUtils.getUnsafe();
            fieldThreadingDetector = PalettedContainer.class.getDeclaredField(Refraction.pickName("threadingDetector", "f"));
            fieldThreadingDetectorOffset = unsafe.objectFieldOffset(fieldThreadingDetector);

            fieldLock = ThreadingDetector.class.getDeclaredField(Refraction.pickName("lock", "c"));
            fieldLockOffset = unsafe.objectFieldOffset(fieldLock);

            fieldGameEventDispatcherSections = LevelChunk.class.getDeclaredField(Refraction.pickName(
                    "gameEventDispatcherSections", "t"));
            fieldGameEventDispatcherSections.setAccessible(true);
            Method removeBlockEntityTicker = LevelChunk.class.getDeclaredMethod(
                    Refraction.pickName(
                            "removeBlockEntityTicker",
                            "l"
                    ), BlockPos.class
            );
            removeBlockEntityTicker.setAccessible(true);
            methodremoveTickingBlockEntity = MethodHandles.lookup().unreflect(removeBlockEntityTicker);

            fieldRemove = BlockEntity.class.getDeclaredField(Refraction.pickName("remove", "p"));
            fieldRemove.setAccessible(true);

            CHUNKSECTION_BASE = unsafe.arrayBaseOffset(LevelChunkSection[].class);
            int scale = unsafe.arrayIndexScale(LevelChunkSection[].class);
            if ((scale & (scale - 1)) != 0) {
                throw new Error("data type scale not a power of two");
            }
            CHUNKSECTION_SHIFT = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable rethrow) {
            rethrow.printStackTrace();
            throw new RuntimeException(rethrow);
        }
    }

    static boolean setSectionAtomic(
            LevelChunkSection[] sections,
            LevelChunkSection expected,
            LevelChunkSection value,
            int layer
    ) {
        long offset = ((long) layer << CHUNKSECTION_SHIFT) + CHUNKSECTION_BASE;
        if (layer >= 0 && layer < sections.length) {
            return ReflectionUtils.getUnsafe().compareAndSwapObject(sections, offset, expected, value);
        }
        return false;
    }

    static DelegateSemaphore applyLock(LevelChunkSection section) {
        try {
            synchronized (section) {
                Unsafe unsafe = ReflectionUtils.getUnsafe();
                PalettedContainer<net.minecraft.world.level.block.state.BlockState> blocks = section.getStates();
                ThreadingDetector currentThreadingDetector = (ThreadingDetector) unsafe.getObject(
                        blocks,
                        fieldThreadingDetectorOffset
                );
                synchronized (currentThreadingDetector) {
                    Semaphore currentLock = (Semaphore) unsafe.getObject(currentThreadingDetector, fieldLockOffset);
                    if (currentLock instanceof DelegateSemaphore) {
                        return (DelegateSemaphore) currentLock;
                    }
                    DelegateSemaphore newLock = new DelegateSemaphore(1, currentLock);
                    unsafe.putObject(currentThreadingDetector, fieldLockOffset, newLock);
                    return newLock;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
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
                return nmsChunk;
            }
            nmsChunk = serverLevel.getChunkSource().getChunkAtIfLoadedImmediately(chunkX, chunkZ);
            if (nmsChunk != null) {
                return nmsChunk;
            }
            // Avoid "async" methods from the main thread.
            if (Fawe.isMainThread()) {
                return serverLevel.getChunk(chunkX, chunkZ);
            }
            CompletableFuture<org.bukkit.Chunk> future = serverLevel.getWorld().getChunkAtAsync(chunkX, chunkZ, true, true);
            try {
                CraftChunk chunk = (CraftChunk) future.get();
                return chunk.getHandle();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return TaskManager.taskManager().sync(() -> serverLevel.getChunk(chunkX, chunkZ));
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
            ClientboundLevelChunkWithLightPacket packet;
            if (PaperLib.isPaper()) {
                packet = new ClientboundLevelChunkWithLightPacket(
                        levelChunk,
                        nmsWorld.getChunkSource().getLightEngine(),
                        null,
                        null,
                        true,
                        false // last false is to not bother with x-ray
                );
            } else {
                // deprecated on paper - deprecation suppressed
                packet = new ClientboundLevelChunkWithLightPacket(
                        levelChunk,
                        nmsWorld.getChunkSource().getLightEngine(),
                        null,
                        null,
                        true
                );
            }
            nearbyPlayers(nmsWorld, coordIntPair).forEach(p -> p.connection.send(packet));
        });
    }

    private static List<ServerPlayer> nearbyPlayers(ServerLevel serverLevel, ChunkPos coordIntPair) {
        return serverLevel.getChunkSource().chunkMap.getPlayers(coordIntPair, false);
    }

    /*
    NMS conversion
     */
    public static LevelChunkSection newChunkSection(
            final int layer, final char[] blocks, boolean fastmode,
            CachedBukkitAdapter adapter, IdMap<Holder<Biome>> biomeRegistry,
            @Nullable PalettedContainer<Holder<Biome>> biomes
    ) {
        return newChunkSection(layer, null, blocks, fastmode, adapter, biomeRegistry, biomes);
    }

    public static LevelChunkSection newChunkSection(
            final int layer, final Function<Integer, char[]> get, char[] set,
            boolean fastmode, CachedBukkitAdapter adapter, IdMap<Holder<Biome>> biomeRegistry,
            @Nullable PalettedContainer<Holder<Biome>> biomes
    ) {
        if (set == null) {
            return newChunkSection(layer, biomeRegistry, biomes);
        }
        final int[] blockToPalette = FaweCache.INSTANCE.BLOCK_TO_PALETTE.get();
        final int[] paletteToBlock = FaweCache.INSTANCE.PALETTE_TO_BLOCK.get();
        final long[] blockStates = FaweCache.INSTANCE.BLOCK_STATES.get();
        final int[] blocksCopy = FaweCache.INSTANCE.SECTION_BLOCKS.get();
        try {
            int[] num_palette_buffer = new int[1];
            Map<BlockVector3, Integer> ticking_blocks = new HashMap<>();
            int air;
            if (get == null) {
                air = createPalette(blockToPalette, paletteToBlock, blocksCopy, num_palette_buffer,
                        set, ticking_blocks, fastmode, adapter
                );
            } else {
                air = createPalette(layer, blockToPalette, paletteToBlock, blocksCopy,
                        num_palette_buffer, get, set, ticking_blocks, fastmode, adapter
                );
            }
            int num_palette = num_palette_buffer[0];
            // BlockStates

            int bitsPerEntry = MathMan.log2nlz(num_palette - 1);
            Object configuration =
                    PalettedContainer.Strategy.SECTION_STATES.getConfiguration(new FakeIdMapBlock(num_palette), bitsPerEntry);
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
            final Palette<net.minecraft.world.level.block.state.BlockState> blockStatePalette;
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
            LevelChunkSection levelChunkSection;
            try {
                //fieldStorage.set(dataPaletteBlocks, nmsBits);
                //fieldPalette.set(dataPaletteBlocks, blockStatePalettedContainer);
                if (biomes == null) {
                    biomes = new PalettedContainer<>(
                            biomeRegistry,
                            biomeRegistry.byIdOrThrow(WorldEditPlugin
                                    .getInstance()
                                    .getBukkitImplAdapter()
                                    .getInternalBiomeId(BiomeTypes.PLAINS)),
                            PalettedContainer.Strategy.SECTION_BIOMES,
                            null
                    );
                }
                levelChunkSection = new LevelChunkSection(layer, blockStatePalettedContainer, biomes);
                setCount(ticking_blocks.size(), 4096 - air, levelChunkSection);
                if (!fastmode) {
                    ticking_blocks.forEach((pos, ordinal) -> levelChunkSection.setBlockState(
                            pos.getBlockX(),
                            pos.getBlockY(),
                            pos.getBlockZ(),
                            Block.stateById(ordinal)
                    ));
                }
            } catch (final IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            return levelChunkSection;
        } catch (final Throwable e) {
            Arrays.fill(blockToPalette, Integer.MAX_VALUE);
            throw e;
        }
    }

    private static LevelChunkSection newChunkSection(
            int layer, IdMap<Holder<Biome>> biomeRegistry,
            @Nullable PalettedContainer<Holder<Biome>> biomes
    ) {
        PalettedContainer<net.minecraft.world.level.block.state.BlockState> dataPaletteBlocks = new PalettedContainer<>(
                Block.BLOCK_STATE_REGISTRY,
                Blocks.AIR.defaultBlockState(),
                PalettedContainer.Strategy.SECTION_STATES,
                null
        );
        PalettedContainer<Holder<Biome>> biomesPalette = biomes != null ? biomes : new PalettedContainer<>(
                biomeRegistry,
                biomeRegistry.byIdOrThrow(WorldEditPlugin
                        .getInstance()
                        .getBukkitImplAdapter()
                        .getInternalBiomeId(BiomeTypes.PLAINS)),
                PalettedContainer.Strategy.SECTION_BIOMES,
                null
        );
        return new LevelChunkSection(layer, dataPaletteBlocks, biomesPalette);
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
                PalettedContainer.Strategy.SECTION_BIOMES,
                null
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

    public static void setCount(final int tickingBlockCount, final int nonEmptyBlockCount, final LevelChunkSection section) throws
            IllegalAccessException {
        fieldTickingFluidCount.setShort(section, (short) 0); // TODO FIXME
        fieldTickingBlockCount.setShort(section, (short) tickingBlockCount);
        fieldNonEmptyBlockCount.setShort(section, (short) nonEmptyBlockCount);
    }

    public static Holder<Biome> adapt(Holder<Biome> biome, LevelAccessor levelAccessor) {
        final Registry<Biome> biomeRegistry = levelAccessor.registryAccess().ownedRegistryOrThrow(Registry.BIOME_REGISTRY);
        final IdMap<Holder<Biome>> holders = biomeRegistry.asHolderIdMap();
        ResourceLocation resourceLocation = biomeRegistry.getKey(biome.value());
        if (resourceLocation == null) {
            return holders.getId(biome) == -1 ? Holder.Reference.createStandAlone(biomeRegistry, Biomes.OCEAN)
                    : null;
        }
        return Holder.Reference.createStandAlone(biomeRegistry, ResourceKey.create(biomeRegistry.key(), resourceLocation));
    }

    @SuppressWarnings("unchecked")
    static void removeBeacon(BlockEntity beacon, LevelChunk levelChunk) {
        try {
            // Do the method ourselves to avoid trying to reflect generic method parameters
            // similar to removeGameEventListener
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
                                        ((Int2ObjectMap<GameEventDispatcher>) fieldGameEventDispatcherSections.get(levelChunk))
                                                .remove(i);
                                    } catch (IllegalAccessException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                    fieldRemove.set(beacon, true);
                }
            }
            methodremoveTickingBlockEntity.invoke(levelChunk, beacon.getBlockPos());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
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

        @NotNull
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

        @NotNull
        @Override
        public Iterator<Biome> iterator() {
            return Collections.emptyIterator();
        }

    }

}
