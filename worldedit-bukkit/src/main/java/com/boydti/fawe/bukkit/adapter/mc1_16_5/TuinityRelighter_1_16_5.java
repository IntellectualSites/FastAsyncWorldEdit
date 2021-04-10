package com.boydti.fawe.bukkit.adapter.mc1_16_5;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.IQueueChunk;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.implementation.chunk.ChunkHolder;
import com.boydti.fawe.beta.implementation.lighting.Relighter;
import com.boydti.fawe.util.TaskManager;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R3.MCUtil;
import net.minecraft.server.v1_16_R3.WorldServer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class TuinityRelighter_1_16_5 implements Relighter {

    private static final IntConsumer nothingIntConsumer = i -> {};

    private final WorldServer world;
    private final MethodHandle relight;
    private final ReentrantLock lock = new ReentrantLock();

    private final IQueueExtent<IQueueChunk> queue;

    public TuinityRelighter_1_16_5(WorldServer world, IQueueExtent<IQueueChunk> queue) {
        this.queue = queue;
        MethodHandle methodHandle = null;
        try {
            Method relightMethod = world.getChunkProvider().getLightEngine().getClass().getMethod(
                    "relight",
                    Set.class,
                    Consumer.class,
                    IntConsumer.class
                    );
            methodHandle = MethodHandles.lookup().unreflect(relightMethod);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
        }
        this.relight = methodHandle;
        this.world = world;
    }

    @Override
    public boolean addChunk(int cx, int cz, byte[] skipReason, int bitmask) {
        List<ChunkCoordIntPair> chunks = MCUtil.getSpiralOutChunks(new BlockPosition(cx << 4, 0, cz << 4), 1);
        TaskManager.IMP.task(() ->
                {
                    try {
                        relight.invoke(world.getChunkProvider().getLightEngine(),
                                new HashSet<>(chunks),
                                (Consumer<?>) coord -> sendChunk(cx, cz), // send chunk after lighting was done
                                nothingIntConsumer
                                );
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }
        );
        return true;
    }

    private void sendChunk(int cx, int cz) {
        ChunkHolder<?> chunk = (ChunkHolder<?>) queue.getOrCreateChunk(cx, cz);
        Fawe.imp().getPlatformAdapter().sendChunk(chunk.getOrCreateGet(), -1, false);
    }

    @Override
    public void addLightUpdate(int x, int y, int z) {

    }

    @Override
    public void fixLightingSafe(boolean sky) {

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
        return true;
    }

    @Override
    public void close() throws Exception {

    }
}
