package com.boydti.fawe.object;

import java.util.function.Supplier;

public interface Lazy<T> extends Supplier<T> {
    Supplier<T> init();
    public default T get() { return init().get(); }
}