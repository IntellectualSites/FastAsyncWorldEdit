package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator.level;

import com.fastasyncworldedit.bukkit.adapter.regeneration.Regenerator;
import com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator.noop.NoopLightEngine;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.RegenOptions;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.BooleanSupplier;

public class FaweChunkSource extends ChunkSource {

    private final Registry<Biome> biomeRegistry;
    private final RegenOptions options;
    private final LevelHeightAccessor levelHeightAccessor;
    private final BlockGetter blockGetter;
    private final Long2ObjectLinkedOpenHashMap<FaweProtoChunkAccess> cachedAccesses;
    private final Region region;

    public FaweChunkSource(
            final Region region, final Registry<Biome> biomeRegistry, final RegenOptions options,
            final LevelHeightAccessor levelHeightAccessor
    ) {
        this.region = region;
        this.biomeRegistry = biomeRegistry;
        this.options = options;
        this.levelHeightAccessor = levelHeightAccessor;
        this.cachedAccesses = new Long2ObjectLinkedOpenHashMap<>();
        this.blockGetter = new FaweBlockGetter(this);
    }

    @Nullable
    @Override
    public ChunkAccess getChunk(final int x, final int z, final @NotNull ChunkStatus leastStatus, final boolean create) {
        if (x < (this.region.getMinimumPoint().getBlockX() >> 4) - Regenerator.TASK_MARGIN) {
            return null;
        }
        if (x > (this.region.getMaximumPoint().getBlockX() >> 4) + Regenerator.TASK_MARGIN) {
            return null;
        }
        if (z < (this.region.getMinimumPoint().getBlockZ() >> 4) - Regenerator.TASK_MARGIN) {
            return null;
        }
        if (z > (this.region.getMaximumPoint().getBlockZ() >> 4) + Regenerator.TASK_MARGIN) {
            return null;
        }
        FaweProtoChunkAccess chunkAccess = cachedAccesses.get(ChunkPos.asLong(x, z));
        if (chunkAccess == null && create) {
            cachedAccesses.put(ChunkPos.asLong(x, z), chunkAccess = new FaweProtoChunkAccess(this, new ChunkPos(x, z),
                    biomeRegistry, leastStatus, options
            ));
        }
        if (chunkAccess != null && !chunkAccess.getStatus().isOrAfter(leastStatus)) {
            return null;
        }
        return chunkAccess;
    }

    @Override
    public void tick(final @NotNull BooleanSupplier shouldKeepTicking, final boolean tickChunks) {
    }

    @Override
    public @NotNull String gatherStats() {
        return toString();
    }

    @Override
    public int getLoadedChunksCount() {
        return this.cachedAccesses.size();
    }

    @Override
    public @NotNull LevelLightEngine getLightEngine() {
        return new NoopLightEngine(this);
    }

    @Override
    public @NotNull BlockGetter getLevel() {
        return this.blockGetter;
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.cachedAccesses.clear();
    }

    public LevelHeightAccessor levelHeightAccessor() {
        return levelHeightAccessor;
    }

}
