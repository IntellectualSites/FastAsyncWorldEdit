package com.boydti.fawe.object.change;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.object.HasIQueueExtent;
import com.boydti.fawe.util.ExtentTraverser;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import java.util.Map;

public class MutableTileChange implements Change {

    public CompoundTag tag;
    public boolean create;

    public MutableTileChange(CompoundTag tag, boolean create) {
        this.tag = tag;
        this.create = create;
    }

    @Override
    public void undo(UndoContext context) throws WorldEditException {
        if (!create) {
            create(context);
        }
    }

    @Override
    public void redo(UndoContext context) throws WorldEditException {
        if (create) {
            create(context);
        }
    }

    private IQueueExtent queue;
    private boolean checkedQueue;

    public void create(UndoContext context) {
        if (queue != null) {
            perform(queue);
        }
        if (!checkedQueue) {
            checkedQueue = true;
            Extent extent = context.getExtent();
            ExtentTraverser found = new ExtentTraverser(extent).find(HasIQueueExtent.class);
            if (found != null) {
                perform(queue = ((HasIQueueExtent) found.get()).getQueue());
            } else {
                Fawe.debug("FAWE does not support: " + extent + " for " + getClass() + " (bug Empire92)");
            }
        }
    }

    public void perform(IQueueExtent queue) {
        Map<String, Tag> map = tag.getValue();
        int x = ((IntTag) map.get("x")).getValue();
        int y = ((IntTag) map.get("y")).getValue();
        int z = ((IntTag) map.get("z")).getValue();
        queue.setTile(x, y, z, tag);
    }
}
