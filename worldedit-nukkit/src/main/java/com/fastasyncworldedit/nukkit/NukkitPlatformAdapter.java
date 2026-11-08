package com.fastasyncworldedit.nukkit;

import com.fastasyncworldedit.core.FAWEPlatformAdapterImpl;
import com.fastasyncworldedit.core.queue.IChunkGet;

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
 * {@link #sendChunk} is therefore a no-op: chunk resends after an edit are driven by
 * {@link NukkitWorld#refreshChunk}, which the edit flush path already invokes per-touched
 * chunk via {@code level.requestChunk}. Re-sending through FAWE's packet layer would only
 * duplicate that work, so this method intentionally does nothing rather than throwing.
 * <p>
 * Key differences from Bukkit:
 * <ul>
 *   <li>Bukkit FAWE can serialize and send chunk packets directly; Nukkit cannot</li>
 *   <li>Nukkit delegates chunk packet sending to the level implementation</li>
 *   <li>Fake chunk packets for clipboard visualization are unsupported</li>
 * </ul>
 *
 * @see com.fastasyncworldedit.core.FAWEPlatformAdapterImpl
 * @see NukkitWorld#refreshChunk
 */
public class NukkitPlatformAdapter implements FAWEPlatformAdapterImpl {

    @Override
    public void sendChunk(IChunkGet chunk, int mask, boolean lighting) {
        // Intentional no-op. The edit flush path (NukkitGetBlocks) already resends each
        // touched chunk to its viewers via level.requestChunk, so the FAWE relighter-driven
        // re-send here would be redundant. Constructing a Bedrock LevelChunkPacket from FAWE's
        // Java-internal chunk representation is not feasible without full palette serialization.
    }

}
