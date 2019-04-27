package com.boydti.fawe.bukkit.v1_13.beta.holder;

import com.boydti.fawe.bukkit.v1_13.beta.IChunk;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

public class WeakChunk extends ReferenceChunk {
    public WeakChunk(IChunk parent) {
        super(parent);
    }

    @Override
    protected Reference<FinalizedChunk> toRef(FinalizedChunk parent) {
        return new WeakReference<>(parent);
    }
}
