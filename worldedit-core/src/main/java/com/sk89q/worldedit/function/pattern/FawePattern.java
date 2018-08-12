package com.sk89q.worldedit.function.pattern;

import com.sk89q.minecraft.util.commands.Link;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.UtilityCommands;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockState;

/**
 * Returns a {@link BlockStateHolder} for a given position.
 *  - Adapts the vector apply to integer
 */
@Link(clazz = UtilityCommands.class, value = "patterns")
public interface FawePattern extends Pattern {

    @Deprecated
    default BlockStateHolder apply(Vector position) {
        throw new UnsupportedOperationException("Please use apply(extent, get, set)");
    }

    /**
     * Return a {@link BlockStateHolder} for the given position.
     *
     * @return a block
     */
    @Override
    boolean apply(Extent extent, Vector get, Vector set) throws WorldEditException;
}

