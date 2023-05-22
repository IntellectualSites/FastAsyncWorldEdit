package com.fastasyncworldedit.core.function.mask;

import com.fastasyncworldedit.core.command.SuggestInputParseException;
import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.math.FastBitSet;
import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.fastasyncworldedit.core.registry.state.PropertyKeySet;
import com.fastasyncworldedit.core.util.MutableCharSequence;
import com.fastasyncworldedit.core.util.StringMan;
import com.fastasyncworldedit.core.world.block.BlanketBaseBlock;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BlockMaskBuilder {

    private static final Operator GREATER = (a, b) -> a > b;
    private static final Operator LESS = (a, b) -> a < b;
    private static final Operator EQUAL = (a, b) -> a == b;
    private static final Operator EQUAL_OR_NULL = (a, b) -> a == b;
    private static final Operator GREATER_EQUAL = (a, b) -> a >= b;
    private static final Operator LESS_EQUAL = (a, b) -> a <= b;
    private static final Operator NOT = (a, b) -> a != b;

    private static final long[] ALL = new long[0];
    private final long[][] bitSets;
    private boolean[] ordinals;
    private boolean optimizedStates = true;

    public BlockMaskBuilder() {
        this(new long[BlockTypes.size()][]);
    }

    protected BlockMaskBuilder(long[][] bitSets) {
        this.bitSets = bitSets;
    }

    private boolean filterRegex(BlockType blockType, PropertyKey key, String regex) {
        Property<Object> property = blockType.getProperty(key);
        if (property == null) {
            return false;
        }
        List<Object> values = property.getValues();
        boolean result = false;
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            if (!value.toString().matches(regex) && has(blockType, property, i)) {
                filter(blockType, property, i);
                result = true;
            }
        }
        return result;
    }

    private boolean filterOperator(BlockType blockType, PropertyKey key, Operator operator, CharSequence value) {
        Property<Object> property = blockType.getProperty(key);
        if (property == null) {
            return false;
        }
        int index = property.getIndexFor(value);
        List<Object> values = property.getValues();
        boolean result = false;
        for (int i = 0; i < values.size(); i++) {
            if (!operator.test(index, i) && has(blockType, property, i)) {
                filter(blockType, property, i);
                result = true;
            }
        }
        return result;
    }

    private boolean filterRegexOrOperator(BlockType type, PropertyKey key, Operator operator, CharSequence value) {
        boolean result = false;
        if (!type.hasProperty(key)) {
            if (operator == EQUAL) {
                result = bitSets[type.getInternalId()] != null;
                remove(type);
            }
        } else {
            if (value.length() == 0) {
                return result;
            }
            if ((operator == EQUAL || operator == EQUAL_OR_NULL) && !StringMan.isAlphanumericUnd(value)) {
                result = filterRegex(type, key, value.toString());
            } else {
                result = filterOperator(type, key, operator, value);
            }
        }
        return result;
    }

    public BlockMaskBuilder addRegex(final String input) throws InputParseException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> fut = executor.submit(() -> {
            if (input.charAt(input.length() - 1) == ']') {
                int propStart = StringMan.findMatchingBracket(input, input.length() - 1);
                if (propStart == -1) {
                    throw new InputParseException(Caption.of("fawe.error.no-block-found", TextComponent.of(input)));
                }

                MutableCharSequence charSequence = MutableCharSequence.getTemporal();
                charSequence.setString(input);
                charSequence.setSubstring(0, propStart);

                BlockType type = null;
                List<BlockType> blockTypeList = null;
                if (StringMan.isAlphanumericUnd(charSequence)) {
                    type = BlockTypes.parse(charSequence.toString());
                    add(type);
                } else {
                    String regex = charSequence.toString();
                    blockTypeList = new ArrayList<>();
                    for (BlockType myType : BlockTypesCache.values) {
                        if (myType.getId().matches(regex)) {
                            blockTypeList.add(myType);
                            add(myType);
                        }
                    }
                    if (blockTypeList.isEmpty()) {
                        throw new InputParseException(Caption.of("fawe.error.no-block-found", TextComponent.of(input)));
                    }
                    if (blockTypeList.size() == 1) {
                        type = blockTypeList.get(0);
                    }
                }
                // Empty string
                charSequence.setSubstring(0, 0);

                PropertyKey key = null;
                int length = input.length();
                int last = propStart + 1;
                Operator operator = null;
                for (int i = last; i < length; i++) {
                    char c = input.charAt(i);
                    switch (c) {
                        case '[', '{', '(' -> {
                            int next = StringMan.findMatchingBracket(input, i);
                            if (next != -1) {
                                i = next;
                            }
                        }
                        case ']', ',' -> {
                            charSequence.setSubstring(last, i);
                            if (key == null && PropertyKey.getByName(charSequence) == null) {
                                suggest(
                                        input,
                                        charSequence.toString(),
                                        type != null ? Collections.singleton(type) : blockTypeList
                                );
                            }
                            if (operator == null) {
                                throw new SuggestInputParseException(
                                        Caption.of("fawe.error.no-operator-for-input", input),
                                        () -> Arrays.asList("=", "~", "!", "<", ">", "<=", ">=")
                                );
                            }
                            boolean filtered = false;
                            if (type != null) {
                                filtered = filterRegexOrOperator(type, key, operator, charSequence);
                            } else {
                                for (BlockType myType : blockTypeList) {
                                    filtered |= filterRegexOrOperator(myType, key, operator, charSequence);
                                }
                            }
                            if (!filtered) {
                                String value = charSequence.toString();
                                final PropertyKey fKey = key;
                                Collection<BlockType> types = type != null ? Collections.singleton(type) : blockTypeList;
                                throw new SuggestInputParseException(Caption.of("fawe.error.no-value-for-input", input), () -> {
                                    HashSet<String> values = new HashSet<>();
                                    types.stream().filter(t -> t.hasProperty(fKey)).forEach(t -> {
                                        Property<Object> p = t.getProperty(fKey);
                                        for (int j = 0; j < p.getValues().size(); j++) {
                                            if (has(t, p, j)) {
                                                String o = p.getValues().get(j).toString();
                                                if (o.startsWith(value)) {
                                                    values.add(o);
                                                }
                                            }
                                        }
                                    });
                                    return new ArrayList<>(values);
                                });
                            }
                            // Reset state
                            key = null;
                            operator = null;
                            last = i + 1;
                        }
                        case '~', '!', '=', '<', '>' -> {
                            charSequence.setSubstring(last, i);
                            boolean extra = input.charAt(i + 1) == '=';
                            if (extra) {
                                i++;
                            }
                            operator = switch (c) {
                                case '~' -> EQUAL_OR_NULL;
                                case '!' -> NOT;
                                case '=' -> EQUAL;
                                case '<' -> extra ? LESS_EQUAL : LESS;
                                case '>' -> extra ? GREATER_EQUAL : GREATER;
                                default -> operator;
                            };
                            if (charSequence.length() > 0 || key == null) {
                                key = PropertyKey.getByName(charSequence);
                                if (key == null) {
                                    suggest(
                                            input,
                                            charSequence.toString(),
                                            type != null ? Collections.singleton(type) : blockTypeList
                                    );
                                }
                            }
                            last = i + 1;
                        }
                        default -> {
                        }
                    }
                }
            } else {
                if (StringMan.isAlphanumericUnd(input)) {
                    add(BlockTypes.parse(input));
                } else {
                    boolean success = false;
                    for (BlockType myType : BlockTypesCache.values) {
                        if (myType.getId().matches(input)) {
                            add(myType);
                            success = true;
                        }
                    }
                    if (!success) {
                        throw new InputParseException(Caption.of("fawe.error.no-block-found", TextComponent.of(input)));
                    }
                }
            }
        });
        try {
            fut.get(5L, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof InputParseException) {
                throw (InputParseException) e.getCause();
            }
        } catch (InterruptedException | TimeoutException ignored) {
        } finally {
            executor.shutdown();
        }
        return this;
    }

    private <T> boolean has(BlockType type, Property<T> property, int index) {
        AbstractProperty<T> prop = (AbstractProperty<T>) property;
        long[] states = bitSets[type.getInternalId()];
        if (states == null) {
            return false;
        }
        int localI = index << prop.getBitOffset() >> BlockTypesCache.BIT_OFFSET;
        return (states == ALL || FastBitSet.get(states, localI));
    }

    private void suggest(String input, String property, Collection<BlockType> finalTypes) throws InputParseException {
        throw new SuggestInputParseException(Caption.of("worldedit.error.parser.unknown-property", property, input), () -> {
            Set<PropertyKey> keys = PropertyKeySet.empty();
            finalTypes.forEach(t -> t.getProperties().forEach(p -> keys.add(p.getKey())));
            return keys.stream().map(PropertyKey::getName)
                    .filter(p -> StringMan.blockStateMatches(property, p))
                    .sorted(StringMan.blockStateComparator(property))
                    .collect(Collectors.toList());
        });
    }

    public boolean isEmpty() {
        return Arrays.stream(bitSets).noneMatch(Objects::nonNull);
    }

    public BlockMaskBuilder addAll() {
        Arrays.fill(bitSets, ALL);
        reset(true);
        return this;
    }

    private void reset(boolean optimized) {
        this.ordinals = null;
        this.optimizedStates = optimized;
    }

    public BlockMaskBuilder clear() {
        Arrays.fill(bitSets, null);
        reset(true);
        return this;
    }

    public BlockMaskBuilder remove(BlockType type) {
        bitSets[type.getInternalId()] = null;
        return this;
    }

    public <T extends BlockStateHolder<T>> BlockMaskBuilder remove(BlockStateHolder<T> state) {
        BlockType type = state.getBlockType();
        int i = type.getInternalId();
        long[] states = bitSets[i];
        if (states != null) {
            if (states == ALL) {
                bitSets[i] = states = FastBitSet.create(type.getMaxStateId() + 1);
                Arrays.fill(states, -1);
            }
            int stateId = state.getInternalPropertiesId();
            FastBitSet.clear(states, stateId);
            reset(false);
        }
        return this;
    }

    public BlockMaskBuilder filter(BlockType type) {
        for (int i = 0; i < bitSets.length; i++) {
            if (i != type.getInternalId()) {
                bitSets[i] = null;
            }
        }
        return this;
    }

    public <T extends BlockStateHolder<T>> BlockMaskBuilder filter(BlockStateHolder<T> state) {
        filter(state.getBlockType());
        BlockType type = state.getBlockType();
        int i = type.getInternalId();
        long[] states = bitSets[i];
        if (states != null) {
            int stateId = state.getInternalPropertiesId();
            boolean set = true;
            if (states == ALL) {
                bitSets[i] = states = FastBitSet.create(type.getMaxStateId() + 1);
            } else {
                set = FastBitSet.get(states, stateId);
                Arrays.fill(states, 0);
            }
            if (set) {
                FastBitSet.set(states, stateId);
            } else {
                bitSets[i] = null;
            }
            reset(true);
        }
        return this;
    }

    public BlockMaskBuilder filter(Predicate<BlockType> allow) {
        for (int i = 0; i < bitSets.length; i++) {
            BlockType type = BlockTypes.get(i);
            if (!allow.test(type)) {
                bitSets[i] = null;
            }
        }
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> BlockMaskBuilder filter(
            Predicate<BlockType> typePredicate,
            BiPredicate<BlockType, Map.Entry<Property<T>, T>> allowed
    ) {
        for (int i = 0; i < bitSets.length; i++) {
            long[] states = bitSets[i];
            if (states == null) {
                continue;
            }
            BlockType type = BlockTypes.get(i);
            if (!typePredicate.test(type)) {
                bitSets[i] = null;
                continue;
            }
            List<AbstractProperty<?>> properties = (List<AbstractProperty<?>>) type.getProperties();
            for (AbstractProperty<?> prop : properties) {
                List<?> values = prop.getValues();
                for (int j = 0; j < values.size(); j++) {
                    int localI = j << prop.getBitOffset() >> BlockTypesCache.BIT_OFFSET;
                    if (states == ALL || FastBitSet.get(states, localI)) {
                        if (!allowed.test(type, new AbstractMap.SimpleEntry(prop, values.get(j)))) {
                            if (states == ALL) {
                                bitSets[i] = states = FastBitSet.create(type.getMaxStateId() + 1);
                                FastBitSet.setAll(states);
                            }
                            FastBitSet.clear(states, localI);
                            reset(false);
                        }
                    }
                }
            }
        }
        return this;
    }

    public BlockMaskBuilder add(BlockType type) {
        bitSets[type.getInternalId()] = ALL;
        return this;
    }

    public <T extends BlockStateHolder<T>> BlockMaskBuilder add(BlockStateHolder<T> state) {
        BlockType type = state.getBlockType();
        if (state instanceof BlanketBaseBlock) {
            return add(type);
        }
        int i = type.getInternalId();
        long[] states = bitSets[i];
        if (states != ALL) {
            if (states == null) {
                bitSets[i] = states = FastBitSet.create(type.getMaxStateId() + 1);
            }
            int stateId = state.getInternalPropertiesId();
            FastBitSet.set(states, stateId);
            reset(false);
        }
        return this;
    }

    public <T extends BlockStateHolder<T>> BlockMaskBuilder addBlocks(Collection<T> blocks) {
        for (BlockStateHolder<T> block : blocks) {
            add(block);
        }
        return this;
    }

    public BlockMaskBuilder addTypes(Collection<BlockType> blocks) {
        for (BlockType block : blocks) {
            add(block);
        }
        return this;
    }

    public <T extends BlockStateHolder<T>> BlockMaskBuilder addBlocks(T... blocks) {
        for (BlockStateHolder<T> block : blocks) {
            add(block);
        }
        return this;
    }

    public BlockMaskBuilder addTypes(BlockType... blocks) {
        for (BlockType block : blocks) {
            add(block);
        }
        return this;
    }

    public BlockMaskBuilder addAll(Predicate<BlockType> allow) {
        for (int i = 0; i < bitSets.length; i++) {
            BlockType type = BlockTypes.get(i);
            if (allow.test(type)) {
                bitSets[i] = ALL;
            }
        }
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public BlockMaskBuilder addAll(
            Predicate<BlockType> typePredicate,
            BiPredicate<BlockType, Map.Entry<Property<?>, ?>> propPredicate
    ) {
        for (int i = 0; i < bitSets.length; i++) {
            long[] states = bitSets[i];
            if (states == ALL) {
                continue;
            }
            BlockType type = BlockTypes.get(i);
            if (!typePredicate.test(type)) {
                continue;
            }
            for (AbstractProperty<?> prop : (List<AbstractProperty<?>>) type.getProperties()) {
                List<?> values = prop.getValues();
                for (int j = 0; j < values.size(); j++) {
                    int localI = j << prop.getBitOffset() >> BlockTypesCache.BIT_OFFSET;
                    if (states == null || !FastBitSet.get(states, localI)) {
                        if (propPredicate.test(type, new AbstractMap.SimpleEntry(prop, values.get(j)))) {
                            if (states == null) {
                                bitSets[i] = states = FastBitSet.create(type.getMaxStateId() + 1);
                            }
                            FastBitSet.set(states, localI);
                            reset(false);
                        }
                    }
                }
            }
        }
        return this;
    }

    public <T> BlockMaskBuilder add(BlockType type, Property<T> property, int index) {
        AbstractProperty<T> prop = (AbstractProperty<T>) property;
        long[] states = bitSets[type.getInternalId()];
        if (states == ALL) {
            return this;
        }

        List<T> values = property.getValues();
        int localI = index << prop.getBitOffset() >> BlockTypesCache.BIT_OFFSET;
        if (states == null || !FastBitSet.get(states, localI)) {
            if (states == null) {
                bitSets[type.getInternalId()] = states = FastBitSet.create(type.getMaxStateId() + 1);
            }
            set(type, states, property, index);
            reset(false);
        }
        return this;
    }

    public <T> BlockMaskBuilder filter(BlockType type, Property<T> property, int index) {
        AbstractProperty<T> prop = (AbstractProperty<T>) property;
        long[] states = bitSets[type.getInternalId()];
        if (states == null) {
            return this;
        }
        List<T> values = property.getValues();
        int localI = index << prop.getBitOffset() >> BlockTypesCache.BIT_OFFSET;
        if (states == ALL || FastBitSet.get(states, localI)) {
            if (states == ALL) {
                bitSets[type.getInternalId()] = states = FastBitSet.create(type.getMaxStateId() + 1);
                FastBitSet.setAll(states);
            }
            clear(type, states, property, index);
            reset(false);
        }
        return this;
    }

    private void applyRecursive(List<Property> properties, int propertiesIndex, int state, long[] states, boolean set) {
        AbstractProperty current = (AbstractProperty) properties.get(propertiesIndex);
        List values = current.getValues();
        if (propertiesIndex + 1 < properties.size()) {
            for (int i = 0; i < values.size(); i++) {
                int newState = current.modifyIndex(state, i);
                applyRecursive(properties, propertiesIndex + 1, newState, states, set);
            }
        } else {
            for (int i = 0; i < values.size(); i++) {
                int index = current.modifyIndex(state, i) >> BlockTypesCache.BIT_OFFSET;
                if (set) {
                    FastBitSet.set(states, index);
                } else {
                    FastBitSet.clear(states, index);
                }
            }
        }
    }

    private void set(BlockType type, long[] bitSet, Property property, int index) {
        FastBitSet.set(bitSet, index);
        if (type.getProperties().size() > 1) {
            ArrayList<Property> properties = new ArrayList<>(type.getProperties());
            properties.remove(property);
            int state = ((AbstractProperty) property).modifyIndex(type.getInternalId(), index);
            applyRecursive(properties, 0, state, bitSet, true);
        }
    }

    private void clear(BlockType type, long[] bitSet, Property property, int index) {
        FastBitSet.clear(bitSet, index);
        if (type.getProperties().size() > 1) {
            ArrayList<Property> properties = new ArrayList<>(type.getProperties());
            properties.remove(property);
            int state = ((AbstractProperty) property).modifyIndex(type.getInternalId(), index);
            applyRecursive(properties, 0, state, bitSet, false);
        }
    }

    public BlockMaskBuilder optimize() {
        if (!optimizedStates) {
            for (int i = 0; i < bitSets.length; i++) {
                long[] bitSet = bitSets[i];
                if (bitSet == null || bitSet == ALL) {
                    continue;
                }
                BlockType type = BlockTypes.get(i);
                int maxStateId = type.getMaxStateId();
                if (maxStateId == 0) {
                    if (bitSet[0] == 0) {
                        bitSets[i] = null;
                        continue;
                    } else {
                        bitSets[i] = ALL;
                        continue;
                    }
                }
                int set = 0;
                int clear = 0;
                for (AbstractProperty<?> prop : (List<AbstractProperty<?>>) type.getProperties()) {
                    List<?> values = prop.getValues();
                    for (int j = 0; j < values.size(); j++) {
                        int localI = j << prop.getBitOffset() >> BlockTypesCache.BIT_OFFSET;
                        if (FastBitSet.get(bitSet, localI)) {
                            set++;
                        } else {
                            clear++;
                        }
                    }
                }
                if (set == 0) {
                    bitSets[i] = null;
                } else if (clear == 0) {
                    bitSets[i] = ALL;
                }
            }
            reset(true);
        }
        return this;
    }

    private boolean[] getOrdinals() {
        if (ordinals == null) {
            ordinals = new boolean[BlockTypesCache.states.length];
            for (int i = 0; i < BlockTypesCache.values.length; i++) {
                long[] bitSet = bitSets[i];
                if (bitSet == null) {
                    continue;
                }
                BlockType type = BlockTypesCache.values[i];
                if (bitSet == ALL) {
                    for (BlockState state : type.getAllStates()) {
                        ordinals[state.getOrdinal()] = true;
                    }
                } else {
                    for (BlockState state : type.getAllStates()) {
                        if (FastBitSet.get(bitSet, state.getInternalPropertiesId())) {
                            ordinals[state.getOrdinal()] = true;
                        }
                    }

                }
            }
        }
        return ordinals;
    }

    public BlockMask build(Extent extent) {
        return new BlockMask(extent, getOrdinals());
    }

    private interface Operator {

        boolean test(int left, int right);

    }

}
