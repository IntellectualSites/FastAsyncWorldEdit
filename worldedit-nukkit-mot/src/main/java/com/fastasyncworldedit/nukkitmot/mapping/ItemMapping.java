package com.fastasyncworldedit.nukkitmot.mapping;

import cn.nukkit.item.Item;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemMapping.class);
    private static final Gson GSON = new Gson();

    private static final Map<String, NukkitItemData> JE_TO_BE = new HashMap<>();
    private static final Map<Integer, String> BE_TO_JE = new HashMap<>();
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
            LOGGER.info("Loaded {} JE item IDs", JE_ITEM_IDS.size());
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

            mappings.forEach((javaId, entry) -> {
                Item nukkitItem = Item.fromString(entry.bedrockId());
                if (nukkitItem != null) {
                    NukkitItemData data = new NukkitItemData(nukkitItem.getId(), entry.bedrockData());
                    JE_TO_BE.put(javaId, data);
                    BE_TO_JE.putIfAbsent(nukkitItem.getId(), javaId);
                }
            });

            LOGGER.info("Loaded {} item mappings", JE_TO_BE.size());
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
            return new NukkitItemData(0, 0);
        }
        return result;
    }

    /**
     * Convert Nukkit item ID to JE item ID.
     */
    public static String beToJe(int beItemId) {
        String result = BE_TO_JE.get(beItemId);
        if (result == null) {
            return "minecraft:air";
        }
        return result;
    }

    public record NukkitItemData(int itemId, int metadata) {
    }

    private record ItemEntry(
            @SerializedName("bedrock_identifier")
            String bedrockId,
            @SerializedName("bedrock_data")
            int bedrockData
    ) {
    }

}
