package com.fastasyncworldedit.core.extent.processor.lighting;

import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.sk89q.worldedit.world.World;

import javax.annotation.Nonnull;

/**
 * This abstracts the creation of {@link Relighter}s to allow more modular code.
 */
@FunctionalInterface
public interface RelighterFactory {

    /**
     * Create a new {@link Relighter} that can be used by a {@link RelightProcessor}.
     * <p>
     * Implementations are meant to configure an appropriate Relighter using the specified
     * parameters. There are no guarantees about the returned objects other than being non-null.
     * If no valid Relighter can be created, {@link NullRelighter#INSTANCE} should be returned.
     *
     * @param relightMode the relight mode to use during relighting.
     * @param world       the world in which relighting should be done.
     * @param queue       the queue extent to work with.
     * @return a new Relighter instance with the specified settings.
     */
    @Nonnull
    Relighter createRelighter(RelightMode relightMode, World world, IQueueExtent<?> queue);

}
