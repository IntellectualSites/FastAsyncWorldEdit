package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_18_R1;

import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PaperweightPostProcesssor implements IBatchProcessor {

    @Override
    public IChunkSet processSet(final IChunk chunk, final IChunkGet get, final IChunkSet set) {
        return set;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void postProcessSet(final IChunk chunk, final IChunkGet iChunkGet, final IChunkSet iChunkSet) {
        boolean tickExisting = Settings.settings().EXPERIMENTAL.ALLOW_TICK_EXISTING;
        boolean tickPlaced = Settings.settings().EXPERIMENTAL.ALLOW_TICK_PLACED;
        boolean tickFluid = Settings.settings().EXPERIMENTAL.ALLOW_TICK_FLUIDS;
        // The PostProcessor shouldn't be added, but just in case
        if (!tickExisting && !tickPlaced && !tickFluid) {
            return;
        }
        PaperweightGetBlocks_Copy getBlocks = (PaperweightGetBlocks_Copy) iChunkGet;
        layer: for (int layer = iChunkSet.getMinSectionPosition(); layer <= iChunkSet.getMaxSectionPosition(); layer++) {
            Map<BlockPos, Integer> tickingBlocks = tickExisting || tickPlaced ? new HashMap<>() : null;
            Map<BlockPos, Fluid> fluidBlocks = tickFluid ? new HashMap<>() : null;
            int existingFluidCount = 0;
            char[] set = iChunkSet.load(layer);
            char[] get = null;
            for (int i = 0; i < 4096; i++) {
                char ordinal = BlockTypesCache.ReservedIDs.__RESERVED__;
                char replacedOrdinal = BlockTypesCache.ReservedIDs.__RESERVED__;
                if (tickPlaced || tickFluid) {
                    ordinal = set[i];
                }
                boolean fromGet = false; // Used for liquids
                if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__ && (tickExisting || tickFluid)) {
                    if (get == null) {
                        get = getBlocks.load(layer);
                    }
                    // If this is null, then it's because we're loading a layer in the range of 0->15, but blocks aren't
                    // actually being set
                    if (get == null) {
                        continue layer;
                    }
                    fromGet = true;
                    ordinal = replacedOrdinal = get[i];
                }
                if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                    continue;
                } else if (tickFluid && !fromGet) { // if fromGet, don't do the same again
                    if (get == null) {
                        get = getBlocks.load(layer);
                    }
                    replacedOrdinal = get[i];
                }
                boolean ticking = BlockTypesCache.ticking[ordinal];
                boolean replacedWasTicking = BlockTypesCache.ticking[replacedOrdinal];
                boolean replacedWasLiquid = false;
                BlockState replacedState = null;
                if (!ticking) {
                    if (tickFluid) {
                        // If the block being replaced was not ticking, it cannot be a liquid
                        if (!replacedWasTicking) {
                            continue;
                        }
                        // If the block being replaced is not fluid, we do not need to worry
                        if (!(replacedWasLiquid =
                                (replacedState = BlockState.getFromOrdinal(replacedOrdinal)).getMaterial().isLiquid())) {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
                BlockState state = BlockState.getFromOrdinal(ordinal);
                boolean liquid = state.getMaterial().isLiquid();
                int x = i & 15;
                int y = (i >> 8) & 15;
                int z = (i >> 4) & 15;
                BlockPos position = new BlockPos((chunk.getX() << 4) + x, (layer << 4) + y, (chunk.getZ() << 4) + z);
                // Liquids do not go into this list!
                if (!liquid && (tickExisting || tickPlaced)) {
                    tickingBlocks.put(
                            position,
                            WorldEditPlugin.getInstance().getBukkitImplAdapter().getInternalBlockStateId(state).orElse(0)
                    );
                }
                if (tickFluid && (liquid || replacedWasLiquid)) {
                    if (liquid) {
                        addFluid(fluidBlocks, state, position);
                        continue;
                    }
                    // If the replaced fluid (is?) adjacent to water. Do not bother to check adjacent chunks(sections) as this
                    // may be time consuming. Chances are any fluid blocks in adjacent chunks are being replaced or will end up
                    // being ticked anyway. We only need it to be "hit" once.
                    if (!wasAdjacentToWater(get, set, i, x, y, z)) {
                        continue;
                    }
                    existingFluidCount--; // Easy way of saying "some liquid was removed"
                    addFluid(fluidBlocks, replacedState, position);
                }
            }
            final int l = layer;
            int tickingBlockCount = 0;
            if (tickingBlocks != null) {
                tickingBlocks.forEach((pos, ordinal) -> getBlocks.levelChunk.getSections()[l - iChunkGet.getMinSectionPosition()].tickingList.add(
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        Block.stateById(ordinal)
                ));
                tickingBlockCount = tickingBlocks.size();
            }
            int fluidBlockCount = existingFluidCount;
            if (fluidBlocks != null) {
                fluidBlocks.forEach((pos, type) -> getBlocks.serverLevel.scheduleTick(
                        pos,
                        type,
                        type.getTickDelay(getBlocks.serverLevel)
                ));
                fluidBlockCount += fluidBlocks.size();
            }
            try {
                PaperweightPlatformAdapter.setCounts(
                        tickingBlockCount,
                        -1,
                        fluidBlockCount,
                        getBlocks.levelChunk.getSections()[l - iChunkGet.getMinSectionPosition()]
                );
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    @Override
    public Extent construct(final Extent child) {
        throw new UnsupportedOperationException("Processing only");
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.READING_SET_BLOCKS;
    }

    private boolean wasAdjacentToWater(char[] get, char[] set, int i, int x, int y, int z) {
        char ordinal;
        char reserved = BlockTypesCache.ReservedIDs.__RESERVED__;
        if (x > 0 && set[i - 1] != reserved) {
            if (BlockTypesCache.ticking[(ordinal = get[i - 1])] && isFluid(ordinal)) {
                return true;
            }
        }
        if (x < 15 && set[i + 1] != reserved) {
            if (BlockTypesCache.ticking[(ordinal = get[i + 1])] && isFluid(ordinal)) {
                return true;
            }
        }
        if (z > 0 && set[i - 16] != reserved) {
            if (BlockTypesCache.ticking[(ordinal = get[i - 16])] && isFluid(ordinal)) {
                return true;
            }
        }
        if (z < 15 && set[i + 16] != reserved) {
            if (BlockTypesCache.ticking[(ordinal = get[i + 16])] && isFluid(ordinal)) {
                return true;
            }
        }
        if (y > 0 && set[i - 256] != reserved) {
            if (BlockTypesCache.ticking[(ordinal = get[i - 256])] && isFluid(ordinal)) {
                return true;
            }
        }
        if (y < 15 && set[i + 256] != reserved) {
            return BlockTypesCache.ticking[(ordinal = get[i + 256])] && isFluid(ordinal);
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private boolean isFluid(char ordinal) {
        return BlockState.getFromOrdinal(ordinal).getMaterial().isLiquid();
    }

    @SuppressWarnings("deprecation")
    private void addFluid(final Map<BlockPos, Fluid> fluidBlocks, final BlockState replacedState, final BlockPos position) {
        Fluid type;
        if (replacedState.getBlockType() == BlockTypes.LAVA) {
            type = (int) replacedState.getState(PropertyKey.LEVEL) == 0 ? Fluids.LAVA : Fluids.FLOWING_LAVA;
        } else {
            type = (int) replacedState.getState(PropertyKey.LEVEL) == 0 ? Fluids.WATER : Fluids.FLOWING_WATER;
        }
        fluidBlocks.put(position, type);
    }

}
