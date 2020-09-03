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
import com.boydti.fawe.bukkit.adapter.mc1_14.*;
import com.boydti.fawe.bukkit.adapter.mc1_14.nbt.LazyCompoundTag_1_14;
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
import com.sk89q.worldedit.internal.wna.WorldNativeAccess;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.*;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import net.minecraft.server.v1_14_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_14_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_14_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import com.sk89q.jnbt.StringTag;

public final class FAWE_Spigot_v1_14_R4 extends CachedBukkitAdapter implements IDelegateBukkitImplAdapter<NBTBase> {
    private final Spigot_v1_14_R4 parent;
    private char[] ibdToStateOrdinal;

    // ------------------------------------------------------------------------
    // Code that may break between versions of Minecraft
    // ------------------------------------------------------------------------

    public FAWE_Spigot_v1_14_R4() throws NoSuchFieldException, NoSuchMethodException {
        this.parent = new Spigot_v1_14_R4();
    }

    @Override
    public BukkitImplAdapter<NBTBase> getParent() {
        return parent;
    }

    private synchronized boolean init() {
        if (ibdToStateOrdinal != null && ibdToStateOrdinal[1] != 0) return false;
        ibdToStateOrdinal = new char[Block.REGISTRY_ID.a()]; // size
        for (int i = 0; i < ibdToStateOrdinal.length; i++) {
            BlockState state = BlockTypesCache.states[i];
            BlockMaterial_1_14 material = (BlockMaterial_1_14) state.getMaterial();
            int id = Block.REGISTRY_ID.getId(material.getState());
            ibdToStateOrdinal[id] = state.getOrdinalChar();
        }
        return true;
    }

    @Override
    public BlockMaterial getMaterial(BlockType blockType) {
        Block block = getBlock(blockType);
        return new BlockMaterial_1_14(block);
    }

    @Override
    public BlockMaterial getMaterial(BlockState state) {
        IBlockData bs = ((CraftBlockData) Bukkit.createBlockData(state.getAsString())).getState();
        return new BlockMaterial_1_14(bs.getBlock(), bs);
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
    public Set<SideEffect> getSupportedSideEffects() {
        return SideEffectSet.defaults().getSideEffectsToApply();
    }

    public boolean setBlock(org.bukkit.Chunk chunk, int x, int y, int z, BlockStateHolder state, boolean update) {
        CraftChunk craftChunk = (CraftChunk) chunk;
        Chunk nmsChunk = craftChunk.getHandle();
        World nmsWorld = nmsChunk.getWorld();

        IBlockData blockData = ((BlockMaterial_1_14) state.getMaterial()).getState();
        ChunkSection[] sections = nmsChunk.getSections();
        int y4 = y >> 4;
        ChunkSection section = sections[y4];

        IBlockData existing;
        if (section == null) {
            existing = ((BlockMaterial_1_14) BlockTypes.AIR.getDefaultState().getMaterial()).getState();
        } else {
            existing = section.getType(x & 15, y & 15, z & 15);
        }

        BlockPosition pos = new BlockPosition(x, y, z);

        nmsChunk.removeTileEntity(pos); // Force delete the old tile entity

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
                sections[y4] = section = new ChunkSection(y4 << 4);
            }
            nmsChunk.setType(pos = new BlockPosition(x, y, z), blockData, false);
        }
        if (update) {
            nmsWorld.getMinecraftWorld().notify(pos, existing, blockData, 0);
        }
        return true;
    }

    @Override
    public WorldNativeAccess<?, ?, ?> createWorldNativeAccess(org.bukkit.World world) {
        return new FAWEWorldNativeAccess_1_14(this,
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
        checkNotNull(entity);

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
        BlockMaterial_1_14 material = (BlockMaterial_1_14) state.getMaterial();
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
            } catch(ArrayIndexOutOfBoundsException e1){
                Fawe.debug("Attempted to convert " + ibd.getBlock() + " with ID " + Block.REGISTRY_ID.getId(ibd) + " to char. ibdToStateOrdinal length: " + ibdToStateOrdinal.length + ". Defaulting to air!");
                return 0;
            }
        }
    }

    @Override
    public <B extends BlockStateHolder<B>> BlockData adapt(B state) {
        BlockMaterial_1_14 material = (BlockMaterial_1_14) state.getMaterial();
        return material.getCraftBlockData();
    }

    private MapChunkUtil_1_14 mapUtil = new MapChunkUtil_1_14();

    @Override
    public void sendFakeChunk(org.bukkit.World world, Player player, ChunkPacket packet) {
        WorldServer nmsWorld = ((CraftWorld) world).getHandle();
        PlayerChunk map = BukkitAdapter_1_14.getPlayerChunk(nmsWorld, packet.getChunkX(), packet.getChunkZ());
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
        if (foreign instanceof LazyCompoundTag_1_14) {
            return ((LazyCompoundTag_1_14) foreign).get();
        }
        return parent.fromNative(foreign);
    }

    @Override
    public IChunkGet get(org.bukkit.World world, int chunkX, int chunkZ) {
        return new BukkitGetBlocks_1_14(world, chunkX, chunkZ);
    }

    @Override
    public int getInternalBiomeId(BiomeType biome) {
        BiomeBase base = CraftBlock.biomeToBiomeBase(BukkitAdapter.adapt(biome));
        return IRegistry.BIOME.a(base);
    }
}
