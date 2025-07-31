package com.fastasyncworldedit.core;

import com.fastasyncworldedit.core.queue.IChunkGet;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface FAWEPlatformAdapterImpl {

    void sendChunk(IChunkGet chunk, int mask, boolean lighting, boolean obfuscateAntiXRay);

}
