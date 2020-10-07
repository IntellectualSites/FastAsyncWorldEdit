package com.boydti.fawe.bukkit.wrapper;

import com.boydti.fawe.Fawe;
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
import java.util.function.Supplier;

public class AsyncChunk implements Chunk {

    private final AsyncWorld world;
    private final int z;
    private final int x;

    public AsyncChunk(World world, int x, int z) {
        this.world = world instanceof AsyncWorld ? (AsyncWorld) world : new AsyncWorld(world, true);
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
        return new AsyncBlock(world, (this.x << 4) + x, y, (this.z << 4) + z);
    }

    @Override
    public ChunkSnapshot getChunkSnapshot() {
        return getChunkSnapshot(false, true, false);
    }

    @Override
    public ChunkSnapshot getChunkSnapshot(boolean includeMaxblocky, boolean includeBiome,
        boolean includeBiomeTempRain) {
        if (Fawe.isMainThread()) {
            return world.getChunkAt(x, z)
                .getChunkSnapshot(includeMaxblocky, includeBiome, includeBiomeTempRain);
        }
        return whenLoaded(() -> world.getChunkAt(x, z)
            .getChunkSnapshot(includeBiome, includeBiome, includeBiomeTempRain));
    }

    private <T> T whenLoaded(Supplier<T> task) {
        if (Fawe.isMainThread()) {
            return task.get();
        }
        if (world.isWorld()) {
            if (world.isChunkLoaded(x, z)) {
                return task.get();
            }
        }
        return TaskManager.IMP.sync(task);
    }

    @Override
    public Entity[] getEntities() {
        if (!isLoaded()) {
            return new Entity[0];
        }
        return whenLoaded(() -> world.getChunkAt(x, z).getEntities());
    }

    @Override
    public BlockState[] getTileEntities() {
        if (!isLoaded()) {
            return new BlockState[0];
        }
        return TaskManager.IMP.sync(() -> world.getChunkAt(x, z).getTileEntities());
    }

    @Override
    @NotNull
    public BlockState[] getTileEntities(boolean useSnapshot) {
        if (!isLoaded()) {
            return new BlockState[0];
        }
        return TaskManager.IMP.sync(() -> world.getChunkAt(x, z).getTileEntities(useSnapshot));
    }

    @Override
    public boolean isLoaded() {
        return world.isChunkLoaded(x, z);
    }

    @Override
    public boolean load(final boolean generate) {
        return TaskManager.IMP.sync(() -> world.loadChunk(x, z, generate));
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
        return false;
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
        return 0; //todo
    }

    @Override
    public void setInhabitedTime(long ticks) {
        //todo
    }

    @Override
    public boolean contains(@NotNull BlockData block) {
        //todo
        return false;
    }
}
