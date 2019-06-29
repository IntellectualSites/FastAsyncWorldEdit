package com.boydti.fawe.example;

import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.RunnableVal;
import java.util.Collection;

public interface IFaweQueueMap {

    Collection<FaweChunk> getFaweChunks();

    void forEachChunk(RunnableVal<FaweChunk> onEach);

    FaweChunk getFaweChunk(int cx, int cz);

    FaweChunk getCachedFaweChunk(int cx, int cz);

    void add(FaweChunk chunk);

    void clear();

    int size();

    boolean next(int size, long time);
}
