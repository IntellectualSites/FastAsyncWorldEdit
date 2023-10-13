package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regenerator.level;

import com.sk89q.worldedit.world.RegenOptions;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.jetbrains.annotations.NotNull;

public class FaweProtoChunkSection extends LevelChunkSection {

    private final RegenOptions options;

    public FaweProtoChunkSection(final Registry<Biome> biomeRegistry, final RegenOptions options) {
        super(
                new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(),
                        PalettedContainer.Strategy.SECTION_STATES, null
                ),
                new PalettedContainer<>(
                        biomeRegistry.asHolderIdMap(),
                        options.hasBiomeType() && options.getBiomeType() != null ?
                                biomeRegistry.getHolderOrThrow(ResourceKey.create(
                                        Registries.BIOME, ResourceLocation.of(options.getBiomeType().getId(), ':')
                                )) : biomeRegistry.getHolderOrThrow(Biomes.PLAINS),
                        PalettedContainer.Strategy.SECTION_BIOMES,
                        null
                )
        );
        this.options = options;
    }

    @Override
    public @NotNull BlockState setBlockState(final int x, final int y, final int z, final @NotNull BlockState state) {
        return this.states.getAndSetUnchecked(x, y, z, state);
    }

    @Override
    public @NotNull BlockState setBlockState(
            final int x,
            final int y,
            final int z,
            final @NotNull BlockState state,
            final boolean lock
    ) {
        return setBlockState(x, y, z, state);
    }

    /**
     * We don't recalculate blocks after modification, just access every chunk
     *
     * @return always {@code false}
     */
    @Override
    public boolean hasOnlyAir() {
        return false;
    }

    @Override
    public boolean isRandomlyTicking() {
        return false;
    }

    @Override
    public boolean isRandomlyTickingBlocks() {
        return false;
    }

    @Override
    public boolean isRandomlyTickingFluids() {
        return false;
    }

    /**
     * We have no working TickingList (and don't need one) and don't recalculate block counts (see {@link #hasOnlyAir()})
     */
    @Override
    public void recalcBlockCounts() {
    }

    /**
     * If a custom BiomeType has been supplied by the user, don't allow any biome writes into the palette
     */
    @Override
    public void setBiome(final int x, final int y, final int z, final @NotNull Holder<Biome> biome) {
        if (options.hasBiomeType()) {
            return;
        }
        super.setBiome(x, y, z, biome);
    }

    /**
     * If a custom BiomeType has been supplied by the user, we don't write into the data palette as the default value is
     * already the user defined biome type.
     */
    @Override
    public void fillBiomesFromNoise(
            final @NotNull BiomeResolver biomeSupplier,
            final Climate.@NotNull Sampler sampler,
            final int x,
            final int y,
            final int z
    ) {
        if (options.hasBiomeType()) {
            // Don't populate anything - is handled by default palette value set in constructor
            return;
        }
        super.fillBiomesFromNoise(biomeSupplier, sampler, x, y, z);
    }

    /**
     * No need for locking
     */
    @Override
    public void acquire() {
    }

    /**
     * No need for locking
     */
    @Override
    public void release() {
    }

}
