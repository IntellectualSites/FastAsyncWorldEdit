package com.boydti.fawe.jnbt.streamer;

public interface InfoReader extends StreamReader<Integer> {
    void apply(int length, int type);

    @Override
    default void apply(int i, Integer value) {
        apply(i, value.intValue());
    }
}
