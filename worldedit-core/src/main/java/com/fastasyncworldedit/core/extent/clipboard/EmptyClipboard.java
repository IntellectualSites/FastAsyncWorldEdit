package com.fastasyncworldedit.core.extent.clipboard;

import com.fastasyncworldedit.core.beta.implementation.lighting.HeightMapType;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EmptyClipboard implements Clipboard {
    private static final EmptyClipboard instance = new EmptyClipboard();

    public static EmptyClipboard getInstance() {
        return instance;
    }

    @NotNull
    public Region getRegion() {
        return new CuboidRegion(BlockVector3.ZERO, BlockVector3.ZERO);
    }

    @NotNull
    public BlockVector3 getDimensions() {
        return BlockVector3.ZERO;
    }

    @NotNull
    public BlockVector3 getOrigin() {
        return BlockVector3.ZERO;
    }

    public void setOrigin(@NotNull BlockVector3 origin) {
    }

    public void removeEntity(@NotNull Entity entity) {
    }

    @NotNull
    public BlockVector3 getMinimumPoint() {
        return BlockVector3.ZERO;
    }

    @NotNull
    public BlockVector3 getMaximumPoint() {
        return BlockVector3.ZERO;
    }

    @NotNull
    public BaseBlock getFullBlock(@NotNull BlockVector3 position) {
        return BlockTypes.AIR.getDefaultState().toBaseBlock();
    }

    @NotNull
    public BlockState getBlock(@NotNull BlockVector3 position) {
        return BlockTypes.AIR.getDefaultState();
    }

    @Nullable
    public BiomeType getBiome(@NotNull BlockVector3 position) {
        return null;
    }

    @NotNull
    public int[] getHeightMap(@Nullable HeightMapType type) {
        return new int[256];
    }

    public boolean setBlock(@NotNull BlockVector3 position, BlockStateHolder block) throws WorldEditException {
        return false;
    }

    public boolean setBlock(int x, int y, int z, BlockStateHolder block) throws WorldEditException {
        return false;
    }

    public boolean setTile(int x, int y, int z, @NotNull CompoundTag tile) throws WorldEditException {
        return false;
    }

    public boolean setBiome(@NotNull BlockVector3 position, @NotNull BiomeType biome) {
        return false;
    }

    public boolean fullySupports3DBiomes() {
        return false;
    }

    public boolean setBiome(int x, int y, int z, @NotNull BiomeType biome) {
        return false;
    }

    private EmptyClipboard() {
    }

}
