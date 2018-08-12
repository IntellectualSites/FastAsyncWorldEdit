package com.boydti.fawe.object.function.block;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class CombinedBlockCopy implements RegionFunction {

    protected final Extent source;
    protected final Extent destination;
    private final RegionFunction function;

    public CombinedBlockCopy(Extent source, Extent destination, RegionFunction combined) {
        this.source = source;
        this.destination = destination;
        this.function = combined;
    }

    @Override
    public boolean apply(Vector position) throws WorldEditException {
        BlockStateHolder block = source.getBlock(position);
        function.apply(position);
        return destination.setBlock(position, block);
    }
}
