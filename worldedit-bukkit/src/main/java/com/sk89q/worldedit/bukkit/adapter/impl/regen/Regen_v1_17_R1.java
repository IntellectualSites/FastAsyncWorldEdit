package com.sk89q.worldedit.bukkit.adapter.impl.regen;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.IChunkCache;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.bukkit.adapter.mc1_17.BukkitGetBlocks_1_17;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import com.sk89q.worldedit.bukkit.adapter.Regenerator;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.io.file.SafeFiles;
import com.sk89q.worldedit.world.RegenOptions;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.RegistryMaterials;
import net.minecraft.data.RegistryGeneration;
import net.minecraft.data.worldgen.biome.BiomeRegistry;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkProviderServer;
import net.minecraft.server.level.LightEngineThreaded;
import net.minecraft.server.level.PlayerChunk;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.level.progress.WorldLoadListener;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldSettings;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.biome.WorldChunkManagerOverworld;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkConverter;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.dimension.WorldDimension;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.ChunkGeneratorAbstract;
import net.minecraft.world.level.levelgen.ChunkProviderFlat;
import net.minecraft.world.level.levelgen.GeneratorSettingBase;
import net.minecraft.world.level.levelgen.GeneratorSettings;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.SimpleRandomSource;
import net.minecraft.world.level.levelgen.flat.GeneratorSettingsFlat;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.levelgen.synth.NoiseGeneratorPerlin;
import net.minecraft.world.level.newbiome.area.Area;
import net.minecraft.world.level.newbiome.area.AreaFactory;
import net.minecraft.world.level.newbiome.context.AreaContextTransformed;
import net.minecraft.world.level.newbiome.layer.GenLayer;
import net.minecraft.world.level.newbiome.layer.GenLayers;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer8;
import net.minecraft.world.level.storage.Convertable;
import net.minecraft.world.level.storage.WorldDataServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.generator.CustomChunkGenerator;
import org.bukkit.generator.BlockPopulator;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Regen_v1_17_R1 extends Regenerator<IChunkAccess, ProtoChunk, Chunk, Regen_v1_17_R1.ChunkStatusWrap> {

    private static final Field serverWorldsField;
    private static final Field worldPaperConfigField;
    private static final Field flatBedrockField;
    private static final Field generatorSettingBaseSupplierField;
    private static final Field generatorSettingFlatField;
    private static final Field delegateField;
    private static final Field chunkProviderField;

    //list of chunk stati in correct order without FULL
    private static final Map<ChunkStatus, Concurrency> chunkStati = new LinkedHashMap<>();

    static {
        chunkStati.put(ChunkStatus.a, Concurrency.FULL);        // radius -1, does nothing
        chunkStati.put(ChunkStatus.b, Concurrency.NONE);        // uses unsynchronized maps
        chunkStati.put(ChunkStatus.c, Concurrency.FULL);        // radius 8, but no writes to other chunks, only current chunk
        chunkStati.put(ChunkStatus.d, Concurrency.FULL);        // radius 0
        chunkStati.put(ChunkStatus.e, Concurrency.RADIUS);      // radius 8
        chunkStati.put(ChunkStatus.f, Concurrency.FULL);        // radius 0
        chunkStati.put(ChunkStatus.g, Concurrency.NONE);        // radius 0, but RADIUS and FULL change results
        chunkStati.put(ChunkStatus.h, Concurrency.NONE);        // radius 0, but RADIUS and FULL change results
        chunkStati.put(ChunkStatus.i, Concurrency.NONE);        // uses unsynchronized maps
        chunkStati.put(ChunkStatus.j, Concurrency.FULL);        // radius 1, but no writes to other chunks, only current chunk
        chunkStati.put(ChunkStatus.k, Concurrency.FULL);        // radius 0
        chunkStati.put(ChunkStatus.l, Concurrency.FULL);        // radius 0

        try {
            serverWorldsField = CraftServer.class.getDeclaredField("worlds");
            serverWorldsField.setAccessible(true);

            Field tmpPaperConfigField = null;
            Field tmpFlatBedrockField = null;
            try { //only present on paper
                tmpPaperConfigField = World.class.getDeclaredField("paperConfig");
                tmpPaperConfigField.setAccessible(true);

                tmpFlatBedrockField = tmpPaperConfigField.getType().getDeclaredField("generateFlatBedrock");
                tmpFlatBedrockField.setAccessible(true);
            } catch (Exception e) {
                tmpPaperConfigField = null;
                tmpFlatBedrockField = null;
            }
            worldPaperConfigField = tmpPaperConfigField;
            flatBedrockField = tmpFlatBedrockField;

            generatorSettingBaseSupplierField = ChunkGeneratorAbstract.class.getDeclaredField("h");
            generatorSettingBaseSupplierField.setAccessible(true);

            generatorSettingFlatField = ChunkProviderFlat.class.getDeclaredField("e");
            generatorSettingFlatField.setAccessible(true);

            delegateField = CustomChunkGenerator.class.getDeclaredField("delegate");
            delegateField.setAccessible(true);

            chunkProviderField = WorldServer.class.getDeclaredField("chunkProvider");
            chunkProviderField.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //runtime
    private WorldServer originalNMSWorld;
    private ChunkProviderServer originalChunkProvider;
    private WorldServer freshNMSWorld;
    private ChunkProviderServer freshChunkProvider;
    private Convertable.ConversionSession session;
    private DefinedStructureManager structureManager;
    private LightEngineThreaded lightEngine;
    private ChunkGenerator generator;

    private Path tempDir;

    private boolean generateFlatBedrock = false;

    public Regen_v1_17_R1(org.bukkit.World originalBukkitWorld, Region region, Extent target, RegenOptions options) {
        super(originalBukkitWorld, region, target, options);
    }

    @Override
    protected boolean prepare() {
        this.originalNMSWorld = ((CraftWorld) originalBukkitWorld).getHandle();
        originalChunkProvider = originalNMSWorld.getChunkProvider();
        if (!(originalChunkProvider instanceof ChunkProviderServer)) {
            return false;
        }

        //flat bedrock? (only on paper)
        try {
            generateFlatBedrock = flatBedrockField.getBoolean(worldPaperConfigField.get(originalNMSWorld));
        } catch (Exception ignored) {
        }

        seed = options.getSeed().orElse(originalNMSWorld.getSeed());
        chunkStati.forEach((s, c) -> super.chunkStati.put(new ChunkStatusWrap(s), c));

        return true;
    }

    @Override
    protected boolean initNewWorld() throws Exception {
        //world folder
        tempDir = java.nio.file.Files.createTempDirectory("WorldEditWorldGen");

        //prepare for world init (see upstream implementation for reference)
        org.bukkit.World.Environment env = originalBukkitWorld.getEnvironment();
        org.bukkit.generator.ChunkGenerator gen = originalBukkitWorld.getGenerator();
        Convertable convertable = Convertable.a(tempDir);
        ResourceKey<WorldDimension> worldDimKey = getWorldDimKey(env);
        session = convertable.c("worldeditregentempworld", worldDimKey);
        WorldDataServer originalWorldData = originalNMSWorld.E;

        MinecraftServer server = originalNMSWorld.getServer().getServer();
        WorldDataServer levelProperties = (WorldDataServer) server.getSaveData();
        RegistryReadOps<NBTBase> nbtRegOps = RegistryReadOps.a(DynamicOpsNBT.a, server.aC.i(), IRegistryCustom.a());
        GeneratorSettings newOpts = GeneratorSettings.a.encodeStart(nbtRegOps, levelProperties.getGeneratorSettings()).flatMap(tag -> GeneratorSettings.a.parse(this.recursivelySetSeed(new Dynamic<>(nbtRegOps, tag), seed, new HashSet<>()))).result().orElseThrow(() -> new IllegalStateException("Unable to map GeneratorOptions"));
        WorldSettings newWorldSettings = new WorldSettings("worldeditregentempworld", originalWorldData.e.getGameType(), originalWorldData.e.isHardcore(), originalWorldData.e.getDifficulty(), originalWorldData.e.e(), originalWorldData.e.getGameRules(), originalWorldData.e.g());
        WorldDataServer newWorldData = new WorldDataServer(newWorldSettings, newOpts, Lifecycle.stable());

        //init world
        freshNMSWorld = Fawe.get().getQueueHandler().sync((Supplier<WorldServer>) () -> new WorldServer(server, server.aA, session, newWorldData, originalNMSWorld.getDimensionKey(), originalNMSWorld.getDimensionManager(), new RegenNoOpWorldLoadListener(), ((WorldDimension) newOpts.d().a(worldDimKey)).c(), originalNMSWorld.isDebugWorld(), seed, ImmutableList.of(), false, env, gen) {
            @Override
            public void doTick(BooleanSupplier booleansupplier) { //no ticking
            }

            private final BiomeBase singleBiome = options.hasBiomeType() ? RegistryGeneration.i.get(MinecraftKey.a(options.getBiomeType().getId())) : null;

            @Override
            public BiomeBase a(int i, int j, int k) {
                if (options.hasBiomeType()) {
                    return singleBiome;
                }
                return this.getChunkProvider().getChunkGenerator().getWorldChunkManager().getBiome(i, j, k);
            }
        }).get();
        freshNMSWorld.b = true;
        removeWorldFromWorldsMap();
        newWorldData.checkName(originalNMSWorld.E.getName()); //rename to original world name



        freshChunkProvider = new ChunkProviderServer(freshNMSWorld, session, server.getDataFixer(), server.getDefinedStructureManager(), server.aA, originalChunkProvider.getChunkGenerator(), freshNMSWorld.spigotConfig.viewDistance, server.isSyncChunkWrites(), new RegenNoOpWorldLoadListener(), (chunkCoordIntPair, state) -> { }, () -> server.F().getWorldPersistentData()) {
            // redirect to our protoChunks list
            @Override
            public IChunkAccess getChunkAt(int x, int z, ChunkStatus chunkstatus, boolean flag) {
                return getProtoChunkAt(x, z);
            }
        };
        chunkProviderField.set(freshNMSWorld, freshChunkProvider);

        //generator
        if (originalChunkProvider.getChunkGenerator() instanceof ChunkProviderFlat) {
            GeneratorSettingsFlat generatorSettingFlat = (GeneratorSettingsFlat) generatorSettingFlatField.get(originalChunkProvider.getChunkGenerator());
            generator = new ChunkProviderFlat(generatorSettingFlat);
        } else if (originalChunkProvider.getChunkGenerator() instanceof ChunkGeneratorAbstract) {
            Supplier<GeneratorSettingBase> generatorSettingBaseSupplier = (Supplier<GeneratorSettingBase>) generatorSettingBaseSupplierField.get(originalChunkProvider.getChunkGenerator());
            WorldChunkManager chunkManager = originalChunkProvider.getChunkGenerator().getWorldChunkManager();
            if (chunkManager instanceof WorldChunkManagerOverworld) {
                chunkManager = fastOverWorldChunkManager(chunkManager);
            }
            generator = new ChunkGeneratorAbstract(chunkManager, seed, generatorSettingBaseSupplier);
        } else if (originalChunkProvider.getChunkGenerator() instanceof CustomChunkGenerator) {
            ChunkGenerator delegate = (ChunkGenerator) delegateField.get(originalChunkProvider.getChunkGenerator());
            generator = delegate;
        } else {
            System.out.println("Unsupported generator type " + originalChunkProvider.getChunkGenerator().getClass().getName());
            return false;
        }
        if (originalNMSWorld.generator != null) {
            // wrap custom world generator
            generator = new CustomChunkGenerator(freshNMSWorld, generator, originalNMSWorld.generator);
            generateConcurrent = originalNMSWorld.generator.isParallelCapable();
        }

        //lets start then
        structureManager = server.getDefinedStructureManager();
        lightEngine = freshChunkProvider.getLightEngine();

        return true;
    }

    @Override
    protected void cleanup() {
        try {
            session.close();
        } catch (Exception e) {
        }

        //shutdown chunk provider
        try {
            Fawe.get().getQueueHandler().sync(() -> {
                try {
                    freshChunkProvider.close(false);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
        }

        //remove world from server
        try {
            Fawe.get().getQueueHandler().sync(() -> {
                removeWorldFromWorldsMap();
            });
        } catch (Exception e) {
        }

        //delete directory
        try {
            SafeFiles.tryHardToDeleteDir(tempDir);
        } catch (Exception e) {
        }
    }

    @Override
    protected ProtoChunk createProtoChunk(int x, int z) {
        return new ProtoChunk(new ChunkCoordIntPair(x, z), ChunkConverter.a, freshNMSWorld) {
            public boolean generateFlatBedrock() {
                return generateFlatBedrock;
            }

            // no one will ever see the entities!
            @Override
            public List<NBTTagCompound> z() {
                return Collections.emptyList();
            }
        };
    }

    @Override
    protected Chunk createChunk(ProtoChunk protoChunk) {
        return new Chunk(
                freshNMSWorld,
                protoChunk,
                null // we don't want to add entities
        );
    }

    @Override
    protected ChunkStatusWrap getFullChunkStatus() {
        return new ChunkStatusWrap(ChunkStatus.m);
    }

    @Override
    protected List<BlockPopulator> getBlockPopulators() {
        return originalNMSWorld.getWorld().getPopulators();
    }

    @Override
    protected void populate(Chunk chunk, Random random, BlockPopulator pop) {
        pop.populate(freshNMSWorld.getWorld(), random, chunk.bukkitChunk);
    }

    @Override
    protected IChunkCache<IChunkGet> initSourceQueueCache() {
        return (chunkX, chunkZ) -> new BukkitGetBlocks_1_17(freshNMSWorld, chunkX, chunkZ) {
            @Override
            public Chunk ensureLoaded(World nmsWorld, int x, int z) {
                return getChunkAt(x, z);
            }
        };
    }

    protected class ChunkStatusWrap extends ChunkStatusWrapper<IChunkAccess> {

        private final ChunkStatus chunkStatus;

        public ChunkStatusWrap(ChunkStatus chunkStatus) {
            this.chunkStatus = chunkStatus;
        }

        @Override
        public int requiredNeigborChunkRadius() {
            return chunkStatus.f();
        }

        @Override
        public String name() {
            return chunkStatus.d();
        }

        @Override
        public void processChunk(Long xz, List<IChunkAccess> accessibleChunks) {
            chunkStatus.a(
                    Runnable::run, // TODO revisit, we might profit from this somehow?
                    freshNMSWorld,
                    generator,
                    structureManager,
                    lightEngine,
                    c -> CompletableFuture.completedFuture(Either.left(c)),
                    accessibleChunks);
        }
    }

    //util
    private void removeWorldFromWorldsMap() {
        Fawe.get().getQueueHandler().sync(() -> {
            try {
                Map<String, org.bukkit.World> map = (Map<String, org.bukkit.World>) serverWorldsField.get(Bukkit.getServer());
                map.remove("worldeditregentempworld");
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    private ResourceKey<WorldDimension> getWorldDimKey(org.bukkit.World.Environment env) {
        switch (env) {
            case NETHER:
                return WorldDimension.c;
            case THE_END:
                return WorldDimension.d;
            case NORMAL:
            default:
                return WorldDimension.b;
        }
    }

    private Dynamic<NBTBase> recursivelySetSeed(Dynamic<NBTBase> dynamic, long seed, Set<Dynamic<NBTBase>> seen) {
        return !seen.add(dynamic) ? dynamic : dynamic.updateMapValues((pair) -> {
            if (((Dynamic) pair.getFirst()).asString("").equals("seed")) {
                return pair.mapSecond((v) -> {
                    return v.createLong(seed);
                });
            } else {
                return ((Dynamic) pair.getSecond()).getValue() instanceof NBTTagCompound ? pair.mapSecond((v) -> {
                    return this.recursivelySetSeed((Dynamic) v, seed, seen);
                }) : pair;

            }
        });
    }

    private WorldChunkManager fastOverWorldChunkManager(WorldChunkManager chunkManager) throws Exception {
        Field legacyBiomeInitLayerField = WorldChunkManagerOverworld.class.getDeclaredField("i");
        legacyBiomeInitLayerField.setAccessible(true);
        Field largeBiomesField = WorldChunkManagerOverworld.class.getDeclaredField("j");
        largeBiomesField.setAccessible(true);
        Field biomeRegistryField = WorldChunkManagerOverworld.class.getDeclaredField("k");
        biomeRegistryField.setAccessible(true);
        Field areaLazyField = GenLayer.class.getDeclaredField("b");
        areaLazyField.setAccessible(true);
        Method initAreaFactoryMethod = GenLayers.class.getDeclaredMethod("a", boolean.class, int.class, int.class, LongFunction.class);
        initAreaFactoryMethod.setAccessible(true);

        //init new WorldChunkManagerOverworld
        boolean legacyBiomeInitLayer = legacyBiomeInitLayerField.getBoolean(chunkManager);
        boolean largebiomes = largeBiomesField.getBoolean(chunkManager);
        IRegistry<BiomeBase> biomeRegistrynms = (IRegistry<BiomeBase>) biomeRegistryField.get(chunkManager);
        IRegistry<BiomeBase> biomeRegistry;
        if (options.hasBiomeType()) {
            BiomeBase biome = RegistryGeneration.i.get(MinecraftKey.a(options.getBiomeType().getId()));
            biomeRegistry = new RegistryMaterials<>(ResourceKey.a(new MinecraftKey("fawe_biomes")), Lifecycle.experimental());
            ((RegistryMaterials) biomeRegistry).a(0, RegistryGeneration.i.c(biome).get(), biome, Lifecycle.experimental());
        } else {
            biomeRegistry = biomeRegistrynms;
        }
        chunkManager = new FastWorldChunkManagerOverworld(seed, legacyBiomeInitLayer, largebiomes, biomeRegistry);

        //replace genLayer
        AreaFactory<FastAreaLazy> factory = (AreaFactory<FastAreaLazy>) initAreaFactoryMethod.invoke(null, legacyBiomeInitLayer, largebiomes ? 6 : 4, 4, (LongFunction) (l -> new FastWorldGenContextArea(seed, l)));
        ((FastWorldChunkManagerOverworld) chunkManager).genLayer = new FastGenLayer(factory);

        return chunkManager;
    }

    private static class FastWorldChunkManagerOverworld extends WorldChunkManager {

        private GenLayer genLayer;
        private final IRegistry<BiomeBase> k;
        private final boolean isSingleRegistry;

        public FastWorldChunkManagerOverworld(long seed, boolean legacyBiomeInitLayer, boolean largeBiomes, IRegistry<BiomeBase> biomeRegistry) {
            super(biomeRegistry.g().collect(Collectors.toList()));
            this.k = biomeRegistry;
            this.isSingleRegistry = biomeRegistry.d().size() == 1;
            this.genLayer = GenLayers.a(seed, legacyBiomeInitLayer, largeBiomes ? 6 : 4, 4);
        }

        @Override
        protected Codec<? extends WorldChunkManager> a() {
            return WorldChunkManagerOverworld.e;
        }

        @Override
        public WorldChunkManager a(long l) {
            // TODO?
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        public BiomeBase getBiome(int i, int i1, int i2) {
            if (this.isSingleRegistry) {
                return this.k.fromId(0);
            }
            return this.genLayer.a(this.k, i, i2);
        }
    }


    private static class FastWorldGenContextArea implements AreaContextTransformed<FastAreaLazy> {

        private final ConcurrentHashMap<Long, Integer> sharedAreaMap = new ConcurrentHashMap<>();
        private final NoiseGeneratorPerlin perlinNoise;
        private final long magicrandom;
        private final ConcurrentHashMap<Long, Long> map = new ConcurrentHashMap<>(); //needed for multithreaded generation

        public FastWorldGenContextArea(long seed, long lconst) {
            this.magicrandom = mix(seed, lconst);
            this.perlinNoise = new NoiseGeneratorPerlin(new SimpleRandomSource(seed));
        }

        @Override
        public FastAreaLazy a(AreaTransformer8 var0) {
            return new FastAreaLazy(sharedAreaMap, var0);
        }

        @Override
        public void a(long x, long z) {
            long l = this.magicrandom;
            l = LinearCongruentialGenerator.a(l, x);
            l = LinearCongruentialGenerator.a(l, z);
            l = LinearCongruentialGenerator.a(l, x);
            l = LinearCongruentialGenerator.a(l, z);
            this.map.put(Thread.currentThread().getId(), l);
        }

        @Override
        public int a(int y) {
            long tid = Thread.currentThread().getId();
            long e = this.map.computeIfAbsent(tid, i -> 0L);
            int mod = (int) Math.floorMod(e >> 24L, (long) y);
            this.map.put(tid, LinearCongruentialGenerator.a(e, this.magicrandom));
            return mod;
        }

        @Override
        public NoiseGeneratorPerlin a() {
            return this.perlinNoise;
        }

        private static long mix(long seed, long lconst) {
            long l1 = lconst;
            l1 = LinearCongruentialGenerator.a(l1, lconst);
            l1 = LinearCongruentialGenerator.a(l1, lconst);
            l1 = LinearCongruentialGenerator.a(l1, lconst);
            long l2 = seed;
            l2 = LinearCongruentialGenerator.a(l2, l1);
            l2 = LinearCongruentialGenerator.a(l2, l1);
            l2 = LinearCongruentialGenerator.a(l2, l1);
            return l2;
        }
    }

    private static class FastGenLayer extends GenLayer {

        private final FastAreaLazy areaLazy;

        public FastGenLayer(AreaFactory<FastAreaLazy> factory) throws Exception {
            super(() -> null);
            this.areaLazy = factory.make();
        }

        @Override
        public BiomeBase a(IRegistry<BiomeBase> registry, int x, int z) {
            ResourceKey<BiomeBase> key = BiomeRegistry.a(this.areaLazy.a(x, z));
            if (key == null)
                return registry.a(BiomeRegistry.a(0));
            BiomeBase biome = registry.a(key);
            if (biome == null)
                return registry.a(BiomeRegistry.a(0));
            return biome;
        }
    }

    private static class FastAreaLazy implements Area {

        private final AreaTransformer8 transformer;
        //ConcurrentHashMap is 50% faster that Long2IntLinkedOpenHashMap in a syncronized context
        //using a map for each thread worsens the performance significantly due to cache misses (factor 5)
        private final ConcurrentHashMap<Long, Integer> sharedMap;

        public FastAreaLazy(ConcurrentHashMap<Long, Integer> sharedMap, AreaTransformer8 transformer) {
            this.sharedMap = sharedMap;
            this.transformer = transformer;
        }

        @Override
        public int a(int x, int z) {
            long zx = ChunkCoordIntPair.pair(x, z);
            return this.sharedMap.computeIfAbsent(zx, i -> this.transformer.apply(x, z));
        }
    }

    private static class RegenNoOpWorldLoadListener implements WorldLoadListener {

        private RegenNoOpWorldLoadListener() {
        }

        @Override
        public void a(ChunkCoordIntPair chunkCoordIntPair) {
        }

        @Override
        public void a(ChunkCoordIntPair chunkCoordIntPair, @Nullable ChunkStatus chunkStatus) {
        }

        @Override
        public void a() {

        }

        @Override
        public void b() {
        }

        // TODO Paper only? @Override
        public void setChunkRadius(int i) {
        }
    }
}
