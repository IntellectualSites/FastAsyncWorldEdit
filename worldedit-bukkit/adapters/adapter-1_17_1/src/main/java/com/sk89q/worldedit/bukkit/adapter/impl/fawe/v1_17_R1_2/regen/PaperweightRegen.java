package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_17_R1_2.regen;

import com.fastasyncworldedit.bukkit.adapter.Regenerator;
import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.queue.IChunkCache;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.util.ReflectionUtils;
import com.fastasyncworldedit.core.util.TaskManager;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.sk89q.worldedit.bukkit.adapter.Refraction;
import com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_17_R1_2.PaperweightGetBlocks;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.io.file.SafeFiles;
import com.sk89q.worldedit.world.RegenOptions;
import io.papermc.lib.PaperLib;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.biome.Biomes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.biome.OverworldBiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SimpleRandomSource;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.newbiome.area.Area;
import net.minecraft.world.level.newbiome.area.AreaFactory;
import net.minecraft.world.level.newbiome.context.BigContext;
import net.minecraft.world.level.newbiome.layer.Layer;
import net.minecraft.world.level.newbiome.layer.Layers;
import net.minecraft.world.level.newbiome.layer.traits.PixelTransformer;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.generator.CustomChunkGenerator;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PaperweightRegen extends Regenerator<ChunkAccess, ProtoChunk, LevelChunk, PaperweightRegen.ChunkStatusWrap> {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private static final Field worldsField;
    private static final Field paperConfigField;
    private static final Field generateFlatBedrockField;
    private static final Field generatorSettingFlatField;
    private static final Field generatorSettingBaseSupplierField;
    private static final Field delegateField;
    private static final Field chunkSourceField;

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
        chunkStati.put(
                ChunkStatus.LIQUID_CARVERS,
                Concurrency.NONE
        );   // liquid carvers: radius 0, but RADIUS and FULL change results
        chunkStati.put(ChunkStatus.FEATURES, Concurrency.NONE);   // features: uses unsynchronized maps
        chunkStati.put(
                ChunkStatus.LIGHT,
                Concurrency.FULL
        );   // light: radius 1, but no writes to other chunks, only current chunk
        chunkStati.put(ChunkStatus.SPAWN, Concurrency.FULL);   // spawn: radius 0
        chunkStati.put(ChunkStatus.HEIGHTMAPS, Concurrency.FULL);   // heightmaps: radius 0

        try {
            worldsField = CraftServer.class.getDeclaredField("worlds");
            worldsField.setAccessible(true);

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
            generateFlatBedrockField = tmpFlatBedrockField;

            generatorSettingBaseSupplierField = NoiseBasedChunkGenerator.class.getDeclaredField(Refraction.pickName(
                    "settings", "g"));
            generatorSettingBaseSupplierField.setAccessible(true);

            generatorSettingFlatField = FlatLevelSource.class.getDeclaredField(Refraction.pickName("settings", "e"));
            generatorSettingFlatField.setAccessible(true);

            delegateField = CustomChunkGenerator.class.getDeclaredField("delegate");
            delegateField.setAccessible(true);

            chunkSourceField = ServerLevel.class.getDeclaredField(Refraction.pickName("chunkSource", "C"));
            chunkSourceField.setAccessible(true);
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
    private StructureManager structureManager;
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
                generateFlatBedrock = generateFlatBedrockField.getBoolean(paperConfigField.get(originalServerWorld));
            } catch (Exception ignored) {
            }
        }

        seed = options.getSeed().orElse(originalServerWorld.getSeed());
        chunkStati.forEach((s, c) -> super.chunkStatuses.put(new ChunkStatusWrap(s), c));

        return true;
    }

    @Override
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

        BiomeProvider biomeProvider = getBiomeProvider();

        MinecraftServer server = originalServerWorld.getCraftServer().getServer();
        WorldGenSettings originalOpts = originalWorldData.worldGenSettings();
        WorldGenSettings newOpts = options.getSeed().isPresent()
                ? originalOpts.withSeed(originalWorldData.isHardcore(), OptionalLong.of(seed))
                : originalOpts;
        LevelSettings newWorldSettings = new LevelSettings(
                "faweregentempworld",
                originalWorldData.settings.gameType(),
                originalWorldData.settings.hardcore(),
                originalWorldData.settings.difficulty(),
                originalWorldData.settings.allowCommands(),
                originalWorldData.settings.gameRules(),
                originalWorldData.settings.getDataPackConfig()
        );
        PrimaryLevelData newWorldData = new PrimaryLevelData(newWorldSettings, newOpts, Lifecycle.stable());

        //init world
        freshWorld = Fawe.instance().getQueueHandler().sync((Supplier<ServerLevel>) () -> new ServerLevel(
                server,
                server.executor,
                session,
                newWorldData,
                originalServerWorld.dimension(),
                originalServerWorld.dimensionType(),
                new RegenNoOpWorldLoadListener(),
                // placeholder. Required for new ChunkProviderServer, but we create and then set it later
                newOpts.dimensions().get(levelStemResourceKey).generator(),
                originalServerWorld.isDebug(),
                seed,
                ImmutableList.of(),
                false,
                environment,
                generator,
                biomeProvider
        ) {
            private final Biome singleBiome = options.hasBiomeType() ? BuiltinRegistries.BIOME.get(ResourceLocation.tryParse(
                    options
                            .getBiomeType()
                            .getId())) : null;

            @Override
            public void tick(BooleanSupplier shouldKeepTicking) { //no ticking
            }

            @Override
            public Biome getUncachedNoiseBiome(int biomeX, int biomeY, int biomeZ) {
                if (options.hasBiomeType()) {
                    return singleBiome;
                }
                return PaperweightRegen.this.chunkGenerator.getBiomeSource().getNoiseBiome(biomeX, biomeY, biomeZ);
            }
        }).get();
        freshWorld.noSave = true;
        removeWorldFromWorldsMap();
        newWorldData.checkName(originalServerWorld.serverLevelData.getLevelName()); //rename to original world name
        if (paperConfigField != null) {
            paperConfigField.set(freshWorld, originalServerWorld.paperConfig);
        }

        //generator
        if (originalChunkProvider.getGenerator() instanceof FlatLevelSource) {
            FlatLevelGeneratorSettings generatorSettingFlat = (FlatLevelGeneratorSettings) generatorSettingFlatField.get(
                    originalChunkProvider.getGenerator());
            chunkGenerator = new FlatLevelSource(generatorSettingFlat);
        } else if (originalChunkProvider.getGenerator() instanceof NoiseBasedChunkGenerator) {
            Supplier<NoiseGeneratorSettings> generatorSettingBaseSupplier = (Supplier<NoiseGeneratorSettings>) generatorSettingBaseSupplierField
                    .get(originalChunkProvider.getGenerator());
            BiomeSource biomeSource;
            if (options.hasBiomeType()) {
                biomeSource = new FixedBiomeSource(BuiltinRegistries.BIOME.get(ResourceLocation.tryParse(options
                        .getBiomeType()
                        .getId())));
            } else {
                biomeSource = originalChunkProvider.getGenerator().getBiomeSource();
                if (biomeSource instanceof OverworldBiomeSource) {
                    biomeSource = fastOverworldBiomeSource(biomeSource);
                }
            }
            chunkGenerator = new NoiseBasedChunkGenerator(biomeSource, seed, generatorSettingBaseSupplier);
        } else if (originalChunkProvider.getGenerator() instanceof CustomChunkGenerator) {
            chunkGenerator = (ChunkGenerator) delegateField.get(originalChunkProvider.getGenerator());
        } else {
            LOGGER.error("Unsupported generator type {}", originalChunkProvider.getGenerator().getClass().getName());
            return false;
        }
        if (generator != null) {
            chunkGenerator = new CustomChunkGenerator(freshWorld, chunkGenerator, generator);
            generateConcurrent = generator.isParallelCapable();
        }

        freshChunkProvider = new ServerChunkCache(
                freshWorld,
                session,
                server.getFixerUpper(),
                server.getStructureManager(),
                server.executor,
                chunkGenerator,
                freshWorld.spigotConfig.viewDistance,
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

        chunkSourceField.set(freshWorld, freshChunkProvider);
        //let's start then
        structureManager = server.getStructureManager();
        threadedLevelLightEngine = freshChunkProvider.getLightEngine();

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
                } catch (IOException e) {
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
        return PaperLib.isPaper()
                ? new FastProtoChunk(new ChunkPos(x, z), UpgradeData.EMPTY, freshWorld, freshWorld) // paper
                : new FastProtoChunk(new ChunkPos(x, z), UpgradeData.EMPTY, freshWorld); // spigot
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
        TaskManager.taskManager().task(() -> blockPopulator.populate(freshWorld.getWorld(), random, levelChunk.getBukkitChunk()));
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
                Map<String, org.bukkit.World> map = (Map<String, org.bukkit.World>) worldsField.get(Bukkit.getServer());
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private BiomeSource fastOverworldBiomeSource(BiomeSource biomeSource) throws Exception {
        Field legacyBiomeInitLayerField = OverworldBiomeSource.class.getDeclaredField(
                Refraction.pickName("legacyBiomeInitLayer", "i"));
        legacyBiomeInitLayerField.setAccessible(true);
        Field largeBiomesField = OverworldBiomeSource.class.getDeclaredField(Refraction.pickName("largeBiomes", "j"));
        largeBiomesField.setAccessible(true);
        Field biomeRegistryField = OverworldBiomeSource.class.getDeclaredField(Refraction.pickName("biomes", "k"));
        biomeRegistryField.setAccessible(true);
        Field areaLazyField = Layer.class.getDeclaredField(Refraction.pickName("area", "b"));
        areaLazyField.setAccessible(true);
        Method initAreaFactoryMethod = Layers.class.getDeclaredMethod(
                Refraction.pickName("getDefaultLayer", "a"),
                boolean.class,
                int.class,
                int.class,
                LongFunction.class
        );
        initAreaFactoryMethod.setAccessible(true);

        //init new WorldChunkManagerOverworld
        boolean legacyBiomeInitLayer = legacyBiomeInitLayerField.getBoolean(biomeSource);
        boolean largebiomes = largeBiomesField.getBoolean(biomeSource);
        Registry<Biome> biomeRegistryMojang = (Registry<Biome>) biomeRegistryField.get(biomeSource);
        Registry<Biome> biomeRegistry;
        if (options.hasBiomeType()) {
            Biome biome = BuiltinRegistries.BIOME.get(ResourceLocation.tryParse(options.getBiomeType().getId()));
            biomeRegistry = new MappedRegistry<>(
                    ResourceKey.createRegistryKey(new ResourceLocation("fawe_biomes")),
                    Lifecycle.experimental()
            );
            ((MappedRegistry) biomeRegistry).registerMapping(0, BuiltinRegistries.BIOME.getResourceKey(biome).get(), biome,
                    Lifecycle.experimental()
            );
        } else {
            biomeRegistry = biomeRegistryMojang;
        }

        //replace genLayer
        AreaFactory<FastAreaLazy> factory = (AreaFactory<FastAreaLazy>) initAreaFactoryMethod.invoke(
                null,
                legacyBiomeInitLayer,
                largebiomes ? 6 : 4,
                4,
                (LongFunction) (salt -> new FastWorldGenContextArea(seed, salt))
        );
        biomeSource = new FastOverworldBiomeSource(biomeRegistry, new FastGenLayer(factory));

        return biomeSource;
    }

    private static class FastOverworldBiomeSource extends BiomeSource {

        private final Registry<Biome> biomeRegistry;
        private final boolean isSingleRegistry;
        private final FastGenLayer fastGenLayer;

        public FastOverworldBiomeSource(
                Registry<Biome> biomeRegistry,
                FastGenLayer genLayer
        ) {
            super(biomeRegistry.stream().collect(Collectors.toList()));
            this.biomeRegistry = biomeRegistry;
            this.isSingleRegistry = biomeRegistry.entrySet().size() == 1;
            this.fastGenLayer = genLayer;
        }

        @Override
        protected Codec<? extends BiomeSource> codec() {
            return OverworldBiomeSource.CODEC;
        }

        @Override
        public BiomeSource withSeed(final long seed) {
            return null;
        }

        @Override
        public Biome getNoiseBiome(int biomeX, int biomeY, int biomeZ) {
            if (this.isSingleRegistry) {
                return this.biomeRegistry.byId(0);
            }
            return this.fastGenLayer.get(this.biomeRegistry, biomeX, biomeZ);
        }

    }

    private static class FastWorldGenContextArea implements BigContext<FastAreaLazy> {

        private final ConcurrentHashMap<Long, Integer> sharedAreaMap = new ConcurrentHashMap<>();
        private final ImprovedNoise improvedNoise;
        private final long magicrandom;
        private final ConcurrentHashMap<Long, Long> map = new ConcurrentHashMap<>(); //needed for multithreaded generation

        public FastWorldGenContextArea(long seed, long lconst) {
            this.magicrandom = mix(seed, lconst);
            this.improvedNoise = new ImprovedNoise(new SimpleRandomSource(seed));
        }

        private static long mix(long seed, long salt) {
            long l = LinearCongruentialGenerator.next(salt, salt);
            l = LinearCongruentialGenerator.next(l, salt);
            l = LinearCongruentialGenerator.next(l, salt);
            long m = LinearCongruentialGenerator.next(seed, l);
            m = LinearCongruentialGenerator.next(m, l);
            m = LinearCongruentialGenerator.next(m, l);
            return m;
        }

        @Override
        public FastAreaLazy createResult(PixelTransformer pixelTransformer) {
            return new FastAreaLazy(sharedAreaMap, pixelTransformer);
        }

        @Override
        public void initRandom(long x, long z) {
            long l = this.magicrandom;
            l = LinearCongruentialGenerator.next(l, x);
            l = LinearCongruentialGenerator.next(l, z);
            l = LinearCongruentialGenerator.next(l, x);
            l = LinearCongruentialGenerator.next(l, z);
            this.map.put(Thread.currentThread().getId(), l);
        }

        @Override
        public int nextRandom(int y) {
            long tid = Thread.currentThread().getId();
            long e = this.map.computeIfAbsent(tid, i -> 0L);
            int mod = (int) Math.floorMod(e >> 24L, (long) y);
            this.map.put(tid, LinearCongruentialGenerator.next(e, this.magicrandom));
            return mod;
        }

        @Override
        public ImprovedNoise getBiomeNoise() {
            return this.improvedNoise;
        }

    }

    private static class FastGenLayer extends Layer {

        private final FastAreaLazy fastAreaLazy;

        public FastGenLayer(AreaFactory<FastAreaLazy> factory) {
            super(() -> null);
            this.fastAreaLazy = factory.make();
        }

        @Override
        public Biome get(Registry<Biome> registry, int x, int z) {
            ResourceKey<Biome> key = Biomes.byId(this.fastAreaLazy.get(x, z));
            if (key == null) {
                return registry.get(Biomes.byId(0));
            }
            Biome biome = registry.get(key);
            if (biome == null) {
                return registry.get(Biomes.byId(0));
            }
            return biome;
        }

    }

    private record FastAreaLazy(ConcurrentHashMap<Long, Integer> sharedMap, PixelTransformer transformer) implements Area {

        @Override
        public int get(int x, int z) {
            long zx = ChunkPos.asLong(x, z);
            return this.sharedMap.computeIfAbsent(zx, i -> this.transformer.apply(x, z));
        }

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

        // avoid warning on paper
        public FastProtoChunk(ChunkPos pos, UpgradeData upgradeData, LevelHeightAccessor world, ServerLevel serverLevel) {
            super(pos, upgradeData, world, serverLevel);
        }

        // compatibility with spigot
        public FastProtoChunk(ChunkPos pos, UpgradeData upgradeData, LevelHeightAccessor levelHeightAccessor) {
            super(pos, upgradeData, levelHeightAccessor);
        }

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
            return chunkStatus.getName();
        }

        @Override
        public CompletableFuture<?> processChunk(List<ChunkAccess> accessibleChunks) {
            return chunkStatus.generate(
                    Runnable::run, // TODO revisit, we might profit from this somehow?
                    freshWorld,
                    chunkGenerator,
                    structureManager,
                    threadedLevelLightEngine,
                    c -> CompletableFuture.completedFuture(Either.left(c)),
                    accessibleChunks
            );
        }

    }

}
