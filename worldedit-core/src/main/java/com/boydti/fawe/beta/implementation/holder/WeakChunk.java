package com.boydti.fawe.beta.implementation.holder;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IQueueExtent;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A {@link ReferenceChunk} using {@link WeakReference} to hold the chunk.
 */
public class WeakChunk extends ReferenceChunk {

    public WeakChunk(IChunk parent, IQueueExtent queueExtent) {
        super(parent, queueExtent);
    }

    @Override
    protected Reference<FinalizedChunk> toReference(FinalizedChunk parent) {
        return new WeakReference<>(parent);
    }
}
