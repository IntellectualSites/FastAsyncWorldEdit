package com.boydti.fawe.object.change;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.math.MutableBlockVector2;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.world.biome.BiomeTypes;

public class MutableBiomeChange implements Change {

    private final MutableBlockVector3 mutable = new MutableBlockVector3();
    private int from;
    private int to;

    public MutableBiomeChange() {
        this.from = 0;
        this.to = 0;
    }

    public void setBiome(int x, int y, int z, int from, int to) {
        mutable.setComponents(x, y, z);
        this.from = from;
        this.to = to;
    }

    @Override
    public void undo(UndoContext context) throws WorldEditException {
        context.getExtent().setBiome(mutable, BiomeTypes.get(from));
    }

    @Override
    public void redo(UndoContext context) throws WorldEditException {
        context.getExtent().setBiome(mutable, BiomeTypes.get(to));
    }
}
