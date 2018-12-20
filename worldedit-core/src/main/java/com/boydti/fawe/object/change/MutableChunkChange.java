package com.boydti.fawe.object.change;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.HasFaweQueue;
import com.boydti.fawe.util.ExtentTraverser;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.world.block.BlockTypes;

public class MutableChunkChange implements Change {

    private final boolean checkEmpty;
    public FaweChunk from;
    public FaweChunk to;

    public MutableChunkChange(FaweChunk from, FaweChunk to, boolean checkEmpty) {
        this.from = from;
        this.to = to;
        this.checkEmpty = checkEmpty;
    }

    @Override
    public void undo(UndoContext context) throws WorldEditException {
        create(context, true);
    }

    @Override
    public void redo(UndoContext context) throws WorldEditException {
        create(context, false);
    }

    private FaweQueue queue;
    private boolean checkedQueue;

    public void create(UndoContext context, boolean undo) {
        if (queue != null) {
            perform(queue, undo);
        }
        if (!checkedQueue) {
            checkedQueue = true;
            Extent extent = context.getExtent();
            ExtentTraverser found = new ExtentTraverser(extent).find(HasFaweQueue.class);
            if (found != null) {
                perform(queue = ((HasFaweQueue) found.get()).getQueue(), undo);
            } else {
                Fawe.debug("FAWE does not support: " + extent + " for " + getClass() + " (bug Empire92)");
            }
        }
    }

    public void perform(FaweQueue queue, boolean undo) {
        if (undo) {
            if (checkEmpty) {
                int[][] previousIds = from.getCombinedIdArrays();
                int[][] nextIds = to.getCombinedIdArrays();
                for (int i = 0; i < previousIds.length; i++) {
                    if (previousIds[i] == null && nextIds[i] != null) {
                        from.fillCuboid(0, 15, i << 4, (i << 4) + 15, 0, 15, BlockTypes.AIR.getInternalId());
                    }
                }
            }
            queue.setChunk(from);
        } else {
            queue.setChunk(to);
        }
    }
}
