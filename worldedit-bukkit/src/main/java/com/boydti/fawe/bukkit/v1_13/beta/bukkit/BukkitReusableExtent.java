package com.boydti.fawe.bukkit.v1_13.beta.bukkit;

import com.boydti.fawe.bukkit.v1_13.beta.SingleThreadQueueExtent;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.world.World;
import net.minecraft.server.v1_13_R2.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;

import static com.google.common.base.Preconditions.checkNotNull;

public class BukkitReusableExtent extends SingleThreadQueueExtent {
    private org.bukkit.World bukkitWorld;
    private WorldServer nmsWorld;

    public void init(World world) {
        super.init(world);
        world = getWorld();

        if (world instanceof BukkitWorld) {
            this.bukkitWorld = ((BukkitWorld) world).getWorld();
        } else {
            this.bukkitWorld = Bukkit.getWorld(world.getName());
        }
        checkNotNull(this.bukkitWorld);
        CraftWorld craftWorld = ((CraftWorld) bukkitWorld);
        this.nmsWorld = craftWorld.getHandle();
    }

}
