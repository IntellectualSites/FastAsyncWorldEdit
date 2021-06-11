package com.fastasyncworldedit.bukkit.adapter.mc1_16_5;

import com.fastasyncworldedit.beta.IQueueChunk;
import com.fastasyncworldedit.beta.IQueueExtent;
import com.fastasyncworldedit.beta.implementation.lighting.NullRelighter;
import com.fastasyncworldedit.beta.implementation.lighting.Relighter;
import com.fastasyncworldedit.beta.implementation.lighting.RelighterFactory;
import com.fastasyncworldedit.object.RelightMode;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.jetbrains.annotations.NotNull;

public class TuinityRelighterFactory_1_16_5 implements RelighterFactory {
    @Override
    public @NotNull Relighter createRelighter(RelightMode relightMode, World world, IQueueExtent<IQueueChunk> queue) {
        org.bukkit.World w = Bukkit.getWorld(world.getName());
        if (w == null) return NullRelighter.INSTANCE;
        return new TuinityRelighter_1_16_5(((CraftWorld) w).getHandle(), queue);
    }
}
