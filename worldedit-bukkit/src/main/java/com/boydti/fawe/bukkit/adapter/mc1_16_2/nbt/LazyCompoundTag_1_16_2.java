package com.boydti.fawe.bukkit.adapter.mc1_16_2.nbt;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import net.minecraft.server.v1_16_R2.NBTBase;
import net.minecraft.server.v1_16_R2.NBTNumber;
import net.minecraft.server.v1_16_R2.NBTTagCompound;
import net.minecraft.server.v1_16_R2.NBTTagList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class LazyCompoundTag_1_16_2 extends CompoundTag {

    private final Supplier<NBTTagCompound> nmsTag;
    private CompoundTag cachedValue;

    public LazyCompoundTag_1_16_2(Supplier<NBTTagCompound> tag) {
        super(new HashMap<>());
        this.nmsTag = tag;
    }

    public LazyCompoundTag_1_16_2(NBTTagCompound tag) {
        this(() -> tag);
    }

    public NBTTagCompound get() {
        return nmsTag.get();
    }

    @Override
    public Map<String, Tag> getValue() {
        if (cachedValue == null) {
            cachedValue = (CompoundTag) WorldEditPlugin.getInstance().getBukkitImplAdapter().toNative(nmsTag.get());
        }
        return cachedValue.getValue();
    }

    public boolean containsKey(String key) {
        return nmsTag.get().hasKey(key);
    }

    public byte[] getByteArray(String key) {
        return nmsTag.get().getByteArray(key);
    }

    public byte getByte(String key) {
        return nmsTag.get().getByte(key);
    }

    public double getDouble(String key) {
        return nmsTag.get().getDouble(key);
    }

    public double asDouble(String key) {
        NBTBase value = nmsTag.get().get(key);
        if (value instanceof NBTNumber) {
            return ((NBTNumber) value).asDouble();
        }
        return 0;
    }

    public float getFloat(String key) {
        return nmsTag.get().getFloat(key);
    }

    public int[] getIntArray(String key) {
        return nmsTag.get().getIntArray(key);
    }

    public int getInt(String key) {
        return nmsTag.get().getInt(key);
    }

    public int asInt(String key) {
        NBTBase value = nmsTag.get().get(key);
        if (value instanceof NBTNumber) {
            return ((NBTNumber) value).asInt();
        }
        return 0;
    }

    public List<Tag> getList(String key) {
        NBTBase tag = nmsTag.get().get(key);
        if (tag instanceof NBTTagList) {
            ArrayList<Tag> list = new ArrayList<>();
            NBTTagList nbtList = (NBTTagList) tag;
            for (NBTBase elem : nbtList) {
                if (elem instanceof NBTTagCompound) {
                    list.add(new LazyCompoundTag_1_16_2((NBTTagCompound) elem));
                } else {
                    list.add(WorldEditPlugin.getInstance().getBukkitImplAdapter().toNative(elem));
                }
            }
            return list;
        }
        return Collections.emptyList();
    }

    public ListTag getListTag(String key) {
        NBTBase tag = nmsTag.get().get(key);
        if (tag instanceof NBTTagList) {
            return (ListTag) WorldEditPlugin.getInstance().getBukkitImplAdapter().toNative(tag);
        }
        return new ListTag(StringTag.class, Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    public <T extends Tag> List<T> getList(String key, Class<T> listType) {
        ListTag listTag = getListTag(key);
        if (listTag.getType().equals(listType)) {
            return (List<T>) listTag.getValue();
        } else {
            return Collections.emptyList();
        }
    }

    public long[] getLongArray(String key) {
        return nmsTag.get().getLongArray(key);
    }

    public long getLong(String key) {
        return nmsTag.get().getLong(key);
    }

    public long asLong(String key) {
        NBTBase value = nmsTag.get().get(key);
        if (value instanceof NBTNumber) {
            return ((NBTNumber) value).asLong();
        }
        return 0;
    }

    public short getShort(String key) {
        return nmsTag.get().getShort(key);
    }

    public String getString(String key) {
        return nmsTag.get().getString(key);
    }

    @Override
    public String toString() {
        return nmsTag.get().toString();
    }
}
