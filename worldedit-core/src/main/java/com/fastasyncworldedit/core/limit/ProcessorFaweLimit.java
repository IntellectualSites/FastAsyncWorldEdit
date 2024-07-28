package com.fastasyncworldedit.core.limit;

import com.fastasyncworldedit.core.FaweCache;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Allows concurrent limit calculations for THROW_MAX_XXX(amount) methods. Other methods use the default implementations in
 * {@link FaweLimit}
 *
 * @since TODO
 */
public class ProcessorFaweLimit extends FaweLimit {

    public AtomicLong ATOMIC_MAX_CHANGES = new AtomicLong();
    public AtomicInteger ATOMIC_MAX_FAILS = new AtomicInteger();
    public AtomicLong ATOMIC_MAX_CHECKS = new AtomicLong();
    public AtomicInteger ATOMIC_MAX_ITERATIONS = new AtomicInteger();
    public AtomicInteger ATOMIC_MAX_BLOCKSTATES = new AtomicInteger();
    public AtomicInteger ATOMIC_MAX_ENTITIES = new AtomicInteger();

    public ProcessorFaweLimit(FaweLimit other) {
        set(other);
    }

    @Override
    public void THROW_MAX_CHANGES(int amt) {
        if (amt == 0) {
            return;
        }
        final long changes = MAX_CHANGES;
        if (ATOMIC_MAX_CHANGES.updateAndGet(i -> (MAX_CHANGES = Math.min(i, changes) - amt)) < 0) {
            throw FaweCache.MAX_CHANGES;
        }
    }

    @Override
    public void THROW_MAX_CHANGES(long amt) {
        if (amt == 0) {
            return;
        }
        final long changes = MAX_CHANGES;
        if (ATOMIC_MAX_CHANGES.updateAndGet(i -> (MAX_CHANGES = Math.min(i, changes) - amt))< 0) {
            throw FaweCache.MAX_CHANGES;
        }
    }

    @Override
    public void THROW_MAX_FAILS(int amt) {
        if (amt == 0) {
            return;
        }
        final int fails = MAX_FAILS;
        if (ATOMIC_MAX_FAILS.updateAndGet(i -> (MAX_FAILS = Math.min(i, fails) - amt)) < 0) {
            throw FaweCache.MAX_FAILS;
        }
    }

    @Override
    public void THROW_MAX_CHECKS(int amt) {
        final long checks = MAX_CHECKS;
        if (ATOMIC_MAX_CHECKS.updateAndGet(i -> (MAX_CHECKS = Math.min(i, checks) - amt)) < 0) {
            throw FaweCache.MAX_CHECKS;
        }
    }

    @Override
    public void THROW_MAX_CHECKS(long amt) {
        if (amt == 0) {
            return;
        }
        final long checks = MAX_CHECKS;
        if (ATOMIC_MAX_CHECKS.updateAndGet(i -> (MAX_CHECKS = Math.min(i, checks) - amt)) < 0) {
            throw FaweCache.MAX_CHECKS;
        }
    }

    @Override
    public void THROW_MAX_ITERATIONS(int amt) {
        if (amt == 0) {
            return;
        }
        final int iterations = MAX_ITERATIONS;
        if (ATOMIC_MAX_ITERATIONS.updateAndGet(i -> (MAX_ITERATIONS = Math.min(i, iterations) - amt)) < 0) {
            throw FaweCache.MAX_ITERATIONS;
        }
    }

    @Override
    public void THROW_MAX_BLOCKSTATES(int amt) {
        if (amt == 0) {
            return;
        }
        final int states = MAX_BLOCKSTATES;
        if (ATOMIC_MAX_BLOCKSTATES.updateAndGet(i -> (MAX_BLOCKSTATES = Math.min(i, states) - amt)) < 0) {
            throw FaweCache.MAX_TILES;
        }
    }

    @Override
    public void THROW_MAX_ENTITIES(int amt) {
        if (amt == 0) {
            return;
        }
        final int entities = MAX_ENTITIES;
        if (ATOMIC_MAX_ENTITIES.updateAndGet(i -> (MAX_ENTITIES = Math.min(i, entities) - amt)) < 0) {
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
        newLimit.MAX_CHANGES = originalLimit.MAX_CHANGES - Math.min(this.ATOMIC_MAX_CHANGES.get(), MAX_CHANGES);
        newLimit.MAX_FAILS = originalLimit.MAX_FAILS - Math.min(this.ATOMIC_MAX_FAILS.get(), MAX_FAILS);
        newLimit.MAX_CHECKS = originalLimit.MAX_CHECKS - Math.min(this.ATOMIC_MAX_CHECKS.get(), MAX_CHECKS);
        newLimit.MAX_ITERATIONS = originalLimit.MAX_ITERATIONS - Math.min(this.ATOMIC_MAX_ITERATIONS.get(), MAX_ITERATIONS);
        newLimit.MAX_BLOCKSTATES = originalLimit.MAX_BLOCKSTATES - Math.min(this.ATOMIC_MAX_BLOCKSTATES.get(), MAX_BLOCKSTATES);
        newLimit.MAX_ENTITIES = originalLimit.MAX_ENTITIES - Math.min(this.ATOMIC_MAX_ENTITIES.get(), MAX_ENTITIES);
        return newLimit;
    }

}
