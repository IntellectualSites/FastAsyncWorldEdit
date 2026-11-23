package com.fastasyncworldedit.nukkit.adapter.mot;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.entity.Entity;
import cn.nukkit.item.Item;
import cn.nukkit.level.GlobalBlockPalette;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.format.leveldb.BlockStateMapping;
import cn.nukkit.level.format.leveldb.NukkitLegacyMapper;
import cn.nukkit.level.format.leveldb.structure.BlockStateSnapshot;
import cn.nukkit.level.generator.object.tree.ObjectCherryTree;
import cn.nukkit.level.generator.object.tree.ObjectCrimsonTree;
import cn.nukkit.level.generator.object.tree.ObjectDarkOakTree;
import cn.nukkit.level.generator.object.tree.ObjectMangroveTree;
import cn.nukkit.level.generator.object.tree.ObjectNetherTree;
import cn.nukkit.level.generator.object.tree.ObjectPaleOakTree;
import cn.nukkit.level.generator.object.tree.ObjectSavannaTree;
import cn.nukkit.level.generator.object.tree.ObjectSwampTree;
import cn.nukkit.level.generator.object.tree.ObjectTree;
import cn.nukkit.level.generator.object.tree.ObjectWarpedTree;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.Identifier;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplAdapter;
import com.fastasyncworldedit.nukkit.adapter.NukkitPlatformCapabilities;
import com.fastasyncworldedit.nukkit.mapping.ItemMapping.NukkitItemData;
import com.fastasyncworldedit.nukkit.mapping.NukkitBlockData;
import com.fastasyncworldedit.nukkit.util.NukkitTreeTypes.NukkitTreeKind;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import org.apache.logging.log4j.Logger;
import org.cloudburstmc.nbt.NbtMap;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
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
 * @see com.fastasyncworldedit.nukkit.adapter.nkx.NukkitAdapter
 * @see NukkitImplLoader
 */
public class NukkitMOTAdapter implements NukkitImplAdapter {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private static final Set<NukkitPlatformCapabilities> CAPABILITIES = Set.of(
            NukkitPlatformCapabilities.THREE_DIMENSIONAL_BIOMES
    );

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
    public int getBlockRuntimeId(Player player, int fullId) {
        int blockId = fullId >> getBlockDataBits();
        int meta = fullId & getBlockDataMask();
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
    public NukkitBlockData createBlockData(NbtMap nbtState) {
        BlockStateSnapshot snapshot = BlockStateMapping.get().getStateUnsafe(nbtState);
        if (snapshot == null) {
            return null;
        }
        int legacyId = snapshot.getLegacyId();
        if (legacyId == -1) {
            return null;
        }
        return NukkitBlockData.legacy(legacyId, snapshot.getLegacyData());
    }

    @Override
    public int getFullBlockId(Object chunk, int x, int y, int z, int layer) {
        return ((FullChunk) chunk).getFullBlock(x, y, z, layer);
    }

    @Override
    public void setFullBlockId(Object chunk, int x, int y, int z, int layer, int fullId) {
        ((FullChunk) chunk).setFullBlockId(x, y, z, layer, fullId);
    }

    @Override
    public Object getChunk(Level level, int chunkX, int chunkZ) {
        return level.getChunk(chunkX, chunkZ, true);
    }

    @Override
    public int getChunkBiomeId(Object chunk, int x, int y, int z) {
        return ((FullChunk) chunk).getBiomeId(x, y, z);
    }

    @Override
    public void setChunkBiomeId(Object chunk, int x, int y, int z, int biomeId) {
        ((FullChunk) chunk).setBiomeId(x, y, z, biomeId);
    }

    @Override
    public int getLevelBiomeId(Level level, int x, int y, int z) {
        FullChunk chunk = level.getChunk(x >> 4, z >> 4, true);
        return chunk.getBiomeId(x & 0xF, y, z & 0xF);
    }

    @Override
    public void setLevelBiomeId(Level level, int x, int y, int z, int biomeId) {
        FullChunk chunk = level.getChunk(x >> 4, z >> 4, true);
        chunk.setBiomeId(x & 0xF, y, z & 0xF, biomeId);
    }

    @Override
    public Map<Long, BlockEntity> getBlockEntities(Object chunk) {
        return ((FullChunk) chunk).getBlockEntities();
    }

    @Override
    @Nullable
    public BlockEntity getTile(Object chunk, int x, int y, int z) {
        return ((FullChunk) chunk).getTile(x, y, z);
    }

    @Override
    public int getBlockSkyLight(Object chunk, int x, int y, int z) {
        return ((FullChunk) chunk).getBlockSkyLight(x, y, z);
    }

    @Override
    public int getBlockLight(Object chunk, int x, int y, int z) {
        return ((FullChunk) chunk).getBlockLight(x, y, z);
    }

    @Override
    public void setChunkChanged(Object chunk, boolean changed) {
        ((FullChunk) chunk).setChanged(changed);
    }

    @Override
    @Nullable
    public BlockEntity createBlockEntity(String id, Object chunk, CompoundTag nbt) {
        return BlockEntity.createBlockEntity(id, (FullChunk) chunk, nbt);
    }

    @Override
    @Nullable
    public Entity createEntity(String id, Object chunk, CompoundTag nbt) {
        return Entity.createEntity(id, (FullChunk) chunk, nbt);
    }

    @Override
    public Map<Long, Entity> getChunkEntities(Level level, int chunkX, int chunkZ) {
        return level.getChunkEntities(chunkX, chunkZ);
    }

    @Override
    public int getFullBlockId(Block block) {
        return block.getFullId();
    }

    @Override
    public Block getBlock(int fullId) {
        int blockId = fullId >> getBlockDataBits();
        int meta = fullId & getBlockDataMask();
        return Block.get(blockId, meta);
    }

    @Override
    public boolean isWaterFullId(int fullId) {
        int blockId = fullId >> getBlockDataBits();
        return blockId == 8 || blockId == 9;
    }

    @Override
    public int getAirFullId() {
        return 0;
    }

    @Override
    public int getStillWaterFullId() {
        return 9 << getBlockDataBits();
    }

    @Override
    @Nullable
    public NukkitItemData createItemData(String bedrockId, int metadata) {
        Item item = Item.fromString(bedrockId);
        if (item == null || item.isNull()) {
            return null;
        }
        return new NukkitItemData(bedrockId, item.getId(), item.getDamage());
    }

    @Override
    public String getItemMappingKey(Item item) {
        return item.getNamespaceId();
    }

    @Override
    public String getItemMappingKey(NukkitItemData data) {
        return data.identifier();
    }

    @Override
    public Item getItem(NukkitItemData data, int amount) {
        return Item.get(data.itemId(), data.metadata(), amount);
    }

    @Override
    public Item getAirItem() {
        return Item.get(Item.AIR);
    }

    @Override
    public boolean matchesItem(Item item, NukkitItemData data) {
        return item.getId() == data.itemId() && item.getDamage() == data.metadata();
    }

    @Override
    public boolean isAirItem(Item item) {
        return item.getId() == Item.AIR || item.isNull();
    }

    @Override
    public UUID getEntityUUID(Entity entity) {
        return entity.getUniqueId();
    }

    @Override
    public void recalculateLight(Object chunk) {
        FullChunk c = (FullChunk) chunk;
        c.recalculateHeightMap();
        c.populateSkyLight();
        c.populateBlockLight();
    }

    @Override
    public boolean supportsTree(NukkitTreeKind kind) {
        // ObjectAzaleaTree only exists on NKX; MOT has no azalea generator.
        return kind != NukkitTreeKind.AZALEA;
    }

    @Override
    public boolean growTree(Level level, NukkitTreeKind kind, int x, int y, int z) {
        cn.nukkit.math.NukkitRandom random = new cn.nukkit.math.NukkitRandom();
        try {
            int code = kind.getLegacyCode();
            if (code >= 0) {
                ObjectTree.growTree(level, x, y, z, random, code);
                return true;
            }
            return switch (kind) {
                // MOT's dark oak/acacia/swamp/cherry/mangrove/pale-oak generators extend
                // BasicGenerator, so the entry point is generate(ChunkManager, NukkitRandom, Vector3).
                case DARK_OAK -> new ObjectDarkOakTree().generate(level, random, new Vector3(x, y, z));
                case ACACIA -> new ObjectSavannaTree().generate(level, random, new Vector3(x, y, z));
                case SWAMP -> new ObjectSwampTree().generate(level, random, new Vector3(x, y, z));
                case CHERRY -> new ObjectCherryTree().generate(level, random, new Vector3(x, y, z));
                case MANGROVE -> new ObjectMangroveTree().generate(level, random, new Vector3(x, y, z));
                case PALE_OAK -> new ObjectPaleOakTree().generate(level, random, new Vector3(x, y, z));
                case CRIMSON, WARPED -> placeNetherTree(
                        kind == NukkitTreeKind.CRIMSON ? new ObjectCrimsonTree() : new ObjectWarpedTree(),
                        level, x, y, z, random
                );
                default -> throw new UnsupportedOperationException(
                        "Nukkit-MOT provides no generator for tree kind " + kind
                );
            };
        } catch (UnsupportedOperationException e) {
            // Unsupported kinds must propagate (contract of NukkitImplAdapter#growTree), not be
            // swallowed into a generic placement failure.
            throw e;
        } catch (Exception e) {
            LOGGER.warn("Failed to place {} tree on Nukkit-MOT at {},{},{}", kind, x, y, z, e);
            return false;
        }
    }

    /**
     * Crimson/warped "fungi" generators extend {@code ObjectNetherTree} (an {@code ObjectTree}),
     * so mirror {@code ObjectTree.growTree}: check placement first, then write — the placement
     * check is not silent here because the result is returned to the caller.
     */
    private static boolean placeNetherTree(
            ObjectNetherTree tree, Level level, int x, int y, int z, cn.nukkit.math.NukkitRandom random
    ) {
        if (!tree.canPlaceObject(level, x, y, z, random)) {
            return false;
        }
        tree.placeObject(level, x, y, z, random);
        return true;
    }

}
