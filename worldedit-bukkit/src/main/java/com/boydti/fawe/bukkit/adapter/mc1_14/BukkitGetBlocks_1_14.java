package com.boydti.fawe.bukkit.adapter.mc1_14;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.implementation.queue.QueueHandler;
import com.boydti.fawe.beta.implementation.blocks.CharGetBlocks;
import com.boydti.fawe.bukkit.adapter.DelegateLock;
import com.boydti.fawe.object.collection.AdaptedMap;
import com.boydti.fawe.object.collection.BitArray4096;
import com.boydti.fawe.util.ReflectionUtils;
import com.google.common.collect.Iterables;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.minecraft.server.v1_14_R1.BiomeBase;
import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.Chunk;
import net.minecraft.server.v1_14_R1.ChunkSection;
import net.minecraft.server.v1_14_R1.DataBits;
import net.minecraft.server.v1_14_R1.DataPalette;
import net.minecraft.server.v1_14_R1.DataPaletteBlock;
import net.minecraft.server.v1_14_R1.DataPaletteHash;
import net.minecraft.server.v1_14_R1.DataPaletteLinear;
import net.minecraft.server.v1_14_R1.Entity;
import net.minecraft.server.v1_14_R1.EntityTypes;
import net.minecraft.server.v1_14_R1.IBlockData;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.NBTTagInt;
import net.minecraft.server.v1_14_R1.TileEntity;
import net.minecraft.server.v1_14_R1.WorldServer;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.block.CraftBlock;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Function;

public class BukkitGetBlocks_1_14 extends CharGetBlocks {
    public ChunkSection[] sections;
    public Chunk nmsChunk;
    public CraftWorld world;
    public int X, Z;
//    private boolean forceLoad;

    public BukkitGetBlocks_1_14(World world, int X, int Z, boolean forceLoad) {
        this.world = (CraftWorld) world;
        this.X = X;
        this.Z = Z;
//        if (forceLoad) {
//            this.world.getHandle().setForceLoaded(X, Z, this.forceLoad = true);
//        }
    }

//    @Override
//    protected void finalize() {
//        if (forceLoad) {
//            this.world.getHandle().setForceLoaded(X, Z, forceLoad = false);
//        }
//    }


    public int getX() {
        return X;
    }

    public int getZ() {
        return Z;
    }

    @Override
    public BiomeType getBiomeType(int x, int z) {
        BiomeBase base = getChunk().getBiomeIndex()[(z << 4) + x];
        return BukkitAdapter.adapt(CraftBlock.biomeBaseToBiome(base));
    }

    @Override
    public CompoundTag getTag(int x, int y, int z) {
        TileEntity tile = getChunk().getTileEntity(new BlockPosition((x & 15) + (X << 4), y, (z & 15) + (Z << 4)));
        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        return (CompoundTag) adapter.toNative(tile);
    }

    private static final Function<BlockPosition, BlockVector3> posNms2We = new Function<BlockPosition, BlockVector3>() {
        @Override
        public BlockVector3 apply(BlockPosition v) {
            return BlockVector3.at(v.getX(), v.getY(), v.getZ());
        }
    };

    private final static Function<TileEntity, CompoundTag> nmsTile2We = new Function<TileEntity, CompoundTag>() {
        @Override
        public CompoundTag apply(TileEntity tileEntity) {
            BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
            return (CompoundTag) adapter.toNative(tileEntity.b());
        }
    };

    @Override
    public Map<BlockVector3, CompoundTag> getTiles() {
        Map<BlockPosition, TileEntity> nmsTiles = getChunk().getTileEntities();
        if (nmsTiles.isEmpty()) {
            return Collections.emptyMap();
        }
        return AdaptedMap.immutable(nmsTiles, posNms2We, nmsTile2We);
    }

    @Override
    public CompoundTag getEntity(UUID uuid) {
        org.bukkit.entity.Entity bukkitEnt = world.getEntity(uuid);
        if (bukkitEnt != null) {
            return BukkitAdapter.adapt(bukkitEnt).getState().getNbtData();
        }
        for (List<Entity> entry : getChunk().getEntitySlices()) {
            if (entry != null) {
                for (Entity entity : entry) {
                    if (uuid.equals(entity.getUniqueID())) {
                        return BukkitAdapter.adapt(bukkitEnt).getState().getNbtData();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Set<CompoundTag> getEntities() {
        List<Entity>[] slices = getChunk().getEntitySlices();
        int size = 0;
        for (List<Entity> slice : slices) {
            if (slice != null) size += slice.size();
        }
        if (slices.length == 0) {
            return Collections.emptySet();
        }
        int finalSize = size;
        return new AbstractSet<CompoundTag>() {
            @Override
            public int size() {
                return finalSize;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(Object get) {
                if (!(get instanceof CompoundTag)) {
                    return false;
                }
                CompoundTag getTag = (CompoundTag) get;
                Map<String, Tag> value = getTag.getValue();
                CompoundTag getParts = (CompoundTag) value.get("UUID");
                UUID getUUID = new UUID(getParts.getLong("Most"), getParts.getLong("Least"));
                for (List<Entity> slice : slices) {
                    if (slice != null) {
                        for (Entity entity : slice) {
                            UUID uuid = entity.getUniqueID();
                            if (uuid.equals(getUUID)) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }

            @NotNull
            @Override
            public Iterator<CompoundTag> iterator() {
                Iterable<CompoundTag> result = Iterables.transform(Iterables.concat(slices), new com.google.common.base.Function<Entity, CompoundTag>() {
                    @Nullable
                    @Override
                    public CompoundTag apply(@Nullable Entity input) {
                        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
                        NBTTagCompound tag = new NBTTagCompound();
                        return (CompoundTag) adapter.toNative(input.save(tag));
                    }
                });
                return result.iterator();
            }
        };
    }

    @Override
    public char[] load(int layer) {
        return load(layer, null);
    }

    private void updateGet(BukkitGetBlocks_1_14 get, Chunk nmsChunk, ChunkSection[] sections, ChunkSection section, char[] arr, int layer) {
        synchronized (get) {
            if (this.nmsChunk != nmsChunk) {
                this.nmsChunk = nmsChunk;
                this.sections = sections.clone();
                this.reset();
            }
            if (this.sections == null) {
                this.sections = sections.clone();
            }
            if (this.sections[layer] != section) {
                this.sections[layer] = section;
            }
            this.blocks[layer] = arr;
        }
    }

    private void removeEntity(Entity entity) {
        entity.die();
        entity.valid = false;
    }

    @Override
    public <T extends Future<T>> T call(IChunkSet set, Runnable finalizer) {
        try {
            WorldServer nmsWorld = world.getHandle();
            Chunk nmsChunk = BukkitAdapter_1_14.ensureLoaded(nmsWorld, X, Z);

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
                            tile.n();
                            tile.invalidateBlockCache();
                        }
                    }
                }
            }

            int bitMask = 0;
            synchronized (nmsChunk) {
                ChunkSection[] sections = nmsChunk.getSections();

                for (int layer = 0; layer < 16; layer++) {
                    if (!set.hasSection(layer)) continue;

                    bitMask |= 1 << layer;

                    char[] setArr = set.getArray(layer);
                    ChunkSection newSection;
                    ChunkSection existingSection = sections[layer];
                    if (existingSection == null) {
                        newSection = BukkitAdapter_1_14.newChunkSection(layer, setArr);
                        if (BukkitAdapter_1_14.setSectionAtomic(sections, null, newSection, layer)) {
                            updateGet(this, nmsChunk, sections, newSection, setArr, layer);
                            continue;
                        } else {
                            existingSection = sections[layer];
                            if (existingSection == null) {
                                System.out.println("Skipping invalid null section. chunk:" + X + "," + Z + " layer: " + layer);
                                continue;
                            }
                        }
                    }
                    DelegateLock lock = BukkitAdapter_1_14.applyLock(existingSection);
                    synchronized (this) {
                        synchronized (lock) {
                            lock.untilFree();
                            ChunkSection getSection;
                            if (this.nmsChunk != nmsChunk) {
                                this.nmsChunk = nmsChunk;
                                this.sections = null;
                                this.reset();
                            } else {
                                getSection = this.getSections()[layer];
                                if (getSection != existingSection) {
                                    this.sections[layer] = existingSection;
                                    this.reset();
                                } else if (lock.isModified()) {
                                    this.reset(layer);
                                }
                            }
                            char[] getArr = this.load(layer);
                            for (int i = 0; i < 4096; i++) {
                                char value = setArr[i];
                                if (value != 0) {
                                    getArr[i] = value;
                                }
                            }
                            newSection = BukkitAdapter_1_14.newChunkSection(layer, getArr);
                            if (!BukkitAdapter_1_14.setSectionAtomic(sections, existingSection, newSection, layer)) {
                                System.out.println("Failed to set chunk section:" + X + "," + Z + " layer: " + layer);
                                continue;
                            } else {
                                updateGet(this, nmsChunk, sections, newSection, setArr, layer);
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
                                            removeEntity(entity);
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

                                EntityTypes<?> type = EntityTypes.a(id).orElse(null);
                                if (type != null) {
                                    Entity entity = type.a(nmsWorld);
                                    if (entity != null) {
                                        UUID uuid = entity.getUniqueID();
                                        entityTagMap.put("UUIDMost", new LongTag(uuid.getMostSignificantBits()));
                                        entityTagMap.put("UUIDLeast", new LongTag(uuid.getLeastSignificantBits()));
                                        if (nativeTag != null) {
                                            BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
                                            final NBTTagCompound tag = (NBTTagCompound) adapter.fromNative(nativeTag);
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
                        }
                    };

                }

                // set tiles
                Map<BlockVector3, CompoundTag> tiles = set.getTiles();
                if (tiles != null && !tiles.isEmpty()) {
                    if (syncTasks == null) syncTasks = new Runnable[1];

                    syncTasks[0] = new Runnable() {
                        @Override
                        public void run() {
                            for (final Map.Entry<BlockVector3, CompoundTag> entry : tiles.entrySet()) {
                                final CompoundTag nativeTag = entry.getValue();
                                final BlockVector3 blockHash = entry.getKey();
                                final int x = blockHash.getX()+ bx;
                                final int y = blockHash.getY();
                                final int z = blockHash.getZ() + bz;
                                final BlockPosition pos = new BlockPosition(x, y, z);
                                synchronized (nmsWorld) {
                                    TileEntity tileEntity = nmsWorld.getTileEntity(pos);
                                    if (tileEntity == null || tileEntity.isRemoved()) {
                                        nmsWorld.removeTileEntity(pos);
                                        tileEntity = nmsWorld.getTileEntity(pos);
                                    }
                                    if (tileEntity != null) {
                                        BukkitImplAdapter adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
                                        final NBTTagCompound tag = (NBTTagCompound) adapter.fromNative(nativeTag);
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
                        nmsChunk.d(true); // Set Modified
                        nmsChunk.mustNotSave = false;
                        nmsChunk.markDirty();
                        // send to player
                        BukkitAdapter_1_14.sendChunk(nmsWorld, X, Z, finalMask);
                        if (finalizer != null) finalizer.run();
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
                                if (finalizer != null) finalizer.run();
                                return null;
                            } else {
                                return queueHandler.async(callback, null);
                            }
                        }
                    };
                    return (T) (Future) queueHandler.sync(chain);
                } else {
                    if (callback == null) {
                        if (finalizer != null) finalizer.run();
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
    public synchronized char[] load(int layer, char[] data) {
        ChunkSection section = getSections()[layer];
        // Section is null, return empty array
        if (section == null) {
            return FaweCache.IMP.EMPTY_CHAR_4096;
        }
        if (data == null || data == FaweCache.IMP.EMPTY_CHAR_4096) {
            data = new char[4096];
        }
        DelegateLock lock = BukkitAdapter_1_14.applyLock(section);
        synchronized (lock) {
            lock.untilFree();
            lock.setModified(false);
            // Efficiently convert ChunkSection to raw data
            try {
                Spigot_v1_14_R4 adapter = ((Spigot_v1_14_R4) WorldEditPlugin.getInstance().getBukkitImplAdapter());

                final DataPaletteBlock<IBlockData> blocks = section.getBlocks();
                final DataBits bits = (DataBits) BukkitAdapter_1_14.fieldBits.get(blocks);
                final DataPalette<IBlockData> palette = (DataPalette<IBlockData>) BukkitAdapter_1_14.fieldPalette.get(blocks);

                final int bitsPerEntry = bits.c();
                final long[] blockStates = bits.a();

                new BitArray4096(blockStates, bitsPerEntry).toRaw(data);

                int num_palette;
                if (palette instanceof DataPaletteLinear) {
                    num_palette = ((DataPaletteLinear<IBlockData>) palette).b();
                } else if (palette instanceof DataPaletteHash) {
                    num_palette = ((DataPaletteHash<IBlockData>) palette).b();
                } else {
                    num_palette = 0;
                    int[] paletteToBlockInts = FaweCache.IMP.PALETTE_TO_BLOCK.get();
                    char[] paletteToBlockChars = FaweCache.IMP.PALETTE_TO_BLOCK_CHAR.get();
                    try {
                        for (int i = 0; i < 4096; i++) {
                            char paletteVal = data[i];
                            char ordinal = paletteToBlockChars[paletteVal];
                            if (ordinal == Character.MAX_VALUE) {
                                paletteToBlockInts[num_palette++] = paletteVal;
                                IBlockData ibd = palette.a(data[i]);
                                if (ibd == null) {
                                    ordinal = BlockTypes.AIR.getDefaultState().getOrdinalChar();
                                } else {
                                    ordinal = adapter.adaptToChar(ibd);
                                }
                                paletteToBlockChars[paletteVal] = ordinal;
                            }
                            data[i] = ordinal;
                        }
                    } finally {
                        for (int i = 0; i < num_palette; i++) {
                            int paletteVal = paletteToBlockInts[i];
                            paletteToBlockChars[paletteVal] = Character.MAX_VALUE;
                        }
                    }
                    return data;
                }

                char[] paletteToBlockChars = FaweCache.IMP.PALETTE_TO_BLOCK_CHAR.get();
                try {
                    final int size = num_palette;
                    if (size != 1) {
                        for (int i = 0; i < size; i++) {
                            char ordinal = ordinal(palette.a(i), adapter);
                            paletteToBlockChars[i] = ordinal;
                        }
                        for (int i = 0; i < 4096; i++) {
                            char paletteVal = data[i];
                            char val = paletteToBlockChars[paletteVal];
                            data[i] = val;
                        }
                    } else {
                        char ordinal = ordinal(palette.a(0), adapter);
                        Arrays.fill(data, ordinal);
                    }
                } finally {
                    for (int i = 0; i < num_palette; i++) {
                        paletteToBlockChars[i] = Character.MAX_VALUE;
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            return data;
        }
    }

    private final char ordinal(IBlockData ibd, Spigot_v1_14_R4 adapter) {
        if (ibd == null) {
            return BlockTypes.AIR.getDefaultState().getOrdinalChar();
        } else {
            return adapter.adaptToChar(ibd);
        }
    }

    public ChunkSection[] getSections() {
        ChunkSection[] tmp = sections;
        if (tmp == null) {
            synchronized (this) {
                tmp = sections;
                if (tmp == null) {
                    Chunk chunk = getChunk();
                    sections = tmp = chunk.getSections().clone();
                }
            }
        }
        return tmp;
    }

    public Chunk getChunk() {
        Chunk tmp = nmsChunk;
        if (tmp == null) {
            synchronized (this) {
                tmp = nmsChunk;
                if (tmp == null) {
                    nmsChunk = tmp = BukkitAdapter_1_14.ensureLoaded(this.world.getHandle(), X, Z);
                }
            }
        }
        return tmp;
    }

    @Override
    public boolean hasSection(int layer) {
        return getSections()[layer] != null;
    }

    @Override
    public boolean trim(boolean aggressive) {
        if (aggressive) {
            sections = null;
            nmsChunk = null;
        }
        return super.trim(aggressive);
    }
}
