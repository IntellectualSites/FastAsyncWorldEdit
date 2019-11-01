package com.boydti.fawe.jnbt.streamer;

public interface StreamReader<T> {
    void apply(int i, T value);
}
