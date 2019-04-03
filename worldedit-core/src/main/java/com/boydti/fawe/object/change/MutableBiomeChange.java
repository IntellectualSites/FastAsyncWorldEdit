package com.boydti.fawe.object.change;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.math.MutableBlockVector2;
import com.sk89q.worldedit.world.biome.BiomeType;

public class MutableBiomeChange implements Change {

    private MutableBlockVector2 mutable = new MutableBlockVector2();
    private BiomeType from;
    private BiomeType to;

    public MutableBiomeChange() {
        this.from = new BiomeType(0);
        this.to = new BiomeType(0);
    }

    public void setBiome(int x, int z, int from, int to) {
        mutable.setComponents(x, z);
        this.from.setId(from);
        this.to.setId(to);
    }

    @Override
    public void undo(UndoContext context) throws WorldEditException {
        context.getExtent().setBiome(mutable, from);
    }

    @Override
    public void redo(UndoContext context) throws WorldEditException {
        context.getExtent().setBiome(mutable, to);
    }
}
