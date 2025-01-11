package com.sk89q.worldedit.world.block;

import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.fastasyncworldedit.core.util.MathMan;
import com.google.common.primitives.Booleans;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.Registries;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BlockTypesCache {

    /*
     -----------------------------------------------------
                    Settings
     -----------------------------------------------------
     */
    protected static final class Settings {

        final int internalId;
        final BlockState defaultState;
        final AbstractProperty<?>[] propertiesMapArr;
        final AbstractProperty<?>[] propertiesArr;
        final List<AbstractProperty<?>> propertiesList;
        final Map<String, AbstractProperty<?>> propertiesMap;
        final Set<AbstractProperty<?>> propertiesSet;
        final BlockMaterial blockMaterial;
        final int permutations;
        int[] stateOrdinals;

        Settings(BlockType type, String id, int internalId, List<BlockState> states) {
            this.internalId = internalId;
            String propertyString = null;
            int propI = id.indexOf('[');
            if (propI != -1) {
                propertyString = id.substring(propI + 1, id.length() - 1);
            }

            int maxInternalStateId = 0;
            Map<String, ? extends Property<?>> properties = WorldEdit.getInstance().getPlatformManager().queryCapability(
                    Capability.GAME_HOOKS).getRegistries().getBlockRegistry().getProperties(type);
            if (!properties.isEmpty()) {
                // Ensure the properties are registered
                int maxOrdinal = 0;
                for (String key : properties.keySet()) {
                    maxOrdinal = Math.max(PropertyKey.getOrCreate(key).getId(), maxOrdinal);
                }
                this.propertiesMapArr = new AbstractProperty[maxOrdinal + 1];
                int prop_arr_i = 0;
                this.propertiesArr = new AbstractProperty[properties.size()];
                // Preserve properties order with LinkedHashMap
                HashMap<String, AbstractProperty<?>> propMap = new LinkedHashMap<>();

                int bitOffset = 0;
                for (Map.Entry<String, ? extends Property<?>> entry : properties.entrySet()) {
                    PropertyKey key = PropertyKey.getOrCreate(entry.getKey());
                    AbstractProperty<?> property = ((AbstractProperty) entry.getValue()).withOffset(bitOffset);
                    this.propertiesMapArr[key.getId()] = property;
                    this.propertiesArr[prop_arr_i++] = property;
                    propMap.put(entry.getKey(), property);

                    maxInternalStateId += (property.getValues().size() << bitOffset);
                    bitOffset += property.getNumBits();
                }
                this.propertiesList = Arrays.asList(this.propertiesArr);
                this.propertiesMap = Collections.unmodifiableMap(propMap);
                this.propertiesSet = new LinkedHashSet<>(this.propertiesMap.values());
            } else {
                this.propertiesMapArr = new AbstractProperty[0];
                this.propertiesArr = this.propertiesMapArr;
                this.propertiesList = Collections.emptyList();
                this.propertiesMap = Collections.emptyMap();
                this.propertiesSet = Collections.emptySet();
            }
            this.permutations = maxInternalStateId;

            this.blockMaterial = WorldEdit
                    .getInstance()
                    .getPlatformManager()
                    .queryCapability(Capability.GAME_HOOKS)
                    .getRegistries()
                    .getBlockRegistry()
                    .getMaterial(type);

            if (!propertiesList.isEmpty()) {
                this.stateOrdinals = generateStateOrdinals(internalId, states.size(), maxInternalStateId, propertiesList);

                for (int propId = 0; propId < this.stateOrdinals.length; propId++) {
                    int ordinal = this.stateOrdinals[propId];
                    if (ordinal != -1) {
                        int stateId = internalId + (propId << BIT_OFFSET);
                        CompoundTag defaultNBT = blockMaterial.getDefaultTile();
                        BlockState state = defaultNBT != null ? new BlockState(
                                type,
                                stateId,
                                ordinal,
                                blockMaterial.getDefaultTile()
                        ) :
                                new BlockState(type, stateId, ordinal);
                        states.add(state);
                    }
                }
                int defaultPropId = parseProperties(propertyString, propertiesMap) >> BIT_OFFSET;

                this.defaultState = states.get(this.stateOrdinals[defaultPropId]);
            } else {
                CompoundTag defaultNBT = blockMaterial.getDefaultTile();
                this.defaultState = defaultNBT != null ? new BlockState(
                        type,
                        internalId,
                        states.size(),
                        blockMaterial.getDefaultTile()
                ) :
                        new BlockState(type, internalId, states.size());
                states.add(this.defaultState);
            }
        }

        private int parseProperties(String properties, Map<String, AbstractProperty<?>> propertyMap) {
            int id = internalId;
            for (String keyPair : properties.split(",")) {
                String[] split = keyPair.split("=");
                String name = split[0];
                String value = split[1];
                AbstractProperty btp = propertyMap.get(name);
                id = btp.modify(id, btp.getValueFor(value));
            }
            return id;
        }

    }


    private static int[] generateStateOrdinals(int internalId, int ordinal, int maxStateId, List<AbstractProperty<?>> props) {
        if (props.isEmpty()) {
            return null;
        }
        int[] result = new int[maxStateId];
        Arrays.fill(result, -1);
        int[] state = new int[props.size()];
        int[] sizes = new int[props.size()];
        for (int i = 0; i < props.size(); i++) {
            sizes[i] = props.get(i).getValues().size();
        }
        int index = 0;
        outer:
        while (true) {
            // Create the state
            int stateId = internalId;
            for (int i = 0; i < state.length; i++) {
                stateId = props.get(i).modifyIndex(stateId, state[i]);
            }
            // Map it to the ordinal
            result[stateId >> BIT_OFFSET] = ordinal++;
            // Increment the state
            while (++state[index] == sizes[index]) {
                state[index] = 0;
                index++;
                if (index == state.length) {
                    break outer;
                }
            }
            index = 0;
        }
        return result;
    }

    /*
     -----------------------------------------------------
                    Static Initializer
     -----------------------------------------------------
     */

    public static final int BIT_OFFSET; // Used internally
    protected static final int BIT_MASK; // Used internally

    //private static final Map<String, BlockType> $REGISTRY = new HashMap<>();
    //public static final NamespacedRegistry<BlockType> REGISTRY = new NamespacedRegistry<>("block type", $REGISTRY);

    public static final BlockType[] values;
    public static final BlockState[] states;
    /**
     * Array of blockstates in order of ordinal indicating if the block ticks, e.g. leaves, water
     */
    public static final boolean[] ticking;
    private static final Map<String, List<Property<?>>> allProperties = new HashMap<>();

    protected static final Set<String> $NAMESPACES = new LinkedHashSet<>();

    static {
        try {
            ArrayList<BlockState> stateList = new ArrayList<>();
            ArrayList<Boolean> tickList = new ArrayList<>();

            Platform platform = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS);
            Registries registries = platform.getRegistries();
            BlockRegistry blockReg = registries.getBlockRegistry();
            Collection<String> blocks = blockReg.values();
            Map<String, String> blockMap = blocks.stream().collect(Collectors.toMap(item -> item.charAt(item.length() - 1) == ']'
                    ? item.substring(0, item.indexOf('['))
                    : item, item -> item));

            int size = blockMap.size() + 1;
            BIT_OFFSET = MathMan.log2nlz(size);
            BIT_MASK = ((1 << BIT_OFFSET) - 1);
            values = new BlockType[size];

            // Register reserved IDs. Ensure air/reserved are 0/1/2/3
            {
                for (Field field : ReservedIDs.class.getDeclaredFields()) {
                    if (field.getType() == int.class) {
                        int internalId = field.getInt(null);
                        String id = "minecraft:" + field.getName().toLowerCase(Locale.ROOT);
                        String defaultState = blockMap.remove(id);
                        if (defaultState == null) {
                            defaultState = id;
                        }
                        if (values[internalId] != null) {
                            // Ugly way of ensuring a stacktrace is printed so we can see the culprit. Rethrow because we still
                            // want to cancel whatever initialised the class.
                            try {
                                throw new IllegalStateException(String.format(
                                        "Invalid duplicate id for %s! Something has gone very wrong. Are " +
                                                "any plugins shading FAWE?!", id));
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                                throw e;
                            }
                        }
                        BlockType type = register(defaultState, internalId, stateList, tickList);
                        // Note: Throws IndexOutOfBoundsError if nothing is registered and blocksMap is empty
                        values[internalId] = type;
                    }
                }
            }

            { // Register real blocks
                int internalId = 0;
                for (Map.Entry<String, String> entry : blockMap.entrySet()) {
                    String defaultState = entry.getValue();
                    // Skip already registered ids
                    for (; values[internalId] != null; internalId++) {
                    }
                    BlockType type = register(defaultState, internalId, stateList, tickList);
                    values[internalId] = type;
                }
            }
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) {
                    values[i] = values[0];
                }
            }

            states = stateList.toArray(new BlockState[stateList.size()]);
            ticking = Booleans.toArray(tickList);

        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static BlockType register(final String id, int internalId, List<BlockState> states, List<Boolean> tickList) {
        // Get the enum name (remove namespace if minecraft:)
        int propStart = id.indexOf('[');
        String typeName = id.substring(0, propStart == -1 ? id.length() : propStart);
        String enumName = (typeName.startsWith("minecraft:") ? typeName.substring(10) : typeName).toUpperCase(Locale.ROOT);
        int oldsize = states.size();
        BlockType existing = new BlockType(id, internalId, states);
        tickList.addAll(Collections.nCopies(states.size() - oldsize,
                existing.getMaterial().isTicksRandomly() || existing.getMaterial().isLiquid()));
        // register states
        BlockType.REGISTRY.register(typeName, existing);
        String nameSpace = typeName.substring(0, typeName.indexOf(':'));
        $NAMESPACES.add(nameSpace);
        return existing;
    }

    /**
     * Get a list of all block properties available.
     *
     * @return map of string key against property of all block properties available
     */
    public static Map<String, List<Property<?>>> getAllProperties() {
        synchronized (allProperties) {
            if (allProperties.size() == 0) {
                allProperties.putAll(WorldEdit
                        .getInstance()
                        .getPlatformManager()
                        .queryCapability(Capability.GAME_HOOKS)
                        .getRegistries()
                        .getBlockRegistry()
                        .getAllProperties());
            }
            return allProperties;
        }
    }

    /**
     * Statically-set reserved IDs. Should be used as minimally as possible, and for IDs that will see frequent use
     */
    public static class ReservedIDs {
        public static final int __RESERVED__ = 0;
        public static final int AIR = 1;
        public static final int CAVE_AIR = 2;
        public static final int VOID_AIR = 3;

    }

}
