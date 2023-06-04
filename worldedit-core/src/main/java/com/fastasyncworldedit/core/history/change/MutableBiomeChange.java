package com.fastasyncworldedit.core.history.change;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;

public class MutableBiomeChange implements Change {

    private final MutableBlockVector3 mutable = new MutableBlockVector3();
    private int from;
    private int to;

    public MutableBiomeChange() {
        this.from = BlockTypesCache.ReservedIDs.__RESERVED__;
        this.to = BlockTypesCache.ReservedIDs.__RESERVED__;
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
