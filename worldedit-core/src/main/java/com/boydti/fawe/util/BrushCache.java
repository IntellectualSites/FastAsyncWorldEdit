package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.brush.BrushSettings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class BrushCache {
    private static final WeakHashMap<Object, BrushTool> brushCache = new WeakHashMap<>();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final CompoundTag getNBT(BaseItem item) {
        return item.hasNbtData() ? item.getNbtData() : null;
    }

    private static final Object getKey(BaseItem item) {
        return item.getNativeItem();
    }

    private static final ThreadLocal<Boolean> RECURSION = new ThreadLocal<>();

    public static final BrushTool getTool(Player player, LocalSession session, BaseItem item) {
        if (!item.hasNbtData()) return null;
        Object key = getKey(item);
        if (key == null) return null;
        BrushTool cached = brushCache.get(key);
        if (cached != null) return cached;

        CompoundTag nbt = item.getNbtData();
        if (nbt == null) return null;
        StringTag json = (StringTag) nbt.getValue().get("weBrushJson");
        if (json != null) {
            try {
                if (RECURSION.get() != null) return null;
                RECURSION.set(true);

                BrushTool tool = BrushTool.fromString(player, session, json.getValue());
                tool.setHolder(item);
                brushCache.put(key, tool);
                return tool;
            } catch (Throwable ignore) {
                ignore.printStackTrace();
                Fawe.debug("Invalid brush for " + player + " holding " + item.getType() + ": " + json.getValue());
                if (item != null) {
                    item.setNbtData(null);
                    brushCache.remove(key);
                }
            } finally {
                RECURSION.remove();
            }
        }
        return null;
    }

    public static BrushTool getCachedTool(BaseItem item) {
        Object key = getKey(item);
        if (key != null) return brushCache.get(key);
        return null;
    }

    public static final BrushTool setTool(BaseItem item, BrushTool tool) {
        if (item.getNativeItem() == null) return null;

        CompoundTag nbt = item.getNbtData();
        Map<String, Tag> map;
        if (nbt == null) {
            if (tool == null) {
                return tool;
            }
            nbt = new CompoundTag(map = new HashMap<>());
        } else {
            map = ReflectionUtils.getMap(nbt.getValue());
        }
        brushCache.remove(getKey(item));
        CompoundTag display = (CompoundTag) map.get("display");
        Map<String, Tag> displayMap;
        if (tool != null) {
            String json = tool.toString(gson);
            map.put("weBrushJson", new StringTag(json));
            if (display == null) {
                map.put("display", new CompoundTag(displayMap = new HashMap()));
            } else {
                displayMap = ReflectionUtils.getMap(display.getValue());
            }
            displayMap.put("Lore", FaweCache.asTag(json.split("\\r?\\n")));
            String primary = (String) tool.getPrimary().getSettings().get(BrushSettings.SettingType.BRUSH);
            String secondary = (String) tool.getSecondary().getSettings().get(BrushSettings.SettingType.BRUSH);
            if (primary == null) primary = secondary;
            if (secondary == null) secondary = primary;
            if (primary != null) {
                String name = primary == secondary ? primary.split(" ")[0] : primary.split(" ")[0] + " / " + secondary.split(" ")[0];
                displayMap.put("Name", new StringTag(name));
            }
        } else if (map.containsKey("weBrushJson")) {
            map.remove("weBrushJson");
            if (display != null) {
                displayMap = ReflectionUtils.getMap(display.getValue());
                displayMap.remove("Lore");
                displayMap.remove("Name");
                if (displayMap.isEmpty()) {
                    map.remove("display");
                }
            }

        } else {
            return tool;
        }
        item.setNbtData(nbt);
        if (tool != null) {
            brushCache.put(getKey(item), tool);
        }
        return tool;
    }
}