package com.fastasyncworldedit.core.queue.implementation.preloader;

import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.world.World;

import javax.annotation.Nonnull;

public interface Preloader {

    /**
     * Tell the preloader to stop attempting to preload chunks
     */
    void cancel();

    /**
     * Cancel any preloading related to the given Actor
     *
     * @param actor Actor to cancel preloading of
     */
    void cancel(@Nonnull Actor actor);

    /**
     * Update the preloading for the given player, in the given world. Uses the player's current selection.
     *
     * @param actor Actor to update
     * @param world World to use
     */
    void update(@Nonnull Actor actor, @Nonnull World world);

}
