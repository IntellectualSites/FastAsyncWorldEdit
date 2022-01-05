package com.fastasyncworldedit.bukkit.adapter;

import com.fastasyncworldedit.core.FAWEPlatformAdapterImpl;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.util.MathMan;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import java.util.Map;
import java.util.function.Function;

public class NMSAdapter implements FAWEPlatformAdapterImpl {

    public static int createPalette(
            int[] blockToPalette, int[] paletteToBlock, int[] blocksCopy,
            int[] num_palette_buffer, char[] set, Map<BlockVector3, Integer> ticking_blocks, boolean fastmode,
            CachedBukkitAdapter adapter
    ) {
        int air = 0;
        int num_palette = 0;
        for (int i = 0; i < 4096; i++) {
            char ordinal = set[i];
            if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                ordinal = BlockTypesCache.ReservedIDs.AIR;
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
        char lastOrdinal = 0;
        boolean lastticking = false;
        boolean tick_placed = Settings.settings().EXPERIMENTAL.ALLOW_TICK_PLACED;
        for (int i = 0; i < 4096; i++) {
            char ordinal = set[i];
            switch (ordinal) {
                case BlockTypesCache.ReservedIDs.__RESERVED__:
                    ordinal = BlockTypesCache.ReservedIDs.AIR;
                case BlockTypesCache.ReservedIDs.AIR, BlockTypesCache.ReservedIDs.CAVE_AIR, BlockTypesCache.ReservedIDs.VOID_AIR:
                    air++;
                    break;
                default:
                    if (!fastmode && !tick_placed) {
                        boolean ticking;
                        if (ordinal != lastOrdinal) {
                            ticking = BlockTypesCache.ticking[ordinal];
                            lastOrdinal = ordinal;
                            lastticking = ticking;
                        } else {
                            ticking = lastticking;
                        }
                        if (ticking) {
                            BlockState state = BlockState.getFromOrdinal(ordinal);
                            ticking_blocks
                                    .put(
                                            BlockVector3.at(i & 15, (i >> 8) & 15, (i >> 4) & 15),
                                            WorldEditPlugin.getInstance().getBukkitImplAdapter()
                                                    .getInternalBlockStateId(state).orElse(0)
                                    );
                        }
                    }
            }
            int palette = blockToPalette[ordinal];
            blocksCopy[i] = palette;
        }
        num_palette_buffer[0] = num_palette;
        return air;
    }

    public static int createPalette(
            int layer, int[] blockToPalette, int[] paletteToBlock,
            int[] blocksCopy, int[] num_palette_buffer, Function<Integer, char[]> get, char[] set,
            Map<BlockVector3, Integer> ticking_blocks, boolean fastmode,
            CachedBukkitAdapter adapter
    ) {
        int air = 0;
        int num_palette = 0;
        char[] getArr = null;
        for (int i = 0; i < 4096; i++) {
            char ordinal = set[i];
            if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                if (getArr == null) {
                    getArr = get.apply(layer);
                }
                ordinal = getArr[i];
                if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                    ordinal = BlockTypesCache.ReservedIDs.AIR;
                }
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
        char lastOrdinal = BlockTypesCache.ReservedIDs.__RESERVED__;
        boolean lastticking = false;
        boolean tick_placed = Settings.settings().EXPERIMENTAL.ALLOW_TICK_PLACED;
        boolean tick_existing = Settings.settings().EXPERIMENTAL.ALLOW_TICK_EXISTING;
        for (int i = 0; i < 4096; i++) {
            char ordinal = set[i];
            switch (ordinal) {
                case BlockTypesCache.ReservedIDs.__RESERVED__ -> {
                    if (getArr == null) {
                        getArr = get.apply(layer);
                    }
                    set[i] = switch (ordinal = getArr[i]) {
                        case BlockTypesCache.ReservedIDs.__RESERVED__:
                            ordinal = BlockTypesCache.ReservedIDs.AIR;
                        case BlockTypesCache.ReservedIDs.AIR, BlockTypesCache.ReservedIDs.CAVE_AIR, BlockTypesCache.ReservedIDs.VOID_AIR:
                            air++;
                            yield ordinal;
                        default:
                            if (!fastmode && !tick_placed && tick_existing) {
                                boolean ticking;
                                if (ordinal != lastOrdinal) {
                                    ticking = BlockTypesCache.ticking[ordinal];
                                    lastOrdinal = ordinal;
                                    lastticking = ticking;
                                } else {
                                    ticking = lastticking;
                                }
                                if (ticking) {
                                    BlockState state = BlockState.getFromOrdinal(ordinal);
                                    ticking_blocks.put(BlockVector3.at(i & 15, (i >> 8) & 15, (i >> 4) & 15),
                                            WorldEditPlugin
                                                    .getInstance()
                                                    .getBukkitImplAdapter()
                                                    .getInternalBlockStateId(state)
                                                    .orElse(0)
                                    );
                                }
                            }
                            yield ordinal;
                    };
                }
                case BlockTypesCache.ReservedIDs.AIR, BlockTypesCache.ReservedIDs.CAVE_AIR, BlockTypesCache.ReservedIDs.VOID_AIR -> air++;
            }
            if (!fastmode && tick_placed) {
                boolean ticking;
                if (ordinal != lastOrdinal) {
                    ticking = BlockTypesCache.ticking[ordinal];
                    lastOrdinal = ordinal;
                    lastticking = ticking;
                } else {
                    ticking = lastticking;
                }
                if (ticking) {
                    BlockState state = BlockState.getFromOrdinal(ordinal);
                    ticking_blocks.put(
                            BlockVector3.at(i & 15, (i >> 8) & 15, (i >> 4) & 15),
                            WorldEditPlugin.getInstance().getBukkitImplAdapter()
                                    .getInternalBlockStateId(state).orElse(0)
                    );
                }
            }
            int palette = blockToPalette[ordinal];
            blocksCopy[i] = palette;
        }

        num_palette_buffer[0] = num_palette;
        return air;
    }

    @Override
    public void sendChunk(IChunkGet chunk, int mask, boolean lighting) {
        if (!(chunk instanceof BukkitGetBlocks)) {
            throw new IllegalArgumentException("(IChunkGet) chunk not of type BukkitGetBlocks");
        }
        ((BukkitGetBlocks) chunk).send(mask, lighting);
    }

}
