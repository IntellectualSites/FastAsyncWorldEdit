package com.boydti.fawe.bukkit.util;

import com.boydti.fawe.bukkit.beta.BukkitQueue;
import com.boydti.fawe.util.ReflectionUtils;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.inventory.ItemStack;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static com.google.gson.internal.$Gson$Preconditions.checkNotNull;

public class ItemUtil {

    private final Method methodAsNMSCopy;
    private final Method methodGetTag;
    private final Method methodHasTag;
    private final Method methodSetTag;
    private final Method methodAsBukkitCopy;
    private final Field fieldHandle;
    private final BukkitImplAdapter adapter;

    private SoftReference<Int2ObjectOpenHashMap<WeakReference<Tag>>> hashToNMSTag = new SoftReference(new Int2ObjectOpenHashMap<>());

    public ItemUtil() throws Exception {
        this.adapter = WorldEditPlugin.getInstance().getBukkitImplAdapter();
        checkNotNull(adapter);
        Class<?> classCraftItemStack = BukkitReflectionUtils.getCbClass("inventory.CraftItemStack");
        Class<?> classNMSItem = BukkitReflectionUtils.getNmsClass("ItemStack");
        this.methodAsNMSCopy = ReflectionUtils.setAccessible(classCraftItemStack.getDeclaredMethod("asNMSCopy", ItemStack.class));
        this.methodHasTag = ReflectionUtils.setAccessible(classNMSItem.getDeclaredMethod("hasTag"));
        this.methodGetTag = ReflectionUtils.setAccessible(classNMSItem.getDeclaredMethod("getTag"));
        this.fieldHandle = ReflectionUtils.setAccessible(classCraftItemStack.getDeclaredField("handle"));
        Class<?> classNBTTagCompound = BukkitReflectionUtils.getNmsClass("NBTTagCompound");
        this.methodSetTag = ReflectionUtils.setAccessible(classNMSItem.getDeclaredMethod("setTag", classNBTTagCompound));
        this.methodAsBukkitCopy = ReflectionUtils.setAccessible(classCraftItemStack.getDeclaredMethod("asBukkitCopy", classNMSItem));
    }

    public Object getNMSItem(ItemStack item) {
        try {
            Object nmsItem = fieldHandle.get(item);
            if (nmsItem == null) nmsItem = methodAsNMSCopy.invoke(null, item);
            return nmsItem;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }



    public CompoundTag getNBT(ItemStack item) {
        try {
            if (!item.hasItemMeta()) return null;
            Object nmsItem = fieldHandle.get(item);
            if (nmsItem == null) nmsItem = methodAsNMSCopy.invoke(null, item);
            if (methodHasTag.invoke(nmsItem).equals(true)) {
                Object nmsTag = methodGetTag.invoke(nmsItem);
                if (nmsTag == null) return null;

                Int2ObjectOpenHashMap<WeakReference<Tag>> map = hashToNMSTag.get();
                if (map == null) {
                    map = new Int2ObjectOpenHashMap<>();
                    hashToNMSTag = new SoftReference<>(new Int2ObjectOpenHashMap<>(map));
                }
                WeakReference<Tag> nativeTagRef = map.get(nmsTag.hashCode());
                if (nativeTagRef != null) {
                    Tag nativeTag = nativeTagRef.get();
                    if (nativeTag != null) return (CompoundTag) nativeTag;
                }
                Tag nativeTag = adapter.toNative(nmsTag);
                map.put(nmsTag.hashCode(), new WeakReference<>(nativeTag));
                return null;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public ItemStack setNBT(ItemStack item, CompoundTag tag) {
        try {
            Object nmsItem = fieldHandle.get(item);
            boolean copy = false;
            if (nmsItem == null) {
                copy = true;
                nmsItem = methodAsNMSCopy.invoke(null, item);
            }
            Object nmsTag = adapter.fromNative(tag);
            methodSetTag.invoke(nmsItem, nmsTag);
            if (copy) return (ItemStack) methodAsBukkitCopy.invoke(null, nmsItem);
            return item;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}
