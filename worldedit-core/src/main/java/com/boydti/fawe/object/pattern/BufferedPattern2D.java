package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;

public class BufferedPattern2D extends BufferedPattern {

    public BufferedPattern2D(Actor fp, Pattern parent) {
        super(fp, parent);
    }

    @Override
    public boolean set(BlockVector3 pos) {
        return set.add(pos.getBlockX(), 0, pos.getBlockY());
    }
}
