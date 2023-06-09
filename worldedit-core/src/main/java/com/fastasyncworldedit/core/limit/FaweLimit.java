package com.fastasyncworldedit.core.limit;

import com.fastasyncworldedit.core.FaweCache;

import java.util.Collections;
import java.util.Set;

public class FaweLimit {

    public int MAX_ACTIONS = 0;
    public long MAX_CHANGES = 0;
    public int MAX_FAILS = 0;
    public long MAX_CHECKS = 0;
    public int MAX_ITERATIONS = 0;
    public int MAX_BLOCKSTATES = 0;
    public int MAX_ENTITIES = 0;
    public int MAX_HISTORY = 0;
    public int MAX_EXPRESSION_MS = 0;
    public int INVENTORY_MODE = Integer.MAX_VALUE;
    public int SPEED_REDUCTION = Integer.MAX_VALUE;
    public boolean FAST_PLACEMENT = false;
    public boolean CONFIRM_LARGE = true;
    public boolean RESTRICT_HISTORY_TO_REGIONS = true;
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
        MAX.MAX_CHANGES = Long.MAX_VALUE;
        MAX.MAX_FAILS = Integer.MAX_VALUE;
        MAX.MAX_CHECKS = Long.MAX_VALUE;
        MAX.MAX_ITERATIONS = Integer.MAX_VALUE;
        MAX.MAX_BLOCKSTATES = Integer.MAX_VALUE;
        MAX.MAX_ENTITIES = Integer.MAX_VALUE;
        MAX.MAX_HISTORY = Integer.MAX_VALUE;
        MAX.MAX_EXPRESSION_MS = 50;
        MAX.FAST_PLACEMENT = true;
        MAX.CONFIRM_LARGE = true;
        MAX.RESTRICT_HISTORY_TO_REGIONS = false;
        MAX.STRIP_NBT = Collections.emptySet();
        MAX.UNIVERSAL_DISALLOWED_BLOCKS = false;
        MAX.DISALLOWED_BLOCKS = Collections.emptySet();
        MAX.REMAP_PROPERTIES = Collections.emptySet();
    }

    public boolean MAX_CHANGES() {
        return MAX_CHANGES-- > 0;
    }

    public boolean MAX_FAILS() {
        return MAX_FAILS-- > 0;
    }

    public boolean MAX_CHECKS() {
        return MAX_CHECKS-- > 0;
    }

    public boolean MAX_ITERATIONS() {
        return MAX_ITERATIONS-- > 0;
    }

    public boolean MAX_BLOCKSTATES() {
        return MAX_BLOCKSTATES-- > 0;
    }

    public boolean MAX_ENTITIES() {
        return MAX_ENTITIES-- > 0;
    }

    public void THROW_MAX_CHANGES() {
        if (MAX_CHANGES-- <= 0) {
            throw FaweCache.MAX_CHANGES;
        }
    }

    public void THROW_MAX_FAILS() {
        if (MAX_FAILS-- <= 0) {
            throw FaweCache.MAX_CHECKS;
        }
    }

    public void THROW_MAX_CHECKS() {
        if (MAX_CHECKS-- <= 0) {
            throw FaweCache.MAX_CHECKS;
        }
    }

    public void THROW_MAX_ITERATIONS() {
        if (MAX_ITERATIONS-- <= 0) {
            throw FaweCache.MAX_ITERATIONS;
        }
    }

    public void THROW_MAX_BLOCKSTATES() {
        if (MAX_BLOCKSTATES-- <= 0) {
            throw FaweCache.MAX_TILES;
        }
    }

    public void THROW_MAX_ENTITIES() {
        if (MAX_ENTITIES-- <= 0) {
            throw FaweCache.MAX_ENTITIES;
        }
    }

    public void THROW_MAX_CHANGES(int amt) {
        if ((MAX_CHANGES -= amt) <= 0) {
            throw FaweCache.MAX_CHANGES;
        }
    }

    public void THROW_MAX_CHANGES(long amt) {
        if ((MAX_CHANGES -= amt) <= 0) {
            throw FaweCache.MAX_CHANGES;
        }
    }

    public void THROW_MAX_FAILS(int amt) {
        if ((MAX_FAILS -= amt) <= 0) {
            throw FaweCache.MAX_CHECKS;
        }
    }

    public void THROW_MAX_CHECKS(int amt) {
        if ((MAX_CHECKS -= amt) <= 0) {
            throw FaweCache.MAX_CHECKS;
        }
    }

    public void THROW_MAX_CHECKS(long amt) {
        if ((MAX_CHECKS -= amt) <= 0) {
            throw FaweCache.MAX_CHECKS;
        }
    }

    public void THROW_MAX_ITERATIONS(int amt) {
        if ((MAX_ITERATIONS -= amt) <= 0) {
            throw FaweCache.MAX_ITERATIONS;
        }
    }

    public void THROW_MAX_BLOCKSTATES(int amt) {
        if ((MAX_BLOCKSTATES -= amt) <= 0) {
            throw FaweCache.MAX_TILES;
        }
    }

    public void THROW_MAX_ENTITIES(int amt) {
        if ((MAX_ENTITIES -= amt) <= 0) {
            throw FaweCache.MAX_ENTITIES;
        }
    }

    public boolean isUnlimited() {
        return MAX_CHANGES == Long.MAX_VALUE
                && MAX_FAILS == Integer.MAX_VALUE
                && MAX_CHECKS == Long.MAX_VALUE
                && MAX_ITERATIONS == Integer.MAX_VALUE
                && MAX_BLOCKSTATES == Integer.MAX_VALUE
                && MAX_ENTITIES == Integer.MAX_VALUE
                && MAX_HISTORY == Integer.MAX_VALUE
                && INVENTORY_MODE == 0
                && SPEED_REDUCTION == 0
                && FAST_PLACEMENT
                && !RESTRICT_HISTORY_TO_REGIONS
                && (STRIP_NBT == null || STRIP_NBT.isEmpty())
                // && !UNIVERSAL_DISALLOWED_BLOCKS --> do not include this, it effectively has no relevance
                && (DISALLOWED_BLOCKS == null || DISALLOWED_BLOCKS.isEmpty())
                && (REMAP_PROPERTIES == null || REMAP_PROPERTIES.isEmpty());
    }

    public void set(FaweLimit limit) {
        MAX_ACTIONS = limit.MAX_ACTIONS;
        MAX_CHANGES = limit.MAX_CHANGES;
        MAX_BLOCKSTATES = limit.MAX_BLOCKSTATES;
        MAX_CHECKS = limit.MAX_CHECKS;
        MAX_ENTITIES = limit.MAX_ENTITIES;
        MAX_FAILS = limit.MAX_FAILS;
        MAX_ITERATIONS = limit.MAX_ITERATIONS;
        MAX_HISTORY = limit.MAX_HISTORY;
        INVENTORY_MODE = limit.INVENTORY_MODE;
        SPEED_REDUCTION = limit.SPEED_REDUCTION;
        FAST_PLACEMENT = limit.FAST_PLACEMENT;
        CONFIRM_LARGE = limit.CONFIRM_LARGE;
        RESTRICT_HISTORY_TO_REGIONS = limit.RESTRICT_HISTORY_TO_REGIONS;
        STRIP_NBT = limit.STRIP_NBT;
        UNIVERSAL_DISALLOWED_BLOCKS = limit.UNIVERSAL_DISALLOWED_BLOCKS;
        DISALLOWED_BLOCKS = limit.DISALLOWED_BLOCKS;
        REMAP_PROPERTIES = limit.REMAP_PROPERTIES;
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
        limit.FAST_PLACEMENT = FAST_PLACEMENT;
        limit.CONFIRM_LARGE = CONFIRM_LARGE;
        limit.RESTRICT_HISTORY_TO_REGIONS = RESTRICT_HISTORY_TO_REGIONS;
        limit.STRIP_NBT = STRIP_NBT;
        limit.UNIVERSAL_DISALLOWED_BLOCKS = UNIVERSAL_DISALLOWED_BLOCKS;
        limit.DISALLOWED_BLOCKS = DISALLOWED_BLOCKS;
        limit.REMAP_PROPERTIES = REMAP_PROPERTIES;
        return limit;
    }

    @Override
    public String toString() {
        return MAX_CHANGES + "";
    }

}
