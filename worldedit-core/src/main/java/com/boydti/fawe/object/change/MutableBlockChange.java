package com.boydti.fawe.object.change;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.world.block.BlockState;

public class MutableBlockChange implements Change {

    public int z;
    public int y;
    public int x;
    public int ordinal;


    public MutableBlockChange(int x, int y, int z, int ordinal) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.ordinal = ordinal;
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
        context.getExtent().setBlock(x, y, z, BlockState.getFromOrdinal(ordinal));
    }
}
