package com.sk89q.worldedit.function.pattern;


import com.sk89q.worldedit.world.block.BlockState;
import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A pattern that reads from {@link Clipboard}.
 */
public class ClipboardPattern extends AbstractPattern {

    private final Clipboard clipboard;
    private final int sx, sy, sz;
    private final BlockVector3 min;
//    private final BlockVector3 size;

    /**
     * Create a new clipboard pattern.
     *
     * @param clipboard the clipboard
     */
    public ClipboardPattern(Clipboard clipboard) {
        checkNotNull(clipboard);
        this.clipboard = clipboard;
        BlockVector3 size = clipboard.getMaximumPoint().subtract(clipboard.getMinimumPoint()).add(1, 1, 1);
        this.sx = size.getBlockX();
        this.sy = size.getBlockY();
        this.sz = size.getBlockZ();
        this.min = clipboard.getMinimumPoint();
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        int xp = position.getBlockX() % sx;
        int yp = position.getBlockY() % sy;
        int zp = position.getBlockZ() % sz;
        if (xp < 0) xp += sx;
        if (yp < 0) yp += sy;
        if (zp < 0) zp += sz;
        return clipboard.getFullBlock(BlockVector3.at(min.getX() + xp, min.getY() + yp, min.getZ() + zp));
    }


}