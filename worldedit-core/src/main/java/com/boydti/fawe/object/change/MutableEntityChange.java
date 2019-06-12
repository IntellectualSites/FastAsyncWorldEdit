package com.boydti.fawe.object.change;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.HasFaweQueue;
import com.boydti.fawe.object.extent.FastWorldEditExtent;
import com.boydti.fawe.util.ExtentTraverser;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MutableEntityChange implements Change {

    public CompoundTag tag;
    public boolean create;

    public MutableEntityChange(CompoundTag tag, boolean create) {
        this.tag = tag;
        this.create = create;
    }

    @Override
    public void undo(UndoContext context) throws WorldEditException {
        if (!create) {
            create(context);
        } else {
            delete(context);
        }
    }

    @Override
    public void redo(UndoContext context) throws WorldEditException {
        if (create) {
            create(context);
        } else {
            delete(context);
        }
    }

    public void delete(UndoContext context) {
        Extent extent = context.getExtent();
        ExtentTraverser<FastWorldEditExtent> find = new ExtentTraverser(extent).find(FastWorldEditExtent.class);
        if (find != null) {
            FastWorldEditExtent fwee = find.get();
            Map<String, Tag> map = tag.getValue();
            long most;
            long least;
            if (map.containsKey("UUIDMost")) {
                most = ((LongTag) map.get("UUIDMost")).getValue();
                least = ((LongTag) map.get("UUIDLeast")).getValue();
            } else if (map.containsKey("PersistentIDMSB")) {
                most = ((LongTag) map.get("PersistentIDMSB")).getValue();
                least = ((LongTag) map.get("PersistentIDLSB")).getValue();
            } else {
                Fawe.debug("Skipping entity without uuid.");
                return;
            }
            List<DoubleTag> pos = (List<DoubleTag>) map.get("Pos").getValue();
            int x = MathMan.roundInt(pos.get(0).getValue());
            int y = MathMan.roundInt(pos.get(1).getValue());
            int z = MathMan.roundInt(pos.get(2).getValue());
            UUID uuid = new UUID(most, least);
            fwee.getQueue().removeEntity(x, y, z, uuid);
        } else {
            Fawe.debug("FAWE doesn't support: " + context + " for " + getClass() + " (bug Empire92)");
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
        Tag posTag = map.get("Pos");
        if (posTag == null) {
            Fawe.debug("Missing pos tag: " + tag);
            return;
        }
        List<DoubleTag> pos = (List<DoubleTag>) posTag.getValue();
        int x = MathMan.roundInt(pos.get(0).getValue());
        int y = MathMan.roundInt(pos.get(1).getValue());
        int z = MathMan.roundInt(pos.get(2).getValue());
        queue.setEntity(x, y, z, tag);
    }
}
