package com.boydti.fawe.bukkit.adapter.mc1_16_5;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IQueueChunk;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.implementation.chunk.ChunkHolder;
import com.boydti.fawe.beta.implementation.lighting.NMSRelighter;
import com.boydti.fawe.beta.implementation.lighting.Relighter;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.v1_16_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R3.ChunkStatus;
import net.minecraft.server.v1_16_R3.LightEngineThreaded;
import net.minecraft.server.v1_16_R3.MCUtil;
import net.minecraft.server.v1_16_R3.TicketType;
import net.minecraft.server.v1_16_R3.Unit;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class TuinityRelighter_1_16_5 implements Relighter {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private static final MethodHandle relight;

    private static final int CHUNKS_PER_BATCH = 1024; // 32 * 32
    private static final int CHUNKS_PER_BATCH_SQRT_LOG2 = 5; // for shifting

    private static final TicketType<Unit> FAWE_TICKET = TicketType.a("fawe_ticket", (a, b) -> 0);
    private static final int LIGHT_LEVEL = MCUtil.getTicketLevelFor(ChunkStatus.LIGHT);

    private final WorldServer world;
    private final ReentrantLock lock = new ReentrantLock();

    private final IQueueExtent<IQueueChunk> queue;

    private final Long2ObjectLinkedOpenHashMap<LongSet> regions = new Long2ObjectLinkedOpenHashMap<>();

    private final ReentrantLock areaLock = new ReentrantLock();
    private final NMSRelighter delegate;

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
        this.delegate = new NMSRelighter(queue, false);
    }

    @Override
    public boolean addChunk(int cx, int cz, byte[] skipReason, int bitmask) {
        areaLock.lock();
        try {
            long key = MathMan.pairInt(cx >> CHUNKS_PER_BATCH_SQRT_LOG2, cz >> CHUNKS_PER_BATCH_SQRT_LOG2);
            // TODO probably submit here already if chunks.size == CHUNKS_PER_BATCH?
            LongSet chunks = this.regions.computeIfAbsent(key, k -> new LongArraySet(CHUNKS_PER_BATCH >> 2));
            chunks.add(ChunkCoordIntPair.pair(cx, cz));
        } finally {
            areaLock.unlock();
        }
        return true;
    }

    @Override
    public void addLightUpdate(int x, int y, int z) {
        delegate.addLightUpdate(x, y, z);
    }

    @Override
    public void fixLightingSafe(boolean sky) {
        this.areaLock.lock();
        try {
            if (regions.isEmpty()) return;
            LongSet first = regions.removeFirst();
            fixLighting(first, () -> fixLightingSafe(true));
        } finally {
            this.areaLock.unlock();
        }
    }

    private void fixLighting(LongSet chunks, Runnable andThen) {
        Set<ChunkCoordIntPair> coords = new HashSet<>();
        LongIterator iterator = chunks.iterator();
        while (iterator.hasNext()) {
            coords.add(new ChunkCoordIntPair(iterator.nextLong()));
        }
        TaskManager.IMP.task(() -> {
            List<CompletableFuture<?>> futures = new ArrayList<>();
            for (ChunkCoordIntPair pos : coords) {
                futures.add(world.getWorld().getChunkAtAsync(pos.x, pos.z)
                        .thenAccept(c -> world.getChunkProvider().addTicketAtLevel(FAWE_TICKET, pos, LIGHT_LEVEL, Unit.INSTANCE)));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenAccept(v ->
                    invokeRelight(coords, c -> {
                    }, i -> {
                        if (i != coords.size()) {
                            LOGGER.warn("Processed " + i + " chunks instead of " + coords.size());
                        }
                        TaskManager.IMP.task(() -> postProcessChunks(coords));
                        TaskManager.IMP.laterAsync(andThen, 0);
                    })
            );
        });
    }

    private void invokeRelight(Set<ChunkCoordIntPair> coords,
                               Consumer<ChunkCoordIntPair> chunkCallback,
                               IntConsumer processCallback) {
        try {
            // cast is required for invokeExact
            int unused = (int) relight.invokeExact(world.getChunkProvider().getLightEngine(),
                    coords,
                    chunkCallback, // callback per chunk
                    processCallback // callback for all chunks
            );
        } catch (Throwable throwable) {
            LOGGER.error("Error occurred on relighting", throwable);
        }
    }

    private void postProcessChunks(Set<ChunkCoordIntPair> coords) {
        boolean delay = Settings.IMP.LIGHTING.DELAY_PACKET_SENDING;
        for (ChunkCoordIntPair pos : coords) {
            int x = pos.x;
            int z = pos.z;
            if (delay) { // we still need to send the block changes of that chunk
                BukkitAdapter_1_16_5.sendChunk(world, x, z, -1, false);
            }
            world.getChunkProvider().removeTicketAtLevel(FAWE_TICKET, pos, LIGHT_LEVEL, Unit.INSTANCE);
        }
    }

    @Override
    public void clear() {

    }

    @Override
    public void removeLighting() {
        this.delegate.removeLighting();
    }

    @Override
    public void fixBlockLighting() {
        fixLightingSafe(true);
    }

    @Override
    public void fixSkyLighting() {
        fixLightingSafe(true);
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
        fixLightingSafe(true);
    }
}
