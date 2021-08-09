package com.fastasyncworldedit.bukkit.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

/**
 * Class to prevent the above/below being removed from shaded/relocated dependencies via minimization
 */
final class DoNotMiniseThese {
    private final Long2ObjectLinkedOpenHashMap a = null;
    private final LongArraySet b = null;
    private final LongIterator c = null;
    private final LongSet d = null;
    private final Int2ObjectMap e = null;
}
