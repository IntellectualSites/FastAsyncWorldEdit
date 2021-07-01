package com.fastasyncworldedit.core;

import com.fastasyncworldedit.core.beta.IChunkGet;

public interface FAWEPlatformAdapterImpl {

    void sendChunk(IChunkGet chunk, int mask, boolean lighting);

}
