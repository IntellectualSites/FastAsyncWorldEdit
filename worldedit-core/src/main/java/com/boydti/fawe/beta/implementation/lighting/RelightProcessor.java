package com.boydti.fawe.beta.implementation.lighting;

import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.implementation.processors.ProcessorScope;
import com.boydti.fawe.config.Settings;
import com.sk89q.worldedit.extent.Extent;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class RelightProcessor implements IBatchProcessor {

    private final Relighter relighter;

    public RelightProcessor(Relighter relighter) {
        if (relighter instanceof NullRelighter) {
            throw new IllegalArgumentException("NullRelighter cannot be used for a RelightProcessor");
        }
        this.relighter = relighter;
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        if (Settings.IMP.LIGHTING.MODE == 2) {
            relighter.addChunk(chunk.getX(), chunk.getZ(), null, chunk.getBitMask());
        } else if (Settings.IMP.LIGHTING.MODE == 1) {
            byte[] fix = new byte[16];
            boolean relight = false;
            for (int i = 15; i >= 0; i--) {
                if (!set.hasSection(i)) {
                    fix[i] = Relighter.SkipReason.AIR;
                    continue;
                }
                relight = true;
                break;
            }
            if (relight) {
                relighter.addChunk(chunk.getX(), chunk.getZ(), fix, chunk.getBitMask());
            }
        }
        return set;
    }

    @Override
    public Future<IChunkSet> postProcessSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        return CompletableFuture.completedFuture(set);
    }

    @Override
    public @Nullable Extent construct(Extent child) {
        throw new UnsupportedOperationException("Processing only");
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.CUSTOM;
    }
}
