package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.regen;

import com.fastasyncworldedit.bukkit.adapter.Regenerator;
import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.queue.IChunkCache;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.util.ReflectionUtils;
import com.fastasyncworldedit.core.util.TaskManager;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Lifecycle;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.Refraction;
import com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R2.PaperweightGetBlocks;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.io.file.SafeFiles;
import com.sk89q.worldedit.world.RegenOptions;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTaskPriorityQueueSorter.Message;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.v1_20_R2.CraftServer;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R2.generator.CustomChunkGenerator;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static net.minecraft.core.registries.Registries.BIOME;

public class PaperweightRegen extends Regenerator<ChunkAccess, ProtoChunk, LevelChunk, PaperweightRegen.ChunkStatusWrap> {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private static final Field serverWorldsField;
    private static final Field paperConfigField;
    private static final Field flatBedrockField;
    private static final Field generatorSettingFlatField;
    private static final Field generatorSettingBaseSupplierField;
    private static final Field delegateField;
    private static final Field chunkSourceField;
    private static final Field generatorStructureStateField;
    private static final Field ringPositionsField;
    private static final Field hasGeneratedPositionsField;

    //list of chunk stati in correct order without FULL
    private static final Map<ChunkStatus, Concurrency> chunkStati = new LinkedHashMap<>();

    static {
        chunkStati.put(ChunkStatus.EMPTY, Concurrency.FULL);   // empty: radius -1, does nothing
        chunkStati.put(ChunkStatus.STRUCTURE_STARTS, Concurrency.NONE);   // structure starts: uses unsynchronized maps
        chunkStati.put(
                ChunkStatus.STRUCTURE_REFERENCES,
                Concurrency.FULL
        );   // structure refs: radius 8, but only writes to current chunk
        chunkStati.put(ChunkStatus.BIOMES, Concurrency.FULL);   // biomes: radius 0
        chunkStati.put(ChunkStatus.NOISE, Concurrency.RADIUS); // noise: radius 8
        chunkStati.put(ChunkStatus.SURFACE, Concurrency.NONE);   // surface: radius 0, requires NONE
        chunkStati.put(ChunkStatus.CARVERS, Concurrency.NONE);   // carvers: radius 0, but RADIUS and FULL change results
        /*chunkStati.put(
                ChunkStatus.LIQUID_CARVERS,
                Concurrency.NONE
        );   // liquid carvers: radius 0, but RADIUS and FULL change results*/
        chunkStati.put(ChunkStatus.FEATURES, Concurrency.NONE);   // features: uses unsynchronized maps
        chunkStati.put(
                ChunkStatus.LIGHT,
                Concurrency.FULL
        );   // light: radius 1, but no writes to other chunks, only current chunk
        chunkStati.put(ChunkStatus.SPAWN, Concurrency.FULL);   // spawn: radius 0
        // chunkStati.put(ChunkStatus.HEIGHTMAPS, Concurrency.FULL);   // heightmaps: radius 0

        try {
            serverWorldsField = CraftServer.class.getDeclaredField("worlds");
            serverWorldsField.setAccessible(true);

            Field tmpPaperConfigField;
            Field tmpFlatBedrockField;
            try { //only present on paper
                tmpPaperConfigField = Level.class.getDeclaredField("paperConfig");
                tmpPaperConfigField.setAccessible(true);

                tmpFlatBedrockField = tmpPaperConfigField.getType().getDeclaredField("generateFlatBedrock");
                tmpFlatBedrockField.setAccessible(true);
            } catch (Exception e) {
                tmpPaperConfigField = null;
                tmpFlatBedrockField = null;
            }
            paperConfigField = tmpPaperConfigField;
            flatBedrockField = tmpFlatBedrockField;

            generatorSettingBaseSupplierField = NoiseBasedChunkGenerator.class.getDeclaredField(Refraction.pickName(
                    "settings", "e"));
            generatorSettingBaseSupplierField.setAccessible(true);

            generatorSettingFlatField = FlatLevelSource.class.getDeclaredField(Refraction.pickName("settings", "d"));
            generatorSettingFlatField.setAccessible(true);

            delegateField = CustomChunkGenerator.class.getDeclaredField("delegate");
            delegateField.setAccessible(true);

            chunkSourceField = ServerLevel.class.getDeclaredField(Refraction.pickName("chunkSource", "I"));
            chunkSourceField.setAccessible(true);

            generatorStructureStateField = ChunkMap.class.getDeclaredField(Refraction.pickName("chunkGeneratorState", "v"));
            generatorStructureStateField.setAccessible(true);

            ringPositionsField = ChunkGeneratorStructureState.class.getDeclaredField(Refraction.pickName("ringPositions", "g"));
            ringPositionsField.setAccessible(true);

            hasGeneratedPositionsField = ChunkGeneratorStructureState.class.getDeclaredField(
                    Refraction.pickName("hasGeneratedPositions", "h")
            );
            hasGeneratedPositionsField.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //runtime
    private ServerLevel originalServerWorld;
    private ServerChunkCache originalChunkProvider;
    private ServerLevel freshWorld;
    private ServerChunkCache freshChunkProvider;
    private LevelStorageSource.LevelStorageAccess session;
    private StructureTemplateManager structureTemplateManager;
    private ThreadedLevelLightEngine threadedLevelLightEngine;
    private ChunkGenerator chunkGenerator;

    private Path tempDir;

    private boolean generateFlatBedrock = false;

    public PaperweightRegen(org.bukkit.World originalBukkitWorld, Region region, Extent target, RegenOptions options) {
        super(originalBukkitWorld, region, target, options);
    }

    @Override
    protected boolean prepare() {
        this.originalServerWorld = ((CraftWorld) originalBukkitWorld).getHandle();
        originalChunkProvider = originalServerWorld.getChunkSource();

        //flat bedrock? (only on paper)
        if (paperConfigField != null) {
            try {
                generateFlatBedrock = flatBedrockField.getBoolean(paperConfigField.get(originalServerWorld));
            } catch (Exception ignored) {
            }
        }

        seed = options.getSeed().orElse(originalServerWorld.getSeed());
        chunkStati.forEach((s, c) -> super.chunkStatuses.put(new ChunkStatusWrap(s), c));

        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected boolean initNewWorld() throws Exception {
        //world folder
        tempDir = java.nio.file.Files.createTempDirectory("FastAsyncWorldEditWorldGen");

        //prepare for world init (see upstream implementation for reference)
        org.bukkit.World.Environment environment = originalBukkitWorld.getEnvironment();
        org.bukkit.generator.ChunkGenerator generator = originalBukkitWorld.getGenerator();
        LevelStorageSource levelStorageSource = LevelStorageSource.createDefault(tempDir);
        ResourceKey<LevelStem> levelStemResourceKey = getWorldDimKey(environment);
        session = levelStorageSource.createAccess("faweregentempworld", levelStemResourceKey);
        PrimaryLevelData originalWorldData = originalServerWorld.serverLevelData;

        MinecraftServer server = originalServerWorld.getCraftServer().getServer();
        WorldOptions originalOpts = originalWorldData.worldGenOptions();
        WorldOptions newOpts = options.getSeed().isPresent()
                ? originalOpts.withSeed(OptionalLong.of(seed))
                : originalOpts;
        LevelSettings newWorldSettings = new LevelSettings(
                "faweregentempworld",
                originalWorldData.settings.gameType(),
                originalWorldData.settings.hardcore(),
                originalWorldData.settings.difficulty(),
                originalWorldData.settings.allowCommands(),
                originalWorldData.settings.gameRules(),
                originalWorldData.settings.getDataConfiguration()
        );

        PrimaryLevelData.SpecialWorldProperty specialWorldProperty =
                originalWorldData.isFlatWorld()
                        ? PrimaryLevelData.SpecialWorldProperty.FLAT
                        : originalWorldData.isDebugWorld()
                                ? PrimaryLevelData.SpecialWorldProperty.DEBUG
                                : PrimaryLevelData.SpecialWorldProperty.NONE;
        PrimaryLevelData newWorldData = new PrimaryLevelData(newWorldSettings, newOpts, specialWorldProperty, Lifecycle.stable());

        BiomeProvider biomeProvider = getBiomeProvider();


        //init world
        freshWorld = Fawe.instance().getQueueHandler().sync((Supplier<ServerLevel>) () -> new ServerLevel(
                server,
                server.executor,
                session,
                newWorldData,
                originalServerWorld.dimension(),
                DedicatedServer.getServer().registryAccess().registry(Registries.LEVEL_STEM).orElseThrow()
                        .getOrThrow(levelStemResourceKey),
                new RegenNoOpWorldLoadListener(),
                originalServerWorld.isDebug(),
                seed,
                ImmutableList.of(),
                false,
                originalServerWorld.getRandomSequences(),
                environment,
                generator,
                biomeProvider
        ) {

            private final Holder<Biome> singleBiome = options.hasBiomeType() ? DedicatedServer.getServer().registryAccess()
                    .registryOrThrow(BIOME).asHolderIdMap().byIdOrThrow(
                            WorldEditPlugin.getInstance().getBukkitImplAdapter().getInternalBiomeId(options.getBiomeType())
                    ) : null;

            @Override
            public void tick(BooleanSupplier shouldKeepTicking) { //no ticking
            }

            @Override
            public Holder<Biome> getUncachedNoiseBiome(int biomeX, int biomeY, int biomeZ) {
                if (options.hasBiomeType()) {
                    return singleBiome;
                }
                return PaperweightRegen.this.chunkGenerator.getBiomeSource().getNoiseBiome(
                        biomeX, biomeY, biomeZ, getChunkSource().randomState().sampler()
                );
            }
        }).get();
        freshWorld.noSave = true;
        removeWorldFromWorldsMap();
        newWorldData.checkName(originalServerWorld.serverLevelData.getLevelName()); //rename to original world name
        if (paperConfigField != null) {
            paperConfigField.set(freshWorld, originalServerWorld.paperConfig());
        }

        ChunkGenerator originalGenerator = originalChunkProvider.getGenerator();
        if (originalGenerator instanceof FlatLevelSource flatLevelSource) {
            FlatLevelGeneratorSettings generatorSettingFlat = flatLevelSource.settings();
            chunkGenerator = new FlatLevelSource(generatorSettingFlat);
        } else if (originalGenerator instanceof NoiseBasedChunkGenerator noiseBasedChunkGenerator) {
            Holder<NoiseGeneratorSettings> generatorSettingBaseSupplier = (Holder<NoiseGeneratorSettings>) generatorSettingBaseSupplierField.get(
                    originalGenerator);
            BiomeSource biomeSource;
            if (options.hasBiomeType()) {

                biomeSource = new FixedBiomeSource(
                        DedicatedServer.getServer().registryAccess()
                                .registryOrThrow(BIOME).asHolderIdMap().byIdOrThrow(
                                        WorldEditPlugin.getInstance().getBukkitImplAdapter().getInternalBiomeId(options.getBiomeType())
                                )
                );
            } else {
                biomeSource = originalGenerator.getBiomeSource();
            }
            chunkGenerator = new NoiseBasedChunkGenerator(
                    biomeSource,
                    generatorSettingBaseSupplier
            );
        } else if (originalGenerator instanceof CustomChunkGenerator customChunkGenerator) {
            chunkGenerator = customChunkGenerator.getDelegate();
        } else {
            LOGGER.error("Unsupported generator type {}", originalGenerator.getClass().getName());
            return false;
        }
        if (generator != null) {
            chunkGenerator = new CustomChunkGenerator(freshWorld, chunkGenerator, generator);
            generateConcurrent = generator.isParallelCapable();
        }
//        chunkGenerator.conf = freshWorld.spigotConfig; - Does not exist anymore, may need to be re-addressed

        freshChunkProvider = new ServerChunkCache(
                freshWorld,
                session,
                server.getFixerUpper(),
                server.getStructureManager(),
                server.executor,
                chunkGenerator,
                freshWorld.spigotConfig.viewDistance,
                freshWorld.spigotConfig.simulationDistance,
                server.forceSynchronousWrites(),
                new RegenNoOpWorldLoadListener(),
                (chunkCoordIntPair, state) -> {
                },
                () -> server.overworld().getDataStorage()
        ) {
            // redirect to LevelChunks created in #createChunks
            @Override
            public ChunkAccess getChunk(int x, int z, ChunkStatus chunkstatus, boolean create) {
                ChunkAccess chunkAccess = getChunkAt(x, z);
                if (chunkAccess == null && create) {
                    chunkAccess = createChunk(getProtoChunkAt(x, z));
                }
                return chunkAccess;
            }
        };

        if (seed == originalOpts.seed() && !options.hasBiomeType()) {
            // Optimisation for needless ring position calculation when the seed and biome is the same.
            ChunkGeneratorStructureState state = (ChunkGeneratorStructureState) generatorStructureStateField.get(originalChunkProvider.chunkMap);
            boolean hasGeneratedPositions = hasGeneratedPositionsField.getBoolean(state);
            if (hasGeneratedPositions) {
                Map<ConcentricRingsStructurePlacement, CompletableFuture<List<ChunkPos>>> origPositions =
                        (Map<ConcentricRingsStructurePlacement, CompletableFuture<List<ChunkPos>>>) ringPositionsField.get(state);
                Map<ConcentricRingsStructurePlacement, CompletableFuture<List<ChunkPos>>> copy = new Object2ObjectArrayMap<>(
                        origPositions);
                ChunkGeneratorStructureState newState = (ChunkGeneratorStructureState) generatorStructureStateField.get(freshChunkProvider.chunkMap);
                ringPositionsField.set(newState, copy);
                hasGeneratedPositionsField.setBoolean(newState, true);
            }
        }


        chunkSourceField.set(freshWorld, freshChunkProvider);
        //let's start then
        structureTemplateManager = server.getStructureManager();
        threadedLevelLightEngine = new NoOpLightEngine(freshChunkProvider);

        return true;
    }

    @Override
    protected void cleanup() {
        try {
            session.close();
        } catch (Exception ignored) {
        }

        //shutdown chunk provider
        try {
            Fawe.instance().getQueueHandler().sync(() -> {
                try {
                    freshChunkProvider.close(false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception ignored) {
        }

        //remove world from server
        try {
            Fawe.instance().getQueueHandler().sync(this::removeWorldFromWorldsMap);
        } catch (Exception ignored) {
        }

        //delete directory
        try {
            SafeFiles.tryHardToDeleteDir(tempDir);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected ProtoChunk createProtoChunk(int x, int z) {
        return new FastProtoChunk(new ChunkPos(x, z), UpgradeData.EMPTY, freshWorld,
                this.freshWorld.registryAccess().registryOrThrow(BIOME), null
        );
    }

    @Override
    protected LevelChunk createChunk(ProtoChunk protoChunk) {
        return new LevelChunk(
                freshWorld,
                protoChunk,
                null // we don't want to add entities
        );
    }

    @Override
    protected ChunkStatusWrap getFullChunkStatus() {
        return new ChunkStatusWrap(ChunkStatus.FULL);
    }

    @Override
    protected List<BlockPopulator> getBlockPopulators() {
        return originalServerWorld.getWorld().getPopulators();
    }

    @Override
    protected void populate(LevelChunk levelChunk, Random random, BlockPopulator blockPopulator) {
        // BlockPopulator#populate has to be called synchronously for TileEntity access
        TaskManager.taskManager().task(() -> {
            final CraftWorld world = freshWorld.getWorld();
            final Chunk chunk = world.getChunkAt(levelChunk.locX, levelChunk.locZ);
            blockPopulator.populate(world, random, chunk);
        });
    }

    @Override
    protected IChunkCache<IChunkGet> initSourceQueueCache() {
        return (chunkX, chunkZ) -> new PaperweightGetBlocks(freshWorld, chunkX, chunkZ) {
            @Override
            public LevelChunk ensureLoaded(ServerLevel nmsWorld, int x, int z) {
                return getChunkAt(x, z);
            }
        };
    }

    //util
    @SuppressWarnings("unchecked")
    private void removeWorldFromWorldsMap() {
        Fawe.instance().getQueueHandler().sync(() -> {
            try {
                Map<String, org.bukkit.World> map = (Map<String, org.bukkit.World>) serverWorldsField.get(Bukkit.getServer());
                map.remove("faweregentempworld");
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private ResourceKey<LevelStem> getWorldDimKey(org.bukkit.World.Environment env) {
        return switch (env) {
            case NETHER -> LevelStem.NETHER;
            case THE_END -> LevelStem.END;
            default -> LevelStem.OVERWORLD;
        };
    }

    private static class RegenNoOpWorldLoadListener implements ChunkProgressListener {

        private RegenNoOpWorldLoadListener() {
        }

        @Override
        public void updateSpawnPos(ChunkPos spawnPos) {
        }

        @Override
        public void onStatusChange(ChunkPos pos, @Nullable ChunkStatus status) {
        }

        @Override
        public void start() {

        }

        @Override
        public void stop() {
        }

        // TODO Paper only(?) @Override
        public void setChunkRadius(int radius) {
        }

    }

    private class FastProtoChunk extends ProtoChunk {

        public FastProtoChunk(
                final ChunkPos pos,
                final UpgradeData upgradeData,
                final LevelHeightAccessor world,
                final Registry<Biome> biomeRegistry,
                @Nullable final BlendingData blendingData
        ) {
            super(pos, upgradeData, world, biomeRegistry, blendingData);
        }

        // avoid warning on paper

        // compatibility with spigot

        public boolean generateFlatBedrock() {
            return generateFlatBedrock;
        }

        // no one will ever see the entities!
        @Override
        public List<CompoundTag> getEntities() {
            return Collections.emptyList();
        }

    }

    protected class ChunkStatusWrap extends ChunkStatusWrapper<ChunkAccess> {

        private final ChunkStatus chunkStatus;

        public ChunkStatusWrap(ChunkStatus chunkStatus) {
            this.chunkStatus = chunkStatus;
        }

        @Override
        public int requiredNeighborChunkRadius() {
            return chunkStatus.getRange();
        }

        @Override
        public String name() {
            return chunkStatus.toString();
        }

        @Override
        public CompletableFuture<?> processChunk(List<ChunkAccess> accessibleChunks) {
            return chunkStatus.generate(
                    Runnable::run, // TODO revisit, we might profit from this somehow?
                    freshWorld,
                    chunkGenerator,
                    structureTemplateManager,
                    threadedLevelLightEngine,
                    c -> CompletableFuture.completedFuture(Either.left(c)),
                    accessibleChunks
            );
        }

    }

    /**
     * A light engine that does nothing. As light is calculated after pasting anyway, we can avoid
     * work this way.
     */
    static class NoOpLightEngine extends ThreadedLevelLightEngine {

        private static final ProcessorMailbox<Runnable> MAILBOX = ProcessorMailbox.create(task -> {
        }, "fawe-no-op");
        private static final ProcessorHandle<Message<Runnable>> HANDLE = ProcessorHandle.of("fawe-no-op", m -> {
        });

        public NoOpLightEngine(final ServerChunkCache chunkProvider) {
            super(chunkProvider, chunkProvider.chunkMap, false, MAILBOX, HANDLE);
        }

        @Override
        public CompletableFuture<ChunkAccess> lightChunk(final ChunkAccess chunk, final boolean excludeBlocks) {
            return CompletableFuture.completedFuture(chunk);
        }

    }

}
