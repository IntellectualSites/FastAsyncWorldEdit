package com.boydti.fawe.bukkit.wrapper;

import com.bekvon.bukkit.residence.commands.material;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.HasFaweQueue;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.queue.DelegateFaweQueue;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.world.biome.BiomeType;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Consumer;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Modify the world from an async thread<br>
 *  - Use world.commit() to execute all the changes<br>
 *  - Any Chunk/Block/BlockState objects returned should also be safe to use from the same async thread<br>
 *  - Only block read,write and biome write are fast, other methods will perform slower async<br>
 *  -
 *  @see #wrap(World)
 *  @see #create(WorldCreator)
 */
public class AsyncWorld extends DelegateFaweQueue implements World, HasFaweQueue {

    private World parent;
    private FaweQueue queue;
    private BukkitImplAdapter adapter;

    @Override
    public <T> void spawnParticle(Particle particle, double v, double v1, double v2, int i, double v3, double v4, double v5, double v6, T t) {
        parent.spawnParticle(particle, v, v1, v2, i, v3, v4, v5, v6, t);
    }

    /**
     * @deprecated use {@link #wrap(World)} instead
     * @param parent Parent world
     * @param autoQueue
     */
    @Deprecated
    public AsyncWorld(World parent, boolean autoQueue) {
        this(parent, FaweAPI.createQueue(parent.getName(), autoQueue));
    }

    public AsyncWorld(String world, boolean autoQueue) {
        this(Bukkit.getWorld(world), autoQueue);
    }

    /**
     * @deprecated use {@link #wrap(World)} instead
     * @param parent
     * @param queue
     */
    @Deprecated
    public AsyncWorld(World parent, FaweQueue queue) {
        super(queue);
        this.parent = parent;
        this.queue = queue;
        if (queue instanceof BukkitQueue_0) {
            this.adapter = BukkitQueue_0.getAdapter();
        } else {
            try {
                this.adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Wrap a world for async usage
     * @param world
     * @return
     */
    public static AsyncWorld wrap(World world) {
        if (world instanceof AsyncWorld) {
            return (AsyncWorld) world;
        }
        return new AsyncWorld(world, false);
    }

    public void changeWorld(World world, FaweQueue queue) {
        this.parent = world;
        if (queue != this.queue) {
            if (this.queue != null) {
                final FaweQueue oldQueue = this.queue;
                TaskManager.IMP.async(oldQueue::flush);
            }
            this.queue = queue;
        }
        setParent(queue);
    }

    @Override
    public String toString() {
        return super.toString() + ":" + queue.toString();
    }

    public World getBukkitWorld() {
        return parent;
    }

    public FaweQueue getQueue() {
        return queue;
    }

    /**
     * Create a world async (untested)
     *  - Only optimized for 1.10
     * @param creator
     * @return
     */
    public synchronized static AsyncWorld create(final WorldCreator creator) {
        BukkitQueue_0 queue = (BukkitQueue_0) SetQueue.IMP.getNewQueue(creator.name(), true, false);
        World world = queue.createWorld(creator);
        return wrap(world);
    }

    public Operation commit() {
        flush();
        return null;
    }

    public void flush() {
        if (queue != null) {
            queue.flush();
        }
    }

    @Override
    public WorldBorder getWorldBorder() {
        return TaskManager.IMP.sync(new RunnableVal<WorldBorder>() {
            @Override
            public void run(WorldBorder value) {
                this.value = parent.getWorldBorder();
            }
        });
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int i) {
        parent.spawnParticle(particle, location, i);
    }

    @Override
    public void spawnParticle(Particle particle, double v, double v1, double v2, int i) {
        parent.spawnParticle(particle, v, v1, v2, i);
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int i, T t) {
        parent.spawnParticle(particle, location, i, t);
    }

    @Override
    public <T> void spawnParticle(Particle particle, double v, double v1, double v2, int i, T t) {
        parent.spawnParticle(particle, v, v1, v2, i, t);
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int i, double v, double v1, double v2) {
        parent.spawnParticle(particle, location, i, v, v1, v2);
    }

    @Override
    public void spawnParticle(Particle particle, double v, double v1, double v2, int i, double v3, double v4, double v5) {
        parent.spawnParticle(particle, v, v1, v2, i, v3, v4, v5);
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int i, double v, double v1, double v2, T t) {
        parent.spawnParticle(particle, location, i, v, v1, v2, t);
    }

    @Override
    public <T> void spawnParticle(Particle particle, double v, double v1, double v2, int i, double v3, double v4, double v5, T t) {
        parent.spawnParticle(particle, v, v1, v2, i, v3, v4, v5, t);
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int i, double v, double v1, double v2, double v3) {
        parent.spawnParticle(particle, location, i, v, v1, v2, v3);
    }

    @Override
    public void spawnParticle(Particle particle, double v, double v1, double v2, int i, double v3, double v4, double v5, double v6) {
        parent.spawnParticle(particle, v, v1, v2, i, v3, v4, v5, v6);
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int i, double v, double v1, double v2, double v3, T t) {
        parent.spawnParticle(particle, location, i, v, v1, v2, v3, t);
    }

    @Override
    public boolean setSpawnLocation(Location location) {
        return parent.setSpawnLocation(location);
    }

    @Override
    public AsyncBlock getBlockAt(final int x, final int y, final int z) {
        return new AsyncBlock(this, queue, x, y, z);
    }

    @Override
    public AsyncBlock getBlockAt(Location loc) {
        return getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    @Override
    public int getHighestBlockYAt(int x, int z) {
        for (int y = getMaxHeight() - 1; y >= 0; y--) {
            int stateId = queue.getCachedCombinedId4Data(x, y, z, BlockTypes.AIR.getInternalId());
            BlockType type = BlockTypes.getFromStateId(stateId);
            if (!type.getMaterial().isAir()) return y;
        }
        return 0;
    }

    @Override
    public int getHighestBlockYAt(Location loc) {
        return getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ());
    }

    @Override
    public AsyncBlock getHighestBlockAt(int x, int z) {
        int y = getHighestBlockYAt(x, z);
        return getBlockAt(x, y, z);
    }

    @Override
    public AsyncBlock getHighestBlockAt(Location loc) {
        return getHighestBlockAt(loc.getBlockX(), loc.getBlockZ());
    }

    @Override
    public AsyncChunk getChunkAt(int x, int z) {
        return new AsyncChunk(this, queue, x, z);
    }

    @Override
    public AsyncChunk getChunkAt(Location location) {
        return getChunkAt(location.getBlockX(), location.getBlockZ());
    }

    @Override
    public AsyncChunk getChunkAt(Block block) {
        return getChunkAt(block.getX(), block.getZ());
    }

    @Override
    public boolean isChunkGenerated(int x, int z) {
        return parent.isChunkGenerated(x, z);
    }

    @Override
    public boolean isChunkLoaded(Chunk chunk) {
        return chunk.isLoaded();
    }

    @Override
    public Chunk[] getLoadedChunks() {
        return parent.getLoadedChunks();
    }

    @Override
    public void loadChunk(final Chunk chunk) {
        if (!chunk.isLoaded()) {
            TaskManager.IMP.sync(new RunnableVal<Object>() {
                @Override
                public void run(Object value) {
                    parent.loadChunk(chunk);
                }
            });
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof World)) {
            return false;
        }
        World other = (World) obj;
        return StringMan.isEqual(other.getName(), getName());
    }

    @Override
    public int hashCode() {
        return this.getUID().hashCode();
    }

    @Override
    public boolean isChunkLoaded(int x, int z) {
        return parent.isChunkLoaded(x, z);
    }

    @Override
    public boolean isChunkInUse(int x, int z) {
        return parent.isChunkInUse(x, z);
    }

    @Override
    public void loadChunk(final int x, final int z) {
        if (!isChunkLoaded(x, z)) {
            TaskManager.IMP.sync(new RunnableVal<Object>() {
                @Override
                public void run(Object value) {
                    parent.loadChunk(x, z);
                }
            });
        }
    }

    @Override
    public boolean loadChunk(final int x, final int z, final boolean generate) {
        if (!isChunkLoaded(x, z)) {
            return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
                @Override
                public void run(Boolean value) {
                    this.value = parent.loadChunk(x, z, generate);
                }
            });
        }
        return true;
    }

    @Override
    public boolean unloadChunk(final Chunk chunk) {
        if (chunk.isLoaded()) {
            return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
                @Override
                public void run(Boolean value) {
                    this.value = parent.unloadChunk(chunk);
                }
            });
        }
        return true;
    }

    @Override
    public boolean unloadChunk(int x, int z) {
        return unloadChunk(x, z, true);
    }

    @Override
    public boolean unloadChunk(int x, int z, boolean save) {
        if (isChunkLoaded(x, z)) {
            return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
                @Override
                public void run(Boolean value) {
                    this.value = parent.unloadChunk(x, z, save);
                }
            });
        }
        return true;
    }

    @Override
    public boolean unloadChunkRequest(int x, int z) {
        if (isChunkLoaded(x, z)) {
            return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
                @Override
                public void run(Boolean value) {
                    this.value = parent.unloadChunkRequest(x, z);
                }
            });
        }
        return true;
    }

    @Override
    public boolean regenerateChunk(final int x, final int z) {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
               this.value = parent.regenerateChunk(x, z);
            }
        });
    }

    @Override
    @Deprecated
    public boolean refreshChunk(int x, int z) {
        queue.sendChunk(queue.getFaweChunk(x, z));
        return true;
    }

    @Override
    public Item dropItem(final Location location, final ItemStack item) {
        return TaskManager.IMP.sync(new RunnableVal<Item>() {
            @Override
            public void run(Item value) {
                this.value = parent.dropItem(location, item);
            }
        });
    }

    @Override
    public Item dropItemNaturally(final Location location, final ItemStack item) {
        return TaskManager.IMP.sync(new RunnableVal<Item>() {
            @Override
            public void run(Item value) {
                this.value = parent.dropItemNaturally(location, item);
            }
        });
    }

    @Override
    public Arrow spawnArrow(final Location location, final Vector direction, final float speed, final float spread) {
        return TaskManager.IMP.sync(new RunnableVal<Arrow>() {
            @Override
            public void run(Arrow value) {
                this.value = parent.spawnArrow(location, direction, speed, spread);
            }
        });
    }

    @Override
    public <T extends AbstractArrow> @NotNull T spawnArrow(@NotNull Location location, @NotNull Vector direction, float speed, float spread, @NotNull Class<T> clazz) {
        return parent.spawnArrow(location, direction, speed, spread, clazz);
    }

    @Override
    public boolean generateTree(final Location location, final TreeType type) {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                this.value = parent.generateTree(location, type);
            }
        });
    }

    @Override
    public boolean generateTree(final Location loc, final TreeType type, final BlockChangeDelegate delegate) {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                this.value = parent.generateTree(loc, type, delegate);
            }
        });
    }

    @Override
    public Entity spawnEntity(Location loc, EntityType type) {
        return spawn(loc, type.getEntityClass());
    }

    @Override
    public LightningStrike strikeLightning(final Location loc) {
        return TaskManager.IMP.sync(new RunnableVal<LightningStrike>() {
            @Override
            public void run(LightningStrike value) {
                this.value = parent.strikeLightning(loc);
            }
        });
    }

    @Override
    public LightningStrike strikeLightningEffect(final Location loc) {
        return TaskManager.IMP.sync(new RunnableVal<LightningStrike>() {
            @Override
            public void run(LightningStrike value) {
                this.value = parent.strikeLightningEffect(loc);
            }
        });
    }

    @Override
    public List getEntities() {
        return TaskManager.IMP.sync(new RunnableVal<List<Entity>>() {
            @Override
            public void run(List<Entity> value) {
                this.value = parent.getEntities();
            }
        });
    }

    @Override
    public List<LivingEntity> getLivingEntities() {
        return TaskManager.IMP.sync(new RunnableVal<List<LivingEntity>>() {
            @Override
            public void run(List<LivingEntity> value) {
                this.value = parent.getLivingEntities();
            }
        });
    }

    @Override
    @Deprecated
    public <T extends Entity> Collection<T> getEntitiesByClass(final Class<T>... classes) {
        return TaskManager.IMP.sync(new RunnableVal<Collection<T>>() {
            @Override
            public void run(Collection<T> value) {
                this.value = parent.getEntitiesByClass(classes);
            }
        });
    }

    @Override
    public <T extends Entity> Collection<T> getEntitiesByClass(final Class<T> cls) {
        return TaskManager.IMP.sync(new RunnableVal<Collection<T>>() {
            @Override
            public void run(Collection<T> value) {
                this.value = parent.getEntitiesByClass(cls);
            }
        });
    }

    @Override
    public Collection<Entity> getEntitiesByClasses(final Class<?>... classes) {
        return TaskManager.IMP.sync(new RunnableVal<Collection<Entity>>() {
            @Override
            public void run(Collection<Entity> value) {
                this.value = parent.getEntitiesByClasses(classes);
            }
        });
    }

    @Override
    public List<Player> getPlayers() {
        return TaskManager.IMP.sync(new RunnableVal<List<Player>>() {
            @Override
            public void run(List<Player> value) {
                this.value = parent.getPlayers();
            }
        });
    }

    @Override
    public Collection<Entity> getNearbyEntities(final Location location, final double x, final double y, final double z) {
        return TaskManager.IMP.sync(new RunnableVal<Collection<Entity>>() {
            @Override
            public void run(Collection<Entity> value) {
                this.value = parent.getNearbyEntities(location, x, y, z);
            }
        });
    }

    @Override
    public String getName() {
        return parent.getName();
    }

    @Override
    public UUID getUID() {
        return parent.getUID();
    }

    @Override
    public Location getSpawnLocation() {
        return parent.getSpawnLocation();
    }

    @Override
    public boolean setSpawnLocation(final int x, final int y, final int z) {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                this.value = parent.setSpawnLocation(x, y, z);
            }
        });
    }

    @Override
    public long getTime() {
        return parent.getTime();
    }

    @Override
    public void setTime(long time) {
        parent.setTime(time);
    }

    @Override
    public long getFullTime() {
        return parent.getFullTime();
    }

    @Override
    public void setFullTime(long time) {
        parent.setFullTime(time);
    }

    @Override
    public boolean hasStorm() {
        return parent.hasStorm();
    }

    @Override
    public void setStorm(boolean hasStorm) {
        parent.setStorm(hasStorm);
    }

    @Override
    public int getWeatherDuration() {
        return parent.getWeatherDuration();
    }

    @Override
    public void setWeatherDuration(int duration) {
        parent.setWeatherDuration(duration);
    }

    @Override
    public boolean isThundering() {
        return parent.isThundering();
    }

    @Override
    public void setThundering(boolean thundering) {
        parent.setThundering(thundering);
    }

    @Override
    public int getThunderDuration() {
        return parent.getThunderDuration();
    }

    @Override
    public void setThunderDuration(int duration) {
        parent.setThunderDuration(duration);
    }

    public boolean createExplosion(double x, double y, double z, float power) {
        return this.createExplosion(x, y, z, power, false, true);
    }

    public boolean createExplosion(double x, double y, double z, float power, boolean setFire) {
        return this.createExplosion(x, y, z, power, setFire, true);
    }

    public boolean createExplosion(final double x, final double y, final double z, final float power, final boolean setFire, final boolean breakBlocks) {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                this.value = parent.createExplosion(x, y, z, power, setFire, breakBlocks);
            }
        });
    }

    public boolean createExplosion(Location loc, float power) {
        return this.createExplosion(loc, power, false);
    }

    public boolean createExplosion(Location loc, float power, boolean setFire) {
        return this.createExplosion(loc.getX(), loc.getY(), loc.getZ(), power, setFire);
    }

    @Override
    public Environment getEnvironment() {
        return parent.getEnvironment();
    }

    @Override
    public long getSeed() {
        return parent.getSeed();
    }

    @Override
    public boolean getPVP() {
        return parent.getPVP();
    }

    @Override
    public void setPVP(boolean pvp) {
        parent.setPVP(pvp);
    }

    @Override
    public ChunkGenerator getGenerator() {
        return parent.getGenerator();
    }

    @Override
    public void save() {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.save();
            }
        });
    }

    @Override
    public List<BlockPopulator> getPopulators() {
        return parent.getPopulators();
    }

    @Override
    public <T extends Entity> T spawn(final Location location, final Class<T> clazz) throws IllegalArgumentException {
        return TaskManager.IMP.sync(new RunnableVal<T>() {
            @Override
            public void run(T value) {
                this.value = parent.spawn(location, clazz);
            }
        });
    }

    @Override
    public <T extends Entity> T spawn(Location location, Class<T> clazz, Consumer<T> function) throws IllegalArgumentException {
        return TaskManager.IMP.sync(new RunnableVal<T>() {
            @Override
            public void run(T value) {
                this.value = parent.spawn(location, clazz, function);
            }
        });
    }

    @Override
    public FallingBlock spawnFallingBlock(Location location, MaterialData data) throws IllegalArgumentException {
        return TaskManager.IMP.sync(new RunnableVal<FallingBlock>() {
            @Override
            public void run(FallingBlock value) {
                this.value = parent.spawnFallingBlock(location, data);
            }
        });
    }

    @Override
    @Deprecated
    public FallingBlock spawnFallingBlock(Location location, Material material, byte data) throws IllegalArgumentException {
        return TaskManager.IMP.sync(() -> parent.spawnFallingBlock(location, material, data));
    }

    @Override
    public FallingBlock spawnFallingBlock(Location location, BlockData blockData) throws IllegalArgumentException {
        return TaskManager.IMP.sync(() -> parent.spawnFallingBlock(location, blockData));
    }

    @Override
    public void playEffect(Location location, Effect effect, int data) {
        this.playEffect(location, effect, data, 64);
    }

    @Override
    public void playEffect(final Location location, final Effect effect, final int data, final int radius) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.playEffect(location, effect, data, radius);
            }
        });
    }

    @Override
    public <T> void playEffect(Location loc, Effect effect, T data) {
        this.playEffect(loc, effect, data, 64);
    }

    @Override
    public <T> void playEffect(final Location location, final Effect effect, final T data, final int radius) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.playEffect(location, effect, data, radius);
            }
        });
    }

    @Override
    public ChunkSnapshot getEmptyChunkSnapshot(final int x, final int z, final boolean includeBiome, final boolean includeBiomeTempRain) {
        return TaskManager.IMP.sync(new RunnableVal<ChunkSnapshot>() {
            @Override
            public void run(ChunkSnapshot value) {
                this.value = parent.getEmptyChunkSnapshot(x, z, includeBiome, includeBiomeTempRain);
            }
        });
    }

    @Override
    public void setSpawnFlags(boolean allowMonsters, boolean allowAnimals) {
        parent.setSpawnFlags(allowMonsters, allowAnimals);
    }

    @Override
    public boolean getAllowAnimals() {
        return parent.getAllowAnimals();
    }

    @Override
    public boolean getAllowMonsters() {
        return parent.getAllowMonsters();
    }

    @Override
    public Biome getBiome(int x, int z) {
        return adapter.adapt(queue.getBiomeType(x, z));
    }

    @Override
    public void setBiome(int x, int z, Biome bio) {
        BiomeType biome = adapter.adapt(bio);
        queue.setBiome(x, z, biome);
    }

    @Override
    public double getTemperature(int x, int z) {
        return parent.getTemperature(x, z);
    }

    @Override
    public double getHumidity(int x, int z) {
        return parent.getHumidity(x, z);
    }

    @Override
    public int getMaxHeight() {
        return parent.getMaxHeight();
    }

    @Override
    public int getSeaLevel() {
        return parent.getSeaLevel();
    }

    @Override
    public boolean getKeepSpawnInMemory() {
        return parent.getKeepSpawnInMemory();
    }

    @Override
    public void setKeepSpawnInMemory(final boolean keepLoaded) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.setKeepSpawnInMemory(keepLoaded);
            }
        });
    }

    @Override
    public boolean isAutoSave() {
        return parent.isAutoSave();
    }

    @Override
    public void setAutoSave(boolean value) {
        parent.setAutoSave(value);
    }

    @Override
    public void setDifficulty(Difficulty difficulty) {
        parent.setDifficulty(difficulty);
    }

    @Override
    public Difficulty getDifficulty() {
        return parent.getDifficulty();
    }

    @Override
    public File getWorldFolder() {
        return parent.getWorldFolder();
    }

    @Override
    public WorldType getWorldType() {
        return parent.getWorldType();
    }

    @Override
    public boolean canGenerateStructures() {
        return parent.canGenerateStructures();
    }

    @Override
    public long getTicksPerAnimalSpawns() {
        return parent.getTicksPerAnimalSpawns();
    }

    @Override
    public void setTicksPerAnimalSpawns(int ticksPerAnimalSpawns) {
        parent.setTicksPerAnimalSpawns(ticksPerAnimalSpawns);
    }

    @Override
    public long getTicksPerMonsterSpawns() {
        return parent.getTicksPerMonsterSpawns();
    }

    @Override
    public void setTicksPerMonsterSpawns(int ticksPerMonsterSpawns) {
        parent.setTicksPerMonsterSpawns(ticksPerMonsterSpawns);
    }

    @Override
    public int getMonsterSpawnLimit() {
        return parent.getMonsterSpawnLimit();
    }

    @Override
    public void setMonsterSpawnLimit(int limit) {
        parent.setMonsterSpawnLimit(limit);
    }

    @Override
    public int getAnimalSpawnLimit() {
        return parent.getAnimalSpawnLimit();
    }

    @Override
    public void setAnimalSpawnLimit(int limit) {
        parent.setAnimalSpawnLimit(limit);
    }

    @Override
    public int getWaterAnimalSpawnLimit() {
        return parent.getWaterAnimalSpawnLimit();
    }

    @Override
    public void setWaterAnimalSpawnLimit(int limit) {
        parent.setWaterAnimalSpawnLimit(limit);
    }

    @Override
    public int getAmbientSpawnLimit() {
        return parent.getAmbientSpawnLimit();
    }

    @Override
    public void setAmbientSpawnLimit(int limit) {
        parent.setAmbientSpawnLimit(limit);
    }

    @Override
    public void playSound(final Location location, final Sound sound, final float volume, final float pitch) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.playSound(location, sound, volume, pitch);
            }
        });
    }

    @Override
    public void playSound(final Location location, final String sound, final float volume, final float pitch) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.playSound(location, sound, volume, pitch);
            }
        });
    }

    @Override
    public void playSound(Location location, Sound sound, SoundCategory category, float volume, float pitch) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.playSound(location, sound, category, volume, pitch);
            }
        });
    }

    @Override
    public void playSound(Location location, String sound, SoundCategory category, float volume, float pitch) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.playSound(location, sound, category, volume, pitch);
            }
        });
    }

    @Override
    public String[] getGameRules() {
        return parent.getGameRules();
    }

    @Override
    public String getGameRuleValue(String rule) {
        return parent.getGameRuleValue(rule);
    }

    @Override
    public boolean setGameRuleValue(String rule, String value) {
        return parent.setGameRuleValue(rule, value);
    }

    @Override
    public boolean isGameRule(String rule) {
        return parent.isGameRule(rule);
    }

    @Override
    public <T> T getGameRuleValue(GameRule<T> gameRule) {
        return parent.getGameRuleValue(gameRule);
    }

    @Override
    public <T> T getGameRuleDefault(GameRule<T> gameRule) {
        return parent.getGameRuleDefault(gameRule);
    }

    @Override
    public <T> boolean setGameRule(GameRule<T> gameRule, T t) {
        return parent.setGameRule(gameRule, t);
    }

    @Override
    public Spigot spigot() {
        return parent.spigot();
    }

    @Override
    public void setMetadata(final String key, final MetadataValue meta) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.setMetadata(key, meta);
            }
        });
    }

    @Override
    public List<MetadataValue> getMetadata(String key) {
        return parent.getMetadata(key);
    }

    @Override
    public boolean hasMetadata(String key) {
        return parent.hasMetadata(key);
    }

    @Override
    public void removeMetadata(final String key, final Plugin plugin) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.removeMetadata(key, plugin);
            }
        });
    }

    @Override
    public void sendPluginMessage(Plugin source, String channel, byte[] message) {
        parent.sendPluginMessage(source, channel, message);
    }

    @Override
    public Set<String> getListeningPluginChannels() {
        return parent.getListeningPluginChannels();
    }

    public BukkitImplAdapter getAdapter() {
        return adapter;
    }

	@Override
	public Collection<Entity> getNearbyEntities(BoundingBox arg0) {
		return parent.getNearbyEntities(arg0);
	}

	@Override
	public Collection<Entity> getNearbyEntities(BoundingBox arg0, Predicate<Entity> arg1) {
		return parent.getNearbyEntities(arg0, arg1);
	}

	@Override
	public Collection<Entity> getNearbyEntities(Location arg0, double arg1, double arg2, double arg3,
			Predicate<Entity> arg4) {
		return parent.getNearbyEntities(arg0, arg1, arg2, arg3, arg4);
	}

	@Override
	public boolean isChunkForceLoaded(int arg0, int arg1) {
		return parent.isChunkForceLoaded(arg0, arg1);
	}

	@Override
	public Location locateNearestStructure(Location arg0, StructureType arg1, int arg2, boolean arg3) {
		return parent.locateNearestStructure(arg0, arg1, arg2, arg3);
	}

	@Override
	public RayTraceResult rayTrace(Location arg0, Vector arg1, double arg2, FluidCollisionMode arg3, boolean arg4,
			double arg5, Predicate<Entity> arg6) {
		return parent.rayTrace(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
	}

	@Override
	public RayTraceResult rayTraceBlocks(Location arg0, Vector arg1, double arg2) {
		return parent.rayTraceBlocks(arg0, arg1, arg2);
	}

	@Override
	public RayTraceResult rayTraceBlocks(Location arg0, Vector arg1, double arg2, FluidCollisionMode arg3) {
		return parent.rayTraceBlocks(arg0, arg1, arg2, arg3);
	}

	@Override
	public RayTraceResult rayTraceBlocks(Location arg0, Vector arg1, double arg2, FluidCollisionMode arg3,
			boolean arg4) {
		return parent.rayTraceBlocks(arg0, arg1, arg2, arg3, arg4);
	}

	@Override
	public RayTraceResult rayTraceEntities(Location arg0, Vector arg1, double arg2) {
		return parent.rayTraceEntities(arg0, arg1, arg2);
	}

	@Override
	public RayTraceResult rayTraceEntities(Location arg0, Vector arg1, double arg2, double arg3) {
		return parent.rayTraceEntities(arg0, arg1, arg2, arg3);
	}

	@Override
	public RayTraceResult rayTraceEntities(Location arg0, Vector arg1, double arg2, Predicate<Entity> arg3) {
		return parent.rayTraceEntities(arg0, arg1, arg2, arg3);
	}

	@Override
	public RayTraceResult rayTraceEntities(Location arg0, Vector arg1, double arg2, double arg3,
			Predicate<Entity> arg4) {
		return parent.rayTraceEntities(arg0, arg1, arg2, arg3, arg4);
	}

	@Override
	public <T> void spawnParticle(Particle arg0, Location arg1, int arg2, double arg3, double arg4, double arg5,
			double arg6, T arg7, boolean arg8) {
		parent.spawnParticle(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
	}

	@Override
	public <T> void spawnParticle(Particle arg0, double arg1, double arg2, double arg3, int arg4, double arg5,
			double arg6, double arg7, double arg8, T arg9, boolean arg10) {
		parent.spawnParticle(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
		
	}

	@Override
	public void setChunkForceLoaded(int x, int z, boolean forced) {
		parent.setChunkForceLoaded(x, z, forced);
	}

	@Override
	public Collection<Chunk> getForceLoadedChunks() {
		return parent.getForceLoadedChunks();
	}

    @Override
    public boolean addPluginChunkTicket(int x, int z, @NotNull Plugin plugin) {
        return getBukkitWorld().addPluginChunkTicket(x, z, plugin);
    }

    @Override
    public boolean removePluginChunkTicket(int x, int z, @NotNull Plugin plugin) {
        return getBukkitWorld().removePluginChunkTicket(x, z, plugin);
    }

    @Override
    public void removePluginChunkTickets(@NotNull Plugin plugin) {
        getBukkitWorld().removePluginChunkTickets(plugin);
    }

    @Override
    public @NotNull Collection<Plugin> getPluginChunkTickets(int x, int z) {
        return getBukkitWorld().getPluginChunkTickets(x, z);
    }

    @Override
    public @NotNull Map<Plugin, Collection<Chunk>> getPluginChunkTickets() {
        return getBukkitWorld().getPluginChunkTickets();
    }

    @Override
    public int getHighestBlockYAt(int x, int z, com.destroystokyo.paper.HeightmapType heightmap) throws UnsupportedOperationException {
        return TaskManager.IMP.sync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                return parent.getHighestBlockYAt(x, z, heightmap);
            }
        });
    }

    @Override
    public int getEntityCount() {
        return TaskManager.IMP.sync(new RunnableVal<Integer>() {
            @Override
            public void run(Integer value) {
                this.value = parent.getEntityCount();
            }
        });
    }

    @Override
    public int getTileEntityCount() {
        return TaskManager.IMP.sync(new RunnableVal<Integer>() {
            @Override
            public void run(Integer value) {
                this.value = parent.getTileEntityCount();
            }
        });
    }

    @Override
    public int getTickableTileEntityCount() {
        return TaskManager.IMP.sync(new RunnableVal<Integer>() {
            @Override
            public void run(Integer value) {
                this.value = parent.getTickableTileEntityCount();
            }
        });
    }

    @Override
    public int getChunkCount() {
        return TaskManager.IMP.sync(new RunnableVal<Integer>() {
            @Override
            public void run(Integer value) {
                this.value = parent.getChunkCount();
            }
        });
    }

    @Override
    public int getPlayerCount() {
        return TaskManager.IMP.sync(new RunnableVal<Integer>() {
            @Override
            public void run(Integer value) {
                this.value = parent.getPlayerCount();
            }
        });
    }

    @Override
    public CompletableFuture<Chunk> getChunkAtAsync(int arg0, int arg1, boolean arg2) {
        return parent.getChunkAtAsync(arg0, arg1, arg2);
    }

    @Override
    public boolean isDayTime() {
        return parent.isDayTime();
    }

    @Override
    public void getChunkAtAsync(int x, int z, ChunkLoadCallback cb) {
        parent.getChunkAtAsync(x, z, cb);
    }

    @Override
    public void getChunkAtAsync(Location location, ChunkLoadCallback cb) {
        parent.getChunkAtAsync(location, cb);
    }

    @Override
    public void getChunkAtAsync(Block block, ChunkLoadCallback cb) {
        parent.getChunkAtAsync(block, cb);
    }

    @Override
    public Entity getEntity(UUID uuid) {
        return TaskManager.IMP.sync(() -> parent.getEntity(uuid));
    }


    @Override
    public boolean createExplosion(Entity source, Location loc, float power, boolean setFire, boolean breakBlocks) {
        return TaskManager.IMP.sync(() -> parent.createExplosion(source, loc, power, setFire, breakBlocks));
    }


    @Override
    public <T> void spawnParticle(Particle particle, List<Player> receivers, Player source, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, T data) {
        parent.spawnParticle(particle, receivers, source, x, y, z, count, offsetX, offsetY, offsetZ, extra, data);
    }

    @Override
    public <T> void spawnParticle(Particle particle, List<Player> list, Player player, double v, double v1, double v2, int i, double v3, double v4, double v5, double v6, T t, boolean b) {
        parent.spawnParticle(particle, list, player, v, v1, v2, i, v3, v4, v5, v6, t, b);
    }
}
