package com.boydti.fawe.bukkit.beta;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class DelegateLock extends ReentrantLock {
    private final ReentrantLock parent;
    private volatile boolean modified;

    public DelegateLock(ReentrantLock parent) {
        this.parent = parent;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    @Override
    public void lock() {
        modified = true;
        parent.lock();
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
        this.notifyAll();
    }

    public ReentrantLock getParent() {
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
