package com.boydti.fawe.jnbt.streamer;

import java.io.IOException;

public interface StreamReader<T> {
    void apply(int i, T value) throws IOException;
}
