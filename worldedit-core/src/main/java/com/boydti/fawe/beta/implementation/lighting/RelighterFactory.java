package com.boydti.fawe.beta.implementation.lighting;

import com.boydti.fawe.beta.IQueueChunk;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.object.RelightMode;
import com.sk89q.worldedit.world.World;

@FunctionalInterface
public interface RelighterFactory {

    Relighter createRelighter(RelightMode relightMode, World world, IQueueExtent<IQueueChunk> queue);
}
