package com.boydti.fawe.bukkit.beta;

import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.implementation.QueueHandler;

public class BukkitQueueHandler extends QueueHandler {
    @Override
    public IQueueExtent create() {
        return new BukkitQueue();
    }
}
