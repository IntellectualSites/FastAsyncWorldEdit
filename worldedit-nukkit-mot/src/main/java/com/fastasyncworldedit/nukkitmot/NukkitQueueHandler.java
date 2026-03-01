package com.fastasyncworldedit.nukkitmot;

import com.fastasyncworldedit.core.queue.implementation.QueueHandler;

/**
 * Nukkit-specific queue handler.
 * Nukkit has no AsyncCatcher or Timings system to manage, so these are no-ops.
 */
public class NukkitQueueHandler extends QueueHandler {

    @Override
    public void startUnsafe(boolean parallel) {
        // Nukkit has no AsyncCatcher or Timings to disable
    }

    @Override
    public void endUnsafe(boolean parallel) {
        // Nukkit has no AsyncCatcher or Timings to re-enable
    }

}
