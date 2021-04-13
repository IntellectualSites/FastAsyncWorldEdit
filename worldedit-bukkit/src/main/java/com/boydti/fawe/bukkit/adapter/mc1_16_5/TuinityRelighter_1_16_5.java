package com.boydti.fawe.bukkit.adapter.mc1_16_5;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IQueueChunk;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.implementation.chunk.ChunkHolder;
import com.boydti.fawe.beta.implementation.lighting.Relighter;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.server.v1_16_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R3.LightEngineThreaded;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

public class TuinityRelighter_1_16_5 implements Relighter {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private static final IntConsumer nothingIntConsumer = i -> {};
    private static final MethodHandle relight;

    private final WorldServer world;
    private final ReentrantLock lock = new ReentrantLock();

    private final IQueueExtent<IQueueChunk> queue;

    private final ReentrantLock lightLock = new ReentrantLock();
    private final LongList chunks = new LongArrayList();

    static {
        MethodHandle tmp = null;
        try {
            Method relightMethod = LightEngineThreaded.class.getMethod(
                    "relight",
                    Set.class,
                    Consumer.class,
                    IntConsumer.class
            );
            tmp = MethodHandles.lookup().unreflect(relightMethod);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            LOGGER.error("Failed to locate relight method in LightEngineThreaded on Tuinity. " +
                    "Is everything up to date?", e);
        }
        relight = tmp;
    }

    public TuinityRelighter_1_16_5(WorldServer world, IQueueExtent<IQueueChunk> queue) {
        this.queue = queue;
        this.world = world;
    }

    @Override
    public boolean addChunk(int cx, int cz, byte[] skipReason, int bitmask) {
        lightLock.lock();
        try {
            chunks.add(ChunkCoordIntPair.pair(cx, cz));
        } finally {
            lightLock.unlock();
        }
        return true;
    }

    @Override
    public void addLightUpdate(int x, int y, int z) {

    }

    @Override
    public void fixLightingSafe(boolean sky) {
        Set<ChunkCoordIntPair> chunks = this.chunks.stream().map(ChunkCoordIntPair::new).collect(Collectors.toSet());
        TaskManager.IMP.task(() ->
                {
                    try {
                        // cast is required for invokeExact
                        int unused = (int) relight.invokeExact(world.getChunkProvider().getLightEngine(),
                                (Set<?>) new HashSet<>(chunks), // explicit cast to make invokeExact work
                                (Consumer<?>) coord -> {}, // no callback
                                nothingIntConsumer
                        );
                    } catch (Throwable throwable) {
                        LOGGER.error("Error occurred on relighting", throwable);
                    }
                }
        );
        if (Settings.IMP.LIGHTING.DELAY_PACKET_SENDING) {
            for (long key : this.chunks) {
                int x = ChunkCoordIntPair.getX(key);
                int z = ChunkCoordIntPair.getZ(key);
                ChunkHolder<?> chunk = (ChunkHolder<?>) queue.getOrCreateChunk(x, z);
                IChunkGet toSend = chunk.getOrCreateGet();
                TaskManager.IMP.async(() -> {
                    Fawe.imp().getPlatformAdapter().sendChunk(toSend, -1, false);
                });
            }
        }
    }

    @Override
    public void clear() {

    }

    @Override
    public void removeLighting() {

    }

    @Override
    public void fixBlockLighting() {

    }

    @Override
    public void fixSkyLighting() {

    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ReentrantLock getLock() {
        return this.lock;
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public void close() throws Exception {

    }
}
