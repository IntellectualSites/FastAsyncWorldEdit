package com.boydti.fawe.object.number;

public final class MutableLong {
    private long value;

    public final void increment() {
        value++;
    }

    public void set(long value) {
        this.value = value;
    }

    public long get() {
        return value;
    }

    public void add(long amount) {
        this.value += amount;
    }
}
