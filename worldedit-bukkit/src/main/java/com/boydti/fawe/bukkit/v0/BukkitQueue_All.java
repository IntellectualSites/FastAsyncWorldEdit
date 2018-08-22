package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.util.BukkitReflectionUtils;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.NullRelighter;
import com.boydti.fawe.example.Relighter;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.ReflectionUtils;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;
import com.google.common.collect.MapMaker;
import com.sk89q.jnbt.CompoundTag;
import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

public class BukkitQueue_All extends BukkitQueue_0<ChunkSnapshot, ChunkSnapshot, ChunkSnapshot> {

    public static int ALLOCATE;
    private ConcurrentMap<Long, ChunkSnapshot> chunkCache = new MapMaker()
            .weakValues()
            .makeMap();

    public BukkitQueue_All(com.sk89q.worldedit.world.World world) {
        super(world);
        if (Settings.IMP.QUEUE.EXTRA_TIME_MS != Integer.MIN_VALUE) {
            ALLOCATE = Settings.IMP.QUEUE.EXTRA_TIME_MS;
            Settings.IMP.QUEUE.EXTRA_TIME_MS = Integer.MIN_VALUE;
            Settings.IMP.QUEUE.PARALLEL_THREADS = 1;
        }
    }

    public BukkitQueue_All(String world) {
        super(world);
        if (Settings.IMP.QUEUE.EXTRA_TIME_MS != Integer.MIN_VALUE) {
            ALLOCATE = Settings.IMP.QUEUE.EXTRA_TIME_MS;
            Settings.IMP.QUEUE.EXTRA_TIME_MS = Integer.MIN_VALUE;
            Settings.IMP.QUEUE.PARALLEL_THREADS = 1;
        }
    }

    @Override
    public boolean queueChunkLoad(int cx, int cz, RunnableVal<ChunkSnapshot> operation) {
        if (PAPER) {
            try {
                new PaperChunkCallback(getImpWorld(), cx, cz) {
                    @Override
                    public void onLoad(Chunk chunk) {
                        try {
                            ChunkSnapshot snapshot = tryGetSnasphot(chunk);
                            operation.run(snapshot);
                        } catch (Throwable e) {
                            PAPER = false;
                        }
                    }
                };
                return true;
            } catch (Throwable ignore) {
                PAPER = false;
            }
        }
        return super.queueChunkLoad(cx, cz);
    }

    @Override
    public Relighter getRelighter() {
        return NullRelighter.INSTANCE;
    }

    private static Class<?> classRegionFileCache;
    private static Class<?> classRegionFile;
    private static Class<?> classCraftChunk;
    private static Class<?> classCraftWorld;
    private static Class<?> classNMSChunk;
    private static Class<?> classNMSWorld;
    private static Class<?> classChunkProviderServer;
    private static Class<?> classIChunkLoader;
    private static Class<?> classChunkRegionLoader;
    private static Class<?> classIChunkProvider;
    private static Method methodGetHandleChunk;
    private static Method methodGetHandleWorld;
    private static Method methodFlush;
    private static Method methodNeedsSaving;
    private static Field fieldChunkProvider;
    private static Field fieldChunkLoader;
    private static Field fieldRegionMap;
    private static Field fieldRegionRAF;

    static {
        try {
            BukkitReflectionUtils.init();
            classRegionFileCache = BukkitReflectionUtils.getNmsClass("RegionFileCache");
            classRegionFile = BukkitReflectionUtils.getNmsClass("RegionFile");
            classCraftChunk = BukkitReflectionUtils.getCbClass("CraftChunk");
            classNMSChunk = BukkitReflectionUtils.getNmsClass("Chunk");
            classCraftWorld = BukkitReflectionUtils.getCbClass("CraftWorld");
            classNMSWorld = BukkitReflectionUtils.getNmsClass("World");
            classChunkProviderServer = BukkitReflectionUtils.getNmsClass("ChunkProviderServer");
            classIChunkProvider = BukkitReflectionUtils.getNmsClass("IChunkProvider");
            classIChunkLoader = BukkitReflectionUtils.getNmsClass("IChunkLoader");
            classChunkRegionLoader = BukkitReflectionUtils.getNmsClass("ChunkRegionLoader");

            methodGetHandleChunk = ReflectionUtils.setAccessible(classCraftChunk.getDeclaredMethod("getHandle"));
            methodGetHandleWorld = ReflectionUtils.setAccessible(classCraftWorld.getDeclaredMethod("getHandle"));
            methodFlush = ReflectionUtils.findMethod(classChunkRegionLoader, boolean.class);
            methodNeedsSaving = ReflectionUtils.findMethod(classNMSChunk, boolean.class, boolean.class);

            fieldChunkProvider = ReflectionUtils.findField(classNMSWorld, classIChunkProvider);
            fieldChunkLoader = ReflectionUtils.findField(classChunkProviderServer, classIChunkLoader);

            fieldRegionMap = ReflectionUtils.findField(classRegionFileCache, Map.class);
            fieldRegionRAF = ReflectionUtils.findField(classRegionFile, RandomAccessFile.class);
        } catch (Throwable ignore) {
            ignore.printStackTrace();
        }
    }

    @Override
    public boolean setMCA(int mcaX, int mcaZ, RegionWrapper allowed, Runnable whileLocked, boolean saveChunks, boolean load) {
        if (classRegionFileCache == null) {
            return super.setMCA(mcaX, mcaZ, allowed, whileLocked, saveChunks, load);
        }
        TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                long start = System.currentTimeMillis();
                long last = start;
                synchronized (classRegionFileCache) {
                    try {
                        World world = getWorld();
                        boolean autoSave = world.isAutoSave();

                        if (world.getKeepSpawnInMemory()) world.setKeepSpawnInMemory(false);

                        ArrayDeque<Chunk> unloaded = null;
                        if (load) {
                            int bcx = mcaX << 5;
                            int bcz = mcaZ << 5;
                            int tcx = bcx + 31;
                            int tcz = bcz + 31;
                            for (Chunk chunk : world.getLoadedChunks()) {
                                int cx = chunk.getX();
                                int cz = chunk.getZ();
                                if (cx >= bcx && cx <= tcx && cz >= bcz && cz <= tcz) {
                                    Object nmsChunk = methodGetHandleChunk.invoke(chunk);
                                    boolean mustSave = saveChunks && (boolean) methodNeedsSaving.invoke(nmsChunk, false);
                                    chunk.unload(mustSave, false);
                                    if (unloaded == null) unloaded = new ArrayDeque<Chunk>();
                                    unloaded.add(chunk);
                                }
                            }
                        } else {
                            world.save();
                        }

                        Object nmsWorld = methodGetHandleWorld.invoke(world);
                        Object chunkProviderServer = fieldChunkProvider.get(nmsWorld);
                        Object chunkRegionLoader = fieldChunkLoader.get(chunkProviderServer);
                        while ((boolean) methodFlush.invoke(chunkRegionLoader));

                        if (unloaded != null) {
                            Map regionMap = (Map) fieldRegionMap.get(null);
                            File file = new File(world.getWorldFolder(), "region" + File.separator + "r." + mcaX + "." + mcaZ + ".mca");
                            Object regionFile = regionMap.remove(file);
                            if (regionFile != null) {
                                RandomAccessFile raf = (RandomAccessFile) fieldRegionRAF.get(regionFile);
                                raf.close();
                            }
                        }

                        whileLocked.run();

                        if (load && unloaded != null) {
                            final ArrayDeque<Chunk> finalUnloaded = unloaded;
                            TaskManager.IMP.async(new Runnable() {
                                @Override
                                public void run() {
                                    for (Chunk chunk : finalUnloaded) {
                                        int cx = chunk.getX();
                                        int cz = chunk.getZ();
                                        if (world.isChunkLoaded(cx, cz)) continue;
                                        SetQueue.IMP.addTask(() -> {
                                            world.loadChunk(chunk.getX(), chunk.getZ(), false);
                                            world.refreshChunk(chunk.getX(), chunk.getZ());
                                        });

                                    }
                                }
                            });
                            // load chunks

                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        return true;
    }

    @Override
    public void setHeightMap(FaweChunk chunk, byte[] heightMap) {
        // Not supported
    }

    @Override
    public void setSkyLight(ChunkSnapshot chunk, int x, int y, int z, int value) {
        // Not supported
    }

    @Override
    public void setBlockLight(ChunkSnapshot chunk, int x, int y, int z, int value) {
        // Not supported
    }

    @Override
    public int getCombinedId4Data(ChunkSnapshot chunk, int x, int y, int z) {
        if (chunk.isSectionEmpty(y >> 4)) {
            return BlockTypes.AIR.getInternalId();
        }
        BlockData blockData = chunk.getBlockData(x & 15, y, z & 15);
        return BukkitAdapter.adapt(blockData).getInternalId();
    }

    @Override
    public int getBiome(ChunkSnapshot chunkSnapshot, int x, int z) {
        Biome biome = chunkSnapshot.getBiome(x & 15, z & 15);
        return getAdapter().getBiomeId(biome);
    }

    @Override
    public ChunkSnapshot getSections(ChunkSnapshot chunkSnapshot) {
        return chunkSnapshot;
    }

    @Override
    public ChunkSnapshot getCachedChunk(World world, int cx, int cz) {
        long pair = MathMan.pairInt(cx, cz);
        ChunkSnapshot cached = chunkCache.get(pair);
        if (cached != null) return cached;
        if (world.isChunkLoaded(cx, cz)) {
            Long originalKeep = keepLoaded.get(pair);
            keepLoaded.put(pair, Long.MAX_VALUE);
            if (world.isChunkLoaded(cx, cz)) {
                Chunk chunk = world.getChunkAt(cx, cz);
                ChunkSnapshot snapshot = getAndCacheChunk(chunk);
                if (originalKeep != null) {
                    keepLoaded.put(pair, originalKeep);
                } else {
                    keepLoaded.remove(pair);
                }
                return snapshot;
            } else {
                keepLoaded.remove(pair);
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public int getEmmittedLight(final ChunkSnapshot chunk, int x, int y, int z) {
        return chunk.getBlockEmittedLight(x & 15, y, z & 15);
    }

    @Override
    public int getSkyLight(final ChunkSnapshot chunk, int x, int y, int z) {
        return chunk.getBlockSkyLight(x & 15, y, z & 15);
    }

    @Override
    public int getLight(final ChunkSnapshot chunk, int x, int y, int z) {
        x = x & 15;
        z = z & 15;
        return Math.max(chunk.getBlockEmittedLight(x, y, z), chunk.getBlockSkyLight(x, y, z));
    }

    @Override
    public ChunkSnapshot loadChunk(World world, int x, int z, boolean generate) {
        Chunk chunk = world.getChunkAt(x, z);
        chunk.load(generate);
        return chunk.isLoaded() ? getAndCacheChunk(chunk) : null;
    }

    private ChunkSnapshot tryGetSnasphot(Chunk chunk) {
        try {
            return chunk.getChunkSnapshot(false, true, false);
        } catch (Throwable ignore) {
            Throwable cause = ignore;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (cause instanceof IllegalStateException) {
                return null;
            }
            throw ignore;
        }
    }

    private ChunkSnapshot getAndCacheChunk(Chunk chunk) {
        ChunkSnapshot snapshot = tryGetSnasphot(chunk);
        if (snapshot == null) {
            snapshot = tryGetSnasphot(chunk);
            if (snapshot == null) {
                snapshot = TaskManager.IMP.sync(() -> tryGetSnasphot(chunk));
                if (snapshot == null) {
                    snapshot = chunk.getChunkSnapshot(false, true, false);
                }
            }
        }
        chunkCache.put(MathMan.pairInt(chunk.getX(), chunk.getZ()), snapshot);
        return snapshot;
    }

    @Override
    public ChunkSnapshot getCachedSections(World impWorld, int cx, int cz) {
        return getCachedChunk(impWorld, cx, cz);
    }

    @Override
    public CompoundTag getTileEntity(ChunkSnapshot chunk, int x, int y, int z) {
        if (getAdapter() == null) {
            return null;
        }
        Location loc = new Location(getWorld(), x, y, z);
        BlockStateHolder block = getAdapter().getBlock(loc);
        return block.getNbtData();
    }

    @Override
    public FaweChunk getFaweChunk(int x, int z) {
        return new BukkitChunk_All(this, x, z);
    }

    @Override
    public boolean supports(Capability capability) {
        switch (capability) {
            case CHANGE_TASKS: return getAdapter() != null;
        }
        return super.supports(capability);
    }

    private int skip;

    @Override
    public void startSet(boolean parallel) {
        super.startSet(true);
    }

    private Field fieldNeighbors;
    private Method chunkGetHandle;

    /**
     * Exploiting a bug in the vanilla lighting algorithm for faster block placement
     *  - Could have been achieved without reflection by force unloading specific chunks
     *  - Much faster just setting the variable manually though
     * @param chunk
     * @return
     */
    protected Object[] disableLighting(Chunk chunk) {
        try {
            if (chunkGetHandle == null) {
                chunkGetHandle = chunk.getClass().getDeclaredMethod("getHandle");
                chunkGetHandle.setAccessible(true);
            }
            Object nmsChunk = chunkGetHandle.invoke(chunk);
            if (fieldNeighbors == null) {
                fieldNeighbors = nmsChunk.getClass().getDeclaredField("neighbors");
                fieldNeighbors.setAccessible(true);
            }
            Object value = fieldNeighbors.get(nmsChunk);
            fieldNeighbors.set(nmsChunk, 0);
            return new Object[] {nmsChunk, value};
        } catch (Throwable ignore) {}
        return null;
    }

    protected void disableLighting(Object[] disableResult) {
        if (disableResult != null) {
            try {
                fieldNeighbors.set(disableResult[0], 0);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    protected void resetLighting(Object[] disableResult) {
        if (disableResult != null) {
            try {
                fieldNeighbors.set(disableResult[0], disableResult[1]);
            } catch (Throwable ignore) {
                ignore.printStackTrace();
            }
        }
    }

    protected void enableLighting(Object[] disableResult) {
        if (disableResult != null) {
            try {
                fieldNeighbors.set(disableResult[0], 0x739C0);
            } catch (Throwable ignore) {}
        }
    }

    @Override
    public void endSet(boolean parallel) {
        super.endSet(true);
    }
}
