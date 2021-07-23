package com.fastasyncworldedit.core.queue.implementation.lighting;

import java.util.HashMap;
import java.util.Map;

public enum RelightMode {
    NONE(0), // no relighting
    OPTIMAL(1), // relight changed light sources and changed blocks
    ALL(2); // relight every single block

    private static final Map<Integer, RelightMode> map = new HashMap<>();

    static {
        for (RelightMode mode : RelightMode.values()) {
            map.put(mode.mode, mode);
        }
    }

    private final int mode;

    RelightMode(int mode) {
        this.mode = mode;
    }

    public static RelightMode valueOf(int mode) {
        return map.get(mode);
    }

    public int getMode() {
        return mode;
    }
}
