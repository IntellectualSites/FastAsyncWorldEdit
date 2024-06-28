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
import com.sk89q.worldedit.extension.input.ParserContext;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

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
    private final ParserContext context;
    private boolean[] ordinals;
    private boolean optimizedStates = true;

    public BlockMaskBuilder() {
        this(new long[BlockTypes.size()][]);
    }

    /**
     * Create a new instance with a given {@link ParserContext} to use if parsing regex
     *
     * @since TODO
     */
    public BlockMaskBuilder(ParserContext context) {
        this(new long[BlockTypes.size()][], context);
    }

    protected BlockMaskBuilder(long[][] bitSets) {
        this.bitSets = bitSets;
        this.context = new ParserContext();
    }

    /**
     * Create a new instance with a given {@link ParserContext} to use if parsing regex
     *
     * @since TODO
     */
    protected BlockMaskBuilder(long[][] bitSets, ParserContext context) {
        this.bitSets = bitSets;
        this.context = context;
    }

    private boolean handleRegex(BlockType blockType, PropertyKey key, String regex, FuzzyStateAllowingBuilder builder) {
        Property<Object> property = blockType.getProperty(key);
        if (property == null) {
            return false;
        }
        boolean result = false;
        List<Object> values = property.getValues();
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).toString().matches(regex)) {
                builder.allow(property, i);
                result = true;
            }
        }
        return result;
    }

    private boolean handleOperator(
            BlockType blockType,
            PropertyKey key,
            Operator operator,
            CharSequence stringValue,
            FuzzyStateAllowingBuilder builder
    ) {
        Property<Object> property = blockType.getProperty(key);
        if (property == null) {
            return false;
        }
        int index = property.getIndexFor(stringValue);
        List<Object> values = property.getValues();
        boolean result = false;
        for (int i = 0; i < values.size(); i++) {
            if (operator.test(index, i)) {
                builder.allow(property, i);
                result = true;
            }
        }
        return result;
    }

    private boolean handleRegexOrOperator(
            BlockType type,
            PropertyKey key,
            Operator operator,
            CharSequence value,
            FuzzyStateAllowingBuilder builder
    ) {
        if (!type.hasProperty(key) && operator == EQUAL) {
            return false;
        }
        if (value.length() == 0) {
            return false;
        }
        if ((operator == EQUAL || operator == EQUAL_OR_NULL) && !StringMan.isAlphanumericUnd(value)) {
            return handleRegex(type, key, value.toString(), builder);
        } else {
            return handleOperator(type, key, operator, value, builder);
        }
    }

    private void add(FuzzyStateAllowingBuilder builder) {
        long[] states = bitSets[builder.getType().getInternalId()];
        if (states == ALL) {
            bitSets[builder.getType().getInternalId()] = states = FastBitSet.create(builder.getType().getMaxStateId() + 1);
            FastBitSet.unsetAll(states);
        }
        applyRecursive(0, builder.getType().getInternalId(), builder, states);
    }

    private void applyRecursive(
            int propertiesIndex,
            int state,
            FuzzyStateAllowingBuilder builder,
            long[] states
    ) {
        AbstractProperty<?> current = (AbstractProperty<?>) builder.getType().getProperties().get(propertiesIndex);
        List<?> values = current.getValues();
        if (propertiesIndex + 1 < builder.getType().getProperties().size()) {
            for (int i = 0; i < values.size(); i++) {
                if (builder.allows(current) || builder.allows(current, i)) {
                    int newState = current.modifyIndex(state, i);
                    applyRecursive(propertiesIndex + 1, newState, builder, states);
                }
            }
        } else {
            for (int i = 0; i < values.size(); i++) {
                if (builder.allows(current) || builder.allows(current, i)) {
                    int index = current.modifyIndex(state, i) >> BlockTypesCache.BIT_OFFSET;
                    FastBitSet.set(states, index);
                }
            }
        }
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

                List<BlockType> blockTypeList;
                List<FuzzyStateAllowingBuilder> builders;
                if (StringMan.isAlphanumericUnd(charSequence)) {
                    BlockType type = BlockTypes.parse(charSequence.toString(), context);
                    blockTypeList = Collections.singletonList(type);
                    builders = Collections.singletonList(new FuzzyStateAllowingBuilder(type));
                    add(type);
                } else {
                    String regex = charSequence.toString();
                    blockTypeList = new ArrayList<>();
                    builders = new ArrayList<>();
                    Pattern pattern = Pattern.compile("(minecraft:)?" + regex);
                    for (BlockType type : BlockTypesCache.values) {
                        if (pattern.matcher(type.id()).find()) {
                            blockTypeList.add(type);
                            builders.add(new FuzzyStateAllowingBuilder(type));
                            add(type);
                        }
                    }
                    if (blockTypeList.isEmpty()) {
                        throw new InputParseException(Caption.of("fawe.error.no-block-found", TextComponent.of(input)));
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
                            if (key == null && (key = PropertyKey.getByName(charSequence)) == null) {
                                suggest(
                                        input,
                                        charSequence.toString(),
                                        blockTypeList
                                );
                            }
                            if (operator == null) {
                                throw new SuggestInputParseException(
                                        Caption.of("fawe.error.no-operator-for-input", input),
                                        () -> Arrays.asList("=", "~", "!", "<", ">", "<=", ">=")
                                );
                            }
                            for (int index = 0; index < blockTypeList.size(); index++) {
                                if (!handleRegexOrOperator(
                                        blockTypeList.get(index),
                                        key,
                                        operator,
                                        charSequence,
                                        builders.get(index)
                                )) {
                                    // If we cannot find a matching property for all to mask, do not mask the block
                                    blockTypeList.remove(index);
                                    builders.remove(index);
                                    index--;
                                }
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
                                            blockTypeList
                                    );
                                }
                            }
                            last = i + 1;
                        }
                        default -> {
                        }
                    }
                }
                for (FuzzyStateAllowingBuilder builder : builders) {
                    if (builder.allows()) {
                        add(builder);
                    }
                }
            } else {
                if (StringMan.isAlphanumericUnd(input)) {
                    add(BlockTypes.parse(input, context));
                } else {
                    boolean success = false;
                    for (BlockType myType : BlockTypesCache.values) {
                        if (myType.id().matches("(minecraft:)?" + input)) {
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

    private static class FuzzyStateAllowingBuilder {

        private final BlockType type;
        private final Map<Property<?>, List<Integer>> masked = new HashMap<>();

        private FuzzyStateAllowingBuilder(BlockType type) {
            this.type = type;
        }

        private BlockType getType() {
            return this.type;
        }

        private List<Property<?>> getMaskedProperties() {
            return masked
                    .entrySet()
                    .stream()
                    .filter(e -> !e.getValue().isEmpty())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        private void allow(Property<?> property, int index) {
            checkNotNull(property);
            if (!type.hasProperty(property.getKey())) {
                throw new IllegalArgumentException(String.format(
                        "Property %s cannot be applied to block type %s",
                        property.getName(),
                        type.id()
                ));
            }
            masked.computeIfAbsent(property, k -> new ArrayList<>()).add(index);
        }

        private boolean allows() {
            //noinspection SimplifyStreamApiCallChains - Marginally faster like this
            return !masked.isEmpty() && !masked.values().stream().anyMatch(List::isEmpty);
        }

        private boolean allows(Property<?> property) {
            return !masked.containsKey(property);
        }

        private boolean allows(Property<?> property, int index) {
            if (!masked.containsKey(property)) {
                return true;
            }
            return masked.get(property).contains(index);
        }

    }

}
