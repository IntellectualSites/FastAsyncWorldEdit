package com.fastasyncworldedit.forge1710.registry;

import com.sk89q.worldedit.registry.state.IntegerProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Maps Minecraft 1.7.10 blocks (numeric id + 4-bit metadata) to FAWE block states and back.
 *
 * <p>1.7.10 blocks do not declare their states, so every block is exposed as {@code namespace:name[legacy_meta=0..15]}.
 * The few ids FAWE reserves with a fixed ordinal (air variants) are exposed without properties.</p>
 */
public final class NativeBlockMapper {

    private static final Logger LOGGER = LogManager.getLogger("FAWE-Forge1710");

    public static final String META_PROPERTY_NAME = "legacy_meta";
    /**
     * Created lazily: AbstractProperty reads BlockTypesCache.BIT_OFFSET in its constructor, so it may only be built while
     * BlockTypesCache initialises (which asks {@link #getProperties}) or later, never before the platform is ready.
     */
    private static volatile IntegerProperty metaProperty;

    /** FAWE ordinals are chars and Character.MAX_VALUE is used as an "unset" sentinel by schematic writers. */
    private static final int MAX_STATES = Character.MAX_VALUE;

    private static volatile NativeBlockMapper instance;

    /** lower-cased WorldEdit id -> Forge block. Insertion ordered by numeric id for stable output. */
    private final Map<String, Block> blocksById = new LinkedHashMap<>();
    /** lower-cased WorldEdit ids that are exposed without the metadata property. */
    private final List<String> propertylessIds = new ArrayList<>();
    private static final List<String> PSEUDO_AIR = List.of("minecraft:cave_air", "minecraft:void_air");
    /** Ids registered only because FAWE reserves them; they read/write as air and never win the native->state lookup. */
    private final List<String> pseudoAirIds = new ArrayList<>();

    private volatile char[] nativeToOrdinal;
    private volatile int[] ordinalToNative;

    private NativeBlockMapper() {
        List<Block> sorted = new ArrayList<>();
        for (Object o : GameData.getBlockRegistry()) {
            sorted.add((Block) o);
        }
        sorted.sort((a, b) -> Integer.compare(Block.getIdFromBlock(a), Block.getIdFromBlock(b)));
        int states = 0;
        for (Block block : sorted) {
            String forgeName = GameData.getBlockRegistry().getNameForObject(block);
            if (forgeName == null) {
                continue;
            }
            String id = forgeName.toLowerCase(Locale.ROOT);
            if (id.indexOf(':') < 0) {
                id = "minecraft:" + id;
            }
            Block previous = blocksById.putIfAbsent(id, block);
            if (previous != null) {
                LOGGER.warn("Block '{}' collides with '{}' after lower-casing; only the first is editable",
                        forgeName, GameData.getBlockRegistry().getNameForObject(previous));
                continue;
            }
            if (id.equals("minecraft:air")) {
                propertylessIds.add(id);
                states += 1;
            } else {
                states += 16;
            }
        }
        // FAWE reserves fixed ids for cave_air and void_air and sizes its tables from this registry, so they must be listed
        // even though 1.7.10 has no such blocks. They behave as plain air.
        for (String pseudoAir : PSEUDO_AIR) {
            if (!blocksById.containsKey(pseudoAir)) {
                blocksById.put(pseudoAir, Blocks.air);
                propertylessIds.add(pseudoAir);
                pseudoAirIds.add(pseudoAir);
                states += 1;
            }
        }
        if (states >= MAX_STATES) {
            throw new IllegalStateException("Too many block states for FAWE (" + states + " >= " + MAX_STATES
                    + "); reduce the number of installed block mods");
        }
        LOGGER.info("Exposing {} blocks as {} FAWE block states", blocksById.size(), states);
    }

    public static NativeBlockMapper get() {
        NativeBlockMapper local = instance;
        if (local == null) {
            synchronized (NativeBlockMapper.class) {
                local = instance;
                if (local == null) {
                    instance = local = new NativeBlockMapper();
                }
            }
        }
        return local;
    }

    public static IntegerProperty metaProperty() {
        IntegerProperty local = metaProperty;
        if (local == null) {
            synchronized (NativeBlockMapper.class) {
                local = metaProperty;
                if (local == null) {
                    metaProperty = local = new IntegerProperty(META_PROPERTY_NAME, metaValues());
                }
            }
        }
        return local;
    }

    private static List<Integer> metaValues() {
        List<Integer> values = new ArrayList<>(16);
        for (int i = 0; i < 16; i++) {
            values.add(i);
        }
        return Collections.unmodifiableList(values);
    }

    /**
     * Block ids in their default state, in the format {@link BlockTypesCache} expects.
     */
    public List<String> values() {
        List<String> values = new ArrayList<>(blocksById.size());
        for (String id : blocksById.keySet()) {
            values.add(hasMetaProperty(id) ? id + "[" + META_PROPERTY_NAME + "=0]" : id);
        }
        return values;
    }

    public boolean hasMetaProperty(String id) {
        return !propertylessIds.contains(id);
    }

    public Map<String, ? extends Property<?>> getProperties(String id) {
        if (!blocksById.containsKey(id) || !hasMetaProperty(id)) {
            return Collections.emptyMap();
        }
        return Collections.singletonMap(META_PROPERTY_NAME, metaProperty());
    }

    public Block getBlock(String id) {
        return blocksById.get(id);
    }

    public Block getBlock(BlockType type) {
        return blocksById.get(type.id());
    }

    /**
     * FAWE ordinal for a native block id and metadata. Unknown ids map to air.
     */
    public char toOrdinal(int blockId, int meta) {
        char[] table = nativeTables();
        int index = (blockId << 4) | (meta & 15);
        if (index < 0 || index >= table.length) {
            return (char) BlockTypesCache.ReservedIDs.AIR;
        }
        return table[index];
    }

    /**
     * Native (blockId << 4 | meta) for a FAWE ordinal, or -1 if the state has no native block.
     */
    public int toNative(int ordinal) {
        int[] table = ordinalToNativeTable();
        return ordinal >= 0 && ordinal < table.length ? table[ordinal] : -1;
    }

    public BlockState toState(Block block, int meta) {
        return BlockTypesCache.states[toOrdinal(Block.getIdFromBlock(block), meta)];
    }

    private char[] nativeTables() {
        char[] local = nativeToOrdinal;
        if (local == null) {
            buildTables();
            local = nativeToOrdinal;
        }
        return local;
    }

    private int[] ordinalToNativeTable() {
        int[] local = ordinalToNative;
        if (local == null) {
            buildTables();
            local = ordinalToNative;
        }
        return local;
    }

    /**
     * Built lazily because FAWE ordinals only exist once {@link BlockTypesCache} has been initialised from this registry.
     */
    private synchronized void buildTables() {
        if (nativeToOrdinal != null) {
            return;
        }
        int maxId = 0;
        for (Block block : blocksById.values()) {
            maxId = Math.max(maxId, Block.getIdFromBlock(block));
        }
        char air = (char) BlockTypesCache.ReservedIDs.AIR;
        char[] toOrdinal = new char[(maxId + 1) << 4];
        java.util.Arrays.fill(toOrdinal, air);
        int[] toNative = new int[BlockTypesCache.states.length];
        java.util.Arrays.fill(toNative, -1);
        for (Map.Entry<String, Block> entry : blocksById.entrySet()) {
            BlockType type = BlockTypes.get(entry.getKey());
            if (type == null) {
                LOGGER.warn("Block {} was not registered with FAWE", entry.getKey());
                continue;
            }
            int blockId = Block.getIdFromBlock(entry.getValue());
            if (pseudoAirIds.contains(entry.getKey())) {
                toNative[type.getDefaultState().getOrdinalChar()] = 0;
                continue;
            }
            if (!hasMetaProperty(entry.getKey())) {
                char ordinal = type.getDefaultState().getOrdinalChar();
                for (int meta = 0; meta < 16; meta++) {
                    toOrdinal[(blockId << 4) | meta] = ordinal;
                }
                toNative[ordinal] = blockId << 4;
                continue;
            }
            // BlockTypesCache re-creates properties with per-type bit offsets, so use the type's own instance.
            Property<Integer> metaProperty = type.getProperty(META_PROPERTY_NAME);
            for (int meta = 0; meta < 16; meta++) {
                BlockState state = type.getDefaultState().with(metaProperty, meta);
                char ordinal = state.getOrdinalChar();
                toOrdinal[(blockId << 4) | meta] = ordinal;
                toNative[ordinal] = (blockId << 4) | meta;
            }
        }
        ordinalToNative = toNative;
        nativeToOrdinal = toOrdinal;
    }

}
