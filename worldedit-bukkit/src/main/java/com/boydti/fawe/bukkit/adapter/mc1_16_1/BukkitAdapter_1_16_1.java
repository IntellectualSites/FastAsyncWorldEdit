package com.boydti.fawe.bukkit.adapter.mc1_16_1;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.adapter.DelegateLock;
import com.boydti.fawe.bukkit.adapter.NMSAdapter;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.collection.BitArrayUnstretched;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.ReflectionUtils;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import io.papermc.lib.PaperLib;
import net.jpountz.util.UnsafeUtils;
import net.minecraft.server.v1_16_R1.BiomeBase;
import net.minecraft.server.v1_16_R1.BiomeStorage;
import net.minecraft.server.v1_16_R1.Block;
import net.minecraft.server.v1_16_R1.Chunk;
import net.minecraft.server.v1_16_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R1.ChunkSection;
import net.minecraft.server.v1_16_R1.DataBits;
import net.minecraft.server.v1_16_R1.DataPalette;
import net.minecraft.server.v1_16_R1.DataPaletteBlock;
import net.minecraft.server.v1_16_R1.DataPaletteLinear;
import net.minecraft.server.v1_16_R1.GameProfileSerializer;
import net.minecraft.server.v1_16_R1.IBlockData;
import net.minecraft.server.v1_16_R1.PacketPlayOutLightUpdate;
import net.minecraft.server.v1_16_R1.PlayerChunk;
import net.minecraft.server.v1_16_R1.PlayerChunkMap;
import net.minecraft.server.v1_16_R1.World;
import net.minecraft.server.v1_16_R1.WorldServer;
import org.bukkit.craftbukkit.v1_16_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_16_R1.CraftWorld;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public final class BukkitAdapter_1_16_1 extends NMSAdapter {
    /*
    NMS fields
    */
    public static final Field fieldBits;
    public static final Field fieldPalette;
    public static final Field fieldSize;

    public static final Field fieldBitsPerEntry;

    public static final Field fieldFluidCount;
    public static final Field fieldTickingBlockCount;
    public static final Field fieldNonEmptyBlockCount;

    private static final Field fieldDirtyCount;
    private static final Field fieldDirtyBits;

    private static final Field fieldBiomeArray;

    private final static MethodHandle methodGetVisibleChunk;

    private static final int CHUNKSECTION_BASE;
    private static final int CHUNKSECTION_SHIFT;

    private static final Field fieldLock;

    static {
        try {
            fieldSize = DataPaletteBlock.class.getDeclaredField("i");
            fieldSize.setAccessible(true);
            fieldBits = DataPaletteBlock.class.getDeclaredField("a");
            fieldBits.setAccessible(true);
            fieldPalette = DataPaletteBlock.class.getDeclaredField("h");
            fieldPalette.setAccessible(true);

            fieldBitsPerEntry = DataBits.class.getDeclaredField("c");
            fieldBitsPerEntry.setAccessible(true);

            fieldFluidCount = ChunkSection.class.getDeclaredField("e");
            fieldFluidCount.setAccessible(true);
            fieldTickingBlockCount = ChunkSection.class.getDeclaredField("tickingBlockCount");
            fieldTickingBlockCount.setAccessible(true);
            fieldNonEmptyBlockCount = ChunkSection.class.getDeclaredField("nonEmptyBlockCount");
            fieldNonEmptyBlockCount.setAccessible(true);

            fieldDirtyCount = PlayerChunk.class.getDeclaredField("dirtyCount");
            fieldDirtyCount.setAccessible(true);
            fieldDirtyBits = PlayerChunk.class.getDeclaredField("r");
            fieldDirtyBits.setAccessible(true);

            fieldBiomeArray = BiomeStorage.class.getDeclaredField("g");
            fieldBiomeArray.setAccessible(true);

            Method declaredGetVisibleChunk = PlayerChunkMap.class.getDeclaredMethod("getVisibleChunk", long.class);
            declaredGetVisibleChunk.setAccessible(true);
            methodGetVisibleChunk = MethodHandles.lookup().unreflect(declaredGetVisibleChunk);

            Field tmp = DataPaletteBlock.class.getDeclaredField("j");
            ReflectionUtils.setAccessibleNonFinal(tmp);
            fieldLock = tmp;
            fieldLock.setAccessible(true);

            Unsafe unsafe = UnsafeUtils.getUNSAFE();
            CHUNKSECTION_BASE = unsafe.arrayBaseOffset(ChunkSection[].class);
            int scale = unsafe.arrayIndexScale(ChunkSection[].class);
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

    protected static boolean setSectionAtomic(ChunkSection[] sections, ChunkSection expected, ChunkSection value, int layer) {
        long offset = ((long) layer << CHUNKSECTION_SHIFT) + CHUNKSECTION_BASE;
        if (layer >= 0 && layer < sections.length) {
            return UnsafeUtils.getUNSAFE().compareAndSwapObject(sections, offset, expected, value);
        }
        return false;
    }

    protected static DelegateLock applyLock(ChunkSection section) {
        //todo there has to be a better way to do this. Maybe using a() in DataPaletteBlock which acquires the lock in NMS?
        try {
            synchronized (section) {
                DataPaletteBlock<IBlockData> blocks = section.getBlocks();
                ReentrantLock currentLock = (ReentrantLock) fieldLock.get(blocks);
                if (currentLock instanceof DelegateLock) {
                    return (DelegateLock) currentLock;
                }
                DelegateLock newLock = new DelegateLock(currentLock);
                fieldLock.set(blocks, newLock);
                return newLock;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static Chunk ensureLoaded(World nmsWorld, int chunkX, int chunkZ) {
        Chunk nmsChunk = nmsWorld.getChunkProvider().getChunkAt(chunkX, chunkZ, false);
        if (nmsChunk != null) {
            return nmsChunk;
        }
        if (Fawe.isMainThread()) {
            return nmsWorld.getChunkAt(chunkX, chunkZ);
        }
        if (PaperLib.isPaper()) {
            CraftWorld craftWorld = nmsWorld.getWorld();
            CompletableFuture<org.bukkit.Chunk> future = craftWorld.getChunkAtAsync(chunkX, chunkZ, true);
            try {
                CraftChunk chunk = (CraftChunk) future.get();
                return chunk.getHandle();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        // TODO optimize
        return TaskManager.IMP.sync(() -> nmsWorld.getChunkAt(chunkX, chunkZ));
    }

    public static PlayerChunk getPlayerChunk(WorldServer nmsWorld, final int cx, final int cz) {
        PlayerChunkMap chunkMap = nmsWorld.getChunkProvider().playerChunkMap;
        try {
            return (PlayerChunk) methodGetVisibleChunk.invoke(chunkMap, ChunkCoordIntPair.pair(cx, cz));
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    public static void sendChunk(WorldServer nmsWorld, int chunkX, int chunkZ, int mask, boolean lighting) {
        PlayerChunk playerChunk = getPlayerChunk(nmsWorld, chunkX, chunkZ);
        if (playerChunk == null) {
            return;
        }
        if (playerChunk.hasBeenLoaded()) {
            try {
                int dirtyBits = fieldDirtyBits.getInt(playerChunk);
                if (dirtyBits == 0) {
                    nmsWorld.getChunkProvider().playerChunkMap.a(playerChunk);
                }
                if (mask == 0) {
                    dirtyBits = 65535;
                } else {
                    dirtyBits |= mask;
                }

                fieldDirtyBits.set(playerChunk, dirtyBits);
                fieldDirtyCount.set(playerChunk, 64);

                if (lighting) {
                    ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(chunkX, chunkZ);
                    boolean trustEdges = false; //Added in 1.16.1 Not sure what it does.
                    PacketPlayOutLightUpdate packet = new PacketPlayOutLightUpdate(chunkCoordIntPair, nmsWorld.getChunkProvider().getLightEngine(), trustEdges);
                    playerChunk.players.a(chunkCoordIntPair, false).forEach(p -> {
                        p.playerConnection.sendPacket(packet);
                    });
                }

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /*
    NMS conversion
     */
    public static ChunkSection newChunkSection(final int layer, final char[] blocks, boolean fastmode) {
        return newChunkSection(layer, null, blocks, fastmode);
    }

    public static ChunkSection newChunkSection(final int layer, final Function<Integer, char[]> get, char[] set, boolean fastmode) {
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
                        set, ticking_blocks, fastmode);
            } else {
                air = createPalette(layer, blockToPalette, paletteToBlock, blocksCopy,
                        num_palette_buffer, get, set, ticking_blocks, fastmode);
            }
            int num_palette = num_palette_buffer[0];
            // BlockStates
            int bitsPerEntry = MathMan.log2nlz(num_palette - 1);
            if (Settings.IMP.PROTOCOL_SUPPORT_FIX || num_palette != 1) {
                bitsPerEntry = Math.max(bitsPerEntry, 4); // Protocol support breaks <4 bits per entry
            } else {
                bitsPerEntry = Math.max(bitsPerEntry, 1); // For some reason minecraft needs 4096 bits to store 0 entries
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

            ChunkSection section = newChunkSection(layer);
            // set palette & data bits
            final DataPaletteBlock<IBlockData> dataPaletteBlocks = section.getBlocks();
            // private DataPalette<T> h;
            // protected DataBits a;
            final long[] bits = Arrays.copyOfRange(blockStates, 0, blockBitArrayEnd);
            final DataBits nmsBits = new DataBits(bitsPerEntry, 4096, bits);
            final DataPalette<IBlockData> palette;
            palette = new DataPaletteLinear<>(Block.REGISTRY_ID, bitsPerEntry, dataPaletteBlocks, GameProfileSerializer::c);

            // set palette
            for (int i = 0; i < num_palette; i++) {
                final int ordinal = paletteToBlock[i];
                blockToPalette[ordinal] = Integer.MAX_VALUE;
                final BlockState state = BlockTypesCache.states[ordinal];
                final IBlockData ibd = ((BlockMaterial_1_16_1) state.getMaterial()).getState();
                palette.a(ibd);
            }
            try {
                fieldBits.set(dataPaletteBlocks, nmsBits);
                fieldPalette.set(dataPaletteBlocks, palette);
                fieldSize.set(dataPaletteBlocks, bitsPerEntry);
                setCount(ticking_blocks.size(), 4096 - air, section);
                if (!fastmode) {
                    ticking_blocks.forEach((pos, ordinal) -> section
                            .setType(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(),
                                    Block.getByCombinedId(ordinal)));
                }
            } catch (final IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }

            return section;
        } catch (final Throwable e) {
            Arrays.fill(blockToPalette, Integer.MAX_VALUE);
            throw e;
        }
    }

    private static ChunkSection newChunkSection(int layer) {
        return new ChunkSection(layer << 4);
    }

    public static void setCount(final int tickingBlockCount, final int nonEmptyBlockCount, final ChunkSection section) throws NoSuchFieldException, IllegalAccessException {
        fieldFluidCount.setShort(section, (short) 0); // TODO FIXME
        fieldTickingBlockCount.setShort(section, (short) tickingBlockCount);
        fieldNonEmptyBlockCount.setShort(section, (short) nonEmptyBlockCount);
    }

    public static BiomeBase[] getBiomeArray(BiomeStorage storage) {
        try {
            return (BiomeBase[]) fieldBiomeArray.get(storage);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}
