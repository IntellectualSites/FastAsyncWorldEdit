package com.fastasyncworldedit;

import com.fastasyncworldedit.beta.IChunkGet;

public interface FAWEPlatformAdapterImpl {

    void sendChunk(IChunkGet chunk, int mask, boolean lighting);

}
