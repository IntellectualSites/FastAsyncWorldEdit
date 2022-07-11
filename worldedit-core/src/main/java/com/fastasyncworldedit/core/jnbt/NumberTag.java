package com.fastasyncworldedit.core.jnbt;

import com.sk89q.jnbt.Tag;
import org.enginehub.linbus.tree.LinTag;

/**
 * A numerical {@link Tag}
 */
public abstract class NumberTag extends Tag {

    protected NumberTag(LinTag linTag) {
        super(linTag);
    }

    @Override
    public abstract Number getValue();

}
