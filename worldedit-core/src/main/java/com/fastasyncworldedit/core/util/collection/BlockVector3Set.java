package com.fastasyncworldedit.core.util.collection;

import com.sk89q.worldedit.math.BlockVector3;

import java.util.Set;

public interface BlockVector3Set extends Set<BlockVector3> {

    boolean add(int x, int y, int z);

}
