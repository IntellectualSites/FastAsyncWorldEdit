package com.boydti.fawe.object.brush.visualization;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.weather.WeatherType;
import javax.annotation.Nullable;

public abstract class ImmutableVirtualWorld implements VirtualWorld {
    @Override
    public int getMaxY() {
        return 255;
    }

    @Override
    public boolean regenerateChunk(int x, int z, @Nullable BiomeType biome, @Nullable Long seed) {
        return unsupported();
    }

    @Override
    public BiomeType getBiome(BlockVector2 position) {
        return BiomeTypes.FOREST;
    }

    @Override
    public String getName() {
        return Integer.toString(hashCode());
    }

    @Override
    public boolean setBlock(BlockVector3 position, BlockStateHolder block, boolean notifyAndLight) throws WorldEditException {
        return setBlock(position, block);
    }

    @Override
    public int getBlockLightLevel(BlockVector3 position) {
        return 0;
    }

    @Override
    public boolean clearContainerBlockContents(BlockVector3 position) {
        return unsupported();
    }

    @Override
    public void dropItem(Vector3 position, BaseItemStack item) {
        unsupported();
    }

    @Override
    public boolean generateTree(TreeGenerator.TreeType type, EditSession editSession, BlockVector3 position) throws MaxChangedBlocksException {
        return unsupported();
    }


    @Override
    public boolean regenerate(Region region, EditSession editSession) {
        return unsupported();
    }

    private boolean unsupported() {
        throw new UnsupportedOperationException("World is immutable");
    }

    @Override
    public boolean setBlock(BlockVector3 pt, BlockStateHolder block) throws WorldEditException {
        return unsupported();
    }

    @Override
    public void simulateBlockMine(BlockVector3 position) {
        unsupported();
    }

    @Override
    public void setWeather(WeatherType weatherType) {
        unsupported();
    }

    @Override
    public void setWeather(WeatherType weatherType, long duration) {
        unsupported();
    }
}
