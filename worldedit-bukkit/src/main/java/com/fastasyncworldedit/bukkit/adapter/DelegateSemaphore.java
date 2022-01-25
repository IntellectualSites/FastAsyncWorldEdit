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
    public boolean tryAcquire() {
        try {
            this.delegate.acquire();
            return true;
        } catch (InterruptedException e) {
            return true;
        }
    }

    @Override
    public void acquire() throws InterruptedException {
        this.delegate.acquire();
    }

    @Override
    public void release() {
        this.delegate.release();
    }

}
