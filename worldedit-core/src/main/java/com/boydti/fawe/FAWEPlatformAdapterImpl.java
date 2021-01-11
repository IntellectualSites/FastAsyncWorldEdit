package com.boydti.fawe;

import com.boydti.fawe.beta.IChunkGet;

public interface FAWEPlatformAdapterImpl {

    void sendChunk(IChunkGet chunk, int mask, boolean lighting);

}
