package com.fastasyncworldedit.core.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.StampedLock;

/**
 * Allows for reentrant behaviour of a wrapped {@link StampedLock}. Will not count the number of times it is re-entered.
 *
 * @since TODO
 */
public class ReentrantWrappedStampedLock implements Lock {

    private final StampedLock parent = new StampedLock();
    private volatile Thread owner;
    private volatile long stamp = 0;

    @Override
    public void lock() {
        if (Thread.currentThread() == owner) {
            return;
        }
        stamp = parent.writeLock();
        owner = Thread.currentThread();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        if (Thread.currentThread() == owner) {
            return;
        }
        stamp = parent.writeLockInterruptibly();
        owner = Thread.currentThread();
    }

    @Override
    public boolean tryLock() {
        if (Thread.currentThread() == owner) {
            return true;
        }
        if (parent.isWriteLocked()) {
            return false;
        }
        stamp = parent.writeLock();
        owner = Thread.currentThread();
        return true;
    }

    @Override
    public boolean tryLock(final long time, @NotNull final TimeUnit unit) throws InterruptedException {
        if (Thread.currentThread() == owner) {
            return true;
        }
        if (!parent.isWriteLocked()) {
            stamp = parent.writeLock();
            owner = Thread.currentThread();
            return true;
        }
        stamp = parent.tryWriteLock(time, unit);
        owner = Thread.currentThread();
        return false;
    }

    @Override
    public void unlock() {
        if (owner != Thread.currentThread()) {
            throw new IllegalCallerException("The lock should only be unlocked by the owning thread when a stamp is not supplied");
        }
        unlock(stamp);
    }

    @NotNull
    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException("Conditions are not supported by StampedLock");
    }

    /**
     * Retrieves the stamp associated with the current lock. 0 if the wrapped {@link StampedLock} is not write-locked. This method is
     * thread-checking.
     *
     * @return lock stam[ or 0 if not locked.
     * @throws IllegalCallerException if the {@link StampedLock} is write-locked and the calling thread is not the lock owner
     * @since TODO
     */
    public long getStampChecked() {
        if (stamp != 0 && owner != Thread.currentThread()) {
            throw new IllegalCallerException("The stamp should be be acquired by a thread that does not own the lock");
        }
        return stamp;
    }

    /**
     * Unlock the wrapped {@link StampedLock} using the given stamp. This can be called by any thread.
     *
     * @param stamp Stamp to unlock with
     * @throws IllegalMonitorStateException if the given stamp does not match the lock's stamp
     * @since TODO
     */
    public void unlock(final long stamp) {
        parent.unlockWrite(stamp);
        this.stamp = 0;
        owner = null;
    }

    /**
     * Returns true if the lock is currently held.
     *
     * @return true if the lock is currently held.
     * @since TODO
     */
    public boolean isLocked() {
        return owner == null && this.stamp == 0 && parent.isWriteLocked(); // Be verbose
    }

}
