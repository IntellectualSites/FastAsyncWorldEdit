package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R3.regen;

import com.fastasyncworldedit.bukkit.adapter.Regenerator;
import com.fastasyncworldedit.bukkit.util.FoliaLibHolder;
import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.queue.IChunkCache;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.implementation.chunk.ChunkCache;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Lifecycle;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.Refraction;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.io.file.SafeFiles;
import com.sk89q.worldedit.world.RegenOptions;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.ProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.generator.BiomeProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static net.minecraft.core.registries.Registries.BIOME;

public class PaperweightRegen extends Regenerator {

    private static final Field serverWorldsField;
    private static final Field paperConfigField;
    private static final Field generatorSettingBaseSupplierField;

    static {
        try {
            serverWorldsField = CraftServer.class.getDeclaredField("worlds");
            serverWorldsField.setAccessible(true);

            Field tmpPaperConfigField;
            try { // only present on paper
                tmpPaperConfigField = Level.class.getDeclaredField("paperConfig");
                tmpPaperConfigField.setAccessible(true);
            } catch (Exception e) {
                tmpPaperConfigField = null;
            }
            paperConfigField = tmpPaperConfigField;

            generatorSettingBaseSupplierField = NoiseBasedChunkGenerator.class.getDeclaredField(Refraction.pickName(
                    "settings", "e"));
            generatorSettingBaseSupplierField.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // runtime
    private ServerLevel originalServerWorld;
    private ServerLevel freshWorld;
    private LevelStorageSource.LevelStorageAccess session;

    private Path tempDir;

    public PaperweightRegen(
            World originalBukkitWorld,
            Region region,
            Extent target,
            RegenOptions options) {
        super(originalBukkitWorld, region, target, options);
    }

    @Override
    protected void runTasks(final BooleanSupplier shouldKeepTicking) {
        while (shouldKeepTicking.getAsBoolean()) {
            if (!this.freshWorld.getChunkSource().pollTask()) {
                return;
            }
        }
    }

    @Override
    protected boolean prepare() {
        this.originalServerWorld = ((CraftWorld) originalBukkitWorld).getHandle();
        seed = options.getSeed().orElse(originalServerWorld.getSeed());
        return true;
    }

    @Override
    protected boolean initNewWorld() throws Exception {
        // world folder
        tempDir = java.nio.file.Files.createTempDirectory("FastAsyncWorldEditWorldGen");

        // prepare for world init (see upstream implementation for reference)
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
                originalWorldData.settings.getDataConfiguration());

        PrimaryLevelData.SpecialWorldProperty specialWorldProperty = originalWorldData.isFlatWorld()
                ? PrimaryLevelData.SpecialWorldProperty.FLAT
                : originalWorldData.isDebugWorld()
                        ? PrimaryLevelData.SpecialWorldProperty.DEBUG
                        : PrimaryLevelData.SpecialWorldProperty.NONE;
        PrimaryLevelData newWorldData = new PrimaryLevelData(newWorldSettings, newOpts, specialWorldProperty,
                Lifecycle.stable());

        BiomeProvider biomeProvider = getBiomeProvider();

        // init world
        freshWorld = Fawe.instance().getQueueHandler().sync((Supplier<ServerLevel>) () -> new ServerLevel(
                server,
                server.executor,
                session,
                newWorldData,
                originalServerWorld.dimension(),
                new LevelStem(
                        originalServerWorld.dimensionTypeRegistration(),
                        originalServerWorld.getChunkSource().getGenerator()),
                new RegenNoOpWorldLoadListener(),
                originalServerWorld.isDebug(),
                seed,
                ImmutableList.of(),
                false,
                originalServerWorld.getRandomSequences(),
                environment,
                generator,
                biomeProvider) {

            private final Holder<Biome> singleBiome = options.hasBiomeType()
                    ? DedicatedServer.getServer().registryAccess()
                            .registryOrThrow(BIOME).asHolderIdMap().byIdOrThrow(
                                    WorldEditPlugin.getInstance().getBukkitImplAdapter()
                                            .getInternalBiomeId(options.getBiomeType()))
                    : null;

            @Override
            public @NotNull Holder<Biome> getUncachedNoiseBiome(int biomeX, int biomeY, int biomeZ) {
                if (options.hasBiomeType()) {
                    return singleBiome;
                }
                return super.getUncachedNoiseBiome(biomeX, biomeY, biomeZ);
            }

            @Override
            public void save(
                    @org.jetbrains.annotations.Nullable final ProgressListener progressListener,
                    final boolean flush,
                    final boolean savingDisabled) {
                // noop, spigot
            }

            @Override
            public void save(
                    @Nullable final ProgressListener progressListener,
                    final boolean flush,
                    final boolean savingDisabled,
                    final boolean close) {
                // noop, paper
            }
        }).get();
        freshWorld.noSave = true;
        removeWorldFromWorldsMap();
        newWorldData.checkName(originalServerWorld.serverLevelData.getLevelName()); // rename to original world name
        if (paperConfigField != null) {
            paperConfigField.set(freshWorld, originalServerWorld.paperConfig());
        }

        if (FoliaLibHolder.isFolia()) {
            return initWorldForFolia(newWorldData);
        }

        return true;
    }

    private boolean initWorldForFolia(PrimaryLevelData worldData) throws ExecutionException, InterruptedException {
        MinecraftServer console = ((CraftServer) Bukkit.getServer()).getServer();

        ChunkPos spawnChunk = new ChunkPos(
                freshWorld.getChunkSource().randomState().sampler().findSpawnPosition());

        setRandomSpawnSelection(spawnChunk);

        CompletableFuture<Boolean> initFuture = new CompletableFuture<>();

        org.bukkit.Location spawnLocation = new org.bukkit.Location(
                freshWorld.getWorld(),
                spawnChunk.x << 4,
                64,
                spawnChunk.z << 4);
        FoliaLibHolder.getScheduler().runAtLocation(spawnLocation, task -> {
            try {
                console.initWorld(freshWorld, worldData, worldData, worldData.worldGenOptions());
                initFuture.complete(true);
            } catch (Exception e) {
                initFuture.completeExceptionally(e);
            }
        });

        return initFuture.get();
    }

    private void setRandomSpawnSelection(ChunkPos spawnChunk) {
        try {
            Field randomSpawnField = ServerLevel.class.getDeclaredField("randomSpawnSelection");
            randomSpawnField.setAccessible(true);
            randomSpawnField.set(freshWorld, spawnChunk);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set randomSpawnSelection for Folia world initialization", e);
        }
    }

    @Override
    protected void cleanup() {
        try {
            session.close();
        } catch (Exception ignored) {
        }

        // shutdown chunk provider
        try {
            Fawe.instance().getQueueHandler().sync(() -> {
                try {
                    freshWorld.getChunkSource().getDataStorage().cache.clear();
                    freshWorld.getChunkSource().close(false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception ignored) {
        }

        // remove world from server
        try {
            Fawe.instance().getQueueHandler().sync(this::removeWorldFromWorldsMap);
        } catch (Exception ignored) {
        }

        // delete directory
        try {
            SafeFiles.tryHardToDeleteDir(tempDir);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected World getFreshWorld() {
        return freshWorld != null ? freshWorld.getWorld() : null;
    }

    @Override
    protected IChunkCache<IChunkGet> initSourceQueueCache() {
        return new ChunkCache<>(BukkitAdapter.adapt(freshWorld.getWorld()));
    }

    // util
    @SuppressWarnings("unchecked")
    private void removeWorldFromWorldsMap() {
        try {
            Map<String, org.bukkit.World> map = (Map<String, org.bukkit.World>) serverWorldsField
                    .get(Bukkit.getServer());
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
                @org.jetbrains.annotations.Nullable final ChunkStatus status) {

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
