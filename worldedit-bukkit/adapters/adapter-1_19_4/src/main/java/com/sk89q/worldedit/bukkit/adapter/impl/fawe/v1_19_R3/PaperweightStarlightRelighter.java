package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_19_R3;

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
    protected CompletableFuture<?> chunkLoadFuture(final ChunkPos chunkPos) {
        return serverLevel.getWorld().getChunkAtAsync(chunkPos.x, chunkPos.z)
                .thenAccept(c -> serverLevel.getChunkSource().addTicketAtLevel(
                        FAWE_TICKET,
                        chunkPos,
                        LIGHT_LEVEL,
                        Unit.INSTANCE
                ));
    }

    protected void invokeRelight(
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
