package com.fastasyncworldedit.core.command.tool.brush;

import com.fastasyncworldedit.core.math.LocalBlockVectorSet;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.fastasyncworldedit.core.util.collection.BlockVector3Set;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;

public class ScatterOverlayBrush extends ScatterBrush {

    public ScatterOverlayBrush(int count, int distance) {
        super(count, distance);
    }

    @Override
    @Deprecated(forRemoval = true, since = "TODO")
    public void apply(
            EditSession editSession,
            LocalBlockVectorSet placed,
            BlockVector3 position,
            Pattern p,
            double size
    ) throws MaxChangedBlocksException {
        apply(editSession, LocalBlockVectorSet.wrap(placed), position, p, size);
    }

    @Override
    public void apply(
            EditSession editSession,
            BlockVector3Set placed,
            BlockVector3 pt,
            Pattern p,
            double size
    ) throws
            MaxChangedBlocksException {
        final int x = pt.x();
        final int y = pt.y();
        final int z = pt.z();
        BlockVector3 dir = getDirection(pt);
        if (dir == null) {
            getDir:
            {
                MutableBlockVector3 mut = new MutableBlockVector3(pt);
                for (int yy = 0; yy < size; yy++) {
                    if ((dir = getDirection(mut.mutY(y + yy))) != null) {
                        break getDir;
                    }
                }
                for (int yy = 0; yy > -size; yy--) {
                    if ((dir = getDirection(mut.mutY(y - yy))) != null) {
                        break getDir;
                    }
                }
                return;
            }
        }
        editSession.setBlock(x + dir.x(), y + dir.y(), z + dir.z(), p);
    }

}
