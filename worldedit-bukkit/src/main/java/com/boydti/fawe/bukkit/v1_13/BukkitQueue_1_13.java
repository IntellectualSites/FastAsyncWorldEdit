package com.boydti.fawe.bukkit.v1_13;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.BukkitPlayer;
import com.boydti.fawe.bukkit.adapter.v1_13_1.BlockMaterial_1_13;
import com.boydti.fawe.bukkit.adapter.v1_13_1.Spigot_v1_13_R2;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.IntFaweChunk;
import com.boydti.fawe.jnbt.anvil.BitArray4096;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.brush.visualization.VisualChunk;
import com.boydti.fawe.object.visitor.FaweChunkVisitor;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.ReflectionUtils;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockID;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import net.minecraft.server.v1_13_R2.BiomeBase;
import net.minecraft.server.v1_13_R2.Block;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.ChunkProviderServer;
import net.minecraft.server.v1_13_R2.ChunkSection;
import net.minecraft.server.v1_13_R2.DataBits;
import net.minecraft.server.v1_13_R2.DataPalette;
import net.minecraft.server.v1_13_R2.DataPaletteBlock;
import net.minecraft.server.v1_13_R2.DataPaletteHash;
import net.minecraft.server.v1_13_R2.DataPaletteLinear;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.EnumSkyBlock;
import net.minecraft.server.v1_13_R2.GameProfileSerializer;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.Packet;
import net.minecraft.server.v1_13_R2.PacketDataSerializer;
import net.minecraft.server.v1_13_R2.PacketPlayOutMultiBlockChange;
import net.minecraft.server.v1_13_R2.PlayerChunk;
import net.minecraft.server.v1_13_R2.PlayerChunkMap;
import net.minecraft.server.v1_13_R2.RegistryID;
import net.minecraft.server.v1_13_R2.TileEntity;
import net.minecraft.server.v1_13_R2.WorldChunkManager;
import net.minecraft.server.v1_13_R2.WorldData;
import net.minecraft.server.v1_13_R2.WorldServer;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.CraftChunk;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

public class BukkitQueue_1_13 extends BukkitQueue_0<net.minecraft.server.v1_13_R2.Chunk, ChunkSection[], ChunkSection> {

    public final static Field fieldBits;
    public final static Field fieldPalette;
    protected final static Field fieldSize;

    protected final static Field fieldHashBlocks;
    protected final static Field fieldLinearBlocks;
    protected final static Field fieldHashIndex;
    protected final static Field fieldRegistryb;
    protected final static Field fieldRegistryc;
    protected final static Field fieldRegistryd;
    protected final static Field fieldRegistrye;
    protected final static Field fieldRegistryf;

    protected final static Field fieldLinearIndex;
    protected final static Field fieldDefaultBlock;

    protected final static Field fieldFluidCount;
    protected final static Field fieldTickingBlockCount;
    protected final static Field fieldNonEmptyBlockCount;

    protected final static Field fieldSection;
    protected final static Field fieldLiquidCount;
    protected final static Field fieldEmittedLight;
    protected final static Field fieldSkyLight;


//    protected final static Field fieldBiomes;

    protected final static Field fieldChunkGenerator;
    protected final static Field fieldSeed;
//    protected final static Field fieldBiomeCache;
//    protected final static Field fieldBiomes2;
    protected final static Field fieldGenLayer1;
    protected final static Field fieldGenLayer2;
    protected final static Field fieldSave;
//    protected final static MutableGenLayer genLayer;
    protected final static ChunkSection emptySection;

//    protected static final Method methodResize;

    protected final static Field fieldDirtyCount;
    protected final static Field fieldDirtyBits;

    static {
        try {
            emptySection = new ChunkSection(0, true);
            Arrays.fill(emptySection.getSkyLightArray().asBytes(), (byte) 255);
            fieldSection = ChunkSection.class.getDeclaredField("blockIds");
            fieldLiquidCount = ChunkSection.class.getDeclaredField("e");
            fieldEmittedLight = ChunkSection.class.getDeclaredField("emittedLight");
            fieldSkyLight = ChunkSection.class.getDeclaredField("skyLight");
            fieldSection.setAccessible(true);
            fieldLiquidCount.setAccessible(true);
            fieldEmittedLight.setAccessible(true);
            fieldSkyLight.setAccessible(true);

            fieldFluidCount = ChunkSection.class.getDeclaredField("e");
            fieldTickingBlockCount = ChunkSection.class.getDeclaredField("tickingBlockCount");
            fieldNonEmptyBlockCount = ChunkSection.class.getDeclaredField("nonEmptyBlockCount");
            fieldFluidCount.setAccessible(true);
            fieldTickingBlockCount.setAccessible(true);
            fieldNonEmptyBlockCount.setAccessible(true);



//            fieldBiomes = ChunkProviderGenerate.class.getDeclaredField("D"); // *
//            fieldBiomes.setAccessible(true);

            fieldChunkGenerator = ChunkProviderServer.class.getDeclaredField("chunkGenerator");
            fieldChunkGenerator.setAccessible(true);
            fieldSeed = WorldData.class.getDeclaredField("e");
            fieldSeed.setAccessible(true);

//            fieldBiomeCache = WorldChunkManager.class.getDeclaredField("d"); // *
//            fieldBiomeCache.setAccessible(true);
//            fieldBiomes2 = WorldChunkManager.class.getDeclaredField("e"); // *
//            fieldBiomes2.setAccessible(true);
            fieldGenLayer1 = WorldChunkManager.class.getDeclaredField("b") ;
            fieldGenLayer2 = WorldChunkManager.class.getDeclaredField("c") ;
            fieldGenLayer1.setAccessible(true);
            fieldGenLayer2.setAccessible(true);

            fieldSave = ReflectionUtils.setAccessible(net.minecraft.server.v1_13_R2.Chunk.class.getDeclaredField("s")); //*

            fieldHashBlocks = DataPaletteHash.class.getDeclaredField("b");
            fieldHashBlocks.setAccessible(true);
            fieldLinearBlocks = DataPaletteLinear.class.getDeclaredField("b");
            fieldLinearBlocks.setAccessible(true);

            fieldHashIndex = DataPaletteHash.class.getDeclaredField("f");
            fieldHashIndex.setAccessible(true);

            fieldRegistryb = RegistryID.class.getDeclaredField("b");
            fieldRegistryc = RegistryID.class.getDeclaredField("c");
            fieldRegistryd = RegistryID.class.getDeclaredField("d");
            fieldRegistrye = RegistryID.class.getDeclaredField("e");
            fieldRegistryf = RegistryID.class.getDeclaredField("f");
            fieldRegistryb.setAccessible(true);
            fieldRegistryc.setAccessible(true);
            fieldRegistryd.setAccessible(true);
            fieldRegistrye.setAccessible(true);
            fieldRegistryf.setAccessible(true);

            fieldLinearIndex = DataPaletteLinear.class.getDeclaredField("f");
            fieldLinearIndex.setAccessible(true);

            fieldDefaultBlock = DataPaletteBlock.class.getDeclaredField("g");
            fieldDefaultBlock.setAccessible(true);

            fieldSize = DataPaletteBlock.class.getDeclaredField("i");
            fieldSize.setAccessible(true);

            fieldBits = DataPaletteBlock.class.getDeclaredField("a");
            fieldBits.setAccessible(true);

            fieldPalette = DataPaletteBlock.class.getDeclaredField("h");
            fieldPalette.setAccessible(true);

//            methodResize = DataPaletteBlock.class.getDeclaredMethod("b", int.class);
//            methodResize.setAccessible(true);

            fieldDirtyCount = PlayerChunk.class.getDeclaredField("dirtyCount");
            fieldDirtyBits = PlayerChunk.class.getDeclaredField("h");
            fieldDirtyCount.setAccessible(true);
            fieldDirtyBits.setAccessible(true);

            Fawe.debug("Using adapter: " + getAdapter());
            Fawe.debug("=========================================");
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public BukkitQueue_1_13(final com.sk89q.worldedit.world.World world) {
        super(world);
        getImpWorld();
    }

    public BukkitQueue_1_13(final String world) {
        super(world);
        getImpWorld();
    }

    private boolean save(final net.minecraft.server.v1_13_R2.Chunk chunk, final ChunkProviderServer cps) {
        cps.saveChunk(chunk, false);
        chunk.a(false);
        return true;
    }

    @Override
    public ChunkSection[] getSections(final net.minecraft.server.v1_13_R2.Chunk chunk) {
        return chunk.getSections();
    }

    @Override
    public net.minecraft.server.v1_13_R2.Chunk loadChunk(final World world, final int x, final int z, final boolean generate) {
        final ChunkProviderServer provider = ((CraftWorld) world).getHandle().getChunkProvider();
        if (generate) {
            return provider.getChunkAt(x, z, true, true);
        } else {
            return provider.getChunkAt(x, z, true, false);
        }
    }

    @Override
    public ChunkSection[] getCachedSections(final World world, final int cx, final int cz) {
        final net.minecraft.server.v1_13_R2.Chunk chunk = ((CraftWorld) world).getHandle().getChunkProvider().getChunkAt(cx, cz, false, false);
        if (chunk != null) {
            return chunk.getSections();
        }
        return null;
    }

    @Override
    public net.minecraft.server.v1_13_R2.Chunk getCachedChunk(final World world, final int cx, final int cz) {
        return ((CraftWorld) world).getHandle().getChunkProvider().getChunkAt(cx, cz, false, false);
    }

    @Override
    public ChunkSection getCachedSection(final ChunkSection[] chunkSections, final int cy) {
        return chunkSections[cy];
    }

    @Override
    public void saveChunk(final net.minecraft.server.v1_13_R2.Chunk chunk) {
        chunk.f(true); // Set Modified
        chunk.mustSave = true;
    }

    @Override
    public boolean regenerateChunk(final World world, final int x, final int z, final BiomeType biome, final Long seed) {
//        if (biome != null) {
//            try {
//                if (seed == null) {
//                    seed = world.getSeed();
//                }
//                nmsWorld.worldData.getSeed();
//                boolean result;
//                ChunkProviderGenerate generator = new ChunkProviderGenerate(nmsWorld, seed, false, "");
//                Biome bukkitBiome = getAdapter().getBiome(biome.getId());
//                BiomeBase base = BiomeBase.getBiome(biome.getId());
//                fieldBiomes.set(generator, new BiomeBase[]{base});
//                boolean cold = base.getTemperature() <= 1;
//                net.minecraft.server.v1_13_R2.ChunkGenerator existingGenerator = nmsWorld.getChunkProvider().chunkGenerator;
//                long existingSeed = world.getSeed();
//                {
//                    if (genLayer == null) genLayer = new MutableGenLayer(seed);
//                    genLayer.set(biome.getId());
//                    Object existingGenLayer1 = fieldGenLayer1.get(nmsWorld.getWorldChunkManager());
//                    Object existingGenLayer2 = fieldGenLayer2.get(nmsWorld.getWorldChunkManager());
//                    fieldGenLayer1.set(nmsWorld.getWorldChunkManager(), genLayer);
//                    fieldGenLayer2.set(nmsWorld.getWorldChunkManager(), genLayer);
//
//                    fieldSeed.set(nmsWorld.worldData, seed);
//
//                    ReflectionUtils.setFailsafeFieldValue(fieldBiomeCache, this.nmsWorld.getWorldChunkManager(), new BiomeCache(this.nmsWorld.getWorldChunkManager()));
//
//                    ReflectionUtils.setFailsafeFieldValue(fieldChunkGenerator, this.nmsWorld.getChunkProvider(), generator);
//
//                    keepLoaded.remove(MathMan.pairInt(x, z));
//                    result = getWorld().regenerateChunk(x, z);
//                    net.minecraft.server.v1_13_R2.Chunk nmsChunk = getCachedChunk(world, x, z);
//                    if (nmsChunk != null) {
//                        nmsChunk.f(true); // Set Modified
//                        nmsChunk.mustSave = true;
//                    }
//
//                    ReflectionUtils.setFailsafeFieldValue(fieldChunkGenerator, this.nmsWorld.getChunkProvider(), existingGenerator);
//
//                    fieldSeed.set(nmsWorld.worldData, existingSeed);
//
//                    fieldGenLayer1.set(nmsWorld.getWorldChunkManager(), existingGenLayer1);
//                    fieldGenLayer2.set(nmsWorld.getWorldChunkManager(), existingGenLayer2);
//                }
//                return result;
//            } catch (Throwable e) {
//                e.printStackTrace();
//            }
//        }
        return super.regenerateChunk(world, x, z, biome, seed);
    }

    @Override
    public boolean setMCA(final int mcaX, final int mcaZ, final RegionWrapper allowed, final Runnable whileLocked, final boolean saveChunks, final boolean load) {
        throw new UnsupportedOperationException("Anvil not implemented yet");
//        TaskManager.IMP.sync(new RunnableVal<Boolean>() {
//            @Override
//            public void run(Boolean value) {
//                long start = System.currentTimeMillis();
//                long last = start;
//                synchronized (RegionFileCache.class) {
//                    World world = getWorld();
//                    if (world.getKeepSpawnInMemory()) world.setKeepSpawnInMemory(false);
//                    ChunkProviderServer provider = nmsWorld.getChunkProvider();
//
//                    boolean mustSave = false;
//                    boolean[][] chunksUnloaded = null;
//                    { // Unload chunks
//                        Iterator<net.minecraft.server.v1_13_R2.Chunk> iter = provider.a().iterator();
//                        while (iter.hasNext()) {
//                            net.minecraft.server.v1_13_R2.Chunk chunk = iter.next();
//                            if (chunk.locX >> 5 == mcaX && chunk.locZ >> 5 == mcaZ) {
//                                boolean isIn = allowed.isInChunk(chunk.locX, chunk.locZ);
//                                if (isIn) {
//                                    if (!load) {
//                                        mustSave |= saveChunks && save(chunk, provider);
//                                        continue;
//                                    }
//                                    iter.remove();
//                                    boolean save = saveChunks && chunk.a(false);
//                                    mustSave |= save;
//                                    provider.unloadChunk(chunk, save);
//                                    if (chunksUnloaded == null) {
//                                        chunksUnloaded = new boolean[32][];
//                                    }
//                                    int relX = chunk.locX & 31;
//                                    boolean[] arr = chunksUnloaded[relX];
//                                    if (arr == null) {
//                                        arr = chunksUnloaded[relX] = new boolean[32];
//                                    }
//                                    arr[chunk.locZ & 31] = true;
//                                }
//                            }
//                        }
//                    }
//                    if (mustSave) {
//                        provider.c(); // TODO only the necessary chunks
//                    }
//
//                    File unloadedRegion = null;
//                    if (load && !RegionFileCache.a.isEmpty()) {
//                        Map<File, RegionFile> map = RegionFileCache.a;
//                        Iterator<Map.Entry<File, RegionFile>> iter = map.entrySet().iterator();
//                        String requiredPath = world.getName() + File.separator + "region";
//                        while (iter.hasNext()) {
//                            Map.Entry<File, RegionFile> entry = iter.next();
//                            File file = entry.getKey();
//                            int[] regPos = MainUtil.regionNameToCoords(file.getPath());
//                            if (regPos[0] == mcaX && regPos[1] == mcaZ && file.getPath().contains(requiredPath)) {
//                                if (file.exists()) {
//                                    unloadedRegion = file;
//                                    RegionFile regionFile = entry.getValue();
//                                    iter.remove();
//                                    try {
//                                        regionFile.c();
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                                break;
//                            }
//                        }
//                    }
//
//                    long now = System.currentTimeMillis();
//                    if (whileLocked != null) whileLocked.run();
//                    if (!load) return;
//
//                    { // Load the region again
//                        if (unloadedRegion != null && chunksUnloaded != null && unloadedRegion.exists()) {
//                            final boolean[][] finalChunksUnloaded = chunksUnloaded;
//                            TaskManager.IMP.async(() -> {
//                                int bx = mcaX << 5;
//                                int bz = mcaZ << 5;
//                                for (int x = 0; x < finalChunksUnloaded.length; x++) {
//                                    boolean[] arr = finalChunksUnloaded[x];
//                                    if (arr != null) {
//                                        for (int z = 0; z < arr.length; z++) {
//                                            if (arr[z]) {
//                                                int cx = bx + x;
//                                                int cz = bz + z;
//                                                SetQueue.IMP.addTask(new Runnable() {
//                                                    @Override
//                                                    public void run() {
//                                                        net.minecraft.server.v1_13_R2.Chunk chunk = provider.getChunkAt(cx, cz, null, false);
//                                                        if (chunk != null) {
//                                                            PlayerChunk pc = getPlayerChunk(nmsWorld, cx, cz);
//                                                            if (pc != null) {
//                                                                sendChunk(pc, chunk, 0);
//                                                            }
//                                                        }
//                                                    }
//                                                });
//                                            }
//                                        }
//                                    }
//                                }
//                            });
//                        }
//                    }
//                }
//            }
//        });
//        return true;
    }

    @Override
    public boolean next(final int amount, final long time) {
        return super.next(amount, time);
    }

    @Override
    public void setSkyLight(final ChunkSection section, final int x, final int y, final int z, final int value) {
        section.getSkyLightArray().a(x & 15, y & 15, z & 15, value);
    }

    @Override
    public void setBlockLight(final ChunkSection section, final int x, final int y, final int z, final int value) {
        section.getEmittedLightArray().a(x & 15, y & 15, z & 15, value);
    }

//    @Override
//    public World createWorld(final WorldCreator creator) {
//        final String name = creator.name();
//        ChunkGenerator generator = creator.generator();
//        final CraftServer server = (CraftServer) Bukkit.getServer();
//        final MinecraftServer console = server.getServer();
//        final File folder = new File(server.getWorldContainer(), name);
//        final World world = server.getWorld(name);
//        final WorldType type = WorldType.getType(creator.type().getName());
//        final boolean generateStructures = creator.generateStructures();
//        if (world != null) {
//            return world;
//        }
//        if (folder.exists() && !folder.isDirectory()) {
//            throw new IllegalArgumentException("File exists with the name '" + name + "' and isn't a folder");
//        }
//        TaskManager.IMP.sync(new RunnableVal<Object>() {
//            @Override
//            public void run(Object value) {
//                try {
//                    Field field = CraftServer.class.getDeclaredField("worlds");
//                    field.setAccessible(true);
//                    Map<Object, Object> existing = (Map<Object, Object>) field.get(server);
//                    if (!existing.getClass().getName().contains("SynchronizedMap")) {
//                        field.set(server, Collections.synchronizedMap(existing));
//                    }
//                } catch (Throwable e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        if (generator == null) {
//            generator = server.getGenerator(name);
//        }
//        int dimension = 10 + console.worlds.size();
//        boolean used = false;
//        do {
//            for (final WorldServer ws : console.worlds) {
//                used = (ws.dimension == dimension);
//                if (used) {
//                    ++dimension;
//                    break;
//                }
//            }
//        } while (used);
//        final boolean hardcore = false;
//        final IDataManager sdm = new ServerNBTManager(server.getWorldContainer(), name, true, server.getHandle().getServer().dataConverterManager);
//        WorldData worlddata = sdm.getWorldData();
//        final WorldSettings worldSettings;
//        if (worlddata == null) {
//            worldSettings = new WorldSettings(creator.seed(), EnumGamemode.getById(server.getDefaultGameMode().getValue()), generateStructures, hardcore, type);
//            worldSettings.setGeneratorSettings(creator.generatorSettings());
//            worlddata = new WorldData(worldSettings, name);
//        } else {
//            worldSettings = null;
//        }
//        worlddata.checkName(name);
//        final WorldServer internal = (WorldServer)new WorldServer(console, sdm, worlddata, dimension, console.methodProfiler, creator.environment(), generator).b();
//        startSet(true); // Temporarily allow async chunk load since the world isn't added yet
//        if (worldSettings != null) {
//            internal.a(worldSettings);
//        }
//        endSet(true);
//        internal.scoreboard = server.getScoreboardManager().getMainScoreboard().getHandle();
//        internal.tracker = new EntityTracker(internal);
//        internal.addIWorldAccess(new WorldManager(console, internal));
//        internal.worldData.setDifficulty(EnumDifficulty.EASY);
//        internal.setSpawnFlags(true, true);
//        if (generator != null) {
//            internal.getWorld().getPopulators().addAll(generator.getDefaultPopulators(internal.getWorld()));
//        }
//        // Add the world
//        return TaskManager.IMP.sync(new RunnableVal<World>() {
//            @Override
//            public void run(World value) {
//                console.worlds.add(internal);
//                server.getPluginManager().callEvent(new WorldInitEvent(internal.getWorld()));
//                server.getPluginManager().callEvent(new WorldLoadEvent(internal.getWorld()));
//                this.value = internal.getWorld();
//            }
//        });
//    }

    @Override
    public int getCombinedId4Data(final ChunkSection lastSection, final int x, final int y, final int z) {
        final DataPaletteBlock<IBlockData> dataPalette = lastSection.getBlocks();
        final IBlockData ibd = dataPalette.a(x & 15, y & 15, z & 15);
        final int ordinal = ((Spigot_v1_13_R2) getAdapter()).adaptToInt(ibd);
        return BlockTypes.states[ordinal].getInternalId();
    }

    @Override
    public BiomeType getBiome(final net.minecraft.server.v1_13_R2.Chunk chunk, final int x, final int z) {
        final BiomeBase base = chunk.getBiomeIndex()[((z & 15) << 4) + (x & 15)];
        return getAdapter().adapt(CraftBlock.biomeBaseToBiome(base));
    }

    @Override
    public int getOpacity(final ChunkSection section, final int x, final int y, final int z) {
        final DataPaletteBlock<IBlockData> dataPalette = section.getBlocks();
        final IBlockData ibd = dataPalette.a(x & 15, y & 15, z & 15);
        pos.a(x, y, z);
        return ibd.b(nmsWorld, pos);
    }

    @Override
    public int getBrightness(final ChunkSection section, final int x, final int y, final int z) {
        final DataPaletteBlock<IBlockData> dataPalette = section.getBlocks();
        final IBlockData ibd = dataPalette.a(x & 15, y & 15, z & 15);
        return ibd.e();
    }

    @Override
    public int getOpacityBrightnessPair(final ChunkSection section, final int x, final int y, final int z) {
        final DataPaletteBlock<IBlockData> dataPalette = section.getBlocks();
        final IBlockData ibd = dataPalette.a(x & 15, y & 15, z & 15);
        pos.a(x, y, z);
        final int opacity = ibd.b(nmsWorld, pos);
        final int brightness = ibd.e();
        return MathMan.pair16(brightness, opacity);
    }

    @Override
    public void sendChunk(final int x, final int z, final int bitMask) {
        final net.minecraft.server.v1_13_R2.Chunk chunk = getCachedChunk(getWorld(), x, z);
        if (chunk != null) {
            sendChunk(getPlayerChunk((WorldServer) chunk.getWorld(), chunk.locX, chunk.locZ), chunk, bitMask);
        }
    }

    @Override
    public void sendChunkUpdatePLIB(final FaweChunk chunk, final FawePlayer... players) {
//        PlayerChunkMap playerManager = ((CraftWorld) getWorld()).getHandle().getPlayerChunkMap();
//        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
//        WirePacket packet = null;
//        try {
//            for (int i = 0; i < players.length; i++) {
//                CraftPlayer bukkitPlayer = ((CraftPlayer) ((BukkitPlayer) players[i]).parent);
//                EntityPlayer player = bukkitPlayer.getHandle();
//
//                if (playerManager.a(player, chunk.getX(), chunk.getZ())) {
//                    if (packet == null) {
//                        byte[] data;
//                        byte[] buffer = new byte[8192];
//                        if (chunk instanceof LazyFaweChunk) {
//                            chunk = (FaweChunk) chunk.getChunk();
//                        }
//                        if (chunk instanceof MCAChunk) {
//                            data = new MCAChunkPacket((MCAChunk) chunk, true, true, hasSky()).apply(buffer);
//                        } else {
//                            data = new FaweChunkPacket(chunk, true, true, hasSky()).apply(buffer);
//                        }
//                        packet = new WirePacket(PacketType.Play.Server.MAP_CHUNK, data);
//                    }
//                    manager.sendWirePacket(bukkitPlayer, packet);
//                }
//            }
//        } catch (InvocationTargetException e) {
//            throw new RuntimeException(e);
//        }
        super.sendChunkUpdatePLIB(chunk, players); // TODO remove
    }

    @Override
    public void sendBlockUpdate(final FaweChunk chunk, final FawePlayer... players) {
        try {
            final PlayerChunkMap playerManager = ((CraftWorld) getWorld()).getHandle().getPlayerChunkMap();
            boolean watching = false;
            final boolean[] watchingArr = new boolean[players.length];
            for (int i = 0; i < players.length; i++) {
                final EntityPlayer player = ((CraftPlayer) ((BukkitPlayer) players[i]).parent).getHandle();
                if (playerManager.a(player, chunk.getX(), chunk.getZ())) {
                    watchingArr[i] = true;
                    watching = true;
                }
            }
            if (!watching) return;
            final LongAdder size = new LongAdder();
            if (chunk instanceof VisualChunk) {
                size.add(((VisualChunk) chunk).size());
            } else if (chunk instanceof IntFaweChunk) {
                size.add(((IntFaweChunk) chunk).getTotalCount());
            } else {
                chunk.forEachQueuedBlock(new FaweChunkVisitor() {
                    @Override
                    public void run(final int localX, final int y, final int localZ, final int combined) {
                        size.add(1);
                    }
                });
            }
            if (size.intValue() == 0) return;
            final PacketPlayOutMultiBlockChange packet = new PacketPlayOutMultiBlockChange();
            final ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
            final PacketDataSerializer buffer = new PacketDataSerializer(byteBuf);
            buffer.writeInt(chunk.getX());
            buffer.writeInt(chunk.getZ());
            buffer.d(size.intValue());
            chunk.forEachQueuedBlock(new FaweChunkVisitor() {
                @Override
                public void run(final int localX, final int y, final int localZ, int combined) {
                    final short index = (short) (localX << 12 | localZ << 8 | y);
                    if (combined < 16) combined = 0;
                    buffer.writeShort(index);
                    buffer.d(combined);
                }
            });
            packet.a(buffer);
            for (int i = 0; i < players.length; i++) {
                if (watchingArr[i]) ((CraftPlayer) ((BukkitPlayer) players[i]).parent).getHandle().playerConnection.sendPacket(packet);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void refreshChunk(final FaweChunk fc) {
        sendChunk(fc.getX(), fc.getZ(), fc.getBitMask());
    }

    public void sendPacket(final int cx, final int cz, final Packet packet) {
        final PlayerChunk chunk = getPlayerChunk(nmsWorld, cx, cz);
        if (chunk != null) {
            for (final EntityPlayer player : chunk.players) {
                player.playerConnection.sendPacket(packet);
            }
        }
    }

    private PlayerChunk getPlayerChunk(final WorldServer w, final int cx, final int cz) {
        final PlayerChunkMap chunkMap = w.getPlayerChunkMap();
        final PlayerChunk playerChunk = chunkMap.getChunk(cx, cz);
        if (playerChunk == null) {
            return null;
        }
        if (playerChunk.players.isEmpty()) {
            return null;
        }
        return playerChunk;
    }

    public boolean sendChunk(final PlayerChunk playerChunk, final net.minecraft.server.v1_13_R2.Chunk nmsChunk, final int mask) {
        if (playerChunk == null) {
            return false;
        }
        if (playerChunk.e()) {
            final ChunkSection[] sections = nmsChunk.getSections();
            for (int layer = 0; layer < 16; layer++) {
                if (sections[layer] == null && (mask & (1 << layer)) != 0) {
                    sections[layer] = new ChunkSection(layer << 4, nmsWorld.worldProvider.g());
                }
            }
            TaskManager.IMP.sync(new Supplier<Object>() {
                @Override
                public Object get() {
                    try {
                        int dirtyBits = fieldDirtyBits.getInt(playerChunk);
                        if (dirtyBits == 0) {
                            ((CraftWorld) getWorld()).getHandle().getPlayerChunkMap().a(playerChunk);
                        }
                        if (mask == 0) {
                            dirtyBits = 65535;
                        } else {
                            dirtyBits |= mask;
                        }

                        fieldDirtyBits.set(playerChunk, dirtyBits);
                        fieldDirtyCount.set(playerChunk, 64);
                    } catch (final IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            });
        }
//        if (mask == 0) {
//            PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(nmsChunk, 65535);
//            for (EntityPlayer player : playerChunk.players) {
//                player.playerConnection.sendPacket(packet);
//            }
//            return true;
//        }
//        // Send chunks
//        boolean empty = false;
//        ChunkSection[] sections = nmsChunk.getSections();
//        for (int i = 0; i < sections.length; i++) {
//            if (sections[i] == null) {
//                sections[i] = emptySection;
//                empty = true;
//            }
//        }
//        if (mask == 0 || mask == 65535 && hasEntities(nmsChunk)) {
//            PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(nmsChunk, 65280);
//            for (EntityPlayer player : playerChunk.players) {
//                player.playerConnection.sendPacket(packet);
//            }
//            mask = 255;
//        }
//        PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(nmsChunk, mask);
//        for (EntityPlayer player : playerChunk.players) {
//            player.playerConnection.sendPacket(packet);
//        }
//        if (empty) {
//            for (int i = 0; i < sections.length; i++) {
//                if (sections[i] == emptySection) {
//                    sections[i] = null;
//                }
//            }
//        }
        return true;
    }

    public boolean hasEntities(final net.minecraft.server.v1_13_R2.Chunk nmsChunk) {
        try {
            final Collection<Entity>[] entities = nmsChunk.entitySlices;
            for (int i = 0; i < entities.length; i++) {
                final Collection<Entity> slice = entities[i];
                if (slice != null && !slice.isEmpty()) {
                    return true;
                }
            }
        } catch (final Throwable ignore) {}
        return false;
    }

    @Override
    public boolean removeSectionLighting(final ChunkSection section, final int layer, final boolean sky) {
        if (section != null) {
            Arrays.fill(section.getEmittedLightArray().asBytes(), (byte) 0);
            if (sky) {
                final byte[] light = section.getSkyLightArray().asBytes();
                if (light != null) {
                    Arrays.fill(light, (byte) 0);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void setFullbright(final ChunkSection[] sections) {
        for (int i = 0; i < sections.length; i++) {
            final ChunkSection section = sections[i];
            if (section != null) {
                final byte[] bytes = section.getSkyLightArray().asBytes();
                Arrays.fill(bytes, (byte) 255);
            }
        }
    }

    @Override
    public int getSkyLight(final ChunkSection section, final int x, final int y, final int z) {
        return section.c(x & 15, y & 15, z & 15);
    }

    @Override
    public int getEmmittedLight(final ChunkSection section, final int x, final int y, final int z) {
        return section.d(x & 15, y & 15, z & 15);
    }

    @Override
    public void relightBlock(final int x, final int y, final int z) {
        pos.c(x, y, z);
        nmsWorld.c(EnumSkyBlock.BLOCK, pos);
    }

    @Override
    public void relightSky(final int x, final int y, final int z) {
        pos.c(x, y, z);
        nmsWorld.c(EnumSkyBlock.SKY, pos);
    }

    @Override
    public void relight(final int x, final int y, final int z) {
        pos.c(x, y, z);
        nmsWorld.r(pos);
    }

    protected WorldServer nmsWorld;

    @Override
    public World getImpWorld() {
        final World world = super.getImpWorld();
        if (world != null) {
            this.nmsWorld = ((CraftWorld) world).getHandle();
            return super.getImpWorld();
        } else {
            return null;
        }
    }

    public static void setCount(final int tickingBlockCount, final int nonEmptyBlockCount, final ChunkSection section) throws NoSuchFieldException, IllegalAccessException {
        fieldFluidCount.set(section, 0); // TODO FIXME
        fieldTickingBlockCount.set(section, tickingBlockCount);
        fieldNonEmptyBlockCount.set(section, nonEmptyBlockCount);
    }

    public int getNonEmptyBlockCount(final ChunkSection section) throws IllegalAccessException {
        return (int) fieldNonEmptyBlockCount.get(section);
    }

    public void setPalette(final ChunkSection section, final DataPaletteBlock palette) throws NoSuchFieldException, IllegalAccessException {
        fieldSection.set(section, palette);
        Arrays.fill(section.getEmittedLightArray().asBytes(), (byte) 0);
    }

    public static ChunkSection newChunkSection(final int y2, final boolean flag, final int[] blocks) {
        ChunkSection section = new ChunkSection(y2 << 4, flag);
        if (blocks == null) {
            return section;
        }
        final int[] blockToPalette = FaweCache.BLOCK_TO_PALETTE.get();
        final int[] paletteToBlock = FaweCache.PALETTE_TO_BLOCK.get();
        final long[] blockstates = FaweCache.BLOCK_STATES.get();
        final int[] blocksCopy = FaweCache.SECTION_BLOCKS.get();
        try {
            int num_palette = 0;
            int air = 0;
            for (int i = 0; i < 4096; i++) {
                int stateId = blocks[i];
                switch (stateId) {
                    case 0:
                    case BlockID.AIR:
                    case BlockID.CAVE_AIR:
                    case BlockID.VOID_AIR:
                        stateId = BlockID.AIR;
                        air++;
                }
                final int ordinal = BlockState.getFromInternalId(stateId).getOrdinal(); // TODO fixme Remove all use of BlockTypes.BIT_OFFSET so that this conversion isn't necessary
                int palette = blockToPalette[ordinal];
                if (palette == Integer.MAX_VALUE) {
                    blockToPalette[ordinal] = palette = num_palette;
                    paletteToBlock[num_palette] = ordinal;
                    num_palette++;
                }
                blocksCopy[i] = palette;
            }

            // BlockStates
            int bitsPerEntry = MathMan.log2nlz(num_palette - 1);
            if (Settings.IMP.PROTOCOL_SUPPORT_FIX || num_palette != 1) {
                bitsPerEntry = Math.max(bitsPerEntry, 4); // Protocol support breaks <4 bits per entry
            } else {
                bitsPerEntry = Math.max(bitsPerEntry, 1); // For some reason minecraft needs 4096 bits to store 0 entries
            }

            final int blockBitArrayEnd = (bitsPerEntry * 4096) >> 6;
            if (num_palette == 1) {
                for (int i = 0; i < blockBitArrayEnd; i++) blockstates[i] = 0;
            } else {
                final BitArray4096 bitArray = new BitArray4096(blockstates, bitsPerEntry);
                bitArray.fromRaw(blocksCopy);
            }

            // set palette & data bits
            final DataPaletteBlock<IBlockData> dataPaletteBlocks = section.getBlocks();
            // private DataPalette<T> h;
            // protected DataBits a;
            final long[] bits = Arrays.copyOfRange(blockstates, 0, blockBitArrayEnd);
            final DataBits nmsBits = new DataBits(bitsPerEntry, 4096, bits);
            final DataPalette<IBlockData> palette;
//                palette = new DataPaletteHash<>(Block.REGISTRY_ID, bitsPerEntry, dataPaletteBlocks, GameProfileSerializer::d, GameProfileSerializer::a);
            palette = new DataPaletteLinear<>(Block.REGISTRY_ID, bitsPerEntry, dataPaletteBlocks, GameProfileSerializer::d);

            // set palette
            for (int i = 0; i < num_palette; i++) {
                final int ordinal = paletteToBlock[i];
                blockToPalette[ordinal] = Integer.MAX_VALUE;
                final BlockState state = BlockTypes.states[ordinal];
                final IBlockData ibd = ((BlockMaterial_1_13) state.getMaterial()).getState();
                palette.a(ibd);
            }
            try {
                fieldBits.set(dataPaletteBlocks, nmsBits);
                fieldPalette.set(dataPaletteBlocks, palette);
                fieldSize.set(dataPaletteBlocks, bitsPerEntry);
                setCount(0, 4096 - air, section);
            } catch (final IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }

            return section;
        } catch (final Throwable e){
            Arrays.fill(blockToPalette, Integer.MAX_VALUE);
            throw e;
        }
    }

    protected BlockPosition.MutableBlockPosition pos = new BlockPosition.MutableBlockPosition(0, 0, 0);

    @Override
    public CompoundTag getTileEntity(final net.minecraft.server.v1_13_R2.Chunk chunk, final int x, final int y, final int z) {
        final Map<BlockPosition, TileEntity> tiles = chunk.getTileEntities();
        pos.c(x, y, z);
        final TileEntity tile = tiles.get(pos);
        return tile != null ? getTag(tile) : null;
    }

    public CompoundTag getTag(final TileEntity tile) {
        try {
            final NBTTagCompound tag = new NBTTagCompound();
            tile.save(tag); // readTagIntoEntity
            return (CompoundTag) toNative(tag);
        } catch (final Exception e) {
            MainUtil.handleError(e);
            return null;
        }
    }

    @Deprecated
    public boolean unloadChunk(final String world, final Chunk chunk) {
        final net.minecraft.server.v1_13_R2.Chunk c = ((CraftChunk) chunk).getHandle();
        c.mustSave = false;
        if (chunk.isLoaded()) {
            chunk.unload(false, false);
        }
        return true;
    }

    @Override
    public BukkitChunk_1_13 getFaweChunk(final int x, final int z) {
        return new BukkitChunk_1_13(this, x, z);
    }
}
