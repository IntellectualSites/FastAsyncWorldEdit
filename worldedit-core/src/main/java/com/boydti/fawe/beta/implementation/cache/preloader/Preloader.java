package com.boydti.fawe.beta.implementation.cache.preloader;

import com.sk89q.worldedit.entity.Player;

public interface Preloader {
    void cancel(Player player);

    void update(Player player);
}
