package com.boydti.fawe.bukkit.beta;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.implementation.SimpleCharQueueExtent;
import com.boydti.fawe.beta.implementation.SingleThreadQueueExtent;
import com.boydti.fawe.beta.implementation.WorldChunkCache;
import com.boydti.fawe.bukkit.adapter.v1_13_1.BlockMaterial_1_13;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.jnbt.anvil.BitArray4096;
import com.boydti.fawe.object.collection.IterableThreadLocal;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockID;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.jpountz.util.UnsafeUtils;
import net.minecraft.server.v1_13_R2.Block;
import net.minecraft.server.v1_13_R2.Chunk;
import net.minecraft.server.v1_13_R2.ChunkCoordIntPair;
import net.minecraft.server.v1_13_R2.ChunkProviderServer;
import net.minecraft.server.v1_13_R2.ChunkSection;
import net.minecraft.server.v1_13_R2.DataBits;
import net.minecraft.server.v1_13_R2.DataPalette;
import net.minecraft.server.v1_13_R2.DataPaletteBlock;
import net.minecraft.server.v1_13_R2.DataPaletteLinear;
import net.minecraft.server.v1_13_R2.GameProfileSerializer;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_13_R2.CraftChunk;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import sun.misc.Unsafe;

import static com.google.common.base.Preconditions.checkNotNull;

public class BukkitQueue extends SimpleCharQueueExtent {

    private org.bukkit.World bukkitWorld;
    private WorldServer nmsWorld;

    @Override
    public synchronized void init(WorldChunkCache cache) {
        World world = cache.getWorld();
        if (world instanceof BukkitWorld) {
            this.bukkitWorld = ((BukkitWorld) world).getWorld();
        } else {
            this.bukkitWorld = Bukkit.getWorld(world.getName());
        }
        checkNotNull(this.bukkitWorld);
        CraftWorld craftWorld = ((CraftWorld) bukkitWorld);
        this.nmsWorld = craftWorld.getHandle();
        super.init(cache);
    }

    public WorldServer getNmsWorld() {
        return nmsWorld;
    }

    public org.bukkit.World getBukkitWorld() {
        return bukkitWorld;
    }

    @Override
    protected synchronized void reset() {
        super.reset();
    }

//    private static final IterableThreadLocal<BukkitFullChunk> FULL_CHUNKS = new IterableThreadLocal<BukkitFullChunk>() {
//        @Override
//        public BukkitFullChunk init() {
//            return new BukkitFullChunk();
//        }
//    };

    @Override
    public IChunk create(boolean full) {
//        if (full) {
//            //TODO implement
//            return FULL_CHUNKS.get();
//        }
        return new BukkitChunkHolder();
    }

    /*
    NMS fields
    */
    public final static Field fieldBits;
    public final static Field fieldPalette;
    public final static Field fieldSize;

    public final static Field fieldFluidCount;
    public final static Field fieldTickingBlockCount;
    public final static Field fieldNonEmptyBlockCount;

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

            fieldFluidCount = ChunkSection.class.getDeclaredField("e");
            fieldFluidCount.setAccessible(true);
            fieldTickingBlockCount = ChunkSection.class.getDeclaredField("tickingBlockCount");
            fieldTickingBlockCount.setAccessible(true);
            fieldNonEmptyBlockCount = ChunkSection.class.getDeclaredField("nonEmptyBlockCount");
            fieldNonEmptyBlockCount.setAccessible(true);

            fieldLock = DataPaletteBlock.class.getDeclaredField("j");
            fieldLock.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            int modifiers = modifiersField.getInt(fieldLock);
            modifiers &= ~Modifier.FINAL;
            modifiersField.setInt(fieldLock, modifiers);

            Unsafe unsafe = UnsafeUtils.getUNSAFE();
            CHUNKSECTION_BASE = unsafe.arrayBaseOffset(ChunkSection[].class);
            int scale = unsafe.arrayIndexScale(ChunkSection[].class);
            if ((scale & (scale - 1)) != 0)
                throw new Error("data type scale not a power of two");
            CHUNKSECTION_SHIFT = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable rethrow) {
            rethrow.printStackTrace();
            throw new RuntimeException(rethrow);
        }
    }

    public static boolean setSectionAtomic(ChunkSection[] sections, ChunkSection expected, ChunkSection value, int layer) {
        long offset = ((long) layer << CHUNKSECTION_SHIFT) + CHUNKSECTION_BASE;
        if (layer >= 0 && layer < sections.length) {
            return UnsafeUtils.getUNSAFE().compareAndSwapObject(sections, offset, expected, value);
        }
        return false;
    }

    public static DelegateLock applyLock(ChunkSection section) {
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

    private static boolean PAPER = true;

    public Chunk ensureLoaded(int X, int Z) {
        return ensureLoaded(nmsWorld, X, Z);
    }

    public static Chunk ensureLoaded(net.minecraft.server.v1_13_R2.World nmsWorld, int X, int Z) {
        ChunkProviderServer provider = (ChunkProviderServer) nmsWorld.getChunkProvider();
        Chunk nmsChunk = provider.chunks.get(ChunkCoordIntPair.a(X, Z));
        if (nmsChunk != null) {
            return nmsChunk;
        }
        if (Fawe.isMainThread()) {
            return nmsWorld.getChunkAt(X, Z);
        }
        if (PAPER) {
            CraftWorld craftWorld = nmsWorld.getWorld();
            CompletableFuture<org.bukkit.Chunk> future = craftWorld.getChunkAtAsync(X, Z, true);
            try {
                CraftChunk chunk = (CraftChunk) future.get();
                return chunk.getHandle();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (Throwable e) {
                System.out.println("Error, cannot load chunk async (paper not installed?)");
                PAPER = false;
            }
        }
        // TODO optimize
        return TaskManager.IMP.sync(() -> nmsWorld.getChunkAt(X, Z));
    }

    /*
    NMS conversion
     */

    public static ChunkSection newChunkSection(final int layer, final boolean flag, final char[] blocks) {
        ChunkSection section = new ChunkSection(layer << 4, flag);
        if (blocks == null) {
            return section;
        }
        final int[] blockToPalette = FaweCache.BLOCK_TO_PALETTE.get();
        final int[] paletteToBlock = FaweCache.PALETTE_TO_BLOCK.get();
        final long[] blockstates = FaweCache.BLOCK_STATES.get();
        final int[] blocksCopy = FaweCache.SECTION_BLOCKS.get();
        try {
            int num_palette = 0;
            int air = 0;
            for (int i = 0; i < 4096; i++) {
                char ordinal = blocks[i];
                switch (ordinal) {
                    case 0:
                    case BlockID.AIR:
                    case BlockID.CAVE_AIR:
                    case BlockID.VOID_AIR:
                        air++;
                }
                int palette = blockToPalette[ordinal];
                if (palette == Integer.MAX_VALUE) {
                    blockToPalette[ordinal] = palette = num_palette;
                    paletteToBlock[num_palette] = ordinal;
                    num_palette++;
                }
                blocksCopy[i] = palette;
            }

            // BlockStates
            int bitsPerEntry = MathMan.log2nlz(num_palette - 1);
            if (Settings.IMP.PROTOCOL_SUPPORT_FIX || num_palette != 1) {
                bitsPerEntry = Math.max(bitsPerEntry, 4); // Protocol support breaks <4 bits per entry
            } else {
                bitsPerEntry = Math.max(bitsPerEntry, 1); // For some reason minecraft needs 4096 bits to store 0 entries
            }

            final int blockBitArrayEnd = (bitsPerEntry * 4096) >> 6;
            if (num_palette == 1) {
                for (int i = 0; i < blockBitArrayEnd; i++) blockstates[i] = 0;
            } else {
                final BitArray4096 bitArray = new BitArray4096(blockstates, bitsPerEntry);
                bitArray.fromRaw(blocksCopy);
            }

            // set palette & data bits
            final DataPaletteBlock<IBlockData> dataPaletteBlocks = section.getBlocks();
            // private DataPalette<T> h;
            // protected DataBits a;
            final long[] bits = Arrays.copyOfRange(blockstates, 0, blockBitArrayEnd);
            final DataBits nmsBits = new DataBits(bitsPerEntry, 4096, bits);
            final DataPalette<IBlockData> palette;
//                palette = new DataPaletteHash<>(Block.REGISTRY_ID, bitsPerEntry, dataPaletteBlocks, GameProfileSerializer::d, GameProfileSerializer::a);
            palette = new DataPaletteLinear<>(Block.REGISTRY_ID, bitsPerEntry, dataPaletteBlocks, GameProfileSerializer::d);

            // set palette
            for (int i = 0; i < num_palette; i++) {
                final int ordinal = paletteToBlock[i];
                blockToPalette[ordinal] = Integer.MAX_VALUE;
                final BlockState state = BlockTypes.states[ordinal];
                final IBlockData ibd = ((BlockMaterial_1_13) state.getMaterial()).getState();
                palette.a(ibd);
            }
            try {
                fieldBits.set(dataPaletteBlocks, nmsBits);
                fieldPalette.set(dataPaletteBlocks, palette);
                fieldSize.set(dataPaletteBlocks, bitsPerEntry);
                setCount(0, 4096 - air, section);
            } catch (final IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }

            return section;
        } catch (final Throwable e){
            Arrays.fill(blockToPalette, Integer.MAX_VALUE);
            throw e;
        }
    }

    public static void setCount(final int tickingBlockCount, final int nonEmptyBlockCount, final ChunkSection section) throws NoSuchFieldException, IllegalAccessException {
        fieldFluidCount.set(section, 0); // TODO FIXME
        fieldTickingBlockCount.set(section, tickingBlockCount);
        fieldNonEmptyBlockCount.set(section, nonEmptyBlockCount);
    }
}
