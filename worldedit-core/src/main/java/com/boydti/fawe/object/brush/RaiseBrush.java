package com.boydti.fawe.object.brush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;

public class RaiseBrush extends ErodeBrush {
    public RaiseBrush() {
        this(6, 0, 1, 1);
    }
    public RaiseBrush(int erodeFaces, int erodeRec, int fillFaces, int fillRec) {
        super(2, 1, 5, 1);
    }
}
