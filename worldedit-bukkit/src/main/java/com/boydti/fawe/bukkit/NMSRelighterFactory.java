package com.boydti.fawe.bukkit;

import com.boydti.fawe.beta.IQueueChunk;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.implementation.lighting.NMSRelighter;
import com.boydti.fawe.beta.implementation.lighting.Relighter;
import com.boydti.fawe.beta.implementation.lighting.RelighterFactory;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.RelightMode;
import com.sk89q.worldedit.world.World;
import org.jetbrains.annotations.NotNull;

public class NMSRelighterFactory implements RelighterFactory {
    @Override
    public @NotNull Relighter createRelighter(RelightMode relightMode, World world, IQueueExtent<IQueueChunk> queue) {
        return new NMSRelighter(queue, Settings.IMP.LIGHTING.DO_HEIGHTMAPS,
                relightMode != null ? relightMode : RelightMode.valueOf(Settings.IMP.LIGHTING.MODE));
    }
}
