package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.collection.LocalBlockVectorSet;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;

public class ScatterOverlayBrush extends ScatterBrush {
    public ScatterOverlayBrush(int count, int distance) {
        super(count, distance);
    }

    @Override
    public void apply(EditSession editSession, LocalBlockVectorSet placed, BlockVector3 pt, Pattern p, double size) throws MaxChangedBlocksException {
        int x = pt.getBlockX();
        int y = pt.getBlockY();
        int z = pt.getBlockZ();
        BlockVector3 dir = getDirection(pt);
//        dir.setComponents(x + dir.getBlockX(), y + dir.getBlockY(), z + dir.getBlockZ());
        editSession.setBlock(x + dir.getBlockX(), y + dir.getBlockY(), z + dir.getBlockZ(), p);
    }
}