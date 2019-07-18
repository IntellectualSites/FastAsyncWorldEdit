package com.boydti.fawe.object.change;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.object.HasIQueueExtent;
import com.boydti.fawe.util.ExtentTraverser;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;

public class MutableBlockChange implements Change {

    public int z;
    public int y;
    public int x;
    public int combinedId;


    public MutableBlockChange(int x, int y, int z, int combinedId) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.combinedId = combinedId;
    }

    @Override
    public void undo(UndoContext context) throws WorldEditException {
        create(context);
    }

    @Override
    public void redo(UndoContext context) throws WorldEditException {
        create(context);
    }

    private IQueueExtent queue;
    private boolean checkedQueue;

    public void create(UndoContext context) {
        if (queue != null) {
            queue.setBlock(x, y, z, combinedId);
        }
        if (!checkedQueue) {
            checkedQueue = true;
            Extent extent = context.getExtent();
            ExtentTraverser found = new ExtentTraverser(extent).find(HasIQueueExtent.class);
            if (found != null) {
                (queue = ((HasIQueueExtent) found.get()).getQueue()).setBlock(x, y, z, combinedId);
            } else {
                Fawe.debug("FAWE does not support: " + extent + " for " + getClass() + " (bug Empire92)");
            }
        }
    }
}
