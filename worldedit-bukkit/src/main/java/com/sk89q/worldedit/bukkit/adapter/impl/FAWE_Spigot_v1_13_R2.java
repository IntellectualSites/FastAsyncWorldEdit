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

import com.bekvon.bukkit.residence.commands.material;
import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.implementation.packet.ChunkPacket;
import com.boydti.fawe.beta.implementation.queue.SingleThreadQueueExtent;
import com.boydti.fawe.bukkit.adapter.mc1_13.BlockMaterial_1_13;
import com.boydti.fawe.bukkit.adapter.mc1_13.BukkitAdapter_1_13;
import com.boydti.fawe.bukkit.adapter.mc1_13.BukkitGetBlocks_1_13;
import com.boydti.fawe.bukkit.adapter.mc1_13.LazyCompoundTag_1_13;
import com.boydti.fawe.bukkit.adapter.mc1_13.MapChunkUtil_1_13;
import com.boydti.fawe.bukkit.adapter.mc1_14.BukkitGetBlocks_1_14;
import com.google.common.io.Files;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.blocks.TileEntityBlock;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.bukkit.adapter.CachedBukkitAdapter;
import com.sk89q.worldedit.bukkit.adapter.IDelegateBukkitImplAdapter;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.LazyBaseEntity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import net.minecraft.server.v1_13_R2.BiomeBase;
import net.minecraft.server.v1_13_R2.Block;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.Chunk;
import net.minecraft.server.v1_13_R2.ChunkProviderServer;
import net.minecraft.server.v1_13_R2.ChunkSection;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.EntityTypes;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.IDataManager;
import net.minecraft.server.v1_13_R2.IRegistry;
import net.minecraft.server.v1_13_R2.ItemStack;
import net.minecraft.server.v1_13_R2.MinecraftKey;
import net.minecraft.server.v1_13_R2.MinecraftServer;
import net.minecraft.server.v1_13_R2.NBTBase;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.NBTTagInt;
import net.minecraft.server.v1_13_R2.PacketPlayOutMapChunk;
import net.minecraft.server.v1_13_R2.PlayerChunk;
import net.minecraft.server.v1_13_R2.ServerNBTManager;
import net.minecraft.server.v1_13_R2.TileEntity;
import net.minecraft.server.v1_13_R2.World;
import net.minecraft.server.v1_13_R2.WorldData;
import net.minecraft.server.v1_13_R2.WorldNBTStorage;
import net.minecraft.server.v1_13_R2.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_13_R2.CraftChunk;
import org.bukkit.craftbukkit.v1_13_R2.CraftServer;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

public final class FAWE_Spigot_v1_13_R2 extends CachedBukkitAdapter implements IDelegateBukkitImplAdapter<NBTBase> {
    private final Spigot_v1_13_R2_2 parent;

    // ------------------------------------------------------------------------
    // Code that may break between versions of Minecraft
    // ------------------------------------------------------------------------

    public FAWE_Spigot_v1_13_R2() throws NoSuchFieldException, NoSuchMethodException {
        this.parent = new Spigot_v1_13_R2_2();
    }

    @Override
    public BukkitImplAdapter<NBTBase> getParent() {
        return parent;
    }

    public char[] idbToStateOrdinal;

    private synchronized boolean init() {
        if (idbToStateOrdinal != null) return false;
        idbToStateOrdinal = new char[Block.REGISTRY_ID.a()]; // size
        for (int i = 0; i < idbToStateOrdinal.length; i++) {
            BlockState state = BlockTypesCache.states[i];
            BlockMaterial mat = state.getMaterial();
            if (mat instanceof BlockMaterial_1_13) {
                BlockMaterial_1_13 nmsMat = (BlockMaterial_1_13) mat;
                int id = Block.REGISTRY_ID.getId(nmsMat.getState());
                idbToStateOrdinal[id] = state.getOrdinalChar();
            }
        }
        return true;
    }

    @Override
    public BlockMaterial getMaterial(BlockType blockType) {
        Block block = getBlock(blockType);
        return block != null ? new BlockMaterial_1_13(block) : null;
    }

    @Override
    public BlockMaterial getMaterial(BlockState state) {
        IBlockData bs = ((CraftBlockData) Bukkit.createBlockData(state.getAsString())).getState();
        return bs != null ? new BlockMaterial_1_13(bs.getBlock(), bs) : null;
    }

    public Block getBlock(BlockType blockType) {
        return IRegistry.BLOCK.get(new MinecraftKey(blockType.getNamespace(), blockType.getResource()));
    }

    @SuppressWarnings("deprecation")
    @Override
    public BaseBlock getBlock(Location location) {
        checkNotNull(location);

        CraftWorld craftWorld = ((CraftWorld) location.getWorld());
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        org.bukkit.block.Block bukkitBlock = location.getBlock();
        BlockState state = BukkitAdapter.adapt(bukkitBlock.getBlockData());
        if (state.getBlockType().getMaterial().hasContainer()) {
            //Read the NBT data
            TileEntity te = craftWorld.getHandle().getTileEntity(new BlockPosition(x, y, z));
            if (te != null) {
                NBTTagCompound tag = new NBTTagCompound();
                te.save(tag); // readTileEntityIntoTag
                return state.toBaseBlock((CompoundTag) toNative(tag));
            }
        }

        return state.toBaseBlock();
    }

    @Override
    public boolean setBlock(Location location, BlockStateHolder state, boolean notifyAndLight) {
        return this.setBlock(location.getChunk(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), state, notifyAndLight);
    }

    public boolean setBlock(org.bukkit.Chunk chunk, int x, int y, int z, BlockStateHolder state, boolean update) {
        CraftChunk craftChunk = (CraftChunk) chunk;
        Chunk nmsChunk = craftChunk.getHandle();
        World nmsWorld = nmsChunk.getWorld();

        IBlockData blockData = ((BlockMaterial_1_13) state.getMaterial()).getState();
        ChunkSection[] sections = nmsChunk.getSections();
        int y4 = y >> 4;
        ChunkSection section = sections[y4];

        IBlockData existing;
        if (section == null) {
            existing = ((BlockMaterial_1_13) BlockTypes.AIR.getDefaultState().getMaterial()).getState();
        } else {
            existing = section.getType(x & 15, y & 15, z & 15);
        }

        BlockPosition pos = new BlockPosition(x, y, z);

        nmsChunk.d(pos); // removeTileEntity } Force delete the old tile entity

        CompoundTag nativeTag = state instanceof BaseBlock ? ((BaseBlock)state).getNbtData() : null;
        if (nativeTag != null || existing instanceof TileEntityBlock) {
            nmsWorld.setTypeAndData(pos, blockData, 0);
            // remove tile
            if (nativeTag != null) {
                // We will assume that the tile entity was created for us,
                // though we do not do this on the Forge version
                TileEntity tileEntity = nmsWorld.getTileEntity(pos);
                if (tileEntity != null) {
                    NBTTagCompound tag = (NBTTagCompound) fromNative(nativeTag);
                    tag.set("x", new NBTTagInt(x));
                    tag.set("y", new NBTTagInt(y));
                    tag.set("z", new NBTTagInt(z));
                    tileEntity.load(tag); // readTagIntoTileEntity
                }
            }
        } else {
            if (existing == blockData) return true;
            if (section == null) {
                if (blockData.isAir()) return true;
                sections[y4] = section = new ChunkSection(y4 << 4, nmsWorld.worldProvider.g());
            }
            nmsChunk.setType(pos = new BlockPosition(x, y, z), blockData, false);
        }
        if (update) {
            nmsWorld.getMinecraftWorld().notify(pos, existing, blockData, 0);
        }
        return true;
    }

    @Nullable
    private static String getEntityId(Entity entity) {
        MinecraftKey minecraftkey = EntityTypes.getName(entity.P());
        return minecraftkey == null ? null : minecraftkey.toString();
    }

    private static void readEntityIntoTag(Entity entity, NBTTagCompound tag) {
        entity.save(tag);
    }

    @Override
    public BaseEntity getEntity(org.bukkit.entity.Entity entity) {
        checkNotNull(entity);

        CraftEntity craftEntity = ((CraftEntity) entity);
        Entity mcEntity = craftEntity.getHandle();

        String id = getEntityId(mcEntity);

        if (id != null) {
            EntityType type = com.sk89q.worldedit.world.entity.EntityTypes.get(id);
            Supplier<CompoundTag> saveTag = () -> {
                NBTTagCompound tag = new NBTTagCompound();
                readEntityIntoTag(mcEntity, tag);
                return (CompoundTag) toNative(tag);
            };
            return new LazyBaseEntity(type, saveTag);
        } else {
            return null;
        }
    }

    @Override
    public OptionalInt getInternalBlockStateId(BlockState state) {
        BlockMaterial_1_13 material = (BlockMaterial_1_13) state.getMaterial();
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
        return BlockTypesCache.states[adaptToInt(ibd)];
    }

    public int adaptToInt(IBlockData ibd) {
        try {
            int id = Block.REGISTRY_ID.getId(ibd);
            return idbToStateOrdinal[id];
        } catch (NullPointerException e) {
            init();
            return adaptToInt(ibd);
        }
    }

    public char adaptToChar(IBlockData ibd) {
        try {
            int id = Block.REGISTRY_ID.getId(ibd);
            return idbToStateOrdinal[id];
        } catch (NullPointerException e) {
            init();
            return adaptToChar(ibd);
        }
    }

    @Override
    public BlockData adapt(BlockStateHolder state) {
        BlockMaterial_1_13 material = (BlockMaterial_1_13) state.getMaterial();
        return material.getCraftBlockData();
    }

    @Override
    public void notifyAndLightBlock(Location position, BlockState previousType) {
        this.setBlock(position.getChunk(), position.getBlockX(), position.getBlockY(), position.getBlockZ(), previousType, true);
    }

    private MapChunkUtil_1_13 mapUtil = new MapChunkUtil_1_13();

    @Override
    public void sendFakeChunk(org.bukkit.World world, Player target, ChunkPacket packet) {
        WorldServer nmsWorld = ((CraftWorld) world).getHandle();
        PlayerChunk map = BukkitAdapter_1_13.getPlayerChunk(nmsWorld, packet.getChunkX(), packet.getChunkZ());
        if (map != null && map.e()) {
            boolean flag = false;
            List<EntityPlayer> inChunk = map.players;
            if (inChunk != null && !inChunk.isEmpty()) {
                EntityPlayer nmsTarget = target != null ? ((CraftPlayer) target).getHandle() : null;
                for (EntityPlayer current : inChunk) {
                    if (nmsTarget == null || current == nmsTarget) {
                        synchronized (packet) {
                            PacketPlayOutMapChunk nmsPacket = (PacketPlayOutMapChunk) packet.getNativePacket();
                            if (nmsPacket == null) {
                                nmsPacket = mapUtil.create(FAWE_Spigot_v1_13_R2.this, packet);
                                packet.setNativePacket(nmsPacket);
                            }
                            try {
                                FaweCache.IMP.CHUNK_FLAG.get().set(true);
                                current.playerConnection.sendPacket(nmsPacket);
                            } finally {
                                FaweCache.IMP.CHUNK_FLAG.get().set(false);
                            }
                        }
                    }
                }
            }
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
        if (foreign instanceof LazyCompoundTag_1_13) {
            return ((LazyCompoundTag_1_13) foreign).get();
        }
        return parent.fromNative(foreign);
    }

    @Override
    public boolean regenerate(org.bukkit.World world, Region region, @Nullable Long seed, @Nullable BiomeType biome, EditSession editSession) {
        WorldServer originalWorld = ((CraftWorld) world).getHandle();
        ChunkProviderServer provider = originalWorld.getChunkProvider();
        if (!(provider instanceof ChunkProviderServer)) {
            return false;
        }

        File saveFolder = Files.createTempDir();
        // register this just in case something goes wrong
        // normally it should be deleted at the end of this method
        saveFolder.deleteOnExit();
        try {
            MinecraftServer server = originalWorld.getServer().getServer();
            IDataManager originalDataManager = originalWorld.getDataManager();
            ServerNBTManager saveHandler = new ServerNBTManager(saveFolder,
                    originalWorld.getDataManager().getDirectory().getName(), server, server.dataConverterManager);
            WorldData newWorldData = new WorldData(originalWorld.worldData.a((NBTTagCompound) null),
                    server.dataConverterManager, getDataVersion(), null);
            newWorldData.checkName(UUID.randomUUID().toString());

            ChunkGenerator generator = world.getGenerator();
            org.bukkit.World.Environment environment = world.getEnvironment();
            if (seed != null) {
                if (biome == BiomeTypes.NETHER) {
                    environment = org.bukkit.World.Environment.NETHER;
                } else if (biome == BiomeTypes.THE_END) {
                    environment = org.bukkit.World.Environment.THE_END;
                } else {
                    environment = org.bukkit.World.Environment.NORMAL;
                }
                generator = null;
            }
            try (WorldServer freshWorld = new WorldServer(server,
                    saveHandler,
                    originalWorld.worldMaps,
                    newWorldData,
                    originalWorld.worldProvider.getDimensionManager(),
                    originalWorld.methodProfiler,
                    environment,
                    generator)) {
                SingleThreadQueueExtent extent = new SingleThreadQueueExtent();
                extent.init(null, (x, z) -> new BukkitGetBlocks_1_13(freshWorld, x, z) {
                    @Override
                    public Chunk ensureLoaded(World nmsWorld, int X, int Z) {
                        Chunk cached = nmsWorld.getChunkIfLoaded(X, Z);
                        if (cached != null) return cached;
                        Future<Chunk> future = Fawe.get().getQueueHandler().sync((Supplier<Chunk>) () -> freshWorld.getChunkAt(X, Z));
//                            while (!future.isDone()) {
//                                // this feels so dirty
//                                freshWorld.getChunkProvider().runTasks();
//                            }
                        try {
                            return future.get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, null);
                for (BlockVector3 vec : region) {
                    editSession.setBlock(vec, extent.getFullBlock(vec));
                }
            }
        } catch (MaxChangedBlocksException e) {
            throw new RuntimeException(e);
        } finally {
            saveFolder.delete();
        }
        return true;
    }

    @Override
    public IChunkGet get(org.bukkit.World world, int chunkX, int chunkZ) {
        return new BukkitGetBlocks_1_13(world, chunkX, chunkZ);
    }

    @Override
    public int getInternalBiomeId(BiomeType biome) {
        BiomeBase base = CraftBlock.biomeToBiomeBase(BukkitAdapter.adapt(biome));
        return IRegistry.BIOME.a(base);
    }
}
