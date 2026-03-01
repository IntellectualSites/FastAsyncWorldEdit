package com.fastasyncworldedit.nukkitmot.mapping;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.sk89q.worldedit.nukkitmot.WorldEditNukkitPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Maps JE biome identifiers to Nukkit biome IDs and vice versa.
 */
public final class BiomeMapping {

    private static final Gson GSON = new Gson();

    private static final Map<String, Integer> JE_TO_BE = new HashMap<>();
    private static final Map<Integer, String> BE_TO_JE = new HashMap<>();

    private BiomeMapping() {
    }

    public static void init() {
        try (InputStream stream = BiomeMapping.class.getClassLoader().getResourceAsStream("mapping/biomes.json")) {
            if (stream == null) {
                throw new RuntimeException("biomes.json not found");
            }

            Map<String, BiomeEntry> mappings = GSON.fromJson(
                    new JsonReader(new InputStreamReader(Objects.requireNonNull(stream))),
                    new TypeToken<Map<String, BiomeEntry>>() {
                    }.getType()
            );

            mappings.forEach((javaId, entry) -> {
                JE_TO_BE.put(javaId, entry.bedrockId());
                BE_TO_JE.put(entry.bedrockId(), javaId);
            });

            WorldEditNukkitPlugin.getInstance().getLogger().info("Loaded " + JE_TO_BE.size() + " biome mappings");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load biome mapping", e);
        }
    }

    /**
     * Convert JE biome ID (e.g., "minecraft:plains") to Nukkit biome ID.
     */
    public static int jeToBe(String jeBiomeId) {
        Integer result = JE_TO_BE.get(jeBiomeId);
        if (result == null) {
            return 1; // plains as fallback
        }
        return result;
    }

    /**
     * Convert Nukkit biome ID to JE biome ID.
     */
    public static String beToJe(int beBiomeId) {
        String result = BE_TO_JE.get(beBiomeId);
        if (result == null) {
            return "minecraft:plains";
        }
        return result;
    }

    /**
     * Get all JE biome identifiers for registry population.
     */
    public static Collection<String> getAllJeBiomes() {
        return JE_TO_BE.keySet();
    }

    private record BiomeEntry(
            @SerializedName("bedrock_id")
            int bedrockId
    ) {
    }

}
