package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator;

import com.fastasyncworldedit.bukkit.adapter.regeneration.ChunkWorker;
import com.fastasyncworldedit.bukkit.adapter.regeneration.Regenerator;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.PaperweightFaweAdapter;
import com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator.level.FaweChunkSource;
import com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator.worker.BiomesWorker;
import com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator.worker.CarversWorker;
import com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator.worker.FeaturesWorker;
import com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator.worker.NoiseWorker;
import com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator.worker.SurfaceWorker;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import com.sk89q.worldedit.world.RegenOptions;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class VersionedRegenerator extends Regenerator<ServerLevel, WorldGenRegion, ChunkAccess, ChunkGenerator, ChunkStatus> {

    /**
     * Each chunk has their own ChunkSource as chunks are worked on in parallel and therefor neighbour chunks may differ in status
     */
    private final Long2ObjectLinkedOpenHashMap<FaweChunkSource> chunkSources;

    public VersionedRegenerator(
            final World world, final Region region, final RegenOptions options,
            final Extent extent
    ) {
        super(world, region, options, extent);
        this.chunkSources = new Long2ObjectLinkedOpenHashMap<>(region.getChunks().size());

        for (final BlockVector2 chunk : region.getChunks()) {
            FaweChunkSource source = new FaweChunkSource(
                    region,
                    serverLevel().registryAccess().registryOrThrow(Registries.BIOME),
                    options,
                    serverLevel()
            );
            this.chunkSources.put(ChunkPos.asLong(chunk.getX(), chunk.getZ()), source);
        }
    }

    @Override
    public CompletableFuture<Boolean> flushPalettesIntoWorld(final ChunkAccess chunkAccess, final WorldGenRegion worldGenRegion) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = false;
            final int minX = Math.max(chunkAccess.getPos().getMinBlockX(), region.getMinimumPoint().getBlockX());
            final int minY = Math.max(chunkAccess.getMinBuildHeight(), region.getMinimumY());
            final int minZ = Math.max(chunkAccess.getPos().getMinBlockZ(), region.getMinimumPoint().getBlockZ());
            final int maxX = Math.min(chunkAccess.getPos().getMaxBlockX(), region.getMaximumPoint().getBlockX());
            final int maxY = Math.min(chunkAccess.getMaxBuildHeight(), region.getMaximumY());
            final int maxZ = Math.min(chunkAccess.getPos().getMaxBlockZ(), region.getMaximumPoint().getBlockZ());
            for (final BlockPos blockPos : BlockPos.MutableBlockPos.betweenClosed(
                    minX, minY, minZ,
                    maxX, maxY, maxZ
            )) {
                // We still have to validate boundaries, as region may not be a cuboid
                if (!(region.contains(blockPos.getX(), blockPos.getY(), blockPos.getZ()))) {
                    continue;
                }
                if ((options.shouldRegenBiomes() || options.hasBiomeType()) &&
                        (blockPos.getX() % 4 == 0 && blockPos.getY() % 4 == 0 && blockPos.getZ() % 4 == 0)) {
                    result |= this.extent.setBiome(
                            blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                            BiomeTypes.get(worldGenRegion.getBiome(blockPos).unwrapKey()
                                    .orElseThrow().location().toString())
                    );
                }
                final BlockEntity blockEntity = chunkAccess.getBlockEntity(blockPos);
                final com.sk89q.worldedit.world.block.BlockState blockState = ((PaperweightFaweAdapter) WorldEditPlugin
                        .getInstance()
                        .getBukkitImplAdapter())
                        .adapt(blockEntity != null ? blockEntity.getBlockState() : chunkAccess.getBlockState(blockPos));
                if (blockEntity != null) {
                    result |= this.extent.setBlock(
                            blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                            blockState.toBaseBlock(LazyReference.from(() -> {
                                //noinspection unchecked
                                if (!(WorldEditPlugin.getInstance().getBukkitImplAdapter()
                                        .toNativeBinary(blockEntity.saveWithId()) instanceof CompoundBinaryTag tag)) {
                                    throw new IllegalStateException("Entity Binary-Tag is no CompoundBinaryTag");
                                }
                                return tag;
                            }))
                    );
                    continue;
                }
                result |= this.extent.setBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockState);
            }
            this.chunkSources.remove(chunkAccess.getPos().toLong());
            return result;
        }, provideExecutor().get());
    }

    @Override
    public @Nullable ChunkAccess toNativeChunkAccess(
            final ServerLevel level,
            final BlockVector2 chunk,
            final int x,
            final int z,
            final ChunkStatus leastStatus
    ) {
        return this.chunkSources.get(ChunkPos.asLong(chunk.getX(), chunk.getZ()))
                .getChunk(x, z, leastStatus, true);
    }

    @Override
    protected @NonNull CompletableFuture<@Nullable Void> primeHeightmaps(final ChunkAccess chunk) {
        return CompletableFuture.runAsync(
                () -> Heightmap.primeHeightmaps(chunk, ChunkStatus.POST_FEATURES),
                provideExecutor().get()
        );
    }

    @Override
    public WorldGenRegion worldGenRegionForRegion(
            final ServerLevel level,
            final List<ChunkAccess> chunkAccesses,
            final Region region,
            final ChunkStatus status
    ) {
        return new DelegatingWorldGenRegion(
                level, chunkAccesses,
                status,
                PLACEMENT_RADII[status.getIndex() - ChunkStatus.BIOMES.getIndex()],
                options
        );
    }

    @Override
    public ChunkGenerator generatorFromLevel(final ServerLevel serverLevel) {
        return serverLevel.chunkSource.getGenerator();
    }

    @Override
    protected void setChunkStatus(final List<ChunkAccess> chunkAccesses, final ChunkStatus status) {
        chunkAccesses.forEach(chunkAccess -> ((ProtoChunk) chunkAccess).setStatus(status));
    }

    @Override
    public @NonNull List<@NonNull ChunkWorker<ServerLevel, ChunkAccess, WorldGenRegion, ChunkGenerator, ChunkStatus>> workers() {
        return List.of(
                BiomesWorker.INSTANCE,
                NoiseWorker.INSTANCE,
                SurfaceWorker.INSTANCE,
                CarversWorker.INSTANCE,
                FeaturesWorker.INSTANCE
        );
    }

    @Override
    public @NonNull ServerLevel serverLevel() {
        return ((CraftWorld) this.world).getHandle();
    }

    @Override
    public void close() throws IOException {
        this.chunkSources.clear();
        super.close();
    }

}
