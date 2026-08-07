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
        String result = BE_TO_JE.get(adapter.getItemMappingKey(item));
        if (result != null) {
            return result;
        }
        // Air may map under a different key than the live item's key (e.g. PNX identifier-based
        // keys), so resolve it explicitly when the item is air before falling through to throw.
        if (adapter.isAirItem(item)) {
            NukkitItemData airData = adapter.createItemData("minecraft:air", 0);
            if (airData != null) {
                String airResult = BE_TO_JE.get(adapter.getItemMappingKey(airData));
                if (airResult != null) {
                    return airResult;
                }
            }
        }
        throw new UnsupportedOperationException(
                "No Java item mapping for Nukkit item: " + adapter.getItemMappingKey(item)
        );
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
