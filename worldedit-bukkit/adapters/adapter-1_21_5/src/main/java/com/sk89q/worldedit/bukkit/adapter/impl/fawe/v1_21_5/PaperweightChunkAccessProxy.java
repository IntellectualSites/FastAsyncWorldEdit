package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_21_5;

import ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray;
import com.fastasyncworldedit.core.util.ReflectionUtils;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickContainerAccess;
import sun.misc.Unsafe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * This exists solely for the {@link PaperweightChunkAccessProxy#markPosForPostprocessing(BlockPos)} override, as the way we
 * handle feature/structure generation means the chunks returned in {@link FaweBlockStateListPopulator#getChunk(BlockPos)} (and
 * other getChunk) are not {@link net.minecraft.world.level.chunk.ProtoChunk} types, so do not override the
 * {@link ChunkAccess#markPosForPostprocessing(BlockPos)} function.
 */
@SuppressWarnings({"removal", "deprecation"})
public final class PaperweightChunkAccessProxy extends ChunkAccess {

    ChunkAccess parent;

    @SuppressWarnings("DataFlowIssue")
    private PaperweightChunkAccessProxy() {
        super(null, null, null, null, -1, null, null);
        throw new IllegalStateException("Cannot be instantiated");
    }

    public static PaperweightChunkAccessProxy getInstance() {
        Unsafe unsafe = ReflectionUtils.getUnsafe();

        try {
            return (PaperweightChunkAccessProxy) unsafe.allocateInstance(PaperweightChunkAccessProxy.class);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int hashCode() {
        return parent.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return parent.equals((obj instanceof PaperweightChunkAccessProxy ? ((PaperweightChunkAccessProxy) obj).parent : obj));
    }

    @Override
    public String toString() {
        return parent.toString();
    }

    @Override
    public @Nonnull BlockState getBlockState(final @Nonnull BlockPos pos) {
        return parent.getBlockState(pos);
    }

    @Override
    public @Nullable BlockState getBlockStateIfLoaded(final @Nonnull BlockPos blockPos) {
        return parent.getBlockStateIfLoaded(blockPos);
    }

    @Override
    public @Nullable Block getBlockIfLoaded(final @Nonnull BlockPos blockposition) {
        return parent.getBlockIfLoaded(blockposition);
    }

    @Override
    public @Nullable FluidState getFluidIfLoaded(final @Nonnull BlockPos blockPos) {
        return parent.getFluidIfLoaded(blockPos);
    }

    @Override
    public @Nonnull FluidState getFluidState(final @Nonnull BlockPos pos) {
        return parent.getFluidState(pos);
    }

    @Override
    public int getLightEmission(final @Nonnull BlockPos pos) {
        return parent.getLightEmission(pos);
    }

    @Override
    public @Nonnull Stream<BlockState> getBlockStates(final @Nonnull AABB box) {
        return parent.getBlockStates(box);
    }

    @Override
    public @Nonnull BlockHitResult isBlockInLine(final @Nonnull ClipBlockStateContext context) {
        return parent.isBlockInLine(context);
    }

    @Override
    public @Nonnull BlockHitResult clip(final @Nonnull ClipContext raytrace1, final @Nonnull BlockPos blockposition) {
        return parent.clip(raytrace1, blockposition);
    }

    @Override
    public @Nonnull BlockHitResult clip(
            final @Nonnull ClipContext raytrace1,
            final @Nonnull BlockPos blockposition,
            final @Nullable Predicate<? super org.bukkit.block.Block> canCollide
    ) {
        return parent.clip(raytrace1, blockposition, canCollide);
    }

    @Override
    public @Nonnull BlockHitResult clip(final @Nonnull ClipContext context) {
        return parent.clip(context);
    }

    @Override
    public @Nonnull BlockHitResult clip(
            final @Nonnull ClipContext context,
            final @Nullable Predicate<? super org.bukkit.block.Block> canCollide
    ) {
        return parent.clip(context, canCollide);
    }

    @Override
    public @Nullable BlockHitResult clipWithInteractionOverride(
            final @Nonnull Vec3 start,
            final @Nonnull Vec3 end,
            final @Nonnull BlockPos pos,
            final @Nonnull VoxelShape shape,
            final @Nonnull BlockState state
    ) {
        return parent.clipWithInteractionOverride(start, end, pos, shape, state);
    }

    @Override
    public double getBlockFloorHeight(
            final @Nonnull VoxelShape blockCollisionShape,
            final @Nonnull Supplier<VoxelShape> belowBlockCollisionShapeGetter
    ) {
        return parent.getBlockFloorHeight(blockCollisionShape, belowBlockCollisionShapeGetter);
    }

    @Override
    public double getBlockFloorHeight(final @Nonnull BlockPos pos) {
        return parent.getBlockFloorHeight(pos);
    }

    @Override
    public BlockEntity getBlockEntity(final @Nonnull BlockPos pos) {
        return parent.getBlockEntity(pos);
    }

    @Override
    public <T extends BlockEntity> @Nonnull Optional<T> getBlockEntity(
            final @Nonnull BlockPos pos,
            final @Nonnull BlockEntityType<T> type
    ) {
        return parent.getBlockEntity(pos, type);
    }

    @Override
    public @Nonnull SWMRNibbleArray[] starlight$getBlockNibbles() {
        return parent.starlight$getBlockNibbles();
    }

    @Override
    public void starlight$setBlockNibbles(final @Nonnull SWMRNibbleArray[] nibbles) {
        parent.starlight$setBlockNibbles(nibbles);
    }

    @Override
    public @Nonnull SWMRNibbleArray[] starlight$getSkyNibbles() {
        return parent.starlight$getBlockNibbles();
    }

    @Override
    public void starlight$setSkyNibbles(final @Nonnull SWMRNibbleArray[] nibbles) {
        parent.starlight$setSkyNibbles(nibbles);
    }

    @Override
    public @Nonnull boolean[] starlight$getSkyEmptinessMap() {
        return parent.starlight$getSkyEmptinessMap();
    }

    @Override
    public void starlight$setSkyEmptinessMap(final @Nonnull boolean[] emptinessMap) {
        parent.starlight$setSkyEmptinessMap(emptinessMap);
    }

    @Override
    public @Nonnull boolean[] starlight$getBlockEmptinessMap() {
        return parent.starlight$getBlockEmptinessMap();
    }

    @Override
    public void starlight$setBlockEmptinessMap(final @Nonnull boolean[] emptinessMap) {
        parent.starlight$setBlockEmptinessMap(emptinessMap);
    }

    @Override
    public @Nonnull GameEventListenerRegistry getListenerRegistry(final int sectionY) {
        return parent.getListenerRegistry(sectionY);
    }

    @Override
    public @Nonnull BlockState getBlockState(final int i, final int i1, final int i2) {
        return parent.getBlockState(i, i1, i2);
    }

    @Override
    public @Nullable BlockState setBlockState(final @Nonnull BlockPos pos, final @Nonnull BlockState state) {
        return parent.setBlockState(pos, state);
    }

    @Override
    public @Nullable BlockState setBlockState(
            final @Nonnull BlockPos blockPos,
            final @Nonnull BlockState blockState,
            final int i
    ) {
        return parent.setBlockState(blockPos, blockState, i);
    }

    @Override
    public void setBlockEntity(final @Nonnull BlockEntity blockEntity) {
        parent.setBlockEntity(blockEntity);
    }

    @Override
    public void addEntity(final @Nonnull Entity entity) {
        parent.addEntity(entity);
    }

    @Override
    public int getHighestFilledSectionIndex() {
        return parent.getHighestFilledSectionIndex();
    }

    @Override
    public int getHighestSectionPosition() {
        return parent.getHighestSectionPosition();
    }

    @Override
    public @Nonnull Set<BlockPos> getBlockEntitiesPos() {
        return parent.getBlockEntitiesPos();
    }

    @Override
    public @Nonnull LevelChunkSection[] getSections() {
        return parent.getSections();
    }

    @Override
    public @Nonnull LevelChunkSection getSection(final int index) {
        return parent.getSection(index);
    }

    @Override
    public @Nonnull Collection<Map.Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
        return parent.getHeightmaps();
    }

    @Override
    public void setHeightmap(final @Nonnull Heightmap.Types type, @Nonnull final long[] data) {
        parent.setHeightmap(type, data);
    }

    @Override
    public @Nonnull Heightmap getOrCreateHeightmapUnprimed(final @Nonnull Heightmap.Types type) {
        return parent.getOrCreateHeightmapUnprimed(type);
    }

    @Override
    public boolean hasPrimedHeightmap(final @Nonnull Heightmap.Types type) {
        return parent.hasPrimedHeightmap(type);
    }

    @Override
    public int getHeight(final @Nonnull Heightmap.Types type, final int x, final int z) {
        return parent.getHeight(type, x, z);
    }

    @Override
    public @Nonnull ChunkPos getPos() {
        return parent.getPos();
    }

    @Override
    public @Nullable StructureStart getStartForStructure(final @Nonnull Structure structure) {
        return parent.getStartForStructure(structure);
    }

    @Override
    public void setStartForStructure(final @Nonnull Structure structure, final @Nonnull StructureStart structureStart) {
        parent.setStartForStructure(structure, structureStart);
    }

    @Override
    public @Nonnull Map<Structure, StructureStart> getAllStarts() {
        return parent.getAllStarts();
    }

    @Override
    public void setAllStarts(final @Nonnull Map<Structure, StructureStart> structureStarts) {
        parent.setAllStarts(structureStarts);
    }

    @Override
    public @Nonnull LongSet getReferencesForStructure(final @Nonnull Structure structure) {
        return parent.getReferencesForStructure(structure);
    }

    @Override
    public void addReferenceForStructure(final @Nonnull Structure structure, final long reference) {
        parent.addReferenceForStructure(structure, reference);
    }

    @Override
    public @Nonnull Map<Structure, LongSet> getAllReferences() {
        return parent.getAllReferences();
    }

    @Override
    public void setAllReferences(final @Nonnull Map<Structure, LongSet> structureReferencesMap) {
        parent.setAllReferences(structureReferencesMap);
    }

    @Override
    public boolean isYSpaceEmpty(final int startY, final int endY) {
        return parent.isYSpaceEmpty(startY, endY);
    }

    @Override
    public boolean isSectionEmpty(final int y) {
        return parent.isSectionEmpty(y);
    }

    @Override
    public void markUnsaved() {
        parent.markUnsaved();
    }

    @Override
    public boolean tryMarkSaved() {
        return parent.tryMarkSaved();
    }

    @Override
    public boolean isUnsaved() {
        return parent.isUnsaved();
    }

    @Override
    public @Nonnull ChunkStatus getPersistedStatus() {
        return parent.getPersistedStatus();
    }

    @Override
    public @Nonnull ChunkStatus getHighestGeneratedStatus() {
        return parent.getHighestGeneratedStatus();
    }

    @Override
    public void removeBlockEntity(final @Nonnull BlockPos blockPos) {
        parent.removeBlockEntity(blockPos);
    }

    @Override
    public void markPosForPostprocessing(final @Nonnull BlockPos pos) {
        //Do nothing. ALL THIS FOR THIS METHOD :)
    }

    @Override
    public @Nonnull ShortList[] getPostProcessing() {
        return parent.getPostProcessing();
    }

    @Override
    public void addPackedPostProcess(final @Nonnull ShortList offsets, final int index) {
        parent.addPackedPostProcess(offsets, index);
    }

    @Override
    public void setBlockEntityNbt(final @Nonnull CompoundTag tag) {
        parent.setBlockEntityNbt(tag);
    }

    @Override
    public @Nullable CompoundTag getBlockEntityNbt(final @Nonnull BlockPos pos) {
        return parent.getBlockEntityNbt(pos);
    }

    @Override
    public @Nullable CompoundTag getBlockEntityNbtForSaving(
            final @Nonnull BlockPos blockPos,
            final @Nonnull HolderLookup.Provider provider
    ) {
        return parent.getBlockEntityNbtForSaving(blockPos, provider);
    }

    @Override
    public void findBlocks(final @Nonnull Predicate<BlockState> predicate, final @Nonnull BiConsumer<BlockPos, BlockState> output) {
        parent.findBlocks(predicate, output);
    }

    @Override
    public @Nonnull TickContainerAccess<Block> getBlockTicks() {
        return parent.getBlockTicks();
    }

    @Override
    public @Nonnull TickContainerAccess<Fluid> getFluidTicks() {
        return parent.getFluidTicks();
    }

    @Override
    public boolean canBeSerialized() {
        return parent.canBeSerialized();
    }

    @Override
    public @Nonnull PackedTicks getTicksForSerialization(final long l) {
        return parent.getTicksForSerialization(l);
    }

    @Override
    public @Nonnull UpgradeData getUpgradeData() {
        return parent.getUpgradeData();
    }

    @Override
    public boolean isOldNoiseGeneration() {
        return parent.isOldNoiseGeneration();
    }

    @Override
    public @Nullable BlendingData getBlendingData() {
        return parent.getBlendingData();
    }

    @Override
    public long getInhabitedTime() {
        return parent.getInhabitedTime();
    }

    @Override
    public void incrementInhabitedTime(final long amount) {
        parent.incrementInhabitedTime(amount);
    }

    @Override
    public void setInhabitedTime(final long inhabitedTime) {
        parent.setInhabitedTime(inhabitedTime);
    }

    @Override
    public boolean isLightCorrect() {
        return parent.isLightCorrect();
    }

    @Override
    public void setLightCorrect(final boolean lightCorrect) {
        parent.setLightCorrect(lightCorrect);
    }

    @Override
    public int getMinY() {
        return parent.getMinY();
    }

    @Override
    public int getHeight() {
        return parent.getHeight();
    }

    @Override
    public @Nonnull NoiseChunk getOrCreateNoiseChunk(final @Nonnull Function<ChunkAccess, NoiseChunk> noiseChunkCreator) {
        return parent.getOrCreateNoiseChunk(noiseChunkCreator);
    }

    @Override
    public @Nonnull BiomeGenerationSettings carverBiome(final @Nonnull Supplier<BiomeGenerationSettings> caverBiomeSettingsSupplier) {
        return parent.carverBiome(caverBiomeSettingsSupplier);
    }

    @Override
    public @Nonnull Holder<Biome> getNoiseBiome(final int x, final int y, final int z) {
        return parent.getNoiseBiome(x, y, z);
    }

    @Override
    public void setBiome(final int x, final int y, final int z, final @Nonnull Holder<Biome> biome) {
        parent.setBiome(x, y, z, biome);
    }

    @Override
    public void fillBiomesFromNoise(final @Nonnull BiomeResolver resolver, final @Nonnull Climate.Sampler sampler) {
        parent.fillBiomesFromNoise(resolver, sampler);
    }

    @Override
    public boolean hasAnyStructureReferences() {
        return parent.hasAnyStructureReferences();
    }

    @Override
    public @Nullable BelowZeroRetrogen getBelowZeroRetrogen() {
        return parent.getBelowZeroRetrogen();
    }

    @Override
    public boolean isUpgrading() {
        return parent.isUpgrading();
    }

    @Override
    public @Nonnull LevelHeightAccessor getHeightAccessorForGeneration() {
        return parent.getHeightAccessorForGeneration();
    }

    @Override
    public void initializeLightSources() {
        parent.initializeLightSources();
    }

    @Override
    public @Nonnull ChunkSkyLightSources getSkyLightSources() {
        return parent.getSkyLightSources();
    }

    @Override
    public int getMaxY() {
        return parent.getMaxY();
    }

    @Override
    public int getSectionsCount() {
        return parent.getSectionsCount();
    }

    @Override
    public int getMinSectionY() {
        return parent.getMinSectionY();
    }

    @Override
    public int getMaxSectionY() {
        return parent.getMaxSectionY();
    }

    @Override
    public boolean isInsideBuildHeight(final int y) {
        return parent.isInsideBuildHeight(y);
    }

    @Override
    public boolean isOutsideBuildHeight(final @Nonnull BlockPos pos) {
        return parent.isOutsideBuildHeight(pos);
    }

    @Override
    public boolean isOutsideBuildHeight(final int y) {
        return parent.isOutsideBuildHeight(y);
    }

    @Override
    public int getSectionIndex(final int y) {
        return parent.getSectionIndex(y);
    }

    @Override
    public int getSectionIndexFromSectionY(final int coord) {
        return parent.getSectionIndexFromSectionY(coord);
    }

    @Override
    public int getSectionYFromSectionIndex(final int index) {
        return parent.getSectionYFromSectionIndex(index);
    }

}
