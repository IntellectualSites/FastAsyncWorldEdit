package com.fastasyncworldedit.nukkit;

import com.fastasyncworldedit.core.FAWEPlatformAdapterImpl;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplLoader;
import com.fastasyncworldedit.nukkit.adapter.NukkitPlatformCapabilities;

/**
 * Nukkit platform adapter for FAWE chunk sending.
 * <p>
 * Nukkit handles chunk packet serialization and sending internally via
 * {@code level.requestChunk()}. Unlike Bukkit, where FAWE can access NMS
 * packet classes to send custom chunk data (e.g. for clipboard previews
 * or fast chunk updates), Nukkit does not expose the necessary APIs for
 * direct packet construction. The Bedrock protocol's LevelChunkPacket
 * format (PalettedBlockStorage, 3D biomes, block state runtime IDs) is
 * complex and not public in Nukkit's API.
 * <p>
 * Consequently, {@link #sendChunk} throws UnsupportedOperationException.
 * Chunk updates are instead triggered through Nukkit's standard chunk
 * refresh mechanism in {@link com.sk89q.worldedit.nukkit.NukkitWorld#refreshChunk}.
 * <p>
 * Key differences from Bukkit:
 * <ul>
 *   <li>Bukkit FAWE can serialize and send chunk packets directly; Nukkit cannot</li>
 *   <li>Nukkit delegates chunk packet sending to the level implementation</li>
 *   <li>Fake chunk packets for clipboard visualization are unsupported</li>
 * </ul>
 *
 * @see com.fastasyncworldedit.core.FAWEPlatformAdapterImpl
 * @see com.sk89q.worldedit.nukkit.NukkitWorld#refreshChunk
 */
public class NukkitPlatformAdapter implements FAWEPlatformAdapterImpl {

    @Override
    public void sendChunk(IChunkGet chunk, int mask, boolean lighting) {
        if (!supports(NukkitPlatformCapabilities.FAKE_CHUNKS)) {
            throw new UnsupportedOperationException(
                    "Explicit chunk packet sending is not supported on Nukkit. "
                            + "Nukkit handles chunk updates internally via level.requestChunk(). "
                            + "This FAWE feature requires direct chunk packet serialization "
                            + "which is not available in the Nukkit API."
            );
        }
    }

    private static boolean supports(NukkitPlatformCapabilities capability) {
        try {
            return NukkitImplLoader.supports(capability);
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

}
