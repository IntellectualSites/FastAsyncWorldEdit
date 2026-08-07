package com.fastasyncworldedit.nukkit.adapter.pnx;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockState;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.entity.Entity;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.IChunk;
import cn.nukkit.math.Vector3f;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.types.inventory.transaction.UseItemData;
import cn.nukkit.registry.Registries;
import cn.nukkit.utils.HashUtils;
import com.fastasyncworldedit.nukkit.adapter.NukkitAdapter;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplAdapter;
import com.fastasyncworldedit.nukkit.adapter.NukkitPlatformCapabilities;
import com.fastasyncworldedit.nukkit.mapping.ItemMapping.NukkitItemData;
import com.fastasyncworldedit.nukkit.mapping.NukkitBlockData;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Direction;
import org.cloudburstmc.nbt.NbtMap;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter implementation for PowerNukkitX.
 * <p>
 * PNX keeps the {@code cn.nukkit} package but has moved chunks and blocks to a
 * {@code BlockState} based API. Unlike the legacy Nukkit forks (MOT/NKX) which
 * use {@code blockId + metadata} packed into a full id, PNX identifies block
 * states solely by a stable FNV-1a hash ({@code BlockState.blockStateHash()}).
 * <p>
 * <b>Full-id namespace.</b> This adapter uses the {@code blockStateHash} as the
 * unified "full id" everywhere internally: chunk reads/writes
 * ({@link #getFullBlockId}, {@link #setFullBlockId}), mapping tables
 * ({@link #createBlockData}), and the air/water constants. The Bedrock network
 * layer, however, uses a different per-level runtime id, so
 * {@link #getBlockRuntimeId} translates a hash to the network runtime id via
 * {@code Block.get(state).getRuntimeId()}. Mixing these two namespaces (as the
 * previous implementation did) silently breaks waterlogging detection, fake
 * block sends, and air writes.
 * <p>
 * This adapter is compiled in its own module against the PNX API, then loaded
 * reflectively by the common Nukkit loader at runtime.
 *
 * @see NukkitImplLoader
 */
public class PowerNukkitXAdapter implements NukkitImplAdapter {

    private static final Set<NukkitPlatformCapabilities> CAPABILITIES = Set.of(
            NukkitPlatformCapabilities.THREE_DIMENSIONAL_BIOMES
    );

    /**
     * Cached hash for {@code minecraft:air}. Lazily computed because the PNX
     * block registry may not be initialised when this adapter is constructed
     * during platform detection (see {@link NukkitImplLoader#detect()}).
     */
    private volatile int airFullId = -1;
    private volatile int waterFullId = -1;
    private volatile int flowingWaterFullId = -1;

    @Override
    public String getPlatformName() {
        return "PowerNukkitX";
    }

    @Override
    public String getVersion() {
        return NukkitImplAdapter.detectServerVersion(getPlatformName());
    }

    @Override
    public Set<NukkitPlatformCapabilities> getCapabilities() {
        return CAPABILITIES;
    }

    /**
     * PNX does not pack metadata into block ids; the hash already encodes the
     * full state. Returning {@code 0} keeps the {@code (1 << bits) - 1} mask a
     * no-op so that legacy-style arithmetic in {@code NukkitBlockData} is a
     * passthrough for the hash value.
     */
    @Override
    public int getBlockDataBits() {
        return 0;
    }

    @Override
    @Nullable
    public String getPlayerLanguageCode(Player player) {
        return player.getLocale().toString();
    }

    /**
     * Convert a block-state hash (this adapter's full id) to the Bedrock network
     * runtime id required by {@code UpdateBlockPacket}.
     */
    @Override
    public int getBlockRuntimeId(Player player, int fullId) {
        return Block.get(resolveBlockState(fullId)).getRuntimeId();
    }

    @Override
    @Nullable
    public String getEntityIdentifier(Entity entity) {
        String identifier = entity.getIdentifier();
        return identifier != null ? identifier : null;
    }

    /**
     * Return the PNX block-state version so {@code BlockMapping} can record it.
     * <p>
     * Unlike the legacy Nukkit forks, PNX does not expose the vanilla palette as
     * a list of {@link NbtMap} entries: block states live in
     * {@link Registries#BLOCKSTATE}, keyed by hash, and
     * {@link #createBlockData} computes those hashes from {@code name + states}
     * (the {@code version} field does not participate in the hash). So the
     * palette contents are not required for mapping; only the version number is
     * read by {@code BlockMapping}, and only for logging.
     * <p>
     * We therefore return a single synthetic entry carrying the version parsed
     * from the embedded {@code block_palette.nbt} resource, or an empty list if
     * the resource is unavailable. Mapping still works via
     * {@link #createBlockData} when this returns empty.
     */
    @Override
    public List<NbtMap> loadBlockPalette() {
        try (var stream = Registries.class.getClassLoader()
                .getResourceAsStream("gamedata/kaooot/block_palette.nbt")) {
            if (stream == null) {
                return List.of();
            }
            // The resource is gzip-compressed big-endian NBT, matching how PNX's own
            // BlockStateRegistry reads it (NBTIO.readCompressed defaults to BIG_ENDIAN).
            // We only need the integer "version" from any palette entry, so read the
            // root compound and sample its first child.
            cn.nukkit.nbt.tag.CompoundTag root = cn.nukkit.nbt.NBTIO.readCompressed(
                    stream.readAllBytes(), java.nio.ByteOrder.BIG_ENDIAN);
            cn.nukkit.nbt.tag.ListTag<?> blocks = root.getList("blocks", cn.nukkit.nbt.tag.CompoundTag.class);
            if (blocks.size() == 0) {
                return List.of();
            }
            Object first = blocks.get(0);
            if (!(first instanceof cn.nukkit.nbt.tag.CompoundTag entry)) {
                return List.of();
            }
            int version = entry.getInt("version");
            return List.of(NbtMap.builder()
                    .putString("name", "minecraft:air")
                    .putCompound("states", NbtMap.builder().build())
                    .putInt("version", version)
                    .build());
        } catch (Exception ignored) {
            // Version is non-essential; createBlockData hashing still works.
            return List.of();
        }
    }

    @Override
    @Nullable
    public NukkitBlockData createBlockData(NbtMap nbtState) {
        int fullId = blockStateHash(nbtState);
        return Registries.BLOCKSTATE.get(fullId) != null ? NukkitBlockData.fullId(fullId) : null;
    }

    @Override
    public int getFullBlockId(Object chunk, int x, int y, int z, int layer) {
        return ((IChunk) chunk).getBlockState(x, y, z, layer).blockStateHash();
    }

    @Override
    public void setFullBlockId(Object chunk, int x, int y, int z, int layer, int fullId) {
        ((IChunk) chunk).setBlockState(x, y, z, resolveBlockState(fullId), layer);
    }

    @Override
    public Object getChunk(Level level, int chunkX, int chunkZ) {
        return level.getChunk(chunkX, chunkZ, true);
    }

    @Override
    public int getChunkBiomeId(Object chunk, int x, int y, int z) {
        return ((IChunk) chunk).getBiomeId(x, y, z);
    }

    @Override
    public void setChunkBiomeId(Object chunk, int x, int y, int z, int biomeId) {
        ((IChunk) chunk).setBiomeId(x, y, z, biomeId);
    }

    @Override
    public int getLevelBiomeId(Level level, int x, int y, int z) {
        return level.getBiomeId(x, y, z);
    }

    @Override
    public void setLevelBiomeId(Level level, int x, int y, int z, int biomeId) {
        level.setBiomeId(x, y, z, biomeId);
    }

    @Override
    public Map<Long, BlockEntity> getBlockEntities(Object chunk) {
        return ((IChunk) chunk).getBlockEntities();
    }

    @Override
    @Nullable
    public BlockEntity getTile(Object chunk, int x, int y, int z) {
        return ((IChunk) chunk).getTile(x, y, z);
    }

    @Override
    public int getBlockSkyLight(Object chunk, int x, int y, int z) {
        return ((IChunk) chunk).getBlockSkyLight(x, y, z);
    }

    @Override
    public int getBlockLight(Object chunk, int x, int y, int z) {
        return ((IChunk) chunk).getBlockLight(x, y, z);
    }

    @Override
    public void setChunkChanged(Object chunk, boolean changed) {
        ((IChunk) chunk).setChanged(changed);
    }

    @Override
    @Nullable
    public BlockEntity createBlockEntity(String id, Object chunk, CompoundTag nbt) {
        return BlockEntity.createBlockEntity(id, (IChunk) chunk, nbt);
    }

    @Override
    @Nullable
    public Entity createEntity(String id, Object chunk, CompoundTag nbt) {
        return Entity.createEntity(id, (IChunk) chunk, nbt);
    }

    @Override
    public Map<Long, Entity> getChunkEntities(Level level, int chunkX, int chunkZ) {
        return level.getChunkEntities(chunkX, chunkZ);
    }

    @Override
    public int getFullBlockId(Block block) {
        return block.getBlockState().blockStateHash();
    }

    @Override
    public Block getBlock(int fullId) {
        return Block.get(resolveBlockState(fullId));
    }

    @Override
    public boolean isWaterFullId(int fullId) {
        return fullId == getStillWaterFullId() || fullId == flowingWaterFullId;
    }

    @Override
    public int getAirFullId() {
        int value = airFullId;
        if (value == -1) {
            synchronized (this) {
                value = airFullId;
                if (value == -1) {
                    value = hashForBlock("minecraft:air");
                    airFullId = value;
                }
            }
        }
        return value;
    }

    @Override
    public int getStillWaterFullId() {
        int value = waterFullId;
        if (value == -1) {
            synchronized (this) {
                value = waterFullId;
                if (value == -1) {
                    value = hashForBlock("minecraft:water");
                    waterFullId = value;
                }
            }
        }
        return value;
    }

    @Override
    @Nullable
    public NukkitItemData createItemData(String bedrockId, int metadata) {
        Item item = Item.get(normalizeIdentifier(bedrockId), metadata, 1);
        return new NukkitItemData(normalizeIdentifier(bedrockId), -1, metadata);
    }

    @Override
    public String getItemMappingKey(Item item) {
        return item.getId() + ":" + item.getDamage();
    }

    @Override
    public String getItemMappingKey(NukkitItemData data) {
        return normalizeIdentifier(data.identifier()) + ":" + data.metadata();
    }

    @Override
    public Item getItem(NukkitItemData data, int amount) {
        return Item.get(normalizeIdentifier(data.identifier()), data.metadata(), amount);
    }

    @Override
    public Item getAirItem() {
        return getItem(new NukkitItemData("minecraft:air", -1, 0), 0);
    }

    @Override
    public boolean matchesItem(Item item, NukkitItemData data) {
        return getItemMappingKey(item).equals(getItemMappingKey(data));
    }

    @Override
    public boolean isAirItem(Item item) {
        String id = item.getId();
        return item.isNull() || "minecraft:air".equals(id) || "air".equals(id);
    }

    @Override
    public boolean useItemOn(Level level, BlockVector3 position, Item item, Direction face) {
        UseItemData useItemData = new UseItemData();
        useItemData.clickPos = new Vector3f(0.5f, 0.5f, 0.5f);
        useItemData.triggerType = UseItemData.TriggerType.PLAYER_INPUT;
        useItemData.clientInteractPrediction = UseItemData.PredictedResult.SUCCESS;
        return level.useItemOn(
                NukkitAdapter.adapt(position),
                item,
                NukkitAdapter.adapt(face),
                useItemData,
                null,
                true
        ) != null;
    }

    @Override
    public UUID getEntityUUID(Entity entity) {
        UUID uuid = entity.getUniqueId();
        return uuid != null ? uuid : new UUID(0, entity.getId());
    }

    @Override
    public void recalculateLight(Object chunk) {
        IChunk c = (IChunk) chunk;
        c.recalculateHeightMap();
        c.populateSkyLight();
        // PNX recomputes block light from block emission during chunk serialization.
    }

    @Override
    public boolean growTree(
            Level level,
            com.sk89q.worldedit.util.TreeGenerator.TreeType type,
            int x, int y, int z
    ) {
        // PNX's tree API (LegacyTreeGenerator + BlockManager + WoodType + RandomSourceProvider)
        // differs fundamentally from the NKX/MOT ObjectTree/ChunkManager surface. Tree generation
        // is therefore unavailable on PNX until a dedicated PNX tree integration is written.
        return false;
    }

    /**
     * Resolve a block-state hash to a live PNX {@link BlockState}, falling back
     * to air when the hash is unknown (including the sentinel {@code 0} used by
     * some callers) so that clearing a layer never throws.
     */
    private BlockState resolveBlockState(int fullId) {
        if (fullId == 0) {
            fullId = getAirFullId();
        }
        BlockState blockState = Registries.BLOCKSTATE.get(fullId);
        if (blockState == null) {
            blockState = Registries.BLOCKSTATE.get(getAirFullId());
        }
        if (blockState == null) {
            throw new IllegalStateException("PNX block registry is not initialised; cannot resolve block state hash: " + fullId);
        }
        return blockState;
    }

    /**
     * Compute the stable PNX block-state hash for a block identifier by routing
     * through the live block object, so the hash matches what chunks store.
     */
    private int hashForBlock(String identifier) {
        return Block.get(normalizeIdentifier(identifier)).getBlockState().blockStateHash();
    }

    private int blockStateHash(NbtMap nbtState) {
        return HashUtils.fnv1a_32_nbt_palette(toCompoundTag(nbtState));
    }

    private static CompoundTag toCompoundTag(NbtMap nbtState) {
        CompoundTag states = new CompoundTag();
        NbtMap sourceStates = nbtState.getCompound("states");
        for (Map.Entry<String, Object> entry : sourceStates.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Boolean bool) {
                states.putByte(entry.getKey(), bool ? 1 : 0);
            } else if (value instanceof Byte number) {
                states.putByte(entry.getKey(), number);
            } else if (value instanceof Number number) {
                states.putInt(entry.getKey(), number.intValue());
            } else {
                states.putString(entry.getKey(), String.valueOf(value));
            }
        }
        return new CompoundTag()
                .putString("name", normalizeIdentifier(nbtState.getString("name")))
                .putCompound("states", states);
    }

    private static String normalizeIdentifier(String identifier) {
        return identifier.contains(":") ? identifier : "minecraft:" + identifier;
    }

}
