package com.fastasyncworldedit.nukkit;

import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.level.particle.DestroyBlockParticle;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.implementation.packet.ChunkPacket;
import com.fastasyncworldedit.nukkit.adapter.NukkitAdapter;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplAdapter;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplLoader;
import com.fastasyncworldedit.nukkit.adapter.NukkitPlatformCapabilities;
import com.fastasyncworldedit.nukkit.blocks.NukkitGetBlocks;
import com.fastasyncworldedit.nukkit.mapping.BiomeMapping;
import com.fastasyncworldedit.nukkit.mapping.BlockMapping;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.weather.WeatherType;
import com.sk89q.worldedit.world.weather.WeatherTypes;
import org.enginehub.linbus.tree.LinTagType;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Nukkit world implementation for WorldEdit's {@link com.sk89q.worldedit.world.World}.
 * <p>
 * This class delegates world operations to Nukkit's {@code Level} API.
 * Unlike Bukkit, where FAWE bypasses the Bukkit API and writes directly to
 * NMS chunk sections for performance, Nukkit operations go through the
 * public Block and Level APIs. This is necessary because Nukkit does not
 * expose an equivalent low-level chunk section API.
 * <p>
 * Tree generation throws {@link UnsupportedOperationException} because
 * Nukkit's {@code ObjectTree} / {@code TreeGenerator} APIs mutate the
 * world directly without providing a block capture mechanism. FAWE's
 * history and undo system requires capturing all block changes, which
 * is impossible with Nukkit's tree API.
 * <p>
 * {@link #sendFakeChunk} is unsupported because converting Java Edition
 * block states to Bedrock runtime IDs and serializing into Nukkit's
 * LevelChunkPacket format is not exposed in the Nukkit API. Clipboard
 * visualization previews therefore do not work on Nukkit.
 * <p>
 * Key differences from Bukkit:
 * <ul>
 *   <li>setBlock uses Nukkit's public API instead of direct NMS writes</li>
 *   <li>Tree generation cannot participate in history/undo</li>
 *   <li>Fake chunk packets for clipboard previews are unsupported</li>
 *   <li>Biomes are stored as 2D columns, not Java Edition 3D biome sections</li>
 *   <li>Side effects are applied atomically in setBlock</li>
 * </ul>
 *
 * @see com.sk89q.worldedit.world.AbstractWorld
 * @see com.fastasyncworldedit.nukkit.adapter.NukkitPlatformCapabilities
 */
public class NukkitWorld extends AbstractWorld {

    private final WeakReference<Level> levelRef;
    private final String name;

    public NukkitWorld(Level level) {
        this.levelRef = new WeakReference<>(level);
        this.name = level.getName();
    }

    public Level getLevel() {
        Level level = levelRef.get();
        if (level == null) {
            throw new RuntimeException("World '" + name + "' has been unloaded");
        }
        return level;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getNameUnsafe() {
        return name;
    }

    @Override
    public String id() {
        return getName();
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block, SideEffectSet sideEffects)
            throws WorldEditException {
        NukkitImplAdapter adapter = NukkitImplLoader.get();
        int fullId = NukkitAdapter.adaptFullId(block.toImmutableState());

        Level level = getLevel();
        Block nukkitBlock = adapter.getBlock(fullId);
        // Nukkit update param controls neighbor updates and physics
        boolean update = sideEffects.shouldApply(SideEffect.NEIGHBORS)
                || sideEffects.shouldApply(SideEffect.UPDATE);
        return level.setBlock(position.x(), position.y(), position.z(), nukkitBlock, true, update);
    }

    @Override
    public Set<SideEffect> applySideEffects(
            BlockVector3 position, BlockState previousType, SideEffectSet sideEffectSet
    ) throws WorldEditException {
        // Nukkit applies all supported side effects atomically in setBlock
        return Set.of();
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        BlockState state = getBlock(position);
        Level level = getLevel();
        cn.nukkit.blockentity.BlockEntity be = level.getBlockEntity(NukkitAdapter.adapt(position));
        if (be != null && be.namedTag != null) {
            return state.toBaseBlock(LazyReference.computed(
                    com.fastasyncworldedit.nukkit.NukkitNbtConverter.toLinCompound(be.namedTag)
            ));
        }
        return state.toBaseBlock();
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        Level level = getLevel();
        Block block = level.getBlock(position.x(), position.y(), position.z());
        return NukkitAdapter.adaptBlockState(block);
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        Level level = getLevel();
        int biomeId = NukkitImplLoader.get().getLevelBiomeId(level, position.x(), position.y(), position.z());
        String jeBiome = BiomeMapping.beToJe(biomeId);
        BiomeType type = BiomeTypes.get(jeBiome);
        return type != null ? type : BiomeTypes.PLAINS;
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        Level level = getLevel();
        int beBiomeId = BiomeMapping.jeToBe(biome.id());
        NukkitImplLoader.get().setLevelBiomeId(level, position.x(), position.y(), position.z(), beBiomeId);
        return true;
    }

    /**
     * Delegate the int-coordinate overload to the {@link BlockVector3} variant. Without this
     * override the inherited {@code World} default silently returns {@code false} and writes no
     * biome, breaking callers that address biomes by raw coordinates.
     */
    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return setBiome(BlockVector3.at(x, y, z), biome);
    }

    @Override
    public boolean fullySupports3DBiomes() {
        return NukkitImplLoader.supports(NukkitPlatformCapabilities.THREE_DIMENSIONAL_BIOMES);
    }

    @Override
    public boolean clearContainerBlockContents(BlockVector3 position) {
        cn.nukkit.blockentity.BlockEntity be = getLevel().getBlockEntity(NukkitAdapter.adapt(position));
        if (be instanceof cn.nukkit.inventory.InventoryHolder holder) {
            holder.getInventory().clearAll();
            return true;
        }
        return false;
    }

    @Override
    public void dropItem(com.sk89q.worldedit.math.Vector3 position, BaseItemStack item) {
        cn.nukkit.item.Item nukkitItem = NukkitAdapter.adaptItem(item);
        if (!NukkitImplLoader.get().isAirItem(nukkitItem)) {
            getLevel().dropItem(NukkitAdapter.adapt(position), nukkitItem);
        }
    }

    @Override
    public void simulateBlockMine(BlockVector3 position) {
        Level level = getLevel();
        level.useBreakOn(NukkitAdapter.adapt(position));
    }

    @Override
    public Collection<BaseItemStack> getBlockDrops(BlockVector3 position) {
        Level level = getLevel();
        Block block = level.getBlock(position.x(), position.y(), position.z());
        cn.nukkit.item.Item[] drops = block.getDrops(NukkitImplLoader.get().getAirItem());
        List<BaseItemStack> result = new ArrayList<>(drops.length);
        NukkitImplAdapter adapter = NukkitImplLoader.get();
        for (cn.nukkit.item.Item drop : drops) {
            if (drop != null && !adapter.isAirItem(drop)) {
                result.add(NukkitAdapter.adaptItemStack(drop));
            }
        }
        return result;
    }

    @Override
    public boolean canPlaceAt(BlockVector3 position, BlockState blockState) {
        NukkitImplAdapter adapter = NukkitImplLoader.get();
        Block block = adapter.getBlock(NukkitAdapter.adaptFullId(blockState));
        return block.canBePlaced();
    }

    @Override
    public boolean useItem(BlockVector3 position, BaseItem item, Direction face) {
        cn.nukkit.item.Item nukkitItem = NukkitAdapter.adaptItem(item);
        if (NukkitImplLoader.get().isAirItem(nukkitItem)) {
            return false;
        }
        return NukkitImplLoader.get().useItemOn(getLevel(), position, nukkitItem, face);
    }

    @Override
    public boolean playEffect(Vector3 position, int type, int data) {
        if (!isSupportedLevelEvent(type)) {
            return false;
        }
        getLevel().addLevelEvent(NukkitAdapter.adapt(position), type, data);
        return true;
    }

    @Override
    public boolean playBlockBreakEffect(Vector3 position, BlockType type) {
        NukkitImplAdapter adapter = NukkitImplLoader.get();
        Block block = adapter.getBlock(NukkitAdapter.adaptFullId(type.getDefaultState()));
        getLevel().addParticle(new DestroyBlockParticle(NukkitAdapter.adapt(position), block));
        return true;
    }

    private static boolean isSupportedLevelEvent(int type) {
        return (type >= 1000 && type <= 2040)
                || (type >= 3001 && type <= 4000)
                || (type >= 9801 && type <= 9815)
                || type == 16384;
    }

    @Override
    @Deprecated
    public boolean generateTree(TreeGenerator.TreeType type, EditSession editSession, BlockVector3 position) {
        try {
            return generateTreeLegacy(type, editSession, position);
        } catch (MaxChangedBlocksException e) {
            return false;
        }
    }

    /**
     * Place a tree using the platform-native generator and capture the resulting block changes into
     * the given {@link EditSession} so FAWE history/undo covers the edit.
     * <p>
     * Nukkit's {@code ObjectTree} mutates the level directly via {@code ChunkManager} without a
     * block-capture hook, so this method snapshots the affected bounding box before placement and
     * replays the differing blocks through {@code editSession.setBlock}. That both records the
     * change for undo and re-applies it through FAWE's queue so lighting/refresh is consistent.
     */
    private boolean generateTreeLegacy(
            TreeGenerator.TreeType type,
            EditSession editSession,
            BlockVector3 position
    ) throws MaxChangedBlocksException {
        Level level = getLevel();
        NukkitImplAdapter adapter = NukkitImplLoader.get();
        int ox = position.x();
        int oy = position.y();
        int oz = position.z();

        // Tree footprint: leaves can span ±5 on x/z (mega spruce/jungle canopies), and trunks can
        // reach ~40 blocks tall for tall/mega variants. Snapshot a generous box so the diff captures
        // everything ObjectTree may write; the cost is proportional to actual block changes, not box
        // size, so over-estimating is safe. Note: trees taller than this bound would escape history
        // capture, but Nukkit's ObjectTree variants stay well within these limits.
        int radius = 6;
        int height = 48;
        int minY = Math.max(level.getMinBlockY(), oy - 1);
        int maxY = Math.min(level.getMaxBlockY(), oy + height);

        // Pre-snapshot: record JE ordinals via the adapter before placement.
        int sizeX = radius * 2 + 1;
        int sizeZ = radius * 2 + 1;
        int sizeY = maxY - minY + 1;
        char[][][] before = new char[sizeX][sizeZ][sizeY];
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Object chunk = adapter.getChunk(level, (ox + dx) >> 4, (oz + dz) >> 4);
                if (chunk == null) {
                    continue;
                }
                for (int dy = 0; dy < sizeY; dy++) {
                    int wx = ox + dx;
                    int wy = minY + dy;
                    int wz = oz + dz;
                    int fullId = adapter.getFullBlockId(chunk, wx & 0xF, wy, wz & 0xF, 0);
                    before[dx + radius][dz + radius][dy] = BlockMapping.fullIdToJeOrdinal(fullId);
                }
            }
        }

        boolean placed = adapter.growTree(level, type, ox, oy, oz);
        if (!placed) {
            return false;
        }

        // Post-snapshot: replay differing blocks through the EditSession for history + refresh.
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Object chunk = adapter.getChunk(level, (ox + dx) >> 4, (oz + dz) >> 4);
                if (chunk == null) {
                    continue;
                }
                for (int dy = 0; dy < sizeY; dy++) {
                    int wx = ox + dx;
                    int wy = minY + dy;
                    int wz = oz + dz;
                    int fullId = adapter.getFullBlockId(chunk, wx & 0xF, wy, wz & 0xF, 0);
                    char after = BlockMapping.fullIdToJeOrdinal(fullId);
                    char pre = before[dx + radius][dz + radius][dy];
                    if (after != pre) {
                        com.sk89q.worldedit.world.block.BlockState state =
                                after == Character.MAX_VALUE
                                        ? com.sk89q.worldedit.world.block.BlockTypes.AIR.getDefaultState()
                                        : com.sk89q.worldedit.world.block.BlockTypesCache.states[after];
                        editSession.setBlock(wx, wy, wz, state);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean generateTree(
            com.sk89q.worldedit.world.generation.TreeType type,
            EditSession editSession,
            BlockVector3 position
    ) throws MaxChangedBlocksException {
        // The new TreeType-based contract maps onto the legacy TreeGenerator.TreeType generator so
        // the same snapshot/replay capture path applies. TreeType carries only an id string, so
        // resolve it back to a legacy type when possible; otherwise fall back to a plain oak tree.
        TreeGenerator.TreeType legacy = resolveLegacyTreeType(type);
        return generateTreeLegacy(legacy, editSession, position);
    }

    /**
     * Best-effort resolution from the new {@code TreeType} record to a legacy
     * {@link TreeGenerator.TreeType}. Falls back to {@code TREE} (oak) when unknown.
     */
    private static TreeGenerator.TreeType resolveLegacyTreeType(
            com.sk89q.worldedit.world.generation.TreeType type
    ) {
        if (type == null) {
            return TreeGenerator.TreeType.TREE;
        }
        String id = type.id();
        // Strip any "minecraft:" prefix and try by alias; fallback to oak.
        String key = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        TreeGenerator.TreeType resolved = TreeGenerator.TreeType.lookup(key);
        return resolved != null ? resolved : TreeGenerator.TreeType.TREE;
    }

    /**
     * Region regeneration is not supported on Nukkit: there is no exposed API to reconstruct a
     * region from the level's generator without disturbing surrounding terrain. Override (rather
     * than relying on the inherited {@code @NonAbstractForCompatibility} default) so that calling
     * {@code //regen} reports failure instead of throwing {@link IllegalStateException}.
     */
    @Override
    public boolean regenerate(
            com.sk89q.worldedit.regions.Region region,
            com.sk89q.worldedit.EditSession editSession
    ) {
        return false;
    }

    @Override
    public BlockVector3 getSpawnPosition() {
        Level level = getLevel();
        cn.nukkit.math.Vector3 spawn = level.getSpawnLocation();
        return BlockVector3.at(spawn.getFloorX(), spawn.getFloorY(), spawn.getFloorZ());
    }

    @Override
    public void refreshChunk(int chunkX, int chunkZ) {
        Level level = getLevel();
        for (cn.nukkit.Player player : level.getChunkPlayers(chunkX, chunkZ).values()) {
            level.requestChunk(chunkX, chunkZ, player);
        }
    }

    @Override
    public IChunkGet get(int chunkX, int chunkZ) {
        return new NukkitGetBlocks(getLevel(), chunkX, chunkZ);
    }

    @Override
    public void sendFakeChunk(@Nullable Player player, ChunkPacket packet) {
        // Intentional no-op. Constructing a Bedrock LevelChunkPacket from FAWE's Java-internal
        // chunk representation requires full PalettedBlockStorage + 3D biome serialization that
        // Nukkit does not expose. Clipboard visualization previews are therefore unavailable on
        // Nukkit; failing silently keeps callers (e.g. WorldWrapper) functional instead of throwing.
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        List<NukkitEntity> entities = new ArrayList<>();
        for (cn.nukkit.entity.Entity entity : getLevel().getEntities()) {
            if (region.contains(BlockVector3.at(entity.getFloorX(), entity.getFloorY(), entity.getFloorZ()))) {
                entities.add(new NukkitEntity(entity));
            }
        }
        return entities;
    }

    @Override
    public List<? extends Entity> getEntities() {
        List<NukkitEntity> entities = new ArrayList<>();
        for (cn.nukkit.entity.Entity entity : getLevel().getEntities()) {
            entities.add(new NukkitEntity(entity));
        }
        return entities;
    }

    @Override
    public int getMinY() {
        return getLevel().getMinBlockY();
    }

    @Override
    public int getMaxY() {
        return getLevel().getMaxBlockY();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (other instanceof NukkitWorld otherWorld) {
            return name.equals(otherWorld.name);
        } else if (other instanceof com.sk89q.worldedit.world.World otherWorld) {
            return name.equals(otherWorld.getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean tile(int x, int y, int z, FaweCompoundTag tile) throws WorldEditException {
        if (tile.linTag().findTag("id", LinTagType.stringTag()) == null) {
            return false;
        }
        Level level = getLevel();
        NukkitImplAdapter adapter = NukkitImplLoader.get();
        Object chunk = adapter.getChunk(level, x >> 4, z >> 4);
        if (chunk == null) {
            return false;
        }
        cn.nukkit.nbt.tag.CompoundTag nbt = com.fastasyncworldedit.nukkit.NukkitNbtConverter.toNukkit(tile);
        nbt.putInt("x", x);
        nbt.putInt("y", y);
        nbt.putInt("z", z);

        String id = com.fastasyncworldedit.nukkit.mapping.BlockEntityIdMapping.normalize(nbt.getString("id"));

        cn.nukkit.blockentity.BlockEntity existing = adapter.getTile(chunk, x & 0xF, y, z & 0xF);
        if (existing != null) {
            existing.close();
        }
        return adapter.createBlockEntity(id, chunk, nbt) != null;
    }

    @Override
    public WeatherType getWeather() {
        Level level = getLevel();
        if (level.isThundering()) {
            return WeatherTypes.THUNDER_STORM;
        } else if (level.isRaining()) {
            return WeatherTypes.RAIN;
        }
        return WeatherTypes.CLEAR;
    }

    @Override
    public void setWeather(WeatherType weatherType) {
        Level level = getLevel();
        if (weatherType == WeatherTypes.THUNDER_STORM) {
            level.setRaining(true);
            level.setThundering(true);
        } else if (weatherType == WeatherTypes.RAIN) {
            level.setRaining(true);
            level.setThundering(false);
        } else {
            level.setRaining(false);
            level.setThundering(false);
        }
    }

    @Override
    public long getRemainingWeatherDuration() {
        Level level = getLevel();
        if (level.isThundering()) {
            return level.getThunderTime();
        } else if (level.isRaining()) {
            return level.getRainTime();
        }
        return 0;
    }

    @Override
    public void setWeather(WeatherType weatherType, long duration) {
        setWeather(weatherType);
        Level level = getLevel();
        if (weatherType == WeatherTypes.THUNDER_STORM) {
            level.setRainTime((int) duration);
            level.setThunderTime((int) duration);
        } else if (weatherType == WeatherTypes.RAIN) {
            level.setRainTime((int) duration);
        }
    }

    @Override
    public void checkLoadedChunk(BlockVector3 pt) {
        getLevel().loadChunk(pt.x() >> 4, pt.z() >> 4, false);
    }

    @Override
    public void flush() {
        // No-op: Nukkit handles chunk flushing internally
    }

}
