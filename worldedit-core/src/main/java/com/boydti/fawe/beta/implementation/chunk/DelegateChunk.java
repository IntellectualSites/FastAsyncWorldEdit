package com.boydti.fawe.beta.implementation.chunk;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IDelegateChunk;
import com.boydti.fawe.beta.IQueueChunk;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BiomeType;

import java.util.Set;
import java.util.UUID;

/**
 * Implementation of IDelegateChunk
 * @param <T>
 */
public class DelegateChunk<T extends IQueueChunk> implements IDelegateChunk<T> {

    private T parent;

    public DelegateChunk(final T parent) {
        this.parent = parent;
    }

    public final T getParent() {
        return parent;
    }

    public final void setParent(final T parent) {
        this.parent = parent;
    }
}
