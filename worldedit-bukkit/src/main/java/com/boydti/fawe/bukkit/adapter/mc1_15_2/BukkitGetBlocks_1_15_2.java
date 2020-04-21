package com.boydti.fawe.bukkit.adapter.mc1_15_2;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.beta.implementation.blocks.CharGetBlocks;
import com.boydti.fawe.beta.implementation.queue.QueueHandler;
import com.boydti.fawe.bukkit.adapter.DelegateLock;
import com.boydti.fawe.bukkit.adapter.mc1_15_2.nbt.LazyCompoundTag_1_15_2;
import com.boydti.fawe.object.collection.AdaptedMap;
import com.boydti.fawe.object.collection.BitArray4096;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.bukkit.adapter.impl.FAWE_Spigot_v1_15_R2;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.minecraft.server.v1_15_R1.BiomeBase;
import net.minecraft.server.v1_15_R1.BiomeStorage;
import net.minecraft.server.v1_15_R1.BlockPosition;
import net.minecraft.server.v1_15_R1.Chunk;
import net.minecraft.server.v1_15_R1.ChunkSection;
import net.minecraft.server.v1_15_R1.DataBits;
import net.minecraft.server.v1_15_R1.DataPalette;
import net.minecraft.server.v1_15_R1.DataPaletteBlock;
import net.minecraft.server.v1_15_R1.DataPaletteHash;
import net.minecraft.server.v1_15_R1.DataPaletteLinear;
import net.minecraft.server.v1_15_R1.Entity;
import net.minecraft.server.v1_15_R1.EntityTypes;
import net.minecraft.server.v1_15_R1.IBlockData;
import net.minecraft.server.v1_15_R1.LightEngineThreaded;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.NBTTagInt;
import net.minecraft.server.v1_15_R1.TileEntity;
import net.minecraft.server.v1_15_R1.WorldServer;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.block.CraftBlock;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Function;

import static org.slf4j.LoggerFactory.getLogger;

public class BukkitGetBlocks_1_15_2 extends CharGetBlocks {

    private static final Logger log = LoggerFactory.getLogger(BukkitGetBlocks_1_15_2.class);

    private static final Function<BlockPosition, BlockVector3> posNms2We = v -> BlockVector3.at(v.getX(), v.getY(), v.getZ());
    private final static Function<TileEntity, CompoundTag> nmsTile2We = tileEntity -> new LazyCompoundTag_1_15_2(Suppliers.memoize(() -> tileEntity.save(new NBTTagCompound())));
    public ChunkSection[] sections;
    public Chunk nmsChunk;
    public WorldServer world;
    public int X, Z;

    public BukkitGetBlocks_1_15_2(World world, int X, int Z) {
        this(((CraftWorld) world).getHandle(), X, Z);
    }

    public BukkitGetBlocks_1_15_2(WorldServer world, int X, int Z) {
        this.world = world;
        this.X = X;
        this.Z = Z;
    }

    public int getX() {
        return X;
    }

    public int getZ() {
        return Z;
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        BiomeStorage index = getChunk().getBiomeIndex();
        BiomeBase base = null;
        if (y == -1) {
            for (y = 0; y < FaweCache.IMP.WORLD_HEIGHT; y++) {
                base = index.getBiome(x, y, z);
                if (base != null) break;
            }
        } else {
            base = index.getBiome(x, y, z);
        }
        return base != null ? BukkitAdapter.adapt(CraftBlock.biomeBaseToBiome(base)) : null;
    }

    @Override
    public CompoundTag getTile(int x, int y, int z) {
        TileEntity tileEntity = getChunk().getTileEntity(new BlockPosition((x & 15) + (X << 4), y, (z & 15) + (Z << 4)));
        if (tileEntity == null) {
            return null;
        }
        return new LazyCompoundTag_1_15_2(Suppliers.memoize(() -> tileEntity.save(new NBTTagCompound())));
    }

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
        Entity entity = world.getEntity(uuid);
        if (entity != null) {
            org.bukkit.entity.Entity bukkitEnt = entity.getBukkitEntity();
            return BukkitAdapter.adapt(bukkitEnt).getState().getNbtData();
        }
        for (List<Entity> entry : getChunk().getEntitySlices()) {
            if (entry != null) {
                for (Entity ent : entry) {
                    if (uuid.equals(ent.getUniqueID())) {
                        org.bukkit.entity.Entity bukkitEnt = ent.getBukkitEntity();
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

    private void updateGet(BukkitGetBlocks_1_15_2 get, Chunk nmsChunk, ChunkSection[] sections, ChunkSection section, char[] arr, int layer) {
        synchronized (get) {
            if (this.nmsChunk != nmsChunk) {
                this.nmsChunk = nmsChunk;
                this.sections = sections.clone();
                this.reset();
            }
            if (this.sections == null) {
                this.sections = sections.clone();
            }
            //TODO: Understand why this causes #329, what the purpose of this is, and what may or may not break after commenting this out.
//            if (this.sections[layer] != section) {
//                this.sections[layer] = section;
//            }
            this.blocks[layer] = arr;
        }
    }

    private void removeEntity(Entity entity) {
        entity.die();
        entity.valid = false;
    }

    public Chunk ensureLoaded(net.minecraft.server.v1_15_R1.World nmsWorld, int X, int Z) {
        return BukkitAdapter_1_15_2.ensureLoaded(nmsWorld, X, Z);
    }

    @Override
    public <T extends Future<T>> T call(IChunkSet set, Runnable finalizer) {
        try {
            WorldServer nmsWorld = world;
            Chunk nmsChunk = ensureLoaded(nmsWorld, X, Z);

            // Remove existing tiles
            {
                // Create a copy so that we can remove blocks
                Map<BlockPosition, TileEntity> tiles = new HashMap<>(nmsChunk.getTileEntities());
                if (!tiles.isEmpty()) {
                    for (Map.Entry<BlockPosition, TileEntity> entry : tiles.entrySet()) {
                        final BlockPosition pos = entry.getKey();
                        final int lx = pos.getX() & 15;
                        final int ly = pos.getY();
                        final int lz = pos.getZ() & 15;
                        final int layer = ly >> 4;
                        if (!set.hasSection(layer)) {
                            continue;
                        }

                        int ordinal = set.getBlock(lx, ly, lz).getOrdinal();
                        if (ordinal != 0) {
                            TileEntity tile = entry.getValue();
                            nmsChunk.removeTileEntity(tile.getPosition());
                            nmsChunk.markDirty();
                        }
                    }
                }
            }

            int bitMask = 0;
            synchronized (nmsChunk) {
                ChunkSection[] sections = nmsChunk.getSections();

                for (int layer = 0; layer < 16; layer++) {
                    if (!set.hasSection(layer)){
                        continue;
                    }

                    bitMask |= 1 << layer;

                    char[] setArr = set.load(layer);
                    ChunkSection newSection;
                    ChunkSection existingSection = sections[layer];
                    if (existingSection == null) {
                        newSection = BukkitAdapter_1_15_2.newChunkSection(layer, setArr);
                        if (BukkitAdapter_1_15_2.setSectionAtomic(sections, null, newSection, layer)) {
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
                    DelegateLock lock = BukkitAdapter_1_15_2.applyLock(existingSection);
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
                            newSection = BukkitAdapter_1_15_2.newChunkSection(layer, this::load, setArr);
                            if (!BukkitAdapter_1_15_2.setSectionAtomic(sections, existingSection, newSection, layer)) {
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
                    BiomeStorage currentBiomes = nmsChunk.getBiomeIndex();
                    for (int z = 0, i = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++, i++) {
                            final BiomeType biome = biomes[i];
                            if (biome != null) {
                                final Biome craftBiome = BukkitAdapter.adapt(biome);
                                BiomeBase nmsBiome = CraftBlock.biomeToBiomeBase(craftBiome);
                                for (int y = 0; y < FaweCache.IMP.WORLD_HEIGHT; y++) {
                                    currentBiomes.setBiome(x, y, z, nmsBiome);
                                }
                            }
                        }
                    }
                }

                Runnable[] syncTasks = null;

                int bx = X << 4;
                int bz = Z << 4;

                Set<UUID> entityRemoves = set.getEntityRemoves();
                if (entityRemoves != null && !entityRemoves.isEmpty()) {
                    if (syncTasks == null) syncTasks = new Runnable[3];

                    syncTasks[2] = () -> {
                        final List<Entity>[] entities = nmsChunk.getEntitySlices();

                        for (final Collection<Entity> ents : entities) {
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
                    };
                }

                Set<CompoundTag> entities = set.getEntities();
                if (entities != null && !entities.isEmpty()) {
                    if (syncTasks == null) syncTasks = new Runnable[2];

                    syncTasks[1] = () -> {
                        for (final CompoundTag nativeTag : entities) {
                            final Map<String, Tag> entityTagMap = nativeTag.getValue();
                            final StringTag idTag = (StringTag) entityTagMap.get("Id");
                            final ListTag posTag = (ListTag) entityTagMap.get("Pos");
                            final ListTag rotTag = (ListTag) entityTagMap.get("Rotation");
                            if (idTag == null || posTag == null || rotTag == null) {
                                getLogger(BukkitGetBlocks_1_15_2.class).debug("Unknown entity tag: " + nativeTag);
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
                    };

                }

                // set tiles
                Map<BlockVector3, CompoundTag> tiles = set.getTiles();
                if (tiles != null && !tiles.isEmpty()) {
                    if (syncTasks == null) syncTasks = new Runnable[1];

                    syncTasks[0] = () -> {
                        for (final Map.Entry<BlockVector3, CompoundTag> entry : tiles.entrySet()) {
                            final CompoundTag nativeTag = entry.getValue();
                            final BlockVector3 blockHash = entry.getKey();
                            final int x = blockHash.getX() + bx;
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
                                    tag.set("x", NBTTagInt.a(x));
                                    tag.set("y", NBTTagInt.a(y));
                                    tag.set("z", NBTTagInt.a(z));
                                    tileEntity.load(tag);
                                }
                            }
                        }
                    };
                }

                //Lighting
                // TODO optimize, cause this is really slow
                LightEngineThreaded engine = (LightEngineThreaded) nmsChunk.e();
                engine.a(nmsChunk, false);

                Runnable callback;
                if (bitMask == 0 && biomes == null) {
                    callback = null;
                } else {
                    int finalMask = bitMask;
                    callback = () -> {
                        // Set Modified
                        nmsChunk.d(true); // Set Modified
                        nmsChunk.mustNotSave = false;
                        nmsChunk.markDirty();
                        // send to player
                        BukkitAdapter_1_15_2.sendChunk(nmsWorld, X, Z, finalMask);
                        if (finalizer != null) finalizer.run();
                    };
                }
                if (syncTasks != null) {
                    QueueHandler queueHandler = Fawe.get().getQueueHandler();
                    Runnable[] finalSyncTasks = syncTasks;

                    // Chain the sync tasks and the callback
                    Callable<Future> chain = () -> {
                        try {
                            // Run the sync tasks
                            for (Runnable task : finalSyncTasks) {
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
                        } catch (Throwable e) {
                            e.printStackTrace();
                            throw e;
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
    public synchronized char[] update(int layer, char[] data) {
        ChunkSection section = getSections()[layer];
        // Section is null, return empty array
        if (section == null) {
            return FaweCache.IMP.EMPTY_CHAR_4096;
        }
        if (data == null || data == FaweCache.IMP.EMPTY_CHAR_4096) {
            data = new char[4096];
        }
        DelegateLock lock = BukkitAdapter_1_15_2.applyLock(section);
        synchronized (lock) {
            lock.untilFree();
            lock.setModified(false);
            // Efficiently convert ChunkSection to raw data
            try {
                FAWE_Spigot_v1_15_R2 adapter = ((FAWE_Spigot_v1_15_R2) WorldEditPlugin.getInstance().getBukkitImplAdapter());

                final DataPaletteBlock<IBlockData> blocks = section.getBlocks();
                final DataBits bits = (DataBits) BukkitAdapter_1_15_2.fieldBits.get(blocks);
                final DataPalette<IBlockData> palette = (DataPalette<IBlockData>) BukkitAdapter_1_15_2.fieldPalette.get(blocks);

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

                char[] paletteToOrdinal = FaweCache.IMP.PALETTE_TO_BLOCK_CHAR.get();
                try {
                    if (num_palette != 1) {
                        for (int i = 0; i < num_palette; i++) {
                            char ordinal = ordinal(palette.a(i), adapter);
                            paletteToOrdinal[i] = ordinal;
                        }
                        for (int i = 0; i < 4096; i++) {
                            char paletteVal = data[i];
                            char val = paletteToOrdinal[paletteVal];
                            if (val == Character.MAX_VALUE) {
                                val = ordinal(palette.a(i), adapter);
                                paletteToOrdinal[i] = val;
                            }
                            data[i] = val;
                        }
                    } else {
                        char ordinal = ordinal(palette.a(0), adapter);
                        Arrays.fill(data, ordinal);
                    }
                } finally {
                    for (int i = 0; i < num_palette; i++) {
                        paletteToOrdinal[i] = Character.MAX_VALUE;
                    }
                }
                return data;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    private final char ordinal(IBlockData ibd, FAWE_Spigot_v1_15_R2 adapter) {
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
                    nmsChunk = tmp = ensureLoaded(this.world, X, Z);
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
