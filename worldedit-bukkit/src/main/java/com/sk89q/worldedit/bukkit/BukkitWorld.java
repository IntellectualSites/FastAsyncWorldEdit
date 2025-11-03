/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.bukkit;

import com.fastasyncworldedit.bukkit.util.WorldUnloadedException;
import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.internal.exception.FaweException;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.implementation.packet.ChunkPacket;
import com.fastasyncworldedit.core.util.FoliaUtil;
import com.fastasyncworldedit.core.util.TaskManager;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.bukkit.adapter.UnsupportedVersionEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.internal.wna.WorldNativeAccess;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.RegenOptions;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.generation.ConfiguredFeatureType;
import com.sk89q.worldedit.world.generation.StructureType;
import com.sk89q.worldedit.world.weather.WeatherType;
import com.sk89q.worldedit.world.weather.WeatherTypes;
import io.papermc.lib.PaperLib;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class BukkitWorld extends AbstractWorld {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private static final boolean HAS_3D_BIOMES;
    //FAWE start - allow access for easy checking if World#getMin/MaxHeight exists
    public static final boolean HAS_MIN_Y;
    //FAWE end

    private static final Map<Integer, Effect> effects = new HashMap<>();

    static {
        for (Effect effect : Effect.values()) {
            @SuppressWarnings("deprecation")
            int id = effect.getId();
            effects.put(id, effect);
        }

        boolean temp;
        try {
            World.class.getMethod("getBiome", int.class, int.class, int.class);
            temp = true;
        } catch (NoSuchMethodException e) {
            temp = false;
        }
        HAS_3D_BIOMES = temp;
        try {
            World.class.getMethod("getMinHeight");
            temp = true;
        } catch (NoSuchMethodException e) {
            temp = false;
        }
        HAS_MIN_Y = temp;
    }

    protected WeakReference<World> worldRef;
    //FAWE start
    protected final String worldNameRef;
    //FAWE end
    private final WorldNativeAccess<?, ?, ?> worldNativeAccess;

    /**
     * Construct the object.
     *
     * @param world the world
     */
    public BukkitWorld(World world) {
        this.worldRef = new WeakReference<>(world);
        //FAWE start
        this.worldNameRef = world.getName();
        //FAWE end
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        if (adapter != null) {
            this.worldNativeAccess = adapter.createWorldNativeAccess(world);
        } else {
            this.worldNativeAccess = null;
        }
    }

    @Override
    public List<com.sk89q.worldedit.entity.Entity> getEntities(Region region) {
        World world = getWorld();
        List<Entity> ents = FoliaUtil.isFoliaServer()
                ? TaskManager.taskManager().syncWhenFree(world::getEntities)
                : TaskManager.taskManager().sync(world::getEntities);
        List<com.sk89q.worldedit.entity.Entity> entities = new ArrayList<>();
        for (Entity ent : ents) {
            if (region.contains(BukkitAdapter.asBlockVector(ent.getLocation()))) {
                entities.add(BukkitAdapter.adapt(ent));
            }
        }
        return entities;
    }

    @Override
    public List<com.sk89q.worldedit.entity.Entity> getEntities() {
        World world = getWorld();
        List<Entity> ents = FoliaUtil.isFoliaServer()
                ? TaskManager.taskManager().syncWhenFree(world::getEntities)
                : TaskManager.taskManager().sync(world::getEntities);
        List<com.sk89q.worldedit.entity.Entity> list = new ArrayList<>();
        for (Entity entity : ents) {
            list.add(BukkitAdapter.adapt(entity));
        }
        return list;
    }

    @Override
    public int removeEntities(Region region) {
        World world = getWorld();
        if (FoliaUtil.isFoliaServer()) {
            return TaskManager.taskManager().syncWhenFree(() -> {
                Plugin plugin = WorldEditPlugin.getInstance();
                AtomicInteger scheduled = new AtomicInteger(0);
                for (Entity entity : world.getEntities()) {
                    if (!region.contains(BukkitAdapter.asBlockVector(entity.getLocation()))) {
                        continue;
                    }
                    try {
                        entity.getScheduler().execute(plugin, entity::remove, null, 1);
                        scheduled.incrementAndGet();
                    } catch (UnsupportedOperationException ignored) {
                    }
                }
                return scheduled.get();
            });
        }
        return TaskManager.taskManager().sync(() -> {
            int removed = 0;
            for (Entity entity : world.getEntities()) {
                if (!region.contains(BukkitAdapter.asBlockVector(entity.getLocation()))) {
                    continue;
                }
                try {
                    entity.remove();
                    try {
                        if (entity.isDead() || !entity.isValid()) {
                            removed++;
                        }
                    } catch (Throwable t) {
                        if (!entity.isValid()) {
                            removed++;
                        }
                    }
                } catch (UnsupportedOperationException ignored) {
                }
            }
            return removed;
        });
    }

    //FAWE: createEntity was moved to IChunkExtent to prevent issues with Async Entity Add.

    /**
     * Get the world handle.
     *
     * @return the world
     */
    public World getWorld() {
        //FAWE start
        World tmp = worldRef.get();
        if (tmp == null) {
            tmp = Bukkit.getWorld(worldNameRef);
            if (tmp != null) {
                worldRef = new WeakReference<>(tmp);
            }
        }
        //FAWE end
        return checkNotNull(tmp, "The world was unloaded and the reference is unavailable");
    }

    //FAWE start

    /**
     * Get the world handle.
     *
     * @return the world
     */
    protected World getWorldChecked() throws WorldEditException {
        World tmp = worldRef.get();
        if (tmp == null) {
            tmp = Bukkit.getWorld(worldNameRef);
            if (tmp != null) {
                worldRef = new WeakReference<>(tmp);
            }
        }
        if (tmp == null) {
            throw new WorldUnloadedException(worldNameRef);
        }
        return tmp;
    }
    //FAWE end

    @Override
    public String getName() {
        //FAWE start - Throw WorldUnloadedException rather than NPE when world unloaded and attempted to be accessed
        return getWorldChecked().getName();
        //FAWE end
    }

    //FAWE start - allow history to read an unloaded world's name
    @Override
    public String getNameUnsafe() {
        return worldNameRef;
    }
    //FAWE end

    @Override
    public String id() {
        return getWorld().getName().replace(" ", "_").toLowerCase(Locale.ROOT);
    }

    @Override
    public Path getStoragePath() {
        Path worldFolder = getWorld().getWorldFolder().toPath();
        switch (getWorld().getEnvironment()) {
            case NETHER:
                return worldFolder.resolve("DIM-1");
            case THE_END:
                return worldFolder.resolve("DIM1");
            case NORMAL:
            default:
                return worldFolder;
        }
    }

    @Override
    public int getBlockLightLevel(BlockVector3 pt) {
        //FAWE start - safe edit region
        testCoords(pt);
        //FAWE end
        return getWorld().getBlockAt(pt.x(), pt.y(), pt.z()).getLightLevel();
    }

    @Override
    public boolean regenerate(Region region, Extent extent, RegenOptions options) {
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        try {
            if (adapter != null) {
                return adapter.regenerate(getWorld(), region, extent, options);
            } else {
                throw new UnsupportedOperationException("Missing BukkitImplAdapter for this version.");
            }
        } catch (FaweException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.warn("Regeneration via adapter failed.", e);
            return false;
        }
    }

    @Override
    public boolean clearContainerBlockContents(BlockVector3 pt) {
        checkNotNull(pt);
        //FAWE start - safe edit region
        testCoords(pt);
        //FAWE end
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        if (adapter != null) {
            try {
                return adapter.clearContainerBlockContents(getWorld(), pt);
            } catch (Exception ignored) {
            }
        }
        if (!getBlock(pt).getBlockType().getMaterial().hasContainer()) {
            return false;
        }

        Block block = getWorld().getBlockAt(pt.x(), pt.y(), pt.z());
        BlockState state = PaperLib.getBlockState(block, false).getState();
        if (!(state instanceof InventoryHolder)) {
            return false;
        }

        TaskManager.taskManager().sync(() -> {
            InventoryHolder chest = (InventoryHolder) state;
            Inventory inven = chest.getInventory();
            if (chest instanceof Chest) {
                inven = ((Chest) chest).getBlockInventory();
            }
            inven.clear();
            return null;
        });
        return true;
    }

    /**
     * An EnumMap that stores which WorldEdit TreeTypes apply to which Bukkit TreeTypes.
     */
    private static final EnumMap<TreeGenerator.TreeType, TreeType> treeTypeMapping =
            new EnumMap<>(TreeGenerator.TreeType.class);

    static {
        for (TreeGenerator.TreeType type : TreeGenerator.TreeType.values()) {
            try {
                TreeType bukkitType = TreeType.valueOf(type.name());
                treeTypeMapping.put(type, bukkitType);
            } catch (IllegalArgumentException e) {
                // Unhandled TreeType
            }
        }
        // Other mappings for WE-specific values
        treeTypeMapping.put(TreeGenerator.TreeType.SHORT_JUNGLE, TreeType.SMALL_JUNGLE);
        treeTypeMapping.put(TreeGenerator.TreeType.RANDOM, TreeType.BROWN_MUSHROOM);
        treeTypeMapping.put(TreeGenerator.TreeType.RANDOM_REDWOOD, TreeType.REDWOOD);
        treeTypeMapping.put(TreeGenerator.TreeType.PINE, TreeType.REDWOOD);
        treeTypeMapping.put(TreeGenerator.TreeType.RANDOM_BIRCH, TreeType.BIRCH);
        treeTypeMapping.put(TreeGenerator.TreeType.RANDOM_JUNGLE, TreeType.JUNGLE);
        treeTypeMapping.put(TreeGenerator.TreeType.RANDOM_MUSHROOM, TreeType.BROWN_MUSHROOM);
        for (TreeGenerator.TreeType type : TreeGenerator.TreeType.values()) {
            if (treeTypeMapping.get(type) == null) {
                //FAWE start
                LOGGER.info("No TreeType mapping for TreeGenerator.TreeType." + type);
                LOGGER.info("The above message is displayed because your FAWE version is newer than {}" +
                        " and contains features of future minecraft versions which do not exist in {} hence the tree type" +
                        " {} is not available. This is not an error. This version of FAWE will work on your version of " +
                        " Minecraft. This is an informative message only.", Bukkit.getVersion(), Bukkit.getVersion(), type);
                //FAWE end
            }
        }
    }

    public static TreeType toBukkitTreeType(TreeGenerator.TreeType type) {
        return treeTypeMapping.get(type);
    }

    @Override
    public boolean generateTree(TreeGenerator.TreeType type, EditSession editSession, BlockVector3 pt) {
        //FAWE start - allow tree commands to be undone and obey region restrictions
        testCoords(pt);
        return WorldEditPlugin.getInstance().getBukkitImplAdapter().generateTree(type, editSession, pt, getWorld());
        //FAWE end
    }

    @Override
    public void dropItem(Vector3 pt, BaseItemStack item) {
        World world = getWorld();
        world.dropItemNaturally(BukkitAdapter.adapt(world, pt), BukkitAdapter.adapt(item));
    }

    @Override
    public void checkLoadedChunk(BlockVector3 pt) {
        //FAWE start - safe edit region
        testCoords(pt);
        //FAWE end
        World world = getWorld();
        //FAWE start
        int X = pt.x() >> 4;
        int Z = pt.z() >> 4;
        if (Fawe.isMainThread()) {
            world.getChunkAt(X, Z);
        } else if (PaperLib.isPaper()) {
            PaperLib.getChunkAtAsync(world, X, Z, true);
        }
        //FAWE end
    }

    @Override
    public boolean equals(Object other) {
        final World ref = worldRef.get();
        if (ref == null) {
            return false;
        } else if (other == null) {
            return false;
        } else if ((other instanceof BukkitWorld)) {
            World otherWorld = ((BukkitWorld) other).worldRef.get();
            return ref.equals(otherWorld);
        } else if (other instanceof com.sk89q.worldedit.world.World) {
            return ((com.sk89q.worldedit.world.World) other).getName().equals(ref.getName());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getWorld().hashCode();
    }

    @Override
    public int getMaxY() {
        return getWorld().getMaxHeight() - 1;
    }

    @Override
    public int getMinY() {
        if (HAS_MIN_Y) {
            return getWorld().getMinHeight();
        }
        return super.getMinY();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void fixAfterFastMode(Iterable<BlockVector2> chunks) {
        World world = getWorld();
        for (BlockVector2 chunkPos : chunks) {
            world.refreshChunk(chunkPos.x(), chunkPos.z());
        }
    }

    @Override
    public boolean playEffect(Vector3 position, int type, int data) {
        World world = getWorld();

        final Effect effect = effects.get(type);
        if (effect == null) {
            return false;
        }

        world.playEffect(BukkitAdapter.adapt(world, position), effect, data);

        return true;
    }

    //FAWE start - allow block break effect of non-legacy blocks
    @Override
    public boolean playBlockBreakEffect(Vector3 position, BlockType type) {
        World world = getWorld();
        world.playEffect(BukkitAdapter.adapt(world, position), Effect.STEP_SOUND, BukkitAdapter.adapt(type));
        return true;
    }
    //FAWE end

    @Override
    public WeatherType getWeather() {
        if (getWorld().isThundering()) {
            return WeatherTypes.THUNDER_STORM;
        } else if (getWorld().hasStorm()) {
            return WeatherTypes.RAIN;
        }

        return WeatherTypes.CLEAR;
    }

    @Override
    public long getRemainingWeatherDuration() {
        return getWorld().getWeatherDuration();
    }

    @Override
    public void setWeather(WeatherType weatherType) {
        if (weatherType == WeatherTypes.THUNDER_STORM) {
            getWorld().setThundering(true);
        } else if (weatherType == WeatherTypes.RAIN) {
            getWorld().setStorm(true);
        } else {
            getWorld().setStorm(false);
            getWorld().setThundering(false);
        }
    }

    @Override
    public void setWeather(WeatherType weatherType, long duration) {
        // Who named these methods...
        if (weatherType == WeatherTypes.THUNDER_STORM) {
            getWorld().setThundering(true);
            getWorld().setThunderDuration((int) duration);
            getWorld().setWeatherDuration((int) duration);
        } else if (weatherType == WeatherTypes.RAIN) {
            getWorld().setStorm(true);
            getWorld().setWeatherDuration((int) duration);
        } else {
            getWorld().setStorm(false);
            getWorld().setThundering(false);
            getWorld().setWeatherDuration((int) duration);
        }
    }

    @Override
    public BlockVector3 getSpawnPosition() {
        return BukkitAdapter.asBlockVector(getWorld().getSpawnLocation());
    }

    @Override
    public void simulateBlockMine(BlockVector3 pt) {
        //FAWE start - safe edit region
        testCoords(pt);
        //FAWE end
        getWorld().getBlockAt(pt.x(), pt.y(), pt.z()).breakNaturally();
    }

    //FAWE start
    @Override
    public Collection<BaseItemStack> getBlockDrops(BlockVector3 position) {
        return getWorld().getBlockAt(position.x(), position.y(), position.z()).getDrops().stream()
                .map(BukkitAdapter::adapt).collect(Collectors.toList());
    }
    //FAWE end

    @Override
    public boolean canPlaceAt(BlockVector3 position, com.sk89q.worldedit.world.block.BlockState blockState) {
        //FAWE start - safe edit region
        testCoords(position);
        //FAWE end
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        if (adapter != null) {
            return adapter.canPlaceAt(getWorld(), position, blockState);
        }
        // We can't check, so assume yes.
        return true;
    }

    @Override
    public boolean generateFeature(ConfiguredFeatureType type, EditSession editSession, BlockVector3 position) {
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        if (adapter != null) {
            return adapter.generateFeature(type, getWorld(), editSession, position);
        }
        // No adapter, we can't generate this.
        return false;
    }

    @Override
    public boolean generateStructure(StructureType type, EditSession editSession, BlockVector3 position) {
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        if (adapter != null) {
            return adapter.generateStructure(type, getWorld(), editSession, position);
        }
        // No adapter, we can't generate this.
        return false;
    }

    private static volatile boolean hasWarnedImplError = false;

    @Override
    public com.sk89q.worldedit.world.block.BlockState getBlock(BlockVector3 position) {
        //FAWE start - safe edit region
        testCoords(position);
        //FAWE end
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        if (adapter != null) {
            try {
                return adapter.getBlock(BukkitAdapter.adapt(getWorld(), position)).toImmutableState();
            } catch (Exception e) {
                if (!hasWarnedImplError) {
                    hasWarnedImplError = true;
                    LOGGER.warn("Unable to retrieve block via impl adapter", e);
                }
            }
        }
        if (WorldEditPlugin.getInstance().getLocalConfiguration().unsupportedVersionEditing) {
            Block bukkitBlock = getWorld().getBlockAt(position.x(), position.y(), position.z());
            return BukkitAdapter.adapt(bukkitBlock.getBlockData());
        } else {
            throw new RuntimeException(new UnsupportedVersionEditException());
        }
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block, SideEffectSet sideEffects) {
        //FAWE start - safe edit region
        testCoords(position);
        //FAWE end
        if (worldNativeAccess != null) {
            try {
                return worldNativeAccess.setBlock(position, block, sideEffects);
            } catch (Exception e) {
                if (block instanceof BaseBlock && ((BaseBlock) block).getNbt() != null) {
                    LOGGER.warn("Tried to set a corrupt tile entity at " + position.toString()
                            + ": " + ((BaseBlock) block).getNbt(), e);
                } else {
                    LOGGER.warn("Failed to set block via adapter, falling back to generic", e);
                }
            }
        }
        Block bukkitBlock = getWorld().getBlockAt(position.x(), position.y(), position.z());
        bukkitBlock.setBlockData(BukkitAdapter.adapt(block), sideEffects.doesApplyAny());
        return true;
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        //FAWE start - safe edit region
        testCoords(position);
        //FAWE end
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        if (adapter != null) {
            return adapter.getFullBlock(BukkitAdapter.adapt(getWorld(), position));
        } else {
            return getBlock(position).toBaseBlock();
        }
    }

    private void testCoords(BlockVector3 position) throws FaweException {
        if (!Settings.settings().REGION_RESTRICTIONS_OPTIONS.RESTRICT_TO_SAFE_RANGE) {
            return;
        }
        int x = position.x();
        int z = position.z();
        if (x > 30000000 || z > 30000000 || x < -30000000 || z < -30000000) {
            throw FaweCache.OUTSIDE_SAFE_REGION;
        }
    }

    @Override
    public Set<SideEffect> applySideEffects(
            BlockVector3 position, com.sk89q.worldedit.world.block.BlockState previousType,
            SideEffectSet sideEffectSet
    ) {
        //FAWE start - safe edit region
        testCoords(position);
        //FAWE end
        if (worldNativeAccess != null) {
            worldNativeAccess.applySideEffects(position, previousType, sideEffectSet);
            return Sets.intersection(
                    WorldEditPlugin.getInstance().getInternalPlatform().getSupportedSideEffects(),
                    sideEffectSet.getSideEffectsToApply()
            );
        }

        return ImmutableSet.of();
    }

    @Override
    public boolean useItem(BlockVector3 position, BaseItem item, Direction face) {
        //FAWE start - safe edit region
        testCoords(position);
        //FAWE end
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        if (adapter != null) {
            return adapter.simulateItemUse(getWorld(), position, item, face);
        }

        return false;
    }

    @Override
    public boolean fullySupports3DBiomes() {
        // Supports if API does and we're not in the overworld
        return HAS_3D_BIOMES && getWorld().getEnvironment() != World.Environment.NORMAL || PaperLib.isVersion(18);
    }

    @SuppressWarnings("deprecation")
    @Override
    public BiomeType getBiome(BlockVector3 position) {
        //FAWE start - safe edit region
        testCoords(position);
        //FAWE end
        if (HAS_3D_BIOMES) {
            return BukkitAdapter.adapt(getWorld().getBiome(position.x(), position.y(), position.z()));
        } else {
            return BukkitAdapter.adapt(getWorld().getBiome(position.x(), position.z()));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        //FAWE start - safe edit region
        testCoords(position);
        //FAWE end
        if (HAS_3D_BIOMES) {
            getWorld().setBiome(position.x(), position.y(), position.z(), BukkitAdapter.adapt(biome));
        } else {
            getWorld().setBiome(position.x(), position.z(), BukkitAdapter.adapt(biome));
        }
        return true;
    }

    //FAWE start

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block)
            throws WorldEditException {
        return setBlock(BlockVector3.at(x, y, z), block);
    }

    @Override
    public boolean tile(int x, int y, int z, FaweCompoundTag tile) throws WorldEditException {
        return false;
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return setBiome(BlockVector3.at(x, y, z), biome);
    }

    @Override
    public void refreshChunk(int chunkX, int chunkZ) {
        testCoords(BlockVector3.at(chunkX << 4, 0, chunkZ << 4));
        getWorld().refreshChunk(chunkX, chunkZ);
    }

    @Override
    public IChunkGet get(int chunkX, int chunkZ) {
        testCoords(BlockVector3.at(chunkX << 4, 0, chunkZ << 4));
        return WorldEditPlugin.getInstance().getBukkitImplAdapter().get(getWorldChecked(), chunkX, chunkZ);
    }

    @Override
    public void sendFakeChunk(Player player, ChunkPacket packet) {
        org.bukkit.entity.Player bukkitPlayer = BukkitAdapter.adapt(player);
        WorldEditPlugin.getInstance().getBukkitImplAdapter().sendFakeChunk(getWorld(), bukkitPlayer, packet);
    }

    @Override
    public void flush() {
        if (worldNativeAccess != null) {
            worldNativeAccess.flush();
        }
    }
    //FAWE end
}
