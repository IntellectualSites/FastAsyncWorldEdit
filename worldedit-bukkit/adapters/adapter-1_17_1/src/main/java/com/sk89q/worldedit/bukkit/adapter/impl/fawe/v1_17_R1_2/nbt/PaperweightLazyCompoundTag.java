package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_17_R1_2.nbt;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.LazyCompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import net.minecraft.nbt.NumericTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PaperweightLazyCompoundTag extends LazyCompoundTag {

    private final Supplier<net.minecraft.nbt.CompoundTag> compoundTagSupplier;
    private CompoundTag compoundTag;

    public PaperweightLazyCompoundTag(Supplier<net.minecraft.nbt.CompoundTag> compoundTagSupplier) {
        super(new HashMap<>());
        this.compoundTagSupplier = compoundTagSupplier;
    }

    public PaperweightLazyCompoundTag(net.minecraft.nbt.CompoundTag compoundTag) {
        this(() -> compoundTag);
    }

    public net.minecraft.nbt.CompoundTag get() {
        return compoundTagSupplier.get();
    }

    @Override
    public Map<String, Tag> getValue() {
        if (compoundTag == null) {
            compoundTag = (CompoundTag) WorldEditPlugin.getInstance().getBukkitImplAdapter().toNative(compoundTagSupplier.get());
        }
        return compoundTag.getValue();
    }

    @Override
    public CompoundBinaryTag asBinaryTag() {
        getValue();
        return compoundTag.asBinaryTag();
    }

    public boolean containsKey(String key) {
        return compoundTagSupplier.get().contains(key);
    }

    public byte[] getByteArray(String key) {
        return compoundTagSupplier.get().getByteArray(key);
    }

    public byte getByte(String key) {
        return compoundTagSupplier.get().getByte(key);
    }

    public double getDouble(String key) {
        return compoundTagSupplier.get().getDouble(key);
    }

    public double asDouble(String key) {
        net.minecraft.nbt.Tag tag = compoundTagSupplier.get().get(key);
        if (tag instanceof NumericTag) {
            return ((NumericTag) tag).getAsDouble();
        }
        return 0;
    }

    public float getFloat(String key) {
        return compoundTagSupplier.get().getFloat(key);
    }

    public int[] getIntArray(String key) {
        return compoundTagSupplier.get().getIntArray(key);
    }

    public int getInt(String key) {
        return compoundTagSupplier.get().getInt(key);
    }

    public int asInt(String key) {
        net.minecraft.nbt.Tag tag = compoundTagSupplier.get().get(key);
        if (tag instanceof NumericTag) {
            return ((NumericTag) tag).getAsInt();
        }
        return 0;
    }

    public List<Tag> getList(String key) {
        net.minecraft.nbt.Tag tag = compoundTagSupplier.get().get(key);
        if (tag instanceof net.minecraft.nbt.ListTag) {
            ArrayList<Tag> list = new ArrayList<>();
            net.minecraft.nbt.ListTag nbtList = (net.minecraft.nbt.ListTag) tag;
            for (net.minecraft.nbt.Tag elem : nbtList) {
                if (elem instanceof net.minecraft.nbt.CompoundTag) {
                    list.add(new PaperweightLazyCompoundTag((net.minecraft.nbt.CompoundTag) elem));
                } else {
                    list.add(WorldEditPlugin.getInstance().getBukkitImplAdapter().toNative(elem));
                }
            }
            return list;
        }
        return Collections.emptyList();
    }

    public ListTag getListTag(String key) {
        net.minecraft.nbt.Tag tag = compoundTagSupplier.get().get(key);
        if (tag instanceof net.minecraft.nbt.ListTag) {
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
        return compoundTagSupplier.get().getLongArray(key);
    }

    public long getLong(String key) {
        return compoundTagSupplier.get().getLong(key);
    }

    public long asLong(String key) {
        net.minecraft.nbt.Tag tag = compoundTagSupplier.get().get(key);
        if (tag instanceof NumericTag) {
            return ((NumericTag) tag).getAsLong();
        }
        return 0;
    }

    public short getShort(String key) {
        return compoundTagSupplier.get().getShort(key);
    }

    public String getString(String key) {
        return compoundTagSupplier.get().getString(key);
    }

    @Override
    public String toString() {
        return compoundTagSupplier.get().toString();
    }

}
