package com.boydti.fawe.jnbt.streamer;

public interface ElemReader<T> extends StreamReader<T> {
    void apply(int index, T value);
}
