package com.fastasyncworldedit.core.queue.implementation.preloader;

import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.world.World;
import javax.annotation.Nonnull;

public interface Preloader {

    void cancel();

    void cancel(@Nonnull Actor player);

    void update(@Nonnull Actor player, @Nonnull World world);

}
