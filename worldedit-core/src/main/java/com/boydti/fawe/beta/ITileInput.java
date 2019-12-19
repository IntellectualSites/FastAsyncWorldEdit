package com.boydti.fawe.beta;

import com.sk89q.jnbt.CompoundTag;

public interface ITileInput {
    CompoundTag getTile(int x, int y, int z);
}
