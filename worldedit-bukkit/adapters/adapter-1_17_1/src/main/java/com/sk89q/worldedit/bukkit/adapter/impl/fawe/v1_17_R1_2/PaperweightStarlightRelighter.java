package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_17_R1_2;

import com.fastasyncworldedit.bukkit.adapter.StarlightRelighter;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import net.minecraft.server.MCUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Unit;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class PaperweightStarlightRelighter extends StarlightRelighter<ServerLevel, ChunkPos> {

    private static final MethodHandle RELIGHT;
    private static final TicketType<Unit> FAWE_TICKET = TicketType.create("fawe_ticket", (a, b) -> 0);
    private static final int LIGHT_LEVEL = MCUtil.getTicketLevelFor(ChunkStatus.LIGHT);

    static {
        MethodHandle tmp = null;
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            tmp = lookup.findVirtual(
                    ThreadedLevelLightEngine.class,
                    "relight",
                    MethodType.methodType(
                            int.class, // return type
                            // params
                            Set.class,
                            Consumer.class,
                            IntConsumer.class
                    )
            );
            tmp = MethodHandles.dropReturn(tmp);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            LOGGER.error("Failed to locate 'relight' method in ThreadedLevelLightEngine. Is everything up to date?", e);
        }
        RELIGHT = tmp;
    }

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

    public static boolean isUsable() {
        return RELIGHT != null;
    }

    @Override
    protected void invokeRelight(
            Set<ChunkPos> coords,
            Consumer<ChunkPos> chunkCallback,
            IntConsumer processCallback
    ) {
        try {
            RELIGHT.invokeExact(
                    serverLevel.getChunkSource().getLightEngine(),
                    coords,
                    chunkCallback, // callback per chunk
                    processCallback // callback for all chunks
            );
        } catch (Throwable throwable) {
            LOGGER.error("Error occurred on relighting", throwable);
        }
    }

    /*
     * Allow the server to unload the chunks again.
     * Also, if chunk packets are sent delayed, we need to do that here
     */
    protected void postProcessChunks(Set<ChunkPos> coords) {
        boolean delay = Settings.settings().LIGHTING.DELAY_PACKET_SENDING;
        for (ChunkPos pos : coords) {
            int x = pos.x;
            int z = pos.z;
            if (delay) { // we still need to send the block changes of that chunk
                PaperweightPlatformAdapter.sendChunk(serverLevel, x, z, false);
            }
            serverLevel.getChunkSource().removeTicketAtLevel(FAWE_TICKET, pos, LIGHT_LEVEL, Unit.INSTANCE);
        }
    }

}
