package com.boydti.fawe.object;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.NullRelighter;
import com.boydti.fawe.example.Relighter;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.SetQueue;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * A queue based Extent capable of queing chunk and region changes
 */
public interface FaweQueue extends HasFaweQueue, Extent {

    enum ProgressType {
        QUEUE,
        DISPATCH,
        DONE,
    }

    enum RelightMode {
        NONE,
        OPTIMAL,
        ALL,
    }

    enum Capability {
        // If history can be recorded in an async task by the dispatcher
        CHANGE_TASKS,
        // If custom chunk packets can be sent
        CHUNK_PACKETS
        //
    }

    default Relighter getRelighter() {
        return NullRelighter.INSTANCE;
    }

    @Override
    default BlockVector3 getMinimumPoint() {
        return BlockVector3.at(-30000000, 0, -30000000);
    }

    @Override
    default BlockVector3 getMaximumPoint() {
        return BlockVector3.at(30000000, getMaxY(), 30000000);
    }

    @Override
    default BlockState getLazyBlock(int x, int y, int z) {
        int combinedId4Data = getCachedCombinedId4Data(x, y, z, BlockTypes.AIR.getInternalId());
        try {
            return BlockState.getFromInternalId(combinedId4Data);
        } catch (Throwable e) {
            e.printStackTrace();
            return BlockTypes.AIR.getDefaultState();
        }
    }

    @Override
    default <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) throws WorldEditException {
        return setBlock(x, y, z, block.getInternalId(), block instanceof BaseBlock ? block.getNbtData() : null);
    }

    @Override
    default BaseBlock getFullBlock(BlockVector3 position) {
        int combinedId4Data = getCachedCombinedId4Data(position.getBlockX(), position.getBlockY(), position.getBlockZ(), BlockTypes.AIR.getInternalId());
        try {
            BaseBlock block = BaseBlock.getFromInternalId(combinedId4Data, null);
            if (block.getMaterial().hasContainer()) {
                CompoundTag tile = getTileEntity(position.getBlockX(), position.getBlockY(), position.getBlockZ());
                if (tile != null) {
                    return BaseBlock.getFromInternalId(combinedId4Data, tile);
                }
            }
            return block;
        } catch (Throwable e) {
            e.printStackTrace();
            return BlockTypes.AIR.getDefaultState().toBaseBlock();
        }
    }

    @Override
    default BiomeType getBiome(BlockVector2 position) {
        return null;
    }

    @Override
    default <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block) throws WorldEditException {
        return setBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ(), block);
    }

    boolean setBlock(final int x, final int y, final int z, int combinedId);

    default boolean setBlock(final int x, final int y, final int z, int combinedId, CompoundTag nbtData) {
        if (setBlock(x, y, z, combinedId)) {
            if (nbtData != null) setTile(x, y, z, nbtData);
            return true;
        }
        return false;
    }

    @Override
    default boolean setBiome(BlockVector2 position, BiomeType biome) {
        return setBiome(position.getBlockX(), position.getBlockZ(), biome);
    }

    @Override
    default FaweQueue getQueue() {
        return this;
    }



    default void addEditSession(EditSession session) {
        if (session == null) {
            return;
        }
        Collection<EditSession> sessions = getEditSessions();
        sessions.add(session);
    }

    /**
     * Add a progress task<br>
     * - Progress type
     * - Amount of type
     *
     * @param progressTask
     */
    default void setProgressTracker(RunnableVal2<ProgressType, Integer> progressTask) {
        this.setProgressTask(progressTask);
    }

    default Collection<EditSession> getEditSessions() {
        return Collections.emptySet();
    }

    default boolean supports(Capability capability) {
        return false;
    }

    default void optimize() {}

    default int setBlocks(CuboidRegion cuboid, int combinedId) {
        RegionWrapper current = new RegionWrapper(cuboid.getMinimumPoint(), cuboid.getMaximumPoint());
        final int minY = cuboid.getMinimumY();
        final int maxY = cuboid.getMaximumY();

        final FaweChunk<?> fc = getFaweChunk(0, 0);
        fc.fillCuboid(0, 15, minY, maxY, 0, 15, combinedId);
        fc.optimize();

        MainUtil.chunkTaskSync(current, new RunnableVal<int[]>() {
            @Override
            public void run(int[] value) {
                FaweChunk newChunk;
                if (value[6] == 0) {
                    newChunk = fc.copy(true);
                    newChunk.setLoc(FaweQueue.this, value[0], value[1]);
                } else {
                    int bx = value[2] & 15;
                    int tx = value[4] & 15;
                    int bz = value[3] & 15;
                    int tz = value[5] & 15;
                    if (bx == 0 && tx == 15 && bz == 0 && tz == 15) {
                        newChunk = fc.copy(true);
                        newChunk.setLoc(FaweQueue.this, value[0], value[1]);
                    } else {
                        newChunk = FaweQueue.this.getFaweChunk(value[0], value[1]);
                        newChunk.fillCuboid(value[2] & 15, value[4] & 15, minY, maxY, value[3] & 15, value[5] & 15, combinedId);
                    }
                }
                newChunk.addToQueue();
            }
        });
        return cuboid.getArea();
    }

    void setTile(int x, int y, int z, CompoundTag tag);

    void setEntity(int x, int y, int z, CompoundTag tag);

    void removeEntity(int x, int y, int z, UUID uuid);

    boolean setBiome(final int x, final int z, final BiomeType biome);

    FaweChunk getFaweChunk(int x, int z);

    Collection<FaweChunk> getFaweChunks();

    default boolean setMCA(int mcaX, int mcaZ, RegionWrapper region, Runnable whileLocked, boolean save, boolean load) {
        if (whileLocked != null) whileLocked.run();
        return true;
    }

    void setChunk(final FaweChunk chunk);

    File getSaveFolder();

    @Override
    default int getMaxY() {
        World weWorld = getWEWorld();
        return weWorld == null ? 255 : weWorld.getMaxY();
    }

    default Settings getSettings() {
        return Settings.IMP;
    }

    default void setSettings(Settings settings) {

    }

    void setWorld(String world);

    World getWEWorld();

    String getWorldName();

    long getModified();

    void setModified(long modified);

    RunnableVal2<ProgressType, Integer> getProgressTask();

    void setProgressTask(RunnableVal2<ProgressType, Integer> progressTask);

    void setChangeTask(RunnableVal2<FaweChunk, FaweChunk> changeTask);

    RunnableVal2<FaweChunk, FaweChunk> getChangeTask();

    SetQueue.QueueStage getStage();

    void setStage(SetQueue.QueueStage stage);

    void addNotifyTask(Runnable runnable);

    void runTasks();

    void addTask(Runnable whenFree);

    default void forEachBlockInChunk(int cx, int cz, RunnableVal2<BlockVector3, BaseBlock> onEach) {
        int bx = cx << 4;
        int bz = cz << 4;
        MutableBlockVector3 mutable = new MutableBlockVector3(0, 0, 0);
        for (int x = 0; x < 16; x++) {
            int xx = x + bx;
            mutable.mutX(xx);
            for (int z = 0; z < 16; z++) {
                int zz = z + bz;
                mutable.mutZ(zz);
                for (int y = 0; y <= getMaxY(); y++) {
                    int combined = getCombinedId4Data(xx, y, zz);
                    BaseBlock block = BlockState.getFromInternalId(combined).toBaseBlock();
                    BlockType type = block.getBlockType();
                    if (type.getMaterial().isAir()) {
                        continue;
                    }
                    mutable.mutY(y);
                    CompoundTag tile = getTileEntity(x, y, z);
                    if (tile != null) {
                        onEach.run(mutable, block.toBaseBlock(tile));
                    } else {
                        onEach.run(mutable, block);
                    }
                }
            }
        }
    }

    default void forEachTileInChunk(int cx, int cz, RunnableVal2<BlockVector3, BaseBlock> onEach) {
        int bx = cx << 4;
        int bz = cz << 4;
        MutableBlockVector3 mutable = new MutableBlockVector3(0, 0, 0);
        for (int x = 0; x < 16; x++) {
            int xx = x + bx;
            for (int z = 0; z < 16; z++) {
                int zz = z + bz;
                for (int y = 0; y < getMaxY(); y++) {
                    int combined = getCombinedId4Data(xx, y, zz);
                    if (combined == 0) {
                        continue;
                    }
                    BlockType type = BlockTypes.getFromStateId(combined);
                    if (type.getMaterial().hasContainer()) {
                        CompoundTag tile = getTileEntity(x, y, z);
                        if (tile != null) {
                            mutable.mutX(xx);
                            mutable.mutZ(zz);
                            mutable.mutY(y);
                            BaseBlock block = BaseBlock.getFromInternalId(combined, tile);
                            onEach.run(mutable, block);
                        }
                    }
                }
            }
        }
    }

    @Deprecated
    default boolean regenerateChunk(int x, int z) {
        return regenerateChunk(x, z, null, null);
    }

    boolean regenerateChunk(int x, int z, @Nullable BiomeType biome, @Nullable Long seed);

    default void startSet(boolean parallel) {
    }

    default void endSet(boolean parallel) {
    }

    default int cancel() {
        clear();
        int count = 0;
        for (EditSession session : getEditSessions()) {
            if (session.cancel()) {
                count++;
            }
        }
        return count;
    }

    void sendBlockUpdate(FaweChunk chunk, FawePlayer... players);

    default void sendChunkUpdate(FaweChunk chunk, FawePlayer... players) {
        sendBlockUpdate(chunk, players);
    }

    @Deprecated
    default boolean next() {
        int amount = Settings.IMP.QUEUE.PARALLEL_THREADS;
        long time = 20; // 30ms
        return next(amount, time);
    }

    /**
     * Gets the FaweChunk and sets the requested blocks
     *
     * @return
     */
    boolean next(int amount, long time);

    default void saveMemory() {
        MainUtil.sendAdmin(BBC.OOM.s());
        // Set memory limited
        MemUtil.memoryLimitedTask();
        // Clear block placement
        clear();
        Fawe.get().getWorldEdit().getSessionManager().clear();
        // GC
        System.gc();
        System.gc();
        // Unload chunks
    }

    void sendChunk(FaweChunk chunk);

    void sendChunk(int x, int z, int bitMask);

    /**
     * This method is called when the server is < 1% available memory
     */
    void clear();

    default boolean hasBlock(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return getCombinedId4Data(x, y, z) != 0;
    }

    BiomeType getBiomeType(int x, int z) throws FaweException.FaweChunkLoadException;

    int getCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException;

    int getCachedCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException;

    default int getAdjacentLight(int x, int y, int z) {
        int light = 0;
        if ((light = Math.max(light, getSkyLight(x - 1, y, z))) == 15) {
            return light;
        }
        if ((light = Math.max(light, getSkyLight(x + 1, y, z))) == 15) {
            return light;
        }
        if ((light = Math.max(light, getSkyLight(x, y, z - 1))) == 15) {
            return light;
        }
        return Math.max(light, getSkyLight(x, y, z + 1));
    }

    boolean hasSky();

    int getSkyLight(int x, int y, int z);

    default int getLight(int x, int y, int z) {
        if (!hasSky()) {
            return getEmmittedLight(x, y, z);
        }
        return Math.max(getSkyLight(x, y, z), getEmmittedLight(x, y, z));
    }

    int getEmmittedLight(int x, int y, int z);

    CompoundTag getTileEntity(int x, int y, int z) throws FaweException.FaweChunkLoadException;

    default int getCombinedId4Data(int x, int y, int z, int def) {
        try {
            return getCombinedId4Data(x, y, z);
        } catch (FaweException ignore) {
            return def;
        }
    }

    default int getCachedCombinedId4Data(int x, int y, int z, int def) {
        try {
            return getCachedCombinedId4Data(x, y, z);
        } catch (FaweException ignore) {
            return def;
        }
    }

    default int getCombinedId4DataDebug(int x, int y, int z, int def, EditSession session) {
        try {
            return getCombinedId4Data(x, y, z);
        } catch (FaweException ignore) {
            BBC.WORLDEDIT_FAILED_LOAD_CHUNK.send(session.getPlayer(),x >> 4, z >> 4);
            return def;
        } catch (Throwable e) {
            e.printStackTrace();
            return BlockTypes.AIR.getInternalId();
        }
    }

    default int getBrightness(int x, int y, int z) {
        int combined = getCombinedId4Data(x, y, z);
        if (combined == 0) {
            return 0;
        }
        return BlockTypes.getFromStateId(combined).getMaterial().getLightValue();
    }

    default int getOpacityBrightnessPair(int x, int y, int z) {
        return MathMan.pair16(Math.min(15, getOpacity(x, y, z)), getBrightness(x, y, z));
    }

    default int getOpacity(int x, int y, int z) {
        int combined = getCombinedId4Data(x, y, z);
        if (combined == 0) {
            return 0;
        }
        return BlockTypes.getFromStateId(combined).getMaterial().getLightOpacity();
    }

    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Lock the thread until the queue is empty
     */
    default void flush() {
        flush(10000);
    }

    /**
     * Lock the thread until the queue is empty
     */
    default void flush(int time) {
        if (size() > 0) {
            if (Fawe.isMainThread()) {
                SetQueue.IMP.flush(this);
            } else {
                if (enqueue()) {
                    while (!isEmpty() && getStage() == SetQueue.QueueStage.ACTIVE) {
                        synchronized (this) {
                            try {
                                this.wait(time);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    default boolean enqueue() {
        return SetQueue.IMP.enqueue(this);
    }

    default void dequeue() {
        SetQueue.IMP.dequeue(this);
    }
}
