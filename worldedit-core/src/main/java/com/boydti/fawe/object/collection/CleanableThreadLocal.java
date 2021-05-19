package com.boydti.fawe.object.collection;

import com.boydti.fawe.util.MainUtil;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CleanableThreadLocal<T> extends ThreadLocal<T> implements AutoCloseable {
    private final Supplier<T> supplier;
    private final Function<T, T> modifier;
    private final LongAdder count = new LongAdder(); // what is that supposed to do?

    public CleanableThreadLocal(Supplier<T> supplier) {
        this(supplier, Function.identity());
    }

    public CleanableThreadLocal(Supplier<T> supplier, Consumer<T> modifier) {
        this(supplier, t -> {
            modifier.accept(t);
            return t;
        });
    }

    public CleanableThreadLocal(Supplier<T> supplier, Function<T, T> modifier) {
        this.supplier = supplier;
        this.modifier = modifier;
    }

    @Override
    protected final T initialValue() {
        T value = modifier.apply(init());
        if (value != null) {
            count.increment();
        }
        return value;
    }

    public T init() {
        return supplier.get();
    }

    public void clean() {
        remove();
    }

    @Override
    public void close() throws IOException {
        clean();
    }
}
