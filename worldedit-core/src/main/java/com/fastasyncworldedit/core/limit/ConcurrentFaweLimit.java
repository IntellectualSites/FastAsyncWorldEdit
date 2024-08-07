package com.fastasyncworldedit.core.limit;

import com.fastasyncworldedit.core.FaweCache;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Allows concurrent limit calculations
 *
 * @since TODO
 */
public class ConcurrentFaweLimit extends FaweLimit {

    public AtomicLong ATOMIC_MAX_CHANGES = new AtomicLong();
    public AtomicInteger ATOMIC_MAX_FAILS = new AtomicInteger();
    public AtomicLong ATOMIC_MAX_CHECKS = new AtomicLong();
    public AtomicInteger ATOMIC_MAX_ITERATIONS = new AtomicInteger();
    public AtomicInteger ATOMIC_MAX_BLOCKSTATES = new AtomicInteger();
    public AtomicInteger ATOMIC_MAX_ENTITIES = new AtomicInteger();

    public ConcurrentFaweLimit(FaweLimit other) {
        set(other);
    }

    @Override
    public boolean MAX_CHANGES() {
        return ATOMIC_MAX_CHANGES.decrementAndGet() < 0;
    }

    @Override
    public boolean MAX_FAILS() {
        return ATOMIC_MAX_FAILS.decrementAndGet() < 0;
    }

    @Override
    public boolean MAX_CHECKS() {
        return ATOMIC_MAX_CHECKS.decrementAndGet() < 0;
    }

    @Override
    public boolean MAX_ITERATIONS() {
        return ATOMIC_MAX_ITERATIONS.decrementAndGet() < 0;
    }

    @Override
    public boolean MAX_BLOCKSTATES() {
        return ATOMIC_MAX_BLOCKSTATES.decrementAndGet() < 0;
    }

    @Override
    public boolean MAX_ENTITIES() {
        return ATOMIC_MAX_ENTITIES.decrementAndGet() < 0;
    }

    @Override
    public void THROW_MAX_CHANGES() {
        if (ATOMIC_MAX_CHANGES.decrementAndGet() < 0) {
            throw FaweCache.MAX_CHANGES;
        }
    }

    @Override
    public void THROW_MAX_FAILS() {
        if (ATOMIC_MAX_FAILS.decrementAndGet() < 0) {
            throw FaweCache.MAX_FAILS;
        }
    }

    @Override
    public void THROW_MAX_CHECKS() {
        if (ATOMIC_MAX_CHECKS.decrementAndGet() < 0) {
            throw FaweCache.MAX_CHECKS;
        }
    }

    @Override
    public void THROW_MAX_ITERATIONS() {
        if (ATOMIC_MAX_ITERATIONS.decrementAndGet() < 0) {
            throw FaweCache.MAX_ITERATIONS;
        }
    }

    @Override
    public void THROW_MAX_BLOCKSTATES() {
        if (ATOMIC_MAX_BLOCKSTATES.decrementAndGet() < 0) {
            throw FaweCache.MAX_TILES;
        }
    }

    @Override
    public void THROW_MAX_ENTITIES() {
        if (ATOMIC_MAX_ENTITIES.decrementAndGet() < 0) {
            throw FaweCache.MAX_ENTITIES;
        }
    }

    @Override
    public void THROW_MAX_CHANGES(int amt) {
        if (amt == 0) {
            return;
        }
        if (ATOMIC_MAX_CHANGES.addAndGet(-amt) < 0) {
            throw FaweCache.MAX_CHANGES;
        }
    }

    @Override
    public void THROW_MAX_CHANGES(long amt) {
        if (amt == 0) {
            return;
        }
        if (ATOMIC_MAX_CHANGES.addAndGet(-amt) < 0) {
            throw FaweCache.MAX_CHANGES;
        }
    }

    @Override
    public void THROW_MAX_FAILS(int amt) {
        if (amt == 0) {
            return;
        }
        if (ATOMIC_MAX_FAILS.addAndGet(-amt) < 0) {
            throw FaweCache.MAX_FAILS;
        }
    }

    @Override
    public void THROW_MAX_CHECKS(int amt) {
        if (amt == 0) {
            return;
        }
        if (ATOMIC_MAX_CHECKS.addAndGet(-amt) < 0) {
            throw FaweCache.MAX_CHECKS;
        }
    }

    @Override
    public void THROW_MAX_CHECKS(long amt) {
        if (amt == 0) {
            return;
        }
        if (ATOMIC_MAX_CHECKS.addAndGet(-amt) < 0) {
            throw FaweCache.MAX_CHECKS;
        }
    }

    @Override
    public void THROW_MAX_ITERATIONS(int amt) {
        if (amt == 0) {
            return;
        }
        if (ATOMIC_MAX_ITERATIONS.addAndGet(-amt) < 0) {
            throw FaweCache.MAX_ITERATIONS;
        }
    }

    @Override
    public void THROW_MAX_BLOCKSTATES(int amt) {
        if (amt == 0) {
            return;
        }
        if (ATOMIC_MAX_BLOCKSTATES.addAndGet(-amt) < 0) {
            throw FaweCache.MAX_TILES;
        }
    }

    @Override
    public void THROW_MAX_ENTITIES(int amt) {
        if (amt == 0) {
            return;
        }
        if (ATOMIC_MAX_ENTITIES.addAndGet(-amt) < 0) {
            throw FaweCache.MAX_ENTITIES;
        }
    }

    @Override
    public void set(FaweLimit other) {
        super.set(other);
        ATOMIC_MAX_CHANGES.set(other.MAX_CHANGES);
        ATOMIC_MAX_FAILS.set(other.MAX_FAILS);
        ATOMIC_MAX_CHECKS.set(other.MAX_CHECKS);
        ATOMIC_MAX_ITERATIONS.set(other.MAX_ITERATIONS);
        ATOMIC_MAX_BLOCKSTATES.set(other.MAX_BLOCKSTATES);
        ATOMIC_MAX_ENTITIES.set(other.MAX_ENTITIES);
    }

    @Override
    public FaweLimit getLimitUsed(FaweLimit originalLimit) {
        FaweLimit newLimit = new FaweLimit();
        newLimit.MAX_CHANGES = originalLimit.MAX_CHANGES - this.ATOMIC_MAX_CHANGES.get();
        newLimit.MAX_FAILS = originalLimit.MAX_FAILS - this.ATOMIC_MAX_FAILS.get();
        newLimit.MAX_CHECKS = originalLimit.MAX_CHECKS - this.ATOMIC_MAX_CHECKS.get();
        newLimit.MAX_ITERATIONS = originalLimit.MAX_ITERATIONS - this.ATOMIC_MAX_ITERATIONS.get();
        newLimit.MAX_BLOCKSTATES = originalLimit.MAX_BLOCKSTATES - this.ATOMIC_MAX_BLOCKSTATES.get();
        newLimit.MAX_ENTITIES = originalLimit.MAX_ENTITIES - this.ATOMIC_MAX_ENTITIES.get();
        return newLimit;
    }

}
