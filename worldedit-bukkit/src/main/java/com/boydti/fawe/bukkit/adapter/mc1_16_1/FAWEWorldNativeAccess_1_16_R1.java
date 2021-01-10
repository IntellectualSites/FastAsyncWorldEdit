package com.boydti.fawe.bukkit.adapter.mc1_16_1;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.IntPair;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.adapter.impl.FAWE_Spigot_v1_16_R1;
import com.sk89q.worldedit.internal.block.BlockStateIdAccess;
import com.sk89q.worldedit.internal.wna.WorldNativeAccess;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.block.BlockState;
import net.minecraft.server.v1_16_R1.Block;
import net.minecraft.server.v1_16_R1.BlockPosition;
import net.minecraft.server.v1_16_R1.Chunk;
import net.minecraft.server.v1_16_R1.ChunkProviderServer;
import net.minecraft.server.v1_16_R1.EnumDirection;
import net.minecraft.server.v1_16_R1.IBlockData;
import net.minecraft.server.v1_16_R1.MinecraftServer;
import net.minecraft.server.v1_16_R1.NBTBase;
import net.minecraft.server.v1_16_R1.NBTTagCompound;
import net.minecraft.server.v1_16_R1.PlayerChunk;
import net.minecraft.server.v1_16_R1.TileEntity;
import net.minecraft.server.v1_16_R1.World;
import org.bukkit.craftbukkit.v1_16_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R1.block.data.CraftBlockData;
import org.bukkit.event.block.BlockPhysicsEvent;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

public class FAWEWorldNativeAccess_1_16_R1 implements WorldNativeAccess<Chunk, IBlockData, BlockPosition> {
    private static final int UPDATE = 1;
    private static final int NOTIFY = 2;

    private final FAWE_Spigot_v1_16_R1 adapter;
    private final WeakReference<World> world;
    private SideEffectSet sideEffectSet;
    private final AtomicInteger lastTick;
    private final Set<CachedChange> cachedChanges = new HashSet<>();
    private final Set<IntPair> cachedChunksToSend = new HashSet<>();

    public FAWEWorldNativeAccess_1_16_R1(FAWE_Spigot_v1_16_R1 adapter, WeakReference<World> world) {
        this.adapter = adapter;
        this.world = world;
        // Use the actual tick as minecraft-defined so we don't try to force blocks into the world when the server's already lagging.
        //  - With the caveat that we don't want to have too many cached changed (1024) so we'd flush those at 1024 anyway.
        this.lastTick = new AtomicInteger(MinecraftServer.currentTick);
    }

    private World getWorld() {
        return Objects.requireNonNull(world.get(), "The reference to the world was lost");
    }

    @Override
    public void setCurrentSideEffectSet(SideEffectSet sideEffectSet) {
        this.sideEffectSet = sideEffectSet;
    }

    @Override
    public Chunk getChunk(int x, int z) {
        return getWorld().getChunkAt(x, z);
    }

    @Override
    public IBlockData toNative(BlockState state) {
        int stateId = adapter.ordinalToIbdID(state.getOrdinalChar());
        return BlockStateIdAccess.isValidInternalId(stateId)
                ? Block.getByCombinedId(stateId)
                : ((CraftBlockData) BukkitAdapter.adapt(state)).getState();
    }

    @Override
    public IBlockData getBlockState(Chunk chunk, BlockPosition position) {
        return chunk.getType(position);
    }

    @Nullable
    @Override
    public synchronized IBlockData setBlockState(Chunk chunk, BlockPosition position, IBlockData state) {
        int currentTick = MinecraftServer.currentTick;
        if (Fawe.isMainThread()) {
            return chunk.setType(position, state,
                this.sideEffectSet != null && this.sideEffectSet.shouldApply(SideEffect.UPDATE));
        }
        // Since FAWE is.. Async we need to do it on the main thread (wooooo.. :( )
        cachedChanges.add(new CachedChange(chunk, position, state));
        cachedChunksToSend.add(new IntPair(chunk.bukkitChunk.getX(), chunk.bukkitChunk.getZ()));
        boolean nextTick = lastTick.get() > currentTick;
        if (nextTick || cachedChanges.size() >= 1024) {
            if (nextTick) {
                lastTick.set(currentTick);
            }
            flushAsync(nextTick);
        }
        return state;
    }


    @Override
    public IBlockData getValidBlockForPosition(IBlockData block, BlockPosition position) {
        return Block.b(block, getWorld(), position);
    }

    @Override
    public BlockPosition getPosition(int x, int y, int z) {
        return new BlockPosition(x, y, z);
    }

    @Override
    public void updateLightingForBlock(BlockPosition position) {
        getWorld().getChunkProvider().getLightEngine().a(position);
    }

    @Override
    public boolean updateTileEntity(BlockPosition position, CompoundTag tag) {
        // We will assume that the tile entity was created for us,
        // though we do not do this on the other versions
        TileEntity tileEntity = getWorld().getTileEntity(position);
        if (tileEntity == null) {
            return false;
        }
        NBTBase nativeTag = adapter.fromNative(tag);
        tileEntity.load(tileEntity.getBlock(), (NBTTagCompound) nativeTag);
        return true;
    }

    @Override
    public void notifyBlockUpdate(BlockPosition position, IBlockData oldState, IBlockData newState) {
        getWorld().notify(position, oldState, newState, UPDATE | NOTIFY);
    }

    @Override
    public boolean isChunkTicking(Chunk chunk) {
        return chunk.getState().isAtLeast(PlayerChunk.State.TICKING);
    }

    @Override
    public void markBlockChanged(BlockPosition position) {
        ((ChunkProviderServer) getWorld().getChunkProvider()).flagDirty(position);
    }

    private static final EnumDirection[] NEIGHBOUR_ORDER = {
            EnumDirection.WEST, EnumDirection.EAST,
            EnumDirection.DOWN, EnumDirection.UP,
            EnumDirection.NORTH, EnumDirection.SOUTH
    };

    @Override
    public void notifyNeighbors(BlockPosition pos, IBlockData oldState, IBlockData newState) {
        World world = getWorld();
        if (sideEffectSet.shouldApply(SideEffect.EVENTS)) {
            world.update(pos, oldState.getBlock());
        } else {
            // When we don't want events, manually run the physics without them.
            // Un-nest neighbour updating
            for (EnumDirection direction : NEIGHBOUR_ORDER) {
                BlockPosition shifted = pos.shift(direction);
                world.getType(shifted).doPhysics(world, shifted, oldState.getBlock(), pos, false);
            }
        }
        if (newState.isComplexRedstone()) {
            world.updateAdjacentComparators(pos, newState.getBlock());
        }
    }

    @Override
    public void updateNeighbors(BlockPosition pos, IBlockData oldState, IBlockData newState, int recursionLimit) {
        World world = getWorld();
        // a == updateNeighbors
        // b == updateDiagonalNeighbors
        oldState.b(world, pos, NOTIFY, recursionLimit);
        if (sideEffectSet.shouldApply(SideEffect.EVENTS)) {
            CraftWorld craftWorld = world.getWorld();
            if (craftWorld != null) {
                BlockPhysicsEvent event = new BlockPhysicsEvent(craftWorld.getBlockAt(pos.getX(), pos.getY(), pos.getZ()), CraftBlockData.fromData(newState));
                world.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }
            }
        }
        newState.a(world, pos, NOTIFY, recursionLimit);
        newState.b(world, pos, NOTIFY, recursionLimit);
    }

    @Override
    public void onBlockStateChange(BlockPosition pos, IBlockData oldState, IBlockData newState) {
        getWorld().a(pos, oldState, newState);
    }

    private synchronized void flushAsync(final boolean sendChunks) {
        final Set<CachedChange> changes = Collections.unmodifiableSet(cachedChanges);
        cachedChanges.clear();
        final Set<IntPair> toSend;
        if (sendChunks) {
            toSend = Collections.unmodifiableSet(cachedChunksToSend);
            cachedChunksToSend.clear();
        } else {
            toSend = Collections.emptySet();
        }
        RunnableVal<Object> r = new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                changes.forEach(cc -> cc.chunk.setType(cc.position, cc.blockData,
                    sideEffectSet != null && sideEffectSet.shouldApply(SideEffect.UPDATE)));
                if (!sendChunks) {
                    return;
                }
                for (IntPair chunk : toSend) {
                    BukkitAdapter_1_16_1.sendChunk(getWorld().getWorld().getHandle(), chunk.x, chunk.z, 0, false);
                }
            }
        };
        TaskManager.IMP.async(() -> TaskManager.IMP.sync(r));
    }

    @Override
    public synchronized void flush() {
        RunnableVal<Object> r = new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                cachedChanges.forEach(cc -> cc.chunk.setType(cc.position, cc.blockData,
                    sideEffectSet != null && sideEffectSet.shouldApply(SideEffect.UPDATE)));
                for (IntPair chunk : cachedChunksToSend) {
                    BukkitAdapter_1_16_1.sendChunk(getWorld().getWorld().getHandle(), chunk.x, chunk.z, 0, false);
                }
            }
        };
        if (Fawe.isMainThread()) {
            r.run();
        } else {
            TaskManager.IMP.sync(r);
        }
        cachedChanges.clear();
        cachedChunksToSend.clear();
    }

    private static final class CachedChange {

        private final Chunk chunk;
        private final BlockPosition position;
        private final IBlockData blockData;

        private CachedChange(Chunk chunk, BlockPosition position, IBlockData blockData) {
            this.chunk = chunk;
            this.position = position;
            this.blockData = blockData;
        }
    }
}
