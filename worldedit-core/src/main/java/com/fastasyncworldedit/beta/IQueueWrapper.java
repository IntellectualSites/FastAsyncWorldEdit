package com.fastasyncworldedit.beta;

public interface IQueueWrapper {
    default IQueueExtent<IQueueChunk> wrapQueue(IQueueExtent<IQueueChunk> queue) {
        return queue;
    }
}
