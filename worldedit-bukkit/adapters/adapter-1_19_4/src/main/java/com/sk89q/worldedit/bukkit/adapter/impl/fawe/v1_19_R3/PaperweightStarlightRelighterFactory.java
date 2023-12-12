package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_19_R3;

import com.fastasyncworldedit.core.extent.processor.lighting.NullRelighter;
import com.fastasyncworldedit.core.extent.processor.lighting.RelightMode;
import com.fastasyncworldedit.core.extent.processor.lighting.Relighter;
import com.fastasyncworldedit.core.extent.processor.lighting.RelighterFactory;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;

import javax.annotation.Nonnull;

public class PaperweightStarlightRelighterFactory implements RelighterFactory {

    @Override
    public @Nonnull Relighter createRelighter(RelightMode relightMode, World world, IQueueExtent<?> queue) {
        org.bukkit.World w = Bukkit.getWorld(world.getName());
        if (w == null) {
            return NullRelighter.INSTANCE;
        }
        return new PaperweightStarlightRelighter(((CraftWorld) w).getHandle(), queue);
    }

}
