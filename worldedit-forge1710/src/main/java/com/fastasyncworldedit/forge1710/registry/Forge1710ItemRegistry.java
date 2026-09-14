package com.fastasyncworldedit.forge1710.registry;

import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.registry.ItemMaterial;
import com.sk89q.worldedit.world.registry.ItemRegistry;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.item.Item;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Forge1710ItemRegistry implements ItemRegistry {

    private volatile Map<String, Item> itemsById;

    public Map<String, Item> items() {
        Map<String, Item> local = itemsById;
        if (local == null) {
            synchronized (this) {
                local = itemsById;
                if (local == null) {
                    local = new LinkedHashMap<>();
                    for (Object o : GameData.getItemRegistry()) {
                        Item item = (Item) o;
                        String name = GameData.getItemRegistry().getNameForObject(item);
                        if (name == null) {
                            continue;
                        }
                        String id = name.toLowerCase(Locale.ROOT);
                        if (id.indexOf(':') < 0) {
                            id = "minecraft:" + id;
                        }
                        local.putIfAbsent(id, item);
                    }
                    itemsById = local;
                }
            }
        }
        return local;
    }

    @Nullable
    public Item getItem(ItemType type) {
        return items().get(type.id());
    }

    @Override
    public Component getRichName(ItemType itemType) {
        Item item = getItem(itemType);
        if (item == null) {
            return TextComponent.of(itemType.id());
        }
        try {
            return TextComponent.of(item.getItemStackDisplayName(new net.minecraft.item.ItemStack(item)));
        } catch (Throwable t) {
            return TextComponent.of(itemType.id());
        }
    }

    @Nullable
    @Override
    public ItemMaterial getMaterial(ItemType itemType) {
        Item item = getItem(itemType);
        if (item == null) {
            return null;
        }
        return new ItemMaterial() {
            @Override
            public int getMaxStackSize() {
                return item.getItemStackLimit();
            }

            @Override
            public int getMaxDamage() {
                return item.getMaxDamage();
            }
        };
    }

    @Override
    public Collection<String> values() {
        List<String> ids = new ArrayList<>(items().keySet());
        return ids;
    }

}
