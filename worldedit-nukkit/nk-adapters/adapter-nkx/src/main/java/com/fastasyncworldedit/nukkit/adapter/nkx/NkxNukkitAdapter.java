package com.fastasyncworldedit.nukkit.adapter.nkx;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockLayer;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.level.GlobalBlockPalette;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.format.leveldb.BlockStateMapping;
import cn.nukkit.level.format.leveldb.NukkitLegacyMapper;
import cn.nukkit.level.format.leveldb.structure.BlockStateSnapshot;
import cn.nukkit.level.generator.object.tree.ObjectCherryTree;
import cn.nukkit.level.generator.object.tree.ObjectMangroveTree;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.math.Vector3;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplAdapter;
import com.fastasyncworldedit.nukkit.adapter.NukkitPlatformCapabilities;
import org.cloudburstmc.nbt.NbtMap;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter implementation for the NKX (upstream Nukkit) platform.
 * <p>
 * NKX is the original/community Nukkit fork with a smaller API surface
 * than MOT. This adapter compiles against NKX-specific classes such as
 * {@code cn.nukkit.block.BlockLayer}. It is loaded at runtime by
 * {@link NukkitImplLoader} when MOT is not detected.
 * <p>
 * NKX uses 6-bit block data (vs MOT's 13-bit) and requires BlockLayer enum
 * values for multi-layer access. Only EntityHuman (players) have UUIDs;
 * other entities must derive synthetic UUIDs from their network ID.
 * <p>
 * Key differences from MOT:
 * <ul>
 *   <li>Does not support PaleOak trees; MOT does</li>
 *   <li>Uses BlockLayer enum for layer access; MOT uses int</li>
 *   <li>Limited capabilities reported; MOT advertises ALL_TREE_TYPES</li>
 * </ul>
 *
 * @see com.fastasyncworldedit.nukkit.adapter.mot.MotNukkitAdapter
 * @see NukkitImplLoader
 */
public class NkxNukkitAdapter implements NukkitImplAdapter {

    private static final BlockLayer[] LAYERS = {BlockLayer.NORMAL, BlockLayer.WATERLOGGED};
    private static final Set<NukkitPlatformCapabilities> CAPABILITIES = Collections.emptySet();

    @Override
    public String getPlatformName() {
        return "NKX";
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
        Locale locale = player.getLocale();
        if (locale == null) {
            return null;
        }
        return locale.toString();
    }

    @Override
    public int getBlockRuntimeId(Player player, int blockId, int meta) {
        return GlobalBlockPalette.getOrCreateRuntimeId(blockId, meta);
    }

    @Override
    @Nullable
    public String getEntityIdentifier(Entity entity) {
        int networkId = entity.getNetworkId();
        if (networkId == -1) {
            return null;
        }
        String saveId = entity.getSaveId();
        if (saveId != null && !saveId.isEmpty()) {
            return saveId.contains(":") ? saveId : "minecraft:" + saveId;
        }
        return null;
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
    public boolean generateTree(String treeType, Level level, int x, int y, int z, NukkitRandom random, Vector3 pos) {
        // NKX: Mangrove and Cherry extend ObjectTree; PaleOak does not exist
        switch (treeType) {
            case "MANGROVE" -> new ObjectMangroveTree().placeObject(level, x, y, z, random);
            case "CHERRY" -> new ObjectCherryTree().placeObject(level, x, y, z, random);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getBlockId(FullChunk chunk, int x, int y, int z, int layer) {
        return chunk.getBlockId(x, y, z, LAYERS[layer]);
    }

    @Override
    public void setFullBlockId(FullChunk chunk, int x, int y, int z, int layer, int fullId) {
        chunk.setFullBlockId(x, y, z, LAYERS[layer], fullId);
    }

    @Override
    public UUID getEntityUUID(Entity entity) {
        // NKX: only EntityHuman (Player) has getUniqueId(); for other entities derive from getId()
        if (entity instanceof EntityHuman human) {
            return human.getUniqueId();
        }
        return new UUID(0, entity.getId());
    }

}
