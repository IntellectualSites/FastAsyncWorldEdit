package com.boydti.fawe.object.pattern;

import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.function.pattern.Pattern;

public class BufferedPattern2D extends BufferedPattern {

    public BufferedPattern2D(FawePlayer fp, Pattern parent) {
        super(fp, parent);
    }

    @Override
    public boolean set(Vector pos) {
        return set.add(pos.getBlockX(), 0, pos.getBlockY());
    }
}
