package com.fastasyncworldedit.bukkit.adapter;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DelegateLock extends ReentrantLockWithGetOwner {

    private final ReentrantLock parent;
    private volatile boolean modified;
    private final AtomicInteger count;

    public DelegateLock(@Nonnull ReentrantLock parent) {
        this.parent = parent;
        this.count = null;
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
        return parent.getHoldCount();
    }

    @Override
    public synchronized boolean isHeldByCurrentThread() {
        return parent.isHeldByCurrentThread();
    }

    @Override
    public synchronized boolean isLocked() {
        return parent.isLocked();
    }

    public void untilFree() {
        ReentrantLock rl = parent;
        if (rl.isLocked()) {
            rl.lock();
            rl.unlock();
        }
    }

    @Override
    public synchronized boolean hasWaiters(Condition condition) {
        return parent.hasWaiters(condition);
    }

    @Override
    public synchronized int getWaitQueueLength(Condition condition) {
        return parent.getWaitQueueLength(condition);
    }

    @Override
    public synchronized String toString() {
        return parent.toString();
    }

}
