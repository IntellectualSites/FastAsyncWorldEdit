package com.boydti.fawe.object.change;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.HasFaweQueue;
import com.boydti.fawe.util.ExtentTraverser;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.extent.inventory.BlockBagException;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

public class MutableFullBlockChange implements Change {

    public int z;
    public int y;
    public int x;
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

    private FaweQueue queue;
    private boolean checkedQueue;

    public void create(UndoContext context) {
        if (queue != null) {
            perform(queue);
        }
        if (!checkedQueue) {
            checkedQueue = true;
            Extent extent = context.getExtent();
            ExtentTraverser found = new ExtentTraverser(extent).find(HasFaweQueue.class);
            if (found != null) {
                perform(queue = ((HasFaweQueue) found.get()).getQueue());
            } else {
                Fawe.debug("FAWE does not support: " + extent + " for " + getClass() + " (bug Empire92)");
            }
        }
    }

    public void perform(FaweQueue queue) {
        BlockTypes idFrom = BlockTypes.get(from);
        if (blockBag != null) {
            BlockTypes idTo = BlockTypes.get(to);
            if (idFrom != idTo) {
                if (allowFetch && from != 0) {
                    try {
                        blockBag.fetchPlacedBlock(BlockState.get(from));
                    } catch (BlockBagException e) {
                        return;
                    }
                }
                if (allowStore && to != 0) {
                    try {
                        blockBag.storeDroppedBlock(BlockState.get(to));
                    } catch (BlockBagException ignored) {
                    }
                }
            }
        }
        queue.setBlock(x, y, z, from);
    }
}
