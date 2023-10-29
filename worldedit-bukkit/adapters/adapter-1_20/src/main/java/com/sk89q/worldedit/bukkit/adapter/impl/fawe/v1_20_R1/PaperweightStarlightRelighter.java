package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R1;

import com.fastasyncworldedit.bukkit.adapter.StarlightRelighter;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Unit;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class PaperweightStarlightRelighter extends StarlightRelighter<ServerLevel, ChunkPos> {

    private static final TicketType<Unit> FAWE_TICKET = TicketType.create("fawe_ticket", (a, b) -> 0);
    private static final int LIGHT_LEVEL = ChunkMap.MAX_VIEW_DISTANCE + ChunkStatus.getDistance(ChunkStatus.LIGHT);

    public PaperweightStarlightRelighter(ServerLevel serverLevel, IQueueExtent<?> queue) {
        super(serverLevel, queue);
    }

    @Override
    protected ChunkPos createChunkPos(final long chunkKey) {
        return new ChunkPos(chunkKey);
    }

    @Override
    protected long asLong(final int chunkX, final int chunkZ) {
        return ChunkPos.asLong(chunkX, chunkZ);
    }

    @Override
    public void fixLightingSafe(boolean sky) {
        this.areaLock.lock();
        try {
            if (regions.isEmpty()) {
                return;
            }
            LongSet first = regions.removeFirst();
            fixLighting(first, () -> fixLightingSafe(true));
        } finally {
            this.areaLock.unlock();
        }
    }

    /*
     * Processes a set of chunks and runs an action afterwards.
     * The action is run async, the chunks are partly processed on the main thread
     * (as required by the server).
     */
    private void fixLighting(LongSet chunks, Runnable andThen) {
        // convert from long keys to ChunkPos
        Set<ChunkPos> coords = new HashSet<>();
        LongIterator iterator = chunks.iterator();
        while (iterator.hasNext()) {
            coords.add(new ChunkPos(iterator.nextLong()));
        }
        if (FoliaSupport.isFolia()) {
            relightRegion(andThen, coords);
            return;
        }
        TaskManager.taskManager().task(() -> {
            // trigger chunk load and apply ticket on main thread
            relightRegion(andThen, coords);
        });
    }

    private void relightRegion(Runnable andThen, Set<ChunkPos> coords) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (ChunkPos pos : coords) {
            futures.add(serverLevel.getWorld().getChunkAtAsync(pos.x, pos.z)
                    .thenAccept(c -> serverLevel.getChunkSource().addTicketAtLevel(
                            FAWE_TICKET,
                            pos,
                            LIGHT_LEVEL,
                            Unit.INSTANCE
                    ))
            );
        }
        Location location = toLocation(coords.iterator().next());
        // collect futures and trigger relight once all chunks are loaded
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenAcceptAsync(v ->
                invokeRelight(
                        coords,
                        c -> {
                        }, // no callback for single chunks required
                        i -> {
                            if (i != coords.size()) {
                                LOGGER.warn("Processed {} chunks instead of {}", i, coords.size());
                            }
                            // post process chunks on main thread
                            TaskManager.taskManager().task(() -> postProcessChunks(coords), location);
                            // call callback on our own threads
                            TaskManager.taskManager().async(andThen);
                        }
                ),
                task -> TaskManager.taskManager().task(task, location)
        );
    }

    private Location toLocation(ChunkPos chunkPos) {
        return PaperweightPlatformAdapter.toLocation(this.serverLevel, chunkPos);
    }

    private void invokeRelight(
            Set<ChunkPos> coords,
            Consumer<ChunkPos> chunkCallback,
            IntConsumer processCallback
    ) {
        try {
            serverLevel.getChunkSource().getLightEngine().relight(coords, chunkCallback, processCallback);
        } catch (Exception e) {
            LOGGER.error("Error occurred on relighting", e);
        }
    }

    /*
     * Allow the server to unload the chunks again.
     * Also, if chunk packets are sent delayed, we need to do that here
     */
    protected void postProcessChunks(Set<ChunkPos> coords) {
        boolean delay = Settings.settings().LIGHTING.DELAY_PACKET_SENDING;
        for (ChunkPos pos : coords) {
            PaperweightPlatformAdapter.task(
                    () -> {
                        int x = pos.x;
                        int z = pos.z;
                        if (delay) { // we still need to send the block changes of that chunk
                            PaperweightPlatformAdapter.sendChunk(serverLevel, x, z, false);
                        }
                        serverLevel.getChunkSource().removeTicketAtLevel(FAWE_TICKET, pos, LIGHT_LEVEL, Unit.INSTANCE);
                    },
                    serverLevel,
                    pos.x,
                    pos.z
            );

        }
    }

}
