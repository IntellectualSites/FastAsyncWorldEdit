package com.boydti.fawe.bukkit.beta;

import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.implementation.SimpleCharQueueExtent;
import com.boydti.fawe.beta.implementation.SingleThreadQueueExtent;
import com.boydti.fawe.beta.implementation.WorldChunkCache;
import com.boydti.fawe.object.collection.IterableThreadLocal;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.world.World;
import net.minecraft.server.v1_13_R2.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;

import static com.google.common.base.Preconditions.checkNotNull;

public class BukkitQueue extends SimpleCharQueueExtent {

    private org.bukkit.World bukkitWorld;
    private WorldServer nmsWorld;

    @Override
    public synchronized void init(WorldChunkCache cache) {
        World world = cache.getWorld();
        if (world instanceof BukkitWorld) {
            this.bukkitWorld = ((BukkitWorld) world).getWorld();
        } else {
            this.bukkitWorld = Bukkit.getWorld(world.getName());
        }
        checkNotNull(this.bukkitWorld);
        CraftWorld craftWorld = ((CraftWorld) bukkitWorld);
        this.nmsWorld = craftWorld.getHandle();
        super.init(cache);
    }

    public WorldServer getNmsWorld() {
        return nmsWorld;
    }

    public org.bukkit.World getBukkitWorld() {
        return bukkitWorld;
    }

    @Override
    protected synchronized void reset() {
        super.reset();
    }

    private static final IterableThreadLocal<BukkitFullChunk> FULL_CHUNKS = new IterableThreadLocal<BukkitFullChunk>() {
        @Override
        public BukkitFullChunk init() {
            return new BukkitFullChunk();
        }
    };

    @Override
    public IChunk create(boolean full) {
        if (full) {
            // TODO implement
//            return FULL_CHUNKS.get();
        }
        return new BukkitChunkHolder();
    }
}
