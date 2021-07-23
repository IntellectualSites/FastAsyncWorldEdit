package com.fastasyncworldedit.bukkit.adapter;

import com.fastasyncworldedit.core.queue.IQueueChunk;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.fastasyncworldedit.core.queue.implementation.lighting.NMSRelighter;
import com.fastasyncworldedit.core.queue.implementation.lighting.Relighter;
import com.fastasyncworldedit.core.queue.implementation.lighting.RelighterFactory;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.queue.implementation.lighting.RelightMode;
import com.sk89q.worldedit.world.World;
import org.jetbrains.annotations.NotNull;

public class NMSRelighterFactory implements RelighterFactory {
    @Override
    public @NotNull Relighter createRelighter(RelightMode relightMode, World world, IQueueExtent<IQueueChunk> queue) {
        return new NMSRelighter(queue,
                relightMode != null ? relightMode : RelightMode.valueOf(Settings.IMP.LIGHTING.MODE));
    }
}
