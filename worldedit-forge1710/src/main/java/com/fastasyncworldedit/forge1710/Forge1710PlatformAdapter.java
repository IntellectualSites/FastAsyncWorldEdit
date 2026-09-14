package com.fastasyncworldedit.forge1710;

import com.fastasyncworldedit.core.FAWEPlatformAdapterImpl;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.forge1710.world.Forge1710GetBlocks;
import com.fastasyncworldedit.core.util.TaskManager;

public class Forge1710PlatformAdapter implements FAWEPlatformAdapterImpl {

    @Override
    public void sendChunk(IChunkGet chunk, int mask, boolean lighting) {
        if (chunk instanceof Forge1710GetBlocks getBlocks) {
            TaskManager.taskManager().task(getBlocks::resendToWatchers);
        }
    }

}
