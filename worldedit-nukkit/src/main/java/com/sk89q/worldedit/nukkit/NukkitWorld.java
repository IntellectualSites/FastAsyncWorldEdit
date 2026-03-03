package com.sk89q.worldedit.nukkit;

import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.implementation.packet.ChunkPacket;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplAdapter;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplLoader;
import com.fastasyncworldedit.nukkit.mapping.BiomeMapping;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
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
import com.sk89q.worldedit.world.weather.WeatherType;
import com.sk89q.worldedit.world.weather.WeatherTypes;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        int blockId = fullId >> adapter.getBlockDataBits();
        int meta = fullId & adapter.getBlockDataMask();

        Level level = getLevel();
        Block nukkitBlock = Block.get(blockId, meta);
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
        int fullId = (block.getId() << NukkitImplLoader.get().getBlockDataBits()) | block.getDamage();
        return NukkitAdapter.adaptBlockState(fullId);
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        Level level = getLevel();
        int biomeId = level.getBiomeId(position.x(), position.z());
        String jeBiome = BiomeMapping.beToJe(biomeId);
        BiomeType type = BiomeTypes.get(jeBiome);
        return type != null ? type : BiomeTypes.PLAINS;
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        Level level = getLevel();
        int beBiomeId = BiomeMapping.jeToBe(biome.id());
        level.setBiomeId(position.x(), position.z(), (byte) beBiomeId);
        return true;
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
        if (nukkitItem.getId() != cn.nukkit.item.Item.AIR) {
            getLevel().dropItem(NukkitAdapter.adapt(position), nukkitItem);
        }
    }

    @Override
    public void simulateBlockMine(BlockVector3 position) {
        Level level = getLevel();
        level.useBreakOn(NukkitAdapter.adapt(position));
    }

    @Override
    public boolean generateTree(TreeGenerator.TreeType type, EditSession editSession, BlockVector3 position) {
        Level level = getLevel();
        cn.nukkit.math.NukkitRandom random = new cn.nukkit.math.NukkitRandom();
        int x = position.x();
        int y = position.y();
        int z = position.z();
        cn.nukkit.math.Vector3 pos = new cn.nukkit.math.Vector3(x, y, z);

        // ObjectTree subclasses use placeObject(ChunkManager, x, y, z, NukkitRandom)
        cn.nukkit.level.generator.object.tree.ObjectTree objectTree = switch (type) {
            case TREE, BIG_TREE -> new cn.nukkit.level.generator.object.tree.ObjectOakTree();
            case REDWOOD, TALL_REDWOOD -> new cn.nukkit.level.generator.object.tree.ObjectSpruceTree();
            case MEGA_REDWOOD -> new cn.nukkit.level.generator.object.tree.ObjectBigSpruceTree(0.45f, 2);
            case BIRCH -> new cn.nukkit.level.generator.object.tree.ObjectBirchTree();
            case TALL_BIRCH -> new cn.nukkit.level.generator.object.tree.ObjectTallBirchTree();
            case SMALL_JUNGLE, SHORT_JUNGLE -> new cn.nukkit.level.generator.object.tree.ObjectJungleTree();
            case CRIMSON_FUNGUS -> new cn.nukkit.level.generator.object.tree.ObjectCrimsonTree();
            case WARPED_FUNGUS -> new cn.nukkit.level.generator.object.tree.ObjectWarpedTree();
            default -> null;
        };
        if (objectTree != null) {
            objectTree.placeObject(level, x, y, z, random);
            return true;
        }

        // TreeGenerator subclasses use generate(ChunkManager, NukkitRandom, Vector3)
        cn.nukkit.level.generator.object.tree.TreeGenerator treeGen = switch (type) {
            case JUNGLE -> new cn.nukkit.level.generator.object.tree.ObjectJungleBigTree(
                    10, 20,
                    Block.get(Block.WOOD, cn.nukkit.block.BlockWood.JUNGLE),
                    Block.get(Block.LEAVES, cn.nukkit.block.BlockWood.JUNGLE)
            );
            case SWAMP -> new cn.nukkit.level.generator.object.tree.ObjectSwampTree();
            case ACACIA -> new cn.nukkit.level.generator.object.tree.ObjectSavannaTree();
            case DARK_OAK -> new cn.nukkit.level.generator.object.tree.ObjectDarkOakTree();
            default -> null;
        };
        if (treeGen != null) {
            return treeGen.generate(level, random, pos);
        }

        // Trees with divergent class hierarchies between MOT and NKX — delegate to adapter
        String adapterType = switch (type) {
            case MANGROVE, TALL_MANGROVE -> "MANGROVE";
            case CHERRY -> "CHERRY";
            case PALE_OAK, PALE_OAK_CREAKING -> "PALE_OAK";
            default -> null;
        };
        if (adapterType != null) {
            return NukkitImplLoader.get().generateTree(adapterType, level, x, y, z, random, pos);
        }

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
        // Not supported on Nukkit: requires converting JE block states to BE runtime ID palettes
        // and serializing into Nukkit's LevelChunkPacket format (PalettedBlockStorage + 3D biomes).
        // Affects non-critical features like clipboard visualization previews.
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
        Level level = getLevel();
        cn.nukkit.level.format.generic.BaseFullChunk chunk = level.getChunk(x >> 4, z >> 4, true);
        if (chunk == null) {
            return false;
        }
        cn.nukkit.nbt.tag.CompoundTag nbt = com.fastasyncworldedit.nukkit.NukkitNbtConverter.toNukkit(tile);
        nbt.putInt("x", x);
        nbt.putInt("y", y);
        nbt.putInt("z", z);

        cn.nukkit.blockentity.BlockEntity existing = chunk.getTile(x & 0xF, y, z & 0xF);
        if (existing != null) {
            existing.close();
        }
        if (nbt.contains("id")) {
            String id = nbt.getString("id").replaceFirst("BlockEntity", "");
            cn.nukkit.blockentity.BlockEntity.createBlockEntity(id, chunk, nbt);
        }
        return true;
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
