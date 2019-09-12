package com.boydti.fawe.bukkit.wrapper;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TaskManager;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class AsyncChunk implements Chunk {

    private final AsyncWorld world;
    private final int z;
    private final int x;
    private final FaweQueue queue;

    public AsyncChunk(World world, FaweQueue queue, int x, int z) {
        this.world = world instanceof AsyncWorld ? (AsyncWorld) world : new AsyncWorld(world, true);
        this.queue = queue;
        this.x = x;
        this.z = z;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Chunk)) {
            return false;
        }
        Chunk other = (Chunk) obj;
        return other.getX() == x && other.getZ() == z && world.equals(other.getWorld());
    }

    @Override
    public int hashCode() {
        return MathMan.pair((short) x, (short) z);
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getZ() {
        return z;
    }

    @Override
    public AsyncWorld getWorld() {
        return world;
    }

    @Override
    public AsyncBlock getBlock(int x, int y, int z) {
        return new AsyncBlock(world, queue, (this.x << 4) + x, y, (this.z << 4) + z);
    }

    @Override
    public ChunkSnapshot getChunkSnapshot() {
        return getChunkSnapshot(false, true, false);
    }

    @Override
    public ChunkSnapshot getChunkSnapshot(boolean includeMaxblocky, boolean includeBiome, boolean includeBiomeTempRain) {
        if (Fawe.isMainThread()) {
            return world.getChunkAt(x, z).getChunkSnapshot(includeMaxblocky, includeBiome, includeBiomeTempRain);
        }
        return whenLoaded(new RunnableVal<ChunkSnapshot>() {
            @Override
            public void run(ChunkSnapshot value) {
                this.value = world.getChunkAt(x, z).getChunkSnapshot(includeBiome, includeBiome, includeBiomeTempRain);
            }
        });
    }

    private <T> T whenLoaded(RunnableVal<T> task) {
        if (Fawe.isMainThread()) {
            task.run();
            return task.value;
        }
        if (queue instanceof BukkitQueue_0) {
            BukkitQueue_0 bq = (BukkitQueue_0) queue;
            if (world.isChunkLoaded(x, z)) {
                if (world.isChunkLoaded(x, z)) {
                    task.run();
                    return task.value;
                }
            }
        }
        return TaskManager.IMP.sync(task);
    }

    @Override
    public Entity[] getEntities() {
        if (!isLoaded()) {
            return new Entity[0];
        }
        return whenLoaded(new RunnableVal<Entity[]>() {
            @Override
            public void run(Entity[] value) {
                world.getChunkAt(x, z).getEntities();
            }
        });
    }

    @Override
    public BlockState[] getTileEntities() {
        if (!isLoaded()) {
            return new BlockState[0];
        }
        return TaskManager.IMP.sync(new RunnableVal<BlockState[]>() {
            @Override
            public void run(BlockState[] value) {
                this.value = world.getChunkAt(x, z).getTileEntities();
            }
        });
    }

    @Override
    public @NotNull BlockState[] getTileEntities(boolean useSnapshot) {
        if (!isLoaded()) {
            return new BlockState[0];
        }
        return TaskManager.IMP.sync(new RunnableVal<BlockState[]>() {
            @Override
            public void run(BlockState[] value) {
                this.value = world.getChunkAt(x, z).getTileEntities(useSnapshot);
            }
        });
    }

    @Override
    public boolean isLoaded() {
        return world.isChunkLoaded(x, z);
    }

    @Override
    public boolean load(final boolean generate) {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                this.value = world.loadChunk(x, z, generate);
            }
        });
    }

    @Override
    public boolean load() {
        return load(false);
    }

    public boolean unload(boolean save) {
        return world.unloadChunk(x, z, save);
    }

    @Override
    public boolean unload() {
        return unload(true);
    }

    @Override
    public boolean isSlimeChunk() {
        return world.getChunkAt(x, z).isSlimeChunk();
    }

	@Override
	public boolean isForceLoaded() {
		return world.isChunkForceLoaded(x, z);
	}

	@Override
	public void setForceLoaded(boolean arg0) {
		world.getChunkAt(x, z).setForceLoaded(arg0);
	}

    @Override
    public boolean addPluginChunkTicket(final Plugin plugin) {
        return world.addPluginChunkTicket(this.getX(), this.getZ(), plugin);
    }

    @Override
    public boolean removePluginChunkTicket(final Plugin plugin) {
        return world.removePluginChunkTicket(this.getX(), this.getZ(), plugin);
    }

    @Override
    public Collection<Plugin> getPluginChunkTickets() {
        return world.getPluginChunkTickets(this.getX(), this.getZ());
    }

    @Override
    public long getInhabitedTime() {
        return world.getChunkAt(x, z).getInhabitedTime();
    }

    @Override
    public void setInhabitedTime(long l) {
        world.getChunkAt(x, z).setInhabitedTime(l);
    }

    @Override
    public boolean contains(@NotNull BlockData blockData) {
        return world.getChunkAt(x, z).contains(blockData);
    }
}
