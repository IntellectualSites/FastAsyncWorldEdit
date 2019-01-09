package com.boydti.fawe.object.change;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.math.MutableBlockVector2D;
import com.sk89q.worldedit.world.biome.BaseBiome;

public class MutableBiomeChange implements Change {

    private MutableBlockVector2D mutable = new MutableBlockVector2D();
    private BaseBiome from;
    private BaseBiome to;

    public MutableBiomeChange() {
        this.from = new BaseBiome(0);
        this.to = new BaseBiome(0);
    }

    public void setBiome(int x, int z, int from, int to) {
        mutable.setComponents(x, z);
        this.from.setId(from);
        this.to.setId(to);
    }

    @Override
    public void undo(UndoContext context) throws WorldEditException {
        context.getExtent().setBiome(mutable.toBlockVector2(), from);
    }

    @Override
    public void redo(UndoContext context) throws WorldEditException {
        context.getExtent().setBiome(mutable.toBlockVector2(), to);
    }
}
