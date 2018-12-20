package com.boydti.fawe.object;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class RunnableVal<T> implements Runnable, Supplier<T>, Consumer<T> {
    public T value;

    public RunnableVal() {
    }

    public RunnableVal(T value) {
        this.value = value;
    }

    @Override
    public final void run() {
        run(this.value);
    }

    public final T runAndGet() {
        run();
        return value;
    }

    public abstract void run(T value);

    @Override
    public T get() {
        return runAndGet();
    }

    @Override
    public void accept(T t) {
        run(value);
    }
}
