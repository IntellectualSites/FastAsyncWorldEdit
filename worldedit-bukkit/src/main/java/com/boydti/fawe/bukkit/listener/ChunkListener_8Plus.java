package com.boydti.fawe.bukkit.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;

public class ChunkListener_8Plus implements Listener{
    private final ChunkListener listener;

    public ChunkListener_8Plus(ChunkListener listener) {
        this.listener = listener;
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockExplodeEvent event) {
        listener.reset();
    }
}
