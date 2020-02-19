package com.boydti.fawe.beta;

public interface IQueueWrapper {
    default IQueueExtent<IQueueChunk> wrapQueue(IQueueExtent<IQueueChunk> queue) {
        return queue;
    }
}
