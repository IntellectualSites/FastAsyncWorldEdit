package com.fastasyncworldedit.bukkit.adapter;

import com.fastasyncworldedit.core.beta.IQueueChunk;
import com.fastasyncworldedit.core.beta.IQueueExtent;
import com.fastasyncworldedit.core.beta.implementation.lighting.NMSRelighter;
import com.fastasyncworldedit.core.beta.implementation.lighting.Relighter;
import com.fastasyncworldedit.core.beta.implementation.lighting.RelighterFactory;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.object.RelightMode;
import com.sk89q.worldedit.world.World;
import org.jetbrains.annotations.NotNull;

public class NMSRelighterFactory implements RelighterFactory {
    @Override
    public @NotNull Relighter createRelighter(RelightMode relightMode, World world, IQueueExtent<IQueueChunk> queue) {
        return new NMSRelighter(queue,
                relightMode != null ? relightMode : RelightMode.valueOf(Settings.IMP.LIGHTING.MODE));
    }
}
