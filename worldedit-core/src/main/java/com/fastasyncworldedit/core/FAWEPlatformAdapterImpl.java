package com.fastasyncworldedit.core;

import com.fastasyncworldedit.core.queue.IChunkGet;

public interface FAWEPlatformAdapterImpl {

    void sendChunk(IChunkGet chunk, int mask, boolean lighting);

}
