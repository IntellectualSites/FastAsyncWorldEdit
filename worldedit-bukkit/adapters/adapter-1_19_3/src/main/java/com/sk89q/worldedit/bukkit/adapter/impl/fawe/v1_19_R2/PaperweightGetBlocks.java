package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_19_R2;

import com.fastasyncworldedit.bukkit.adapter.BukkitGetBlocks;
import com.fastasyncworldedit.bukkit.adapter.DelegateSemaphore;
import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.math.BitArrayUnstretched;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.implementation.QueueHandler;
import com.fastasyncworldedit.core.queue.implementation.blocks.CharGetBlocks;
import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.collection.AdaptedMap;
import com.google.common.base.Suppliers;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_19_R2.nbt.PaperweightLazyCompoundTag;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import io.papermc.lib.PaperLib;
import io.papermc.paper.event.block.BeaconDeactivatedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.IntTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.BitStorage;
import net.minecraft.util.ZeroBitStorage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.HashMapPalette;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LinearPalette;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.apache.logging.log4j.Logger;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R2.block.CraftBlock;
import org.bukkit.event.entity.CreatureSpawnEvent;

import javax.annotation.Nonnull;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.minecraft.core.registries.Registries.BIOME;

public class PaperweightGetBlocks extends CharGetBlocks implements BukkitGetBlocks {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private static final Function<BlockPos, BlockVector3> posNms2We = v -> BlockVector3.at(v.getX(), v.getY(), v.getZ());
    private static final Function<BlockEntity, CompoundTag> nmsTile2We =
            tileEntity -> new PaperweightLazyCompoundTag(Suppliers.memoize(tileEntity::saveWithId));
    private final PaperweightFaweAdapter adapter = ((PaperweightFaweAdapter) WorldEditPlugin
            .getInstance()
            .getBukkitImplAdapter());
    private final ReadWriteLock sectionLock = new ReentrantReadWriteLock();
    private final ServerLevel serverLevel;
    private final int chunkX;
    private final int chunkZ;
    private final int minHeight;
    private final int maxHeight;
    private final int minSectionPosition;
    private final int maxSectionPosition;
    private final Registry<Biome> biomeRegistry;
    private final IdMap<Holder<Biome>> biomeHolderIdMap;
    private LevelChunkSection[] sections;
    private LevelChunk levelChunk;
    private DataLayer[] blockLight;
    private DataLayer[] skyLight;
    private boolean createCopy = false;
    private PaperweightGetBlocks_Copy copy = null;
    private boolean forceLoadSections = true;
    private boolean lightUpdate = false;

    public PaperweightGetBlocks(World world, int chunkX, int chunkZ) {
        this(((CraftWorld) world).getHandle(), chunkX, chunkZ);
    }

    public PaperweightGetBlocks(ServerLevel serverLevel, int chunkX, int chunkZ) {
        super(serverLevel.getMinBuildHeight() >> 4, (serverLevel.getMaxBuildHeight() - 1) >> 4);
        this.serverLevel = serverLevel;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.minHeight = serverLevel.getMinBuildHeight();
        this.maxHeight = serverLevel.getMaxBuildHeight() - 1; // Minecraft max limit is exclusive.
        this.minSectionPosition = minHeight >> 4;
        this.maxSectionPosition = maxHeight >> 4;
        this.skyLight = new DataLayer[getSectionCount()];
        this.blockLight = new DataLayer[getSectionCount()];
        this.biomeRegistry = serverLevel.registryAccess().registryOrThrow(BIOME);
        this.biomeHolderIdMap = biomeRegistry.asHolderIdMap();
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    @Override
    public boolean isCreateCopy() {
        return createCopy;
    }

    @Override
    public void setCreateCopy(boolean createCopy) {
        this.createCopy = createCopy;
    }

    @Override
    public IChunkGet getCopy() {
        return copy;
    }

    @Override
    public void setLightingToGet(char[][] light, int minSectionPosition, int maxSectionPosition) {
        if (light != null) {
            lightUpdate = true;
            try {
                fillLightNibble(light, LightLayer.BLOCK, minSectionPosition, maxSectionPosition);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setSkyLightingToGet(char[][] light, int minSectionPosition, int maxSectionPosition) {
        if (light != null) {
            lightUpdate = true;
            try {
                fillLightNibble(light, LightLayer.SKY, minSectionPosition, maxSectionPosition);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setHeightmapToGet(HeightMapType type, int[] data) {
        // height + 1 to match server internal
        BitArrayUnstretched bitArray = new BitArrayUnstretched(MathMan.log2nlz(getChunk().getHeight() + 1), 256);
        bitArray.fromRaw(data);
        Heightmap.Types nativeType = Heightmap.Types.valueOf(type.name());
        Heightmap heightMap = getChunk().heightmaps.get(nativeType);
        heightMap.setRawData(getChunk(), nativeType, bitArray.getData());
    }

    @Override
    public int getMaxY() {
        return maxHeight;
    }

    @Override
    public int getMinY() {
        return minHeight;
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        LevelChunkSection section = getSections(false)[(y >> 4) - getMinSectionPosition()];
        Holder<Biome> biomes = section.getNoiseBiome(x >> 2, (y & 15) >> 2, z >> 2);
        return PaperweightPlatformAdapter.adapt(biomes, serverLevel);
    }

    @Override
    public void removeSectionLighting(int layer, boolean sky) {
        SectionPos sectionPos = SectionPos.of(getChunk().getPos(), layer);
        DataLayer dataLayer = serverLevel.getChunkSource().getLightEngine().getLayerListener(LightLayer.BLOCK).getDataLayerData(
                sectionPos);
        if (dataLayer != null) {
            lightUpdate = true;
            synchronized (dataLayer) {
                byte[] bytes = dataLayer.getData();
                Arrays.fill(bytes, (byte) 0);
            }
        }
        if (sky) {
            SectionPos sectionPos1 = SectionPos.of(getChunk().getPos(), layer);
            DataLayer dataLayer1 = serverLevel
                    .getChunkSource()
                    .getLightEngine()
                    .getLayerListener(LightLayer.SKY)
                    .getDataLayerData(sectionPos1);
            if (dataLayer1 != null) {
                lightUpdate = true;
                synchronized (dataLayer1) {
                    byte[] bytes = dataLayer1.getData();
                    Arrays.fill(bytes, (byte) 0);
                }
            }
        }
    }

    @Override
    public CompoundTag getTile(int x, int y, int z) {
        BlockEntity blockEntity = getChunk().getBlockEntity(new BlockPos((x & 15) + (
                chunkX << 4), y, (z & 15) + (
                chunkZ << 4)));
        if (blockEntity == null) {
            return null;
        }
        return new PaperweightLazyCompoundTag(Suppliers.memoize(blockEntity::saveWithId));
    }

    @Override
    public Map<BlockVector3, CompoundTag> getTiles() {
        Map<BlockPos, BlockEntity> nmsTiles = getChunk().getBlockEntities();
        if (nmsTiles.isEmpty()) {
            return Collections.emptyMap();
        }
        return AdaptedMap.immutable(nmsTiles, posNms2We, nmsTile2We);
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        int layer = y >> 4;
        int alayer = layer - getMinSectionPosition();
        if (skyLight[alayer] == null) {
            SectionPos sectionPos = SectionPos.of(getChunk().getPos(), layer);
            DataLayer dataLayer =
                    serverLevel.getChunkSource().getLightEngine().getLayerListener(LightLayer.SKY).getDataLayerData(sectionPos);
            // If the server hasn't generated the section's NibbleArray yet, it will be null
            if (dataLayer == null) {
                byte[] LAYER_COUNT = new byte[2048];
                // Safe enough to assume if it's not created, it's under the sky. Unlikely to be created before lighting is fixed anyway.
                Arrays.fill(LAYER_COUNT, (byte) 15);
                dataLayer = new DataLayer(LAYER_COUNT);
                ((LevelLightEngine) serverLevel.getChunkSource().getLightEngine()).queueSectionData(
                        LightLayer.BLOCK,
                        sectionPos,
                        dataLayer,
                        true
                );
            }
            skyLight[alayer] = dataLayer;
        }
        return skyLight[alayer].get(x & 15, y & 15, z & 15);
    }

    @Override
    public int getEmittedLight(int x, int y, int z) {
        int layer = y >> 4;
        int alayer = layer - getMinSectionPosition();
        if (blockLight[alayer] == null) {
            serverLevel.getRawBrightness(new BlockPos(1, 1, 1), 5);
            SectionPos sectionPos = SectionPos.of(getChunk().getPos(), layer);
            DataLayer dataLayer = serverLevel
                    .getChunkSource()
                    .getLightEngine()
                    .getLayerListener(LightLayer.BLOCK)
                    .getDataLayerData(sectionPos);
            // If the server hasn't generated the section's DataLayer yet, it will be null
            if (dataLayer == null) {
                byte[] LAYER_COUNT = new byte[2048];
                // Safe enough to assume if it's not created, it's under the sky. Unlikely to be created before lighting is fixed anyway.
                Arrays.fill(LAYER_COUNT, (byte) 15);
                dataLayer = new DataLayer(LAYER_COUNT);
                ((LevelLightEngine) serverLevel.getChunkSource().getLightEngine()).queueSectionData(LightLayer.BLOCK, sectionPos,
                        dataLayer, true
                );
            }
            blockLight[alayer] = dataLayer;
        }
        return blockLight[alayer].get(x & 15, y & 15, z & 15);
    }

    @Override
    public int[] getHeightMap(HeightMapType type) {
        long[] longArray = getChunk().heightmaps.get(Heightmap.Types.valueOf(type.name())).getRawData();
        BitArrayUnstretched bitArray = new BitArrayUnstretched(9, 256, longArray);
        return bitArray.toRaw(new int[256]);
    }

    @Override
    public CompoundTag getEntity(UUID uuid) {
        Entity entity = serverLevel.getEntity(uuid);
        if (entity != null) {
            org.bukkit.entity.Entity bukkitEnt = entity.getBukkitEntity();
            return BukkitAdapter.adapt(bukkitEnt).getState().getNbtData();
        }
        for (CompoundTag tag : getEntities()) {
            if (uuid.equals(tag.getUUID())) {
                return tag;
            }
        }
        return null;
    }

    @Override
    public Set<CompoundTag> getEntities() {
        List<Entity> entities = PaperweightPlatformAdapter.getEntities(getChunk());
        if (entities.isEmpty()) {
            return Collections.emptySet();
        }
        int size = entities.size();
        return new AbstractSet<>() {
            @Override
            public int size() {
                return size;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(Object get) {
                if (!(get instanceof CompoundTag getTag)) {
                    return false;
                }
                UUID getUUID = getTag.getUUID();
                for (Entity entity : entities) {
                    UUID uuid = entity.getUUID();
                    if (uuid.equals(getUUID)) {
                        return true;
                    }
                }
                return false;
            }

            @Nonnull
            @Override
            public Iterator<CompoundTag> iterator() {
                Iterable<CompoundTag> result = entities.stream().map(input -> {
                    net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                    input.save(tag);
                    return (CompoundTag) adapter.toNative(tag);
                }).collect(Collectors.toList());
                return result.iterator();
            }
        };
    }

    private void removeEntity(Entity entity) {
        entity.discard();
    }

    public LevelChunk ensureLoaded(ServerLevel nmsWorld, int chunkX, int chunkZ) {
        return PaperweightPlatformAdapter.ensureLoaded(nmsWorld, chunkX, chunkZ);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public synchronized <T extends Future<T>> T call(IChunkSet set, Runnable finalizer) {
        forceLoadSections = false;
        copy = createCopy ? new PaperweightGetBlocks_Copy(levelChunk) : null;
        try {
            ServerLevel nmsWorld = serverLevel;
            LevelChunk nmsChunk = ensureLoaded(nmsWorld, chunkX, chunkZ);

            // Remove existing tiles. Create a copy so that we can remove blocks
            Map<BlockPos, BlockEntity> chunkTiles = new HashMap<>(nmsChunk.getBlockEntities());
            List<BlockEntity> beacons = null;
            if (!chunkTiles.isEmpty()) {
                for (Map.Entry<BlockPos, BlockEntity> entry : chunkTiles.entrySet()) {
                    final BlockPos pos = entry.getKey();
                    final int lx = pos.getX() & 15;
                    final int ly = pos.getY();
                    final int lz = pos.getZ() & 15;
                    final int layer = ly >> 4;
                    if (!set.hasSection(layer)) {
                        continue;
                    }

                    int ordinal = set.getBlock(lx, ly, lz).getOrdinal();
                    if (ordinal != 0) {
                        BlockEntity tile = entry.getValue();
                        if (PaperLib.isPaper() && tile instanceof BeaconBlockEntity) {
                            if (beacons == null) {
                                beacons = new ArrayList<>();
                            }
                            beacons.add(tile);
                            PaperweightPlatformAdapter.removeBeacon(tile, nmsChunk);
                            continue;
                        }
                        nmsChunk.removeBlockEntity(tile.getBlockPos());
                        if (createCopy) {
                            copy.storeTile(tile);
                        }
                    }
                }
            }
            final BiomeType[][] biomes = set.getBiomes();

            int bitMask = 0;
            synchronized (nmsChunk) {
                LevelChunkSection[] levelChunkSections = nmsChunk.getSections();

                for (int layerNo = getMinSectionPosition(); layerNo <= getMaxSectionPosition(); layerNo++) {

                    int getSectionIndex = layerNo - getMinSectionPosition();
                    int setSectionIndex = layerNo - set.getMinSectionPosition();

                    if (!set.hasSection(layerNo)) {
                        // No blocks, but might be biomes present. Handle this lazily.
                        if (biomes == null) {
                            continue;
                        }
                        if (layerNo < set.getMinSectionPosition() || layerNo > set.getMaxSectionPosition()) {
                            continue;
                        }
                        if (biomes[setSectionIndex] != null) {
                            synchronized (super.sectionLocks[getSectionIndex]) {
                                LevelChunkSection existingSection = levelChunkSections[getSectionIndex];
                                if (createCopy && existingSection != null) {
                                    copy.storeBiomes(getSectionIndex, existingSection.getBiomes());
                                }

                                if (existingSection == null) {
                                    PalettedContainer<Holder<Biome>> biomeData = PaperweightPlatformAdapter.getBiomePalettedContainer(
                                            biomes[setSectionIndex],
                                            biomeHolderIdMap
                                    );
                                    LevelChunkSection newSection = PaperweightPlatformAdapter.newChunkSection(
                                            layerNo,
                                            new char[4096],
                                            adapter,
                                            biomeRegistry,
                                            biomeData
                                    );
                                    if (PaperweightPlatformAdapter.setSectionAtomic(
                                            levelChunkSections,
                                            null,
                                            newSection,
                                            getSectionIndex
                                    )) {
                                        updateGet(nmsChunk, levelChunkSections, newSection, new char[4096], getSectionIndex);
                                        continue;
                                    } else {
                                        existingSection = levelChunkSections[getSectionIndex];
                                        if (existingSection == null) {
                                            LOGGER.error("Skipping invalid null section. chunk: {}, {} layer: {}", chunkX, chunkZ,
                                                    getSectionIndex
                                            );
                                            continue;
                                        }
                                    }
                                } else {
                                    setBiomesToPalettedContainer(biomes, setSectionIndex, existingSection.getBiomes());
                                }
                            }
                        }
                        continue;
                    }

                    bitMask |= 1 << getSectionIndex;

                    char[] tmp = set.load(layerNo);
                    char[] setArr = new char[4096];
                    System.arraycopy(tmp, 0, setArr, 0, 4096);

                    // synchronise on internal section to avoid circular locking with a continuing edit if the chunk was
                    // submitted to keep loaded internal chunks to queue target size.
                    synchronized (super.sectionLocks[getSectionIndex]) {

                        LevelChunkSection newSection;
                        LevelChunkSection existingSection = levelChunkSections[getSectionIndex];
                        // Don't attempt to tick section whilst we're editing
                        if (existingSection != null) {
                            PaperweightPlatformAdapter.clearCounts(existingSection);
                            if (PaperLib.isPaper()) {
                                existingSection.tickingList.clear();
                            }
                        }

                        if (createCopy) {
                            char[] tmpLoad = loadPrivately(layerNo);
                            char[] copyArr = new char[4096];
                            System.arraycopy(tmpLoad, 0, copyArr, 0, 4096);
                            copy.storeSection(getSectionIndex, copyArr);
                            if (biomes != null && existingSection != null) {
                                copy.storeBiomes(getSectionIndex, existingSection.getBiomes());
                            }
                        }

                        if (existingSection == null) {
                            PalettedContainer<Holder<Biome>> biomeData = biomes == null ? new PalettedContainer<>(
                                    biomeHolderIdMap,
                                    biomeHolderIdMap.byIdOrThrow(WorldEditPlugin
                                            .getInstance()
                                            .getBukkitImplAdapter()
                                            .getInternalBiomeId(
                                                    BiomeTypes.PLAINS)),
                                    PalettedContainer.Strategy.SECTION_BIOMES,
                                    null
                            ) : PaperweightPlatformAdapter.getBiomePalettedContainer(biomes[setSectionIndex], biomeHolderIdMap);
                            newSection = PaperweightPlatformAdapter.newChunkSection(
                                    layerNo,
                                    setArr,
                                    adapter,
                                    biomeRegistry,
                                    biomeData
                            );
                            if (PaperweightPlatformAdapter.setSectionAtomic(
                                    levelChunkSections,
                                    null,
                                    newSection,
                                    getSectionIndex
                            )) {
                                updateGet(nmsChunk, levelChunkSections, newSection, setArr, getSectionIndex);
                                continue;
                            } else {
                                existingSection = levelChunkSections[getSectionIndex];
                                if (existingSection == null) {
                                    LOGGER.error("Skipping invalid null section. chunk: {}, {} layer: {}", chunkX, chunkZ,
                                            getSectionIndex
                                    );
                                    continue;
                                }
                            }
                        }

                        //ensure that the server doesn't try to tick the chunksection while we're editing it. (Again)
                        PaperweightPlatformAdapter.clearCounts(existingSection);
                        if (PaperLib.isPaper()) {
                            existingSection.tickingList.clear();
                        }
                        DelegateSemaphore lock = PaperweightPlatformAdapter.applyLock(existingSection);

                        // Synchronize to prevent further acquisitions
                        synchronized (lock) {
                            lock.acquire(); // Wait until we have the lock
                            lock.release();
                            try {
                                sectionLock.writeLock().lock();
                                if (this.getChunk() != nmsChunk) {
                                    this.levelChunk = nmsChunk;
                                    this.sections = null;
                                    this.reset();
                                } else if (existingSection != getSections(false)[getSectionIndex]) {
                                    this.sections[getSectionIndex] = existingSection;
                                    this.reset();
                                } else if (!Arrays.equals(
                                        update(getSectionIndex, new char[4096], true),
                                        loadPrivately(layerNo)
                                )) {
                                    this.reset(layerNo);
                            /*} else if (lock.isModified()) {
                                this.reset(layerNo);*/
                                }
                            } finally {
                                sectionLock.writeLock().unlock();
                            }

                            PalettedContainer<Holder<Biome>> biomeData = setBiomesToPalettedContainer(
                                    biomes,
                                    setSectionIndex,
                                    existingSection.getBiomes()
                            );

                            newSection =
                                    PaperweightPlatformAdapter.newChunkSection(
                                            layerNo,
                                            this::loadPrivately,
                                            setArr,
                                            adapter,
                                            biomeRegistry,
                                            biomeData
                                    );
                            if (!PaperweightPlatformAdapter.setSectionAtomic(
                                    levelChunkSections,
                                    existingSection,
                                    newSection,
                                    getSectionIndex
                            )) {
                                LOGGER.error("Skipping invalid null section. chunk: {}, {} layer: {}", chunkX, chunkZ,
                                        getSectionIndex
                                );
                            } else {
                                updateGet(nmsChunk, levelChunkSections, newSection, setArr, getSectionIndex);
                            }
                        }
                    }
                }

                Map<HeightMapType, int[]> heightMaps = set.getHeightMaps();
                for (Map.Entry<HeightMapType, int[]> entry : heightMaps.entrySet()) {
                    PaperweightGetBlocks.this.setHeightmapToGet(entry.getKey(), entry.getValue());
                }
                PaperweightGetBlocks.this.setLightingToGet(
                        set.getLight(),
                        set.getMinSectionPosition(),
                        set.getMaxSectionPosition()
                );
                PaperweightGetBlocks.this.setSkyLightingToGet(
                        set.getSkyLight(),
                        set.getMinSectionPosition(),
                        set.getMaxSectionPosition()
                );

                Runnable[] syncTasks = null;

                int bx = chunkX << 4;
                int bz = chunkZ << 4;

                // Call beacon deactivate events here synchronously
                // list will be null on spigot, so this is an implicit isPaper check
                if (beacons != null && !beacons.isEmpty()) {
                    final List<BlockEntity> finalBeacons = beacons;

                    syncTasks = new Runnable[4];

                    syncTasks[3] = () -> {
                        for (BlockEntity beacon : finalBeacons) {
                            BeaconBlockEntity.playSound(beacon.getLevel(), beacon.getBlockPos(), SoundEvents.BEACON_DEACTIVATE);
                            new BeaconDeactivatedEvent(CraftBlock.at(beacon.getLevel(), beacon.getBlockPos())).callEvent();
                        }
                    };
                }

                Set<UUID> entityRemoves = set.getEntityRemoves();
                if (entityRemoves != null && !entityRemoves.isEmpty()) {
                    if (syncTasks == null) {
                        syncTasks = new Runnable[3];
                    }

                    syncTasks[2] = () -> {
                        Set<UUID> entitiesRemoved = new HashSet<>();
                        final List<Entity> entities = PaperweightPlatformAdapter.getEntities(nmsChunk);

                        for (Entity entity : entities) {
                            UUID uuid = entity.getUUID();
                            if (entityRemoves.contains(uuid)) {
                                if (createCopy) {
                                    copy.storeEntity(entity);
                                }
                                removeEntity(entity);
                                entitiesRemoved.add(uuid);
                                entityRemoves.remove(uuid);
                            }
                        }
                        if (Settings.settings().EXPERIMENTAL.REMOVE_ENTITY_FROM_WORLD_ON_CHUNK_FAIL) {
                            for (UUID uuid : entityRemoves) {
                                Entity entity = nmsWorld.getEntities().get(uuid);
                                if (entity != null) {
                                    removeEntity(entity);
                                }
                            }
                        }
                        // Only save entities that were actually removed to history
                        set.getEntityRemoves().clear();
                        set.getEntityRemoves().addAll(entitiesRemoved);
                    };
                }

                Set<CompoundTag> entities = set.getEntities();
                if (entities != null && !entities.isEmpty()) {
                    if (syncTasks == null) {
                        syncTasks = new Runnable[2];
                    }

                    syncTasks[1] = () -> {
                        Iterator<CompoundTag> iterator = entities.iterator();
                        while (iterator.hasNext()) {
                            final CompoundTag nativeTag = iterator.next();
                            final Map<String, Tag> entityTagMap = nativeTag.getValue();
                            final StringTag idTag = (StringTag) entityTagMap.get("Id");
                            final ListTag posTag = (ListTag) entityTagMap.get("Pos");
                            final ListTag rotTag = (ListTag) entityTagMap.get("Rotation");
                            if (idTag == null || posTag == null || rotTag == null) {
                                LOGGER.error("Unknown entity tag: {}", nativeTag);
                                continue;
                            }
                            final double x = posTag.getDouble(0);
                            final double y = posTag.getDouble(1);
                            final double z = posTag.getDouble(2);
                            final float yaw = rotTag.getFloat(0);
                            final float pitch = rotTag.getFloat(1);
                            final String id = idTag.getValue();

                            EntityType<?> type = EntityType.byString(id).orElse(null);
                            if (type != null) {
                                Entity entity = type.create(nmsWorld);
                                if (entity != null) {
                                    final net.minecraft.nbt.CompoundTag tag = (net.minecraft.nbt.CompoundTag) adapter.fromNative(
                                            nativeTag);
                                    for (final String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
                                        tag.remove(name);
                                    }
                                    entity.load(tag);
                                    entity.absMoveTo(x, y, z, yaw, pitch);
                                    entity.setUUID(nativeTag.getUUID());
                                    if (!nmsWorld.addFreshEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
                                        LOGGER.warn(
                                                "Error creating entity of type `{}` in world `{}` at location `{},{},{}`",
                                                id,
                                                nmsWorld.getWorld().getName(),
                                                x,
                                                y,
                                                z
                                        );
                                        // Unsuccessful create should not be saved to history
                                        iterator.remove();
                                    }
                                }
                            }
                        }
                    };
                }

                // set tiles
                Map<BlockVector3, CompoundTag> tiles = set.getTiles();
                if (tiles != null && !tiles.isEmpty()) {
                    if (syncTasks == null) {
                        syncTasks = new Runnable[1];
                    }

                    syncTasks[0] = () -> {
                        for (final Map.Entry<BlockVector3, CompoundTag> entry : tiles.entrySet()) {
                            final CompoundTag nativeTag = entry.getValue();
                            final BlockVector3 blockHash = entry.getKey();
                            final int x = blockHash.getX() + bx;
                            final int y = blockHash.getY();
                            final int z = blockHash.getZ() + bz;
                            final BlockPos pos = new BlockPos(x, y, z);

                            synchronized (nmsWorld) {
                                BlockEntity tileEntity = nmsWorld.getBlockEntity(pos);
                                if (tileEntity == null || tileEntity.isRemoved()) {
                                    nmsWorld.removeBlockEntity(pos);
                                    tileEntity = nmsWorld.getBlockEntity(pos);
                                }
                                if (tileEntity != null) {
                                    final net.minecraft.nbt.CompoundTag tag = (net.minecraft.nbt.CompoundTag) adapter.fromNative(
                                            nativeTag);
                                    tag.put("x", IntTag.valueOf(x));
                                    tag.put("y", IntTag.valueOf(y));
                                    tag.put("z", IntTag.valueOf(z));
                                    tileEntity.load(tag);
                                }
                            }
                        }
                    };
                }

                Runnable callback;
                if (bitMask == 0 && biomes == null && !lightUpdate) {
                    callback = null;
                } else {
                    int finalMask = bitMask != 0 ? bitMask : lightUpdate ? set.getBitMask() : 0;
                    boolean finalLightUpdate = lightUpdate;
                    callback = () -> {
                        // Set Modified
                        nmsChunk.setLightCorrect(true); // Set Modified
                        nmsChunk.mustNotSave = false;
                        nmsChunk.setUnsaved(true);
                        // send to player
                        if (Settings.settings().LIGHTING.MODE == 0 || !Settings.settings().LIGHTING.DELAY_PACKET_SENDING) {
                            this.send(finalMask, finalLightUpdate);
                        }
                        if (finalizer != null) {
                            finalizer.run();
                        }
                    };
                }
                if (syncTasks != null) {
                    QueueHandler queueHandler = Fawe.instance().getQueueHandler();
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
                                if (finalizer != null) {
                                    finalizer.run();
                                }
                                return null;
                            } else {
                                return queueHandler.async(callback, null);
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                            throw e;
                        }
                    };
                    //noinspection unchecked - required at compile time
                    return (T) (Future) queueHandler.sync(chain);
                } else {
                    if (callback == null) {
                        if (finalizer != null) {
                            finalizer.run();
                        }
                    } else {
                        callback.run();
                    }
                }
            }
            return null;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        } finally {
            forceLoadSections = true;
        }
    }

    private void updateGet(
            LevelChunk nmsChunk,
            LevelChunkSection[] chunkSections,
            LevelChunkSection section,
            char[] arr,
            int layer
    ) {
        try {
            sectionLock.writeLock().lock();
            if (this.getChunk() != nmsChunk) {
                this.levelChunk = nmsChunk;
                this.sections = new LevelChunkSection[chunkSections.length];
                System.arraycopy(chunkSections, 0, this.sections, 0, chunkSections.length);
                this.reset();
            }
            if (this.sections == null) {
                this.sections = new LevelChunkSection[chunkSections.length];
                System.arraycopy(chunkSections, 0, this.sections, 0, chunkSections.length);
            }
            if (this.sections[layer] != section) {
                // Not sure why it's funky, but it's what I did in commit fda7d00747abe97d7891b80ed8bb88d97e1c70d1 and I don't want to touch it >dords
                this.sections[layer] = new LevelChunkSection[]{section}.clone()[0];
            }
        } finally {
            sectionLock.writeLock().unlock();
        }
        this.blocks[layer] = arr;
    }

    private char[] loadPrivately(int layer) {
        layer -= getMinSectionPosition();
        if (super.sections[layer] != null) {
            synchronized (super.sectionLocks[layer]) {
                if (super.sections[layer].isFull() && super.blocks[layer] != null) {
                    char[] blocks = new char[4096];
                    System.arraycopy(super.blocks[layer], 0, blocks, 0, 4096);
                    return blocks;
                }
            }
        }
        return PaperweightGetBlocks.this.update(layer, null, true);
    }

    @Override
    public synchronized void send(int mask, boolean lighting) {
        PaperweightPlatformAdapter.sendChunk(serverLevel, chunkX, chunkZ, lighting);
    }

    /**
     * Update a given (nullable) data array to the current data stored in the server's chunk, associated with this
     * {@link PaperweightPlatformAdapter} instance. Not synchronised to the {@link PaperweightPlatformAdapter} instance as synchronisation
     * is handled where necessary in the method, and should otherwise be handled correctly by this method's caller.
     *
     * @param layer      layer index (0 may denote a negative layer in the world, e.g. at y=-32)
     * @param data       array to be updated/filled with data or null
     * @param aggressive if the cached section array should be re-acquired.
     * @return the given array to be filled with data, or a new array if null is given.
     */
    @Override
    @SuppressWarnings("unchecked")
    public char[] update(int layer, char[] data, boolean aggressive) {
        LevelChunkSection section = getSections(aggressive)[layer];
        // Section is null, return empty array
        if (section == null) {
            data = new char[4096];
            Arrays.fill(data, (char) BlockTypesCache.ReservedIDs.AIR);
            return data;
        }
        if (data != null && data.length != 4096) {
            data = new char[4096];
            Arrays.fill(data, (char) BlockTypesCache.ReservedIDs.AIR);
        }
        if (data == null || data == FaweCache.INSTANCE.EMPTY_CHAR_4096) {
            data = new char[4096];
            Arrays.fill(data, (char) BlockTypesCache.ReservedIDs.AIR);
        }
        Semaphore lock = PaperweightPlatformAdapter.applyLock(section);
        synchronized (lock) {
            // Efficiently convert ChunkSection to raw data
            try {
                lock.acquire();

                final PalettedContainer<BlockState> blocks = section.getStates();
                final Object dataObject = PaperweightPlatformAdapter.fieldData.get(blocks);
                final BitStorage bits = (BitStorage) PaperweightPlatformAdapter.fieldStorage.get(dataObject);

                if (bits instanceof ZeroBitStorage) {
                    Arrays.fill(data, adapter.adaptToChar(blocks.get(0, 0, 0))); // get(int) is only public on paper
                    return data;
                }

                final Palette<BlockState> palette = (Palette<BlockState>) PaperweightPlatformAdapter.fieldPalette.get(dataObject);

                final int bitsPerEntry = bits.getBits();
                final long[] blockStates = bits.getRaw();

                new BitArrayUnstretched(bitsPerEntry, 4096, blockStates).toRaw(data);

                int num_palette;
                if (palette instanceof LinearPalette || palette instanceof HashMapPalette) {
                    num_palette = palette.getSize();
                } else {
                    // The section's palette is the global block palette.
                    for (int i = 0; i < 4096; i++) {
                        char paletteVal = data[i];
                        char ordinal = adapter.ibdIDToOrdinal(paletteVal);
                        data[i] = ordinal;
                    }
                    return data;
                }

                char[] paletteToOrdinal = FaweCache.INSTANCE.PALETTE_TO_BLOCK_CHAR.get();
                try {
                    if (num_palette != 1) {
                        for (int i = 0; i < num_palette; i++) {
                            char ordinal = ordinal(palette.valueFor(i), adapter);
                            paletteToOrdinal[i] = ordinal;
                        }
                        for (int i = 0; i < 4096; i++) {
                            char paletteVal = data[i];
                            char val = paletteToOrdinal[paletteVal];
                            if (val == Character.MAX_VALUE) {
                                val = ordinal(palette.valueFor(i), adapter);
                                paletteToOrdinal[i] = val;
                            }
                            data[i] = val;
                        }
                    } else {
                        char ordinal = ordinal(palette.valueFor(0), adapter);
                        Arrays.fill(data, ordinal);
                    }
                } finally {
                    for (int i = 0; i < num_palette; i++) {
                        paletteToOrdinal[i] = Character.MAX_VALUE;
                    }
                }
                return data;
            } catch (IllegalAccessException | InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                lock.release();
            }
        }
    }

    private char ordinal(BlockState ibd, PaperweightFaweAdapter adapter) {
        if (ibd == null) {
            return BlockTypesCache.ReservedIDs.AIR;
        } else {
            return adapter.adaptToChar(ibd);
        }
    }

    public LevelChunkSection[] getSections(boolean force) {
        force &= forceLoadSections;
        sectionLock.readLock().lock();
        LevelChunkSection[] tmp = sections;
        sectionLock.readLock().unlock();
        if (tmp == null || force) {
            try {
                sectionLock.writeLock().lock();
                tmp = sections;
                if (tmp == null || force) {
                    LevelChunkSection[] chunkSections = getChunk().getSections();
                    tmp = new LevelChunkSection[chunkSections.length];
                    System.arraycopy(chunkSections, 0, tmp, 0, chunkSections.length);
                    sections = tmp;
                }
            } finally {
                sectionLock.writeLock().unlock();
            }
        }
        return tmp;
    }

    public LevelChunk getChunk() {
        LevelChunk levelChunk = this.levelChunk;
        if (levelChunk == null) {
            synchronized (this) {
                levelChunk = this.levelChunk;
                if (levelChunk == null) {
                    this.levelChunk = levelChunk = ensureLoaded(this.serverLevel, chunkX, chunkZ);
                }
            }
        }
        return levelChunk;
    }

    private void fillLightNibble(char[][] light, LightLayer lightLayer, int minSectionPosition, int maxSectionPosition) {
        for (int Y = 0; Y <= maxSectionPosition - minSectionPosition; Y++) {
            if (light[Y] == null) {
                continue;
            }
            SectionPos sectionPos = SectionPos.of(levelChunk.getPos(), Y + minSectionPosition);
            DataLayer dataLayer = serverLevel.getChunkSource().getLightEngine().getLayerListener(lightLayer).getDataLayerData(
                    sectionPos);
            if (dataLayer == null) {
                byte[] LAYER_COUNT = new byte[2048];
                Arrays.fill(LAYER_COUNT, lightLayer == LightLayer.SKY ? (byte) 15 : (byte) 0);
                dataLayer = new DataLayer(LAYER_COUNT);
                ((LevelLightEngine) serverLevel.getChunkSource().getLightEngine()).queueSectionData(
                        lightLayer,
                        sectionPos,
                        dataLayer,
                        true
                );
            }
            synchronized (dataLayer) {
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            int i = y << 8 | z << 4 | x;
                            if (light[Y][i] < 16) {
                                dataLayer.set(x, y, z, light[Y][i]);
                            }
                        }
                    }
                }
            }
        }
    }

    private PalettedContainer<Holder<Biome>> setBiomesToPalettedContainer(
            final BiomeType[][] biomes,
            final int sectionIndex,
            final PalettedContainerRO<Holder<Biome>> data
    ) {
        PalettedContainer<Holder<Biome>> biomeData;
        if (data instanceof PalettedContainer<Holder<Biome>> palettedContainer) {
            biomeData = palettedContainer;
        } else {
            LOGGER.warn(
                    "Cannot correctly set biomes to world, existing biomes may be lost. Expected class " +
                            "type {} but got {}",
                    PalettedContainer.class.getSimpleName(),
                    data.getClass().getSimpleName()
            );
            biomeData = data.recreate();
        }
        BiomeType[] sectionBiomes;
        if (biomes == null || (sectionBiomes = biomes[sectionIndex]) == null) {
            return biomeData;
        }
        for (int y = 0, index = 0; y < 4; y++) {
            for (int z = 0; z < 4; z++) {
                for (int x = 0; x < 4; x++, index++) {
                    BiomeType biomeType = sectionBiomes[index];
                    if (biomeType == null) {
                        continue;
                    }
                    biomeData.set(
                            x,
                            y,
                            z,
                            biomeHolderIdMap.byIdOrThrow(WorldEditPlugin
                                    .getInstance()
                                    .getBukkitImplAdapter()
                                    .getInternalBiomeId(biomeType))
                    );
                }
            }
        }
        return biomeData;
    }

    @Override
    public boolean hasSection(int layer) {
        layer -= getMinSectionPosition();
        return getSections(false)[layer] != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized boolean trim(boolean aggressive) {
        skyLight = new DataLayer[getSectionCount()];
        blockLight = new DataLayer[getSectionCount()];
        if (aggressive) {
            sectionLock.writeLock().lock();
            sections = null;
            levelChunk = null;
            sectionLock.writeLock().unlock();
            return super.trim(true);
        } else if (sections == null) {
            // don't bother trimming if there are no sections stored.
            return true;
        } else {
            for (int i = getMinSectionPosition(); i <= getMaxSectionPosition(); i++) {
                int layer = i - getMinSectionPosition();
                if (!hasSection(i) || !super.sections[layer].isFull()) {
                    continue;
                }
                LevelChunkSection existing = getSections(true)[layer];
                try {
                    final PalettedContainer<BlockState> blocksExisting = existing.getStates();

                    final Object dataObject = PaperweightPlatformAdapter.fieldData.get(blocksExisting);
                    final Palette<BlockState> palette = (Palette<BlockState>) PaperweightPlatformAdapter.fieldPalette.get(
                            dataObject);
                    int paletteSize;

                    if (palette instanceof LinearPalette || palette instanceof HashMapPalette) {
                        paletteSize = palette.getSize();
                    } else {
                        super.trim(false, i);
                        continue;
                    }
                    if (paletteSize == 1) {
                        //If the cached palette size is 1 then no blocks can have been changed i.e. do not need to update these chunks.
                        continue;
                    }
                    super.trim(false, i);
                } catch (IllegalAccessException ignored) {
                    super.trim(false, i);
                }
            }
            return true;
        }
    }

}
