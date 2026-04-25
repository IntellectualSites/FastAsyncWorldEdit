package com.fastasyncworldedit.nukkit.adapter.mot;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.GlobalBlockPalette;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.format.leveldb.BlockStateMapping;
import cn.nukkit.level.format.leveldb.NukkitLegacyMapper;
import cn.nukkit.level.format.leveldb.structure.BlockStateSnapshot;
import cn.nukkit.utils.Identifier;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplAdapter;
import com.fastasyncworldedit.nukkit.adapter.NukkitPlatformCapabilities;
import org.cloudburstmc.nbt.NbtMap;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter implementation for the Nukkit-MOT platform.
 * <p>
 * MOT (Memory-Optimized-Terrain) extends Nukkit with modern Bedrock features
 * and additional APIs. This adapter compiles against MOT-specific classes
 * such as {@code cn.nukkit.GameVersion} and {@code Identifier}. It is loaded
 * at runtime by {@link NukkitImplLoader} only when MOT is detected.
 * <p>
 * MOT uses 13-bit block data (vs NKX's 6-bit) and accepts int layer indices
 * for multi-layer block access (waterlogging). All entities have UUIDs.
 * <p>
 * Key differences from NKX:
 * <ul>
 *   <li>Uses int layer parameter (0=normal, 1=waterlogged); NKX uses BlockLayer enum</li>
 *   <li>Player language returned as enum name; NKX returns Locale string</li>
 * </ul>
 *
 * @see com.fastasyncworldedit.nukkit.adapter.nkx.NkxNukkitAdapter
 * @see NukkitImplLoader
 */
public class MotNukkitAdapter implements NukkitImplAdapter {

    private static final Set<NukkitPlatformCapabilities> CAPABILITIES = Set.of();

    @Override
    public String getPlatformName() {
        return "Nukkit-MOT";
    }

    @Override
    public String getVersion() {
        return NukkitImplAdapter.detectServerVersion(getPlatformName());
    }

    @Override
    public Set<NukkitPlatformCapabilities> getCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public int getBlockDataBits() {
        return Block.DATA_BITS;
    }

    @Override
    @Nullable
    public String getPlayerLanguageCode(Player player) {
        var langCode = player.getLanguageCode();
        return langCode != null ? langCode.name() : null;
    }

    @Override
    public int getBlockRuntimeId(Player player, int blockId, int meta) {
        return GlobalBlockPalette.getOrCreateRuntimeId(player.getGameVersion(), blockId, meta);
    }

    @Override
    @Nullable
    public String getEntityIdentifier(Entity entity) {
        Identifier identifier = entity.getIdentifier();
        return identifier != null ? identifier.toString() : null;
    }

    @Override
    public List<NbtMap> loadBlockPalette() {
        return NukkitLegacyMapper.loadBlockPalette();
    }

    @Override
    @Nullable
    public BlockStateSnapshot getBlockStateSnapshot(NbtMap nbtState) {
        return BlockStateMapping.get().getStateUnsafe(nbtState);
    }

    @Override
    public int getBlockId(FullChunk chunk, int x, int y, int z, int layer) {
        return chunk.getBlockId(x, y, z, layer);
    }

    @Override
    public void setFullBlockId(FullChunk chunk, int x, int y, int z, int layer, int fullId) {
        chunk.setFullBlockId(x, y, z, layer, fullId);
    }

    @Override
    public UUID getEntityUUID(Entity entity) {
        return entity.getUniqueId();
    }

}
