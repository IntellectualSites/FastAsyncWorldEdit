package com.boydti.fawe.object;

import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

import java.util.Collections;
import java.util.Set;

/**
 * Created by Jesse on 4/5/2016.
 */
public class FaweLimit {
    public int MAX_ACTIONS = 0;
    public int MAX_CHANGES = 0;
    public int MAX_FAILS = 0;
    public int MAX_CHECKS = 0;
    public int MAX_ITERATIONS = 0;
    public int MAX_BLOCKSTATES = 0;
    public int MAX_ENTITIES = 0;
    public int MAX_HISTORY = 0;
    public int MAX_EXPRESSION_MS = 0;
    public int INVENTORY_MODE = Integer.MAX_VALUE;
    public int SPEED_REDUCTION = Integer.MAX_VALUE;
    public boolean FAST_PLACEMENT = false;
    public boolean CONFIRM_LARGE = true;
    public Set<String> STRIP_NBT = null;

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
        };
        MAX.SPEED_REDUCTION = 0;
        MAX.INVENTORY_MODE = 0;
        MAX.MAX_ACTIONS = 1;
        MAX.MAX_CHANGES = Integer.MAX_VALUE;
        MAX.MAX_FAILS = Integer.MAX_VALUE;
        MAX.MAX_CHECKS = Integer.MAX_VALUE;
        MAX.MAX_ITERATIONS = Integer.MAX_VALUE;
        MAX.MAX_BLOCKSTATES = Integer.MAX_VALUE;
        MAX.MAX_ENTITIES = Integer.MAX_VALUE;
        MAX.MAX_HISTORY = Integer.MAX_VALUE;
        MAX.MAX_EXPRESSION_MS = 50;
        MAX.FAST_PLACEMENT = true;
        MAX.CONFIRM_LARGE = true;
        MAX.STRIP_NBT = null;
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

    public boolean isUnlimited() {
        return MAX_CHANGES == Integer.MAX_VALUE &&
                MAX_FAILS == Integer.MAX_VALUE &&
                MAX_CHECKS == Integer.MAX_VALUE &&
                MAX_ITERATIONS == Integer.MAX_VALUE &&
                MAX_BLOCKSTATES == Integer.MAX_VALUE &&
                MAX_ENTITIES == Integer.MAX_VALUE &&
                MAX_HISTORY == Integer.MAX_VALUE &&
                INVENTORY_MODE == 0 &&
                SPEED_REDUCTION == 0 &&
                FAST_PLACEMENT == true &&
                (STRIP_NBT == null || STRIP_NBT.isEmpty());
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
        STRIP_NBT = limit.STRIP_NBT;
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
        limit.STRIP_NBT = STRIP_NBT;
        return limit;
    }

    @Override
    public String toString() {
        return MAX_CHANGES + "";
    }
}
