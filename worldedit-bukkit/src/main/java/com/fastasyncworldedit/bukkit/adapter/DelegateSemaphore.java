package com.fastasyncworldedit.bukkit.adapter;

import java.util.concurrent.Semaphore;

public class DelegateSemaphore extends Semaphore {

    private final Semaphore delegate;

    public DelegateSemaphore(int permits, Semaphore delegate) {
        super(permits);
        this.delegate = delegate;
    }

    // this is bad
    @Override
    public synchronized boolean tryAcquire() {
        if (delegate == null) {
            return true;
        }
        try {
            this.delegate.acquire();
            return true;
        } catch (InterruptedException e) {
            return true;
        }
    }

    @Override
    public synchronized void acquire() throws InterruptedException {
        if (delegate == null) {
            return;
        }
        this.delegate.acquire();
    }

    @Override
    public void release() {
        if (delegate == null) {
            return;
        }
        this.delegate.release();
    }

}
