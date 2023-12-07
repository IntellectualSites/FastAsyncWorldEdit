package com.fastasyncworldedit.bukkit.adapter.regeneration.queue;

import com.fastasyncworldedit.bukkit.adapter.regeneration.Regenerator;
import com.sk89q.worldedit.math.BlockVector2;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * A section of the to-regenerated selection.
 */
public class QueuedRegenerationSection {

    private final Regenerator<?, ?, ?, ?, ?> regenerator;
    private final BlockVector2[] chunks;

    public QueuedRegenerationSection(final Regenerator<?, ?, ?, ?, ?> regenerator, final BlockVector2[] chunks) {
        this.regenerator = regenerator;
        this.chunks = chunks;
    }

    /**
     * Start the regeneration process for this part of the selection.
     * @return A future completing when all {@link #chunks} are regenerated including their logically combined results
     * (successful / failed)
     */
    public CompletableFuture<Boolean> regenerate() {
        final Set<CompletableFuture<Boolean>> futures = Arrays.stream(chunks)
                .map(this.regenerator::regenerateChunk)
                .collect(Collectors.toUnmodifiableSet());
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(unused -> futures.stream().allMatch(CompletableFuture::join));
    }

}
