package com.fastasyncworldedit.bukkit.adapter;

import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.processor.lighting.NMSRelighter;
import com.fastasyncworldedit.core.extent.processor.lighting.RelightMode;
import com.fastasyncworldedit.core.extent.processor.lighting.Relighter;
import com.fastasyncworldedit.core.extent.processor.lighting.RelighterFactory;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.sk89q.worldedit.world.World;

import javax.annotation.Nonnull;

public class NMSRelighterFactory implements RelighterFactory {

    @Override
    public @Nonnull Relighter createRelighter(RelightMode relightMode, World world, IQueueExtent<?> queue) {
        return new NMSRelighter(
                queue,
                relightMode != null ? relightMode : RelightMode.valueOf(Settings.settings().LIGHTING.MODE)
        );
    }

}
