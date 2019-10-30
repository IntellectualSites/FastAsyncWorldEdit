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

package com.boydti.fawe.bukkit.adapter.mc1_14;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.implementation.ChunkPacket;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.EndTag;
import com.sk89q.jnbt.FloatTag;
import com.sk89q.jnbt.IntArrayTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.LongArrayTag;
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.blocks.TileEntityBlock;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.bukkit.adapter.CachedBukkitAdapter;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.LazyBaseEntity;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.BooleanProperty;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.EnumProperty;
import com.sk89q.worldedit.registry.state.IntegerProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import net.minecraft.server.v1_14_R1.Block;
import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.BlockStateBoolean;
import net.minecraft.server.v1_14_R1.BlockStateDirection;
import net.minecraft.server.v1_14_R1.BlockStateEnum;
import net.minecraft.server.v1_14_R1.BlockStateInteger;
import net.minecraft.server.v1_14_R1.BlockStateList;
import net.minecraft.server.v1_14_R1.Chunk;
import net.minecraft.server.v1_14_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_14_R1.ChunkSection;
import net.minecraft.server.v1_14_R1.Entity;
import net.minecraft.server.v1_14_R1.EntityPlayer;
import net.minecraft.server.v1_14_R1.EntityTypes;
import net.minecraft.server.v1_14_R1.EnumDirection;
import net.minecraft.server.v1_14_R1.EnumHand;
import net.minecraft.server.v1_14_R1.EnumInteractionResult;
import net.minecraft.server.v1_14_R1.IBlockData;
import net.minecraft.server.v1_14_R1.IBlockState;
import net.minecraft.server.v1_14_R1.INamable;
import net.minecraft.server.v1_14_R1.IRegistry;
import net.minecraft.server.v1_14_R1.ItemActionContext;
import net.minecraft.server.v1_14_R1.ItemStack;
import net.minecraft.server.v1_14_R1.MinecraftKey;
import net.minecraft.server.v1_14_R1.MovingObjectPositionBlock;
import net.minecraft.server.v1_14_R1.NBTBase;
import net.minecraft.server.v1_14_R1.NBTTagByte;
import net.minecraft.server.v1_14_R1.NBTTagByteArray;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.NBTTagDouble;
import net.minecraft.server.v1_14_R1.NBTTagEnd;
import net.minecraft.server.v1_14_R1.NBTTagFloat;
import net.minecraft.server.v1_14_R1.NBTTagInt;
import net.minecraft.server.v1_14_R1.NBTTagIntArray;
import net.minecraft.server.v1_14_R1.NBTTagList;
import net.minecraft.server.v1_14_R1.NBTTagLong;
import net.minecraft.server.v1_14_R1.NBTTagLongArray;
import net.minecraft.server.v1_14_R1.NBTTagShort;
import net.minecraft.server.v1_14_R1.NBTTagString;
import net.minecraft.server.v1_14_R1.PacketPlayOutEntityStatus;
import net.minecraft.server.v1_14_R1.PacketPlayOutMapChunk;
import net.minecraft.server.v1_14_R1.PacketPlayOutTileEntityData;
import net.minecraft.server.v1_14_R1.PlayerChunk;
import net.minecraft.server.v1_14_R1.PlayerChunkMap;
import net.minecraft.server.v1_14_R1.TileEntity;
import net.minecraft.server.v1_14_R1.Vec3D;
import net.minecraft.server.v1_14_R1.World;
import net.minecraft.server.v1_14_R1.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_14_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_14_R1.CraftServer;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_14_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Spigot_v1_14_R4 extends CachedBukkitAdapter implements BukkitImplAdapter<NBTBase>{

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Field nbtListTagListField;
    private final Method nbtCreateTagMethod;

    static {
        // A simple test
        if (!Bukkit.getServer().getClass().getName().endsWith("DummyServer")) CraftServer.class.cast(Bukkit.getServer());
    }

    // ------------------------------------------------------------------------
    // Code that may break between versions of Minecraft
    // ------------------------------------------------------------------------

    public Spigot_v1_14_R4() throws NoSuchFieldException, NoSuchMethodException {
        // The list of tags on an NBTTagList
        nbtListTagListField = NBTTagList.class.getDeclaredField("list");
        nbtListTagListField.setAccessible(true);

        // The method to create an NBTBase tag given its type ID
        nbtCreateTagMethod = NBTBase.class.getDeclaredMethod("createTag", byte.class);
        nbtCreateTagMethod.setAccessible(true);
    }

    public char[] idbToStateOrdinal;

    private synchronized boolean init() {
        if (idbToStateOrdinal != null) return false;
        idbToStateOrdinal = new char[Block.REGISTRY_ID.a()]; // size
        for (int i = 0; i < idbToStateOrdinal.length; i++) {
            BlockState state = BlockTypes.states[i];
            BlockMaterial_1_14 material = (BlockMaterial_1_14) state.getMaterial();
            int id = Block.REGISTRY_ID.getId(material.getState());
            idbToStateOrdinal[id] = state.getOrdinalChar();
        }
        return true;
    }

    /**
     * Read the given NBT data into the given tile entity.
     *
     * @param tileEntity the tile entity
     * @param tag the tag
     */
    private static void readTagIntoTileEntity(NBTTagCompound tag, TileEntity tileEntity) {
        try {
            tileEntity.load(tag);
        } catch (Throwable e) {
            Fawe.debug("Invalid tag " + tag + " | " + tileEntity);
        }
    }

    /**
     * Write the tile entity's NBT data to the given tag.
     *
     * @param tileEntity the tile entity
     * @param tag the tag
     */
    private static void readTileEntityIntoTag(TileEntity tileEntity, NBTTagCompound tag) {
        tileEntity.save(tag);
    }

    /**
     * Get the ID string of the given entity.
     *
     * @param entity the entity
     * @return the entity ID or null if one is not known
     */
    @Nullable
    private static String getEntityId(Entity entity) {
        MinecraftKey minecraftkey = EntityTypes.getName(entity.getBukkitEntity().getHandle().getEntityType());
        return minecraftkey == null ? null : minecraftkey.toString();
    }

    /**
     * Create an entity using the given entity ID.
     *
     * @param id the entity ID
     * @param world the world
     * @return an entity or null
     */
    @Nullable
    private static Entity createEntityFromId(String id, World world) {
        return EntityTypes.a(id).get().a(world);
    }

    /**
     * Write the given NBT data into the given entity.
     *
     * @param entity the entity
     * @param tag the tag
     */
    private static void readTagIntoEntity(NBTTagCompound tag, Entity entity) {
        entity.f(tag);
    }

    /**
     * Write the entity's NBT data to the given tag.
     *
     * @param entity the entity
     * @param tag the tag
     */
    private static void readEntityIntoTag(Entity entity, NBTTagCompound tag) {
        entity.save(tag);
    }

    @Override
    public BlockMaterial getMaterial(BlockType blockType) {
        return new BlockMaterial_1_14(getBlock(blockType));
    }

    @Override
    public BlockMaterial getMaterial(BlockState state) {
        IBlockData bs = ((CraftBlockData) Bukkit.createBlockData(state.getAsString())).getState();
        return new BlockMaterial_1_14(bs.getBlock(), bs);
    }

    public Block getBlock(BlockType blockType) {
        return IRegistry.BLOCK.get(new MinecraftKey(blockType.getNamespace(), blockType.getResource()));
    }

    // ------------------------------------------------------------------------
    // Code that is less likely to break
    // ------------------------------------------------------------------------

    @Override
    public int getDataVersion() {
        return CraftMagicNumbers.INSTANCE.getDataVersion();
    }

    @Override
    public DataFixer getDataFixer() {
        return DataConverters_1_14_R4.INSTANCE;
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
                readTileEntityIntoTag(te, tag); // Load data
                return state.toBaseBlock((CompoundTag) toNative(tag));
            }
        }

        return state.toBaseBlock();
    }

    @Override
    public boolean isChunkInUse(org.bukkit.Chunk chunk) {
        CraftChunk craftChunk = (CraftChunk) chunk;
        PlayerChunkMap chunkMap = ((WorldServer) craftChunk.getHandle().getWorld()).getChunkProvider().playerChunkMap;
        return chunkMap.visibleChunks.containsKey(ChunkCoordIntPair.pair(chunk.getX(), chunk.getZ()));
    }

    @Override
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
                    readTagIntoTileEntity(tag, tileEntity); // Load data
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
    public BaseEntity getEntity(org.bukkit.entity.Entity entity) {
        checkNotNull(entity);

        CraftEntity craftEntity = ((CraftEntity) entity);
        Entity mcEntity = craftEntity.getHandle();

        String id = getEntityId(mcEntity);

        if (id != null) {
            EntityType type = com.sk89q.worldedit.world.entity.EntityTypes.get(id);
            Supplier<CompoundTag> saveTag = new Supplier<CompoundTag>() {
                @Override
                public CompoundTag get() {
                    NBTTagCompound tag = new NBTTagCompound();
                    readEntityIntoTag(mcEntity, tag);
                    return (CompoundTag) toNative(tag);
                }
            };
            return new LazyBaseEntity(type, saveTag);
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public org.bukkit.entity.Entity createEntity(Location location, BaseEntity state) {
        checkNotNull(location);
        checkNotNull(state);
        if (state.getType() == com.sk89q.worldedit.world.entity.EntityTypes.PLAYER) return null;
        CraftWorld craftWorld = ((CraftWorld) location.getWorld());
        WorldServer worldServer = craftWorld.getHandle();

        Entity createdEntity = createEntityFromId(state.getType().getId(), craftWorld.getHandle());

        if (createdEntity != null) {
            CompoundTag nativeTag = state.getNbtData();
            if (nativeTag != null) {
                NBTTagCompound tag = (NBTTagCompound) fromNative(nativeTag);
                for (String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
                    tag.remove(name);
                }
                readTagIntoEntity(tag, createdEntity);
            }

            createdEntity.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

            worldServer.addEntity(createdEntity, SpawnReason.CUSTOM);
            return createdEntity.getBukkitEntity();
        } else {
            Fawe.debug("Invalid entity " + state.getType().getId());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, ? extends Property<?>> getProperties(BlockType blockType) {
        Block block;
        try {
            block = IRegistry.BLOCK.get(new MinecraftKey(blockType.getNamespace(), blockType.getResource()));
        } catch (Throwable e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
        if (block == null) {
            logger.warn("Failed to find properties for " + blockType.getId());
            return Collections.emptyMap();
        }
        Map<String, Property<?>> properties = Maps.newLinkedHashMap();
        BlockStateList<Block, IBlockData> blockStateList = block.getStates();
        for (IBlockState state : blockStateList.d()) {
            Property property;
            if (state instanceof BlockStateBoolean) {
                property = new BooleanProperty(state.a(), ImmutableList.copyOf(state.getValues()));
            } else if (state instanceof BlockStateDirection) {
                property = new DirectionalProperty(state.a(),
                        (List<Direction>) state.getValues().stream().map(e -> Direction.valueOf(((INamable) e).getName().toUpperCase())).collect(Collectors.toList()));
            } else if (state instanceof BlockStateEnum) {
                property = new EnumProperty(state.a(),
                        (List<String>) state.getValues().stream().map(e -> ((INamable) e).getName()).collect(Collectors.toList()));
            } else if (state instanceof BlockStateInteger) {
                property = new IntegerProperty(state.a(), ImmutableList.copyOf(state.getValues()));
            } else {
                throw new IllegalArgumentException("WorldEdit needs an update to support " + state.getClass().getSimpleName());
            }

            properties.put(property.getName(), property);
        }
        return properties;
    }

    /**
     * Converts from a non-native NMS NBT structure to a native WorldEdit NBT
     * structure.
     *
     * @param foreign non-native NMS NBT structure
     * @return native WorldEdit NBT structure
     */
    @SuppressWarnings("unchecked")
    @Override
    public Tag toNative(NBTBase foreign) {
        if (foreign == null) {
            return null;
        }
        if (foreign instanceof NBTTagCompound) {
            Map<String, Tag> values = new HashMap<>();
            Set<String> foreignKeys = ((NBTTagCompound) foreign).getKeys(); // map.keySet

            for (String str : foreignKeys) {
                NBTBase base = ((NBTTagCompound) foreign).get(str);
                values.put(str, toNative(base));
            }
            return new CompoundTag(values);
        } else if (foreign instanceof NBTTagByte) {
            return new ByteTag(((NBTTagByte) foreign).asByte());
        } else if (foreign instanceof NBTTagByteArray) {
            return new ByteArrayTag(((NBTTagByteArray) foreign).getBytes()); // data
        } else if (foreign instanceof NBTTagDouble) {
            return new DoubleTag(((NBTTagDouble) foreign).asDouble()); // getDouble
        } else if (foreign instanceof NBTTagFloat) {
            return new FloatTag(((NBTTagFloat) foreign).asFloat());
        } else if (foreign instanceof NBTTagInt) {
            return new IntTag(((NBTTagInt) foreign).asInt());
        } else if (foreign instanceof NBTTagIntArray) {
            return new IntArrayTag(((NBTTagIntArray) foreign).getInts()); // data
        } else if (foreign instanceof NBTTagLongArray) {
            return new LongArrayTag(((NBTTagLongArray) foreign).getLongs()); // data
        } else if (foreign instanceof NBTTagList) {
            try {
                return toNativeList((NBTTagList) foreign);
            } catch (Throwable e) {
                logger.warn("Failed to convert NBTTagList", e);
                return new ListTag(ByteTag.class, new ArrayList<ByteTag>());
            }
        } else if (foreign instanceof NBTTagLong) {
            return new LongTag(((NBTTagLong) foreign).asLong());
        } else if (foreign instanceof NBTTagShort) {
            return new ShortTag(((NBTTagShort) foreign).asShort());
        } else if (foreign instanceof NBTTagString) {
            return new StringTag(foreign.asString());
        } else if (foreign instanceof NBTTagEnd) {
            return new EndTag();
        } else {
            throw new IllegalArgumentException("Don't know how to make native " + foreign.getClass().getCanonicalName());
        }
    }

    /**
     * Convert a foreign NBT list tag into a native WorldEdit one.
     *
     * @param foreign the foreign tag
     * @return the converted tag
     * @throws NoSuchFieldException on error
     * @throws SecurityException on error
     * @throws IllegalArgumentException on error
     * @throws IllegalAccessException on error
     */
    public ListTag toNativeList(NBTTagList foreign) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        List<Tag> values = new ArrayList<>();
        int type = foreign.getTypeId();

        List foreignList;
        foreignList = (List) nbtListTagListField.get(foreign);
        for (int i = 0; i < foreign.size(); i++) {
            NBTBase element = (NBTBase) foreignList.get(i);
            values.add(toNative(element)); // List elements shouldn't have names
        }

        Class<? extends Tag> cls = NBTConstants.getClassFromType(type);
        return new ListTag(cls, values);
    }

    /**
     * Converts a WorldEdit-native NBT structure to a NMS structure.
     *
     * @param foreign structure to convert
     * @return non-native structure
     */
    @Override
    public NBTBase fromNative(Tag foreign) {
        if (foreign == null) {
            return null;
        }
        if (foreign instanceof CompoundTag) {
            NBTTagCompound tag = new NBTTagCompound();
            for (Map.Entry<String, Tag> entry : ((CompoundTag) foreign)
                    .getValue().entrySet()) {
                tag.set(entry.getKey(), fromNative(entry.getValue()));
            }
            return tag;
        } else if (foreign instanceof ByteTag) {
            return new NBTTagByte(((ByteTag) foreign).getValue());
        } else if (foreign instanceof ByteArrayTag) {
            return new NBTTagByteArray(((ByteArrayTag) foreign).getValue());
        } else if (foreign instanceof DoubleTag) {
            return new NBTTagDouble(((DoubleTag) foreign).getValue());
        } else if (foreign instanceof FloatTag) {
            return new NBTTagFloat(((FloatTag) foreign).getValue());
        } else if (foreign instanceof IntTag) {
            return new NBTTagInt(((IntTag) foreign).getValue());
        } else if (foreign instanceof IntArrayTag) {
            return new NBTTagIntArray(((IntArrayTag) foreign).getValue());
        } else if (foreign instanceof LongArrayTag) {
            return new NBTTagLongArray(((LongArrayTag) foreign).getValue());
        } else if (foreign instanceof ListTag) {
            NBTTagList tag = new NBTTagList();
            ListTag foreignList = (ListTag) foreign;
            for (Tag t : foreignList.getValue()) {
                tag.add(fromNative(t));
            }
            return tag;
        } else if (foreign instanceof LongTag) {
            return new NBTTagLong(((LongTag) foreign).getValue());
        } else if (foreign instanceof ShortTag) {
            return new NBTTagShort(((ShortTag) foreign).getValue());
        } else if (foreign instanceof StringTag) {
            return new NBTTagString(((StringTag) foreign).getValue());
        } else if (foreign instanceof EndTag) {
            try {
                return (NBTBase) nbtCreateTagMethod.invoke(null, (byte) 0);
            } catch (Exception e) {
                return null;
            }
        } else {
            throw new IllegalArgumentException("Don't know how to make NMS " + foreign.getClass().getCanonicalName());
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
        return BlockTypes.states[adaptToInt(ibd)];
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
        BlockMaterial_1_14 material = (BlockMaterial_1_14) state.getMaterial();
        return material.getCraftBlockData();
    }

    @Override
    public void sendFakeNBT(Player player, BlockVector3 pos, CompoundTag nbtData) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutTileEntityData(
                new BlockPosition(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()),
                7,
                (NBTTagCompound) fromNative(nbtData)
        ));
    }

    @Override
    public void notifyAndLightBlock(Location position, BlockState previousType) {
        this.setBlock(position.getChunk(), position.getBlockX(), position.getBlockY(), position.getBlockZ(), previousType, true);
    }

    @Override
    public void sendFakeOP(Player player) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityStatus(
                ((CraftPlayer) player).getHandle(), (byte) 28
        ));
    }

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
                        nmsPacket = MapChunkUtil_1_14.create(this, packet);
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

    private static EnumDirection adapt(Direction face) {
        switch (face) {
            case NORTH: return EnumDirection.NORTH;
            case SOUTH: return EnumDirection.SOUTH;
            case WEST: return EnumDirection.WEST;
            case EAST: return EnumDirection.EAST;
            case DOWN: return EnumDirection.DOWN;
            case UP:
            default:
                return EnumDirection.UP;
        }
    }

    private LoadingCache<WorldServer, FakePlayer_v1_14_R4> fakePlayers = CacheBuilder.newBuilder().weakKeys().softValues().build(CacheLoader.from(FakePlayer_v1_14_R4::new));

    @Override
    public synchronized boolean simulateItemUse(org.bukkit.World world, BlockVector3 position, BaseItem item, Direction face) {
        CraftWorld craftWorld = (CraftWorld) world;
        WorldServer worldServer = craftWorld.getHandle();
        ItemStack stack = CraftItemStack.asNMSCopy(BukkitAdapter.adapt(item instanceof BaseItemStack
                ? ((BaseItemStack) item) : new BaseItemStack(item.getType(), item.getNbtData(), 1)));
        stack.setTag((NBTTagCompound) fromNative(item.getNbtData()));

        FakePlayer_v1_14_R4 fakePlayer;
        try {
            fakePlayer = fakePlayers.get(worldServer);
        } catch (ExecutionException ignored) {
            return false;
        }
        fakePlayer.a(EnumHand.MAIN_HAND, stack);
        fakePlayer.setLocation(position.getBlockX(), position.getBlockY(), position.getBlockZ(),
                (float) face.toVector().toYaw(), (float) face.toVector().toPitch());

        final BlockPosition blockPos = new BlockPosition(position.getBlockX(), position.getBlockY(), position.getBlockZ());
        final Vec3D blockVec = new Vec3D(blockPos);
        final EnumDirection enumFacing = adapt(face);
        MovingObjectPositionBlock rayTrace = new MovingObjectPositionBlock(blockVec, enumFacing, blockPos, false);
        ItemActionContext context = new ItemActionContext(fakePlayer, EnumHand.MAIN_HAND, rayTrace);
        EnumInteractionResult result = stack.placeItem(context, EnumHand.MAIN_HAND);
        if (result != EnumInteractionResult.SUCCESS) {
            if (worldServer.getType(blockPos).interact(worldServer, fakePlayer, EnumHand.MAIN_HAND, rayTrace)) {
                result = EnumInteractionResult.SUCCESS;
            } else {
                result = stack.getItem().a(worldServer, fakePlayer, EnumHand.MAIN_HAND).a();
            }
        }

        return result == EnumInteractionResult.SUCCESS;
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
}
