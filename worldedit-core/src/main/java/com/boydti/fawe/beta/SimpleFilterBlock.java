package com.boydti.fawe.beta;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nullable;

public abstract class SimpleFilterBlock extends FilterBlock {
    private final Extent extent;

    public SimpleFilterBlock(Extent extent) {
        this.extent = extent;
    }

    @Override
    public final Extent getExtent() {
        return extent;
    }
}
