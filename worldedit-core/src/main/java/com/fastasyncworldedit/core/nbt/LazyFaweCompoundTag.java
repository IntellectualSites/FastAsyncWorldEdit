package com.fastasyncworldedit.core.nbt;

import org.enginehub.linbus.tree.LinCompoundTag;

import java.util.function.Supplier;

record LazyFaweCompoundTag(Supplier<? extends LinCompoundTag> linTagSupplier) implements FaweCompoundTag {

    @Override
    public LinCompoundTag linTag() {
        return this.linTagSupplier().get();
    }

}
