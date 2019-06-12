package com.boydti.fawe.object.change;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.HasFaweQueue;
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
        Map<String, Tag> map = tag.getValue();
        int x = ((IntTag) map.get("x")).getValue();
        int y = ((IntTag) map.get("y")).getValue();
        int z = ((IntTag) map.get("z")).getValue();
        queue.setTile(x, y, z, tag);
    }
}
