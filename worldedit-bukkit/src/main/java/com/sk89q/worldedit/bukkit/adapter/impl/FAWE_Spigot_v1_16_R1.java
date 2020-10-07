/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.bukkit.adapter.impl;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IChunkCache;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.implementation.packet.ChunkPacket;
import com.boydti.fawe.bukkit.adapter.mc1_16_1.BlockMaterial_1_16_1;
import com.boydti.fawe.bukkit.adapter.mc1_16_1.BukkitAdapter_1_16_1;
import com.boydti.fawe.bukkit.adapter.mc1_16_1.BukkitGetBlocks_1_16_1;
import com.boydti.fawe.bukkit.adapter.mc1_16_1.FAWEWorldNativeAccess_1_16;
import com.boydti.fawe.bukkit.adapter.mc1_16_1.MapChunkUtil_1_16_1;
import com.boydti.fawe.bukkit.adapter.mc1_16_1.nbt.LazyCompoundTag_1_16_1;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
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
import com.sk89q.worldedit.bukkit.adapter.Regenerator.ChunkStatusWrapper;
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
import net.minecraft.server.v1_16_R1.Area;
import net.minecraft.server.v1_16_R1.AreaContextTransformed;
import net.minecraft.server.v1_16_R1.AreaFactory;
import net.minecraft.server.v1_16_R1.AreaTransformer8;
import net.minecraft.server.v1_16_R1.BiomeBase;
import net.minecraft.server.v1_16_R1.Biomes;
import net.minecraft.server.v1_16_R1.Block;
import net.minecraft.server.v1_16_R1.BlockPosition;
import net.minecraft.server.v1_16_R1.Chunk;
import net.minecraft.server.v1_16_R1.ChunkConverter;
import net.minecraft.server.v1_16_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R1.ChunkGenerator;
import net.minecraft.server.v1_16_R1.ChunkGeneratorAbstract;
import net.minecraft.server.v1_16_R1.ChunkProviderFlat;
import net.minecraft.server.v1_16_R1.ChunkProviderServer;
import net.minecraft.server.v1_16_R1.ChunkSection;
import net.minecraft.server.v1_16_R1.ChunkStatus;
import net.minecraft.server.v1_16_R1.Convertable;
import net.minecraft.server.v1_16_R1.DefinedStructureManager;
import net.minecraft.server.v1_16_R1.DynamicOpsNBT;
import net.minecraft.server.v1_16_R1.Entity;
import net.minecraft.server.v1_16_R1.EntityPlayer;
import net.minecraft.server.v1_16_R1.EntityTypes;
import net.minecraft.server.v1_16_R1.GenLayer;
import net.minecraft.server.v1_16_R1.GenLayers;
import net.minecraft.server.v1_16_R1.GeneratorSettingBase;
import net.minecraft.server.v1_16_R1.GeneratorSettings;
import net.minecraft.server.v1_16_R1.GeneratorSettingsFlat;
import net.minecraft.server.v1_16_R1.IBlockData;
import net.minecraft.server.v1_16_R1.IChunkAccess;
import net.minecraft.server.v1_16_R1.IRegistry;
import net.minecraft.server.v1_16_R1.IRegistryCustom;
import net.minecraft.server.v1_16_R1.ItemStack;
import net.minecraft.server.v1_16_R1.LightEngineThreaded;
import net.minecraft.server.v1_16_R1.LinearCongruentialGenerator;
import net.minecraft.server.v1_16_R1.MinecraftKey;
import net.minecraft.server.v1_16_R1.MinecraftServer;
import net.minecraft.server.v1_16_R1.NBTBase;
import net.minecraft.server.v1_16_R1.NBTTagCompound;
import net.minecraft.server.v1_16_R1.NBTTagInt;
import net.minecraft.server.v1_16_R1.NoiseGeneratorPerlin;
import net.minecraft.server.v1_16_R1.PacketPlayOutMapChunk;
import net.minecraft.server.v1_16_R1.PlayerChunk;
import net.minecraft.server.v1_16_R1.ProtoChunk;
import net.minecraft.server.v1_16_R1.RegistryReadOps;
import net.minecraft.server.v1_16_R1.ResourceKey;
import net.minecraft.server.v1_16_R1.TileEntity;
import net.minecraft.server.v1_16_R1.World;
import net.minecraft.server.v1_16_R1.WorldChunkManager;
import net.minecraft.server.v1_16_R1.WorldChunkManagerOverworld;
import net.minecraft.server.v1_16_R1.WorldDataServer;
import net.minecraft.server.v1_16_R1.WorldDimension;
import net.minecraft.server.v1_16_R1.WorldLoadListener;
import net.minecraft.server.v1_16_R1.WorldServer;
import net.minecraft.server.v1_16_R1.WorldSettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_16_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_16_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R1.CraftServer;
import org.bukkit.craftbukkit.v1_16_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_16_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R1.generator.CustomChunkGenerator;
import org.bukkit.entity.Player;
import org.bukkit.generator.BlockPopulator;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;


public final class FAWE_Spigot_v1_16_R1 extends CachedBukkitAdapter implements IDelegateBukkitImplAdapter<NBTBase> {
    private final Spigot_v1_16_R1 parent;
    private char[] ibdToStateOrdinal;
    
    // ------------------------------------------------------------------------
    // Code that may break between versions of Minecraft
    // ------------------------------------------------------------------------

    public FAWE_Spigot_v1_16_R1() throws NoSuchFieldException, NoSuchMethodException {
        this.parent = new Spigot_v1_16_R1();
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
            BlockMaterial_1_16_1 material = (BlockMaterial_1_16_1) state.getMaterial();
            int id = Block.REGISTRY_ID.getId(material.getState());
            ibdToStateOrdinal[id] = state.getOrdinalChar();
        }
        return true;
    }

    @Override
    public BlockMaterial getMaterial(BlockType blockType) {
        Block block = getBlock(blockType);
        return new BlockMaterial_1_16_1(block);
    }

    @Override
    public BlockMaterial getMaterial(BlockState state) {
        IBlockData bs = ((CraftBlockData) Bukkit.createBlockData(state.getAsString())).getState();
        return new BlockMaterial_1_16_1(bs.getBlock(), bs);
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
        IBlockData blockData = ((BlockMaterial_1_16_1) state.getMaterial()).getState();
        ChunkSection[] sections = nmsChunk.getSections();
        int y4 = y >> 4;
        ChunkSection section = sections[y4];

        IBlockData existing;
        if (section == null) {
            existing = ((BlockMaterial_1_16_1) BlockTypes.AIR.getDefaultState().getMaterial()).getState();
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
                    tileEntity.load(tileEntity.getBlock(), tag); // readTagIntoTileEntity - load data
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
        return new FAWEWorldNativeAccess_1_16(this,
                new WeakReference<>(((CraftWorld)world).getHandle()));
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
        BlockMaterial_1_16_1 material = (BlockMaterial_1_16_1) state.getMaterial();
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
        BlockMaterial_1_16_1 material = (BlockMaterial_1_16_1) state.getMaterial();
        return material.getCraftBlockData();
    }

    private MapChunkUtil_1_16_1 mapUtil = new MapChunkUtil_1_16_1();

    @Override
    public void sendFakeChunk(org.bukkit.World world, Player player, ChunkPacket packet) {
        WorldServer nmsWorld = ((CraftWorld) world).getHandle();
        PlayerChunk map = BukkitAdapter_1_16_1.getPlayerChunk(nmsWorld, packet.getChunkX(), packet.getChunkZ());
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
        if (foreign instanceof LazyCompoundTag_1_16_1) {
            return ((LazyCompoundTag_1_16_1) foreign).get();
        }
        return parent.fromNative(foreign);
    }
    
    @Override
    public boolean regenerate(org.bukkit.World bukkitWorld, Region region, Extent target, RegenOptions options) throws Exception {
        return new RegeneratorImpl(bukkitWorld, region, target, options).regenerate();
    }

    @Override
    public IChunkGet get(org.bukkit.World world, int chunkX, int chunkZ) {
        return new BukkitGetBlocks_1_16_1(world, chunkX, chunkZ);
    }

    @Override
    public int getInternalBiomeId(BiomeType biome) {
        BiomeBase base = CraftBlock.biomeToBiomeBase(BukkitAdapter.adapt(biome));
        return IRegistry.BIOME.a(base);
    }
    
    private static class RegeneratorImpl extends Regenerator<IChunkAccess, ProtoChunk, Chunk, RegeneratorImpl.ChunkStatusWrap> {

        private static final Field serverWorldsField;
        private static final Field worldPaperConfigField;
        private static final Field flatBedrockField;
        private static final Field generatorSettingBaseField;
        private static final Field generatorSettingFlatField;
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

                generatorSettingBaseField = ChunkGeneratorAbstract.class.getDeclaredField("h");
                generatorSettingBaseField.setAccessible(true);
                
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

        public RegeneratorImpl(org.bukkit.World originalBukkitWorld, Region region, Extent target, RegenOptions options) {
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
            WorldDataServer originalWorldData = originalNMSWorld.worldDataServer;

            MinecraftServer server = originalNMSWorld.getServer().getServer();
            WorldDataServer levelProperties = (WorldDataServer) server.getSaveData();
            RegistryReadOps<NBTBase> nbtRegOps = RegistryReadOps.a(DynamicOpsNBT.a, server.dataPackResources.h(), IRegistryCustom.b());
            GeneratorSettings newOpts = GeneratorSettings.a.encodeStart(nbtRegOps, levelProperties.getGeneratorSettings()).flatMap(tag -> GeneratorSettings.a.parse(this.recursivelySetSeed(new Dynamic<>(nbtRegOps, tag), seed, new HashSet<>()))).result().orElseThrow(() -> new IllegalStateException("Unable to map GeneratorOptions"));
            WorldSettings newWorldSettings = new WorldSettings("worldeditregentempworld", originalWorldData.b.getGameType(), originalWorldData.b.hardcore, originalWorldData.b.getDifficulty(), originalWorldData.b.e(), originalWorldData.b.getGameRules(), originalWorldData.b.g());
            WorldDataServer newWorldData = new WorldDataServer(newWorldSettings, newOpts, Lifecycle.stable());

            //init world
            freshNMSWorld = Fawe.get().getQueueHandler().sync((Supplier<WorldServer>) () -> new WorldServer(server, server.executorService, session, newWorldData, originalNMSWorld.getDimensionKey(), originalNMSWorld.getTypeKey(), originalNMSWorld.getDimensionManager(), new RegenNoOpWorldLoadListener(), ((WorldDimension)newOpts.e().a(worldDimKey)).c(), originalNMSWorld.isDebugWorld(), seed, ImmutableList.of(), false, env, gen)).get();

            freshChunkProvider = new ChunkProviderServer(freshNMSWorld, session, server.getDataFixer(), server.getDefinedStructureManager(), server.executorService, originalChunkProvider.chunkGenerator, freshNMSWorld.spigotConfig.viewDistance, server.isSyncChunkWrites(), new RegenNoOpWorldLoadListener(), () -> server.D().getWorldPersistentData()) {
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
                GeneratorSettingBase generatorSettingBase = (GeneratorSettingBase) generatorSettingBaseField.get(originalChunkProvider.getChunkGenerator());
                WorldChunkManager chunkManager = originalChunkProvider.getChunkGenerator().getWorldChunkManager();
                if (chunkManager instanceof WorldChunkManagerOverworld) {
                    chunkManager = fastOverWorldChunkManager(chunkManager);
                }
                generator = new ChunkGeneratorAbstract(chunkManager, seed, generatorSettingBase);
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
            return (chunkX, chunkZ) -> new BukkitGetBlocks_1_16_1(freshNMSWorld, chunkX, chunkZ) {
                @Override
                public Chunk ensureLoaded(World nmsWorld, int x, int z) {
                    return getChunkAt(x, z);
                }
            };
        }

        private class ChunkStatusWrap extends ChunkStatusWrapper<IChunkAccess> {

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
        private ResourceKey<WorldDimension> getWorldDimKey(org.bukkit.World.Environment env) {
            switch (env) {
                case NETHER:
                    return WorldDimension.THE_NETHER;
                case THE_END:
                    return WorldDimension.THE_END;
                case NORMAL:
                default:
                    return WorldDimension.OVERWORLD;
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
            Field genLayerField = WorldChunkManagerOverworld.class.getDeclaredField("f");
            genLayerField.setAccessible(true);
            Field areaLazyField = GenLayer.class.getDeclaredField("b");
            areaLazyField.setAccessible(true);
            Method initAreaFactoryMethod = GenLayers.class.getDeclaredMethod("a", boolean.class, int.class, int.class, LongFunction.class);
            initAreaFactoryMethod.setAccessible(true);
            
            //init new WorldChunkManagerOverworld
            boolean legacyBiomeInitLayer = legacyBiomeInitLayerField.getBoolean(chunkManager);
            boolean largebiomes = largeBiomesField.getBoolean(chunkManager);
            chunkManager = new WorldChunkManagerOverworld(seed, legacyBiomeInitLayer, largebiomes);
            
            //replace genLayer
            AreaFactory<FastAreaLazy> factory = (AreaFactory<FastAreaLazy>) initAreaFactoryMethod.invoke(null, legacyBiomeInitLayer, largebiomes ? 6 : 4, 4, (LongFunction) (l -> new FastWorldGenContextArea(seed, l)));
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
}
