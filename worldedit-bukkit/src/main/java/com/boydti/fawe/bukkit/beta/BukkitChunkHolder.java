package com.boydti.fawe.bukkit.beta;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.IGetBlocks;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.ISetBlocks;
import com.boydti.fawe.beta.implementation.QueueHandler;
import com.boydti.fawe.beta.implementation.holder.ChunkHolder;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.bukkit.v1_13.BukkitQueue_1_13;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.world.biome.BiomeType;
import net.minecraft.server.v1_13_R2.BiomeBase;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.Chunk;
import net.minecraft.server.v1_13_R2.ChunkSection;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.EntityTypes;
import net.minecraft.server.v1_13_R2.MinecraftKey;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.NBTTagInt;
import net.minecraft.server.v1_13_R2.TileEntity;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlock;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class BukkitChunkHolder<T extends Future<T>> extends ChunkHolder {
    @Override
    public void init(final IQueueExtent extent, final int X, final int Z) {
        super.init(extent, X, Z);
    }

    @Override
    public IGetBlocks get() {
        BukkitQueue extent = (BukkitQueue) getExtent();
        return new BukkitGetBlocks(extent.getNmsWorld(), getX(), getZ());
    }

    private void updateGet(BukkitGetBlocks get, Chunk nmsChunk, ChunkSection[] sections, ChunkSection section, char[] arr, int layer) {
        synchronized (get) {
            if (get.nmsChunk != nmsChunk) {
                get.nmsChunk = nmsChunk;
                get.sections = sections.clone();
                get.reset();
            }
            if (get.sections == null) {
                get.sections = sections.clone();
            }
            if (get.sections[layer] != section) {
                get.sections[layer] = section;
            }
            get.blocks[layer] = arr;
        }
    }

    @Override
    public synchronized T call() {
        try {
            int X = getX();
            int Z = getZ();
            BukkitQueue extent = (BukkitQueue) getExtent();
            BukkitGetBlocks get = (BukkitGetBlocks) getOrCreateGet();
            ISetBlocks set = getOrCreateSet();

            Chunk nmsChunk = extent.ensureLoaded(X, Z);

            // Remove existing tiles
            {
                Map<BlockPosition, TileEntity> tiles = nmsChunk.getTileEntities();
                if (!tiles.isEmpty()) {
                    final Iterator<Map.Entry<BlockPosition, TileEntity>> iterator = tiles.entrySet().iterator();
                    while (iterator.hasNext()) {
                        final Map.Entry<BlockPosition, TileEntity> entry = iterator.next();
                        final BlockPosition pos = entry.getKey();
                        final int lx = pos.getX() & 15;
                        final int ly = pos.getY();
                        final int lz = pos.getZ() & 15;
                        final int layer = ly >> 4;
                        if (!set.hasSection(layer)) {
                            continue;
                        }
                        if (set.getBlock(lx, ly, lz).getOrdinal() != 0) {
                            TileEntity tile = entry.getValue();
                            tile.z();
                            tile.invalidateBlockCache();
                        }
                    }
                }
            }

            int bitMask = 0;
            synchronized (nmsChunk) {
                ChunkSection[] sections = nmsChunk.getSections();
                World world = extent.getBukkitWorld();
                boolean hasSky = world.getEnvironment() == World.Environment.NORMAL;

                for (int layer = 0; layer < 16; layer++) {
                    if (!set.hasSection(layer)) continue;

                    bitMask |= 1 << layer;

                    char[] setArr = set.getArray(layer);
                    ChunkSection newSection;
                    ChunkSection existingSection = sections[layer];
                    if (existingSection == null) {
                        newSection = extent.newChunkSection(layer, hasSky, setArr);
                        if (BukkitQueue.setSectionAtomic(sections, null, newSection, layer)) {
                            updateGet(get, nmsChunk, sections, newSection, setArr, layer);
                            continue;
                        } else {
                            existingSection = sections[layer];
                            if (existingSection == null) {
                                System.out.println("Skipping invalid null section. chunk:" + X + "," + Z + " layer: " + layer);
                                continue;
                            }
                        }
                    }
                    DelegateLock lock = BukkitQueue.applyLock(existingSection);
                    synchronized (get) {
                        synchronized (lock) {
                            lock.untilFree();

                            ChunkSection getSection;
                            if (get.nmsChunk != nmsChunk) {
                                get.nmsChunk = nmsChunk;
                                get.sections = null;
                                get.reset();
                            } else {
                                getSection = get.getSections()[layer];
                                if (getSection != existingSection) {
                                    get.sections[layer] = existingSection;
                                    get.reset();
                                } else if (lock.isModified()) {
                                    get.reset(layer);
                                }
                            }
                            char[] getArr = get.load(layer);
                            for (int i = 0; i < 4096; i++) {
                                char value = setArr[i];
                                if (value != 0) {
                                    getArr[i] = value;
                                }
                            }
                            newSection = extent.newChunkSection(layer, hasSky, getArr);
                            if (!BukkitQueue.setSectionAtomic(sections, existingSection, newSection, layer)) {
                                System.out.println("Failed to set chunk section:" + X + "," + Z + " layer: " + layer);
                                continue;
                            } else {
                                updateGet(get, nmsChunk, sections, newSection, setArr, layer);
                            }
                        }
                    }
                }

                // Biomes
                BiomeType[] biomes = set.getBiomes();
                if (biomes != null) {
                    // set biomes
                    final BiomeBase[] currentBiomes = nmsChunk.getBiomeIndex();
                    for (int i = 0; i < biomes.length; i++) {
                        final BiomeType biome = biomes[i];
                        if (biome != null) {
                            final Biome craftBiome = BukkitAdapter.adapt(biome);
                            currentBiomes[i] = CraftBlock.biomeToBiomeBase(craftBiome);
                        }
                    }
                }

                Runnable[] syncTasks = null;

                net.minecraft.server.v1_13_R2.World nmsWorld = nmsChunk.getWorld();
                int bx = X << 4;
                int bz = Z << 4;

                Set<UUID> entityRemoves = set.getEntityRemoves();
                if (entityRemoves != null && !entityRemoves.isEmpty()) {
                    if (syncTasks == null) syncTasks = new Runnable[3];

                    syncTasks[2] = new Runnable() {
                        @Override
                        public void run() {
                            final List<Entity>[] entities = nmsChunk.getEntitySlices();

                            for (int i = 0; i < entities.length; i++) {
                                final Collection<Entity> ents = entities[i];
                                if (!ents.isEmpty()) {
                                    final Iterator<Entity> iter = ents.iterator();
                                    while (iter.hasNext()) {
                                        final Entity entity = iter.next();
                                        if (entityRemoves.contains(entity.getUniqueID())) {
                                            iter.remove();
                                            entity.b(false);
                                            entity.die();
                                            entity.valid = false;
                                        }
                                    }
                                }
                            }
                        }
                    };
                }

                Set<CompoundTag> entities = set.getEntities();
                if (entities != null && !entities.isEmpty()) {
                    if (syncTasks == null) syncTasks = new Runnable[2];

                    syncTasks[1] = new Runnable() {
                        @Override
                        public void run() {
                            for (final CompoundTag nativeTag : entities) {
                                final Map<String, Tag> entityTagMap = ReflectionUtils.getMap(nativeTag.getValue());
                                final StringTag idTag = (StringTag) entityTagMap.get("Id");
                                final ListTag posTag = (ListTag) entityTagMap.get("Pos");
                                final ListTag rotTag = (ListTag) entityTagMap.get("Rotation");
                                if (idTag == null || posTag == null || rotTag == null) {
                                    Fawe.debug("Unknown entity tag: " + nativeTag);
                                    continue;
                                }
                                final double x = posTag.getDouble(0);
                                final double y = posTag.getDouble(1);
                                final double z = posTag.getDouble(2);
                                final float yaw = rotTag.getFloat(0);
                                final float pitch = rotTag.getFloat(1);
                                final String id = idTag.getValue();
                                final Entity entity = EntityTypes.a(nmsWorld, new MinecraftKey(id));
                                if (entity != null) {
                                    final UUID uuid = entity.getUniqueID();
                                    entityTagMap.put("UUIDMost", new LongTag(uuid.getMostSignificantBits()));
                                    entityTagMap.put("UUIDLeast", new LongTag(uuid.getLeastSignificantBits()));
                                    if (nativeTag != null) {
                                        final NBTTagCompound tag = (NBTTagCompound) BukkitQueue_1_13.fromNative(nativeTag);
                                        for (final String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
                                            tag.remove(name);
                                        }
                                        entity.f(tag);
                                    }
                                    entity.setLocation(x, y, z, yaw, pitch);
                                    nmsWorld.addEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);
                                }
                            }
                        }
                    };

                }

                // set tiles
                Map<Short, CompoundTag> tiles = set.getTiles();
                if (tiles != null && !tiles.isEmpty()) {
                    if (syncTasks == null) syncTasks = new Runnable[1];

                    syncTasks[0] = new Runnable() {
                        @Override
                        public void run() {
                            for (final Map.Entry<Short, CompoundTag> entry : tiles.entrySet()) {
                                final CompoundTag nativeTag = entry.getValue();
                                final short blockHash = entry.getKey();
                                final int x = (blockHash >> 12 & 0xF) + bx;
                                final int y = (blockHash & 0xFF);
                                final int z = (blockHash >> 8 & 0xF) + bz;
                                final BlockPosition pos = new BlockPosition(x, y, z);
                                synchronized (BukkitQueue_0.class) {
                                    TileEntity tileEntity = nmsWorld.getTileEntity(pos);
                                    if (tileEntity == null || tileEntity.x()) {
                                        nmsWorld.n(pos);
                                        tileEntity = nmsWorld.getTileEntity(pos);
                                    }
                                    if (tileEntity != null) {
                                        final NBTTagCompound tag = (NBTTagCompound) BukkitQueue_1_13.fromNative(nativeTag);
                                        tag.set("x", new NBTTagInt(x));
                                        tag.set("y", new NBTTagInt(y));
                                        tag.set("z", new NBTTagInt(z));
                                        tileEntity.load(tag);
                                    }
                                }
                            }
                        }
                    };
                }

                Runnable callback;
                if (bitMask == 0) {
                    callback = null;
                } else {
                    int finalMask = bitMask;
                    callback = () -> {
                        // Set Modified
                        nmsChunk.f(true);
                        nmsChunk.mustSave = true;
                        nmsChunk.markDirty();
                        // send to player
                        extent.sendChunk(X, Z, finalMask);

                        extent.returnToPool(BukkitChunkHolder.this);
                    };
                }
                if (syncTasks != null) {
                    QueueHandler queueHandler = Fawe.get().getQueueHandler();
                    Runnable[] finalSyncTasks = syncTasks;

                    // Chain the sync tasks and the callback
                    Callable<Future> chain = new Callable<Future>() {
                        @Override
                        public Future call() {
                            // Run the sync tasks
                            for (int i = 1; i < finalSyncTasks.length; i++) {
                                Runnable task = finalSyncTasks[i];
                                if (task != null) {
                                    task.run();
                                }
                            }
                            if (callback == null) {
                                extent.returnToPool(BukkitChunkHolder.this);
                                return null;
                            } else {
                                return queueHandler.async(callback, null);
                            }
                        }
                    };
                    return (T) (Future) queueHandler.sync(chain);
                } else {
                    if (callback == null) {
                        extent.returnToPool(BukkitChunkHolder.this);
                    } else {
                        callback.run();
                    }
                }
            }
            return null;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void set(final Filter filter) {
        // for each block
        // filter.applyBlock(block)
        throw new UnsupportedOperationException("Not implemented");
    }
}