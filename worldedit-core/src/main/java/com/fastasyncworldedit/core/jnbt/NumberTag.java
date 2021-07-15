package com.fastasyncworldedit.core.jnbt;

import com.sk89q.jnbt.Tag;

/**
 * A numerical {@link Tag}
 */
public abstract class NumberTag extends Tag {

    @Override
    public abstract Number getValue();

}
