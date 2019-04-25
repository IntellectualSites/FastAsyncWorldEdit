package com.boydti.fawe.bukkit.v1_13.beta;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.jnbt.anvil.MCAFilter;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.wrappers.WorldWrapper;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import net.minecraft.server.v1_13_R2.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import static com.google.common.base.Preconditions.checkNotNull;

public class ReusableExtent {
    private WorldWrapper wrapped;
    private World world;
    private org.bukkit.World bukkitWorld;
    private WorldServer nmsWorld;

    private void reset() {
        if (world != null) {
            wrapped = null;
            world = null;
            bukkitWorld = null;
            nmsWorld = null;
            lowMemory = false;
        }
    }

    public void init(World world) {
        reset();
        checkNotNull(world);
        if (world instanceof EditSession) {
            world = ((EditSession) world).getWorld();
        }
        checkNotNull(world);
        if (world instanceof WorldWrapper) {
            this.wrapped = (WorldWrapper) world;
            world = WorldWrapper.unwrap(world);
        } else {
            this.world = WorldWrapper.wrap(world);
        }
        this.world = world;
        if (world instanceof BukkitWorld) {
            this.bukkitWorld = ((BukkitWorld) world).getWorld();
        } else {
            this.bukkitWorld = Bukkit.getWorld(world.getName());
        }
        checkNotNull(this.bukkitWorld);
        CraftWorld craftWorld = ((CraftWorld) bukkitWorld);
        this.nmsWorld = craftWorld.getHandle();
        // Save world
    }

    private boolean lowMemory;

    public void setLowMemory() {
        lowMemory = true;
        // set queue state to active
        // trim cached chunks
    }

    private CachedChunk getCachedChunk(int x, int z) {
        // check last
        // otherwise create/load
        // get cached chunk from bukkit
        // otherwise load
        // TODO load async (with paper)
        if (lowMemory) {
            if (Fawe.isMainThread()) {
                // submit other chunks
                next();
            } else {
                // wait until empty
            }
        }
    }

    void setBlock(int x, int y, int z, BlockStateHolder holder) {
        CachedChunk chunk = getCachedChunk(x, z);
        chunk.setBlock(x & 15, y, z & 15, holder);
    }

    void setBiome(int x, int z, BiomeType biome) {
        CachedChunk chunk = getCachedChunk(x, z);
        chunk.setBiome(x, z, biome);
    }

    BlockState getBlock(int x, int y, int z) {
        CachedChunk chunk = getCachedChunk(x, z);
        return chunk.getBlock(x & 15, y, z & 15);
    }

    BiomeType getBiome(int x, int z) {
        CachedChunk chunk = getCachedChunk(x, z);
        return chunk.getBiome(x, z);
    }

    public <T> void apply(Region region, MCAFilter<T> filter) { // TODO not MCAFilter, but another similar class
        // TODO iterate by mca file
        Set<BlockVector2> chunks = region.getChunks();
        Iterator<BlockVector2> chunksIter = chunks.iterator();
        ForkJoinPool pool = TaskManager.IMP.getPublicForkJoinPool();
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
            pool.submit(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        BlockVector2 pos;
                        synchronized (chunksIter) {
                            if (!chunksIter.hasNext()) return;
                            pos = chunksIter.next();
                        }
                        int cx = pos.getX();
                        int cz = pos.getZ();
                        CachedChunk chunk = getCachedChunk(cx, cz);
                        try {
                            if (!filter.appliesChunk(cx, cz)) {
                                continue;
                            }
                            T value = filter.get();
                            chunk = filter.applyChunk(chunk, value);

                            if (chunk == null) continue;

                            // TODO if region contains all parts
                            chunk.filter(filter);
                            // else
                            chunk.filter(region, filter);
                        } finally {
                            // TODO submit chunk
                        }
                    }
                }
            });
        }
    }
}