package com.fastasyncworldedit.core.history.change;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.extent.inventory.BlockBagException;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class MutableFullBlockChange extends BlockPositionChange {
    public int from;
    public int to;
    public BlockBag blockBag;
    public boolean allowFetch;
    public boolean allowStore;

    public MutableFullBlockChange(BlockBag blockBag, int mode, boolean redo) {
        this.blockBag = blockBag;
        allowFetch = redo || mode == 1;
        allowStore = !redo || mode == 1;
    }

    @Override
    public void undo(UndoContext context) throws WorldEditException {
        create(context);
    }

    @Override
    public void redo(UndoContext context) throws WorldEditException {
        create(context);
    }

    public void create(UndoContext context) {
        BlockState fromState = BlockState.getFromOrdinal(from);
        if (blockBag != null) {
            BlockState toState = BlockState.getFromOrdinal(to);
            if (fromState != toState) {
                if (allowFetch && from != BlockTypesCache.ReservedIDs.__RESERVED__) {
                    try {
                        blockBag.fetchPlacedBlock(fromState);
                    } catch (BlockBagException e) {
                        return;
                    }
                }
                if (allowStore && to != BlockTypesCache.ReservedIDs.__RESERVED__) {
                    try {
                        blockBag.storeDroppedBlock(toState);
                    } catch (BlockBagException ignored) {
                    }
                }
            }
        }
        context.getExtent().setBlock(x, y, z, fromState);
    }

}
