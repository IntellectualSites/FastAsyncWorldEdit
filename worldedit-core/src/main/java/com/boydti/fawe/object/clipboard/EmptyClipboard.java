package com.boydti.fawe.object.clipboard;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

public class EmptyClipboard implements Clipboard {

    public static final EmptyClipboard INSTANCE = new EmptyClipboard();

    private EmptyClipboard() {
    }

    @Override
    public Region getRegion() {
        return new CuboidRegion(BlockVector3.ZERO, BlockVector3.ZERO);
    }

    @Override
    public BlockVector3 getDimensions() {
        return BlockVector3.ZERO;
    }

    @Override
    public BlockVector3 getOrigin() {
        return BlockVector3.ZERO;
    }

    @Override
    public void setOrigin(BlockVector3 origin) {
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return BlockVector3.ZERO;
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return BlockVector3.ZERO;
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return BlockTypes.AIR.getDefaultState().toBaseBlock();
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return BlockTypes.AIR.getDefaultState();
    }

    @Override
    public BiomeType getBiome(BlockVector2 position) {
        return null;
    }

    @Override
    public boolean setBlock(BlockVector3 position, BlockStateHolder block) throws WorldEditException {
        return false;
    }

    @Override
    public boolean setBiome(BlockVector2 position, BiomeType biome) {
        return false;
    }

}
