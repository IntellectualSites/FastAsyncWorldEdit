package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R3;

import com.fastasyncworldedit.core.math.IntPair;
import com.fastasyncworldedit.core.util.FoliaSupport;
import com.fastasyncworldedit.core.util.TaskManager;
import com.fastasyncworldedit.core.util.collection.FlushingPartitionedCache;
import com.fastasyncworldedit.core.util.task.RunnableVal;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.internal.block.BlockStateIdAccess;
import com.sk89q.worldedit.internal.wna.WorldNativeAccess;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import com.sk89q.worldedit.world.block.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.block.data.CraftBlockData;
import org.bukkit.event.block.BlockPhysicsEvent;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class PaperweightFaweWorldNativeAccess implements WorldNativeAccess<LevelChunk,
        net.minecraft.world.level.block.state.BlockState, BlockPos> {

    private static final int UPDATE = 1;
    private static final int NOTIFY = 2;
    private static final Direction[] NEIGHBOUR_ORDER = {
            Direction.EAST,
            Direction.WEST,
            Direction.DOWN,
            Direction.UP,
            Direction.NORTH,
            Direction.SOUTH
    };
    private final PaperweightFaweAdapter paperweightFaweAdapter;
    private final WeakReference<Level> level;
    private final AtomicInteger lastTick;
    private final FlushingPartitionedCache<IntPair, CachedChange, Set<CachedChange>> cache;
    private SideEffectSet sideEffectSet;

    public PaperweightFaweWorldNativeAccess(PaperweightFaweAdapter paperweightFaweAdapter, WeakReference<Level> level) {
        this.paperweightFaweAdapter = paperweightFaweAdapter;
        this.level = level;
        // Use the actual tick as minecraft-defined so we don't try to force blocks into the world when the server's already lagging.
        //  - With the caveat that we don't want to have too many cached changed (1024) so we'd flush those at 1024 anyway.
        this.lastTick = new AtomicInteger();
        if (!FoliaSupport.isFolia()) {
            this.lastTick.set(Bukkit.getCurrentTick());
        }
        this.cache = new FlushingPartitionedCache<>(
                cachedChange -> new IntPair(cachedChange.blockPos.getX() >> 4, cachedChange.blockPos.getZ() >> 4),
                HashSet::new,
                (chunk, changes) -> {
                    // TODO (folia) only send chunks based on ticks? Need to make sure everything actually flushed in the end
                    /*boolean nextTick = true;
                    if (!FoliaSupport.isFolia()) {
                        int currentTick = MinecraftServer.currentTick;
                        nextTick = lastTick.get() > currentTick;
                        if (nextTick) {
                            lastTick.set(currentTick);
                        }
                    }*/
                    flushAsync(chunk, changes, true);
                },
                2048,
                16
        );
    }

    private Level getLevel() {
        return Objects.requireNonNull(level.get(), "The reference to the world was lost");
    }

    @Override
    public void setCurrentSideEffectSet(SideEffectSet sideEffectSet) {
        this.sideEffectSet = sideEffectSet;
    }

    @Override
    public LevelChunk getChunk(int x, int z) {
        return getLevel().getChunk(x, z);
    }

    @Override
    public net.minecraft.world.level.block.state.BlockState toNative(BlockState blockState) {
        int stateId = paperweightFaweAdapter.ordinalToIbdID(blockState.getOrdinalChar());
        return BlockStateIdAccess.isValidInternalId(stateId)
                ? Block.stateById(stateId)
                : ((CraftBlockData) BukkitAdapter.adapt(blockState)).getState();
    }

    @Override
    public net.minecraft.world.level.block.state.BlockState getBlockState(LevelChunk levelChunk, BlockPos blockPos) {
        return levelChunk.getBlockState(blockPos);
    }

    @Nullable
    @Override
    public synchronized net.minecraft.world.level.block.state.BlockState setBlockState(
            LevelChunk levelChunk, BlockPos blockPos,
            net.minecraft.world.level.block.state.BlockState blockState
    ) {
        if (PaperweightPlatformAdapter.isTickThreadFor(levelChunk)) {
            return levelChunk.setBlockState(blockPos, blockState,
                    this.sideEffectSet != null && this.sideEffectSet.shouldApply(SideEffect.UPDATE)
            );
        }
        // Since FAWE is.. Async we need to do it on the main thread (wooooo.. :( )
        cache.insert(new CachedChange(levelChunk, blockPos, blockState));
        return blockState;
    }

    @Override
    public net.minecraft.world.level.block.state.BlockState getValidBlockForPosition(
            net.minecraft.world.level.block.state.BlockState blockState,
            BlockPos blockPos
    ) {
        return Block.updateFromNeighbourShapes(blockState, getLevel(), blockPos);
    }

    @Override
    public BlockPos getPosition(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    @Override
    public void updateLightingForBlock(BlockPos blockPos) {
        getLevel().getChunkSource().getLightEngine().checkBlock(blockPos);
    }

    @Override
    public boolean updateTileEntity(BlockPos blockPos, CompoundBinaryTag tag) {
        // We will assume that the tile entity was created for us,
        // though we do not do this on the other versions
        BlockEntity blockEntity = getLevel().getBlockEntity(blockPos);
        if (blockEntity == null) {
            return false;
        }
        net.minecraft.nbt.Tag nativeTag = paperweightFaweAdapter.fromNativeBinary(tag);
        blockEntity.load((CompoundTag) nativeTag);
        return true;
    }

    @Override
    public void notifyBlockUpdate(
            LevelChunk levelChunk, BlockPos blockPos,
            net.minecraft.world.level.block.state.BlockState oldState,
            net.minecraft.world.level.block.state.BlockState newState
    ) {
        if (levelChunk.getSections()[level.get().getSectionIndex(blockPos.getY())] != null) {
            getLevel().sendBlockUpdated(blockPos, oldState, newState, UPDATE | NOTIFY);
        }
    }

    @Override
    public boolean isChunkTicking(LevelChunk levelChunk) {
        return levelChunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING);
    }

    @Override
    public void markBlockChanged(LevelChunk levelChunk, BlockPos blockPos) {
        if (levelChunk.getSections()[level.get().getSectionIndex(blockPos.getY())] != null) {
            ((ServerChunkCache) getLevel().getChunkSource()).blockChanged(blockPos);
        }
    }

    @Override
    public void notifyNeighbors(
            BlockPos blockPos,
            net.minecraft.world.level.block.state.BlockState oldState,
            net.minecraft.world.level.block.state.BlockState newState
    ) {
        Level level = getLevel();
        if (sideEffectSet.shouldApply(SideEffect.EVENTS)) {
            level.blockUpdated(blockPos, oldState.getBlock());
        } else {
            // When we don't want events, manually run the physics without them.
            // Un-nest neighbour updating
            for (Direction direction : NEIGHBOUR_ORDER) {
                BlockPos shifted = blockPos.relative(direction);
                level.getBlockState(shifted).neighborChanged(level, shifted, oldState.getBlock(), blockPos, false);
            }
        }
        if (newState.hasAnalogOutputSignal()) {
            level.updateNeighbourForOutputSignal(blockPos, newState.getBlock());
        }
    }

    @Override
    public void updateNeighbors(
            BlockPos blockPos,
            net.minecraft.world.level.block.state.BlockState oldState,
            net.minecraft.world.level.block.state.BlockState newState,
            int recursionLimit
    ) {
        Level level = getLevel();
        // a == updateNeighbors
        // b == updateDiagonalNeighbors
        oldState.updateIndirectNeighbourShapes(level, blockPos, NOTIFY, recursionLimit);
        if (sideEffectSet.shouldApply(SideEffect.EVENTS)) {
            CraftWorld craftWorld = level.getWorld();
            if (craftWorld != null) {
                BlockPhysicsEvent event = new BlockPhysicsEvent(
                        craftWorld.getBlockAt(blockPos.getX(), blockPos.getY(), blockPos.getZ()),
                        CraftBlockData.fromData(newState)
                );
                level.getCraftServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }
            }
        }
        newState.triggerEvent(level, blockPos, NOTIFY, recursionLimit);
        newState.updateIndirectNeighbourShapes(level, blockPos, NOTIFY, recursionLimit);
    }

    @Override
    public void onBlockStateChange(
            BlockPos blockPos,
            net.minecraft.world.level.block.state.BlockState oldState,
            net.minecraft.world.level.block.state.BlockState newState
    ) {
        getLevel().onBlockStateChange(blockPos, oldState, newState);
    }

    private synchronized void flushAsync(IntPair chunk, Set<CachedChange> changes, final boolean sendChunks) {
        RunnableVal<Object> runnableVal = new RunnableVal<>() {
            @Override
            public void run(Object value) {
                changes.forEach(cc -> cc.levelChunk.setBlockState(cc.blockPos, cc.blockState,
                        sideEffectSet != null && sideEffectSet.shouldApply(SideEffect.UPDATE)
                ));
                if (!sendChunks) {
                    return;
                }
                PaperweightPlatformAdapter.sendChunk(getLevel().getWorld().getHandle(), chunk.x(), chunk.z(), false);
            }
        };
        TaskManager.taskManager().async(
                () -> PaperweightPlatformAdapter.task(runnableVal, getLevel().getWorld().getHandle(), chunk.x(), chunk.z())
        );
    }

    @Override
    public synchronized void flush() {
        this.cache.flush();
    }

    private record CachedChange(
            LevelChunk levelChunk,
            BlockPos blockPos,
            net.minecraft.world.level.block.state.BlockState blockState
    ) {

    }

}
