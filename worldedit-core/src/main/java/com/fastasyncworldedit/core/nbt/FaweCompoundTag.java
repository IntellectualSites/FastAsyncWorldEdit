package com.fastasyncworldedit.core.nbt;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import org.enginehub.linbus.tree.LinCompoundTag;

/**
 * A wrapper around compound tags, potentially lazily transformed.
 * @since TODO
 */
public sealed interface FaweCompoundTag permits EagerFaweCompoundTag, LazyFaweCompoundTag {

    /**
     * {@return a lazy compound component backed by a lazy reference}
     * @param lazyReference the lazy reference to the actual compound tag
     */
    static FaweCompoundTag of(LazyReference<? extends LinCompoundTag> lazyReference) {
        return new LazyFaweCompoundTag(lazyReference::getValue);
    }

    /**
     * {@return a lazy compound component backed by a supplier}
     * Invocations to the supplier are memoized.
     * @param supplier the supplier for the actual compound tag
     */
    static FaweCompoundTag of(Supplier<? extends LinCompoundTag> supplier) {
        return new LazyFaweCompoundTag(Suppliers.memoize(supplier));
    }

    /**
     * {@return a direct reference tho the given compound tag}
     * @param linCompoundTag the tag to wrap
     */
    static FaweCompoundTag of(LinCompoundTag linCompoundTag) {
        return new EagerFaweCompoundTag(linCompoundTag);
    }

    /**
     * {@return the underlying tag}
     */
    LinCompoundTag linTag();

}
