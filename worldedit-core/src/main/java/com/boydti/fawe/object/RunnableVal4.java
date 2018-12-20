package com.boydti.fawe.object;

public abstract class RunnableVal4<T, U, V, W> implements Runnable {
    public T value1;
    public U value2;
    public V value3;
    public W value4;

    public RunnableVal4() {
    }

    public RunnableVal4(T value1, U value2, V value3, W value4) {
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
        this.value4 = value4;
    }

    @Override
    public void run() {
        run(value1, value2, value3, value4);
    }

    public abstract void run(T value1, U value2, V value3, W value4);
}
