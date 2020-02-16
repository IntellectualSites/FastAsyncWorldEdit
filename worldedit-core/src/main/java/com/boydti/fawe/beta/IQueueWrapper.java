package com.boydti.fawe.beta;

import com.boydti.fawe.beta.IQueueExtent;

public interface IQueueWrapper {
    default IQueueExtent<IQueueChunk> wrapQueue(IQueueExtent<IQueueChunk> queue) {
        return queue;
    }
}
