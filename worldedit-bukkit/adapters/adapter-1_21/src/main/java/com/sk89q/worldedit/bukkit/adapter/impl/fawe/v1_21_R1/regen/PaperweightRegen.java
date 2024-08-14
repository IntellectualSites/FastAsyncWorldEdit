package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_21_R1.regen;

import com.fastasyncworldedit.bukkit.adapter.Regenerator;
import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.queue.IChunkCache;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.implementation.chunk.ChunkCache;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Lifecycle;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.Refraction;
import com.sk89q.worldedit.bukkit.adapter.ext.fawe.v1_21_R1.PaperweightAdapter;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.io.file.SafeFiles;
import com.sk89q.worldedit.world.RegenOptions;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.generator.CustomChunkGenerator;
import org.bukkit.generator.BiomeProvider;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;
import java.util.OptionalLong;
import java.util.function.Supplier;

import static net.minecraft.core.registries.Registries.BIOME;

public class PaperweightRegen extends Regenerator<ChunkAccess, ProtoChunk, LevelChunk, ChunkStatus> {

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


    static {
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

            generatorStructureStateField = ChunkMap.class.getDeclaredField(Refraction.pickName("chunkGeneratorState", "w"));
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

    private final PaperweightAdapter adapter;

    //runtime
    private ServerLevel originalServerWorld;
    private ServerChunkCache originalChunkProvider;
    private ServerLevel freshWorld;
    private LevelStorageSource.LevelStorageAccess session;
    private ChunkGenerator chunkGenerator;

    private Path tempDir;

    private boolean generateFlatBedrock = false;

    public PaperweightRegen(
            World originalBukkitWorld,
            Region region,
            Extent target,
            RegenOptions options,
            PaperweightAdapter adapter
    ) {
        super(originalBukkitWorld, region, target, options);
        this.adapter = adapter;
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
                new LevelStem(
                        originalServerWorld.dimensionTypeRegistration(),
                        originalServerWorld.getChunkSource().getGenerator()
                ),
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
            public @NotNull Holder<Biome> getUncachedNoiseBiome(int biomeX, int biomeY, int biomeZ) {
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
            Holder<NoiseGeneratorSettings> generatorSettingBaseSupplier = (Holder<NoiseGeneratorSettings>)
                    generatorSettingBaseSupplierField.get(noiseBasedChunkGenerator);
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
            //noinspection deprecation
            generateConcurrent = generator.isParallelCapable();
        }
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
                    freshWorld.getChunkSource().close(false);
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
        throw new UnsupportedOperationException("should not be called");
    }

    @Override
    protected LevelChunk createChunk(ProtoChunk protoChunk) {
        throw new UnsupportedOperationException("should not be called");
    }

    @Override
    protected IChunkCache<IChunkGet> initSourceQueueCache() {
        return new ChunkCache<>(BukkitAdapter.adapt(freshWorld.getWorld()));
    }

    //util
    @SuppressWarnings("unchecked")
    private void removeWorldFromWorldsMap() {
        try {
            Map<String, org.bukkit.World> map = (Map<String, org.bukkit.World>) serverWorldsField.get(Bukkit.getServer());
            map.remove("faweregentempworld");
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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
        public void updateSpawnPos(@NotNull ChunkPos spawnPos) {
        }

        @Override
        public void onStatusChange(
                final @NotNull ChunkPos pos,
                @org.jetbrains.annotations.Nullable final net.minecraft.world.level.chunk.status.ChunkStatus status
        ) {

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

}
