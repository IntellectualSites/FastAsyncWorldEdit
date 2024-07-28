package com.fastasyncworldedit.core.jnbt;

import com.sk89q.jnbt.Tag;
import org.enginehub.linbus.tree.LinTag;

/**
 * A numerical {@link Tag}
 */
public abstract class NumberTag<LT extends LinTag<? extends Number>> extends Tag<Number, LT> {

    protected NumberTag(LT linTag) {
        super(linTag);
    }

    @Override
    public abstract Number getValue();

}
