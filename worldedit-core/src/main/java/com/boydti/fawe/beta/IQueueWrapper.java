package com.boydti.fawe.beta;

import com.boydti.fawe.beta.IQueueExtent;

public interface IQueueWrapper {
    default IQueueExtent wrapQueue(IQueueExtent queue) {
        return queue;
    }
}
