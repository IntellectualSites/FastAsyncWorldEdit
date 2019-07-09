package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PatternExtent extends AbstractPattern implements Extent {
    private final Pattern pattern;
    private transient BlockStateHolder block;
    private transient BlockVector3 target = BlockVector3.at(0, 0, 0);

    public PatternExtent(Pattern pattern) {
        this.pattern = pattern;
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        target = BlockVector3.at(0, 0, 0);
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return BlockVector3.at(Integer.MIN_VALUE, 0, Integer.MIN_VALUE);
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return BlockVector3.at(Integer.MAX_VALUE, 255, Integer.MAX_VALUE);
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return new ArrayList<>();
    }

    @Override
    public List<? extends Entity> getEntities() {
        return new ArrayList<>();
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        BlockStateHolder tmp = pattern.apply(position);
        if (position == target || (position.getX() == target.getX() && position.getY() == target.getY() && position.getZ() == target.getZ())) {
            block = tmp;
        } else {
            block = null;
        }
        return (BlockState) tmp;
    }

    public void setTarget(BlockVector3 vector) {
        this.target = vector;
    }

    public boolean getAndResetTarget(Extent extent, BlockVector3 set, BlockVector3 get) throws WorldEditException {
        BlockStateHolder result = block;
        if (result != null) {
            block = null;
            return extent.setBlock(set, result);
        } else {
            return pattern.apply(extent, set, target);
        }
    }

    public BlockStateHolder getAndResetTarget() {
        BlockStateHolder result = block;
        if (result != null) {
            block = null;
            return result;
        } else {
            return pattern.apply(target);
        }
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return getBlock(position).toBaseBlock();
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

    @Override
    public BaseBlock apply(BlockVector3 position) {
        return pattern.apply(position);
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 set, BlockVector3 get) throws WorldEditException {
        return pattern.apply(extent, set, get);
    }
}
