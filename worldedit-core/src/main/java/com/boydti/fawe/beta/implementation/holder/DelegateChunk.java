package com.boydti.fawe.beta.implementation.holder;

import com.boydti.fawe.beta.ChunkFilterBlock;
import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.FilterBlock;
import com.boydti.fawe.beta.FilterBlockMask;
import com.boydti.fawe.beta.Flood;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IDelegateChunk;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.regions.Region;

import javax.annotation.Nullable;

/**
 * Implementation of IDelegateChunk
 * @param <T>
 */
public class DelegateChunk<T extends IChunk> implements IDelegateChunk {
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