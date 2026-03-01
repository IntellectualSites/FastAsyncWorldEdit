package com.fastasyncworldedit.nukkitmot.mapping;

import cn.nukkit.block.Block;
import cn.nukkit.level.format.leveldb.BlockStateMapping;
import cn.nukkit.level.format.leveldb.NukkitLegacyMapper;
import cn.nukkit.level.format.leveldb.structure.BlockStateSnapshot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import it.unimi.dsi.fastutil.ints.Int2CharOpenHashMap;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Core mapping class for Java Edition block states to Nukkit block data (and vice versa).
 * <p>
 * Uses GeyserMC's blocks.json for JE ↔ BE block state mapping and Nukkit's
 * BlockStateMapping to convert BE identifier+state to legacy fullId.
 */
public final class BlockMapping {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockMapping.class);

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(new IgnoreFailureTypeAdapterFactory())
            .create();

    // JE hash → BE block data
    private static final Map<Integer, NukkitBlockData> JE_HASH_TO_BE = new HashMap<>();
    // BE fullId → JE block state
    private static final Map<Integer, JeBlockState> BE_FULL_ID_TO_JE = new HashMap<>();
    // JE block default properties
    private static final Map<String, Map<String, String>> JE_BLOCK_DEFAULT_PROPERTIES = new HashMap<>();

    // Ordinal mappings (built lazily after BlockTypesCache is initialized)
    private static int[] jeOrdinalToBeFullId;
    private static Int2CharOpenHashMap beFullIdToJeOrdinal;

    private static int blockStateVersion;
    private static boolean initialized = false;

    private BlockMapping() {
    }

    /**
     * Initialize JE block default properties from je_blocks.json.
     * Must be called before WorldEdit's loadMappings() to provide block data for BlockTypesCache.
     */
    public static void initJeBlockDefaults() {
        if (!JE_BLOCK_DEFAULT_PROPERTIES.isEmpty()) {
            return;
        }
        if (!initJeBlockDefaultProperties()) {
            throw new RuntimeException("Failed to load JE block default properties");
        }
    }

    /**
     * Initialize block state mappings from JSON resources.
     * Must be called during plugin initialization, after {@link #initJeBlockDefaults()}.
     */
    public static void init() {
        if (initialized) {
            return;
        }

        initJeBlockDefaults();
        if (!initBlockStateMapping()) {
            throw new RuntimeException("Failed to load block state mapping");
        }

        initialized = true;
    }

    /**
     * Build ordinal mappings after BlockTypesCache has been initialized.
     * This must be called AFTER WorldEdit registries are set up.
     */
    public static void buildOrdinalMappings() {
        BlockState[] states = BlockTypesCache.states;
        jeOrdinalToBeFullId = new int[states.length];
        beFullIdToJeOrdinal = new Int2CharOpenHashMap(states.length);
        beFullIdToJeOrdinal.defaultReturnValue(Character.MAX_VALUE);

        int mapped = 0;
        int unmapped = 0;

        for (int ordinal = 0; ordinal < states.length; ordinal++) {
            BlockState state = states[ordinal];
            if (state == null) {
                jeOrdinalToBeFullId[ordinal] = 0; // air
                continue;
            }

            // Build JE block state string from WorldEdit BlockState
            String jeStateStr = state.getAsString();
            JeBlockState jeState = JeBlockState.fromString(jeStateStr);
            jeState.completeMissingProperties(JE_BLOCK_DEFAULT_PROPERTIES.get(jeState.getIdentifier()));

            NukkitBlockData beData = JE_HASH_TO_BE.get(jeState.getHash());
            if (beData != null) {
                int fullId = beData.getFullId();
                jeOrdinalToBeFullId[ordinal] = fullId;
                beFullIdToJeOrdinal.putIfAbsent(fullId, (char) ordinal);
                mapped++;
            } else {
                jeOrdinalToBeFullId[ordinal] = 0; // air as fallback
                unmapped++;
            }
        }

        LOGGER.info("Ordinal mappings built: {} mapped, {} unmapped out of {} total states",
                mapped, unmapped, states.length);
    }

    /**
     * Convert JE block state to BE block data.
     */
    public static NukkitBlockData jeToBe(JeBlockState state) {
        NukkitBlockData result = JE_HASH_TO_BE.get(state.getHash());
        if (result == null) {
            return NukkitBlockData.AIR;
        }
        return result;
    }

    /**
     * Convert BE fullId to JE block state.
     */
    @Nullable
    public static JeBlockState beToJe(int fullId) {
        return BE_FULL_ID_TO_JE.get(fullId);
    }

    /**
     * Hot path: convert JE ordinal to BE fullId.
     */
    public static int jeOrdinalToFullId(char ordinal) {
        return jeOrdinalToBeFullId[ordinal];
    }

    /**
     * Hot path: convert BE fullId to JE ordinal.
     * Returns Character.MAX_VALUE if no mapping exists.
     */
    public static char fullIdToJeOrdinal(int fullId) {
        return beFullIdToJeOrdinal.get(fullId);
    }

    /**
     * Get JE block default properties for a given identifier.
     */
    public static Map<String, String> getJeBlockDefaultProperties(String identifier) {
        Map<String, String> props = JE_BLOCK_DEFAULT_PROPERTIES.get(identifier);
        return props != null ? props : Map.of();
    }

    /**
     * Get all JE block default states as string representations.
     * Must be called after {@link #initJeBlockDefaults()}.
     *
     * @return collection of default block state strings, e.g. "minecraft:stone" or
     *         "minecraft:oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]"
     */
    public static Collection<String> getAllJeBlockDefaultStates() {
        List<String> result = new ArrayList<>(JE_BLOCK_DEFAULT_PROPERTIES.size());
        for (Map.Entry<String, Map<String, String>> entry : JE_BLOCK_DEFAULT_PROPERTIES.entrySet()) {
            String id = entry.getKey();
            Map<String, String> properties = entry.getValue();
            if (properties == null || properties.isEmpty()) {
                result.add(id);
            } else {
                StringBuilder sb = new StringBuilder(id).append("[");
                boolean first = true;
                for (Map.Entry<String, String> prop : properties.entrySet()) {
                    if (!first) {
                        sb.append(",");
                    }
                    sb.append(prop.getKey()).append("=").append(prop.getValue());
                    first = false;
                }
                sb.append("]");
                result.add(sb.toString());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static boolean initJeBlockDefaultProperties() {
        try (InputStream stream = BlockMapping.class.getClassLoader().getResourceAsStream("je_blocks.json")) {
            if (stream == null) {
                LOGGER.error("je_blocks.json not found");
                return false;
            }

            Map<String, List<Map<String, ?>>> data = from(stream, new TypeToken<>() {
            });
            for (var entry : data.entrySet()) {
                JE_BLOCK_DEFAULT_PROPERTIES.put(
                        "minecraft:" + entry.getKey(),
                        (Map<String, String>) entry.getValue().get(1)
                );
            }
            LOGGER.info("Loaded {} JE block default properties", JE_BLOCK_DEFAULT_PROPERTIES.size());
        } catch (IOException e) {
            LOGGER.error("Failed to load je_blocks.json", e);
            return false;
        }
        return true;
    }

    private static boolean initBlockStateMapping() {
        // Extract block state version from palette
        List<NbtMap> palette = NukkitLegacyMapper.loadBlockPalette();
        if (!palette.isEmpty()) {
            blockStateVersion = palette.getFirst().getInt("version");
            LOGGER.info("Block state version: {}", blockStateVersion);
        }

        try (InputStream stream = BlockMapping.class.getClassLoader().getResourceAsStream("mapping/blocks.json")) {
            if (stream == null) {
                LOGGER.error("blocks.json not found");
                return false;
            }

            Map<String, List<BlockMappingEntry>> root = from(stream, new TypeToken<>() {
            });
            List<BlockMappingEntry> mappings = root.get("mappings");
            int mapped = 0;
            int failed = 0;

            for (BlockMappingEntry mapping : mappings) {
                JeBlockState jeState = createJeBlockState(mapping.javaState());
                NukkitBlockData beData = createNukkitBlockData(mapping.bedrockState());
                if (beData != null) {
                    JE_HASH_TO_BE.put(jeState.getHash(), beData);
                    BE_FULL_ID_TO_JE.put(beData.getFullId(), jeState);
                    mapped++;
                } else {
                    failed++;
                }
            }
            LOGGER.info("Block state mapping loaded: {} mapped, {} failed", mapped, failed);
        } catch (IOException e) {
            LOGGER.error("Failed to load blocks.json", e);
            return false;
        }
        return true;
    }

    private static JeBlockState createJeBlockState(BlockMappingEntry.JavaState state) {
        Map<String, String> properties = state.properties() == null ? Map.of() : state.properties();
        JeBlockState jeState = JeBlockState.create(state.name(), new TreeMap<>(properties));
        jeState.completeMissingProperties(JE_BLOCK_DEFAULT_PROPERTIES.get(state.name()));
        return jeState;
    }

    @Nullable
    private static NukkitBlockData createNukkitBlockData(BlockMappingEntry.BedrockState state) {
        try {
            String beName = "minecraft:" + state.bedrockId();
            NbtMapBuilder statesBuilder = NbtMap.builder();
            if (state.state() != null) {
                for (Map.Entry<String, Object> entry : state.state().entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof Number number) {
                        statesBuilder.putInt(entry.getKey(), number.intValue());
                    } else if (value instanceof Boolean bool) {
                        statesBuilder.putByte(entry.getKey(), (byte) (bool ? 1 : 0));
                    } else {
                        statesBuilder.putString(entry.getKey(), value.toString());
                    }
                }
            }

            NbtMap nbtState = NbtMap.builder()
                    .putString("name", beName)
                    .putCompound("states", statesBuilder.build())
                    .putInt("version", blockStateVersion)
                    .build();

            BlockStateSnapshot snapshot = BlockStateMapping.get().getStateUnsafe(nbtState);
            if (snapshot == null) {
                return null;
            }

            int legacyId = snapshot.getLegacyId();
            int legacyData = snapshot.getLegacyData();
            if (legacyId == -1) {
                return null;
            }
            return new NukkitBlockData(legacyId, legacyData);
        } catch (Exception e) {
            return null;
        }
    }

    private static <V> V from(InputStream inputStream, TypeToken<V> typeToken) {
        JsonReader reader = new JsonReader(new InputStreamReader(Objects.requireNonNull(inputStream)));
        return GSON.fromJson(reader, typeToken.getType());
    }

    // JSON model records

    public record BlockMappingEntry(
            @SerializedName("java_state")
            JavaState javaState,
            @SerializedName("bedrock_state")
            BedrockState bedrockState
    ) {
        public record JavaState(
                @SerializedName("Name")
                String name,
                @Nullable
                @SerializedName("Properties")
                Map<String, String> properties
        ) {
        }

        public record BedrockState(
                @SerializedName("bedrock_identifier")
                String bedrockId,
                @Nullable
                Map<String, Object> state
        ) {
        }
    }

    public static class IgnoreFailureTypeAdapterFactory implements TypeAdapterFactory {

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            TypeAdapter<T> delegate = gson.getDelegateAdapter(this, typeToken);
            return new TypeAdapter<>() {
                @Override
                public void write(JsonWriter writer, T value) throws IOException {
                    delegate.write(writer, value);
                }

                @Override
                public T read(JsonReader reader) throws IOException {
                    try {
                        return delegate.read(reader);
                    } catch (Exception e) {
                        reader.skipValue();
                        return null;
                    }
                }
            };
        }

    }

}
