package com.boydti.fawe.jnbt.streamer;

import java.io.IOException;

public interface InfoReader extends StreamReader<Integer> {
    void apply(int length, int type) throws IOException;

    @Override
    default void apply(int i, Integer value) throws IOException {
        apply(i, value.intValue());
    }
}
