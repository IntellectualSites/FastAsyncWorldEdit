package com.sk89q.jnbt.fawe;

import com.sk89q.jnbt.Tag;

/**
 * A numerical {@link Tag}
 */
public abstract class NumberTag extends Tag {

    @Override
    public abstract Number getValue();

}
