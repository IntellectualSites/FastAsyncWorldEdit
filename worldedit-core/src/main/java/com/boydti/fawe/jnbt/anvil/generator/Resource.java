package com.boydti.fawe.jnbt.anvil.generator;

import com.sk89q.worldedit.WorldEditException;

import java.util.Random;

public abstract class Resource {
    public Resource() {}

    public abstract boolean spawn(Random random, int x, int z) throws WorldEditException;
}
