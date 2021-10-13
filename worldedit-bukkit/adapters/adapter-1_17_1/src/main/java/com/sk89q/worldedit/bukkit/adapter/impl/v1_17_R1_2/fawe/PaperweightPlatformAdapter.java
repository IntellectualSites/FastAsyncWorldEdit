package com.sk89q.worldedit.bukkit.adapter.impl.v1_17_R1_2.fawe;

import com.fastasyncworldedit.bukkit.adapter.CachedBukkitAdapter;
import com.fastasyncworldedit.bukkit.adapter.DelegateSemaphore;
import com.fastasyncworldedit.bukkit.adapter.NMSAdapter;
import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.math.BitArrayUnstretched;
import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.TaskManager;
import com.fastasyncworldedit.core.util.UnsafeUtility;
import com.mojang.datafixers.util.Either;
import com.sk89q.worldedit.math.BlockVector3;
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
import net.minecraft.util.BitStorage;
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
import org.bukkit.craftbukkit.v1_17_R1.CraftChunk;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.stream.Stream;

public final class PaperweightPlatformAdapter extends NMSAdapter {

    public static final Field fieldStorage;
    public static final Field fieldPalette;
    public static final Field fieldSize;

    public static final Field fieldBitsPerEntry;

    public static final Field fieldFluidCount;
    public static final Field fieldTickingBlockCount;
    public static final Field fieldNonEmptyBlockCount;

    private static final Field fieldBiomeArray;

    private static final MethodHandle methodGetVisibleChunk;

    private static final int CHUNKSECTION_BASE;
    private static final int CHUNKSECTION_SHIFT;

    private static final Field fieldLock;
    private static final long fieldLockOffset;

    private static final Field fieldEventDispatcherMap;
    private static final MethodHandle methodremoveTickingBlockEntity;

    private static final Field fieldTileEntityRemoved;

    static {
        try {
            // TODO
            fieldSize = PalettedContainer.class.getDeclaredField("bits");
            fieldSize.setAccessible(true);
            fieldStorage = PalettedContainer.class.getDeclaredField("storage");
            fieldStorage.setAccessible(true);
            fieldPalette = PalettedContainer.class.getDeclaredField("palette");
            fieldPalette.setAccessible(true);

            fieldBitsPerEntry = BitStorage.class.getDeclaredField("bits");
            fieldBitsPerEntry.setAccessible(true);

            fieldFluidCount = LevelChunkSection.class.getDeclaredField("tickingFluidCount");
            fieldFluidCount.setAccessible(true);
            fieldTickingBlockCount = LevelChunkSection.class.getDeclaredField("tickingBlockCount");
            fieldTickingBlockCount.setAccessible(true);
            fieldNonEmptyBlockCount = LevelChunkSection.class.getDeclaredField("nonEmptyBlockCount");
            fieldNonEmptyBlockCount.setAccessible(true);

            fieldBiomeArray = ChunkBiomeContainer.class.getDeclaredField("biomes");
            fieldBiomeArray.setAccessible(true);

            Method declaredGetVisibleChunk = ChunkMap.class.getDeclaredMethod("getVisibleChunkIfPresent", long.class);
            declaredGetVisibleChunk.setAccessible(true);
            methodGetVisibleChunk = MethodHandles.lookup().unreflect(declaredGetVisibleChunk);

            Unsafe unsafe = UnsafeUtility.getUNSAFE();
            fieldLock = PalettedContainer.class.getDeclaredField("lock");
            fieldLockOffset = unsafe.objectFieldOffset(fieldLock);

            fieldEventDispatcherMap = LevelChunk.class.getDeclaredField("gameEventDispatcherSections");
            fieldEventDispatcherMap.setAccessible(true);
            //TODO paperweight Method -> Field?
            Method removeTickingBlockEntity = LevelChunk.class.getDeclaredMethod("blockEntities", BlockPos.class);
            removeTickingBlockEntity.setAccessible(true);
            methodremoveTickingBlockEntity = MethodHandles.lookup().unreflect(removeTickingBlockEntity);

            fieldTileEntityRemoved = BlockEntity.class.getDeclaredField("remove");
            fieldTileEntityRemoved.setAccessible(true);

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
            return UnsafeUtility.getUNSAFE().compareAndSwapObject(sections, offset, expected, value);
        }
        return false;
    }

    static DelegateSemaphore applyLock(LevelChunkSection section) {
        //todo there has to be a better way to do this. Maybe using a() in DataPaletteBlock which acquires the lock in NMS?
        try {
            synchronized (section) {
                Unsafe unsafe = UnsafeUtility.getUNSAFE();
                PalettedContainer<net.minecraft.world.level.block.state.BlockState> blocks = section.getStates();
                Semaphore currentLock = (Semaphore) unsafe.getObject(blocks, fieldLockOffset);
                if (currentLock instanceof DelegateSemaphore) {
                    return (DelegateSemaphore) currentLock;
                }
                DelegateSemaphore newLock = new DelegateSemaphore(1, currentLock);
                unsafe.putObject(blocks, fieldLockOffset, newLock);
                return newLock;
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
            final int layer, final char[] blocks, boolean fastmode,
            CachedBukkitAdapter adapter
    ) {
        return newChunkSection(layer, null, blocks, fastmode, adapter);
    }

    public static LevelChunkSection newChunkSection(
            final int layer, final Function<Integer, char[]> get, char[] set,
            boolean fastmode, CachedBukkitAdapter adapter
    ) {
        if (set == null) {
            return newChunkSection(layer);
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
            if (Settings.IMP.PROTOCOL_SUPPORT_FIX || num_palette != 1) {
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
                fieldStorage.set(dataPaletteBlocks, nmsBits);
                fieldPalette.set(dataPaletteBlocks, blockStatePalettedContainer);
                fieldSize.set(dataPaletteBlocks, bitsPerEntry);
                setCount(ticking_blocks.size(), 4096 - air, levelChunkSection);
                if (!fastmode) {
                    ticking_blocks.forEach((pos, ordinal) -> levelChunkSection
                            .setBlockState(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(),
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

    private static LevelChunkSection newChunkSection(int layer) {
        return new LevelChunkSection(layer);
    }

    public static void setCount(final int tickingBlockCount, final int nonEmptyBlockCount, final LevelChunkSection section) throws
            IllegalAccessException {
        fieldFluidCount.setShort(section, (short) 0); // TODO FIXME
        fieldTickingBlockCount.setShort(section, (short) tickingBlockCount);
        fieldNonEmptyBlockCount.setShort(section, (short) nonEmptyBlockCount);
    }

    public static Biome[] getBiomeArray(ChunkBiomeContainer chunkBiomeContainer) {
        try {
            return (Biome[]) fieldBiomeArray.get(chunkBiomeContainer);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
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
                                        ((Int2ObjectMap<GameEventDispatcher>) fieldEventDispatcherMap.get(levelChunk))
                                                .remove(i);
                                    } catch (IllegalAccessException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                    fieldTileEntityRemoved.set(beacon, true);
                }
            }
            methodremoveTickingBlockEntity.invoke(levelChunk, beacon.getBlockPos());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

}
