package com.fastasyncworldedit.nukkitmot;

import com.fastasyncworldedit.core.FAWEPlatformAdapterImpl;
import com.fastasyncworldedit.core.queue.IChunkGet;

/**
 * Nukkit platform adapter for FAWE chunk sending.
 */
public class NukkitPlatformAdapter implements FAWEPlatformAdapterImpl {

    @Override
    public void sendChunk(IChunkGet chunk, int mask, boolean lighting) {
        // Chunk sending is handled in NukkitGetBlocks.send()
    }

}
