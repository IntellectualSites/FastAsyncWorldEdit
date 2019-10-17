package com.boydti.fawe.object.queue;

import com.boydti.fawe.object.FaweQueue;

public class DelegateFaweQueue implements IDelegateFaweQueue {
    private FaweQueue parent;

    public DelegateFaweQueue(FaweQueue parent) {
        this.parent = parent;
    }

    public FaweQueue getParent() {
        return parent;
    }

    public void setParent(FaweQueue parent) {
        this.parent = parent;
        setWorld(getQueue().getWorldName());
    }

    @Override
    public FaweQueue getQueue() {
        return parent;
    }
}
