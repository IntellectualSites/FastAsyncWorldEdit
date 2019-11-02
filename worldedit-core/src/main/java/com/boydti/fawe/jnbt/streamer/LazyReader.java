package com.boydti.fawe.jnbt.streamer;

import com.sk89q.jnbt.NBTInputStream;

import java.io.DataInputStream;
import java.io.IOException;

public interface LazyReader extends StreamReader<DataInputStream> {
    void apply(int index, NBTInputStream stream) throws IOException;
}
