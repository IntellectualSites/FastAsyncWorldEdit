package com.fastasyncworldedit.nukkit.mapping;

import cn.nukkit.item.Item;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplAdapter;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplLoader;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.fastasyncworldedit.nukkit.WorldEditNukkitPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Maps JE item identifiers to Nukkit item IDs and vice versa.
 */
public final class ItemMapping {

    private static final Gson GSON = new Gson();

    private static final Map<String, NukkitItemData> JE_TO_BE = new HashMap<>();
    private static final Map<String, String> BE_TO_JE = new HashMap<>();
    // All known JE item IDs from items.json (loaded early for ItemTypesCache)
    private static final Set<String> JE_ITEM_IDS = new HashSet<>();

    private ItemMapping() {
    }

    /**
     * Pre-load JE item IDs from items.json.
     * Must be called before WorldEdit's loadMappings() to provide item data for ItemTypesCache.
     */
    public static void initJeItemIds() {
        if (!JE_ITEM_IDS.isEmpty()) {
            return;
        }
        try (InputStream stream = ItemMapping.class.getClassLoader().getResourceAsStream("mapping/items.json")) {
            if (stream == null) {
                throw new RuntimeException("items.json not found");
            }

            Map<String, ItemEntry> mappings = GSON.fromJson(
                    new JsonReader(new InputStreamReader(Objects.requireNonNull(stream))),
                    new TypeToken<Map<String, ItemEntry>>() {
                    }.getType()
            );

            JE_ITEM_IDS.addAll(mappings.keySet());
            WorldEditNukkitPlugin.getInstance().getLogger().info("Loaded " + JE_ITEM_IDS.size() + " JE item IDs");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load item IDs", e);
        }
    }

    /**
     * Get all known JE item IDs.
     * Must be called after {@link #initJeItemIds()}.
     */
    public static Collection<String> getAllJeItemIds() {
        return JE_ITEM_IDS;
    }

    public static void init() {
        try (InputStream stream = ItemMapping.class.getClassLoader().getResourceAsStream("mapping/items.json")) {
            if (stream == null) {
                throw new RuntimeException("items.json not found");
            }

            Map<String, ItemEntry> mappings = GSON.fromJson(
                    new JsonReader(new InputStreamReader(Objects.requireNonNull(stream))),
                    new TypeToken<Map<String, ItemEntry>>() {
                    }.getType()
            );

            NukkitImplAdapter adapter = NukkitImplLoader.get();
            mappings.forEach((javaId, entry) -> {
                NukkitItemData data = adapter.createItemData(entry.bedrockId(), entry.bedrockData());
                if (data != null) {
                    JE_TO_BE.put(javaId, data);
                    BE_TO_JE.putIfAbsent(adapter.getItemMappingKey(data), javaId);
                }
            });

            WorldEditNukkitPlugin.getInstance().getLogger().info("Loaded " + JE_TO_BE.size() + " item mappings");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load item mapping", e);
        }
    }

    /**
     * Convert JE item ID (e.g., "minecraft:stone") to Nukkit item data.
     */
    public static NukkitItemData jeToBe(String jeItemId) {
        NukkitItemData result = JE_TO_BE.get(jeItemId);
        if (result == null) {
            throw new UnsupportedOperationException("No Nukkit item mapping for Java item: " + jeItemId);
        }
        return result;
    }

    /**
     * Convert Nukkit item ID + metadata to JE item ID.
     */
    public static String beToJe(int beItemId, int metadata) {
        // Try exact match first (id + metadata)
        String result = BE_TO_JE.get(beKey(beItemId, metadata));
        if (result != null) {
            return result;
        }
        // Fallback: try metadata 0
        result = BE_TO_JE.get(beKey(beItemId, 0));
        if (result != null) {
            return result;
        }
        throw new UnsupportedOperationException(
                "No Java item mapping for Nukkit item: " + beItemId + ":" + metadata
        );
    }

    /**
     * Convert a live Nukkit item to JE item ID.
     */
    public static String beToJe(Item item) {
        NukkitImplAdapter adapter = NukkitImplLoader.get();
        // Air is an identical JE/BE constant and never enters BE_TO_JE (createItemData
        // intentionally rejects null items, and air is a null item on every fork).
        // Short-circuit it instead of routing through the lookup table; otherwise holding
        // nothing (or any item that decays to air) throws on every hotbar switch.
        if (adapter.isAirItem(item)) {
            return "minecraft:air";
        }
        // Compute the key defensively: a live item's key lookup can blow up on some forks
        // (e.g. MOT getNamespaceId over runtime-unregistered items). Such items are unmapped,
        // so degrade to a stable id:meta key for the diagnostic rather than propagating the
        // raw platform exception (which masks the intended "unmapped" error).
        String key = safeItemKey(adapter, item);
        String result = BE_TO_JE.get(key);
        if (result != null) {
            return result;
        }
        throw new UnsupportedOperationException(
                "No Java item mapping for Nukkit item: " + key
        );
    }

    /**
     * Compute the mapping key for a live item without letting an unstable platform lookup
     * surface as a raw exception.
     */
    private static String safeItemKey(NukkitImplAdapter adapter, Item item) {
        try {
            return adapter.getItemMappingKey(item);
        } catch (RuntimeException ignored) {
            return item.getId() + ":" + item.getDamage();
        }
    }

    private static String beKey(int itemId, int metadata) {
        return itemId + ":" + metadata;
    }

    public record NukkitItemData(String identifier, int itemId, int metadata) {

    }

    private record ItemEntry(
            @SerializedName("bedrock_identifier")
            String bedrockId,
            @SerializedName("bedrock_data")
            int bedrockData
    ) {

    }

}
