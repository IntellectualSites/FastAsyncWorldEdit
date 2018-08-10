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

package com.boydti.fawe.bukkit.adapter.v1_13_1;

import com.boydti.fawe.Fawe;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.sk89q.jnbt.*;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.blocks.TileEntityBlock;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.bukkit.adapter.CachedBukkitAdapter;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.*;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.registry.BlockMaterial;

import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_13_R2.CraftChunk;
import org.bukkit.craftbukkit.v1_13_R2.CraftServer;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Spigot_v1_13_R2 extends CachedBukkitAdapter implements BukkitImplAdapter<NBTBase>{

    private final Logger logger = Logger.getLogger(getClass().getCanonicalName());

    private final Field nbtListTagListField;
    private final Method nbtCreateTagMethod;
    private Method chunkSetTypeMethod;

    static {
        // A simple test
        if (!Bukkit.getServer().getClass().getName().endsWith("DummyServer")) CraftServer.class.cast(Bukkit.getServer());
    }

    // ------------------------------------------------------------------------
    // Code that may break between versions of Minecraft
    // ------------------------------------------------------------------------

    public Spigot_v1_13_R2() throws NoSuchFieldException, NoSuchMethodException {
        // The list of tags on an NBTTagList
        nbtListTagListField = NBTTagList.class.getDeclaredField("list");
        nbtListTagListField.setAccessible(true);

        // The method to create an NBTBase tag given its type ID
        nbtCreateTagMethod = NBTBase.class.getDeclaredMethod("createTag", byte.class);
        nbtCreateTagMethod.setAccessible(true);
        
        // 1.13.2 Adaptation to find the a/setType method
        try {
        	chunkSetTypeMethod = Chunk.class.getMethod("setType", BlockPosition.class, IBlockData.class, boolean.class);
        }catch(NoSuchMethodException e) {
        	chunkSetTypeMethod = Chunk.class.getMethod("a", BlockPosition.class, IBlockData.class, boolean.class);
        }
    }

    private int[] idbToStateOrdinal;

    private boolean init() {
        if (idbToStateOrdinal != null) return false;
        idbToStateOrdinal = new int[Block.REGISTRY_ID.a()]; // size
        for (int i = 0; i < idbToStateOrdinal.length; i++) {
            BlockState state = BlockTypes.states[i];
            BlockMaterial_1_13 material = (BlockMaterial_1_13) state.getMaterial();
            int id = Block.REGISTRY_ID.getId(material.getState());
            idbToStateOrdinal[id] = state.getOrdinal();
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
        MinecraftKey minecraftkey = EntityTypes.getName(entity.getBukkitEntity().getHandle().P());
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
        return EntityTypes.a(world, new MinecraftKey(id));
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
        return new BlockMaterial_1_13(getBlock(blockType));
    }

    @Override
    public BlockMaterial getMaterial(BlockState state) {
        BlockTypes type = state.getBlockType();
        IBlockData bs = ((CraftBlockData) Bukkit.createBlockData(state.getAsString())).getState();
        return new BlockMaterial_1_13(bs.getBlock(), bs);
    }

    public Block getBlock(BlockType blockType) {
        return IRegistry.BLOCK.getOrDefault(new MinecraftKey(blockType.getNamespace(), blockType.getResource()));
    }

    // ------------------------------------------------------------------------
    // Code that is less likely to break
    // ------------------------------------------------------------------------

    @Override
    public int getBiomeId(Biome biome) {
        BiomeBase mcBiome = CraftBlock.biomeToBiomeBase(biome);
        return mcBiome != null ? IRegistry.BIOME.a(mcBiome) : 0;
    }

    @Override
    public Biome getBiome(int id) {
        BiomeBase mcBiome = IRegistry.BIOME.fromId(id);
        return CraftBlock.biomeBaseToBiome(mcBiome); // Defaults to ocean if it's an invalid ID
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState getBlock(Location location) {
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
                return new BaseBlock(state, (CompoundTag) toNative(tag));
            }
        }

        return state;
    }

    @Override
    public boolean isChunkInUse(org.bukkit.Chunk chunk) {
        CraftChunk craftChunk = (CraftChunk) chunk;
        PlayerChunkMap chunkMap = ((WorldServer) craftChunk.getHandle().getWorld()).getPlayerChunkMap();
        return chunkMap.isChunkInUse(chunk.getX(), chunk.getZ());
    }

    @Override
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
        BlockPosition pos = null;
        CompoundTag nativeTag = state.getNbtData();
        if (nativeTag != null || existing instanceof TileEntityBlock) {
            pos = new BlockPosition(x, y, z);
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
                sections[y4] = section = new ChunkSection(y4 << 4, nmsWorld.worldProvider.g());
            }
            if (existing.e() != blockData.e() || existing.getMaterial().f() != blockData.getMaterial().f()) {
            	try {
					chunkSetTypeMethod.invoke(nmsChunk, pos = new BlockPosition(x, y, z), blockData, false);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					logger.warning("Error when setting block!");
					e.printStackTrace();
				}
            } else {
                section.setType(x & 15, y & 15, z & 15, blockData);
            }
        }
        if (update) {
            if (pos == null) pos = new BlockPosition(x, y, z);
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
            NBTTagCompound tag = new NBTTagCompound();
            readEntityIntoTag(mcEntity, tag);
            return new BaseEntity(com.sk89q.worldedit.world.entity.EntityTypes.get(id), (CompoundTag) toNative(tag));
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public org.bukkit.entity.Entity createEntity(Location location, BaseEntity state) {
        checkNotNull(location);
        checkNotNull(state);

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
    public Map<String, ? extends Property> getProperties(BlockType blockType) {
        Block block;
        try {
            block = IRegistry.BLOCK.getOrDefault(new MinecraftKey(blockType.getNamespace(), blockType.getResource()));
        } catch (Throwable e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
        if (block == null) {
            logger.warning("Failed to find properties for " + blockType.getId());
            return Collections.emptyMap();
        }
        Map<String, Property> properties = Maps.newLinkedHashMap();
        BlockStateList<Block, IBlockData> blockStateList = block.getStates();
        for (IBlockState state : blockStateList.d()) {
            Property property;
            if (state instanceof BlockStateBoolean) {
                property = new BooleanProperty(state.a(), ImmutableList.copyOf(state.d()));
            } else if (state instanceof BlockStateDirection) {
                property = new DirectionalProperty(state.a(),
                        (List<Direction>) state.d().stream().map(e -> Direction.valueOf(((INamable) e).getName().toUpperCase())).collect(Collectors.toList()));
            } else if (state instanceof BlockStateEnum) {
                property = new EnumProperty(state.a(),
                        (List<String>) state.d().stream().map(e -> ((INamable) e).getName()).collect(Collectors.toList()));
            } else if (state instanceof BlockStateInteger) {
                property = new IntegerProperty(state.a(), ImmutableList.copyOf(state.d()));
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
            return new ByteTag(((NBTTagByte) foreign).asByte()); // getByte
        } else if (foreign instanceof NBTTagByteArray) {
            return new ByteArrayTag(((NBTTagByteArray) foreign).c()); // data
        } else if (foreign instanceof NBTTagDouble) {
            return new DoubleTag(((NBTTagDouble) foreign).asDouble()); // getDouble
        } else if (foreign instanceof NBTTagFloat) {
            return new FloatTag(((NBTTagFloat) foreign).asByte()); // getFloat
        } else if (foreign instanceof NBTTagInt) {
            return new IntTag(((NBTTagInt) foreign).asInt()); // getInt
        } else if (foreign instanceof NBTTagIntArray) {
            return new IntArrayTag(((NBTTagIntArray) foreign).d()); // data
        } else if (foreign instanceof NBTTagList) {
            try {
                return toNativeList((NBTTagList) foreign);
            } catch (Throwable e) {
                logger.log(Level.WARNING, "Failed to convert NBTTagList", e);
                return new ListTag(ByteTag.class, new ArrayList<ByteTag>());
            }
        } else if (foreign instanceof NBTTagLong) {
            return new LongTag(((NBTTagLong) foreign).asLong()); // getLong
        } else if (foreign instanceof NBTTagShort) {
            return new ShortTag(((NBTTagShort) foreign).asShort()); // getShort
        } else if (foreign instanceof NBTTagString) {
            return new StringTag(foreign.asString()); // data
        } else if (foreign instanceof NBTTagEnd) {
            return EndTag.INSTANCE;
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
        } else if (foreign instanceof ListTag) {
            NBTTagList tag = new NBTTagList();
            ListTag<?> foreignList = (ListTag) foreign;
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
    public BlockState adapt(BlockData blockData) {
        try {
            CraftBlockData cbd = ((CraftBlockData) blockData);
            IBlockData ibd = cbd.getState();
            int id = Block.REGISTRY_ID.getId(ibd);
            return BlockTypes.states[idbToStateOrdinal[id]];
        } catch (NullPointerException e) {
            if (init()) return adapt(blockData);
            throw e;
        }
    }

    @Override
    public BlockData adapt(BlockStateHolder state) {
        BlockMaterial_1_13 material = (BlockMaterial_1_13) state.getMaterial();
        return material.getCraftBlockData();
    }

	@Override
	public void sendFakeNBT(Player player, BlockVector3 pos, CompoundTag nbtData) {
		// TODO Auto-generated method stub
		
	}
}