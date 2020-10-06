/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.bukkit.adapter.impl;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.implementation.packet.ChunkPacket;
import com.boydti.fawe.beta.implementation.queue.SingleThreadQueueExtent;
import com.boydti.fawe.bukkit.adapter.mc1_15_2.BlockMaterial_1_15_2;
import com.boydti.fawe.bukkit.adapter.mc1_15_2.BukkitAdapter_1_15_2;
import com.boydti.fawe.bukkit.adapter.mc1_15_2.BukkitGetBlocks_1_15_2;
import com.boydti.fawe.bukkit.adapter.mc1_15_2.FAWEWorldNativeAccess_1_15_2;
import com.boydti.fawe.bukkit.adapter.mc1_15_2.MapChunkUtil_1_15_2;
import com.boydti.fawe.bukkit.adapter.mc1_15_2.nbt.LazyCompoundTag_1_15_2;
import com.boydti.fawe.util.MathMan;
import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Either;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.blocks.TileEntityBlock;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.bukkit.adapter.CachedBukkitAdapter;
import com.sk89q.worldedit.bukkit.adapter.IDelegateBukkitImplAdapter;
import com.sk89q.worldedit.bukkit.adapter.Regenerator;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.LazyBaseEntity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.wna.WorldNativeAccess;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.util.io.file.SafeFiles;
import com.sk89q.worldedit.world.RegenOptions;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_15_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_15_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_15_R1.generator.CustomChunkGenerator;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.generator.BlockPopulator;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.bukkit.craftbukkit.v1_15_R1.CraftServer;
import org.bukkit.craftbukkit.v1_15_R1.util.CraftMagicNumbers;

import static com.google.common.base.Preconditions.checkNotNull;

public final class FAWE_Spigot_v1_15_R2 extends CachedBukkitAdapter implements IDelegateBukkitImplAdapter<NBTBase> {
    private final Spigot_v1_15_R2 parent;
    private char[] ibdToStateOrdinal;
    
    // ------------------------------------------------------------------------
    // Code that may break between versions of Minecraft
    // ------------------------------------------------------------------------

    public FAWE_Spigot_v1_15_R2() throws NoSuchFieldException, NoSuchMethodException {
        this.parent = new Spigot_v1_15_R2();
    }

    @Override
    public BukkitImplAdapter<NBTBase> getParent() {
        return parent;
    }

    private synchronized boolean init() {
        if (ibdToStateOrdinal != null && ibdToStateOrdinal[1] != 0) {
            return false;
        }
        ibdToStateOrdinal = new char[Block.REGISTRY_ID.a()]; // size
        for (int i = 0; i < ibdToStateOrdinal.length; i++) {
            BlockState state = BlockTypesCache.states[i];
            BlockMaterial_1_15_2 material = (BlockMaterial_1_15_2) state.getMaterial();
            int id = Block.REGISTRY_ID.getId(material.getState());
            ibdToStateOrdinal[id] = state.getOrdinalChar();
        }
        return true;
    }

    @Override
    public BlockMaterial getMaterial(BlockType blockType) {
        Block block = getBlock(blockType);
        return new BlockMaterial_1_15_2(block);
    }

    @Override
    public BlockMaterial getMaterial(BlockState state) {
        IBlockData bs = ((CraftBlockData) Bukkit.createBlockData(state.getAsString())).getState();
        return new BlockMaterial_1_15_2(bs.getBlock(), bs);
    }

    public Block getBlock(BlockType blockType) {
        return IRegistry.BLOCK.get(new MinecraftKey(blockType.getNamespace(), blockType.getResource()));
    }

    @SuppressWarnings("deprecation")
    @Override
    public BaseBlock getBlock(Location location) {
        Preconditions.checkNotNull(location);

        CraftWorld craftWorld = ((CraftWorld) location.getWorld());
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        final WorldServer handle = craftWorld.getHandle();
        Chunk chunk = handle.getChunkAt(x >> 4, z >> 4);
        final BlockPosition blockPos = new BlockPosition(x, y, z);
        org.bukkit.block.Block bukkitBlock = location.getBlock();
        BlockState state = BukkitAdapter.adapt(bukkitBlock.getBlockData());
        if (state.getBlockType().getMaterial().hasContainer()) {

        // Read the NBT data
            TileEntity te = chunk.a(blockPos, Chunk.EnumTileEntityState.CHECK);
            if (te != null) {
                NBTTagCompound tag = new NBTTagCompound();
                te.save(tag); // readTileEntityIntoTag - load data
                return state.toBaseBlock((CompoundTag) toNative(tag));
            }
        }

        return state.toBaseBlock();
    }

    @Override
    public Set<SideEffect> getSupportedSideEffects() {
        return SideEffectSet.defaults().getSideEffectsToApply();
    }

    public boolean setBlock(org.bukkit.Chunk chunk, int x, int y, int z, BlockStateHolder state, boolean update) {
        CraftChunk craftChunk = (CraftChunk) chunk;
        Chunk nmsChunk = craftChunk.getHandle();
        World nmsWorld = nmsChunk.getWorld();

        BlockPosition blockPos = new BlockPosition(x, y, z);
        IBlockData blockData = ((BlockMaterial_1_15_2) state.getMaterial()).getState();
        ChunkSection[] sections = nmsChunk.getSections();
        int y4 = y >> 4;
        ChunkSection section = sections[y4];

        IBlockData existing;
        if (section == null) {
            existing = ((BlockMaterial_1_15_2) BlockTypes.AIR.getDefaultState().getMaterial()).getState();
        } else {
            existing = section.getType(x & 15, y & 15, z & 15);
        }


        nmsChunk.removeTileEntity(blockPos); // Force delete the old tile entity

        CompoundTag nativeTag = state instanceof BaseBlock ? ((BaseBlock)state).getNbtData() : null;
        if (nativeTag != null || existing instanceof TileEntityBlock) {
            nmsWorld.setTypeAndData(blockPos, blockData, 0);
            // remove tile
            if (nativeTag != null) {
                // We will assume that the tile entity was created for us,
                // though we do not do this on the Forge version
                TileEntity tileEntity = nmsWorld.getTileEntity(blockPos);
                if (tileEntity != null) {
                    NBTTagCompound tag = (NBTTagCompound) fromNative(nativeTag);
                    tag.set("x", NBTTagInt.a(x));
                    tag.set("y", NBTTagInt.a(y));
                    tag.set("z", NBTTagInt.a(z));
                    tileEntity.load(tag); // readTagIntoTileEntity - load data
                }
            }
        } else {
            if (existing == blockData) {
                return true;
            }
            if (section == null) {
                if (blockData.isAir()) {
                    return true;
                }
                sections[y4] = section = new ChunkSection(y4 << 4);
            }
            nmsChunk.setType(blockPos, blockData, false);
        }
        if (update) {
            nmsWorld.getMinecraftWorld().notify(blockPos, existing, blockData, 0);
        }
        return true;
    }

    @Override
    public WorldNativeAccess<?, ?, ?> createWorldNativeAccess(org.bukkit.World world) {
        return new FAWEWorldNativeAccess_1_15_2(this,
                new WeakReference<>(((CraftWorld) world).getHandle()));
    }

    @Nullable
    private static String getEntityId(Entity entity) {
        MinecraftKey minecraftkey = EntityTypes.getName(entity.getEntityType());
        return minecraftkey == null ? null : minecraftkey.toString();
    }

    private static void readEntityIntoTag(Entity entity, NBTTagCompound tag) {
        entity.save(tag);
    }

    @Override
    public BaseEntity getEntity(org.bukkit.entity.Entity entity) {
        Preconditions.checkNotNull(entity);

        CraftEntity craftEntity = ((CraftEntity) entity);
        Entity mcEntity = craftEntity.getHandle();

        String id = getEntityId(mcEntity);

        if (id != null) {
            EntityType type = com.sk89q.worldedit.world.entity.EntityTypes.get(id);
            Supplier<CompoundTag> saveTag = () -> {
                NBTTagCompound tag = new NBTTagCompound();
                readEntityIntoTag(mcEntity, tag);

                //add Id for AbstractChangeSet to work
                CompoundTag natve = (CompoundTag) toNative(tag);
                natve.getValue().put("Id", new StringTag(id));
                return natve;
            };
            return new LazyBaseEntity(type, saveTag);
        } else {
            return null;
        }
    }

    @Override
    public OptionalInt getInternalBlockStateId(BlockState state) {
        BlockMaterial_1_15_2 material = (BlockMaterial_1_15_2) state.getMaterial();
        IBlockData mcState = material.getCraftBlockData().getState();
        return OptionalInt.of(Block.REGISTRY_ID.getId(mcState));
    }

    @Override
    public BlockState adapt(BlockData blockData) {
        CraftBlockData cbd = ((CraftBlockData) blockData);
        IBlockData ibd = cbd.getState();
        return adapt(ibd);
    }

    public BlockState adapt(IBlockData ibd) {
        return BlockTypesCache.states[adaptToChar(ibd)];
    }

    /**
     * @deprecated
     * Method unused. Use #adaptToChar(IBlockData).
     */
    @Deprecated
    public int adaptToInt(IBlockData ibd) {
        synchronized (this) {
            try {
                int id = Block.REGISTRY_ID.getId(ibd);
                return ibdToStateOrdinal[id];
            } catch (NullPointerException e) {
                init();
                return adaptToInt(ibd);
            }
        }
    }

    public char adaptToChar(IBlockData ibd) {
        synchronized (this) {
            try {
                int id = Block.REGISTRY_ID.getId(ibd);
                return ibdToStateOrdinal[id];
            } catch (NullPointerException e) {
                init();
                return adaptToChar(ibd);
            } catch (ArrayIndexOutOfBoundsException e1) {
                Fawe.debug("Attempted to convert " + ibd.getBlock() + " with ID " + Block.REGISTRY_ID.getId(ibd) + " to char. ibdToStateOrdinal length: " + ibdToStateOrdinal.length + ". Defaulting to air!");
                return 0;
            }
        }
    }

    @Override
    public <B extends BlockStateHolder<B>> BlockData adapt(B state) {
        BlockMaterial_1_15_2 material = (BlockMaterial_1_15_2) state.getMaterial();
        return material.getCraftBlockData();
    }

    private MapChunkUtil_1_15_2 mapUtil = new MapChunkUtil_1_15_2();

    @Override
    public void sendFakeChunk(org.bukkit.World world, Player player, ChunkPacket packet) {
        WorldServer nmsWorld = ((CraftWorld) world).getHandle();
        PlayerChunk map = BukkitAdapter_1_15_2.getPlayerChunk(nmsWorld, packet.getChunkX(), packet.getChunkZ());
        if (map != null && map.hasBeenLoaded()) {
            boolean flag = false;
            PlayerChunk.d players = map.players;
            Stream<EntityPlayer> stream = players.a(new ChunkCoordIntPair(packet.getChunkX(), packet.getChunkZ()), flag);

            EntityPlayer checkPlayer = player == null ? null : ((CraftPlayer) player).getHandle();
            stream.filter(entityPlayer -> checkPlayer == null || entityPlayer == checkPlayer)
                    .forEach(entityPlayer -> {
                        synchronized (packet) {
                            PacketPlayOutMapChunk nmsPacket = (PacketPlayOutMapChunk) packet.getNativePacket();
                            if (nmsPacket == null) {
                                nmsPacket = mapUtil.create( this, packet);
                                packet.setNativePacket(nmsPacket);
                            }
                            try {
                                FaweCache.IMP.CHUNK_FLAG.get().set(true);
                                entityPlayer.playerConnection.sendPacket(nmsPacket);
                            } finally {
                                FaweCache.IMP.CHUNK_FLAG.get().set(false);
                            }
                        }
                    });
        }
    }

    @Override
    public Map<String, ? extends Property<?>> getProperties(BlockType blockType) {
        return getParent().getProperties(blockType);
    }

    @Override
    public org.bukkit.inventory.ItemStack adapt(BaseItemStack item) {
        ItemStack stack = new ItemStack(IRegistry.ITEM.get(MinecraftKey.a(item.getType().getId())), item.getAmount());
        stack.setTag(((NBTTagCompound) fromNative(item.getNbtData())));
        return CraftItemStack.asCraftMirror(stack);
    }

    @Override
    public BaseItemStack adapt(org.bukkit.inventory.ItemStack itemStack) {
        final ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
        final BaseItemStack weStack = new BaseItemStack(BukkitAdapter.asItemType(itemStack.getType()), itemStack.getAmount());
        weStack.setNbtData(((CompoundTag) toNative(nmsStack.getTag())));
        return weStack;
    }

    @Override
    public Tag toNative(NBTBase foreign) {
        return parent.toNative(foreign);
    }

    @Override
    public NBTBase fromNative(Tag foreign) {
        if (foreign instanceof LazyCompoundTag_1_15_2) {
            return ((LazyCompoundTag_1_15_2) foreign).get();
        }
        return parent.fromNative(foreign);
    }
    
    @Override
    public boolean regenerate(org.bukkit.World bukkitWorld, Region region, Extent target, RegenOptions options) throws Exception {
        return new ReneratorImpl(bukkitWorld, region, target, options).regenerate();
    }

    @Override
    public IChunkGet get(org.bukkit.World world, int chunkX, int chunkZ) {
        return new BukkitGetBlocks_1_15_2(world, chunkX, chunkZ);
    }

    @Override
    public int getInternalBiomeId(BiomeType biome) {
        BiomeBase base = CraftBlock.biomeToBiomeBase(BukkitAdapter.adapt(biome));
        return IRegistry.BIOME.a(base);
    }
    
    private static class ReneratorImpl extends Regenerator {

        private static final Field serverWorldsField;
        private static final Field worldPaperConfigField;
        private static final Field flatBedrockField;
        private static final Field delegateField;
        private static final Field chunkProviderField;

        //list of chunk stati in correct order without FULL
        private static final Map<ChunkStatus, Concurrency> chunkStati = new LinkedHashMap<>();

        static {
            chunkStati.put(ChunkStatus.EMPTY, Concurrency.FULL);                  // radius -1, does nothing
            chunkStati.put(ChunkStatus.STRUCTURE_STARTS, Concurrency.NONE);       // uses unsynchronized maps
            chunkStati.put(ChunkStatus.STRUCTURE_REFERENCES, Concurrency.FULL);   // radius 8, but no writes to other chunks, only current chunk
            chunkStati.put(ChunkStatus.BIOMES, Concurrency.FULL);                 // radius 0
            chunkStati.put(ChunkStatus.NOISE, Concurrency.RADIUS);                // radius 8
            chunkStati.put(ChunkStatus.SURFACE, Concurrency.FULL);                // radius 0
            chunkStati.put(ChunkStatus.CARVERS, Concurrency.NONE);                // radius 0, but RADIUS and FULL change results
            chunkStati.put(ChunkStatus.LIQUID_CARVERS, Concurrency.NONE);         // radius 0, but RADIUS and FULL change results
            chunkStati.put(ChunkStatus.FEATURES, Concurrency.NONE);               // uses unsynchronized maps
            chunkStati.put(ChunkStatus.LIGHT, Concurrency.FULL);                  // radius 1, but no writes to other chunks, only current chunk
            chunkStati.put(ChunkStatus.SPAWN, Concurrency.FULL);                  // radius 0
            chunkStati.put(ChunkStatus.HEIGHTMAPS, Concurrency.FULL);             // radius 0
            
            try {
                serverWorldsField = CraftServer.class.getDeclaredField("worlds");
                serverWorldsField.setAccessible(true);

               Field tmpPaperConfigField = null;
                Field tmpFlatBedrockField = null;
                try { //only present on paper
                    tmpPaperConfigField = net.minecraft.server.v1_16_R2.World.class.getDeclaredField("paperConfig");
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
        private ExecutorService executor;

        private long seed;
        private Long2ObjectLinkedOpenHashMap<ProtoChunk> protoChunks;
        private Path tempDir;

        private boolean generateFlatBedrock = false;
        private boolean generateConcurrent = true;

        public ReneratorImpl(org.bukkit.World originalBukkitWorld, Region region, Extent target, RegenOptions options) {
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

            return true;
        }

        @Override
        protected boolean initNewWorld() throws Exception {
            //world folder
            tempDir = java.nio.file.Files.createTempDirectory("WorldEditWorldGen");

            //prepare for world init (see upstream implementation for reference)
            org.bukkit.World.Environment env = originalBukkitWorld.getEnvironment();
            org.bukkit.generator.ChunkGenerator gen = originalBukkitWorld.getGenerator();

            seed = options.getSeed().orElse(originalNMSWorld.getSeed());

            MinecraftServer server = originalNMSWorld.getServer().getServer();
            WorldData newWorldData = new WorldData(originalNMSWorld.worldData.a((NBTTagCompound)null), server.dataConverterManager, CraftMagicNumbers.INSTANCE.getDataVersion(), (NBTTagCompound)null);
            newWorldData.setName("worldeditregentempworld");
            WorldNBTStorage saveHandler = new WorldNBTStorage(new File(tempDir.toUri()), originalNMSWorld.getDataManager().getDirectory().getName(), server, server.dataConverterManager);

            //init world
            protoChunks = new Long2ObjectLinkedOpenHashMap<>(); //needs to be an ordered list for RegionLimitedWorldAccess
            freshNMSWorld = Fawe.get().getQueueHandler().sync((Supplier<WorldServer>) () -> new WorldServer(server, server.executorService, saveHandler, newWorldData, originalNMSWorld.worldProvider.getDimensionManager(), originalNMSWorld.getMethodProfiler(), new RegenNoOpWorldLoadListener(), env, gen)).get();
            freshNMSWorld.savingDisabled = true;

            DefinedStructureManager tmpStructureManager = saveHandler.f();
            freshChunkProvider = new ChunkProviderServer(freshNMSWorld, saveHandler.getDirectory(), server.aC(), tmpStructureManager, server.executorService, originalChunkProvider.chunkGenerator, freshNMSWorld.spigotConfig.viewDistance, new RegenNoOpWorldLoadListener(), () -> freshNMSWorld.getWorldPersistentData()) {
                // redirect to our protoChunks list
                @Override
                public IChunkAccess getChunkAt(int i, int j, ChunkStatus chunkstatus, boolean flag) {
                    return protoChunks.get(MathMan.pairInt(i, j));
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
            if (executor != null)
                executor.shutdownNow();

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
        protected boolean generate() throws Exception {
            if (generateConcurrent) {
                executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                System.out.println("using concurrent chunk generation");
            } else {
                System.out.println("using squential chunk generation (concurrent not supported)");
            }

            try {
                long start = System.currentTimeMillis();

                //TODO: can we get that required radius down without affecting chunk generation (e.g. strucures, features, ...)?
                //TODO: maybe do some chunk stages in parallel, e.g. those not requiring neighbour chunks?
                //      for those ChunkStati that need neighbox chunks a special queue could help (e.g. for FEATURES and NOISE)
                //generate chunk coords lists with a certain radius
                Int2ObjectOpenHashMap<List<Long>> chunkCoordsForRadius = new Int2ObjectOpenHashMap<>();
                time(() -> {
                    chunkStati.keySet().stream().map(ChunkStatus::f).distinct().forEach(radius -> {
                        if (radius == -1) //ignore ChunkStatus.EMPTY
                            return;
                        int border = 16 - radius; //9 = 8 + 1, 8: max border radius used in chunk stages, 1: need 1 extra chunk for chunk features to generate at the border of the region
                        chunkCoordsForRadius.put(radius, getChunkCoordsRegen(region, border));
                    });
                }, "precomputing chunkCoordsForRadius lists");

                //create chunks
                time(() -> {
                    for (Long xz : chunkCoordsForRadius.get(0)) {
                        ProtoChunk chunk = new ProtoChunk(new ChunkCoordIntPair(MathMan.unpairIntX(xz), MathMan.unpairIntY(xz)), ChunkConverter.a) {
                            public boolean generateFlatBedrock() { //todo needed?, not present in nms
                                return generateFlatBedrock;
                            }
                        };
                        protoChunks.put(xz, chunk);
                    }
                }, "ctor");

                //generate lists for RegionLimitedWorldAccess, need to be square with odd length (e.g. 17x17), 17 = 1 middle chunk + 8 border chunks * 2
                Int2ObjectOpenHashMap<Long2ObjectOpenHashMap<List<IChunkAccess>>> worldlimits = new Int2ObjectOpenHashMap<>();
                time(() -> {
                    chunkStati.keySet().stream().map(ChunkStatus::f).distinct().forEach(radius -> {
                        if (radius == -1) //ignore ChunkStatus.EMPTY
                            return;
                        Long2ObjectOpenHashMap<List<IChunkAccess>> map = new Long2ObjectOpenHashMap<>();
                        for (Long xz : chunkCoordsForRadius.get(radius)) {
                            int x = MathMan.unpairIntX(xz);
                            int z = MathMan.unpairIntY(xz);
                            List<IChunkAccess> l = new ArrayList<>((radius + 1 + radius) * (radius + 1 + radius));
                            for (int zz = z - radius; zz <= z + radius; zz++) { //order is important, first z then x
                                for (int xx = x - radius; xx <= x + radius; xx++) {
                                    l.add(protoChunks.get(MathMan.pairInt(xx, zz)));
                                }
                            }
                            map.put(xz, l);
                        }
                        worldlimits.put(radius, map);
                    });
                }, "precomputing RegionLimitedWorldAccess chunks lists");

                //run generation tasks exluding FULL chunk status
                for (Map.Entry<ChunkStatus, Concurrency> entry : chunkStati.entrySet()) {
                    time(() -> {
                        ChunkStatus chunkstatus = entry.getKey();
                        int radius = Math.max(0, chunkstatus.f()); //f() = required border chunks, EMPTY.f() == -1

                        List<Long> coords = chunkCoordsForRadius.get(radius);
                        if (this.generateConcurrent && entry.getValue() == Concurrency.RADIUS) {
                            SequentialTasks<ConcurrentTasks<SequentialTasks<Long>>> tasks = getChunkStatusTaskRows(coords, chunkstatus.f());
                            for (ConcurrentTasks<SequentialTasks<Long>> para : tasks) {
                                List scheduled = new ArrayList<>(tasks.size());
                                for (SequentialTasks<Long> row : para) {
                                    scheduled.add((Callable) () -> {
                                        for (Long xz : row) {
                                            processChunk(chunkstatus, xz, worldlimits.get(radius).get(xz));
                                        }
                                        return null;
                                    });
                                }
                                try {
                                    List<Future> futures = executor.invokeAll(scheduled);
                                    for (Future future : futures) {
                                        future.get();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } else if (this.generateConcurrent && entry.getValue() == Concurrency.FULL) {
                            // every chunk can be processed individually
                            List scheduled = new ArrayList(coords.size());
                            for (long xz : coords) {
                                scheduled.add((Callable) () -> {
                                    processChunk(chunkstatus, xz, worldlimits.get(radius).get(xz));
                                    return null;
                                });
                            }
                            try {
                                List<Future> futures = executor.invokeAll(scheduled);
                                for (Future future : futures) {
                                    future.get();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else { // Concurrency.NONE or generateConcurrent == false
                            // run sequential
                            for (long xz : coords) {
                                processChunk(chunkstatus, xz, worldlimits.get(radius).get(xz));
                            }
                        }
                    }, entry.getKey().d());
                }

                //convert to proper chunks
                Long2ObjectOpenHashMap<Chunk> chunks = new Long2ObjectOpenHashMap<>();
                time(() -> {
                    for (Long xz : chunkCoordsForRadius.get(0)) {
                        ProtoChunk chunk = protoChunks.get(xz);
                        chunks.put(xz, new Chunk(freshNMSWorld, chunk));
                    }
                }, "converting to chunks");

                //final chunkstatus
                time(() -> {
                    for (Long xz : chunkCoordsForRadius.get(0)) { //FULL.f() == 0!
                        Chunk chunk = chunks.get(xz);
                        processChunk(ChunkStatus.FULL, xz, Arrays.asList(chunk));
                    }
                }, "full");

                //populate
                List<BlockPopulator> defaultPopulators = originalNMSWorld.getWorld().getPopulators();
                time(() -> {
                    for (Long xz : chunkCoordsForRadius.get(0)) {
                        int x = MathMan.unpairIntX(xz);
                        int z = MathMan.unpairIntY(xz);

                        //prepare chunk seed
                        Random random = getChunkRandom(seed, x, z);

                        //actually populate
                        Chunk c = chunks.get(xz);
                        defaultPopulators.forEach(pop -> {
                            pop.populate(freshNMSWorld.getWorld(), random, c.bukkitChunk);
                        });
                    }
                }, "populate with " + defaultPopulators.size() + " populators");

                System.out.println("Finished chunk generation in " + (System.currentTimeMillis() - start) + " ms");
                source = new SingleThreadQueueExtent();
                source.init(null, (chunkX, chunkZ) -> new BukkitGetBlocks_1_15_2(freshNMSWorld, chunkX, chunkZ) {
                    @Override
                    public Chunk ensureLoaded(World nmsWorld, int X, int Z) {
                        return chunks.get(MathMan.pairInt(X, Z));
                    }
                }, null);
            } catch (Throwable e) {
                e.printStackTrace();
                if (e instanceof RuntimeException)
                    throw (RuntimeException) e;
                throw new RuntimeException(e);
            }
            return true;
        }

        private void processChunk(ChunkStatus chunkstatus, long xz, List<IChunkAccess> accessibleChunks) {
            try {
                chunkstatus.a(freshNMSWorld,
                              generator,
                              structureManager,
                              lightEngine,
                              c -> CompletableFuture.completedFuture(Either.left(c)),
                              accessibleChunks);
            } catch (Exception e) {
                System.err.println("error while running " + chunkstatus.d() + " on chunk " + MathMan.unpairIntX(xz) + "/" + MathMan.unpairIntY(xz));
                e.printStackTrace();
            }
        }

        private void time(Runnable r, String text) {
            long starttask = System.currentTimeMillis();
//            System.out.println(text);
            r.run();
            System.out.println(text + " took " + (System.currentTimeMillis() - starttask) + "ms");
        }
        
//        private void timeHigh(Runnable r, String text) {
//            long starttask = System.currentTimeMillis();
//            r.run();
//            long dur = System.currentTimeMillis() - starttask;
//            if (dur >= 10)
//                System.out.println(text + " took " + dur + "ms");
//        }

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
                int mod = (int) Math.floorMod(e >> 24L, y);
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
}
