package com.sk89q.worldedit.function.generator;

import com.sk89q.worldedit.WorldEditException;

import java.util.Random;

public interface Resource {

    boolean spawn(Random random, int x, int z) throws WorldEditException;
}
