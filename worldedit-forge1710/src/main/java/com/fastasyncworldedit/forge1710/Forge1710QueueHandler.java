package com.fastasyncworldedit.forge1710;

import com.fastasyncworldedit.core.queue.implementation.QueueHandler;

/**
 * 1.7.10 has no async catcher or timings to toggle while FAWE edits chunks.
 */
public class Forge1710QueueHandler extends QueueHandler {

    @Override
    public void startUnsafe(boolean parallel) {
    }

    @Override
    public void endUnsafe(boolean parallel) {
    }

}
