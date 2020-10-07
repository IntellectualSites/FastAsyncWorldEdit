package com.sk89q.worldedit.bukkit.adapter.impl.regen;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.IChunkCache;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.bukkit.adapter.mc1_15_2.BukkitGetBlocks_1_15_2;
import com.mojang.datafixers.util.Either;
import com.sk89q.worldedit.bukkit.adapter.Regenerator;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.io.file.SafeFiles;
import com.sk89q.worldedit.world.RegenOptions;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.server.v1_15_R1.Area;
import net.minecraft.server.v1_15_R1.AreaContextTransformed;
import net.minecraft.server.v1_15_R1.AreaFactory;
import net.minecraft.server.v1_15_R1.AreaTransformer8;
import net.minecraft.server.v1_15_R1.BiomeBase;
import net.minecraft.server.v1_15_R1.BiomeLayoutOverworldConfiguration;
import net.minecraft.server.v1_15_R1.Biomes;
import net.minecraft.server.v1_15_R1.Chunk;
import net.minecraft.server.v1_15_R1.ChunkConverter;
import net.minecraft.server.v1_15_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_15_R1.ChunkGenerator;
import net.minecraft.server.v1_15_R1.ChunkProviderFlat;
import net.minecraft.server.v1_15_R1.ChunkProviderGenerate;
import net.minecraft.server.v1_15_R1.ChunkProviderHell;
import net.minecraft.server.v1_15_R1.ChunkProviderServer;
import net.minecraft.server.v1_15_R1.ChunkProviderTheEnd;
import net.minecraft.server.v1_15_R1.ChunkStatus;
import net.minecraft.server.v1_15_R1.DefinedStructureManager;
import net.minecraft.server.v1_15_R1.GenLayer;
import net.minecraft.server.v1_15_R1.GenLayers;
import net.minecraft.server.v1_15_R1.GeneratorSettingsEnd;
import net.minecraft.server.v1_15_R1.GeneratorSettingsFlat;
import net.minecraft.server.v1_15_R1.GeneratorSettingsNether;
import net.minecraft.server.v1_15_R1.GeneratorSettingsOverworld;
import net.minecraft.server.v1_15_R1.IChunkAccess;
import net.minecraft.server.v1_15_R1.IRegistry;
import net.minecraft.server.v1_15_R1.LightEngineThreaded;
import net.minecraft.server.v1_15_R1.LinearCongruentialGenerator;
import net.minecraft.server.v1_15_R1.MinecraftServer;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.NoiseGeneratorPerlin;
import net.minecraft.server.v1_15_R1.ProtoChunk;
import net.minecraft.server.v1_15_R1.World;
import net.minecraft.server.v1_15_R1.WorldChunkManager;
import net.minecraft.server.v1_15_R1.WorldChunkManagerOverworld;
import net.minecraft.server.v1_15_R1.WorldData;
import net.minecraft.server.v1_15_R1.WorldLoadListener;
import net.minecraft.server.v1_15_R1.WorldNBTStorage;
import net.minecraft.server.v1_15_R1.WorldServer;
import net.minecraft.server.v1_15_R1.WorldType;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_15_R1.CraftServer;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.generator.CustomChunkGenerator;
import org.bukkit.craftbukkit.v1_15_R1.util.CraftMagicNumbers;
import org.bukkit.generator.BlockPopulator;

public class Regen_v1_15_R2 extends Regenerator<IChunkAccess, ProtoChunk, Chunk, Regen_v1_15_R2.ChunkStatusWrap> {

    private static final Field serverWorldsField;
    private static final Field worldPaperConfigField;
    private static final Field flatBedrockField;
    private static final Field delegateField;
    private static final Field chunkProviderField;

    //list of chunk stati in correct order without FULL
    private static final Map<ChunkStatus, Regenerator.Concurrency> chunkStati = new LinkedHashMap<>();

    static {
        chunkStati.put(ChunkStatus.EMPTY, Regenerator.Concurrency.FULL);                  // radius -1, does nothing
        chunkStati.put(ChunkStatus.STRUCTURE_STARTS, Regenerator.Concurrency.NONE);       // uses unsynchronized maps
        chunkStati.put(ChunkStatus.STRUCTURE_REFERENCES, Regenerator.Concurrency.FULL);   // radius 8, but no writes to other chunks, only current chunk
        chunkStati.put(ChunkStatus.BIOMES, Regenerator.Concurrency.FULL);                 // radius 0
        chunkStati.put(ChunkStatus.NOISE, Regenerator.Concurrency.RADIUS);                // radius 8
        chunkStati.put(ChunkStatus.SURFACE, Regenerator.Concurrency.FULL);                // radius 0
        chunkStati.put(ChunkStatus.CARVERS, Regenerator.Concurrency.NONE);                // radius 0, but RADIUS and FULL change results
        chunkStati.put(ChunkStatus.LIQUID_CARVERS, Regenerator.Concurrency.NONE);         // radius 0, but RADIUS and FULL change results
        chunkStati.put(ChunkStatus.FEATURES, Regenerator.Concurrency.NONE);               // uses unsynchronized maps
        chunkStati.put(ChunkStatus.LIGHT, Regenerator.Concurrency.FULL);                  // radius 1, but no writes to other chunks, only current chunk
        chunkStati.put(ChunkStatus.SPAWN, Regenerator.Concurrency.FULL);                  // radius 0
        chunkStati.put(ChunkStatus.HEIGHTMAPS, Regenerator.Concurrency.FULL);             // radius 0

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
    private DefinedStructureManager structureManager;
    private LightEngineThreaded lightEngine;
    private ChunkGenerator generator;

    private Path tempDir;

    private boolean generateFlatBedrock = false;

    public Regen_v1_15_R2(org.bukkit.World originalBukkitWorld, Region region, Extent target, RegenOptions options) {
        super(originalBukkitWorld, region, target, options);
    }

    @Override
    protected boolean prepare() {
        this.originalNMSWorld = ((CraftWorld) originalBukkitWorld).getHandle();
        originalChunkProvider = originalNMSWorld.getChunkProvider();
        if (!(originalChunkProvider instanceof ChunkProviderServer)) {
            return false;
        }

        //flat bedrock? (only on paper) //todo does not exist?
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

        MinecraftServer server = originalNMSWorld.getServer().getServer();
        WorldData newWorldData = new WorldData(originalNMSWorld.worldData.a((NBTTagCompound) null), server.dataConverterManager, CraftMagicNumbers.INSTANCE.getDataVersion(), (NBTTagCompound) null);
        newWorldData.setName("worldeditregentempworld");
        WorldNBTStorage saveHandler = new WorldNBTStorage(new File(tempDir.toUri()), originalNMSWorld.getDataManager().getDirectory().getName(), server, server.dataConverterManager);

        //init world
        freshNMSWorld = Fawe.get().getQueueHandler().sync((Supplier<WorldServer>) () -> new WorldServer(server, server.executorService, saveHandler, newWorldData, originalNMSWorld.worldProvider.getDimensionManager(), originalNMSWorld.getMethodProfiler(), new RegenNoOpWorldLoadListener(), env, gen)).get();
        freshNMSWorld.savingDisabled = true;

        DefinedStructureManager tmpStructureManager = saveHandler.f();
        freshChunkProvider = new ChunkProviderServer(freshNMSWorld, saveHandler.getDirectory(), server.aC(), tmpStructureManager, server.executorService, originalChunkProvider.chunkGenerator, freshNMSWorld.spigotConfig.viewDistance, new RegenNoOpWorldLoadListener(), () -> freshNMSWorld.getWorldPersistentData()) {
            // redirect to our protoChunks list
            @Override
            public IChunkAccess getChunkAt(int x, int z, ChunkStatus chunkstatus, boolean flag) {
                return getProtoChunkAt(x, z);
            }
        };
        chunkProviderField.set(freshNMSWorld, freshChunkProvider);

        //generator
        if (originalChunkProvider.getChunkGenerator() instanceof ChunkProviderFlat) {
            GeneratorSettingsFlat generatorSettingFlat = (GeneratorSettingsFlat) originalChunkProvider.getChunkGenerator().getSettings();
            generator = new ChunkProviderFlat(freshNMSWorld, originalChunkProvider.getChunkGenerator().getWorldChunkManager(), generatorSettingFlat);
        } else if (originalChunkProvider.getChunkGenerator() instanceof ChunkProviderGenerate) { //overworld
            GeneratorSettingsOverworld settings = (GeneratorSettingsOverworld) originalChunkProvider.getChunkGenerator().getSettings();
            WorldChunkManager chunkManager = originalChunkProvider.getChunkGenerator().getWorldChunkManager();
            if (chunkManager instanceof WorldChunkManagerOverworld) { //should always be true
                chunkManager = fastOverWorldChunkManager(chunkManager);
            }
            generator = new ChunkProviderGenerate(freshNMSWorld, chunkManager, settings);
        } else if (originalChunkProvider.getChunkGenerator() instanceof ChunkProviderHell) { //nether
            GeneratorSettingsNether settings = (GeneratorSettingsNether) originalChunkProvider.getChunkGenerator().getSettings();
            generator = new ChunkProviderHell(freshNMSWorld, originalChunkProvider.getChunkGenerator().getWorldChunkManager(), settings);
        } else if (originalChunkProvider.getChunkGenerator() instanceof ChunkProviderTheEnd) { //end
            GeneratorSettingsEnd settings = (GeneratorSettingsEnd) originalChunkProvider.getChunkGenerator().getSettings();
            generator = new ChunkProviderTheEnd(freshNMSWorld, originalChunkProvider.getChunkGenerator().getWorldChunkManager(), settings);
        } else if (originalChunkProvider.getChunkGenerator() instanceof CustomChunkGenerator) {
            ChunkGenerator delegate = (ChunkGenerator) delegateField.get(originalChunkProvider.getChunkGenerator());
            generator = delegate;
        } else {
            System.out.println("Unsupported generator type " + originalChunkProvider.getChunkGenerator().getClass().getName());
            return false;
        }
        if (originalNMSWorld.generator != null) {
            // wrap custom world generator
            generator = new CustomChunkGenerator(freshNMSWorld, originalNMSWorld.generator);
            generateConcurrent = originalNMSWorld.generator.isParallelCapable();
        }

        //lets start then
        structureManager = tmpStructureManager;
        lightEngine = freshChunkProvider.getLightEngine();

        return true;
    }

    @Override
    protected void cleanup() {
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
                try {
                    Map<String, org.bukkit.World> map = (Map<String, org.bukkit.World>) serverWorldsField.get(Bukkit.getServer());
                    map.remove("worldeditregentempworld");
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
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
        return new ProtoChunk(new ChunkCoordIntPair(x, z), ChunkConverter.a) {
            public boolean generateFlatBedrock() {
                return generateFlatBedrock;
            }
        };
    }

    @Override
    protected Chunk createChunk(ProtoChunk protoChunk) {
        return new Chunk(freshNMSWorld, protoChunk);
    }

    @Override
    protected ChunkStatusWrap getFullChunkStatus() {
        return new ChunkStatusWrap(ChunkStatus.FULL);
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
        return (chunkX, chunkZ) -> new BukkitGetBlocks_1_15_2(freshNMSWorld, chunkX, chunkZ) {
            @Override
            public Chunk ensureLoaded(World nmsWorld, int x, int z) {
                return getChunkAt(x, z);
            }
        };
    }

    protected class ChunkStatusWrap extends Regenerator.ChunkStatusWrapper<IChunkAccess> {

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
            chunkStatus.a(freshNMSWorld,
                          generator,
                          structureManager,
                          lightEngine,
                          c -> CompletableFuture.completedFuture(Either.left(c)),
                          accessibleChunks);
        }
    }

    //util
    private WorldChunkManager fastOverWorldChunkManager(WorldChunkManager chunkManager) throws Exception {
        Field genLayerField = WorldChunkManagerOverworld.class.getDeclaredField("d");
        genLayerField.setAccessible(true);
        Field areaLazyField = GenLayer.class.getDeclaredField("b");
        areaLazyField.setAccessible(true);
        Method initAreaFactoryMethod = GenLayers.class.getDeclaredMethod("a", WorldType.class, GeneratorSettingsOverworld.class, LongFunction.class);
        initAreaFactoryMethod.setAccessible(true);

        //init new WorldChunkManagerOverworld
        BiomeLayoutOverworldConfiguration biomeconfig = new BiomeLayoutOverworldConfiguration(freshNMSWorld.getWorldData())
                .a((GeneratorSettingsOverworld) originalChunkProvider.getChunkGenerator().getSettings());
        chunkManager = new WorldChunkManagerOverworld(biomeconfig);

        //replace genLayer
        AreaFactory<FastAreaLazy> factory = (AreaFactory<FastAreaLazy>) initAreaFactoryMethod.invoke(null, biomeconfig.b(), biomeconfig.c(), (LongFunction) (l -> new FastWorldGenContextArea(seed, l)));
        genLayerField.set(chunkManager, new FastGenLayer(factory));

        return chunkManager;
    }

    private static class FastWorldGenContextArea implements AreaContextTransformed<FastAreaLazy> {

        private final ConcurrentHashMap<Long, Integer> sharedAreaMap = new ConcurrentHashMap<>();
        private final NoiseGeneratorPerlin perlinNoise;
        private final long magicrandom;
        private final ConcurrentHashMap<Long, Long> map = new ConcurrentHashMap<>(); //needed for multithreaded generation

        public FastWorldGenContextArea(long seed, long lconst) {
            this.magicrandom = mix(seed, lconst);
            this.perlinNoise = new NoiseGeneratorPerlin(new Random(seed));
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
        public NoiseGeneratorPerlin b() {
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
        public BiomeBase a(int x, int z) {
            BiomeBase biome = IRegistry.BIOME.fromId(this.areaLazy.a(x, z));
            if (biome == null)
                return Biomes.b;
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
        public void b() {
        }
    }
}
