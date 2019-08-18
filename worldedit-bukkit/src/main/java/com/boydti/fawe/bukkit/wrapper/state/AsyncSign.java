package com.boydti.fawe.bukkit.wrapper.state;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.boydti.fawe.bukkit.wrapper.AsyncBlockState;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.serializer.gson.GsonComponentSerializer;
import com.sk89q.worldedit.util.formatting.text.serializer.legacy.LegacyComponentSerializer;
import java.util.Map;

import com.sk89q.worldedit.world.block.BaseBlock;
import org.bukkit.DyeColor;
import org.bukkit.block.Sign;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AsyncSign extends AsyncBlockState implements Sign {
    public AsyncSign(AsyncBlock block, BaseBlock state) {
        super(block, state);
    }

    private boolean isEditable = false;

    @Override
    public String[] getLines() {
        CompoundTag nbt = getNbtData();
        String[] data = new String[4];
        if (nbt != null) {
            for (int i = 1; i <= 4; i++) {
                data[i - 1] = fromJson(nbt.getString("Text" + i));
            }
        }
        return data;
    }

    private String fromJson(String jsonInput) {
        if (jsonInput == null || jsonInput.isEmpty()) return "";
        return GsonComponentSerializer.INSTANCE.deserialize(jsonInput).toString();
    }

    private String toJson(String oldInput) {
        if (oldInput == null || oldInput.isEmpty()) return "";
        return LegacyComponentSerializer.INSTANCE.serialize(TextComponent.of(oldInput));
    }

    @Override
    public String getLine(int index) throws IndexOutOfBoundsException {
        CompoundTag nbt = getNbtData();
        return nbt == null ? "" : fromJson(nbt.getString("Text" + (index + 1)));
    }

    @Override
    public void setLine(int index, String line) throws IndexOutOfBoundsException {
        CompoundTag nbt = getNbtData();
        if (nbt != null) {
            Map<String, Tag> map = ReflectionUtils.getMap(nbt.getValue());
            map.put("Text" + (index + 1), new StringTag(toJson(line)));
        }
    }

    @Override
    public boolean isEditable() {
        return this.isEditable;
    }

    @Override
    public void setEditable(boolean arg0) {
        this.isEditable = arg0;
    }

    @Override
    public @NotNull PersistentDataContainer getPersistentDataContainer() {
        return new AsyncDataContainer(getNbtData());
    }

    @Override
    public @Nullable DyeColor getColor() {
        CompoundTag nbt = getNbtData();
        if (nbt != null) {
            String color = nbt.getString("Color").toUpperCase();
            if (!color.isEmpty()) return DyeColor.valueOf(color);
        }
        return DyeColor.BLACK;
    }

    @Override
    public void setColor(DyeColor color) {
        CompoundTag nbt = getNbtData();
        if (nbt != null) {
            Map<String, Tag> map = ReflectionUtils.getMap(nbt.getValue());
            map.put("Color", new StringTag(color.name().toLowerCase()));
        }
    }
}
