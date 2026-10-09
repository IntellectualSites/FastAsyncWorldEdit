package com.fastasyncworldedit.nukkit;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.implementation.packet.ChunkPacket;
import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NukkitUnsupportedFeatureTest {

    @Test
    void platformAdapterSendChunkIsGracefulNoOp() {
        // sendChunk is a no-op: chunk resends are driven by the edit flush path, not by FAWE's
        // packet layer, so this must not throw or interact with the chunk.
        NukkitPlatformAdapter adapter = new NukkitPlatformAdapter();
        IChunkGet chunk = mock(IChunkGet.class);

        adapter.sendChunk(chunk, 0xFFFF, true);

        verifyNoInteractions(chunk);
    }

    @Test
    void worldSendFakeChunkIsGracefulNoOp() {
        // Fake chunk packets are unsupported on Nukkit but must fail silently rather than throw,
        // so callers (WorldWrapper) remain functional.
        Level level = mock(Level.class);
        when(level.getName()).thenReturn("world");
        NukkitWorld world = new NukkitWorld(level);
        com.sk89q.worldedit.entity.Player player = mock(com.sk89q.worldedit.entity.Player.class);
        ChunkPacket packet = mock(ChunkPacket.class);

        world.sendFakeChunk(player, packet);

        verifyNoInteractions(player, packet);
    }

    @Test
    void playerDispatchCuiEventThrowsUnsupportedOperation() {
        Player player = mock(Player.class);
        NukkitPlayer nukkitPlayer = new NukkitPlayer(player);

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> nukkitPlayer.dispatchCUIEvent(null)
        );
        assertTrue(
                exception.getMessage().contains("WorldEdit CUI (Client User Interface) protocol is not supported on Nukkit")
        );
        verifyNoInteractions(player);
    }

    @Test
    void relighterIsSafeNoOpWithoutLevel() {
        // Without a live Level the relighter cannot resolve chunks and stays a safe no-op.
        NukkitRelighter relighter = new NukkitRelighter(null);
        ReentrantLock lock = relighter.getLock();

        assertFalse(relighter.addChunk(1, 2, new byte[0], 0xFFFF));
        assertTrue(relighter.isEmpty());
        assertTrue(relighter.isFinished());
        assertSame(lock, relighter.getLock());
        relighter.addLightUpdate(1, 64, 2);
        relighter.fixLightingSafe(true);
        relighter.fixBlockLighting();
        relighter.fixSkyLighting();
        relighter.removeLighting();
        relighter.clear();
        relighter.close();
    }
}
