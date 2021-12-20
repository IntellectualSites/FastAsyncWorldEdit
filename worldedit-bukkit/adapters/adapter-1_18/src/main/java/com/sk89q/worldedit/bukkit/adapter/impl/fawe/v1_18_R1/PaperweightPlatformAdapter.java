package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_18_R1;

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
import com.sk89q.worldedit.bukkit.adapter.Refraction;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import io.papermc.lib.PaperLib;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
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
import org.bukkit.craftbukkit.v1_18_R1.CraftChunk;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PaperweightPlatformAdapter extends NMSAdapter {

    public static final Field fieldData;

    public static final Constructor<?> dataConstructor;

    public static final Field fieldStorage;
    public static final Field fieldPalette;


    public static final Field fieldTickingFluidContent;
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

            //TODO FIXME 1.18
            fieldStorage = dataClazz.getDeclaredField(Refraction.pickName("storage", "b"));
            fieldStorage.setAccessible(true);
            fieldPalette = dataClazz.getDeclaredField(Refraction.pickName("palette", "c"));
            fieldPalette.setAccessible(true);

            fieldTickingFluidContent = LevelChunkSection.class.getDeclaredField(Refraction.pickName("tickingFluidCount", "h"));
            fieldTickingFluidContent.setAccessible(true);
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
                            "m"
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
                ThreadingDetector currentThreadingDetector = (ThreadingDetector) unsafe.getObject(blocks,
                        fieldThreadingDetectorOffset) ;
                synchronized(currentThreadingDetector) {
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
        return TaskManager.IMP.sync(() -> serverLevel.getChunk(chunkX, chunkZ));
    }

    public static ChunkHolder getPlayerChunk(ServerLevel nmsWorld, final int chunkX, final int chunkZ) {
        ChunkMap chunkMap = nmsWorld.getChunkSource().chunkMap;
        try {
            return (ChunkHolder) methodGetVisibleChunk.invoke(chunkMap, ChunkPos.asLong(chunkX, chunkZ));
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

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
        TaskManager.IMP.task(() -> {
            ClientboundLevelChunkWithLightPacket packet =
                    new ClientboundLevelChunkWithLightPacket(levelChunk, nmsWorld.getChunkSource().getLightEngine(), null, null
                            , true, false); // last false is to not bother with x-ray
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
            CachedBukkitAdapter adapter, Registry<Biome> biomeRegistry,
            @Nullable PalettedContainer<Biome> biomes
    ) {
        return newChunkSection(layer, null, blocks, fastmode, adapter, biomeRegistry, biomes);
    }

    public static LevelChunkSection newChunkSection(
            final int layer, final Function<Integer, char[]> get, char[] set,
            boolean fastmode, CachedBukkitAdapter adapter, Registry<Biome> biomeRegistry,
            @Nullable PalettedContainer<Biome> biomes
    ) {
        if (set == null) {
            return newChunkSection(layer, biomeRegistry, biomes);
        }
        final int[] blockToPalette = FaweCache.IMP.BLOCK_TO_PALETTE.get();
        final int[] paletteToBlock = FaweCache.IMP.PALETTE_TO_BLOCK.get();
        final long[] blockStates = FaweCache.IMP.BLOCK_STATES.get();
        final int[] blocksCopy = FaweCache.IMP.SECTION_BLOCKS.get();
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

            // set palette & data bits

            final PalettedContainer<net.minecraft.world.level.block.state.BlockState> blockStatePalettedContainer =
                    new PalettedContainer<>(
                            Block.BLOCK_STATE_REGISTRY,
                            Blocks.AIR.defaultBlockState(),
                            PalettedContainer.Strategy.SECTION_STATES,
                            null
                    );
            // private DataPalette<T> h;
            // protected DataBits a;
            final long[] bits = Arrays.copyOfRange(blockStates, 0, blockBitArrayEnd);
            final BitStorage nmsBits;
            if (bitsPerEntry == 0) {
                nmsBits = new ZeroBitStorage(4096);
            } else {
                nmsBits = new SimpleBitStorage(bitsPerEntry, 4096, bits);
            }
            final Palette<net.minecraft.world.level.block.state.BlockState> blockStatePalette;
            if (bitsPerEntry == 0) {
                blockStatePalette = new SingleValuePalette<>(
                        blockStatePalettedContainer.registry,
                        blockStatePalettedContainer,
                        List.of()
                );
            } else if (bitsPerEntry == 4) {
                blockStatePalette = LinearPalette.create(
                        4,
                        blockStatePalettedContainer.registry,
                        blockStatePalettedContainer,
                        List.of()
                );
            } else if (bitsPerEntry < 9) {
                blockStatePalette = HashMapPalette.create(
                        bitsPerEntry,
                        blockStatePalettedContainer.registry,
                        blockStatePalettedContainer,
                        List.of()
                );
            } else {
                blockStatePalette = GlobalPalette.create(
                        bitsPerEntry,
                        blockStatePalettedContainer.registry,
                        blockStatePalettedContainer,
                        List.of());
            }

            // set palette if required
            if (bitsPerEntry < 9) {
                for (int i = 0; i < num_palette; i++) {
                    final int ordinal = paletteToBlock[i];
                    blockToPalette[ordinal] = Integer.MAX_VALUE;
                    final BlockState state = BlockTypesCache.states[ordinal];
                    final net.minecraft.world.level.block.state.BlockState blockState = ((PaperweightBlockMaterial) state.getMaterial()).getState();
                    blockStatePalette.idFor(blockState);
                }
            }
            LevelChunkSection levelChunkSection;
            try {
                Object data = dataConstructor.newInstance(configuration, nmsBits, blockStatePalette);
                fieldData.set(blockStatePalettedContainer, data);
                //fieldStorage.set(dataPaletteBlocks, nmsBits);
                //fieldPalette.set(dataPaletteBlocks, blockStatePalettedContainer);
                if (biomes == null) {
                    biomes = new PalettedContainer<>(
                            biomeRegistry,
                            biomeRegistry.getOrThrow(Biomes.PLAINS),
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
            } catch (final IllegalAccessException | InvocationTargetException | InstantiationException e) {
                throw new RuntimeException(e);
            }

            return levelChunkSection;
        } catch (final Throwable e) {
            Arrays.fill(blockToPalette, Integer.MAX_VALUE);
            throw e;
        }
    }

    private static LevelChunkSection newChunkSection(
            int layer, Registry<Biome> biomeRegistry,
            @Nullable PalettedContainer<Biome> biomes
    ) {
        PalettedContainer<net.minecraft.world.level.block.state.BlockState> dataPaletteBlocks = new PalettedContainer<>(
                Block.BLOCK_STATE_REGISTRY,
                Blocks.AIR.defaultBlockState(),
                PalettedContainer.Strategy.SECTION_STATES,
                null
        );
        PalettedContainer<Biome> biomesPalette = biomes != null ? biomes : new PalettedContainer<>(
                biomeRegistry,
                biomeRegistry.getOrThrow(Biomes.PLAINS),
                PalettedContainer.Strategy.SECTION_BIOMES,
                null
        );
        return new LevelChunkSection(layer, dataPaletteBlocks, biomesPalette);
    }

    /**
     * Create a new {@link PalettedContainer<Biome>}. Should only be used if no biome container existed beforehand.
     */
    public static PalettedContainer<Biome> getBiomePalettedContainer(BiomeType[] biomes, Registry<Biome> biomeRegistry) {
        if (biomes == null) {
            return null;
        }
        // Don't stream this as typically will see 1-4 biomes; stream overhead is large for the small length
        Map<BiomeType, Biome> palette = new HashMap<>();
        for (BiomeType biomeType : new LinkedList<>(Arrays.asList(biomes))) {
            Biome biome;
            if (biomeType == null) {
                biome = biomeRegistry.getOrThrow(Biomes.PLAINS);
            } else {
                biome = biomeRegistry.get(ResourceLocation.tryParse(biomeType.getId()));
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
        PalettedContainer<Biome> biomePalettedContainer = new PalettedContainer<>(
                biomeRegistry,
                biomeRegistry.getOrThrow(Biomes.PLAINS),
                PalettedContainer.Strategy.SECTION_BIOMES,
                null
        );

        final Palette<Biome> biomePalette;
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
                        Biome biome = biomeRegistry.get(ResourceLocation.tryParse(biomeType.getId()));
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
        fieldTickingFluidContent.setShort(section, (short) 0); // TODO FIXME
        fieldTickingBlockCount.setShort(section, (short) tickingBlockCount);
        fieldNonEmptyBlockCount.setShort(section, (short) nonEmptyBlockCount);
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

    static class FakeIdMapBlock implements IdMap<net.minecraft.world.level.block.state.BlockState> {

        private final int size;

        FakeIdMapBlock(int size) {
            this.size = size;
        }

        @Override
        public int getId(final net.minecraft.world.level.block.state.BlockState entry) {
            return 0;
        }

        @Nullable
        @Override
        public net.minecraft.world.level.block.state.BlockState byId(final int index) {
            return null;
        }

        @Override
        public int size() {
            return size;
        }

        @NotNull
        @Override
        public Iterator<net.minecraft.world.level.block.state.BlockState> iterator() {
            return Collections.emptyIterator();
        }

    }

    static class FakeIdMapBiome implements IdMap<Biome> {

        private final int size;

        FakeIdMapBiome(int size) {
            this.size = size;
        }

        @Override
        public int getId(final Biome entry) {
            return 0;
        }

        @Nullable
        @Override
        public Biome byId(final int index) {
            return null;
        }

        @Override
        public int size() {
            return size;
        }

        @NotNull
        @Override
        public Iterator<Biome> iterator() {
            return Collections.emptyIterator();
        }

    }

}
