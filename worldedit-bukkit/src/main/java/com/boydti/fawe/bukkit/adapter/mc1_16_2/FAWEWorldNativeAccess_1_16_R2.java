package com.boydti.fawe.bukkit.adapter.mc1_16_2;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.IntPair;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.adapter.impl.FAWE_Spigot_v1_16_R2;
import com.sk89q.worldedit.internal.block.BlockStateIdAccess;
import com.sk89q.worldedit.internal.wna.WorldNativeAccess;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.block.BlockState;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.server.v1_16_R2.Block;
import net.minecraft.server.v1_16_R2.BlockPosition;
import net.minecraft.server.v1_16_R2.Chunk;
import net.minecraft.server.v1_16_R2.ChunkProviderServer;
import net.minecraft.server.v1_16_R2.EnumDirection;
import net.minecraft.server.v1_16_R2.IBlockData;
import net.minecraft.server.v1_16_R2.NBTBase;
import net.minecraft.server.v1_16_R2.NBTTagCompound;
import net.minecraft.server.v1_16_R2.PlayerChunk;
import net.minecraft.server.v1_16_R2.TileEntity;
import net.minecraft.server.v1_16_R2.World;
import org.bukkit.craftbukkit.v1_16_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R2.block.data.CraftBlockData;
import org.bukkit.event.block.BlockPhysicsEvent;

import java.lang.ref.WeakReference;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

public class FAWEWorldNativeAccess_1_16_R2 implements WorldNativeAccess<Chunk, IBlockData, BlockPosition> {
    private static final int UPDATE = 1;
    private static final int NOTIFY = 2;

    private final FAWE_Spigot_v1_16_R2 adapter;
    private final WeakReference<World> world;
    private SideEffectSet sideEffectSet;
    private final AtomicInteger lastTick;
    private final Set<CachedChange> cachedChanges = new ConcurrentSet<>();

    public FAWEWorldNativeAccess_1_16_R2(FAWE_Spigot_v1_16_R2 adapter, WeakReference<World> world) {
        this.adapter = adapter;
        this.world = world;
        this.lastTick = new AtomicInteger(getWorld().getServer().getCurrentTick());
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
    public IBlockData setBlockState(Chunk chunk, BlockPosition position, IBlockData state) {
        int currentTick = getWorld().getServer().getCurrentTick();
        if (Fawe.isMainThread()) {
            if (lastTick.get() > currentTick) {
                lastTick.set(currentTick);
                flush();
            }
            return chunk.setType(position, state,
                this.sideEffectSet != null && this.sideEffectSet.shouldApply(SideEffect.UPDATE));
        }
        // Since FAWE is.. Async we need to do it on the main thread (wooooo.. :( )
        cachedChanges.add(new CachedChange(chunk, position, state));
        if (lastTick.get() > currentTick || cachedChanges.size() > 1024) {
            lastTick.set(currentTick);
            flush();
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

    @Override
    public synchronized void flush() {
        Set<IntPair> toSend = new LinkedHashSet<>();
        RunnableVal<Object> r = new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                cachedChanges.forEach(cc -> {
                    cc.chunk.setType(cc.position, cc.blockData,
                        sideEffectSet != null && sideEffectSet.shouldApply(SideEffect.UPDATE));
                    toSend.add(new IntPair(cc.chunk.getBukkitChunk().getX(), cc.chunk.bukkitChunk.getZ()));
                });
                for (IntPair chunk : toSend) {
                    BukkitAdapter_1_16_2.sendChunk(getWorld().getWorld().getHandle(), chunk.x, chunk.z, 0, false);
                }
            }
        };
        if (Fawe.isMainThread()) {
            r.run();
        } else {
            TaskManager.IMP.sync(r);
        }
        cachedChanges.clear();
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
