//package com.boydti.fawe.bukkit.v0;
//
//import com.boydti.fawe.FaweCache;
//import com.boydti.fawe.bukkit.util.BukkitReflectionUtils;
//import com.boydti.fawe.util.ReflectionUtils;
//import com.sk89q.jnbt.ByteArrayTag;
//import com.sk89q.jnbt.ByteTag;
//import com.sk89q.jnbt.CompoundTag;
//import com.sk89q.jnbt.DoubleTag;
//import com.sk89q.jnbt.EndTag;
//import com.sk89q.jnbt.FloatTag;
//import com.sk89q.jnbt.IntArrayTag;
//import com.sk89q.jnbt.IntTag;
//import com.sk89q.jnbt.ListTag;
//import com.sk89q.jnbt.LongTag;
//import com.sk89q.jnbt.NBTConstants;
//import com.sk89q.jnbt.ShortTag;
//import com.sk89q.jnbt.StringTag;
//import com.sk89q.jnbt.Tag;
//import com.sk89q.worldedit.blocks.BaseBlock;
//import com.sk89q.worldedit.world.block.BlockState;
//import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
//import com.sk89q.worldedit.entity.BaseEntity;
//import com.sk89q.worldedit.internal.Constants;
//import java.lang.reflect.Constructor;
//import java.lang.reflect.Field;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.lang.reflect.Modifier;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import javax.annotation.Nullable;
//import org.bukkit.Location;
//import org.bukkit.Material;
//import org.bukkit.World;
//import org.bukkit.block.Biome;
//import org.bukkit.entity.Entity;
//import org.bukkit.event.entity.CreatureSpawnEvent;
//
//public class FaweAdapter_All implements BukkitImplAdapter {
//
//    private final Class<?> classCraftBlock;
//    private final Method biomeToBiomeBase;
//    private final Class<?> classBiomeBase;
//    private final Method biomeBaseToTypeId;
//    private final Method getBiome;
//    private final Method biomeBaseToBiome;
//    private final Class<?> classCraftWorld;
//    private final Method getHandleWorld;
//    private final Class<?> classWorld;
//    private final Method getTileEntity1;
//    private final Method getTileEntity2;
//    private final Class<?> classNBTTagCompound;
//    private final Constructor<?> newNBTTagCompound;
//    private final Class<?> classTileEntity;
//    private final Class<?> classCraftEntity;
//    private final Method getHandleEntity;
//    private final Class<?> classNBTTagInt;
//    private final Class<?> classNBTBase;
//    private final Constructor<?> newNBTTagInt;
//    private final Method setNBTTagCompound;
//    private Class<?> classEntity;
//    private Method getBukkitEntity;
//    private Method addEntity;
//    private Method setLocation;
//    private Class<?> classEntityTypes;
//    private Method getEntityId;
//    private Method createEntityFromId;
//    private Method readTagIntoEntity;
//    private Method readEntityIntoTag;
//    private Constructor<?> newMinecraftKey;
//    private Class<?> classMinecraftKey;
//    private Method readTagIntoTileEntity;
//    private Method readTileEntityIntoTag;
//    private Class<?> classBlockPosition;
//    private Constructor<?> newBlockPosition;
//
//    private Map<Class<? extends Tag>, NMSTagConstructor> WEToNMS = new ConcurrentHashMap<>();
//    private Map<Class, WETagConstructor> NMSToWE = new ConcurrentHashMap<>();
//    private Map<Class<? extends Tag>, Integer> TagToId = new ConcurrentHashMap<>();
//
//    public FaweAdapter_All() throws Throwable {
//        BukkitReflectionUtils.init();
//        classCraftWorld = BukkitReflectionUtils.getCbClass("CraftWorld");
//        classCraftBlock = BukkitReflectionUtils.getCbClass("block.CraftBlock");
//        classCraftEntity = BukkitReflectionUtils.getCbClass("entity.CraftEntity");
//        classBiomeBase = BukkitReflectionUtils.getNmsClass("BiomeBase");
//        classWorld = BukkitReflectionUtils.getNmsClass("World");
//        classTileEntity = BukkitReflectionUtils.getNmsClass("TileEntity");
//
//        biomeToBiomeBase = ReflectionUtils.setAccessible(classCraftBlock.getDeclaredMethod("biomeToBiomeBase", Biome.class));
//        biomeBaseToBiome = ReflectionUtils.setAccessible(classCraftBlock.getDeclaredMethod("biomeBaseToBiome", classBiomeBase));
//        getBiome = ReflectionUtils.setAccessible(classBiomeBase.getDeclaredMethod("getBiome", int.class));
//        biomeBaseToTypeId = ReflectionUtils.findMethod(classBiomeBase, int.class, classBiomeBase);
//        getHandleWorld = ReflectionUtils.setAccessible(classCraftWorld.getDeclaredMethod("getHandle"));
//        getHandleEntity = ReflectionUtils.setAccessible(classCraftEntity.getDeclaredMethod("getHandle"));
//        try {
//            classBlockPosition = BukkitReflectionUtils.getNmsClass("BlockPosition");
//        } catch (Throwable ignore) {
//        }
//        if (classBlockPosition != null) {
//            getTileEntity1 = classWorld.getDeclaredMethod("getTileEntity", classBlockPosition);
//            getTileEntity2 = null;
//            newBlockPosition = ReflectionUtils.setAccessible(classBlockPosition.getConstructor(int.class, int.class, int.class));
//        } else {
//            getTileEntity1 = null;
//            getTileEntity2 = ReflectionUtils.setAccessible(classWorld.getDeclaredMethod("getTileEntity", int.class, int.class, int.class));
//        }
//
//        classNBTTagCompound = BukkitReflectionUtils.getNmsClass("NBTTagCompound");
//        classNBTBase = BukkitReflectionUtils.getNmsClass("NBTBase");
//        classNBTTagInt = BukkitReflectionUtils.getNmsClass("NBTTagInt");
//        newNBTTagInt = ReflectionUtils.setAccessible(classNBTTagInt.getConstructor(int.class));
//        setNBTTagCompound = ReflectionUtils.setAccessible(classNBTTagCompound.getDeclaredMethod("set", String.class, classNBTBase));
//        newNBTTagCompound = ReflectionUtils.setAccessible(classNBTTagCompound.getConstructor());
//        try {
//            readTileEntityIntoTag = ReflectionUtils.setAccessible(classTileEntity.getDeclaredMethod("save", classNBTTagCompound));
//        } catch (Throwable ignore) {
//            readTileEntityIntoTag = ReflectionUtils.findMethod(classTileEntity, classNBTTagCompound, classNBTTagCompound);
//            if (readTileEntityIntoTag == null) {
//                readTileEntityIntoTag = ReflectionUtils.findMethod(classTileEntity, 1, Void.TYPE, classNBTTagCompound);
//            }
//        }
//
//
//        try {
//            readTagIntoTileEntity = ReflectionUtils.setAccessible(classTileEntity.getDeclaredMethod("load", classNBTTagCompound));
//        } catch (Throwable ignore) {
//            readTagIntoTileEntity = ReflectionUtils.findMethod(classTileEntity, 0, Void.TYPE, classNBTTagCompound);
//        }
//
//
//        List<String> nmsClasses = Arrays.asList("NBTTagCompound", "NBTTagByte", "NBTTagByteArray", "NBTTagDouble", "NBTTagFloat", "NBTTagInt", "NBTTagIntArray", "NBTTagList", "NBTTagEnd", "NBTTagString", "NBTTagShort", "NBTTagLong");
//        List<Class<? extends Tag>> weClasses = Arrays.asList(CompoundTag.class, ByteTag.class, ByteArrayTag.class, DoubleTag.class, FloatTag.class, IntTag.class, IntArrayTag.class, ListTag.class, EndTag.class, StringTag.class, ShortTag.class, LongTag.class);
//        int[] ids = new int[]{10, 1, 7, 6, 5, 3, 11, 9, 0, 8, 2, 4};
//
//        int noMods = Modifier.STATIC;
//        int hasMods = 0;
//        for (int i = 0; i < nmsClasses.size(); i++) {
//            Class<?> nmsClass = BukkitReflectionUtils.getNmsClass(nmsClasses.get(i));
//            Class<? extends Tag> weClass = weClasses.get(i);
//            TagToId.put(weClass, ids[i]);
//
//            Constructor nmsConstructor = ReflectionUtils.setAccessible(nmsClass.getDeclaredConstructor());
//
//            if (weClass == EndTag.class) {
//                NMSToWE.put(nmsClass, value -> new EndTag());
//                WEToNMS.put(weClass, value -> nmsConstructor.newInstance());
//            } else if (weClass == CompoundTag.class) {
//                Field mapField = ReflectionUtils.findField(nmsClass, Map.class, hasMods, noMods);
//                Constructor<? extends Tag> weConstructor = ReflectionUtils.setAccessible(CompoundTag.class.getConstructor(Map.class));
//
//                NMSToWE.put(nmsClass, value -> {
//                    Map<String, Object> map = (Map) mapField.get(value);
//                    Map<String, Tag> weMap = new HashMap<String, Tag>();
//                    for (Map.Entry<String, Object> entry : map.entrySet()) {
//                        weMap.put(entry.getKey(), toNative(entry.getValue()));
//                    }
//                    return new CompoundTag(weMap);
//                });
//
//                WEToNMS.put(weClass, value -> {
//                    Map<String, Tag> map = ReflectionUtils.getMap(((CompoundTag) value).getValue());
//                    Object nmsTag = nmsConstructor.newInstance();
//                    Map<String, Object> nmsMap = (Map<String, Object>) mapField.get(nmsTag);
//                    for (Map.Entry<String, Tag> entry : map.entrySet()) {
//                        nmsMap.put(entry.getKey(), fromNative(entry.getValue()));
//                    }
//                    return nmsTag;
//                });
//            } else if (weClass == ListTag.class) {
//                Field listField = ReflectionUtils.findField(nmsClass, List.class, hasMods, noMods);
//                Field typeField = ReflectionUtils.findField(nmsClass, byte.class, hasMods, noMods);
//                Constructor<? extends Tag> weConstructor = ReflectionUtils.setAccessible(ListTag.class.getConstructor(Class.class, List.class));
//
//                NMSToWE.put(nmsClass, tag -> {
//                    int type = ((Number) typeField.get(tag)).intValue();
//                    List list = (List) listField.get(tag);
//
//                    Class<? extends Tag> weType = NBTConstants.getClassFromType(type);
//                    ArrayList<Tag> weList = new ArrayList<>();
//                    for (Object nmsTag : list) {
//                        weList.add(toNative(nmsTag));
//                    }
//                    return new ListTag(weType, weList);
//                });
//                WEToNMS.put(weClass, tag -> {
//                    ListTag lt = (ListTag) tag;
//                    List<Tag> list = ReflectionUtils.getList(lt.getValue());
//                    Class<? extends Tag> type = lt.getType();
//
//                    int typeId = TagToId.get(type);
//                    Object nmsTagList = nmsConstructor.newInstance();
//                    typeField.set(nmsTagList, (byte) typeId);
//                    ArrayList<Object> nmsList = (ArrayList<Object>) listField.get(nmsTagList);
//                    for (Tag weTag : list) {
//                        nmsList.add(fromNative(weTag));
//                    }
//                    return nmsTagList;
//                });
//            } else {
//                Field typeField = ReflectionUtils.findField(nmsClass, null, hasMods, noMods);
//                Constructor<? extends Tag> weConstructor = ReflectionUtils.setAccessible(weClass.getConstructor(typeField.getType()));
//
//                NMSToWE.put(nmsClass, tag -> {
//                    Object value = typeField.get(tag);
//                    return weConstructor.newInstance(value);
//                });
//
//                WEToNMS.put(weClass, tag -> {
//                    Object nmsTag = nmsConstructor.newInstance();
//                    typeField.set(nmsTag, tag.getValue());
//                    return nmsTag;
//                });
//            }
//        }
//        try {
//            classEntity = BukkitReflectionUtils.getNmsClass("Entity");
//            classEntityTypes = BukkitReflectionUtils.getNmsClass("EntityTypes");
//
//            getBukkitEntity = ReflectionUtils.setAccessible(classEntity.getDeclaredMethod("getBukkitEntity"));
//            addEntity = ReflectionUtils.setAccessible(classWorld.getDeclaredMethod("addEntity", classEntity, CreatureSpawnEvent.SpawnReason.class));
//            setLocation = ReflectionUtils.setAccessible(classEntity.getDeclaredMethod("setLocation", double.class, double.class, double.class, float.class, float.class));
//
//            try {
//                classMinecraftKey = BukkitReflectionUtils.getNmsClass("MinecraftKey");
//                newMinecraftKey = classMinecraftKey.getConstructor(String.class);
//            } catch (Throwable ignore) {
//            }
//            if (classMinecraftKey != null) {
//                getEntityId = ReflectionUtils.findMethod(classEntityTypes, classMinecraftKey, classEntity);
//                createEntityFromId = ReflectionUtils.findMethod(classEntityTypes, classEntity, classMinecraftKey, classWorld);
//            } else {
//                getEntityId = ReflectionUtils.findMethod(classEntityTypes, String.class, classEntity);
//                createEntityFromId = ReflectionUtils.findMethod(classEntityTypes, classEntity, String.class, classWorld);
//            }
//
//            noMods = Modifier.ABSTRACT | Modifier.PROTECTED | Modifier.PRIVATE;
//            try {
//                readEntityIntoTag = classEntity.getDeclaredMethod("save", classNBTTagCompound);
//            } catch (Throwable ignore) {
//                readEntityIntoTag = ReflectionUtils.findMethod(classEntity, classNBTTagCompound, classNBTTagCompound);
//                if (readEntityIntoTag == null) {
//                    readEntityIntoTag = ReflectionUtils.findMethod(classEntity, 0, 0, noMods, Void.TYPE, classNBTTagCompound);
//                }
//            }
//            ReflectionUtils.setAccessible(readEntityIntoTag);
//            readTagIntoEntity = ReflectionUtils.findMethod(classEntity, 1, 0, noMods, Void.TYPE, classNBTTagCompound);
//            if (readTagIntoEntity == null) {
//                readTagIntoEntity = ReflectionUtils.findMethod(classEntity, 0, 0, noMods, Void.TYPE, classNBTTagCompound);
//            }
//        } catch (Throwable e) {
//            e.printStackTrace();
//            classEntity = null;
//        }
//    }
//
//    @Nullable
//    @Override
//    public BaseEntity getEntity(Entity entity) {
//        try {
//            if (classEntity == null) return null;
//            Object nmsEntity = getHandleEntity.invoke(entity);
//
//            String id = getEntityId(nmsEntity);
//
//            if (id != null) {
//                Object tag = newNBTTagCompound.newInstance();
//                readEntityIntoTag.invoke(nmsEntity, tag);
//                return new BaseEntity(id, (CompoundTag) toNative(tag));
//            }
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
//        return null;
//    }
//
//    private String getEntityId(Object entity) throws InvocationTargetException, IllegalAccessException {
//        Object res = getEntityId.invoke(null, entity);
//        return res == null ? null : res.toString();
//    }
//
//    private Object createEntityFromId(String id, Object world) throws InvocationTargetException, IllegalAccessException, InstantiationException {
//        if (classMinecraftKey != null) {
//            Object key = newMinecraftKey.newInstance(id);
//            return createEntityFromId.invoke(null, key, world);
//        } else {
//            return createEntityFromId.invoke(null, id, world);
//        }
//    }
//
//    @Nullable
//    @Override
//    public Entity createEntity(Location location, BaseEntity state) {
//        try {
//            if (classEntity == null) return null;
//            World world = location.getWorld();
//            Object nmsWorld = getHandleWorld.invoke(world);
//
//            Object createdEntity = createEntityFromId(state.getTypeId(), nmsWorld);
//
//            if (createdEntity != null) {
//                CompoundTag nativeTag = state.getNbtData();
//                Map<String, Tag> rawMap = ReflectionUtils.getMap(nativeTag.getValue());
//                for (String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
//                    rawMap.remove(name);
//                }
//                if (nativeTag != null) {
//                    Object tag = fromNative(nativeTag);
//                    readTagIntoEntity.invoke(createdEntity, tag);
//                }
//
//                setLocation.invoke(createdEntity, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
//
//                addEntity.invoke(nmsWorld, createdEntity, CreatureSpawnEvent.SpawnReason.CUSTOM);
//                return (Entity) getBukkitEntity.invoke(createdEntity);
//            }
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
//        return null;
//    }
//
//    public Tag toNative(Object nmsTag) {
//        try {
//            return NMSToWE.get(nmsTag.getClass()).construct(nmsTag);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public Object fromNative(Tag tag) {
//        try {
//            return WEToNMS.get(tag.getClass()).construct(tag);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Override
//    public int getBlockId(Material material) {
//        return material.getId();
//    }
//
//    @Override
//    public Material getMaterial(int id) {
//        return Material.getMaterial(id);
//    }
//
//    @Override
//    public int getBiomeId(Biome biome) {
//        try {
//            Object biomeBase = biomeToBiomeBase.invoke(null, biome);
//            if (biomeBase != null) return (int) biomeBaseToTypeId.invoke(null, biomeBase);
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
//        return 0;
//    }
//
//    @Override
//    public Biome getBiome(int id) {
//        try {
//            Object biomeBase = getBiome.invoke(null, id);
//            if (biomeBase != null) return (Biome) biomeBaseToBiome.invoke(null, biomeBase);
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
//        return Biome.OCEAN;
//    }
//
//    @Override
//    public BaseBlock getBlock(Location location) {
//        try {
//            World craftWorld = location.getWorld();
//            int x = location.getBlockX();
//            int y = location.getBlockY();
//            int z = location.getBlockZ();
//
//            org.bukkit.block.Block bukkitBlock = location.getBlock();
//            BaseBlock block = FaweCache.getBlock(bukkitBlock.getTypeId(), bukkitBlock.getData());
//
//            // Read the NBT data
//            Object nmsWorld = getHandleWorld.invoke(craftWorld);
//            Object tileEntity = getTileEntity(nmsWorld, x, y, z);
//
//            if (tileEntity != null) {
//                block = new BaseBlock(block);
//                Object tag = newNBTTagCompound.newInstance();
//                readTileEntityIntoTag.invoke(tileEntity, tag);
//                block.setNbtData((CompoundTag) toNative(tag));
//            }
//            return block;
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public Object getTileEntity(Object nmsWorld, int x, int y, int z) {
//        try {
//            if (getTileEntity1 != null) {
//                Object pos = newBlockPosition.newInstance(x, y, z);
//                return getTileEntity1.invoke(nmsWorld, pos);
//            } else {
//                return getTileEntity2.invoke(nmsWorld, x, y, z);
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Override
//    public boolean setBlock(Location location, BaseBlock block, boolean notifyAndLight) {
//        World craftWorld = location.getWorld();
//        int x = location.getBlockX();
//        int y = location.getBlockY();
//        int z = location.getBlockZ();
//
//        boolean changed = location.getBlock().setTypeIdAndData(block.getId(), (byte) block.getData(), notifyAndLight);
//
//        CompoundTag nativeTag = block.getNbtData();
//        if (nativeTag != null) {
//            try {
//                Object nmsWorld = getHandleWorld.invoke(craftWorld);
//                Object tileEntity = getTileEntity(nmsWorld, x, y, z);
//                if (tileEntity != null) {
//                    Object tag = fromNative(nativeTag);
//
//                    setNBTTagCompound.invoke(tag, "x", newNBTTagInt.newInstance(x));
//                    setNBTTagCompound.invoke(tag, "y", newNBTTagInt.newInstance(y));
//                    setNBTTagCompound.invoke(tag, "z", newNBTTagInt.newInstance(z));
//                    readTagIntoTileEntity.invoke(tileEntity, tag); // Load data
//                }
//            } catch (Throwable e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        return changed;
//    }
//
//    private interface NMSTagConstructor {
//        Object construct(Tag value) throws Exception;
//    }
//
//    private interface WETagConstructor {
//        Tag construct(Object value) throws Exception;
//    }
//}
