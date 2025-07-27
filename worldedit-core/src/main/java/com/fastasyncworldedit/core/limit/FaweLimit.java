package com.fastasyncworldedit.core.limit;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.configuration.Settings;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FaweLimit {

    public int MAX_ACTIONS = 0;
    public AtomicLong MAX_CHANGES = new AtomicLong();
    public AtomicInteger MAX_FAILS = new AtomicInteger();
    public AtomicLong MAX_CHECKS = new AtomicLong();
    public AtomicInteger MAX_ITERATIONS = new AtomicInteger();
    public AtomicInteger MAX_BLOCKSTATES = new AtomicInteger();
    public AtomicInteger MAX_ENTITIES = new AtomicInteger();
    public int MAX_HISTORY = 0;
    public int SCHEM_FILE_SIZE_LIMIT = 0;
    public int SCHEM_FILE_NUM_LIMIT = 0;
    public int MAX_EXPRESSION_MS = 0;
    public int MAX_RADIUS = 0;
    public int MAX_SUPER_PICKAXE_SIZE = 0;
    public int MAX_BRUSH_RADIUS = 0;
    public int MAX_BUTCHER_RADIUS = 0;
    public int INVENTORY_MODE = Integer.MAX_VALUE;
    public int SPEED_REDUCTION = Integer.MAX_VALUE;
    public boolean FAST_PLACEMENT = false;
    public boolean CONFIRM_LARGE = true;
    public boolean RESTRICT_HISTORY_TO_REGIONS = true;
    public boolean ALLOW_LEGACY = true;
    public boolean SKIP_ENTITY_SPAWN_EVENTS = false;
    public Set<String> STRIP_NBT = null;
    public boolean UNIVERSAL_DISALLOWED_BLOCKS = true;
    public Set<String> DISALLOWED_BLOCKS = null;
    public Set<PropertyRemap<?>> REMAP_PROPERTIES = null;

    public static FaweLimit MAX;

    static {
        MAX = new FaweLimit() {
            @Override
            public boolean MAX_CHANGES() {
                return true;
            }

            @Override
            public boolean MAX_BLOCKSTATES() {
                return true;
            }

            @Override
            public boolean MAX_CHECKS() {
                return true;
            }

            @Override
            public boolean MAX_ENTITIES() {
                return true;
            }

            @Override
            public boolean MAX_FAILS() {
                return true;
            }

            @Override
            public boolean MAX_ITERATIONS() {
                return true;
            }

            @Override
            public boolean isUnlimited() {
                return true;
            }

            public void THROW_MAX_CHANGES() {
            }

            public void THROW_MAX_FAILS() {
            }

            public void THROW_MAX_CHECKS() {
            }

            public void THROW_MAX_ITERATIONS() {
            }

            public void THROW_MAX_BLOCKSTATES() {
            }

            public void THROW_MAX_ENTITIES() {
            }

            public void THROW_MAX_CHANGES(int amt) {
            }

            public void THROW_MAX_FAILS(int amt) {
            }

            public void THROW_MAX_CHECKS(int amt) {
            }

            public void THROW_MAX_ITERATIONS(int amt) {
            }

            public void THROW_MAX_BLOCKSTATES(int amt) {
            }

            public void THROW_MAX_ENTITIES(int amt) {
            }
        };
        MAX.SPEED_REDUCTION = 0;
        MAX.INVENTORY_MODE = 0;
        MAX.MAX_ACTIONS = 1;
        MAX.MAX_CHANGES = new AtomicLong(Long.MAX_VALUE);
        MAX.MAX_FAILS = new AtomicInteger(Integer.MAX_VALUE);
        MAX.MAX_CHECKS = new AtomicLong(Long.MAX_VALUE);
        MAX.MAX_ITERATIONS = new AtomicInteger(Integer.MAX_VALUE);
        MAX.MAX_BLOCKSTATES = new AtomicInteger(Integer.MAX_VALUE);
        MAX.MAX_ENTITIES = new AtomicInteger(Integer.MAX_VALUE);
        MAX.MAX_HISTORY = Integer.MAX_VALUE;
        MAX.SCHEM_FILE_NUM_LIMIT = Integer.MAX_VALUE;
        MAX.SCHEM_FILE_SIZE_LIMIT = Integer.MAX_VALUE;
        MAX.MAX_EXPRESSION_MS = 50;
        MAX.FAST_PLACEMENT = true;
        MAX.CONFIRM_LARGE =
                Settings.settings().LIMITS.get("default").CONFIRM_LARGE || Settings.settings().GENERAL.LIMIT_UNLIMITED_CONFIRMS;
        MAX.RESTRICT_HISTORY_TO_REGIONS = false;
        MAX.STRIP_NBT = Collections.emptySet();
        MAX.UNIVERSAL_DISALLOWED_BLOCKS = false;
        MAX.ALLOW_LEGACY = true;
        MAX.DISALLOWED_BLOCKS = Collections.emptySet();
        MAX.REMAP_PROPERTIES = Collections.emptySet();
        MAX.MAX_RADIUS = Integer.MAX_VALUE;
        MAX.MAX_SUPER_PICKAXE_SIZE = Integer.MAX_VALUE;
        MAX.MAX_BRUSH_RADIUS = Integer.MAX_VALUE;
        MAX.MAX_BUTCHER_RADIUS = Integer.MAX_VALUE;
    }

    public boolean MAX_CHANGES() {
        return MAX_CHANGES.decrementAndGet() < 0;
    }

    public boolean MAX_FAILS() {
        return MAX_FAILS.decrementAndGet() < 0;
    }

    public boolean MAX_CHECKS() {
        return MAX_CHECKS.decrementAndGet() < 0;
    }

    public boolean MAX_ITERATIONS() {
        return MAX_ITERATIONS.decrementAndGet() < 0;
    }

    public boolean MAX_BLOCKSTATES() {
        return MAX_BLOCKSTATES.decrementAndGet() < 0;
    }

    public boolean MAX_ENTITIES() {
        return MAX_ENTITIES.decrementAndGet() < 0;
    }

    public void THROW_MAX_CHANGES() {
        if (MAX_CHANGES.decrementAndGet() < 0) {
            throw FaweCache.MAX_CHANGES;
        }
    }

    public void THROW_MAX_FAILS() {
        if (MAX_FAILS.decrementAndGet() < 0) {
            throw FaweCache.MAX_FAILS;
        }
    }

    public void THROW_MAX_CHECKS() {
        if (MAX_CHECKS.decrementAndGet() < 0) {
            throw FaweCache.MAX_CHECKS;
        }
    }

    public void THROW_MAX_ITERATIONS() {
        if (MAX_ITERATIONS.decrementAndGet() < 0) {
            throw FaweCache.MAX_ITERATIONS;
        }
    }

    public void THROW_MAX_BLOCKSTATES() {
        if (MAX_BLOCKSTATES.decrementAndGet() < 0) {
            throw FaweCache.MAX_TILES;
        }
    }

    public void THROW_MAX_ENTITIES() {
        if (MAX_ENTITIES.decrementAndGet() < 0) {
            throw FaweCache.MAX_ENTITIES;
        }
    }

    public void THROW_MAX_CHANGES(int amt) {
        if (amt == 0) {
            return;
        }
        if (MAX_CHANGES.addAndGet(-amt) < 0) {
            throw FaweCache.MAX_CHANGES;
        }
    }

    public void THROW_MAX_CHANGES(long amt) {
        if (amt == 0) {
            return;
        }
        if (MAX_CHANGES.addAndGet(-amt) < 0) {
            throw FaweCache.MAX_CHANGES;
        }
    }

    public void THROW_MAX_FAILS(int amt) {
        if (amt == 0) {
            return;
        }
        if (MAX_FAILS.addAndGet(-amt) < 0) {
            throw FaweCache.MAX_FAILS;
        }
    }

    public void THROW_MAX_CHECKS(int amt) {
        if (amt == 0) {
            return;
        }
        if (MAX_CHECKS.addAndGet(-amt) < 0) {
            throw FaweCache.MAX_CHECKS;
        }
    }

    public void THROW_MAX_CHECKS(long amt) {
        if (amt == 0) {
            return;
        }
        if (MAX_CHECKS.addAndGet(-amt) < 0) {
            throw FaweCache.MAX_CHECKS;
        }
    }

    public void THROW_MAX_ITERATIONS(int amt) {
        if (amt == 0) {
            return;
        }
        if (MAX_ITERATIONS.addAndGet(-amt) < 0) {
            throw FaweCache.MAX_ITERATIONS;
        }
    }

    public void THROW_MAX_BLOCKSTATES(int amt) {
        if (amt == 0) {
            return;
        }
        if (MAX_BLOCKSTATES.addAndGet(-amt) < 0) {
            throw FaweCache.MAX_TILES;
        }
    }

    public void THROW_MAX_ENTITIES(int amt) {
        if (amt == 0) {
            return;
        }
        if (MAX_ENTITIES.addAndGet(-amt) < 0) {
            throw FaweCache.MAX_ENTITIES;
        }
    }

    public boolean isUnlimited() {
        return MAX_CHANGES.get() == Long.MAX_VALUE
                && MAX_FAILS.get() == Integer.MAX_VALUE
                && MAX_CHECKS.get() == Long.MAX_VALUE
                && MAX_ITERATIONS.get() == Integer.MAX_VALUE
                && MAX_BLOCKSTATES.get() == Integer.MAX_VALUE
                && MAX_ENTITIES.get() == Integer.MAX_VALUE
                && MAX_HISTORY == Integer.MAX_VALUE
                && SCHEM_FILE_SIZE_LIMIT == Integer.MAX_VALUE
                && SCHEM_FILE_NUM_LIMIT == Integer.MAX_VALUE
                && INVENTORY_MODE == 0
                && SPEED_REDUCTION == 0
                && FAST_PLACEMENT
                && !RESTRICT_HISTORY_TO_REGIONS
                && (STRIP_NBT == null || STRIP_NBT.isEmpty())
                // && !UNIVERSAL_DISALLOWED_BLOCKS --> do not include this, it effectively has no relevance
                && ALLOW_LEGACY
                && SKIP_ENTITY_SPAWN_EVENTS
                && (DISALLOWED_BLOCKS == null || DISALLOWED_BLOCKS.isEmpty())
                && (REMAP_PROPERTIES == null || REMAP_PROPERTIES.isEmpty())
                && MAX_RADIUS == Integer.MAX_VALUE
                && MAX_SUPER_PICKAXE_SIZE == Integer.MAX_VALUE
                && MAX_BRUSH_RADIUS == Integer.MAX_VALUE
                && MAX_BUTCHER_RADIUS == Integer.MAX_VALUE;
    }

    /**
     * Get an {@link FaweLimit} representing the amount of a limit used from a given "original" limit
     *
     * @since 2.12.0
     */
    public FaweLimit getLimitUsed(FaweLimit originalLimit) {
        FaweLimit newLimit = new FaweLimit();
        newLimit.MAX_CHANGES = new AtomicLong(originalLimit.MAX_CHANGES.get() - this.MAX_CHANGES.get());
        newLimit.MAX_FAILS = new AtomicInteger(originalLimit.MAX_FAILS.get() - this.MAX_FAILS.get());
        newLimit.MAX_CHECKS = new AtomicLong(originalLimit.MAX_CHECKS.get() - this.MAX_CHECKS.get());
        newLimit.MAX_ITERATIONS = new AtomicInteger(originalLimit.MAX_ITERATIONS.get() - this.MAX_ITERATIONS.get());
        newLimit.MAX_BLOCKSTATES = new AtomicInteger(originalLimit.MAX_BLOCKSTATES.get() - this.MAX_BLOCKSTATES.get());
        newLimit.MAX_ENTITIES = new AtomicInteger(originalLimit.MAX_ENTITIES.get() - this.MAX_ENTITIES.get());
        return newLimit;
    }

    public void set(FaweLimit limit) {
        MAX_ACTIONS = limit.MAX_ACTIONS;
        MAX_CHANGES.set(limit.MAX_CHANGES.get());
        MAX_FAILS.set(limit.MAX_FAILS.get());
        MAX_CHECKS.set(limit.MAX_CHECKS.get());
        MAX_ITERATIONS.set(limit.MAX_ITERATIONS.get());
        MAX_BLOCKSTATES.set(limit.MAX_BLOCKSTATES.get());
        MAX_ENTITIES.set(limit.MAX_ENTITIES.get());
        MAX_HISTORY = limit.MAX_HISTORY;
        SCHEM_FILE_NUM_LIMIT = limit.SCHEM_FILE_NUM_LIMIT;
        SCHEM_FILE_SIZE_LIMIT = limit.SCHEM_FILE_SIZE_LIMIT;
        INVENTORY_MODE = limit.INVENTORY_MODE;
        SPEED_REDUCTION = limit.SPEED_REDUCTION;
        FAST_PLACEMENT = limit.FAST_PLACEMENT;
        CONFIRM_LARGE = limit.CONFIRM_LARGE;
        RESTRICT_HISTORY_TO_REGIONS = limit.RESTRICT_HISTORY_TO_REGIONS;
        STRIP_NBT = limit.STRIP_NBT;
        UNIVERSAL_DISALLOWED_BLOCKS = limit.UNIVERSAL_DISALLOWED_BLOCKS;
        ALLOW_LEGACY = limit.ALLOW_LEGACY;
        SKIP_ENTITY_SPAWN_EVENTS  = limit.SKIP_ENTITY_SPAWN_EVENTS;
        DISALLOWED_BLOCKS = limit.DISALLOWED_BLOCKS;
        REMAP_PROPERTIES = limit.REMAP_PROPERTIES;
        MAX_RADIUS = limit.MAX_RADIUS;
        MAX_SUPER_PICKAXE_SIZE = limit.MAX_SUPER_PICKAXE_SIZE;
        MAX_BRUSH_RADIUS = limit.MAX_BRUSH_RADIUS;
        MAX_BUTCHER_RADIUS = limit.MAX_BUTCHER_RADIUS;
    }

    public FaweLimit copy() {
        FaweLimit limit = new FaweLimit();
        limit.INVENTORY_MODE = INVENTORY_MODE;
        limit.SPEED_REDUCTION = SPEED_REDUCTION;
        limit.MAX_ACTIONS = MAX_ACTIONS;
        limit.MAX_CHANGES = MAX_CHANGES;
        limit.MAX_BLOCKSTATES = MAX_BLOCKSTATES;
        limit.MAX_CHECKS = MAX_CHECKS;
        limit.MAX_ENTITIES = MAX_ENTITIES;
        limit.MAX_FAILS = MAX_FAILS;
        limit.MAX_ITERATIONS = MAX_ITERATIONS;
        limit.MAX_HISTORY = MAX_HISTORY;
        limit.SCHEM_FILE_SIZE_LIMIT = SCHEM_FILE_SIZE_LIMIT;
        limit.SCHEM_FILE_NUM_LIMIT = SCHEM_FILE_NUM_LIMIT;
        limit.FAST_PLACEMENT = FAST_PLACEMENT;
        limit.CONFIRM_LARGE = CONFIRM_LARGE;
        limit.RESTRICT_HISTORY_TO_REGIONS = RESTRICT_HISTORY_TO_REGIONS;
        limit.STRIP_NBT = STRIP_NBT;
        limit.UNIVERSAL_DISALLOWED_BLOCKS = UNIVERSAL_DISALLOWED_BLOCKS;
        limit.ALLOW_LEGACY = ALLOW_LEGACY;
        limit.SKIP_ENTITY_SPAWN_EVENTS = SKIP_ENTITY_SPAWN_EVENTS;
        limit.DISALLOWED_BLOCKS = DISALLOWED_BLOCKS;
        limit.REMAP_PROPERTIES = REMAP_PROPERTIES;
        limit.MAX_RADIUS = MAX_RADIUS;
        limit.MAX_SUPER_PICKAXE_SIZE = MAX_SUPER_PICKAXE_SIZE;
        limit.MAX_BRUSH_RADIUS = MAX_BRUSH_RADIUS;
        limit.MAX_BUTCHER_RADIUS = MAX_BUTCHER_RADIUS;
        return limit;
    }

    @Override
    public String toString() {
        return MAX_CHANGES + "";
    }

}
