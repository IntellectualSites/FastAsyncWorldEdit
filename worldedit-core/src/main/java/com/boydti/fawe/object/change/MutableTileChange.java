package com.boydti.fawe.object.change;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.IQueueExtent;
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

    public void create(UndoContext context) {
        int x = tag.getInt("x");
        int y = tag.getInt("y");
        int z = tag.getInt("z");
        context.getExtent().setTile(x, y, z, tag);
    }
}
