package com.fastasyncworldedit.core.history.change;

import com.sk89q.worldedit.history.change.Change;
import org.jetbrains.annotations.ApiStatus;

/**
 * Represents a change that is associated with {@code (x, y, z)} block coordinates.
 * @since TODO
 */
@ApiStatus.Internal
public sealed abstract class BlockPositionChange implements Change
        permits MutableBlockChange, MutableFullBlockChange {
    public int x;
    public int y;
    public int z;
}
