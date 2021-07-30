package com.fastasyncworldedit.core.queue;

public interface IQueueWrapper {

    default IQueueExtent<IQueueChunk> wrapQueue(IQueueExtent<IQueueChunk> queue) {
        return queue;
    }

}
