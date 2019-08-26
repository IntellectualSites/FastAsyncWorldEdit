package com.boydti.fawe.bukkit.adapter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DelegateLock extends ReentrantLock {
    private final Lock parent;
    private volatile boolean modified;
    private final AtomicInteger count;

    public DelegateLock(Lock parent) {
        this.parent = parent;
        if (!(parent instanceof ReentrantLock)) {
            count = new AtomicInteger();
        } else {
            count = null;
        }
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    @Override
    public synchronized void lock() {
        modified = true;
        parent.lock();
        if (count != null) {
            count.incrementAndGet();
        }
    }

    @Override
    public synchronized void lockInterruptibly() throws InterruptedException {
        parent.lockInterruptibly();
    }

    @Override
    public synchronized boolean tryLock() {
        return parent.tryLock();
    }

    @Override
    public synchronized boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        return parent.tryLock(timeout, unit);
    }

    @Override
    public void unlock() {
        modified = true;
        parent.unlock();
        if (count != null) {
            if (count.getAndDecrement() <= 0) {
                count.incrementAndGet();
            }
        }
    }

    public Lock getParent() {
        return parent;
    }

    @Override
    public synchronized Condition newCondition() {
        return parent.newCondition();
    }

    @Override
    public synchronized int getHoldCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized boolean isHeldByCurrentThread() {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized boolean isLocked() {
        if (parent instanceof ReentrantLock) {
            return ((ReentrantLock) parent).isLocked();
        }
        return count.get() > 0;
    }

    public void untilFree() {
        if (parent instanceof ReentrantLock) {
            ReentrantLock rl = (ReentrantLock) parent;
            if (rl.isLocked()) {
                rl.lock();
                rl.unlock();
            }
            return;
        }
        while (count.get() > 0);
    }

    @Override
    public synchronized boolean hasWaiters(Condition condition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized int getWaitQueueLength(Condition condition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized String toString() {
        return parent.toString();
    }
}
