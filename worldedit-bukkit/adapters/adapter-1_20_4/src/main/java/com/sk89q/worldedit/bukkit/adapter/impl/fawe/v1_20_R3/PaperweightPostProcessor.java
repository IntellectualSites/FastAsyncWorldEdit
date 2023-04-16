package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R3;

import com.fastasyncworldedit.bukkit.adapter.PostProcessor;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.implementation.blocks.DataArray;
import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class PaperweightPostProcessor extends PostProcessor {

    @SuppressWarnings("deprecation")
    @Override
    public void postProcess(final IChunk chunk, final IChunkGet iChunkGet, final IChunkSet iChunkSet) {
        boolean tickFluid = Settings.settings().EXPERIMENTAL.ALLOW_TICK_FLUIDS;
        // The PostProcessor shouldn't be added, but just in case
        if (!tickFluid) {
            return;
        }
        PaperweightGetBlocks_Copy getBlocks = (PaperweightGetBlocks_Copy) iChunkGet;
        layer:
        for (int layer = iChunkSet.getMinSectionPosition(); layer <= iChunkSet.getMaxSectionPosition(); layer++) {
            DataArray set = iChunkSet.loadIfPresent(layer);
            if (set == null) {
                // No edit means no need to process
                continue;
            }
            DataArray get = null;
            for (int i = 0; i < 4096; i++) {
                int ordinal = set.getAt(i);
                int replacedOrdinal = BlockTypesCache.ReservedIDs.__RESERVED__;
                boolean fromGet = false; // Used for liquids
                if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                    if (get == null) {
                        get = getBlocks.load(layer);
                    }
                    // If this is null, then it's because we're loading a layer in the range of 0->15, but blocks aren't
                    // actually being set
                    if (get == null) {
                        continue layer;
                    }
                    fromGet = true;
                    ordinal = replacedOrdinal = get.getAt(i);
                }
                if (ordinal == BlockTypesCache.ReservedIDs.__RESERVED__) {
                    continue;
                } else if (!fromGet) { // if fromGet, don't do the same again
                    if (get == null) {
                        get = getBlocks.load(layer);
                    }
                    replacedOrdinal = get.getAt(i);
                }
                boolean ticking = BlockTypesCache.ticking[ordinal];
                boolean replacedWasTicking = BlockTypesCache.ticking[replacedOrdinal];
                boolean replacedWasLiquid = false;
                BlockState replacedState = null;
                if (!ticking) {
                    // If the block being replaced was not ticking, it cannot be a liquid
                    if (!replacedWasTicking) {
                        continue;
                    }
                    // If the block being replaced is not fluid, we do not need to worry
                    if (!(replacedWasLiquid =
                            (replacedState = BlockState.getFromOrdinal(replacedOrdinal)).getMaterial().isLiquid())) {
                        continue;
                    }
                }
                BlockState state = BlockState.getFromOrdinal(ordinal);
                boolean liquid = state.getMaterial().isLiquid();
                int x = i & 15;
                int y = (i >> 8) & 15;
                int z = (i >> 4) & 15;
                BlockPos position = new BlockPos((chunk.getX() << 4) + x, (layer << 4) + y, (chunk.getZ() << 4) + z);
                if (liquid || replacedWasLiquid) {
                    if (liquid) {
                        addFluid(getBlocks.serverLevel, state, position);
                        continue;
                    }
                    // If the replaced fluid (is?) adjacent to water. Do not bother to check adjacent chunks(sections) as this
                    // may be time consuming. Chances are any fluid blocks in adjacent chunks are being replaced or will end up
                    // being ticked anyway. We only need it to be "hit" once.
                    if (!wasAdjacentToWater(get, set, i, x, y, z)) {
                        continue;
                    }
                    addFluid(getBlocks.serverLevel, replacedState, position);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void addFluid(final ServerLevel serverLevel, final BlockState replacedState, final BlockPos position) {
        Fluid type;
        if (replacedState.getBlockType() == BlockTypes.LAVA) {
            type = (int) replacedState.getState(PropertyKey.LEVEL) == 0 ? Fluids.LAVA : Fluids.FLOWING_LAVA;
        } else {
            type = (int) replacedState.getState(PropertyKey.LEVEL) == 0 ? Fluids.WATER : Fluids.FLOWING_WATER;
        }
        serverLevel.scheduleTick(
                position,
                type,
                type.getTickDelay(serverLevel)
        );
    }

}
