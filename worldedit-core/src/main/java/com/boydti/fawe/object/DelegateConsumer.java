package com.boydti.fawe.object;

import java.util.function.Consumer;

public abstract class DelegateConsumer<T> implements Consumer<T> {
    private final Consumer parent;

    public DelegateConsumer(Consumer parent) {
        this.parent = parent;
    }

    @Override
    public void accept(T o) {
        parent.accept(o);
    }
}
