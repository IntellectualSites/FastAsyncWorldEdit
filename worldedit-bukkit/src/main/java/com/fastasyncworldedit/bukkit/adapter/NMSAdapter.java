package com.fastasyncworldedit.bukkit.adapter;

import com.fastasyncworldedit.core.FAWEPlatformAdapterImpl;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.util.MathMan;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Function;

public class NMSAdapter implements FAWEPlatformAdapterImpl {

    private static final Logger LOGGER = LogManager.getLogger();

    public static int createPalette(
            int[] blockToPalette,
            int[] paletteToBlock,
            int[] blocksCopy,
            char[] set,
            CachedBukkitAdapter adapter,
            short[] nonEmptyBlockCount
    ) {
        short nonAir = 4096;
        int num_palette = 0;
        for (int i = 0; i < 4096; i++) {
            char ordinal = set[i];
            switch (ordinal) {
                case BlockTypesCache.ReservedIDs.__RESERVED__ -> {
                    ordinal = BlockTypesCache.ReservedIDs.AIR;
                    nonAir--;
                }
                case BlockTypesCache.ReservedIDs.AIR, BlockTypesCache.ReservedIDs.CAVE_AIR, BlockTypesCache.ReservedIDs.VOID_AIR -> nonAir--;
            }
            int palette = blockToPalette[ordinal];
            if (palette == Integer.MAX_VALUE) {
                blockToPalette[ordinal] = num_palette;
                paletteToBlock[num_palette] = ordinal;
                num_palette++;
            }
        }
        int bitsPerEntry = MathMan.log2nlz(num_palette - 1);
        // If bits per entry is over 8, the game uses the global palette.
        if (bitsPerEntry > 8 && adapter != null) {
            // Cannot System#array copy char[] -> int[];
            for (int i = 0; i < adapter.getIbdToStateOrdinal().length; i++) {
                paletteToBlock[i] = adapter.getIbdToStateOrdinal()[i];
            }
            System.arraycopy(adapter.getOrdinalToIbdID(), 0, blockToPalette, 0, adapter.getOrdinalToIbdID().length);
        }
        for (int i = 0; i < 4096; i++) {
            char ordinal = set[i];
            if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                ordinal = BlockTypesCache.ReservedIDs.AIR;
            }
            int palette = blockToPalette[ordinal];
            blocksCopy[i] = palette;
        }

        if (nonEmptyBlockCount != null) {
            nonEmptyBlockCount[0] = nonAir;
        }
        return num_palette;
    }

    public static int createPalette(
            int layer,
            int[] blockToPalette,
            int[] paletteToBlock,
            int[] blocksCopy,
            Function<Integer, char[]> get,
            char[] set,
            CachedBukkitAdapter adapter,
            short[] nonEmptyBlockCount
    ) {
        short nonAir = 4096;
        int num_palette = 0;
        char[] getArr = null;
        for (int i = 0; i < 4096; i++) {
            char ordinal = set[i];
            switch (ordinal) {
                case BlockTypesCache.ReservedIDs.__RESERVED__ -> {
                    if (getArr == null) {
                        getArr = get.apply(layer);
                    }
                    // write to set array as this should be a copied array, and will be important when the changes are written
                    // to the GET chunk cached by FAWE. Future dords, actually read this comment please.
                    set[i] = switch (ordinal = getArr[i]) {
                        case BlockTypesCache.ReservedIDs.__RESERVED__ -> {
                            nonAir--;
                            yield (ordinal = BlockTypesCache.ReservedIDs.AIR);
                        }
                        case BlockTypesCache.ReservedIDs.AIR, BlockTypesCache.ReservedIDs.CAVE_AIR,
                                BlockTypesCache.ReservedIDs.VOID_AIR -> {
                            nonAir--;
                            yield ordinal;
                        }
                        default -> ordinal;
                    };
                }
                case BlockTypesCache.ReservedIDs.AIR, BlockTypesCache.ReservedIDs.CAVE_AIR, BlockTypesCache.ReservedIDs.VOID_AIR -> nonAir--;
            }
            int palette = blockToPalette[ordinal];
            if (palette == Integer.MAX_VALUE) {
                blockToPalette[ordinal] = num_palette;
                paletteToBlock[num_palette] = ordinal;
                num_palette++;
            }
        }
        int bitsPerEntry = MathMan.log2nlz(num_palette - 1);
        // If bits per entry is over 8, the game uses the global palette.
        if (bitsPerEntry > 8 && adapter != null) {
            // Cannot System#array copy char[] -> int[];
            for (int i = 0; i < adapter.getIbdToStateOrdinal().length; i++) {
                paletteToBlock[i] = adapter.getIbdToStateOrdinal()[i];
            }
            System.arraycopy(adapter.getOrdinalToIbdID(), 0, blockToPalette, 0, adapter.getOrdinalToIbdID().length);
        }
        for (int i = 0; i < 4096; i++) {
            char ordinal = set[i];
            if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                LOGGER.error("Empty (__RESERVED__) ordinal given where not expected, default to air.");
                ordinal = BlockTypesCache.ReservedIDs.AIR;
            }
            int palette = blockToPalette[ordinal];
            blocksCopy[i] = palette;
        }

        if (nonEmptyBlockCount != null) {
            nonEmptyBlockCount[0] = nonAir;
        }
        return num_palette;
    }

    @Override
    public void sendChunk(IChunkGet chunk, int mask, boolean lighting) {
        if (!(chunk instanceof BukkitGetBlocks)) {
            throw new IllegalArgumentException("(IChunkGet) chunk not of type BukkitGetBlocks");
        }
        ((BukkitGetBlocks) chunk).send();
    }

}
