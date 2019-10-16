package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.IntFaweChunk;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.entity.EntityTypes;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BukkitChunk_All extends IntFaweChunk<Chunk, BukkitQueue_All> {

    private int layer = -1;
    private int index;
    private boolean place = true;

    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     *
     * @param parent
     * @param x
     * @param z
     */
    public BukkitChunk_All(FaweQueue parent, int x, int z) {
        super(parent, x, z);
    }

    public BukkitChunk_All(FaweQueue parent, int x, int z, int[][] ids, short[] count, short[] air) {
        super(parent, x, z, ids, count, air);
    }

    private static boolean canTick(BlockType type) {
        return type.getMaterial().isTicksRandomly();
    }

    @Override
    public IntFaweChunk copy(boolean shallow) {
        BukkitChunk_All copy;
        if (shallow) {
            copy = new BukkitChunk_All(getParent(), getX(), getZ(), setBlocks, count, air);
            copy.biomes = biomes;
        } else {
            copy = new BukkitChunk_All(getParent(), getX(), getZ(), (int[][]) MainUtil.copyNd(setBlocks), count.clone(), air.clone());
            copy.biomes = biomes != null ? biomes.clone() : null;
        }
        copy.chunk = chunk;
        return copy;
    }

    @Override
    public Chunk getNewChunk() {
        return Bukkit.getWorld(getParent().getWorldName()).getChunkAt(getX(), getZ());
    }

    @Override
    public void start() {
        getChunk().load(true);
    }

    /**
     * @return
     */
    @Override
    public FaweChunk call() {
        long start = System.currentTimeMillis();
        int recommended = 25 + Settings.IMP.QUEUE.EXTRA_TIME_MS;
        boolean more = true;
        final BukkitQueue_All parent = getParent();
        BukkitImplAdapter adapter = BukkitQueue_0.getAdapter();
        final Chunk chunk = getChunk();
        Object[] disableResult = parent.disableLighting(chunk);
        final World world = chunk.getWorld();
        int[][] sections = getCombinedIdArrays();
        final int bx = getX() << 4;
        final int bz = getZ() << 4;
        boolean update = adapter == null || adapter.isChunkInUse(chunk);
        if (layer == -1) {
            if (adapter != null) {
                // Run change task
                RunnableVal2<FaweChunk, FaweChunk> task = parent.getChangeTask();
                BukkitChunk_All_ReadonlySnapshot previous;
                if (task != null) {
                    ChunkSnapshot snapshot = parent.ensureChunkLoaded(getX(), getZ());
                    previous = new BukkitChunk_All_ReadonlySnapshot(parent, this, snapshot, biomes != null);
                    for (BlockState tile : chunk.getTileEntities()) {
                        int x = tile.getX();
                        int y = tile.getY();
                        int z = tile.getZ();
                        if (getBlockCombinedId(x & 15, y, z & 15) != 0) {
                            CompoundTag nbt = adapter.getBlock(new Location(world, x, y, z)).getNbtData();
                            if (nbt != null) {
                                previous.setTile(x & 15, y, z & 15, nbt);
                            }
                        }
                    }
                } else {
                    previous = null;
                }
                // Set entities
                if (adapter != null) {
                    Set<CompoundTag> entitiesToSpawn = this.getEntities();
                    if (!entitiesToSpawn.isEmpty()) {
                        for (CompoundTag tag : entitiesToSpawn) {
                            String id = tag.getString("Id");
                            ListTag posTag = tag.getListTag("Pos");
                            ListTag rotTag = tag.getListTag("Rotation");
                            if (id == null || posTag == null || rotTag == null) {
                                Fawe.debug("Unknown entity tag: " + tag);
                                continue;
                            }
                            double x = posTag.getDouble(0);
                            double y = posTag.getDouble(1);
                            double z = posTag.getDouble(2);
                            float yaw = rotTag.getFloat(0);
                            float pitch = rotTag.getFloat(1);
                            Location loc = new Location(world, x, y, z, yaw, pitch);
                            Entity created = adapter.createEntity(loc, new BaseEntity(EntityTypes.get(id), tag));
                            if (created != null) {
                                UUID uuid = created.getUniqueId();
                                Map<String, Tag> map = ReflectionUtils.getMap(tag.getValue());
                                map.put("UUIDLeast", new LongTag(uuid.getLeastSignificantBits()));
                                map.put("UUIDMost", new LongTag(uuid.getMostSignificantBits()));
                            }
                        }
                    }
                    HashSet<UUID> entsToRemove = this.getEntityRemoves();
                    if (!entsToRemove.isEmpty()) {
                        for (Entity entity : chunk.getEntities()) {
                            if (entsToRemove.contains(entity.getUniqueId()) && !(entity instanceof Player)) {
                                entity.remove();
                            }
                        }
                    }
                }
                if (previous != null) {
                    task.run(previous, this);
                }
            }

            // Biomes
            final BiomeType[] biomes = getBiomeArray();
            if (biomes != null) {
                int index = 0;
                for (int z = 0; z < 16; z++) {
                    int zz = bz + z;
                    for (int x = 0; x < 16; x++, index++) {
                        int xx = bx + x;
                        BiomeType biome = biomes[index];
                        if (biome == null) continue;
                        Biome bukkitBiome = adapter.adapt(biome);
                        if (bukkitBiome != null) {
                            world.setBiome(xx, zz, bukkitBiome);
                        }
                    }
                }
            }
        } else if (index != 0) {
            if (place) {
                layer--;
            } else {
                layer++;
            }
        }
        mainloop:
        do {
            if (place) {
                if (++layer >= sections.length) {
                    place = false;
                    layer = sections.length - 1;
                }
            } else if (--layer < 0) {
                more = false;
                break;
            }
            try {
                // Efficiently merge sections
                int changes = getCount(layer);
                if (changes == 0) {
                    continue;
                }
                final int[] newArray = sections[layer];
                if (newArray == null) {
                    continue;
                }
//                final byte[] cacheX = FaweCache.CACHE_X[layer];
//                final short[] cacheY = FaweCache.CACHE_Y[layer];
//                final byte[] cacheZ = FaweCache.CACHE_Z[layer];
                boolean checkTime = !((getAir(layer) == 4096 || (getCount(layer) == 4096 && getAir(layer) == 0) || (getCount(layer) == getAir(layer))));

                Location mutableLoc = new Location(world, 0, 0, 0);

                if (!checkTime) {
                    int index = 0;
                    for (int y = 0; y < 16; y++) {
                        int yy = (layer << 4) + y;
                        for (int z = 0; z < 16; z++) {
                            int zz = bz + z;
                            for (int x = 0; x < 16; x++, index++) {
                                int combined = newArray[index];
                                if (combined == 0) continue;
                                int xx = bx + x;

                                BlockType type = BlockTypes.getFromStateId(combined);
                                if (type == BlockTypes.__RESERVED__) continue;
                                if (type.getMaterial().isAir()) {
                                    if (!place) {
                                        mutableLoc.setX(xx);
                                        mutableLoc.setY(yy);
                                        mutableLoc.setZ(zz);
                                        setBlock(adapter, chunk, mutableLoc, combined, update);
                                    }
                                    continue;
                                }
                                if (place) {
                                    if (type.getMaterial().hasContainer() && adapter != null) {
                                        CompoundTag nbt = getTile(x, yy, z);
                                        if (nbt != null) {
                                            synchronized (BukkitChunk_All.this) {
                                                BaseBlock state =
                                                        BaseBlock.getFromInternalId(combined, nbt);
                                                adapter.setBlock(chunk, xx, yy, zz, state, update);
                                            }
                                            continue;
                                        }
                                    }
                                    if (type.getMaterial().isTicksRandomly()) {
                                        synchronized (BukkitChunk_All.this) {
                                            setBlock(adapter, chunk, mutableLoc, combined, update);
                                        }
                                    } else {
                                        mutableLoc.setX(xx);
                                        mutableLoc.setY(yy);
                                        mutableLoc.setZ(zz);
                                        setBlock(adapter, chunk, mutableLoc, combined, update);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    int yStart = layer << 4;
                    for (; index < 4096; index++) {
                        int j = place ? index : 4095 - index;
                        int combined = newArray[j];
                        if (combined == 0) continue;
                        BlockType type = BlockTypes.getFromStateId(combined);
                        if (type.getMaterial().isAir()) {
                            if (!place) {
                                int x = j & 15;
                                int y = yStart + (j >> 8);
                                int z = (j >> 4) & 15;
                                mutableLoc.setX(bx + x);
                                mutableLoc.setY(y);
                                mutableLoc.setZ(bz + z);
                                setBlock(adapter, chunk, mutableLoc, combined, update);
                            }
                            continue;
                        } else {
                            boolean light = type.getMaterial().getLightValue() > 0;
                            if (light) {
                                if (place) {
                                    continue;
                                }
                                light = light && getParent().getSettings().LIGHTING.MODE != 0;
                                if (light) {
                                    parent.enableLighting(disableResult);
                                }
                            } else if (!place) {
                                continue;
                            }
                            int x = j & 15;
                            int y = yStart + (j >> 8);
                            int z = (j >> 4) & 15;
                            if (type.getMaterial().hasContainer() && adapter != null) {
                                CompoundTag tile = getTile(x, y, z);
                                if (tile != null) {
                                    synchronized (BukkitChunk_All.this) {
                                        BaseBlock state = BaseBlock.getFromInternalId(combined, tile);
                                        adapter.setBlock(chunk, bx + x, y, bz + z, state, update);
                                    }
                                    continue;
                                }
                            }
                            if (type.getMaterial().isTicksRandomly()) {
                                synchronized (BukkitChunk_All.this) {
                                    mutableLoc.setX(bx + x);
                                    mutableLoc.setY(y);
                                    mutableLoc.setZ(bz + z);
                                    setBlock(adapter, chunk, mutableLoc, combined, update);
                                }
                            } else {
                                mutableLoc.setX(bx + x);
                                mutableLoc.setY(y);
                                mutableLoc.setZ(bz + z);
                                setBlock(adapter, chunk, mutableLoc, combined, update);
                            }
                            if (light) {
                                parent.disableLighting(disableResult);
                            }
                        }
                        if (System.currentTimeMillis() - start > recommended) {
                            index++;
                            break mainloop;
                        }
                    }
                    index = 0;
                }
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        } while (System.currentTimeMillis() - start < recommended);
        if (more || place) {
            this.addToQueue();
        }
        parent.resetLighting(disableResult);
        return this;
    }

    public void setBlock(BukkitImplAdapter adapter, Chunk chunk, Location location, int combinedId, boolean update) {
        com.sk89q.worldedit.world.block.BaseBlock base = com.sk89q.worldedit.world.block.BlockState.getFromInternalId(combinedId).toBaseBlock();
        if (adapter != null) {
            adapter.setBlock(chunk, (int) location.getX(), (int) location.getY(), (int) location.getZ(), base, update);
        } else {
            Block block = location.getWorld().getBlockAt(location);
            block.setBlockData(BukkitAdapter.adapt(base), false);
        }
    }
}
